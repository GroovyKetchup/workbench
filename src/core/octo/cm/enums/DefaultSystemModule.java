package octo.cm.enums;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.List;
import java.util.Map;

import static octo.cm.constant.WorkBenchConst.*;

// 默认系统模块[面板设计]
public enum DefaultSystemModule {

    LOGIN_PAGE("登录页", true, DefaultPanelDesign_DefaultLoginPage),
    DASHBOARD("工作台", true, DefaultPanelDesign_DefaultDashboard),
    ORGANIZATION_MANAGEMENT("组织架构", true, DefaultPanelDesign_OrganizationManagement),
    CUSTOM_SHELL("自定义外壳", false, DefaultPanelDesign_CustomShell);

    final String panelName;
    final boolean isCdpConfig;
    final String contentPath;
    Map<String, Object> params;

    DefaultSystemModule(String panelName, boolean isCdpConfig, String contentPath) {
        this.panelName = panelName;
        this.isCdpConfig = isCdpConfig;
        this.contentPath = contentPath;

    }

    public static DefaultSystemModule getByPanelName(String panelName) {
        for (DefaultSystemModule value : values()) {
            if (value.panelName.equals(panelName)) {
                return value;
            }
        }
        return null;
    }

    public static List<String> panelNames() {
        return CollUtil.newArrayList(
                DASHBOARD.panelName,
                ORGANIZATION_MANAGEMENT.panelName);

    }


    public String getPanelName() {
        return panelName;
    }

    public String getContentPath() {
        return contentPath;
    }

    public boolean isCdpConfig() {
        return isCdpConfig;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public DefaultSystemModule setParams(Map<String, Object> params) {
        this.params = params;
        return this;
    }

    @Override
    public String toString() {
        return new JSONObject()
                .set("key", this.name())
                .set("panelName", this.panelName)
                .set("cdpConfigPath", this.contentPath).toString();
    }

    public static void main(String[] args) {

        System.out.println(JSONUtil.toJsonStr(DefaultSystemModule.values()));

    }
}
