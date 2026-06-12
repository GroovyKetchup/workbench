package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.fe.gpf.dc.basic.IApplicationService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.workbench.app.IApplicationDeploy;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import fe.cmn.app.ability.PopToast;
import fe.cmn.data.PairDto;
import fe.cmn.editor.SelectEditorDto;
import fe.cmn.panel.*;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.widget.WidgetDto;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import gpf.dc.basic.fe.component.view.BaseFormView;
import gpf.dc.basic.param.view.dto.ApplicationSetting;
import gpf.dc.basic.param.view.dto.SettingItemDto;
import gpf.dc.basic.util.GpfDCBasicConst;
import gpf.dc.basic.util.GpfDCBasicUtil;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.app.IpWhitelistConfigDto;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.PanelDesignException;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.app.ApplicationDeployDto;
import octocm.workbench.dto.app.ApplicationExtendConfigDto;
import octocm.workbench.dto.app.ApplicationMenuDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Comment("面板设计工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-18", updateTime = "2025-06-18"
)
public class ApplicationUtil {

    public static final EasyOperation Op = EasyOperation.get();

    // 多层目录的分隔符
    public static final String MULTI_LEVEL_DIR_SEPARATOR = "/";


    // 应用选择下拉框
    public static final String WIDGET_ID_APPLICATION_SELECT_EDITOR = "WIDGET_ID_APPLICATION_SELECT_EDITOR";
    public static final String FormModelId_Application = ApplicationDeployDto.FormModelId;
    /**
     * 对外兼容使用的IP白名单配置字段名。
     * <p>
     * 应用表单底层不再写入名为 {@code ipWhitelistConfig} 的自定义扩展配置项，
     * 而是写入平台 AppViewSetting 已支持的两个标准配置项。
     */
    public static final String EXT_CONFIG_IP_WHITELIST = "ipWhitelistConfig";
    /**
     * 发布态 AppViewSetting 中的IP白名单启用字段名。
     */
    public static final String APP_VIEW_SETTING_ENABLE_IP_WHITELIST_KEY = "enableIpWhitelistAccessControl";
    /**
     * 发布态 AppViewSetting 中的IP白名单文本字段名。
     */
    public static final String APP_VIEW_SETTING_IP_ACCESS_WHITELIST_KEY = "ipAccessWhitelist";
    /**
     * AppViewSetting 配置项标签缓存，避免每次读写IP白名单配置都重新读取资源JSON。
     */
    private static volatile Map<String, String> appViewSettingLabelCache;

    // ========================= 默认应用方法 =========================
    // 获取或弹出框让用户设置默认应用
    public static String getOrSetDefaultPublishAppCode(PanelContext panelContext, OctoDomainOpObserver observer) throws Exception {
        // 如果当前没有设置默认的发布目标（应用）
        // 则打开应用选择框，让用户选择
        String applicationCode = getDefaultPublishApplicationCode(observer);

        if (StrUtil.isBlank(applicationCode)) {
            if (panelContext != null) {
                PopToast.info(panelContext.getChannel(), "当前业务域未配置默认发布目标，请进行选择");
                int maxPopNumber = 10;
                // openSetDefaultPublishApplicationPanel默认是不校验是否为空的，暂时先这样写
                // 之所以加一个maxPopNumber是担心里面有bug会无限循环
                // 后续会调整这个Panel的写法
                while (applicationCode == null && maxPopNumber-- > 0) {
                    applicationCode = ApplicationUtil.openSetDefaultPublishApplicationPanel(panelContext, observer);
                }
                return applicationCode;
            }

        }

        return applicationCode;

    }

    // 获取当前设置的默认发布应用
    public static ApplicationSetting getDefaultPublishApplication(OctoDomainOpObserver observer) throws Exception {
        Object value = getDefaultPublishApplicationCode(observer);
        if (value == null) return null;
        try (IDao dao = IDaoService.newIDao()) {
            return IApplicationService.get().queryApplicationSettingByCode(dao, (String) value);
        }
    }

    // 获取默认应用编号
    public static String getDefaultPublishApplicationCode(OctoDomainOpObserver observer) throws Exception {
        return PanelXParamsUtil.getParamOrDefault(observer,
                WorkBenchConst.ParamKey_DefaultPublishApplication, null);
    }

    // 设置默认应用编号
    public static void setDefaultPublishApplicationCode(OctoDomainOpObserver observer, String appCode) throws Exception {
        PanelXParamsUtil.setParam(observer, WorkBenchConst.ParamKey_DefaultPublishApplication,
                appCode);
    }


    // ========================= 应用发布核心方法 =========================


