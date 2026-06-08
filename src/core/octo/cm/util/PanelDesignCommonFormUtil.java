package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import octo.cm.dto.panelDesign.PanelButtonActionDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.util.*;
import java.util.stream.Collectors;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计常用Form工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-30", updateTime = "2025-06-30"
)
public class PanelDesignCommonFormUtil {

    // 简单操作
    public static final EasyOperation Op = EasyOperation.get();
    public static final String DefaultPermissionType = "默认";


    // 添加默认的面板按钮
    // 1、从面板-按钮中读取默认的按钮
    // 2、根据规则，看下应该分配到表格还是表单
    public static TableData appendDefaultPanelButtonTd(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {

        // 业务域的过滤
        SqlExpression domainFilterExpr = Op.getBusDomainFilterExpr(observer, FormModelId_Axis_Button);

        // 没有系统按钮就不处理了
        ResultSet<Form> systemBtnRs = Op.queryFormPageByCondition(dao, FormModelId_Axis_Button,
                FieldName_CategoryLabel, Text_SystemDefaultButton, cnd -> {
                    // 加上业务域的过滤
                    if (domainFilterExpr != null) {
                        cnd.where().and(domainFilterExpr);
                    }
                    return cnd;
                });
        if (systemBtnRs.isEmpty()) {
            Op.logf("面板[{}]未添加默认的面板按钮，因为未找到系统预制的按钮", panelDesignForm.getString(Form.Code));
            return null;
        }

        // 系统默认的按钮
        List<Form> defaultBtnForms = systemBtnRs.getDataList();

        defaultBtnForms.add(
                getOrCreateDefaultCreateButton(dao, observer, panelDesignForm)
        );

        // 新的按钮Td
        TableData btnTd = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);

        for (Form systemBtnForm : defaultBtnForms) {

            String btnAlias = systemBtnForm.getString("别名");
            String btnDesc = systemBtnForm.getString("按钮说明");

            Form newBtnForm = Op.newForm(btnTd.getFormModelId());
            newBtnForm.setAttrValue("按钮别名", btnAlias);
            newBtnForm.setAttrValue("按钮说明", btnDesc);
            newBtnForm.setAttrValue("面板按钮", Op.toAssociationData(systemBtnForm));
            btnTd.add(newBtnForm);

        }

        return btnTd;
    }


    // 通过按钮名称，创建按钮实现Form
    public static Form createBtnImpl(IDao dao, OctoDomainOpObserver observer, String masterUuid, String btnName, String btnAlias, List<PanelButtonActionDto> btnAction, String btnDesc, String btnCategory) throws Exception {
        if (StrUtil.hasBlank(btnName) || Op.isEmpty(btnAction)) return null;

        Form btnImplForm = Op.newForm(FormModelId_Axis_Button);
        btnImplForm.setAttrValue(Form.Owner, masterUuid);
        btnImplForm.setAttrValue("按钮编号", generateCode(dao, observer, FormModelId_Axis_Button, "BUT", 5));
        btnImplForm.setAttrValue("按钮名称", btnName);
        btnImplForm.setAttrValue("别名", btnAlias);
        btnImplForm.setAttrValue("分类标签", btnCategory);
        btnImplForm.setAttrValue("按钮说明", btnDesc);


        TableData btnActionTd = new TableData(FormModelId_PanelDesign_Action_Orchestration);
        for (PanelButtonActionDto actionDto : btnAction) {
            Form actionForm = new Form(btnActionTd.getFormModelId());
            actionForm.setAttrValue("操作函数", actionDto.getOperateFunction());
            actionForm.setAttrValue("操作说明", actionDto.getOperateDescription());
            btnActionTd.add(actionForm);

        }

        btnImplForm.setAttrValue("按钮动作", btnActionTd);


        btnImplForm = IFormMgr.get().createForm(null, dao, btnImplForm, observer);
        return btnImplForm;

    }


