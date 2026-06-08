package fe.octo.cm.component.catalog.proc;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.action.IActionMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.gpf.dc.runtime.IPDFRuntimeMgr;
import cell.octo.cm.IWorkBenchFeService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.PanelDto;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.*;
import fe.cmn.widget.*;
import fe.octo.cm.component.catalog.WorkBenchCatalog;
import fe.util.component.dto.FeDeliverData;
import fe.util.component.dto.TreeNodeExtraInfo;
import fe.util.component.extlistener.CommandCallbackListener;
import fe.util.component.tree.AbsTreeFeNodeProc;
import gpf.adur.action.Action;
import gpf.adur.data.Form;
import gpf.dc.basic.param.view.FormParameter;
import octo.cm.constant.WorkBenchConst;
import octo.cm.util.UniformStyleUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static cell.rapidView.function.BasicFunc.*;
import static fe.octo.cm.component.catalog.WorkBenchCatalog.CMD_NODE_VIEW_CANCEL;
import static fe.octo.cm.component.catalog.WorkBenchCatalog.CMD_NODE_VIEW_CONFIRM;

@Comment("OctoCM树节点处理抽象类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-17", updateTime = "2025-06-17"
)
public abstract class AbsOctoCmTreeProc extends AbsTreeFeNodeProc {
    public static final String CMD_ON_SEE_PANEL_DESIGN = "CMD_ON_SEE_PANEL_DESIGN";
    public static final String CMD_ON_PUBLISH_TO_PANEL = "CMD_ON_PUBLISH_TO_PANEL";
    public static final String CMD_ON_PUBLISH_TO_APPLICATION = "CMD_ON_PUBLISH_TO_APPLICATION";
    public static final String Text_PublishToPanel = "发布到面板";
    public static final String Text_PublishToApplication = "发布到应用";

    // ========================= 重写方法 =========================

    @Override
    public TreeMenuDto getContextMenu(TreeNodeDto feNode, List<TreeNodeDto> otherSelected, TreeNodeQuerierContext context) throws Exception {

        TreeMenuDto menu = new TreeMenuDto();
        UniformStyleUtil.setMenuStyle(menu);
        menu.setMenuItems(CollUtil.newArrayList(
                createMenuItem(feNode, JDFICons.refresh, "刷新", CMD_REFRESH)
        ));
        return menu;
    }


    // ========================= 抽象方法 =========================

