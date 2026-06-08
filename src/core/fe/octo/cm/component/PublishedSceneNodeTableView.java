package fe.octo.cm.component;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.rapidView.function.BasicFunc;
import cell.rapidView.function.CommonFunctions;
import cell.rapidView.function.RapidViewBasicFunc;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import fe.cmn.app.ability.PopToast;
import fe.cmn.menu.MenuDto;
import fe.cmn.menu.MenuItemDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.PanelDto;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.panel.ability.PopMenu;
import fe.cmn.res.JDFICons;
import fe.cmn.table.TableRowDto;
import fe.cmn.table.listener.TableRowListener;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.octo.cm.component.catalog.WorkBenchParentConstraintCatalog;
import fe.octo.cm.page.WorkBenchPage;
import fe.util.component.dto.FeDeliverData;
import fe.util.component.param.TreeParam;
import gpf.adur.data.Form;
import gpf.dc.basic.fe.component.param.BaseTableViewParam;
import gpf.dc.basic.fe.component.param.BaseViewParam;
import gpf.dc.basic.fe.component.view.FormTable;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.SceneException;
import octo.cm.util.UniformStyleUtil;
import org.nutz.dao.entity.annotation.Comment;

import java.io.IOException;

@Comment("已发布场景需求表格")
public class PublishedSceneNodeTableView extends FormTable<BaseTableViewParam> implements CommonFunctions {

    /**
     *
     */
    private static final long serialVersionUID = -6775794099321852810L;
    public static final String CMD_ON_SHOW_SCENE_TREE = "CMD_ON_SHOW_SCENE_TREE";
    public static final String CMD_ON_SHOW_PANEL_DESIGN = "CMD_ON_SHOW_PANEL_DESIGN";


    @Override
    public void onRowClick(TableRowListener listener, PanelContext panelContext, WidgetDto source) throws Exception {
        TableRowDto row = listener.getRow();
        Form form = (Form) row.getBinaryData();
        MenuDto optModeMenu = new MenuDto();
        optModeMenu.setMenuItems(
                CollUtil.newArrayList(
                        createMenuItemDto(JDFICons.preview, "需求描述", CMD_ON_SHOW_SCENE_TREE, form),
                        createMenuItemDto(JDFICons.preview, "面板设计", CMD_ON_SHOW_PANEL_DESIGN, form)
                )
        );

        PopMenu.show(panelContext, optModeMenu);


    }

    // 展示面板设计
    private void onShowPanelDesign(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        Form form = getFormByListener(listener);
        String sceneCode = form.getString("场景编号");
        String sceneName = form.getString("场景名称");

        if (StrUtil.isBlank(sceneCode)) throw SceneException.Builder.sceneCodeEmpty();

        try (IDao dao = IDaoService.newIDao()) {
            Form needsForm = queryFormByAssignField(dao, WorkBenchConst.FormModelId_PanelDesign, "所属场景", sceneCode);
            if (needsForm == null) {
                PopToast.info(panelContext.getChannel(), "该场景不存在面板设计");
                return;
            }
            RapidViewBasicFunc.get().popupFormPanelOnlyRead(dao, panelContext, needsForm, BasicFunc.PDCFormView, WorkBenchConst.FormViewCode_PanelDesign,
                    StrUtil.format("面板设计({})", sceneName), 0.9, 0.7, true);

        }

    }

    // 展示场景树
    private void onShowSceneTree(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        Form form = getFormByListener(listener);
        String sceneCode = form.getString("场景编号");
        String sceneName = form.getString("场景名称");

        // 自定义一个树
        WorkBenchParentConstraintCatalog<TreeParam> workBenchCatalog = new WorkBenchParentConstraintCatalog<>();
        TreeParam treeParam = new TreeParam();
        treeParam.setRootNodeKey(sceneCode).setRootNodeType(WorkBenchConst.LAYER_NAME_SCENE);
        workBenchCatalog.setWidgetParam(treeParam);
        WidgetDto tree = workBenchCatalog.getWidget(panelContext);

        WorkBenchPage<BaseViewParam> workBenchPage = new WorkBenchPage<>();
        BaseViewParam param = new BaseViewParam();
        workBenchPage.setWidgetParam(param);

        // 把自定义的树传进去
        PanelDto panelDto = (PanelDto) workBenchPage.getWidget(panelContext, tree);
        panelDto.setPreferWidthByWindowSize(0.7).setPreferHeightByWindowSize(0.8);
        PopDialog.show(panelContext, StrUtil.format("{}({})", sceneName, sceneCode), panelDto);


    }


    @Override
    public Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        String serviceCommand = listener.getServiceCommand();
        if (CMD_ON_SHOW_SCENE_TREE.equals(serviceCommand)) {
            onShowSceneTree(listener, panelContext, source);
            return null;
        } else if (CMD_ON_SHOW_PANEL_DESIGN.equals(serviceCommand)) {
            onShowPanelDesign(listener, panelContext, source);
            return null;

        }
        return super.onListener(listener, panelContext, source);
    }

    private Form getFormByListener(ListenerDto listener) throws IOException, ClassNotFoundException {
        FeDeliverData<Form> binaryData = (FeDeliverData<Form>) listener.getBinaryData();
        return binaryData.getData();
    }

    private MenuItemDto createMenuItemDto(String icon, String label, String command, Form form) {

        MenuItemDto itemDto = new MenuItemDto();
        itemDto.setLabel(label).setIcon(icon);
        itemDto.setOnClick(newListener(getService(), command, true, form));
        // 设置样式
        UniformStyleUtil.setMenuItemStyle(itemDto);

        return itemDto;

    }


}
