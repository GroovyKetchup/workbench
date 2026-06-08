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
import gpf.dc.basic.util.GpfDCBasicConst;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.PanelDesignException;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.app.ApplicationDeployDto;
import octocm.workbench.dto.app.ApplicationMenuDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

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
