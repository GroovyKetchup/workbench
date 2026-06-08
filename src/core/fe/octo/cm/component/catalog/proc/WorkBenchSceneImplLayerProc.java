package fe.octo.cm.component.catalog.proc;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IWorkBenchFeService;
import cell.rapidView.function.BasicFunc;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import fe.cmn.panel.PanelContext;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.TreeHighlightListener;
import fe.cmn.tree.TreeNodeDto;
import fe.cmn.tree.TreeNodeQuerier;
import fe.cmn.tree.TreeNodeQuerierContext;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.util.component.dto.TreeNodeExtraInfo;
import fe.util.component.param.WidgetParam;
import fe.util.intf.ServiceIntf;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.SceneException;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Comment("工作台目录场景实现层操作类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-17", updateTime = "2025-06-17"
)
public class WorkBenchSceneImplLayerProc extends AbsOctoCmTreeProc {

    public static final String CurrentProcessTargetType = WorkBenchConst.LAYER_NAME_SCENE_IMPL;
    public static final String CurrentProcessIcon = JDFICons.function_library;

    @Override
    public boolean enableCreate(TreeNodeDto node) {
        return false;
    }

    @Override
    public boolean enableUpdate(TreeNodeDto node) {
        return false;
    }

    @Override
    public boolean enableDelete(TreeNodeDto node) {
        return true;
    }

    @Override
    public String getIcon() {
        return CurrentProcessIcon;
    }