    // 将视图发布到指定应用的菜单中
    // 添加菜单的核心方法
    public static boolean appendViewToAssignAppMenu(OctoDomainOpObserver observer, String applicationCode, String upperFolderName, String viewName, String viewCode, String viewInstCode) throws Exception {

        if (StrUtil.isBlank(applicationCode)) throw ApplicationException.Builder.defaultAppNotSet();
        if (StrUtil.hasBlank(viewName, viewCode, viewInstCode))
            throw ApplicationException.Builder.viewInfoIncomplete();
        if (StrUtil.isBlank(applicationCode)) throw ApplicationException.Builder.appCodeEmpty();


        String upperFolderUuid = getOrCreateMenuFolder(observer, applicationCode, upperFolderName);
        try (IDao dao = IDaoService.newIDao()) {

            Form underApplicationForm = queryApplicationFormByAppCode(dao, applicationCode);
            if (underApplicationForm == null) throw ApplicationException.Builder.notFoundWithCode(applicationCode);

            TableData tableData = underApplicationForm.getTable(ApplicationDeployDto.sMenus);
            if (tableData == null) tableData = new TableData(ApplicationMenuDto.FormModelId);
            boolean isNewMenuItem = true;

            Form menuItemForm = new Form(tableData.getFormModelId());
            // 去个重
            for (Form menuItem : tableData.getRows()) {
                String menuName = menuItem.getString("名称");
                if (StrUtil.isBlank(menuName)) continue;

                if (viewInstCode.equals(menuItem.getString("视图编号"))) {
                    if ("目录".equals(menuItem.getString("类型"))) continue;
                    isNewMenuItem = false;
                    menuItemForm = menuItem;
                }
            }

            // 只有新建的菜单 && 存在上级才设置
            // 已存在的菜单不复用上层目录的逻辑
            if (isNewMenuItem && StrUtil.isNotBlank(upperFolderUuid)) {
                menuItemForm.setAttrValue("父节点", upperFolderUuid);
            }
            menuItemForm.setAttrValue("名称", viewName);
            menuItemForm.setAttrValue("类型", "视图");
            menuItemForm.setAttrValue("视图模型ID", viewCode);
            menuItemForm.setAttrValue("视图编号", viewInstCode);
            menuItemForm.setAttrValue("描述", viewName);
            menuItemForm.setAttrValue("状态", "上线");

            if (isNewMenuItem) {
                tableData.add(menuItemForm);
            }

            underApplicationForm.setAttrValue(ApplicationDeployDto.sMenus, tableData);
            IFormMgr.get().updateForm(null, dao, underApplicationForm, observer);
            IApplicationDeploy.get().deploy(Progress.newOutput(), dao, underApplicationForm, observer);

            dao.commit();

        }

        return true;
    }

    // 将视图发布到指定应用的菜单中
    // FIXME 这里逻辑极其混乱，原先仅服务于基于JDF的前后端耦合机制，后续又提供给纯后台服务，
    // FIXME 之所以没有PanelContext也没报错，单纯是混乱中却把代码走对了
    public static boolean appendViewToDefaultAppMenu(OctoDomainOpObserver observer,
                                                     PanelContext panelContext, String upperFolderName, String viewName, String viewCode, String viewInstCode) throws Exception {

        String applicationCode = getDefaultPublishApplicationCode(observer);

        if (StrUtil.isBlank(applicationCode)) {
            Op.warningToast(panelContext, "你还没有设置默认的发布应用");

            // 获取当前默认应用，当不存在时弹出选择面板
            ApplicationUtil.getOrSetDefaultPublishAppCode(panelContext, observer);

            return false;
        }

        if (StrUtil.hasBlank(viewName, viewCode, viewInstCode)) {
            Op.warningToast(panelContext, "要添加的视图信息不完整,视图名称、视图编号以及视图实例编号不得为空");
            return false;
        }
        if (StrUtil.isBlank(applicationCode)) {
            Op.warningToast(panelContext, "目标应用的编号为空");
            return false;
        }

        return appendViewToAssignAppMenu(observer, applicationCode, upperFolderName, viewName, viewCode, viewInstCode);


    }

    // 设置系统名称
    public static void setAppName(IDao dao, OctoDomainOpObserver observer, String appName) throws Exception {


        String applicationCode = getDefaultPublishApplicationCode(observer);
        if (applicationCode == null) {
            applicationCode = observer.getDomainCode();
        }
        try (IDao dao2 = IDaoService.newIDao()) {
            Form appForm = queryApplicationFormByAppCode(dao2, applicationCode);

            if (appForm == null) throw new RuntimeException("无法找到当前实例对应的应用");

            appForm.setAttrValue(ApplicationDeployDto.sSystemName, appName);
            IFormMgr.get().updateForm(null, dao2, appForm, observer);
            dao2.commit();

            // FIXME 卧槽了，里面appForm还需要再查一次，他妈的，所以需要先commit
            IApplicationDeploy.get().deploy(Progress.newOutput(), dao, appForm, observer);


        }


    }


    // ========================= 支撑方法 =========================


    // ========================= 应用扩展配置方法 =========================

    /**
     * 从已加载的应用表单中读取IP白名单配置，不访问数据库。
     *
     * @param applicationForm 应用表单
     * @return IP白名单配置，未配置时返回默认关闭状态
     * @throws Exception 读取扩展配置失败时抛出
     */
    public static IpWhitelistConfigDto getIpWhitelistConfig(Form applicationForm) throws Exception {
        String enabledLabel = getAppViewSettingLabel(APP_VIEW_SETTING_ENABLE_IP_WHITELIST_KEY);
        String whitelistLabel = getAppViewSettingLabel(APP_VIEW_SETTING_IP_ACCESS_WHITELIST_KEY);
        String enabledValue = getApplicationExtendConfig(applicationForm, enabledLabel);
        String whitelistValue = getApplicationExtendConfig(applicationForm, whitelistLabel);

        // 兼容早期测试数据：旧实现曾把整个DTO保存到 ipWhitelistConfig 自定义行。
        if (StrUtil.isBlank(enabledValue) && StrUtil.isBlank(whitelistValue)) {
            String legacyValue = getApplicationExtendConfig(applicationForm, EXT_CONFIG_IP_WHITELIST);
            if (StrUtil.isNotBlank(legacyValue)) return parseIpWhitelistConfig(legacyValue);
        }

        return normalizeIpWhitelistConfig(new IpWhitelistConfigDto()
                .setEnabled(parseBooleanConfigValue(enabledValue))
                .setItems(parseIpAccessWhitelistItems(whitelistValue)));
    }

