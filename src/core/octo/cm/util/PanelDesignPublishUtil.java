package octo.cm.util;

import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.workbench.gui.Workbench2PanelDesignGuiAction;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.PanelDesignException;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计生效工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-27", updateTime = "2025-06-27"
)
public class PanelDesignPublishUtil {

    private static final EasyOperation Op = EasyOperation.get();
    public static final String VIEW_FORM_MODEL_ID_HTML_VIEW = "octocm.md.fe.view.HtmlView";
    public static final String VIEW_FORM_MODEL_ID_CDP_VIEW = "octocm.md.fe.view.CDPView";

    // 当存在多个视图的时候，将面板名称作为文件夹名称
    private static final boolean ADD_PANEL_NAME_FOLDER_WHEN_MORE_THAN_ONE_VIEW = true;

    // 发布锁，底下构建模型的时候不支持并发
    private static final ReentrantLock lock = new ReentrantLock();

    // 实际的生效方法
    // 当PanelContext为空的时候，将业务域设置为默认应用
    // FIXME 后续将发布和默认应用的逻辑拆分出来（保留此方法用于兼容旧调用链）
    public static void publish(Progress<?> progress, PanelContext panelContext, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        if (progress == null) progress = Progress.newOutput();

        // 1、纯发布（宗智底下的发布代码）
        doPublishOnly(progress, observer, panelDesignForm);

        // 2、挂载到默认应用（旧语义：PanelContext为空时使用业务域默认应用）
        if (panelContext == null) setBusDomainDefaultAppToPublishTarget(observer);
        String applicationCode = ApplicationUtil.getDefaultPublishApplicationCode(observer);

        attachPanelToAppMenu(progress, panelContext, observer, panelDesignForm, applicationCode);
    }


    // ========================= 拆分后的清晰方法 =========================

