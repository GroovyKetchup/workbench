package fe.octo.cm.page;


import com.kwaidoo.ms.tool.ToolUtilities;
import fe.cmn.panel.PanelContext;
import fe.cmn.widget.WidgetDto;
import fe.octo.cm.page.component.PanelXWorkPanel;
import gpf.dc.basic.fe.component.app.AppHomePage;
import gpf.dc.basic.fe.component.param.AppHomePageParam;

public class PanelXHomePage<T extends AppHomePageParam> extends AppHomePage<T> {



    @Override
    public WidgetDto getContent(PanelContext panelContext) throws Exception {
        PanelXWorkPanel<AppHomePageParam> contentPanel = new PanelXWorkPanel<>();
        AppHomePageParam param = ToolUtilities.clone(widgetParam);
        param.setWidgetId(WidgetId_WorkPanel);
        contentPanel.setWidgetParam(param);
        return contentPanel.getWidget(panelContext);
    }
}