    // 通过场景属性名称和场景属性样式，创建属性实现Form
    public static Form getOrCreateAttrImplOnlyText(IDao dao, OctoDomainOpObserver observer, String parentUuid, String sceneAttrName, String sceneAttrStyle) throws Exception {
        Form attrImplForm = queryFormByAssignField(observer, FormModelId_Axis_Data, "属性名称", sceneAttrName);
        if (attrImplForm != null) return attrImplForm;

        return createAttrImplOnlyText(dao, observer, parentUuid, sceneAttrName, sceneAttrStyle);

    }

    // 通过场景属性名称和场景属性样式，创建属性实现Form（尊重传入样式）
    public static Form getOrCreateAttrImpl(IDao dao, OctoDomainOpObserver observer, String parentUuid, String sceneAttrName, String sceneAttrStyle) throws Exception {
        Form attrImplForm = queryFormByAssignField(observer, FormModelId_Axis_Data, "属性名称", sceneAttrName);
        if (attrImplForm != null) return attrImplForm;

        return createAttrImpl(dao, observer, parentUuid, sceneAttrName, sceneAttrStyle);
    }

    // 创建属性实现
    public static Form createAttrImplOnlyText(IDao dao, OctoDomainOpObserver observer, String parentUuid, String sceneAttrName, String sceneAttrStyle) throws Exception {

        Form attrImplForm = Op.newForm(FormModelId_Axis_Data);
        attrImplForm.setAttrValue("属性编号", generateCode(dao, observer, FormModelId_Axis_Data, "FIE", 5));
        // FIXME 这里要把属性名称标准化
        attrImplForm.setAttrValue("属性名称", standardAttrName(sceneAttrName));
        attrImplForm.setAttrValue("属性样式", "文本");
        if (StrUtil.isNotBlank(parentUuid)) {
            attrImplForm.setAttrValue(Form.Owner, parentUuid);
        }
        return IFormMgr.get().createForm(null, dao, attrImplForm, observer);

    }


    // 创建默认的面板权限方案
    public static Form createPanelPermission(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, Form roleForm,
                                             Map<String, Set<String>> nodeOperationMapping, List<String> fieldNames, List<String> btnNames) throws Exception {

        // 晓斌底下提供了一个“默认”的节点，即在没有权限的默认情况下提供一组对按钮的权限
        // 这个设计很诡异，后续可能会调整
        appendDefaultPermission(dao, observer, panelDesignForm, nodeOperationMapping);

        // 节点名称
        List<String> nodeNames = new ArrayList<>(nodeOperationMapping.keySet());

        Form permissionImpl = Op.newForm(FormModelId_Axis_Permission);
        permissionImpl.setAttrValue(Form.Owner, panelDesignForm.getUuid());
        String permissionCode = generateCode(dao, observer, FormModelId_PanelDesign_Status, "PRI", 5);
        permissionImpl.setAttrValue("权限编号", permissionCode);
        permissionImpl.setAttrValue("权限名称",
                StrUtil.format("{}_面板权限", roleForm.getString("角色名称"))
        );
        permissionImpl.setAttrValue("权限角色", CollUtil.newArrayList(Op.toAssociationData(roleForm)));


        permissionImpl.setAttrValue("数据权限", generateSimpleDataPermissionTd(fieldNames, nodeNames));
        permissionImpl.setAttrValue("操作权限", generateSimpleBtnPermissionTd(btnNames, nodeOperationMapping));


        return IFormMgr.get().createForm(null, dao, permissionImpl, observer);


    }


