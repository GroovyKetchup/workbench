package fe.octo.cm.component.catalog.proc;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IWorkBenchFeService;
import cell.rapidView.function.BasicFunc;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import fe.cmn.menu.MenuDto;
import fe.cmn.menu.MenuItemDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.ability.PopMenu;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.*;
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
import octo.cm.util.UniformStyleUtil;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("工作台目录场景层操作类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchSceneLayerProc extends AbsOctoCmTreeProc {

    public static final String CurrentProcessTargetType = WorkBenchConst.LAYER_NAME_SCENE;
    public static final String CurrentProcessFormModelId = WorkBenchConst.FormModelId_SceneLayer;
    public static final String CurrentProcessNodeViewCode = "OctoCM_workbench_场景业务需求";
    public static final String CMD_ON_CREATE_SCENE_IMPL = "CMD_ON_CREATE_SCENE_IMPL";
    public static final String CMD_ON_SHOW_PANEL_DESIGN = "CMD_ON_SHOW_PANEL_DESIGN";

    @Override
    public boolean enableCreate(TreeNodeDto node) {
        return true;
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
        return JDFICons.screen;
    }

    @Override
    public List<TreeNodeDto> queryTree(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        String parentKey = querier.getParentKey();

        if (StrUtil.isBlank(parentKey)) return Collections.emptyList();

        try (IDao dao = IDaoService.newIDao()) {
            IFormMgr mgr = IFormMgr.get();

            Cnd cnd = Cnd.NEW();
            if (isQuerySelf(querier)) {
                cnd.where().andEquals(mgr.getFieldCode("场景编号"), parentKey);
            } else {
                cnd.where().orEquals(mgr.getFieldCode("上层模块编号"), parentKey)
                        .orEquals(mgr.getFieldCode("上层场景编号"), parentKey);
            }


            ResultSet<Form> queried = mgr.queryFormPageWithoutNesting(dao, WorkBenchConst.FormModelId_SceneLayer, cnd,
                    1, Integer.MAX_VALUE);

            if (queried.isEmpty()) return Collections.emptyList();
            List<TreeNodeDto> resultList = new ArrayList<>();
            for (Form form : queried.getDataList()) {
                resultList.add(
                        convert2FeTreeNodeDto(form)
                );
            }

            return resultList;

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
        return false;
    }


    @Override
    public Class<? extends ServiceIntf> getServiceCell() {
        return IWorkBenchFeService.class;
    }

    @Override
    public TreeNodeDto convert2FeTreeNodeDto(Object node) throws Exception {
        Form form = (Form) node;

        String key = form.getString(Form.Code);
        String label = form.getString("场景名称");

        AssociationData upperGoalAc = form.getAssociation("上层模块编号");
        AssociationData uppperSceneAc = form.getAssociation("上层场景编号");
        if (ObjUtil.isAllEmpty(upperGoalAc, uppperSceneAc))
            throw SceneException.Builder.upperModuleOrSceneNotSet(label);

        String upperGoalCode = upperGoalAc != null ? upperGoalAc.getValue() : null;
        String upperSceneCode = uppperSceneAc != null ? uppperSceneAc.getValue() : null;

        String parentKey = StrUtil.isNotBlank(upperSceneCode) ? upperSceneCode : upperGoalCode;

        TreeNodeDto feNode = new TreeNodeDto(key, parentKey, "[场景] " + label, getIcon(), isLeaf());
        feNode.setButtonBar(getButtonBar(feNode));
        feNode.setGestureDetector(buildGestureDetectorDto(feNode, null));

        TreeNodeExtraInfo<Form> extraInfo = new TreeNodeExtraInfo<>();
        extraInfo.setData(form).setNodeType(CurrentProcessTargetType).setInvokeClass(getTreePanel().getClass()).setRealDataUuid(form.getUuid());

        feNode.setBinaryData(extraInfo);
        feNode.setDraggable(getDraggableDto(feNode));

        return feNode;
    }

    @Override
    public TreeMenuDto getContextMenu(TreeNodeDto feNode, List<TreeNodeDto> otherSelected, TreeNodeQuerierContext context) throws Exception {
        TreeMenuDto contextMenu = super.getContextMenu(feNode, otherSelected, context);

        contextMenu.getMenuItems().addAll(
                CollUtil.newArrayList(
                        createMenuItem(feNode, JDFICons.design, Text_PublishToPanel, CMD_ON_PUBLISH_TO_PANEL),
                        createMenuItem(feNode, JDFICons.control, Text_PublishToApplication, CMD_ON_PUBLISH_TO_APPLICATION)
                )
        );
        return contextMenu;
    }

    @Override
    public void onClickCreateButton(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        // 这里要打开菜单，究竟是建立同级别的，还是低下的？

        List<MenuItemDto> menuItems = new ArrayList<>(CollUtil.newArrayList(
                createMenuItem(feNode, "新增子场景", FormModelId_SceneLayer),
                createMenuItem(feNode, "新增数据", FormModelId_SceneLayer_Data),
                createMenuItem(feNode, "新增行为", FormModelId_SceneLayer_Behavior),
                createMenuItem(feNode, "新增约束", FormModelId_SceneLayer_Constraint),
                createMenuItem(feNode, "新增编排", FormModelId_SceneLayer_Orchestration),
                createMenuItem(feNode, "新增展示", FormModelId_SceneLayer_Display),
                createMenuItem(feNode, "新增验证", FormModelId_SceneLayer_Verification)

        ));

        MenuDto menuDto = new MenuDto();
        menuDto.setMenuItems(menuItems);
        UniformStyleUtil.setMenuStyle(menuDto);
        PopMenu.show(panelContext, menuDto);

    }

    @Override
    public void onCreateNode(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        Form triggerForm = getFormByTreeNode(feNode);

        boolean isSameFormModel = CurrentProcessFormModelId.equals(triggerForm.getFormModelId());

        Form form = new Form(CurrentProcessFormModelId);
        if (isSameFormModel) {
            form.setAttrValue("上层场景编号", new AssociationData(FormModelId_SceneLayer, feNode.getKey()));
        } else {
            form.setAttrValue("上层模块编号", new AssociationData(WorkBenchConst.FormModelId_SubGoalLayer, feNode.getKey()));
        }
        popNodeFormView(panelContext, form, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);

    }

    @Override
    public WidgetDto buildNodeFormView(PanelContext panelContext, TreeNodeDto node, Form form, boolean isWriteable) throws Exception {
        return buildNodeFormView(panelContext, form, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);

    }


}
