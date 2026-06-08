package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.fe.progress.CFeProgressCtrlWithTextArea;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.adur.user.IUserMgr;
import cell.jit.IFlowCombineService;
import cell.jit.IWorkSpaceService;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.leavay.dfc.gui.LvUtil;
import fe.cmn.app.ability.PopToast;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import gpf.dc.basic.excel.ConvertContext;
import jit.dto.WorkSpace;
import jit.dto.WorkSpaceLink;
import jit.dto.flowcombine.FlowCombineDto;
import jit.observer.JITFormOpObserver;
import octo.cm.constant.WorkBenchConst;
import org.nutz.dao.entity.annotation.Comment;
import pcr.basic.util.PCRUtils;

import java.util.HashSet;
import java.util.Set;

@Comment("面板设计编排工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-26", updateTime = "2025-06-26"
)
public class PanelDesignPdfUtil {
    // 业务流图
    public static final String JIT_PROCESS_FORM_ID_BUSINESS_FLOW = "gpf.md.jit.BusinessFlow";
    // 数据模型
    public static final String JIT_PROCESS_FORM_ID_DATA_MODEL = "gpf.md.jit.DataModel";
    // 动作定义
    public static final String JIT_PROCESS_FORM_ID_ACTION_DEFINE = "gpf.md.jit.ActionDefine";
    // 权限矩阵
    public static final String JIT_PROCESS_FORM_ID_PRIVILEGE_MATRIX_DO = "gpf.md.basic.PrivilegeMatrixDo";
    // 流程组装
    public static final String JIT_PROCESS_FORM_ID_FLOW_COMBINE = "gpf.md.jit.FlowCombine";

    public static final String 数据模型_业务流图_流程节点 = "gpf.md.slave.jit.FlowNode";
    public static final String 数据模型_业务流图_流程连线 = "gpf.md.slave.jit.FlowLink";
    public static final String 数据模型_业务流图_数据模型清单 = "gpf.md.slave.jit.DataModelList";
    public static final String 数据模型_业务流图_数据项标准 = "gpf.md.slave.jit.DataItemStrandard";
    public static final String 数据模型_业务流图_数据项配置 = "gpf.md.slave.jit.DataItemSetting";
    public static final String 数据模型_权限矩阵_权限配置 = "gpf.md.slave.PrivilegeMatrixDo";
    public static final String 数据模型_权限矩阵_权限配置_数据权限方案 = "gpf.md.basic.DataPrivilegeSolutionDo";
    public static final String 数据模型_权限矩阵_权限配置_数据权限方案_数据项权限配置 = "gpf.md.slave.DataItemPrivilegeSettingDo";
    public static final String 数据模型_权限矩阵_权限配置_动作权限方案 = "gpf.md.basic.ActionPrivilegeSolutionDo";
    public static final String 数据模型_权限矩阵_权限配置_动作权限方案_动作项权限配置 = "gpf.md.slave.ActionItemPrivilegeSettingDo";

    public static final String 数据模型_权限规则 = "gpf.md.basic.PrivilegeRuleDo";
    public static final AssociationData 权限规则_R = new AssociationData(数据模型_权限规则, "R");
    public static final AssociationData 权限规则_W = new AssociationData(数据模型_权限规则, "W");
    public static final AssociationData 权限规则_X = new AssociationData(数据模型_权限规则, "X");
    public static final AssociationData 角色_发起人 = new AssociationData("com.cdao.model.CDoRole", "发起人");
    public static final AssociationData 用户匹配规则_发起人 = new AssociationData("gpf.md.basic.UserMatchRuleDo", "发起人");

    public static final String 流程模型编号模板 = "gpf.md.process.cmstudio.{}";
    public static final String 面板设计工作空间编号 = "CMStudio";