    @Override
    public List<TreeNodeDto> queryTree(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        String parentKey = querier.getParentKey();

        if (StrUtil.isBlank(parentKey)) return Collections.emptyList();
        List<TreeNodeDto> resultList = new ArrayList<>();


        // 把五种实现都加进去
        try (IDao dao = IDaoService.newIDao()) {

            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Data, "数据名称")
            );
            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Behavior, "行为名称")
            );
            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Constraint, "约束名称")
            );
            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Orchestration, "编排名称")
            );
            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Display, "展示名称")
            );
            resultList.addAll(
                    querySceneImpl(dao, parentKey, WorkBenchConst.FormModelId_SceneLayer_Verification, "验证名称")
            );
            return resultList;

        }
    }

    private List<TreeNodeDto> querySceneImpl(IDao dao, String parentKey, String formModelId, String nameField) throws Exception {

        try {
            IFormMgr mgr = IFormMgr.get();

            Cnd cnd = Cnd.NEW();
            cnd.where().andEquals(mgr.getFieldCode("上层场景编号"), parentKey);

            ResultSet<Form> queried = mgr.queryFormPageWithoutNesting(dao, formModelId, cnd,
                    1, Integer.MAX_VALUE);

            if (queried.isEmpty()) return Collections.emptyList();
            List<TreeNodeDto> resultList = new ArrayList<>();
            for (Form form : queried.getDataList()) {
                form.setAttrValue("默认名称", nameField);
                form.setAttrValue("默认前缀", nameField.replace("名称", ""));
                resultList.add(
                        convert2FeTreeNodeDto(form)
                );
            }

            return resultList;
        } catch (Exception e) {
            return new ArrayList<>();
        }

    }


    @Override
    public List<TreeNodeDto> queryAndConvert2TreeNodeDto(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public TreeHighlightListener buildOnHighlightListener(TreeNodeDto node, WidgetParam widgetParam) {
        return null;
    }

    @Override
    public boolean isMatch(String nodeType) {
        return CurrentProcessTargetType.equals(nodeType);
    }

    @Override
    public boolean isLeaf() {
        return true;
    }


    @Override
    public Class<? extends ServiceIntf> getServiceCell() {
        return IWorkBenchFeService.class;
    }

    @Override
    public TreeNodeDto convert2FeTreeNodeDto(Object node) throws Exception {
        Form form = (Form) node;

        String key = form.getString(Form.Code);

        String label = getNodeLabel(form);

        AssociationData upperGoalAc = form.getAssociation("上层模块编号");
        AssociationData uppperSceneAc = form.getAssociation("上层场景编号");
        if (ObjUtil.isAllEmpty(upperGoalAc, uppperSceneAc))
            throw SceneException.Builder.upperModuleOrSceneNotSet(label);

        String upperGoalCode = upperGoalAc != null ? upperGoalAc.getValue() : null;
        String upperSceneCode = uppperSceneAc != null ? uppperSceneAc.getValue() : null;

        String parentKey = StrUtil.isNotBlank(upperSceneCode) ? upperSceneCode : upperGoalCode;

        TreeNodeDto feNode = new TreeNodeDto(key, parentKey, label, getIcon(), isLeaf());
        feNode.setButtonBar(getButtonBar(feNode));
        feNode.setGestureDetector(buildGestureDetectorDto(feNode, null));

        TreeNodeExtraInfo<Form> extraInfo = new TreeNodeExtraInfo<>();
        extraInfo.setData(form).setNodeType(CurrentProcessTargetType).setInvokeClass(getTreePanel().getClass()).setRealDataUuid(form.getUuid());
        feNode.setBinaryData(extraInfo);


        return feNode;

    }

    private String getNodeLabel(Form form) throws Exception {
        String formModelId = form.getFormModelId();
        switch (formModelId) {
            case WorkBenchConst.FormModelId_SceneLayer_Data:
                return "[数据] " + form.getString("数据名称");
            case WorkBenchConst.FormModelId_SceneLayer_Behavior:
                return "[行为] " + form.getString("行为名称");
            case WorkBenchConst.FormModelId_SceneLayer_Constraint:
                return "[约束] " + form.getString("约束名称");
            case WorkBenchConst.FormModelId_SceneLayer_Orchestration:
                return "[编排] " + form.getString("编排名称");
            case WorkBenchConst.FormModelId_SceneLayer_Display:
                return "[展示] " + form.getString("展示名称");
            case WorkBenchConst.FormModelId_SceneLayer_Verification:
                return "[验证] " + form.getString("验证名称");
            default:
                return "";
        }

    }


    @Override
    public void onCreateNode(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        String formModelId = listener.getServiceCommand().split("\\+")[1];
        String viewCode = getViewCodeByFormModelId(formModelId);
        Form form = new Form(formModelId);

        form.setAttrValue("上层场景编号", new AssociationData(WorkBenchConst.FormModelId_SceneLayer, feNode.getKey()));

        popNodeFormView(panelContext, form, BasicFunc.PDCFormView, viewCode);

    }


    @Override
    public WidgetDto buildNodeFormView(PanelContext panelContext, TreeNodeDto node, Form form, boolean isWriteable) throws Exception {
        String formModelId = form.getFormModelId();
        String viewCode = getViewCodeByFormModelId(formModelId);
        return buildNodeFormView(panelContext, form, BasicFunc.PDCFormView, viewCode);

    }

    private static String getViewCodeByFormModelId(String formModelId) {
        String viewCode = null;
        switch (formModelId) {
            case WorkBenchConst.FormModelId_SceneLayer_Data:
                viewCode = "OctoCM_workbench_场景数据业务需求";
                break;
            case WorkBenchConst.FormModelId_SceneLayer_Behavior:
                viewCode = "OctoCM_workbench_场景行为业务需求";
                break;
            case WorkBenchConst.FormModelId_SceneLayer_Constraint:
                viewCode = "OctoCM_workbench_场景约束业务需求";
                break;
            case WorkBenchConst.FormModelId_SceneLayer_Orchestration:
                viewCode = "OctoCM_workbench_场景编排业务需求";
                break;
            case WorkBenchConst.FormModelId_SceneLayer_Display:
                viewCode = "OctoCM_workbench_场景展示业务需求";
                break;
            case WorkBenchConst.FormModelId_SceneLayer_Verification:
                viewCode = "OctoCM_workbench_场景验证业务需求";
                break;
        }
        return viewCode;
    }


}
