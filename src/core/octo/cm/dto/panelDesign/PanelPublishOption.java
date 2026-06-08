package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("面板发布选项")
@ClassDeclare(
        label = "",
        what = "封装面板发布的可选行为，如是否挂载到菜单、挂载到哪个应用",
        why = "原有发布逻辑硬编码了默认应用菜单挂载，语义混乱；此DTO将意图显式化",
        how = "作为参数传递给 IPanelDesignService.publishPanelDesign",
        developer = "裴硕", version = "1.0",
        createTime = "2026-04-23", updateTime = "2026-04-23"
)
public class PanelPublishOption {

    // 是否挂载到应用菜单
    private boolean attachToMenu = false;

    // 目标应用编号；留空时使用业务域的默认发布应用
    private String applicationCode;


    public static PanelPublishOption publishOnly() {
        return new PanelPublishOption().setAttachToMenu(false);
    }

    public static PanelPublishOption attachToDefaultApp() {
        return new PanelPublishOption().setAttachToMenu(true);
    }

    public static PanelPublishOption attachToApp(String applicationCode) {
        return new PanelPublishOption().setAttachToMenu(true).setApplicationCode(applicationCode);
    }


    public boolean isAttachToMenu() {
        return attachToMenu;
    }

    public PanelPublishOption setAttachToMenu(boolean attachToMenu) {
        this.attachToMenu = attachToMenu;
        return this;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public PanelPublishOption setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
        return this;
    }

}
