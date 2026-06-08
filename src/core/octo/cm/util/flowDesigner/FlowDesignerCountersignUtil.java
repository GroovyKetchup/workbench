package octo.cm.util.flowDesigner;

import cell.gpf.dc.runtime.IDCRuntimeContext;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import gpf.adur.user.User;
import gpf.dc.config.RefPDCNode;
import gpf.dc.runtime.PDFInstance;
import gpf.dto.cfg.runtime.RouterOption;
import octo.cm.dto.flowDesigner.rule.CountersignApprovalDto;
import octo.cm.dto.flowDesigner.rule.CountersignConfigDto;
import octo.cm.dto.flowDesigner.rule.CountersignInstanceDto;
import octo.cm.dto.flowDesigner.rule.CountersignParticipantDto;
import octo.cm.util.UserRoleUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Comment("流程设计器会签工具类")
@ClassDeclare(
        label = "流程设计器会签工具类",
        what = "处理流程设计器会签实例读写、会签判定和离开路由",
        why = "支撑前端会签节点发布的会签进入和会签离开规则",
        how = "由 FlowDesignerCountersignRule 调用",
        developer = "裴硕", version = "1.0",
        createTime = "2026-05-14", updateTime = "2026-05-14"
)
public class FlowDesignerCountersignUtil {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_COMPLETED = "completed";
    public static final String ACTION_AGREE = "agree";
    public static final String ACTION_DISAGREE = "disagree";
    public static final String RESULT_PASS = "pass";
    public static final String RESULT_REJECT = "reject";
    public static final String APPROVAL_COUNT = "count";
    public static final String APPROVAL_PERCENT = "percent";

