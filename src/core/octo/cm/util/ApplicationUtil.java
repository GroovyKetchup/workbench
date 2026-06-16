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
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import gpf.dc.basic.fe.component.view.BaseFormView;
import gpf.dc.basic.param.view.dto.ApplicationSetting;
import gpf.dc.basic.param.view.dto.SettingItemDto;
import gpf.dc.basic.util.GpfDCBasicConst;
import gpf.dc.basic.util.GpfDCBasicUtil;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.app.GlobalEventDefinitionRecordDto;
import octo.cm.dto.app.GlobalEventDefinitionsDto;
import octo.cm.dto.app.IpWhitelistConfigDto;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.PanelDesignException;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.consts.OctoCM2WorkBenchConst;
import octocm.workbench.dto.app.ApplicationDeployDto;
import octocm.workbench.dto.app.ApplicationExtendConfigDto;
import octocm.workbench.dto.app.ApplicationMenuDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     * 应用级全局事件定义的事件说明标记。
     * <p>
     * 面板事件模型同时承载普通面板事件和应用全局事件定义，此值用于区分“应用事件”。
     */
    public static final String APP_GLOBAL_EVENT_DESC = ApplicationDeployDto.sAppEvent;
    /**
     * 后端分配的应用级全局事件编号前缀。
     * <p>
     * 事件编号按发布应用 Form 的 Owner 维度递增，仅用于后端识别和展示，不参与运行时事件匹配。
     */
    public static final String APP_GLOBAL_EVENT_CODE_PREFIX = "APP_EVT_";
    /**
     * 应用级全局事件编号数字后缀的固定长度。
     */
    public static final int APP_GLOBAL_EVENT_CODE_LENGTH = 5;
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


    // ========================= 应用级全局事件定义 =========================

    /**
     * 按应用编码读取应用级全局事件定义。
     * <p>
     * 应用编码仅用于定位 workbench 发布应用 Form（{@link ApplicationDeployDto#FormModelId}），
     * 不作为事件定义的稳定归属边界。返回的事件定义以发布应用 Form 的 UUID 作为归属范围。
     *
     * @param dao 当前 DAO
     * @param appCode 用于定位发布应用 Form 的应用编码或系统名称
     * @return 当前版本号和全局事件定义记录
     * @throws Exception 应用不存在或表单数据读取失败时抛出
     */
    public static GlobalEventDefinitionsDto getGlobalEventDefinitions(IDao dao, String appCode) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);
        return getGlobalEventDefinitions(dao, applicationForm);
    }

    /**
     * 从 workbench 发布应用 Form 读取应用级全局事件定义。
     * <p>
     * 入参 Form 必须是 {@link ApplicationDeployDto#FormModelId}。该 Form 的 UUID 作为事件 Owner
     * 边界，只读取面板事件模型中 Owner 等于应用 Form UUID 且事件说明等于
     * {@link #APP_GLOBAL_EVENT_DESC} 的事件 Form。
     *
     * @param dao 当前 DAO
     * @param applicationForm workbench 发布应用 Form
     * @return 当前版本号和全局事件定义记录
     * @throws Exception Form 模型不正确或表单数据读取失败时抛出
     */
    public static GlobalEventDefinitionsDto getGlobalEventDefinitions(IDao dao, Form applicationForm) throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        requireApplicationDeployForm(applicationForm);
        return buildGlobalEventDefinitionsDto(applicationForm.getUuid(),
                queryAppGlobalEventForms(dao, applicationForm.getUuid()));
    }

    /**
     * 按应用编码更新应用级全局事件定义。
     * <p>
     * 此重载会先把 {@code appCode} 解析为 workbench 发布应用 Form，再委托给 Form 入参重载。
     * 已有事件编号保持稳定，因为事件归属边界是发布应用 Form UUID，而不是 {@code appCode}。
     *
     * @param dao 当前 DAO
     * @param observer 表单创建、更新操作使用的领域观察者
     * @param appCode 用于定位发布应用 Form 的应用编码或系统名称
     * @param baseRevision 上一次读取返回的版本号，用于乐观锁校验
     * @param events 期望保存的事件定义记录集合
     * @return 保存后的版本号和事件定义记录
     * @throws Exception 应用不存在、乐观锁冲突或保存失败时抛出
     */
    public static GlobalEventDefinitionsDto updateGlobalEventDefinitions(IDao dao, OctoDomainOpObserver observer,
                                                                         String appCode, String baseRevision,
                                                                         List<GlobalEventDefinitionRecordDto> events)
            throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        Form applicationForm = queryApplicationFormByAppCode(dao, appCode);
        if (applicationForm == null) throw ApplicationException.Builder.notFoundWithCode(appCode);
        return updateGlobalEventDefinitions(dao, observer, applicationForm, baseRevision, events);
    }

    /**
     * 更新指定 workbench 发布应用 Form 的应用级全局事件定义。
     * <p>
     * 入参 Form 必须是 {@link ApplicationDeployDto#FormModelId}。每条记录中，{@code eventName}
     * 保存为面板事件名称，{@code definitionJson} 保存为动作说明。后端不解析
     * {@code definitionJson} 的业务结构，只校验非空且是合法 JSON。
     * <p>
     * 保存行为：
     * <ul>
     *     <li>使用应用 Form UUID 作为事件 Owner。</li>
     *     <li>按 Owner、应用事件说明和事件名称匹配已有事件。</li>
     *     <li>使用 {@link #APP_GLOBAL_EVENT_CODE_PREFIX} 为缺失编号的事件分配编号。</li>
     *     <li>将 {@link ApplicationDeployDto#sAppEvent} 重写为本次保存得到的事件 Form。</li>
     *     <li>删除同一 Owner 下已不在本次定义集合中的残留应用全局事件 Form。</li>
     * </ul>
     *
     * @param dao 当前 DAO
     * @param observer 表单创建、更新操作使用的领域观察者
     * @param applicationForm workbench 发布应用 Form
     * @param baseRevision 上一次读取返回的版本号，用于乐观锁校验
     * @param events 期望保存的事件定义记录集合
     * @return 保存后的版本号和事件定义记录
     * @throws Exception Form 模型不正确、乐观锁冲突、入参校验失败或保存失败时抛出
     */
    public static GlobalEventDefinitionsDto updateGlobalEventDefinitions(IDao dao, OctoDomainOpObserver observer,
                                                                         Form applicationForm, String baseRevision,
                                                                         List<GlobalEventDefinitionRecordDto> events)
            throws Exception {
        if (dao == null) throw new RuntimeException("dao must not be null");
        if (observer == null) throw new RuntimeException("observer must not be null");
        requireApplicationDeployForm(applicationForm);
        if (StrUtil.isBlank(applicationForm.getUuid())) throw new RuntimeException("applicationForm uuid must not be blank");

        List<Form> currentEventForms = queryAppGlobalEventForms(dao, applicationForm.getUuid());
        GlobalEventDefinitionsDto current = buildGlobalEventDefinitionsDto(applicationForm.getUuid(), currentEventForms);
        if (!Objects.equals(baseRevision, current.getRevision())) {
            throw new GlobalEventDefinitionsConflictException(current);
        }

        List<GlobalEventDefinitionRecordDto> normalizedEvents = normalizeGlobalEventRecords(events);
        Map<String, Form> existedEventByName = indexGlobalEventFormsByName(currentEventForms);
        int nextCodeNumber = findMaxAppGlobalEventCodeNumber(currentEventForms) + 1;

        List<Form> savedForms = new ArrayList<>();
        Set<String> keepUuids = new LinkedHashSet<>();
        for (GlobalEventDefinitionRecordDto event : normalizedEvents) {
            Form eventForm = existedEventByName.get(event.getEventName());
            boolean create = eventForm == null;
            if (create) {
                eventForm = Op.newForm(OctoCM2WorkBenchConst.ModelId_面板事件);
                eventForm.setAttrValue(OctoCM2WorkBenchConst.面板事件构面_事件编号,
                        formatAppGlobalEventCode(nextCodeNumber++));
            } else if (StrUtil.isBlank(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_事件编号))) {
                eventForm.setAttrValue(OctoCM2WorkBenchConst.面板事件构面_事件编号,
                        formatAppGlobalEventCode(nextCodeNumber++));
            }

            fillAppGlobalEventForm(applicationForm.getUuid(), eventForm, event);
            Form savedForm = create
                    ? IFormMgr.get().createForm(null, dao, eventForm, observer)
                    : IFormMgr.get().updateForm(null, dao, eventForm, observer);
            savedForms.add(savedForm);
            keepUuids.add(savedForm.getUuid());
        }

        rewriteApplicationGlobalEventReferences(dao, observer, applicationForm, savedForms);
        cleanupResidualAppGlobalEventForms(dao, currentEventForms, keepUuids);

        return getGlobalEventDefinitions(dao, applicationForm);
    }

    /**
     * 校验调用方没有传入 gpf.md.Application 或其他类似应用 Form。
     * <p>
     * 全局事件定义通过 workbench 发布应用 Form 落库，因为 {@link ApplicationDeployDto#sAppEvent}
     * 属于该模型。传入其他 Form 会导致事件记录被写到错误的 Owner UUID 下。
     */
    private static void requireApplicationDeployForm(Form applicationForm) {
        if (applicationForm == null) throw new RuntimeException("applicationForm must not be null");
        String formModelId = applicationForm.getFormModelId();
        if (!Objects.equals(FormModelId_Application, formModelId)) {
            throw new RuntimeException(StrUtil.format(
                    "applicationForm must be [{}], but got [{}]",
                    FormModelId_Application, formModelId));
        }
    }

    /**
     * 调用方基于过期版本更新全局事件定义时抛出的冲突异常。
     * <p>
     * 异常中会携带服务端最新定义，方便接口调用方直接刷新，而不必再次查询。
     */
    public static class GlobalEventDefinitionsConflictException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final GlobalEventDefinitionsDto latest;

        /**
         * 使用服务端最新全局事件定义创建冲突异常。
         *
         * @param latest 当前服务端最新定义
         */
        public GlobalEventDefinitionsConflictException(GlobalEventDefinitionsDto latest) {
            super("Global event definitions were changed by another client. Please refresh and retry.");
            this.latest = latest;
        }

        /**
         * 获取服务端最新全局事件定义。
         *
         * @return 服务端最新全局事件定义
         */
        public GlobalEventDefinitionsDto getLatest() {
            return latest;
        }
    }

    /**
     * 保存前规范化前端传入的事件记录。
     * <p>
     * 事件名称会去除首尾空白并要求唯一。调用方传入的事件编号会被忽略，因为编号由后端分配并维护。
     */
    private static List<GlobalEventDefinitionRecordDto> normalizeGlobalEventRecords(
            List<GlobalEventDefinitionRecordDto> events) {
        List<GlobalEventDefinitionRecordDto> normalized = new ArrayList<>();
        if (events == null || events.isEmpty()) return normalized;

        Set<String> eventNames = new LinkedHashSet<>();
        for (GlobalEventDefinitionRecordDto event : events) {
            if (event == null) continue;
            String eventName = trim(event.getEventName());
            String definitionJson = trim(event.getDefinitionJson());
            if (StrUtil.isBlank(eventName)) throw new IllegalArgumentException("eventName must not be blank");
            if (!eventNames.add(eventName)) {
                throw new IllegalArgumentException(StrUtil.format("Duplicate global event name: {}", eventName));
            }
            validateGlobalEventDefinitionJson(definitionJson);
            normalized.add(new GlobalEventDefinitionRecordDto()
                    .setEventName(eventName)
                    .setDefinitionJson(definitionJson));
        }
        return normalized;
    }

    /**
     * 校验待存储的事件定义内容是 JSON。
     * <p>
     * 此方法故意不校验前端事件定义的业务结构，完整 JSON 会作为不透明定义文档保存。
     */
    private static void validateGlobalEventDefinitionJson(String definitionJson) {
        if (StrUtil.isBlank(definitionJson)) throw new IllegalArgumentException("definitionJson must not be blank");
        try {
            String text = definitionJson.trim();
            if (text.startsWith("[")) {
                JSONUtil.parseArray(text);
            } else {
                JSONUtil.parseObj(text);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("definitionJson must be valid JSON", e);
        }
    }

    /**
     * 查询归属于某个发布应用 Form 的事件 Form。
     * <p>
     * 只有 Owner 等于应用 Form UUID 且事件说明等于 {@link #APP_GLOBAL_EVENT_DESC} 的面板事件
     * Form，才会被视为应用级全局事件定义。
     */
    private static List<Form> queryAppGlobalEventForms(IDao dao, String ownerUuid) throws Exception {
        if (StrUtil.isBlank(ownerUuid)) return new ArrayList<>();

        Cnd cnd = Cnd.NEW();
        cnd.where()
                .andEquals(Form.Owner, ownerUuid)
                .andEquals(Op.getFieldCode(OctoCM2WorkBenchConst.面板事件构面_事件说明), APP_GLOBAL_EVENT_DESC);
        cnd.orderBy(Op.getFieldCode(OctoCM2WorkBenchConst.面板事件构面_事件编号), "asc");

        ResultSet<Form> resultSet = IFormMgr.get().queryFormPage(dao,
                OctoCM2WorkBenchConst.ModelId_面板事件, cnd, 1, Integer.MAX_VALUE, true, true);
        if (resultSet == null || resultSet.isEmpty()) return new ArrayList<>();

        List<Form> dataList = resultSet.getDataList();
        if (dataList == null || dataList.isEmpty()) return new ArrayList<>();

        List<Form> eventForms = new ArrayList<>(dataList);
        eventForms.sort(Comparator
                .comparing((Form form) -> getFormStringQuietly(form, OctoCM2WorkBenchConst.面板事件构面_事件编号))
                .thenComparing(form -> getFormStringQuietly(form, OctoCM2WorkBenchConst.面板事件构面_事件名称)));
        return eventForms;
    }

    /**
     * 根据已落库的事件 Form 构造接口 DTO 和乐观锁版本号。
     */
    private static GlobalEventDefinitionsDto buildGlobalEventDefinitionsDto(String ownerUuid, List<Form> eventForms)
            throws Exception {
        List<GlobalEventDefinitionRecordDto> records = new ArrayList<>();
        if (eventForms != null) {
            for (Form eventForm : eventForms) {
                GlobalEventDefinitionRecordDto record = toGlobalEventDefinitionRecord(eventForm);
                if (record == null) continue;
                records.add(record);
            }
        }

        return new GlobalEventDefinitionsDto()
                .setRevision(buildGlobalEventRevision(ownerUuid, records))
                .setEvents(records);
    }

    /**
     * 将一条已落库的面板事件 Form 转换为接口记录。
     */
    private static GlobalEventDefinitionRecordDto toGlobalEventDefinitionRecord(Form eventForm) throws Exception {
        if (eventForm == null) return null;
        String eventName = trim(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_事件名称));
        if (StrUtil.isBlank(eventName)) return null;
        return new GlobalEventDefinitionRecordDto()
                .setEventCode(trim(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_事件编号)))
                .setEventName(eventName)
                .setDefinitionJson(trim(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_动作说明)));
    }

    /**
     * 根据应用 Owner UUID 和排序后的事件记录构造稳定版本哈希。
     */
    private static String buildGlobalEventRevision(String ownerUuid, List<GlobalEventDefinitionRecordDto> records)
            throws Exception {
        List<GlobalEventDefinitionRecordDto> sortedRecords = new ArrayList<>();
        if (records != null) sortedRecords.addAll(records);
        sortedRecords.sort(Comparator
                .comparing((GlobalEventDefinitionRecordDto record) -> safeText(record.getEventCode()))
                .thenComparing(record -> safeText(record.getEventName()))
                .thenComparing(record -> safeText(record.getDefinitionJson())));

        StringBuilder builder = new StringBuilder();
        builder.append(safeText(ownerUuid)).append('\n');
        for (GlobalEventDefinitionRecordDto record : sortedRecords) {
            builder.append(safeText(record.getEventCode())).append('\t')
                    .append(safeText(record.getEventName())).append('\t')
                    .append(safeText(record.getDefinitionJson())).append('\n');
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(builder.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder revision = new StringBuilder();
        for (byte b : hash) {
            revision.append(String.format("%02x", b & 0xff));
        }
        return revision.toString();
    }

    /**
     * 按事件名称索引已有事件 Form，以便保存时保留既有事件编号。
     */
    private static Map<String, Form> indexGlobalEventFormsByName(List<Form> eventForms) throws Exception {
        Map<String, Form> map = new LinkedHashMap<>();
        if (eventForms == null) return map;
        for (Form eventForm : eventForms) {
            if (eventForm == null) continue;
            String eventName = trim(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_事件名称));
            if (StrUtil.isBlank(eventName) || map.containsKey(eventName)) continue;
            map.put(eventName, eventForm);
        }
        return map;
    }

    /**
     * 将接口记录字段按落库映射写入面板事件 Form。
     */
    private static void fillAppGlobalEventForm(String ownerUuid, Form eventForm,
                                               GlobalEventDefinitionRecordDto event) throws Exception {
        eventForm.setAttrValue(Form.Owner, ownerUuid);
        eventForm.setAttrValue(OctoCM2WorkBenchConst.面板事件构面_事件名称, event.getEventName());
        eventForm.setAttrValue(OctoCM2WorkBenchConst.面板事件构面_事件说明, APP_GLOBAL_EVENT_DESC);
        eventForm.setAttrValue(OctoCM2WorkBenchConst.面板事件构面_动作说明, event.getDefinitionJson());
    }

    /**
     * 将发布应用 Form 的“应用事件”关联字段重写为本次保存得到的事件集合。
     */
    private static void rewriteApplicationGlobalEventReferences(IDao dao, OctoDomainOpObserver observer,
                                                                Form applicationForm, List<Form> savedForms)
            throws Exception {
        List<AssociationData> associations = new ArrayList<>();
        if (savedForms != null) {
            for (Form savedForm : savedForms) {
                associations.add(toAssociationData(savedForm));
            }
        }
        applicationForm.setAttrValue(ApplicationDeployDto.sAppEvent, associations);
        IFormMgr.get().updateForm(null, dao, applicationForm, observer);
    }

    /**
     * 删除保存前属于该应用、但已不在本次提交定义集合中的应用级全局事件 Form。
     */
    private static void cleanupResidualAppGlobalEventForms(IDao dao, List<Form> currentEventForms, Set<String> keepUuids)
            throws Exception {
        if (currentEventForms == null || currentEventForms.isEmpty()) return;
        Set<String> keep = keepUuids == null ? new LinkedHashSet<>() : keepUuids;
        for (Form eventForm : currentEventForms) {
            if (eventForm == null || StrUtil.isBlank(eventForm.getUuid())) continue;
            if (keep.contains(eventForm.getUuid())) continue;
            IFormMgr.get().deleteForm(dao, eventForm.getFormModelId(), eventForm.getUuid());
        }
    }

    /**
     * 查找当前应用 Owner 下已有 APP_EVT 编号的最大数字后缀。
     */
    private static int findMaxAppGlobalEventCodeNumber(List<Form> eventForms) throws Exception {
        int max = 0;
        if (eventForms == null) return max;
        for (Form eventForm : eventForms) {
            if (eventForm == null) continue;
            String eventCode = trim(eventForm.getString(OctoCM2WorkBenchConst.面板事件构面_事件编号));
            int number = parseAppGlobalEventCodeNumber(eventCode);
            if (number > max) max = number;
        }
        return max;
    }

    /**
     * 解析 APP_EVT 编号的数字后缀；不匹配的值按 0 处理。
     */
    private static int parseAppGlobalEventCodeNumber(String eventCode) {
        if (StrUtil.isBlank(eventCode) || !eventCode.startsWith(APP_GLOBAL_EVENT_CODE_PREFIX)) return 0;
        String suffix = eventCode.substring(APP_GLOBAL_EVENT_CODE_PREFIX.length());
        try {
            return Integer.parseInt(suffix);
        } catch (Exception ignored) {
            return 0;
        }
    }

    /**
     * 格式化后端分配的应用级全局事件编号。
     */
    private static String formatAppGlobalEventCode(int number) {
        return APP_GLOBAL_EVENT_CODE_PREFIX + String.format("%0" + APP_GLOBAL_EVENT_CODE_LENGTH + "d", number);
    }

    /**
     * 为发布应用 Form 的“应用事件”字段创建关联数据。
     */
    private static AssociationData toAssociationData(Form form) throws Exception {
        if (form == null) return null;
        String formCode = form.getString(Form.Code);
        if (StrUtil.isBlank(formCode)) formCode = form.getUuid();
        return new AssociationData(form.getFormModelId(), formCode);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static String getFormStringQuietly(Form form, String fieldName) {
        try {
            return form == null ? "" : safeText(form.getString(fieldName));
        } catch (Exception e) {
            return "";
        }
    }

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