    // 创建流程
    // 核心方法
    public static Object createPdf(CFeProgressCtrlWithTextArea progress, PanelContext panelContext, Form dcInstance, String viewFormModel, boolean useLlmGeneratedData) throws Exception {


        try (IDao dao = IDaoService.newIDao()) {


            dcInstance = IFormMgr.get().queryForm(dao, WorkBenchConst.FormModelId_PanelDesign, dcInstance.getUuid());

            String panelName = dcInstance.getString("面板名称");
            TableData dataTd = dcInstance.getTable("面板数据");
            TableData btnTd = dcInstance.getTable("面板按钮");
            TableData busOrchTd = dcInstance.getTable("业务编排");

            // 使用场景中LLM生成的编排模型
            if (useLlmGeneratedData) {
                // FIXME 尝试继续初始化
//                busOrchTd = new TakeEffectProcessPanelDesign().autoFillInBusOrchtrationTd(dcInstance);
            }


            // 没有数据建什么流程
            if (dataTd == null || dataTd.isEmtpy()) {
//                PopToast.error(panelContext.getChannel(), "数据模型为空，无法创建流程");
                return null;
            }
            // 没有编排建什么流程
            if (busOrchTd == null || busOrchTd.isEmtpy()) {
                LvUtil.trace("业务编排为空，无法创建流程");
                return null;
            }

            PopToast.info(panelContext.getChannel(), "开始创建流程");

            // 编排构面Form
            Form busOrchForm = busOrchTd.getRows().get(0);
            String orchName = busOrchForm.getString("编排名称");


            // 1、构建【业务流图】   基于业务编排进行构建
            Form busFlowForm = createBusFlowForm(dao, panelName, busOrchForm);
            // 2、构建【数据模型】   直接把面板数据搬过来
            Form dataModelForm = createDataModelForm(dao, panelName, busOrchForm, viewFormModel, dataTd);
            // 3、构建【动作定义】   暂时不需要
            Form actionDefForm = createActionDefForm(dao, panelName, orchName);
            // 4、构建【权限矩阵】   先建立一个拥有全部权限的权限矩阵
            Form privilegeMatrixForm = createPrivilegeMatrixForm(dao, panelName, orchName, busOrchForm, dataTd, btnTd);
            // 5、最终汇总出【流程组装】
            Form flowCombineForm = doFlowCombine(dao, panelName, orchName, busFlowForm, dataModelForm, actionDefForm, privilegeMatrixForm);

            dao.commit();

            // 6、生效流程
            doFlowEffect(progress, panelContext, flowCombineForm);

            return StrUtil.format(流程模型编号模板, busFlowForm.getString("英文名"));


        }


    }

    // 流程生效
    private static void doFlowEffect(CFeProgressCtrlWithTextArea progress, PanelContext panelContext, Form flowCombineForm) throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            WorkSpace workSpace = IWorkSpaceService.get().queryWorkspaceByCode(dao, 面板设计工作空间编号);
            String jitUser = "gpf.md.user.jitUser";
            String jitOrg = "gpf.md.user.jitOrg";

//            CFeProgressCtrlWithTextArea prog = ProgressDialog.showProgressDialog(panelContext, "生效流程...", false, true);

            JITFormOpObserver observer = new JITFormOpObserver(
                    IUserMgr.get().queryUserByCode(dao, jitUser, "admin"),
                    jitOrg,
                    workSpace

            );
            ConvertContext context = PCRUtils.prepareContext(dao, workSpace);

//            FlowCombineDto flowCombineDto = DtoConvertUtil.convertToDto(flowCombineForm, FlowCombineDto.class);
            FlowCombineDto flowCombineDto = IFlowCombineService.get().queryFlowCombine(dao, flowCombineForm.getUuid());