    public static CountersignConfigDto parseConfig(String configJson) {
        if (StrUtil.isBlank(configJson)) throw new IllegalArgumentException("会签配置不能为空");
        CountersignConfigDto config;
        try {
            config = JSONUtil.toBean(configJson, CountersignConfigDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("会签配置JSON格式非法", e);
        }
        validateConfig(config);
        return config;
    }

    public static void validateConfig(CountersignConfigDto config) {
        if (config == null) throw new IllegalArgumentException("会签配置不能为空");
        if (StrUtil.isBlank(config.getField())) throw new IllegalArgumentException("会签配置缺少会签信息字段");
        if (CollUtil.isEmpty(config.getRoleCodes())) throw new IllegalArgumentException("会签配置缺少参与角色");
        validateApproval(config.getApproval());
        if (CollUtil.isEmpty(config.getUserActions())) throw new IllegalArgumentException("会签配置缺少用户动作");
        if (StrUtil.isBlank(config.getUserActions().get(ACTION_AGREE))) throw new IllegalArgumentException("会签配置缺少同意按钮");
        if (StrUtil.isBlank(config.getUserActions().get(ACTION_DISAGREE))) throw new IllegalArgumentException("会签配置缺少不同意按钮");
        if (CollUtil.isEmpty(config.getRoutes())) throw new IllegalArgumentException("会签配置缺少结果路由");
        if (StrUtil.isBlank(config.getRoutes().get(RESULT_PASS))) throw new IllegalArgumentException("会签配置缺少通过目标节点");
        if (StrUtil.isBlank(config.getRoutes().get(RESULT_REJECT))) throw new IllegalArgumentException("会签配置缺少不通过目标节点");
    }

    public static void validateApproval(CountersignApprovalDto approval) {
        if (approval == null) throw new IllegalArgumentException("会签配置缺少通过规则");
        String type = defaultIfBlank(approval.getType(), APPROVAL_COUNT);
        if (!APPROVAL_COUNT.equals(type) && !APPROVAL_PERCENT.equals(type)) throw new IllegalArgumentException("会签通过规则类型非法：" + type);
        if (approval.getValue() == null || approval.getValue() < 1) throw new IllegalArgumentException("会签通过规则数值必须大于0");
        if (APPROVAL_PERCENT.equals(type) && approval.getValue() > 100) throw new IllegalArgumentException("会签百分比必须在1到100之间");
    }

    public static void startCountersign(String nodeId, IDCRuntimeContext rtx, Form form, CountersignConfigDto config, OctoDomainOpObserver observer) throws Exception {
        if (form == null) throw new IllegalArgumentException("会签进入缺少表单数据");
        CountersignInstanceDto existing = readInstance(rtx, form, config);
        if (existing != null && nodeId.equals(existing.getNodeId())) return;
        writeInstance(rtx, form, config, createInstance(nodeId, config, observer));
    }

    public static CountersignInstanceDto readInstance(IDCRuntimeContext rtx, Form form, CountersignConfigDto config) throws Exception {
        String fieldName = getCountersignFieldName(config);
        Object value = FlowDesignerProcessFormUtil.getTotalFormAttrValue(rtx, form, fieldName);
        if (value == null || StrUtil.isBlank(String.valueOf(value))) return null;
        try {
            return JSONUtil.toBean(String.valueOf(value), CountersignInstanceDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("会签实例数据格式错误", e);
        }
    }

    public static void writeInstance(IDCRuntimeContext rtx, Form form, CountersignConfigDto config, CountersignInstanceDto instance) throws Exception {
        FlowDesignerProcessFormUtil.updateTotalFormAttrValue(rtx, form, getCountersignFieldName(config), JSONUtil.toJsonStr(instance));
    }

    public static CountersignInstanceDto createInstance(String nodeId, CountersignConfigDto config, OctoDomainOpObserver observer) throws Exception {
        List<User> users = UserRoleUtil.queryUserCodesByRoleCodes(observer, config.getRoleCodes());
        List<CountersignParticipantDto> participants = new ArrayList<>();
        if (CollUtil.isNotEmpty(users)) {
            for (User user : users) {
                if (StrUtil.isBlank(user.getCode())) continue;
                participants.add(new CountersignParticipantDto()
                        .setUserCode(user.getCode())
                        .setUserName(user.getFullName())
                        .setStatus(STATUS_PENDING)
                        .setAction("")
                        .setOpinion("")
                        .setTime(""));
            }
        }
        String now = nowText();
        return new CountersignInstanceDto()
                .setNodeId(nodeId)
                .setField(config.getField())
                .setApproval(config.getApproval())
                .setStatus(STATUS_PENDING)
                .setResult("")
                .setParticipants(participants)
                .setUserActions(config.getUserActions())
                .setRoutes(config.getRoutes())
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setVersion(1);
    }

    public static RouterOption leaveCountersign(IDCRuntimeContext rtx, String nodeId, Form form, CountersignConfigDto config, String actionName, String operatorCode, String opinion) throws Exception {
        if (rtx == null) throw new IllegalArgumentException("会签离开缺少运行上下文");
        if (form == null) throw new IllegalArgumentException("会签离开缺少表单数据");
        CountersignInstanceDto instance = readInstance(rtx, form, config);
        if (instance == null) throw new IllegalArgumentException("会签实例不存在，请先执行会签进入");
        if (!nodeId.equals(instance.getNodeId())) throw new IllegalArgumentException("会签实例节点与当前节点不一致：" + nodeId);
        if (STATUS_COMPLETED.equals(instance.getStatus())) return routeToResult(rtx, instance);
        String userAction = resolveUserAction(config, actionName);
        CountersignParticipantDto participant = resolveParticipant(instance, operatorCode);
        if (STATUS_COMPLETED.equals(participant.getStatus())) throw new IllegalStateException("当前用户已提交会签意见");
        participant.setStatus(STATUS_COMPLETED)
                .setAction(userAction)
                .setOpinion(defaultIfBlank(opinion, ""))
                .setTime(nowText());
        evaluate(instance);
        instance.setUpdatedAt(nowText()).setVersion(instance.getVersion() == null ? 1 : instance.getVersion() + 1);
        writeInstance(rtx, form, config, instance);
        if (!STATUS_COMPLETED.equals(instance.getStatus())) return new RouterOption().setGoNextAll(false);
        return routeToResult(rtx, instance);
    }

    public static String resolveUserAction(CountersignConfigDto config, String actionName) {
        if (StrUtil.isBlank(actionName)) throw new IllegalArgumentException("会签离开缺少当前动作名称");
        if (actionName.equals(config.getUserActions().get(ACTION_AGREE))) return ACTION_AGREE;
        if (actionName.equals(config.getUserActions().get(ACTION_DISAGREE))) return ACTION_DISAGREE;
        throw new IllegalArgumentException("当前动作不是会签用户动作：" + actionName);
    }

    public static CountersignParticipantDto resolveParticipant(CountersignInstanceDto instance, String operatorCode) {
        if (StrUtil.isBlank(operatorCode)) throw new IllegalArgumentException("会签离开缺少当前用户编号");
        if (CollUtil.isEmpty(instance.getParticipants())) throw new IllegalArgumentException("会签实例没有参与人");
        for (CountersignParticipantDto participant : instance.getParticipants()) {
            if (operatorCode.equals(participant.getUserCode())) return participant;
        }
        throw new IllegalArgumentException("当前用户不是该会签节点的会签人");
    }

    public static void evaluate(CountersignInstanceDto instance) {
        int agreeCount = 0;
        int pendingCount = 0;
        if (CollUtil.isNotEmpty(instance.getParticipants())) {
            for (CountersignParticipantDto participant : instance.getParticipants()) {
                if (STATUS_COMPLETED.equals(participant.getStatus()) && ACTION_AGREE.equals(participant.getAction())) agreeCount++;
                if (!STATUS_COMPLETED.equals(participant.getStatus())) pendingCount++;
            }
        }
        int quorum = resolveRequiredAgreeCount(instance);
        if (agreeCount >= quorum) {
            instance.setStatus(STATUS_COMPLETED).setResult(RESULT_PASS);
            return;
        }
        if (agreeCount + pendingCount < quorum) {
            instance.setStatus(STATUS_COMPLETED).setResult(RESULT_REJECT);
            return;
        }
        instance.setStatus(STATUS_PENDING).setResult("");
    }

    public static int resolveRequiredAgreeCount(CountersignInstanceDto instance) {
        CountersignApprovalDto approval = instance.getApproval();
        String type = approval == null ? APPROVAL_COUNT : defaultIfBlank(approval.getType(), APPROVAL_COUNT);
        int value = approval == null || approval.getValue() == null ? 1 : approval.getValue();
        if (APPROVAL_PERCENT.equals(type)) {
            int total = CollUtil.isEmpty(instance.getParticipants()) ? 0 : instance.getParticipants().size();
            return Math.max(1, (int) Math.ceil(total * value / 100.0D));
        }
        return Math.max(1, value);
    }

    public static RouterOption routeToResult(IDCRuntimeContext rtx, CountersignInstanceDto instance) throws Exception {
        String nextNodeTitle = resolveResultNextNodeTitle(instance);
        PDFInstance pdfInstance = rtx.getPdfInstance();
        Map<String, RefPDCNode> nodeMap = pdfInstance.getNodes().stream()
                .collect(Collectors.toMap(RefPDCNode::getName, node -> node));
        if (!nodeMap.containsKey(nextNodeTitle)) throw new IllegalArgumentException("流程定义配置错误：节点[" + nextNodeTitle + "]未定义！");
        return new RouterOption().setNexts(Collections.singleton(nodeMap.get(nextNodeTitle).getKey()));
    }

    public static String resolveResultNextNodeTitle(CountersignInstanceDto instance) {
        String result = instance.getResult();
        if (StrUtil.isBlank(result)) throw new IllegalStateException("会签尚未完成，不能计算结果路由");
        if (CollUtil.isNotEmpty(instance.getRoutes()) && StrUtil.isNotBlank(instance.getRoutes().get(result))) return instance.getRoutes().get(result);
        throw new IllegalArgumentException("会签结果缺少目标节点配置：" + result);
    }

    public static String getCountersignFieldName(CountersignConfigDto config) {
        return config.getField();
    }

    private static String nowText() {
        return LocalDateTime.now().toString();
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return StrUtil.isBlank(value) ? defaultValue : value;
    }
}
