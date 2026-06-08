package cell.octo.cm.rule;

import bap.cells.Cells;
import cell.CellIntf;
import cell.fe.octocm.expr.DefaultExpr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.octocm.domain.service.IDomainService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.dc.basic.param.view.BaseFeActionParameter;
import gpf.dto.cfg.runtime.RouterOption;
import octo.cm.dto.flowDesigner.rule.CountersignConfigDto;
import octo.cm.util.flowDesigner.FlowDesignerCountersignUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;
import web.dto.Pair;

import java.util.ArrayList;
import java.util.Set;

@Comment("流程设计器会签规则函数")
@ClassDeclare(
        label = "流程设计器会签规则函数",
        what = "处理流程设计器会签节点进入和离开路由",
        why = "支撑会签节点的实例初始化、意见记录、完成判定和结果路由",
        how = "在流程设计器发布的规则中使用 会签进入(nodeId, configJson) 和 会签离开(nodeId, configJson)",
        developer = "裴硕", version = "1.0",
        createTime = "2026-05-14", updateTime = "2026-05-14"
)
public interface FlowDesignerCountersignRule extends CellIntf {

    @MethodDeclare(
            label = "会签进入",
            what = "进入会签节点时创建或复用会签实例",
            why = "避免重复初始化会签实例，并将会签参与人与配置写入会签信息字段",
            how = "挂载到会签节点进入规则，由前端发布 会签进入(nodeId, configJson)",
            inputs = {
                    @InputDeclare(desc = "GPF运行上下文", name = "rtx", label = "GPF运行上下文", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(desc = "规则命名空间", name = "ruleNamespace", label = "规则命名空间", exampleValue = "$ruleNamespace$"),
                    @InputDeclare(desc = "表单数据", name = "form", label = "表单数据", exampleValue = "$form$"),
                    @InputDeclare(desc = "会签节点ID", name = "nodeId", label = "会签节点ID"),
                    @InputDeclare(desc = "会签配置JSON", name = "configJson", label = "会签配置JSON")
            }
    )
    default Pair<Boolean, String> countersignEnter(IDCRuntimeContext rtx, Set<String> ruleNamespace, Form form, String nodeId, String configJson) throws Exception {
        try {
            CountersignConfigDto config = FlowDesignerCountersignUtil.parseConfig(configJson);
            validateNodeId(nodeId, config);
            FlowDesignerCountersignUtil.startCountersign(nodeId, rtx, form, config, buildObserver(ruleNamespace));

            return new Pair<>(true, "");
        } catch (Exception e) {

            System.out.println("会签进入异常：" + e.getMessage());
            System.out.println(ExceptionUtils.getFullStackTrace(e));
            return new Pair<>(true, "");

        }
    }

    @MethodDeclare(
            label = "会签离开",
            what = "会签节点离开时记录当前用户意见并计算路由",
            why = "会签未完成时阻止流转，会签完成时跳转到会签通过或会签不通过对应节点",
            how = "挂载到会签节点离开路由规则，由前端发布 会签离开(nodeId, configJson)",
            inputs = {
                    @InputDeclare(desc = "GPF运行上下文", name = "rtx", label = "GPF运行上下文", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(desc = "规则命名空间", name = "ruleNamespace", label = "规则命名空间", exampleValue = "$ruleNamespace$"),
                    @InputDeclare(desc = "动作参数", name = "input", label = "动作参数", exampleValue = "$ActionParameter$"),
                    @InputDeclare(desc = "表单数据", name = "form", label = "表单数据", exampleValue = "$form$"),
                    @InputDeclare(desc = "当前操作人编号", name = "operatorCode", label = "当前操作人编号", exampleValue = "$operatorCode$"),
                    @InputDeclare(desc = "会签节点ID", name = "nodeId", label = "会签节点ID"),
                    @InputDeclare(desc = "会签配置JSON", name = "configJson", label = "会签配置JSON")
            }
    )
    default RouterOption countersignLeave(IDCRuntimeContext rtx, Set<String> ruleNamespace, BaseFeActionParameter input, Form form, String operatorCode, String nodeId, String configJson) throws Exception {
        CountersignConfigDto config = FlowDesignerCountersignUtil.parseConfig(configJson);
        validateNodeId(nodeId, config);
        String actionName = (String) input.getRtx().getParam("$actionName");
        return FlowDesignerCountersignUtil.leaveCountersign(rtx, nodeId, form, config, actionName, operatorCode, "");
    }


    @MethodDeclare(
            label = "保存表单_指定流程动作", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(desc = "GPF运行上下文", name = "rtx", label = "GPF运行上下文", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),
                    @InputDeclare(name = "form", label = "form", desc = "", exampleValue = "$form$"),
                    @InputDeclare(name = "actionName", label = "actionName", desc = "")
            }
    )
    default Form saveFormAndAssignActionName(BaseFeActionParameter input, Form form, String actionName) throws Exception {

        input.getRtx().setParam("$actionName", input.getRtx().getActionName());
        input.getRtx().setActionName(actionName);
        return Cells.get(DefaultExpr.class).saveForm(input, form);

    }


    default void validateNodeId(String nodeId, CountersignConfigDto config) {
        if (StrUtil.isBlank(nodeId)) throw new IllegalArgumentException("会签节点ID不能为空");
    }

    default OctoDomainOpObserver buildObserver(Set<String> ruleNamespace) throws Exception {
        if (ruleNamespace == null || ruleNamespace.size() < 2)
            throw new IllegalArgumentException("会签规则缺少业务域命名空间");
        String busDomainCode = new ArrayList<>(ruleNamespace).get(1);
        if (StrUtil.isBlank(busDomainCode)) throw new IllegalArgumentException("会签规则无法识别业务域编号");
        DomainDto domainDto = IDomainService.get().getDomainByCode(busDomainCode);
        if (domainDto == null) throw new IllegalArgumentException("业务域不存在：" + busDomainCode);
        return new OctoDomainOpObserver(domainDto);
    }
}