            IFlowCombineService.get().effectFlowCombine(
                    Progress.wrap(progress),
                    flowCombineDto,
                    context,
                    observer

            );
        }

        return;
    }

    // 转换-流程组装
    private static Form doFlowCombine(IDao dao, String panelName, String orchName, Form busFlowForm, Form dataModelForm, Form actionDefForm, Form privilegeMatrixForm) throws Exception {
        Form form = new Form(JIT_PROCESS_FORM_ID_FLOW_COMBINE).setUuid(IdUtil.fastUUID());

        String code = StrUtil.format("{}_{}_流程组装", panelName, orchName);
        form.setAttrValue(Form.Code, code);
        form.setAttrValue("名称", code);
        form.setAttrValue("业务流图", toAssociationData(busFlowForm));
        form.setAttrValue("数据模型", toAssociationData(dataModelForm));
        form.setAttrValue("动作定义", toAssociationData(actionDefForm));
        form.setAttrValue("权限矩阵", toAssociationData(privilegeMatrixForm));

        return saveForm(dao, form);


    }

    private static Form createPrivilegeMatrixForm(IDao dao, String panelName, String orchName, Form busOrchForm, TableData dataTd, TableData btnTd) throws Exception {
        Form form = new Form(JIT_PROCESS_FORM_ID_PRIVILEGE_MATRIX_DO);
        String pmName = StrUtil.format("{}_{}_权限矩阵", panelName, orchName);
        form.setAttrValue(Form.Code, pmName);
        form.setAttrValue("名称", form.getString(Form.Code));
        form.setAttrValue("权限配置", getPrivilegeConfig(dao, pmName, busOrchForm, dataTd, btnTd));

        return saveForm(dao, form);
    }

    private static TableData getPrivilegeConfig(IDao dao, String pmName, Form busOrchForm, TableData dataTd, TableData btnTd) throws Exception {
        TableData privilegeConfigTd = new TableData(数据模型_权限矩阵_权限配置);

        Set<String> fieldNames = getFieldNamesByDataTd(dataTd);
        Set<String> nodeNames = getNodeNamesByBusOrchForm(busOrchForm);
        Set<String> btnNames = getBtnNamesByDataTd(btnTd);
        if (CollUtil.isEmpty(fieldNames) || CollUtil.isEmpty(nodeNames)) {
            return privilegeConfigTd;
        }
        nodeNames.add("默认");

        AssociationData dataPriPlanAc = generateDataPrivilegePlan(dao, pmName, nodeNames, fieldNames);
        AssociationData actionPriPlanAc = generateActionPrivilegePlan(dao, pmName, nodeNames, btnNames);

        TableData orchNodeTd = busOrchForm.getTable("所有节点");
        if (orchNodeTd == null || orchNodeTd.isEmtpy()) return privilegeConfigTd;
        for (String nodeName : nodeNames) {
            if (StrUtil.isBlank(nodeName)) continue;
            Form privilegeConfigForm = new Form(privilegeConfigTd.getFormModelId());
            privilegeConfigForm.setAttrValue("节点", nodeName);
            privilegeConfigForm.setAttrValue("身份", 角色_发起人);
            privilegeConfigForm.setAttrValue("用户匹配规则", 用户匹配规则_发起人);
            privilegeConfigForm.setAttrValue("规则参数", "");
            privilegeConfigForm.setAttrValue("数据权限方案", dataPriPlanAc);
            privilegeConfigForm.setAttrValue("数据权限方案分类", "默认");
            privilegeConfigForm.setAttrValue("动作权限方案", actionPriPlanAc);
            privilegeConfigForm.setAttrValue("动作权限方案分类", "默认");
            privilegeConfigTd.add(privilegeConfigForm);
        }

        return privilegeConfigTd;


    }

    private static AssociationData generateActionPrivilegePlan(IDao dao, String pmName, Set<String> btnNames, Set<String> fieldNames) throws Exception {
        Form actionPriPlanForm = new Form(数据模型_权限矩阵_权限配置_动作权限方案);
        actionPriPlanForm.setAttrValue(Form.Code, StrUtil.format("{}_动作权限方案_默认", pmName));

        TableData actionItemPriConfTd = new TableData(数据模型_权限矩阵_权限配置_动作权限方案_动作项权限配置);
        for (String nodeName : btnNames) {
            for (String fieldName : fieldNames) {
                Form form = new Form(actionItemPriConfTd.getFormModelId());
                form.setAttrValue("分类", nodeName);
                form.setAttrValue("动作项", fieldName);
                form.setAttrValue("规则", CollUtil.newArrayList(
                        权限规则_R,
                        权限规则_X
                ));
                form.setAttrValue("规则参数", "R();X()");
                actionItemPriConfTd.add(form);
            }
        }

        actionPriPlanForm.setAttrValue("动作项权限配置", actionItemPriConfTd);
        return toAssociationData(saveForm(dao, actionPriPlanForm));

    }

    private static AssociationData generateDataPrivilegePlan(IDao dao, String pmName, Set<String> nodeNames, Set<String> fieldNames) throws Exception {
        Form dataPriPlanForm = new Form(数据模型_权限矩阵_权限配置_数据权限方案);
        dataPriPlanForm.setAttrValue(Form.Code, StrUtil.format("{}_数据权限方案_默认", pmName));

        TableData dataItemPriConfTd = new TableData(数据模型_权限矩阵_权限配置_数据权限方案_数据项权限配置);
        for (String nodeName : nodeNames) {
            for (String fieldName : fieldNames) {
                Form form = new Form(dataItemPriConfTd.getFormModelId());
                form.setAttrValue("分类", nodeName);
                form.setAttrValue("数据项", fieldName);
                form.setAttrValue("规则", CollUtil.newArrayList(
                        权限规则_R,
                        权限规则_W
                ));
                form.setAttrValue("规则参数", "R();W()");
                dataItemPriConfTd.add(form);
            }
        }

        dataPriPlanForm.setAttrValue("数据项权限配置", dataItemPriConfTd);
        return toAssociationData(saveForm(dao, dataPriPlanForm));

    }

    private static Form createActionDefForm(IDao dao, String panelName, String orchName) throws Exception {
        Form form = new Form(JIT_PROCESS_FORM_ID_ACTION_DEFINE);
        form.setAttrValue(Form.Code, StrUtil.format("{}_{}_动作定义", panelName, orchName));
        form.setAttrValue("名称", form.getString(Form.Code));

        return saveForm(dao, form);

    }

    private static Form createDataModelForm(IDao dao, String panelName, Form busOrchForm, String viewFormModel, TableData dataTd) throws Exception {
        Form busDataModelForm = new Form(JIT_PROCESS_FORM_ID_DATA_MODEL).setUuid(IdUtil.fastUUID());
        String orchName = busOrchForm.getString("编排名称");

        busDataModelForm.setAttrValue(Form.Code, StrUtil.format("{}_{}_数据模型", panelName, orchName));
        busDataModelForm.setAttrValue("名称", busDataModelForm.getString(Form.Code));

        TableData dataModelNamesTd = new TableData(数据模型_业务流图_数据模型清单);

        dataModelNamesTd.add(new Form(dataModelNamesTd.getFormModelId())
                        .setAttrValue("序号", "1")
//                .setAttrValue("模型名称", panelName)
                        .setAttrValue("模型名称", viewFormModel)
        );

        busDataModelForm.setAttrValue("数据模型清单", dataModelNamesTd);
        busDataModelForm.setAttrValue("数据项标准", getDataModelTd(dataTd));

        return saveForm(dao, busDataModelForm);
    }

    private static TableData getDataModelTd(TableData dataTd) throws Exception {
        TableData dataModelStandaredTd = new TableData(数据模型_业务流图_数据项标准);
//        if(true) throw new RuntimeException(JSONUtil.toJsonStr(dataTd));
        for (Form row : dataTd.getRows()) {
            AssociationData attrImplAc = row.getAssociation("属性实现");
            if (attrImplAc == null) continue;
            Form attrImplForm = attrImplAc.getForm();
            if (attrImplForm == null) continue;

            String attrName = attrImplForm.getString("属性名称");

            Form form = new Form(dataModelStandaredTd.getFormModelId());
            form.setAttrValue("序号", "1");
            form.setAttrValue("字段名", attrName);

            TableData dataItemConfigTd = new TableData(数据模型_业务流图_数据项配置);
            dataItemConfigTd.add(new Form(dataItemConfigTd.getFormModelId())
                    .setAttrValue("数据项", attrName)
                    .setAttrValue("分类", "1")
                    .setAttrValue("规则", "✓")
                    .setAttrValue("规则参数", "")

            );
            form.setAttrValue("数据项配置", dataItemConfigTd);

            dataModelStandaredTd.add(form);
        }

        return dataModelStandaredTd;


    }

    private static Form createBusFlowForm(IDao dao, String panelName, Form busOrchForm) throws Exception {
        Form busFlowForm = new Form(JIT_PROCESS_FORM_ID_BUSINESS_FLOW).setUuid(IdUtil.fastUUID());

        String orchName = busOrchForm.getString("编排名称");
        TableData orchNodeTd = busOrchForm.getTable("所有节点");
        TableData orchFlowTd = busOrchForm.getTable("节点流转");

        busFlowForm.setAttrValue(Form.Code, StrUtil.format("{}_{}_业务流图", panelName, orchName));
        busFlowForm.setAttrValue("英文名", getEnName(busFlowForm.getString(Form.Code)));
        busFlowForm.setAttrValue("中文名", busFlowForm.getString(Form.Code));
        busFlowForm.setAttrValue("流程节点", getProcessFlowTd(orchNodeTd));
        busFlowForm.setAttrValue("流程连线", getProcessRelation(orchFlowTd));

        return saveForm(dao, busFlowForm);
    }

    private static TableData getProcessRelation(TableData orchFlowTd) throws Exception {
        if (orchFlowTd == null || orchFlowTd.isEmtpy()) return null;
        TableData ralationTd = new TableData(数据模型_业务流图_流程连线);
        for (Form row : orchFlowTd.getRows()) {
            Form form = new Form(ralationTd.getFormModelId());
            form.setAttrValue("节点名", row.getAttrValue("来源节点"));
            form.setAttrValue("连线名称", row.getAttrValue("路径名称"));
            form.setAttrValue("下游节点", row.getAttrValue("目标节点"));
            form.setAttrValue("离开策略", "匹配动作");
            form.setAttrValue("输入参数", getProcessRelationCondition(row.getString("条件表达")));
            form.setAttrValue("连线方式", "");
            ralationTd.add(form);

        }


        return ralationTd;
    }


    private static TableData getProcessFlowTd(TableData orchNodeTd) throws Exception {
        if (orchNodeTd == null || orchNodeTd.isEmtpy()) return null;
        TableData flowTd = new TableData(数据模型_业务流图_流程节点);

        long seqNo = 0;
        for (Form row : orchNodeTd.getRows()) {
            Form form = new Form(flowTd.getFormModelId());
            form.setAttrValue("序号", ++seqNo + "");
            form.setAttrValue("节点名", row.getString("节点名称"));
            form.setAttrValue("动作", row.getString("节点动作"));
            flowTd.add(form);
        }

        return flowTd;

    }


    // ========================= 支撑方法 =========================

    private static String getProcessRelationCondition(String conditionExpress) {
        if (StrUtil.isBlank(conditionExpress)) return null;
        // conditionExpress eg: "匹配动作('提交')"
        // 把'提交'这个名字提取出来

        return conditionExpress.replace("匹配动作('", "").replace("')", "");

    }

    private static Set<String> getFieldNamesByDataTd(TableData dataTd) throws Exception {
        Set<String> resultSet = new HashSet<>();
        if (dataTd == null || dataTd.isEmtpy()) return resultSet;

        for (Form row : dataTd.getRows()) {
            AssociationData attrImplAc = row.getAssociation("属性实现");
            if (attrImplAc == null) continue;
            Form attrImplForm = attrImplAc.getForm();
            if (attrImplForm == null) continue;
            String attrName = attrImplForm.getString("属性名称");
            if (StrUtil.isNotBlank(attrName)) {
                resultSet.add(attrName);
            }
        }


        return resultSet;
    }

    private static Set<String> getBtnNamesByDataTd(TableData btnTd) throws Exception {
        Set<String> resultSet = new HashSet<>();
        if (btnTd == null || btnTd.isEmtpy()) return resultSet;

        for (Form row : btnTd.getRows()) {
            AssociationData btnImplAc = row.getAssociation("实现按钮");
            if (btnImplAc == null) continue;
            Form btnImplForm = btnImplAc.getForm();
            if (btnImplForm == null) continue;
            String btnName = btnImplForm.getString("按钮名称");
            if (StrUtil.isNotBlank(btnName)) {
                resultSet.add(btnName);
            }
        }


        return resultSet;
    }

    private static Set<String> getNodeNamesByBusOrchForm(Form busOrchForm) throws Exception {
        Set<String> resultSet = new HashSet<>();
        if (busOrchForm == null) return resultSet;

        TableData orchNodeTd = busOrchForm.getTable("所有节点");
        if (orchNodeTd == null || orchNodeTd.isEmtpy()) return resultSet;

        for (Form node : orchNodeTd.getRows()) {
            String nodeName = node.getString("节点名称");
            String nextNodeName = node.getString("下游节点");
            if (StrUtil.isNotBlank(nodeName)) {
                resultSet.add(nodeName);
            }            if (StrUtil.isNotBlank(nextNodeName)) {
                resultSet.add(nextNodeName);
            }

        }

        return resultSet;
    }

    private static Form saveForm(IDao dao, Form form) throws Exception {
        if (form == null) return form;
        Form oldForm = IFormMgr.get().queryFormByCode(dao, form.getFormModelId(), form.getString(Form.Code));
        if (null != oldForm) {
            form.setUuid(oldForm.getUuid());
            return IFormMgr.get().updateForm(dao, form);
        } else {
            form = IFormMgr.get().createForm(dao, form);
            WorkSpace workSpace = IWorkSpaceService.get().queryWorkspaceByCode(dao, 面板设计工作空间编号);
            WorkSpaceLink link = new WorkSpaceLink();
            link.setCode(IdUtil.fastUUID());
            link.setWorkSpace(workSpace.getCode()).setWorkSpaceUuid(workSpace.getUuid());
            link.setRelDataModel(form.getFormModelId()).setRelDataUuid(form.getUuid()).setRelDataLabel(form.getStringByCode(Form.Code));
            IWorkSpaceService.get().saveWorkSpaceLink(dao, link);
            return form;
        }

    }

    // 检查是否需要创建流程
    public static boolean isNeedCreatePdf(Form dcInstance) throws Exception {
//        if(true) return false; 
        if (dcInstance == null) return false;
        // 【业务编排】不为空的时候允许创建
        TableData td = dcInstance.getTable("业务编排");
        return !(td == null || td.isEmtpy());
    }

    // 获取英文名称
    private static String getEnName(String orchName) {
        return PinyinUtil.getPinyin(orchName, "");
    }

    private static AssociationData toAssociationData(Form form) throws Exception {
        if (form == null) return null;
        String formModelId = form.getFormModelId();
        String code = form.getString(Form.Code);
        if (code == null || formModelId == null) {
            return null;
        }
        return new AssociationData(formModelId, code);
    }

}