    /**
     * 使用外部传入的dao查询应用表单并读取IP白名单配置，不负责提交事务。
     *
     * @param dao     数据访问对象
     * @param appCode 应用编号
     * @return IP白名单配置，未配置时返回默认关闭状态
     * @throws Exception 查询或解析失败时抛出
     */
    public static IpWhitelistConfigDto getIpWhitelistConfig(IDao dao, String appCode) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);
        return getIpWhitelistConfig(applicationForm);
    }

    /**
     * 将IP白名单配置写入已加载的应用表单对象，不调用updateForm。
     * <p>
     * 适合接入统一保存流程：调用方可在完成其他字段转换后统一更新同一个Form。
     *
     * @param applicationForm 应用表单
     * @param config          IP白名单配置
     * @throws Exception 写入扩展配置失败时抛出
     */
    public static void setIpWhitelistConfig(Form applicationForm, IpWhitelistConfigDto config) throws Exception {
        IpWhitelistConfigDto normalizedConfig = normalizeIpWhitelistConfig(config);
        setApplicationExtendConfig(applicationForm, getAppViewSettingLabel(APP_VIEW_SETTING_ENABLE_IP_WHITELIST_KEY),
                Boolean.TRUE.equals(normalizedConfig.getEnabled()) ? "true" : "false");
        setApplicationExtendConfig(applicationForm, getAppViewSettingLabel(APP_VIEW_SETTING_IP_ACCESS_WHITELIST_KEY),
                toIpAccessWhitelistText(normalizedConfig.getItems()));
        removeApplicationExtendConfig(applicationForm, EXT_CONFIG_IP_WHITELIST);
    }

    /**
     * 将IP白名单配置作为顶层字段暴露给应用配置JSON。
     * <p>
     * 该方法仅用于兼容旧调用方。MultiAgent 应用配置接口应直接透传“扩展配置”，
     * 由前端维护 {@link #APP_VIEW_SETTING_ENABLE_IP_WHITELIST_KEY} 和
     * {@link #APP_VIEW_SETTING_IP_ACCESS_WHITELIST_KEY} 对应的两个标准 AppViewSetting 项。
     *
     * @param appConfig       应用配置JSON
     * @param applicationForm 已加载的应用表单
     * @throws Exception 读取IP白名单配置失败时抛出
     */
    public static void exposeIpWhitelistConfig(JSONObject appConfig, Form applicationForm) throws Exception {
        if (appConfig == null || applicationForm == null) return;
        appConfig.set(EXT_CONFIG_IP_WHITELIST, getIpWhitelistConfig(applicationForm));
        removeApplicationExtendConfigItem(appConfig, EXT_CONFIG_IP_WHITELIST);
    }

    /**
     * 从应用配置JSON中取出顶层IP白名单配置，并将该字段从JSON中移除。
     * <p>
     * {@code ipWhitelistConfig} 不是应用表单模型字段；新调用方应优先直接维护
     * “扩展配置”中的标准 AppViewSetting 项。
     *
     * @param jsonObject 应用配置JSON
     * @return 入参未携带该字段时返回 null，表示保留原有IP白名单配置
     */
    public static IpWhitelistConfigDto takeIpWhitelistConfig(JSONObject jsonObject) {
        if (jsonObject == null || !jsonObject.containsKey(EXT_CONFIG_IP_WHITELIST)) {
            return null;
        }

        Object configValue = jsonObject.remove(EXT_CONFIG_IP_WHITELIST);
        return parseIpWhitelistConfigValue(configValue);
    }

    /**
     * 使用外部传入的dao更新IP白名单配置，不commit、不发布。
     *
     * @param dao      数据访问对象
     * @param observer 业务域观察者
     * @param appCode  应用编号
     * @param config   IP白名单配置
     * @throws Exception 更新失败时抛出
     */
    public static void updateIpWhitelistConfig(IDao dao, OctoDomainOpObserver observer, String appCode,
                                               IpWhitelistConfigDto config) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (observer == null) throw new RuntimeException("observer must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

        setIpWhitelistConfig(applicationForm, config);
        IFormMgr.get().updateForm(null, dao, applicationForm, observer);
    }

    /**
     * 独立保存IP白名单配置。
     * <p>
     * 内部创建dao、更新表单并commit，默认不发布应用。
     *
     * @param observer 业务域观察者
     * @param appCode  应用编号
     * @param config   IP白名单配置
     * @throws Exception 保存失败时抛出
     */
    public static void saveIpWhitelistConfig(OctoDomainOpObserver observer, String appCode,
                                             IpWhitelistConfigDto config) throws Exception {
        saveIpWhitelistConfig(observer, appCode, config, false);
    }

    /**
     * 独立保存IP白名单配置。
     *
     * @param observer 业务域观察者
     * @param appCode  应用编号
     * @param config   IP白名单配置
     * @param deploy   是否在保存后立即发布应用
     * @throws Exception 保存或发布失败时抛出
     */
    public static void saveIpWhitelistConfig(OctoDomainOpObserver observer, String appCode,
                                             IpWhitelistConfigDto config, boolean deploy) throws Exception {
        if (observer == null) throw new RuntimeException("observer must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();

        try (IDao dao = IDaoService.newIDao()) {
            Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
            if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

            setIpWhitelistConfig(applicationForm, config);
            IFormMgr.get().updateForm(null, dao, applicationForm, observer);
            if (deploy) {
                IApplicationDeploy.get().deploy(Progress.newOutput(), dao, applicationForm, observer);
            }
            dao.commit();
        }
    }

    /**
     * 将白名单规则列表转成 AppViewSetting 文本值。
     *
     * @param items 白名单规则列表
     * @return 以换行分隔的白名单文本
     */
    private static String toIpAccessWhitelistText(List<String> items) {
        if (items == null || items.isEmpty()) return "";

        List<String> normalizedItems = new ArrayList<>();
        for (String item : items) {
            if (StrUtil.isBlank(item)) continue;
            normalizedItems.add(item.trim());
        }
        return String.join("\n", normalizedItems);
    }

    /**
     * 根据 AppViewSetting 字段名获取应用表单扩展配置中使用的中文配置项。
     * <p>
     * 配置项清单由底层 {@code resource/ApplicationSetting.json} 提供，
     * 本方法首次调用时读取并缓存，后续不再重复解析资源文件。
     *
     * @param settingKey AppViewSetting 字段名
     * @return 应用表单扩展配置中的配置项名称
     * @throws Exception 配置项不存在时抛出
     */
    private static String getAppViewSettingLabel(String settingKey) throws Exception {
        if (StrUtil.isBlank(settingKey)) throw new RuntimeException("settingKey must not be blank");

        Map<String, String> labelMap = appViewSettingLabelCache;
        if (labelMap == null) {
            synchronized (ApplicationUtil.class) {
                labelMap = appViewSettingLabelCache;
                if (labelMap == null) {
                    labelMap = buildAppViewSettingLabelMap();
                    appViewSettingLabelCache = labelMap;
                }
            }
        }

        String label = labelMap.get(settingKey);
        if (StrUtil.isBlank(label)) {
            throw new RuntimeException(StrUtil.format("应用扩展配置项[{}]不存在", settingKey));
        }
        return label;
    }

    /**
     * 从底层应用视图设置项定义中构建字段名到配置项名称的映射。
     *
     * @return AppViewSetting 字段名到配置项名称的映射
     * @throws Exception 读取底层设置项失败时抛出
     */
    private static Map<String, String> buildAppViewSettingLabelMap() throws Exception {
        Map<String, String> labelMap = new HashMap<>();
        List<SettingItemDto> settingItems = GpfDCBasicUtil.getAppViewSettingItems();
        if (settingItems == null) return labelMap;

        for (SettingItemDto settingItem : settingItems) {
            if (settingItem == null || StrUtil.isBlank(settingItem.getValue())) continue;
            labelMap.put(settingItem.getValue(), settingItem.getLabel());
        }
        return labelMap;
    }

    /**
     * 将扩展配置底层保存的JSON字符串转成IP白名单DTO。
     *
     * @param configValue JSON字符串
     * @return IP白名单配置
     */
    private static IpWhitelistConfigDto parseIpWhitelistConfig(String configValue) {
        if (StrUtil.isBlank(configValue)) return defaultIpWhitelistConfig();
        try {
            return normalizeIpWhitelistConfig(JSONUtil.toBean(configValue, IpWhitelistConfigDto.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("IP白名单配置JSON格式非法", e);
        }
    }

    /**
     * 将JSON对象、JSON字符串或空值转换为IP白名单DTO。
     *
     * @param configValue 顶层 {@code ipWhitelistConfig} 字段值
     * @return 规范化后的IP白名单配置
     */
    private static IpWhitelistConfigDto parseIpWhitelistConfigValue(Object configValue) {
        if (configValue == null) return defaultIpWhitelistConfig();

        try {
            if (configValue instanceof String) {
                String configText = (String) configValue;
                if (StrUtil.isBlank(configText)) return defaultIpWhitelistConfig();
                return normalizeIpWhitelistConfig(JSONUtil.toBean(configText, IpWhitelistConfigDto.class));
            }
            return normalizeIpWhitelistConfig(JSONUtil.toBean(JSONUtil.parseObj(configValue), IpWhitelistConfigDto.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("IP白名单配置JSON格式非法", e);
        }
    }

    /**
     * 将 AppViewSetting 中的白名单文本拆成规则列表。
     *
     * @param whitelistText 白名单文本
     * @return 白名单规则列表
     */
    private static List<String> parseIpAccessWhitelistItems(String whitelistText) {
        List<String> items = new ArrayList<>();
        if (StrUtil.isBlank(whitelistText)) return items;

        String[] parts = whitelistText.split("[\\r\\n,;，；]+");
        for (String part : parts) {
            if (StrUtil.isBlank(part)) continue;
            items.add(part.trim());
        }
        return items;
    }

    /**
     * 解析布尔配置值，兼容平台下拉值和中文显示值。
     *
     * @param value 布尔配置文本
     * @return true 表示启用
     */
    private static Boolean parseBooleanConfigValue(String value) {
        if (StrUtil.isBlank(value)) return false;

        String text = value.trim();
        return "true".equalsIgnoreCase(text)
                || "1".equals(text)
                || "yes".equalsIgnoreCase(text)
                || "y".equalsIgnoreCase(text)
                || "是".equals(text);
    }

    /**
     * 规范化IP白名单配置，补齐默认值。
     *
     * @param config 原始配置
     * @return 规范化后的配置
     */
    private static IpWhitelistConfigDto normalizeIpWhitelistConfig(IpWhitelistConfigDto config) {
        if (config == null) return defaultIpWhitelistConfig();
        if (config.getEnabled() == null) config.setEnabled(false);
        if (config.getItems() == null) {
            config.setItems(new ArrayList<>());
        } else {
            List<String> normalizedItems = new ArrayList<>();
            for (String item : config.getItems()) {
                if (StrUtil.isBlank(item)) continue;
                normalizedItems.add(item.trim());
            }
            config.setItems(normalizedItems);
        }
        return config;
    }

    /**
     * 构造默认IP白名单配置。
     *
     * @return 默认关闭的IP白名单配置
     */
    private static IpWhitelistConfigDto defaultIpWhitelistConfig() {
        return new IpWhitelistConfigDto()
                .setEnabled(false)
                .setItems(new ArrayList<>());
    }

    /**
     * 使用外部传入的dao查询应用表单并读取指定扩展配置，不负责事务提交。
     *
     * @param dao        数据访问对象
     * @param appCode    应用编号
     * @param configItem 配置项名称
     * @return 配置值，不存在时返回 null
     * @throws Exception 查询失败时抛出
     */
    public static String getApplicationExtendConfig(IDao dao, String appCode, String configItem) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);
        return getApplicationExtendConfig(applicationForm, configItem);
    }

    /**
     * 从已加载的应用表单中读取指定扩展配置，不访问数据库。
     *
     * @param applicationForm 应用表单
     * @param configItem      配置项名称
     * @return 配置值，不存在时返回 null
     * @throws Exception 读取失败时抛出
     */
    public static String getApplicationExtendConfig(Form applicationForm, String configItem) throws Exception {
        Form configRow = findApplicationExtendConfigRow(applicationForm, configItem);
        if (configRow == null) return null;
        return configRow.getString(ApplicationExtendConfigDto.sValue);
    }

    /**
     * 参与外部事务的扩展配置更新方法，只执行updateForm，不commit、不发布。
     *
     * @param dao         数据访问对象
     * @param observer    业务域观察者
     * @param appCode     应用编号
     * @param configItem  配置项名称
     * @param configValue 配置值
     * @throws Exception 更新失败时抛出
     */
    public static void updateApplicationExtendConfig(IDao dao, OctoDomainOpObserver observer, String appCode,
                                                     String configItem, String configValue) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (observer == null) throw new RuntimeException("observer must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

        setApplicationExtendConfig(applicationForm, configItem, configValue);
        IFormMgr.get().updateForm(null, dao, applicationForm, observer);
    }

    /**
     * 独立保存指定扩展配置。
     * <p>
     * 内部创建dao、更新表单并commit，默认不发布应用。
     *
     * @param observer    业务域观察者
     * @param appCode     应用编号
     * @param configItem  配置项名称
     * @param configValue 配置值
     * @throws Exception 保存失败时抛出
     */
    public static void saveApplicationExtendConfig(OctoDomainOpObserver observer, String appCode,
                                                   String configItem, String configValue) throws Exception {
        saveApplicationExtendConfig(observer, appCode, configItem, configValue, false);
    }

    /**
     * 独立保存指定扩展配置。
     *
     * @param observer    业务域观察者
     * @param appCode     应用编号
     * @param configItem  配置项名称
     * @param configValue 配置值
     * @param deploy      是否在保存后立即发布应用
     * @throws Exception 保存或发布失败时抛出
     */
    public static void saveApplicationExtendConfig(OctoDomainOpObserver observer, String appCode,
                                                   String configItem, String configValue, boolean deploy) throws Exception {
        if (observer == null) throw new RuntimeException("observer must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();

        try (IDao dao = IDaoService.newIDao()) {
            Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
            if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

            setApplicationExtendConfig(applicationForm, configItem, configValue);
            IFormMgr.get().updateForm(null, dao, applicationForm, observer);
            if (deploy) {
                IApplicationDeploy.get().deploy(Progress.newOutput(), dao, applicationForm, observer);
            }
            dao.commit();
        }
    }

    /**
     * 修改内存中的应用表单扩展配置。
     * <p>
     * 扩展配置统一存放在应用表单的“扩展配置”嵌套表中：
     * 配置项字段为 {@link ApplicationExtendConfigDto#sItem}，
     * 配置值字段为 {@link ApplicationExtendConfigDto#sValue}。
     * 存在同名配置项时更新，不存在时新增一行。
     *
     * @param applicationForm 应用表单
     * @param configItem      配置项名称
     * @param configValue     配置值
     * @throws Exception 写入失败时抛出
     */
    public static void setApplicationExtendConfig(Form applicationForm, String configItem, String configValue) throws Exception {
        if (applicationForm == null) throw new RuntimeException("applicationForm must not be null");
        if (StrUtil.isBlank(configItem)) throw new RuntimeException("configItem must not be blank");

        // 扩展配置表不存在时现场创建，避免调用方关心底层嵌套表结构。
        TableData tableData = applicationForm.getTable(ApplicationDeployDto.sViewSetting);
        if (tableData == null) {
            tableData = new TableData(ApplicationExtendConfigDto.FormModelId);
            applicationForm.setAttrValue(ApplicationDeployDto.sViewSetting, tableData);
        }

        // 历史数据如果存在重复配置项，一并更新为同一个值，避免读取和保存结果不一致。
        boolean found = false;
        for (Form row : tableData.getRows()) {
            if (!isApplicationExtendConfigRow(row, configItem)) continue;
            row.setAttrValue(ApplicationExtendConfigDto.sValue, configValue);
            found = true;
        }
        if (found) return;

        String uuid = IdUtil.fastSimpleUUID();
        Form row = new Form(ApplicationExtendConfigDto.FormModelId);
        row.setUuid(uuid)
                .setAttrValue(Form.Code, uuid)
                .setAttrValue(ApplicationExtendConfigDto.sItem, configItem)
                .setAttrValue(ApplicationExtendConfigDto.sValue, configValue);
        tableData.add(row);
    }

    /**
     * 从内存中的应用表单扩展配置中移除指定配置项。
     *
     * @param applicationForm 应用表单
     * @param configItem      配置项名称
     * @throws Exception 读取配置行失败时抛出
     */
    private static void removeApplicationExtendConfig(Form applicationForm, String configItem) throws Exception {
        if (applicationForm == null || StrUtil.isBlank(configItem)) return;

        TableData tableData = applicationForm.getTable(ApplicationDeployDto.sViewSetting);
        if (tableData == null || tableData.isEmtpy()) return;

        List<Form> rows = new ArrayList<>(tableData.getRows());
        for (Form row : rows) {
            if (isApplicationExtendConfigRow(row, configItem)) {
                tableData.delete(row);
            }
        }
    }

    /**
     * 在应用表单的扩展配置嵌套表中查找指定配置项。
     *
     * @param applicationForm 应用表单
     * @param configItem      配置项名称
     * @return 配置行，不存在时返回 null
     * @throws Exception 读取失败时抛出
     */
    private static Form findApplicationExtendConfigRow(Form applicationForm, String configItem) throws Exception {
        if (applicationForm == null || StrUtil.isBlank(configItem)) return null;
        TableData tableData = applicationForm.getTable(ApplicationDeployDto.sViewSetting);
        if (tableData == null || tableData.isEmtpy()) return null;

        for (Form row : tableData.getRows()) {
            if (isApplicationExtendConfigRow(row, configItem)) {
                return row;
            }
        }

        return null;
    }

    /**
     * 判断表单行是否为指定扩展配置项。
     *
     * @param row        扩展配置行
     * @param configItem 配置项名称
     * @return 匹配时返回 true
     * @throws Exception 读取失败时抛出
     */
    private static boolean isApplicationExtendConfigRow(Form row, String configItem) throws Exception {
        return row != null && configItem.equals(row.getString(ApplicationExtendConfigDto.sItem));
    }

    /**
     * 从应用扩展配置JSON数组中移除指定配置项。
     *
     * @param appConfig  应用配置JSON
     * @param configItem 需要移除的扩展配置项
     */
    private static void removeApplicationExtendConfigItem(JSONObject appConfig, String configItem) {
        if (appConfig == null || StrUtil.isBlank(configItem)) return;

        Object extendConfigValue = appConfig.get(ApplicationDeployDto.sViewSetting);
        if (!(extendConfigValue instanceof JSONArray)) return;

        JSONArray extendConfigs = (JSONArray) extendConfigValue;
        for (int i = extendConfigs.size() - 1; i >= 0; i--) {
            Object rowValue = extendConfigs.get(i);
            if (!(rowValue instanceof JSONObject)) continue;

            JSONObject row = (JSONObject) rowValue;
            if (isApplicationExtendConfigJsonRow(row, configItem)) {
                extendConfigs.remove(i);
            }
        }
        if (extendConfigs.isEmpty()) {
            appConfig.remove(ApplicationDeployDto.sViewSetting);
        }
    }

    /**
     * 判断扩展配置JSON行是否为指定配置项。
     *
     * @param row        扩展配置行JSON
     * @param configItem 配置项名称
     * @return 匹配时返回 true
     */
    private static boolean isApplicationExtendConfigJsonRow(JSONObject row, String configItem) {
        return row != null && configItem.equals(row.getStr(ApplicationExtendConfigDto.sItem));
    }

    // 将视图发布到指定应用的菜单中
    public static void removeMenuItemByPanelCode(OctoDomainOpObserver observer, String applicationCode, String panelCode) throws Exception {

        if (StrUtil.hasBlank(panelCode)) return;

        if (StrUtil.isBlank(applicationCode)) applicationCode = getDefaultPublishApplicationCode(observer);
        if (StrUtil.isBlank(applicationCode)) throw ApplicationException.Builder.defaultAppNotSet();

        String menuInstCodePrefix = buildFullViewInstPrefix(observer.getDomainCode(), panelCode);

        try (IDao dao = IDaoService.newIDao()) {

            Form underApplicationForm = queryApplicationFormByAppCode(dao, applicationCode);
            if (underApplicationForm == null) throw ApplicationException.Builder.notFoundWithCode(applicationCode);

            TableData menuTd = underApplicationForm.getTable(ApplicationDeployDto.sMenus);
            if (menuTd == null) return;
            TableData newMenuTd = new TableData(GpfDCBasicConst.ApplicationMenuTreeModelId);

            for (Form menuItem : menuTd.getRows()) {
                String viewInstCode = menuItem.getString("视图编号");

                // 前缀不是这个业务域和面板的才允许添加进去
                boolean isRemoveTaget = StrUtil.isNotBlank(viewInstCode) && viewInstCode.startsWith(menuInstCodePrefix);
                if (!isRemoveTaget) {
                    newMenuTd.add(menuItem);
                }

            }


            underApplicationForm.setAttrValue(ApplicationDeployDto.sMenus, newMenuTd);
            IFormMgr.get().updateForm(null, dao, underApplicationForm, observer);
            IApplicationDeploy.get().deploy(Progress.newOutput(), dao, underApplicationForm, observer);

            dao.commit();

        }

        return;
    }

    // 获取或新增菜单文件夹
    // 支持循环创建文件夹，eg.folderNamesStr = A/B, 那么将会分别创建A和B两个目录，并返回B的UUid
    private static String getOrCreateMenuFolder(OctoDomainOpObserver observer, String applicationCode, String folderNamesStr) throws Exception {
        if (StrUtil.hasBlank(applicationCode, folderNamesStr)) return null;

        String[] folderNames = folderNamesStr.split(MULTI_LEVEL_DIR_SEPARATOR);


        String prevFolderUuid = null;

        for (String folderName : folderNames) {

            try (IDao dao = IDaoService.newIDao()) {

                Form underApplicationForm = queryApplicationFormByAppCode(dao, applicationCode);
                if (underApplicationForm == null) throw ApplicationException.Builder.notFoundWithCode(applicationCode);

                TableData tableData = underApplicationForm.getTable(ApplicationDeployDto.sMenus);
                if (tableData == null) tableData = new TableData(ApplicationMenuDto.FormModelId);

                String upperFolderUuid = findUpperMenuFolderUuidByUpperFolderName(tableData, folderName);

                // 如果指定了上层文件夹，但是找不到上层节点
                // 那么就是需要自己创建这个节点
                if (StrUtil.isNotBlank(upperFolderUuid)) {
                    prevFolderUuid = upperFolderUuid;
                    continue;
                }

                String uuid = IdUtil.fastSimpleUUID();
                Form menuItemForm = new Form(tableData.getFormModelId());
                menuItemForm.setUuid(uuid);
                menuItemForm.setAttrValue("名称", folderName);
                menuItemForm.setAttrValue("类型", "目录");
//                menuItemForm.setAttrValue("描述", StrUtil.format("创建于[{}]", DateTime.now().toString(DatePattern.NORM_DATETIME_FORMAT)));
                menuItemForm.setAttrValue("状态", "上线");

                // 如果前面一个不为空就设置其为自己的父节点
                if (StrUtil.isNotBlank(prevFolderUuid)) {
                    menuItemForm.setAttrValue("父节点", prevFolderUuid);

                }

                tableData.add(menuItemForm);
                underApplicationForm.setAttrValue(ApplicationDeployDto.sMenus, tableData);

                IFormMgr.get().updateForm(null, dao, underApplicationForm, observer);
                IApplicationDeploy.get().deploy(Progress.newOutput(), dao, underApplicationForm, observer);
                dao.commit();

                prevFolderUuid = uuid;


            }


        }

        return prevFolderUuid;

    }


    // 获取面板设计生效出来的视图示例编号
    public static String getPanelDesignGeneratedViewInstCode(String domainCode, Form panelDesignForm, boolean isTable, boolean isForm, boolean isWebPage) throws Exception {
        if (panelDesignForm == null) throw PanelDesignException.Builder.formEmpty();
        if (!isTable && !isForm && !isWebPage) throw PanelDesignException.Builder.cannotDetermineViewType();

        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");
        if (StrUtil.hasBlank(panelCode, panelName)) throw PanelDesignException.Builder.codeOrNameEmpty();
        // FIXME 由于这里无法从底层获取到视图的真实编号，因此只能手动拼接
        // FIXME 其次，本项目构建Panel面板时，与底层使用的规则不一致，后续需要调整

        String finalPageName = null;

        try {
            if (isTable) {
                Form panelTable = panelDesignForm.getTable("面板表格").getRows().get(0);
                finalPageName = panelTable.getString("表格名称");
            } else if (isWebPage) {
                Form panelWebPage = panelDesignForm.getTable("面板网页").getRows().get(0);
                finalPageName = panelWebPage.getString("页面名称");
            } else if (isForm) {
                Form panelForm = panelDesignForm.getTable("面板表单").getRows().get(0);
                finalPageName = panelForm.getString("表单名称");
            } else {
                // 最后的兜底手段
                finalPageName = panelDesignForm.getString("页面入口");

            }
        } catch (Exception e) {


            // 最后的默认拼接手段
            String suffix = null;
            if (isTable) suffix = "表格";
            if (isForm) suffix = "表单";
            if (isWebPage) suffix = "网页";

            return StrUtil.format("{}_{}_{}_{}", domainCode, panelCode, panelName, suffix);

        }

        return StrUtil.format("{}_{}_{}", domainCode, panelCode, finalPageName);

    }


    // 构建完整的视图名称
    public static String buildFullViewInstCode(String domainCode, String panelCode, String panelName,
                                               String viewName) {
        if (StrUtil.isBlank(viewName)) throw ApplicationException.Builder.viewNameEmpty();
//        if(StrUtil.isBlank(panelName)){
        if (true) {
            return StrUtil.format("{}_{}_{}", domainCode, panelCode, viewName);
        } else {
            return StrUtil.format("{}_{}_{}_{}", domainCode, panelCode, panelName, viewName);
        }
    }

    // 构建完整的视图前缀（不包含视图名称）
    public static String buildFullViewInstPrefix(String domainCode, String panelCode) {
        return StrUtil.format("{}_{}", domainCode, panelCode);
    }


    private static String findUpperMenuFolderUuidByUpperFolderName(TableData tableData, String upperFolderName) throws Exception {
        if (tableData == null || tableData.isEmtpy() || StrUtil.isBlank(upperFolderName)) return null;
        for (Form menuItem : tableData.getRows()) {
            String menuName = menuItem.getString("名称");
            if (!"目录".equals(menuItem.getString("类型"))) continue;
            if (StrUtil.isBlank(menuName)) continue;
            if (menuName.equals(upperFolderName)) {
                return menuItem.getUuid();
            }
        }

        return null;
    }


    public static Form queryApplicationFormByAppCode(IDao dao, String appCode) throws Exception {

        try {
            Cnd cnd = Cnd.NEW();
            cnd.where().orEquals(Op.getFieldCode(ApplicationDeployDto.sName), appCode)
                    .orEquals(Op.getFieldCode(ApplicationDeployDto.sSystemName), appCode)
                    .orEquals(Form.Code, appCode);
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_Application, cnd, 1, 1, true, true);
            if (queryRs.isEmpty()) return null;
            return queryRs.getDataList().get(0);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    // ========================= JDF的遗留方法  =========================


    // 打开设置默认应用的对话框
    // 如果用户操作了，会返回新的应用的code
    public static String openSetDefaultPublishApplicationPanel(PanelContext panelContext, OctoDomainOpObserver observer) throws Exception {
        ApplicationSetting currentPublishApplication = getDefaultPublishApplication(observer);
        PanelDto panelDto = buildSelectedDefaultPublishApplication(panelContext, currentPublishApplication, observer);

        PanelValue panelValue = PopDialog.showInput(panelContext, "设置默认应用", panelDto);
        if (panelValue == null) return null;
        Object selectedResultObj = panelValue.getValue(WIDGET_ID_APPLICATION_SELECT_EDITOR);
        if (!(selectedResultObj instanceof PairDto)) return null;
        PairDto<String, String> selectedResult = (PairDto<String, String>) selectedResultObj;

        String nowTargetPublishAppCode = selectedResult.getKey();
        String nowTargetPublishAppName = selectedResult.getValue();
        if (currentPublishApplication != null && StrUtil.isBlank(nowTargetPublishAppCode)) {
            nowTargetPublishAppCode = currentPublishApplication.getName();
        }

        // 设置参数
        PanelXParamsUtil.setParam(observer, WorkBenchConst.ParamKey_DefaultPublishApplication,
                nowTargetPublishAppCode);

        return nowTargetPublishAppCode;

    }

    private static PanelDto buildSelectedDefaultPublishApplication(PanelContext panelContext, ApplicationSetting
            publishApplication, OctoDomainOpObserver observer) throws Exception {
        BoxDto editor = buildApplicationSelectEditor(panelContext, observer);

        SinglePanelDto panelDto = SinglePanelDto.wrap(editor);
        panelDto.setPreferWidthByWindowSize(0.3d).setPreferHeightByWindowSize(0.2);
        return panelDto;

    }


    private static BoxDto buildApplicationSelectEditor(PanelContext panelContext, OctoDomainOpObserver observer) throws Exception {
        List<PairDto> items = new ArrayList<>();

        try (IDao dao = IDaoService.newIDao()) {
            List<Form> applicationForms = queryApplicationForms(dao, observer, false);

            for (Form appForm : applicationForms) {
                String code = appForm.getString(Form.Code);
                String lable = appForm.getString("标签");
                items.add(new PairDto<>(code, lable));
            }

        }

        SelectEditorDto editorDto = new SelectEditorDto().setWidgetId(WIDGET_ID_APPLICATION_SELECT_EDITOR);
        editorDto.setPreferHeight(30d);
        editorDto.setItems(items);
        return wrapEditor("目标应用", 100, editorDto);

    }


    // 查询当前业务域的应用列表
    public static List<Form> queryApplicationForms(IDao dao, OctoDomainOpObserver observer, boolean hasNestingData) throws Exception {
        Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_Application);
        ResultSet<Form> rs = IFormMgr.get().queryFormPage(dao, FormModelId_Application, cnd,
                1, Integer.MAX_VALUE, false, hasNestingData);
        return rs.getDataList();
    }


    private static BoxDto wrapEditor(String label, double width, WidgetDto editorDto) {
        return new BaseFormView<>().wrapEditor(label, width, null, editorDto, true, true);

    }


}
