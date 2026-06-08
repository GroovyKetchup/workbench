package cell.octo.cm.fe.view;


import cell.CellIntf;
import cmn.anotation.ClassDeclare;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.PanelDto;
import fe.octo.cm.page.PanelXHomePage;
import gpf.dc.basic.fe.component.BaseFeActionIntf;
import gpf.dc.basic.fe.component.app.AppCacheUtil;
import gpf.dc.basic.fe.component.param.AppHomePageParam;
import gpf.dc.basic.param.view.BaseFeActionParameter;
import gpf.dc.basic.param.view.dto.ApplicationSetting;
import gpf.exception.VerifyException;
import octo.cm.exception.business.ApplicationException;
import org.nutz.dao.entity.annotation.Comment;

@Comment("意图引擎首页")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-18", updateTime = "2025-08-18"
)
public interface IPanelXHomePage<T extends BaseFeActionParameter> extends CellIntf, BaseFeActionIntf<T> {
    @Override
    default Object execute(T input) throws Exception {

        PanelXHomePage<AppHomePageParam> homePage = new PanelXHomePage<>();

        PanelContext context = input.getPanelContext();

        AppHomePageParam homePageParam = new AppHomePageParam();

        ApplicationSetting setting = AppCacheUtil.getSetting(context);
        if (setting == null) {
            throw ApplicationException.Builder.notExist();
        }
        String panelGlobalKey = AppCacheUtil.getAppUuid(context);

        homePageParam.setAppLabel(setting.getLabel());
        homePageParam.setAppName(setting.getName());
        homePageParam.setSystemUuid(setting.getUuid());
        homePageParam.setSessionKey(AppCacheUtil.getAppSessionKey(context));

        homePage.setWidgetParam(homePageParam);
        PanelDto panel = (PanelDto) homePage.getWidget(context);
        panel.setPanelGlobalKey(panelGlobalKey);
        return panel;
    }

    @Override
    default Class<? extends T> getInputParamClass() {
        return (Class<? extends T>) BaseFeActionParameter.class;
    }

}