    /**
     * 纯发布：只调底层发布逻辑，不挂载菜单。
     */
    public static void doPublishOnly(Progress<?> progress, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        if (progress == null) progress = Progress.newOutput();
        if (panelDesignForm == null) throw PanelDesignException.Builder.formEmpty();

        if (!lock.tryLock(10, TimeUnit.MINUTES))
            throw PanelDesignException.Builder.failedToAcquirePublishLock();

        try {
            Workbench2PanelDesignGuiAction designGuiAction = Cells.get(Workbench2PanelDesignGuiAction.class);
            designGuiAction.publish(progress, observer.getDomainCode(), panelDesignForm);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 挂载面板视图到指定应用菜单。
     * @param applicationCode 目标应用编号；为空时报错
     */
    public static void attachPanelToAppMenu(Progress<?> progress, PanelContext panelContext,
                                            OctoDomainOpObserver observer, Form panelDesignForm,
                                            String applicationCode) throws Exception {
        if (progress == null) progress = Progress.newOutput();
        if (panelDesignForm == null) throw PanelDesignException.Builder.formEmpty();
        if (StrUtil.isBlank(applicationCode)) throw ApplicationException.Builder.appCodeEmpty();

        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");
        String panelEntry = panelDesignForm.getString("页面入口");

        // 页面入口为空就不进行菜单挂载
        if (StrUtil.isBlank(panelEntry)) {
            progress.sendProcess(100, "面板页面入口为空，跳过菜单挂载", true);
            return;
        }

        Set<String> pageEntrySet = new HashSet<>(Arrays.asList(panelEntry.split(",")));

        List<PublishTargetViewDto> publishTargetViewDtos = new ArrayList<>();
        publishTargetViewDtos.addAll(convertTableDataToTargetViews(observer, panelCode, panelName, pageEntrySet, panelDesignForm.getTable("面板网页")));
        publishTargetViewDtos.addAll(convertTableDataToTargetViews(observer, panelCode, panelName, pageEntrySet, panelDesignForm.getTable("面板表格")));

        if (Op.isEmpty(publishTargetViewDtos)) throw PanelDesignException.Builder.noPageToPublish();

        String upperFolderName = doQueryPanelSceneUpperModuleName(panelDesignForm);
        if (ADD_PANEL_NAME_FOLDER_WHEN_MORE_THAN_ONE_VIEW && publishTargetViewDtos.size() > 1) {
            upperFolderName = StrUtil.format("{}{}{}", upperFolderName, ApplicationUtil.MULTI_LEVEL_DIR_SEPARATOR, panelName);
        }

        // 移除当前面板在目标应用的旧菜单
        ApplicationUtil.removeMenuItemByPanelCode(observer, applicationCode, panelCode);

        for (PublishTargetViewDto viewDto : publishTargetViewDtos) {
            String viewModelCode = viewDto.getViewModelCode();
            String fullViewInstCode = autoGenerateCorrectViewInstCode(viewModelCode, viewDto.getViewInstCode());
            String viewName = viewDto.getViewName();

            progress.sendProcess(80, StrUtil.format("将视图[{}]添加到应用[{}]菜单", fullViewInstCode, applicationCode), true);

            boolean isSucceed = ApplicationUtil.appendViewToAssignAppMenu(observer, applicationCode,
                    upperFolderName, viewName, viewModelCode, fullViewInstCode);

            if (!isSucceed) {
                progress.sendProcess(100, StrUtil.format("将视图[{}]添加到应用[{}]菜单，操作失败", fullViewInstCode, applicationCode), true);
            }
        }

        progress.sendProcess(100, StrUtil.format("面板[{}]挂载菜单完毕", panelCode), true);
    }


    // 实际的生效方法
    public static void publishBatch(Progress<?> progress, PanelContext panelContext, OctoDomainOpObserver observer, List<Form> panelDesignForms) throws Exception {
        if (CollUtil.isEmpty(panelDesignForms)) throw PanelDesignException.Builder.noPanelToPublish();
        if (progress == null) progress = Progress.newOutput();

        int currExecNo = 1;
        int taskTotalNo = panelDesignForms.size();
        Op.sendProgressMsg(progress, 0, StrUtil.format("批量初始化面板设计，任务数量:[{}]", taskTotalNo));
        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;


        for (Form panelDesign : panelDesignForms) {

            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);

            String panelName = panelDesign.getString("面板名称");

            childProgress.sendProcess(0,
                    StrUtil.format("正在初始化[{}] [{}/{}]", panelName, currExecNo++, taskTotalNo),
                    true
            );

            try {

                publish(childProgress, panelContext, observer, panelDesign);

            } catch (Throwable e) {
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("面板发布失败, 面板名称[{}]", panelName),
                                ExceptionUtils.getFullStackTrace(e),
                                e
                        )
                );
                childProgress.sendProcess(100,
                        StrUtil.format("大模型初始化失败: {}", e.getMessage()),
                        true
                );

            }

        }

    }


    private static void setBusDomainDefaultAppToPublishTarget(OctoDomainOpObserver observer) throws Exception {
        String applicationCode = ApplicationUtil.getDefaultPublishApplicationCode(observer);
        if (StrUtil.isBlank(applicationCode)) {
            ApplicationUtil.setDefaultPublishApplicationCode(observer, observer.getDomainCode());
        }
    }


    // 将TableData转换为发布目标Dto
    private static List<PublishTargetViewDto> convertTableDataToTargetViews(OctoDomainOpObserver observer, String panelCode, String panelName, Set<String> pageEntrySet, TableData viewTd) throws Exception {
        if (Op.isEmpty(viewTd)) return Collections.emptyList();

        String tdFormModelId = viewTd.getFormModelId();
        // FIXME 似乎是JDF那边的BUG，tableData的FormModelId可能为空
        if (StrUtil.isBlank(tdFormModelId)) {
            tdFormModelId = viewTd.getRows().get(0).getFormModelId();
            if (StrUtil.isBlank(tdFormModelId)) {
                return Collections.emptyList();
            }
        }
        boolean isWebPage = tdFormModelId.equals(SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
        boolean isTable = tdFormModelId.equals(SlaveFormModelId_PanelDesign_View_Orchestration_Table);
        boolean isForm = tdFormModelId.equals(SlaveFormModelId_PanelDesign_View_Orchestration_Form);

        if (!isWebPage && !isTable && !isForm) return Collections.emptyList();

        String viewModelCode = null, nameField = null;
        if (isForm) {
            throw PanelDesignException.Builder.formPublishNotSupported();
//            viewModelCode = PDFInstanceTableViewDto.FormModelId;
//            nameField = "表单名称";
        }
        if (isWebPage) {
            viewModelCode = VIEW_FORM_MODEL_ID_HTML_VIEW;
            nameField = "页面名称";
        }
        if (isTable) {
            viewModelCode = VIEW_FORM_MODEL_ID_CDP_VIEW;
            nameField = "表格名称";
        }


        List<PublishTargetViewDto> resultList = new ArrayList<>();
        for (Form viewTarget : viewTd.getRows()) {
            String viewName = viewTarget.getString(nameField);
            if (StrUtil.isBlank(viewName)) continue;
            if (!pageEntrySet.contains(viewName)) continue;

            String fullViewInstCode = ApplicationUtil.buildFullViewInstCode(observer.getDomainCode(),
                    panelCode, panelName, viewName);


            resultList.add(new PublishTargetViewDto(viewModelCode, fullViewInstCode, viewName));

        }


        return resultList;
    }

    // 查询面板所属场景对应的模块名称
    // FIXME 当遇到子场景的时候无法做到多级目录
    private static String doQueryPanelSceneUpperModuleName(Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return null;

        try (IDao dao = IDaoService.newIDao()) {
            Form targetSceneForm = Op.queryFormByAc(dao, panelDesignForm.getAssociation("所属场景"));
            if (targetSceneForm == null) return null;
            Form upperModuleForm = Op.queryFormByAc(dao, targetSceneForm.getAssociation("上层模块编号"));
            if (upperModuleForm == null) return null;

            return upperModuleForm.getString("模块名称");

        }


    }


    //FIXME 动态选择，按道理应该只需要ViewName，但是宗智底下现在还给加了_表格
    private static String autoGenerateCorrectViewInstCode(String viewCode, String viewInstCode) {
        if (StrUtil.hasBlank(viewCode, viewInstCode)) return viewInstCode;

        List<String> attemptSuffixList = new ArrayList<>(Arrays.asList("", "_表格"));
        try (IDao dao = IDaoService.newIDao()) {
            for (String suffix : attemptSuffixList) {
                String newCode = StrUtil.format("{}{}", viewInstCode, suffix);
                Cnd cnd = Cnd.NEW();
                cnd.where().andEquals(Form.Code, newCode);
                long countNo = IFormMgr.get().countForm(dao, viewCode, cnd);
                if (countNo > 0) return newCode;
            }


        } catch (Exception e) {
            return viewInstCode;
        }

        return viewInstCode;

    }


    private static class PublishTargetViewDto {
        private String viewModelCode;
        private String viewInstCode;
        private String viewName;

        public PublishTargetViewDto(String viewModelCode, String viewInstCode, String viewName) {
            this.viewModelCode = viewModelCode;
            this.viewInstCode = viewInstCode;
            this.viewName = viewName;
        }

        public String getViewModelCode() {
            return viewModelCode;
        }

        public PublishTargetViewDto setViewModelCode(String viewModelCode) {
            this.viewModelCode = viewModelCode;
            return this;
        }

        public String getViewInstCode() {
            return viewInstCode;
        }

        public PublishTargetViewDto setViewInstCode(String viewInstCode) {
            this.viewInstCode = viewInstCode;
            return this;
        }

        public String getViewName() {
            return viewName;
        }

        public PublishTargetViewDto setViewName(String viewName) {
            this.viewName = viewName;
            return this;
        }
    }


}
