package fe.octo.cm.page.component;

import cmn.anotation.ClassDeclare;
import com.kwaidoo.ms.tool.CmnUtil;
import com.kwaidoo.ms.tool.ToolUtilities;
import fe.cmn.data.CColor;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.SplitViewDto;
import fe.cmn.panel.decoration.SplitViewDecorationDto;
import fe.cmn.tree.decoration.TreeDecorationDto;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.util.FeListenerUtil;
import fe.util.component.extlistener.CommandCallbackListener;
import fe.util.component.extlistener.CommandListener;
import fe.util.component.tree.AbsTreeFeNodeProc;
import fe.util.style.FeStyleConst;
import gpf.dc.basic.fe.component.app.AppWorkPanel;
import gpf.dc.basic.fe.component.app.AppWorkSpace;
import gpf.dc.basic.fe.component.param.AppHomePageParam;
import gpf.dc.basic.fe.component.param.BaseTreeViewParam;
import org.nutz.dao.entity.annotation.Comment;

@Comment("应用-工作面板")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-18", updateTime = "2025-08-18"
)
public class PanelXWorkPanel<T extends AppHomePageParam> extends AppWorkPanel {



    @Override
    public WidgetDto getWidget(PanelContext panelContext) throws Exception {
    	String widgetId = widgetParam.getWidgetId();
    	if(CmnUtil.isStringEmpty(widgetId))
    		widgetId = ToolUtilities.allockUUIDWithUnderline();
        PanelXTreePanel<BaseTreeViewParam> tree = new PanelXTreePanel<>();
        BaseTreeViewParam treeParam = new BaseTreeViewParam();
        treeParam.setWidgetId(WidgetId_LeftTree);
        tree.setWidgetParam(treeParam);
        WidgetDto treeDto = tree.getWidget(panelContext);
        treeDto.setStyleName(FeStyleConst.common_white_menu_tree);
        TreeDecorationDto treeDecorationDto = (TreeDecorationDto)new TreeDecorationDto()
                .setNodeHeight(48.0)
                ;
        treeDto.setDecoration(treeDecorationDto);
        
        AppWorkSpace<AppHomePageParam> dataPanel = new AppWorkSpace<>();
        AppHomePageParam dataParam = (AppHomePageParam) ToolUtilities.clone(widgetParam);
        dataParam.setWidgetId(WidgetId_RightTab);
        dataPanel.setWidgetParam(dataParam);
        WidgetDto rightPanel = dataPanel.getWidget(panelContext);
        ListenerDto lsnr = newListener(ListenerDto.class,getBuilderService(), CMD_OPEN_RIGHT_TAB, true, null,widgetId);
        CommandCallbackListener clickNodeCallback = newCommandCallback(AbsTreeFeNodeProc.CMD_CLICK_NODE, "点击树节点", "", lsnr, false);

        FeListenerUtil.setWidgetCommandCallbackListener(panelContext,treeDto, clickNodeCallback);
        SplitViewDto splitView = new SplitViewDto().setDividRatio(0.14).setLeft(treeDto).setRight(rightPanel);
        SplitViewDecorationDto splitDct = new SplitViewDecorationDto().setDividerColor(new CColor(245, 246, 250, 1)).setDividerThickness(2.0);
        splitView.setDecoration(splitDct);
        splitView.setWidgetId(widgetId);
        
        ListenerDto openTabLsnr = newListener(ListenerDto.class,getService(), CMD_OPEN_RIGHT_TAB, true, null,widgetId);
		CommandListener openTabCmd = new CommandListener(CMD_OPEN_RIGHT_TAB, "打开标签页", "", openTabLsnr, false);
		splitView.addExtendListener(openTabCmd);
        
        splitView.setBinaryData(widgetParam);
        return splitView;
    }




}