    // 创建默认表格行单击事件
    // 添加业务域过滤机制
    public static Form createPanelStatus(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm,
                                         List<String> panelStatus) throws Exception {
        if (panelDesignForm == null || Op.isEmpty(panelStatus)) return null;

        String panelName = panelDesignForm.getString("面板名称");

        Form statusForm = Op.newForm(FormModelId_PanelDesign_Status);
        statusForm.setAttrValue(Form.Owner, panelDesignForm.getUuid());
        String statusCode = generateCode(dao, observer, FormModelId_PanelDesign_Status, "STU", 5);
        statusForm.setAttrValue("状态名称", buildPanelStatusName(panelName));
        statusForm.setAttrValue("状态编号", statusCode);
        TableData valueTd = new TableData(SlaveFormModelId_PanelDesign_Status_value);
        panelStatus = panelStatus.stream().distinct().collect(Collectors.toList());
        for (String status : panelStatus) {
            valueTd.add(
                    Op.newForm(valueTd.getFormModelId()).setAttrValue("状态", status)
            );
        }
        statusForm.setAttrValue("状态值", valueTd);
        return IFormMgr.get().createForm(null, dao, statusForm, observer);


    }


    // 创建默认表格行单击事件
    public static Form createDefaultTableRowClickEvent(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {

        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");

        String formName = buildPanelFormName(panelName);

        String fullViewInstCode = ApplicationUtil.buildFullViewInstCode(observer.getDomainCode(),
                panelCode, null,
                formName
        );
        String opFunctionExpr = StrUtil.format("修改表单('{}')", fullViewInstCode);

        return createQuicklySimpleEvent(dao, observer, panelDesignForm, "表格行点击",
                "点击表格行时，弹出的页面", "行点击", opFunctionExpr);

    }

    // 快速创建事件
    // FIXME 这里没有考虑重复的情况
    public static Form createQuicklySimpleEvent(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, String eventName, String eventDesc, String monitorType, String opFunctionExpr) throws Exception {


        String panelCode = panelDesignForm.getString(Form.Code);
        String panelName = panelDesignForm.getString("面板名称");

        // 最终的事件名称
        String finalEvetName = StrUtil.format("{}_{}", panelName, eventName);


        // 删除老的数据
        removeOldFormByOwnerAndField(dao, FormModelId_PanelDesign_Event,
                panelDesignForm.getUuid(), "事件名称", finalEvetName);


        Form eventForm = Op.newForm(FormModelId_PanelDesign_Event);
        eventForm.setAttrValue(Form.Owner, panelDesignForm.getUuid());

        String eventCode = generateCode(dao, observer, FormModelId_PanelDesign_Event, "EVT", 5);

        eventForm.setAttrValue(Form.Code, IdUtil.fastSimpleUUID());
        eventForm.setAttrValue("事件编号", eventCode);
        eventForm.setAttrValue("事件名称", finalEvetName);
        eventForm.setAttrValue("事件说明", eventDesc);
        eventForm.setAttrValue("监听类型", monitorType);

        TableData eventActionTd = new TableData(FormModelId_PanelDesign_Action_Orchestration);
        Form eventActionForm = Op.newForm(eventActionTd.getFormModelId());
        eventActionForm.setAttrValue("操作函数", opFunctionExpr);
        eventActionTd.add(eventActionForm);

        eventForm.setAttrValue("事件动作", eventActionTd);


        return IFormMgr.get().createForm(null, dao, eventForm, observer);


    }


    // 将创建的事件更新到【面板事件】
    public static void addPanelEvent(Form panelDesignForm, Form eventForm) throws Exception {
        if (panelDesignForm == null || eventForm == null) return;
        TableData eventTd = panelDesignForm.getTable("面板事件");
        if (eventTd == null) eventTd = new TableData(SlaveFormModelId_PanelDesign_Constraint_Event);

        Form slaveEventForm = Op.newForm(eventTd.getFormModelId());
        slaveEventForm.setAttrValue("事件实现", Op.toAssociationData(eventForm));

        eventTd.add(slaveEventForm);
        panelDesignForm.setAttrValue("面板事件", eventTd);

    }

    // 创建默认新增按钮
    // 流程处理类型的面板设计和其他类型的不同
    public static Form getOrCreateDefaultCreateButton(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");

        String createButtonName = StrUtil.format("{}_新增", panelName);

        Form oldBtnForm = getDefaultCreateButton(dao, panelDesignForm);
        if (oldBtnForm != null) {
            IFormMgr.get().deleteForm(dao, oldBtnForm.getFormModelId(), oldBtnForm.getUuid());
        }

        // 2、确定不存在，那么就重新创建

        // 新增无非是拿到表单的名称
        String formName = buildPanelFormName(panelName);
        String fullViewInstCode = ApplicationUtil.buildFullViewInstCode(observer.getDomainCode(),
                panelCode, null,
                formName
        );


        // 不同类型有不同的操作函数
        String opFunctionExpr = null;
        if (PanelCategoryUtil.isProcessHandleCategory(panelDesignForm)) {
            opFunctionExpr = buildDefaultCreateButtonOpFunctionExpr(dao, panelDesignForm, fullViewInstCode);
        }

        // 如果前面的没有处理，那么给与一个默认的回退
        if (StrUtil.isBlank(opFunctionExpr)) {
            opFunctionExpr = StrUtil.format("新增表单('{}')", fullViewInstCode);
        }

        return createBtnImpl(
                dao,
                observer,
                panelDesignForm.getUuid(),
                createButtonName,
                "新增",
                CollUtil.newArrayList(
                        new PanelButtonActionDto()
                                .setOperateFunction(opFunctionExpr)
                ),
                "",
                "新增"
        );

    }


    // 获取默认新增按钮
    public static Form getDefaultCreateButton(IDao dao, Form panelDesignForm) throws Exception {
        String panelName = panelDesignForm.getString("面板名称");
        String createButtonName = StrUtil.format("{}_新增", panelName);

        // 1、首先检索是不是已经存在了，如果存在返回就好
        ResultSet<Form> ownBtnRs = Op.queryFormPageByCondition(dao, FormModelId_Axis_Button, Form.Owner,
                panelDesignForm.getUuid(), cnd -> {
                    cnd.where().andEquals(Op.getFieldCode("按钮名称"), createButtonName);
                    return cnd;
                });
        if (ownBtnRs.isEmpty()) return null;

        return ownBtnRs.getDataList().get(0);

    }


    // 创建属性实现
    public static Form createAttrImpl(IDao dao, OctoDomainOpObserver observer, String parentUuid, String sceneAttrName, String sceneAttrStyle) throws Exception {

        Form attrImplForm = Op.newForm(FormModelId_Axis_Data);
        attrImplForm.setAttrValue("属性编号", generateCode(dao, observer, FormModelId_Axis_Data, "FIE", 5));
        // FIXME 这里要把属性名称标准化
        attrImplForm.setAttrValue("属性名称", standardAttrName(sceneAttrName));
        attrImplForm.setAttrValue("属性样式", standardAttrStyle(observer, sceneAttrStyle));
        if (StrUtil.isNotBlank(parentUuid)) {
            attrImplForm.setAttrValue(Form.Owner, parentUuid);
        }
        return IFormMgr.get().createForm(null, dao, attrImplForm, observer);

    }


    // ========================= 名称规则 / 操作函数规则 =========================

    // 构建【面板状态】名称
    public static String buildPanelStatusName(String panelName) {
        return StrUtil.format("{}状态", panelName);
    }

    // 构建【面板表格】名称
    public static String buildPanelTableName(String panelName) {
        return panelName + "_表格";
    }

    // 构建【面板表单】名称
    public static String buildPanelFormName(String panelName) {
        return StrUtil.format("{}_表单", panelName);
    }


    // 构建【面板-新增按钮】的操作函数
    private static String buildDefaultCreateButtonOpFunctionExpr(IDao dao, Form panelDesignForm,
                                                                 String defaultFormViewInstCode) throws Exception {
        if (panelDesignForm == null || StrUtil.isBlank(defaultFormViewInstCode)) return null;
        String panelName = panelDesignForm.getString("面板名称");

        Form panelStateForm = Op.queryFormByAc(dao, panelDesignForm.getAssociation("面板状态"));
        if (panelStateForm == null) return null;
        String panelStateName = panelStateForm.getString("状态名称");
        if (StrUtil.isBlank(panelStateName)) return null;

        TableData stateValTd = panelStateForm.getTable("状态值");
        if (Op.isEmpty(stateValTd)) return null;

        String firstStateVal = stateValTd.getRows().get(0).getString("状态");
        if (StrUtil.isBlank(firstStateVal)) return null;


        return StrUtil.format("新增表单_设初始值('{}', '{}:{}')",
                defaultFormViewInstCode,
                panelStateName,
                firstStateVal
        );
    }

    // ========================= 支撑方法 =========================

    // 补充默认权限逻辑
    private static void appendDefaultPermission(IDao dao, OctoDomainOpObserver observer,
                                                Form panelDesignForm, Map<String, Set<String>> nodeOperationMapping) throws Exception {

        if (panelDesignForm == null || Op.isEmpty(nodeOperationMapping)) return;
        Set<String> defaultBtnNames = new HashSet<>();
        nodeOperationMapping.put(DefaultPermissionType, defaultBtnNames);


    }


    // 删除老的数据，使用OwnerUUid + 指定字段的名称和值
    public static void removeOldFormByOwnerAndField(IDao dao, String formModelId, String ownerUuid, String fieldName, String fieldValue) throws Exception {

        Cnd oldDataQueryCdn = Cnd.NEW();
        oldDataQueryCdn.where()
                .andEquals(Form.Owner, ownerUuid)
                .andEquals(Op.getFieldCode(fieldName), fieldValue);

        IFormMgr.get().deleteForm(dao, formModelId, oldDataQueryCdn);

    }

    // 清除重复的按钮实现
    public static TableData cleanDuplicatePanelButtonImpl(TableData btnTd) throws Exception {
        if (Op.isEmpty(btnTd)) return btnTd;
        Set<String> existedBtnImplCodeSet = new HashSet<>();
        TableData newTableData = new TableData(btnTd.getFormModelId());

        for (Form row : btnTd.getRows()) {
            AssociationData btnAc = row.getAssociation("面板按钮");
            if (btnAc == null || existedBtnImplCodeSet.contains(btnAc.getValue())) continue;
            existedBtnImplCodeSet.add(btnAc.getValue());
            newTableData.add(row);
        }


        return newTableData;


    }

    // 创建[权限实现-操作权限]样例
    private static TableData generateSimpleBtnPermissionTd(List<String> btnNames, Map<String, Set<String>> nodeOperationMapping) throws Exception {
        TableData td = new TableData(FormModelId_Axis_Permission_Button);
        if (Op.isEmpty(btnNames) || nodeOperationMapping == null || nodeOperationMapping.isEmpty()) return td;

        for (String btnName : btnNames) {
            Form permissionForm = Op.newForm(td.getFormModelId());

            StringJoiner nodeNameJoiner = new StringJoiner(PermissionStatus_Delimiter);
            StringJoiner permissionJoiner = new StringJoiner(PermissionStatus_Delimiter);

            // 从映射表判断真实的权限是什么
            for (Map.Entry<String, Set<String>> nodeOperationEntry : nodeOperationMapping.entrySet()) {
                String nodeName = nodeOperationEntry.getKey();
                Set<String> availableOperations = nodeOperationEntry.getValue();
                if (StrUtil.isBlank(nodeName)) continue;

                nodeNameJoiner.add(nodeName);

                // 默认节点拥有所有权限
                boolean isDefaultBtnName = DefaultPermissionType.equals(nodeName);
                // 拥有当前操作的权限
                boolean hasOperationPerm = !Op.isEmpty(availableOperations) && availableOperations.contains(btnName);

                permissionJoiner.add(
                        isDefaultBtnName || hasOperationPerm ?
                                PermissionStatus_ReadAndExecute :
                                PermissionStatus_NoReadAndWriteAndExecute
                );
            }

            permissionForm.setAttrValue("操作", btnName);
            permissionForm.setAttrValue("状态", nodeNameJoiner.toString());

            permissionForm.setAttrValue("权限", permissionJoiner.toString());
            td.add(permissionForm);

        }
        return td;
    }

    // 创建[权限实现-数据权限]样例
    private static TableData generateSimpleDataPermissionTd(List<String> fieldNames, List<String> nodeNames) throws Exception {
        TableData td = new TableData(FormModelId_Axis_Permission_Data);
        if (Op.isEmpty(fieldNames) || Op.isEmpty(nodeNames)) {
            return td;
        }

        String nodeNamesStr = StrUtil.join(PermissionStatus_Delimiter, nodeNames);
        String allPermissionsStr = StrUtil.repeatAndJoin(PermissionStatus_ReadAndWrite, nodeNames.size(), PermissionStatus_Delimiter);
        for (String fieldName : fieldNames) {
            Form permissionForm = Op.newForm(td.getFormModelId());
            permissionForm.setAttrValue("属性", fieldName);
            permissionForm.setAttrValue("状态", nodeNamesStr);
            permissionForm.setAttrValue("权限", allPermissionsStr);
            td.add(permissionForm);

        }


        return td;
    }


    private static Set<String> AttrLegalTypeSet = new HashSet<>();
    private static Set<String> AttrNeedParamTypeSet = new HashSet<>();


    public static String standardAttrName(String attrName) {
        if (StrUtil.isBlank(attrName)) return attrName;
        attrName = attrName.replace("/", "").replace("\\", "");
        attrName = attrName.replace("&", "")
                .replace("-", "")
                .replace("+", "")
        ;
        if (attrName.length() > 16) {
            attrName = attrName.substring(0, 16);
        }
        return attrName;
    }

    public static String standardAttrStyle(OctoDomainOpObserver observer, String sceneAttrStyle) throws Exception {
        if (StrUtil.isBlank(sceneAttrStyle)) return "文本";
        if (CollUtil.isEmpty(AttrLegalTypeSet)) {
            initAttrLegalTypeConfig(observer);
        }

        // 如果默认的类型直接完全匹配，就是标准的类型
//        if (AttrLegalTypeSet.contains(sceneAttrStyle)) return sceneAttrStyle;

        // 以下是完全匹配这些必须要参数的，那意思就是，这个是不对的，因为没有参数
        if ("枚举".equals(sceneAttrStyle)) return "文本";
        if ("嵌套表格".equals(sceneAttrStyle)) return "文本";
        if ("下拉框预览".equals(sceneAttrStyle)) return "文本";
        if ("表格".equals(sceneAttrStyle)) return "文本";
        if ("下拉框".equals(sceneAttrStyle)) return "文本";
        if ("多选下拉框".equals(sceneAttrStyle)) return "文本";

        // 那么下面就都是填写参数了的
        for (String typeStr : AttrLegalTypeSet) {
            if (sceneAttrStyle.contains(typeStr)) {
                return sceneAttrStyle;
            }
        }

        return sceneAttrStyle;
    }

    public static void initAttrLegalTypeConfig(OctoDomainOpObserver observer) throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Cnd.NEW();
            if (observer != null) {
                // 业务域的过滤
                SqlExpression domainFilterExpr = Op.getBusDomainFilterExpr(observer, FormModelId_Axis_Button);
                if (domainFilterExpr != null) {
                    cnd.where().and(domainFilterExpr);
                }
            }
            ResultSet<Form> rs = IFormMgr.get().queryFormPage(dao, FormModelId_FieldStyle, null, 1, Integer.MAX_VALUE, true, true);
            for (Form row : rs.getDataList()) {
                String code = row.getString(Form.Code);
                if (StrUtil.isBlank(code)) continue;
                AttrLegalTypeSet.add(code);
                String config = row.getString("扩展配置");
                if (StrUtil.isNotBlank(config)) {
                    AttrNeedParamTypeSet.add(code);
                }
            }

        }


    }

    // 查询-匹配任意字段
    public static Form queryFormByAssignField(OctoDomainOpObserver observer, String formModel, String fieldName, String fieldValue) throws Exception {
        return queryFormsByAssignField(observer, formModel, fieldName, fieldValue).stream()
                .findFirst()
                .orElse(null);
    }

    // 查询-匹配指定字段
    public static List<Form> queryFormsByAssignField(OctoDomainOpObserver observer, String formModel, String fieldName, String fieldValue) throws Exception {


        List<Form> resultList = new ArrayList<>();
        if (StrUtil.hasBlank(formModel, fieldName, fieldValue)) return resultList;
        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Cnd.NEW();
            cnd.where().andEquals(IFormMgr.get().getFieldCode(fieldName), fieldValue);
            if (observer != null) {
                // 业务域的过滤
                SqlExpression domainFilterExpr = Op.getBusDomainFilterExpr(observer, formModel);
                if (domainFilterExpr != null) {
                    cnd.where().and(domainFilterExpr);
                }
            }

            ResultSet<Form> formRs = IFormMgr.get().queryFormPage(dao, formModel, cnd, 1, Integer.MAX_VALUE, true, true);
            if (formRs.isEmpty()) return resultList;
            return formRs.getDataList();
        }
    }

    // 生成编号
    public static String generateCode(IDao dao, OctoDomainOpObserver observer, String formModelId, String prefix, int length) throws Exception {

        try {
            Map<String, String[]> codeConfigMapping = new HashMap<>();
            codeConfigMapping.put("IML", new String[]{FormModelId_PanelDesign, "面板编号"});
            codeConfigMapping.put("BTN", new String[]{FormModelId_Axis_Button, "按钮编号"});
            codeConfigMapping.put("BUT", new String[]{FormModelId_Axis_Button, "按钮编号"});
            codeConfigMapping.put("FIE", new String[]{FormModelId_Axis_Data, "属性编号"});
            codeConfigMapping.put("PRI", new String[]{FormModelId_Axis_Permission, "权限编号"});
            codeConfigMapping.put("STU", new String[]{FormModelId_PanelDesign_Status, "状态编号"});
            codeConfigMapping.put("EVT", new String[]{FormModelId_PanelDesign_Event, "事件编号"});

            String[] codeConfig = codeConfigMapping.get(prefix);
            if (codeConfig == null) {
                return StrUtil.format("{}_{}", prefix, IdUtil.fastSimpleUUID());
            }
            String queryFormModelId = codeConfig[0];
            String codeFieldName = codeConfig[1];

            String codeFieldCode = Op.getFieldCode(codeFieldName);
            String codePrefix = prefix + "_";
            long maxSeq = 0;

            Cnd queryCodeCnd = ApplicationUtil.Op.getBusDomainFilterCondition(observer, queryFormModelId);
            if (queryCodeCnd == null) {
                queryCodeCnd = Cnd.NEW();
            }
            queryCodeCnd.where().and(Cnd.exps(codeFieldCode, "like", codePrefix + "%"));
            queryCodeCnd.orderBy(codeFieldCode, "desc");

            ResultSet<Form> formRs = IFormMgr.get().queryFormPage(dao, queryFormModelId, queryCodeCnd,
                    1, Integer.MAX_VALUE, false, false, codeFieldCode);
            if (!formRs.isEmpty()) {
                for (Form form : formRs.getDataList()) {
                    String code = form.getString(codeFieldName);
                    if (StrUtil.isBlank(code) || !code.startsWith(codePrefix)) continue;

                    String seqStr = code.substring(codePrefix.length());
                    if (!seqStr.matches("\\d+")) continue;

                    long seq = Long.parseLong(seqStr);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                }
            }
            return StrUtil.format("{}_{}", prefix, StrUtil.fillBefore(String.valueOf(maxSeq + 1), '0', length));
        } catch (Exception e) {
            return StrUtil.format("{}_{}", prefix, IdUtil.fastSimpleUUID());
        }
    }

}