    //创建节点的方法
    public abstract void onCreateNode(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception;

    // 构建当前节点对应的页面
    public abstract WidgetDto buildNodeFormView(PanelContext panelContext, TreeNodeDto node, Form form, boolean isWriteable) throws Exception;


    // 构建页面
    public WidgetDto buildNodeFormView(PanelContext panelContext, Form form, String viewUUID, String viewInstCode) throws Exception {
        IWorkBenchFeService feService = IWorkBenchFeService.get();
        CommandCallbackListener confirm1Callback = feService.newCallbackListener(IWorkBenchFeService.class, CMD_BUTTON_CONFIRM, CMD_NODE_VIEW_CONFIRM, null, false);
        CommandCallbackListener confirm2Callback = feService.newCallbackListener(IWorkBenchFeService.class, CMD_CONFIRM, CMD_NODE_VIEW_CONFIRM, null, false);
        CommandCallbackListener cancel1Callback = feService.newCallbackListener(IWorkBenchFeService.class, CMD_BUTTON_CANCEL, CMD_NODE_VIEW_CANCEL, null, false);
        CommandCallbackListener cancel2Callback = feService.newCallbackListener(IWorkBenchFeService.class, "CMD_CANCEL", CMD_NODE_VIEW_CANCEL, null, false);
        List<CommandCallbackListener> callbackLsnrs = CollUtil.newArrayList(confirm1Callback, confirm2Callback, cancel1Callback, cancel2Callback);
        return buildNodeFormView(panelContext, form, viewUUID, viewInstCode, callbackLsnrs);
    }

    public WidgetDto buildPopNodeFormView(PanelContext panelContext, Form form, String viewUUID, String viewInstCode) throws Exception {
        IWorkBenchFeService feService = IWorkBenchFeService.get();
        CommandCallbackListener confirm1Callback = feService.newPopupPanelCallbackListener(IWorkBenchFeService.class, CMD_BUTTON_CONFIRM, CMD_NODE_VIEW_CONFIRM, null);
        CommandCallbackListener confirm2Callback = feService.newPopupPanelCallbackListener(IWorkBenchFeService.class, CMD_CONFIRM, CMD_NODE_VIEW_CONFIRM, null);
        CommandCallbackListener cancel1Callback = feService.newPopupPanelCallbackListener(IWorkBenchFeService.class, CMD_BUTTON_CANCEL, CMD_NODE_VIEW_CANCEL, null);
        CommandCallbackListener cancel2Callback = feService.newPopupPanelCallbackListener(IWorkBenchFeService.class, "CMD_CANCEL", CMD_NODE_VIEW_CANCEL, null);
        List<CommandCallbackListener> callbackLsnrs = CollUtil.newArrayList(confirm1Callback, confirm2Callback, cancel1Callback, cancel2Callback);
        return buildNodeFormView(panelContext, form, viewUUID, viewInstCode, callbackLsnrs);
    }


    public WidgetDto buildNodeFormView(PanelContext panelContext, Form form, String viewUUID, String viewInstCode, List<CommandCallbackListener> callbackLsnrs) throws Exception {

        IDCRuntimeContext rtx = IPDFRuntimeMgr.get().newRuntimeContext();

        modifyInvokeClass(callbackLsnrs);

        FormParameter.setCommandCallbackListener(rtx, callbackLsnrs);
        FormParameter.prepareFeActionParameter(rtx, panelContext, (ListenerDto) null, null, form);

        try (IDao dao = IDaoService.newIDao()) {
            Action viewAction = IActionMgr.get().queryActionByCode(dao,
                    viewUUID, viewInstCode);
            if (viewAction == null) return new LabelDto("没有找到这个视图动作");

            return (PanelDto) IActionMgr.get().executeAction(viewAction, rtx);
        }

    }

    public void popNodeFormView(PanelContext panelContext, Form form, String viewUUID, String viewInstCode) throws Exception {
        PanelDto nodeFormView = (PanelDto) buildPopNodeFormView(panelContext, form, viewUUID, viewInstCode);
        nodeFormView.setPreferSize(WindowSizeDto.all(0.6, 0.6));
        PopDialog.showInputwithOutButton(panelContext, "创建", nodeFormView);

    }

    @Override
    public ButtonBarDto getButtonBar(TreeNodeDto node) throws Exception {
        // 有做尝试，但没什么鸟用
        ButtonBarDto buttonBar = super.getButtonBar(node);
//        for (ButtonBarItemDto buttonBarItem : buttonBar.getButtonBarItems()) {
//            DecorationDto decoration = buttonBarItem.getDecoration();
//            if(decoration == null) decoration = new DecorationDto();
//            decoration.setTextStyle(new CTextStyle().setFontSize(20d));
//            buttonBarItem.setDecoration(decoration);
//
//        }
        return buttonBar;
    }


    // ========================= 支撑方法 =========================


    // 检查是不是查询自己
    public boolean isQuerySelf(TreeNodeQuerier querier) {
        Object userObject = querier.getUserObject();
        if (!(userObject instanceof String)) return false;
        return WorkBenchConst.SIGN_QUERY_SELF.equals((String) userObject);

    }

    // 设置默认的拖拽对象
    public DraggableDto getDraggableDto(TreeNodeDto node) throws Exception {
        // 先把处理这个功能
        if (true) return null;
        TreeNodeExtraInfo extraInfo = (TreeNodeExtraInfo) node.getBinaryData();
        DraggableDto dto = new DraggableDto();
        dto.setType(extraInfo.getNodeType());
        dto.setData(node);
        return dto;
    }

    // 统一的创建菜单Item的方法
    public TreeMenuItemDto createMenuItem(TreeNodeDto feNode, String label, String type) throws Exception {
        TreeMenuItemDto itemDto = new TreeMenuItemDto().setNode(feNode);
        itemDto.setLabel(label).setIcon(JDFICons.plus);
        itemDto.setOnClick(newListener(feNode, "CMD_ON_CREATE_SCENE_IMPL" + "+" + type, true));
        // 设置样式
        UniformStyleUtil.setMenuItemStyle(itemDto);
        return itemDto;

    }

    public TreeMenuItemDto createMenuItem(TreeNodeDto feNode, String icon, String label, String cmd) throws Exception {
        return createMenuItem(feNode, label, null)
                .setIcon(icon)
                .setOnClick(newListener(feNode, cmd, true));
    }

    // 从TreeNode里拿Form
    public Form getFormByTreeNode(TreeNodeDto node) throws IOException, ClassNotFoundException {
        if (node == null) return null;
        Object binaryData = node.getBinaryData();
        if (!(binaryData instanceof TreeNodeExtraInfo)) return null;
        TreeNodeExtraInfo<Form> extraInfo = (TreeNodeExtraInfo<Form>) binaryData;
        return extraInfo.getData();
    }

    // 统一的修改回调类
    private static void modifyInvokeClass(List<CommandCallbackListener> callbackListeners) throws ClassNotFoundException, IOException {
        for (CommandCallbackListener callbackListener : callbackListeners) {
            ListenerDto listener = callbackListener.getListener();
            FeDeliverData binaryData = (FeDeliverData) listener.getBinaryData();
            binaryData.setInvokeClass(WorkBenchCatalog.class.getName());
            listener.setBinaryData(binaryData);
        }


    }

}
