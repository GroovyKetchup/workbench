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
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static octo.cm.constant.WorkBenchConst.FormModelId_SceneLayer;
import static octo.cm.constant.WorkBenchConst.FormModelId_SubGoalLayer;

@Comment("工作台目录子目标层操作类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchSubGoalLayerProc extends AbsOctoCmTreeProc {

    public static final String CurrentProcessTargetType = WorkBenchConst.LAYER_NAME_SUBGOAL;
    public static final String CurrentProcessFormModelId = WorkBenchConst.FormModelId_SubGoalLayer;
    public static final String CurrentProcessNodeViewCode = "OctoCM_workbench_模块业务需求";


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
        return JDFICons.target;
    }

    @Override
    public List<TreeNodeDto> queryTree(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        String parentKey = querier.getParentKey();

        if (StrUtil.isBlank(parentKey)) return Collections.emptyList();

        try (IDao dao = IDaoService.newIDao()) {
            IFormMgr mgr = IFormMgr.get();

            Cnd cnd = Cnd.NEW();
            cnd.where().orEquals(mgr.getFieldCode("上层系统编号"), parentKey)
                    .orEquals(mgr.getFieldCode("上层模块编号"), parentKey);

            ResultSet<Form> queried = mgr.queryFormPageWithoutNesting(dao, WorkBenchConst.FormModelId_SubGoalLayer, cnd,
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
        String label = form.getString("模块名称");

        AssociationData upperSystemAc = form.getAssociation("上层系统编号");
        AssociationData upperGoalAc = form.getAssociation("上层模块编号");
        if (ObjUtil.isAllEmpty(upperSystemAc, upperGoalAc))
            throw SceneException.Builder.upperSystemOrModuleNotSet(label);

        String upperSystemCode = upperSystemAc != null ? upperSystemAc.getValue() : null;
        String upperGoalCode = upperGoalAc != null ? upperGoalAc.getValue() : null;

        String parentKey = StrUtil.isNotBlank(upperGoalCode) ? upperGoalCode : upperSystemCode;


        TreeNodeDto feNode = new TreeNodeDto(key, parentKey, "[模块] " + label, getIcon(), isLeaf());
        feNode.setButtonBar(getButtonBar(feNode));
        feNode.setGestureDetector(buildGestureDetectorDto(feNode, null));

        TreeNodeExtraInfo<Form> extraInfo = new TreeNodeExtraInfo<>();
        extraInfo.setData(form).setNodeType(CurrentProcessTargetType).setInvokeClass(getTreePanel().getClass()).setRealDataUuid(form.getUuid());
        feNode.setBinaryData(extraInfo);
        return feNode;
    }

    @Override
    public TreeMenuDto getContextMenu(TreeNodeDto feNode, List<TreeNodeDto> otherSelected, TreeNodeQuerierContext context) throws Exception {
        TreeMenuDto contextMenu = super.getContextMenu(feNode, otherSelected, context);
        contextMenu.getMenuItems().addAll(
                CollUtil.newArrayList(
                        createMenuItem(feNode, JDFICons.design,                 Text_PublishToPanel, CMD_ON_PUBLISH_TO_PANEL),
                        createMenuItem(feNode, JDFICons.control,     Text_PublishToApplication, CMD_ON_PUBLISH_TO_APPLICATION)
                )
        );
        return contextMenu;
    }

    @Override
    public void onClickCreateButton(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        // 这里要打开菜单，究竟是建立同级别的，还是低下的？

        List<MenuItemDto> menuItems = new ArrayList<>(CollUtil.newArrayList(
                createMenuItem(feNode, "新增子模块", FormModelId_SubGoalLayer),
                createMenuItem(feNode, "新增场景", FormModelId_SceneLayer)
        ));

        MenuDto menuDto = new MenuDto();
        menuDto.setMenuItems(menuItems);

        PopMenu.show(panelContext, menuDto);


    }

    @Override
    public void onCreateNode(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {

        Form triggerForm = getFormByTreeNode(feNode);

        boolean isSameFormModel = CurrentProcessFormModelId.equals(triggerForm.getFormModelId());

        Form form = new Form(CurrentProcessFormModelId);
        if (isSameFormModel) {
            form.setAttrValue("上层模块编号", new AssociationData(FormModelId_SubGoalLayer, feNode.getKey()));
        } else {
            form.setAttrValue("上层系统编号", new AssociationData(WorkBenchConst.FormModelId_SystemLayer, feNode.getKey()));
        }

        popNodeFormView(panelContext, form, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);

    }

    @Override
    public WidgetDto buildNodeFormView(PanelContext panelContext, TreeNodeDto node, Form form, boolean isWriteable) throws Exception {
        return buildNodeFormView(panelContext, form, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);
    }


}
