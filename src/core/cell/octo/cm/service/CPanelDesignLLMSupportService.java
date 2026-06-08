package cell.octo.cm.service;

import bap.cells.BasicCell;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.llm.IHtmlGeneratorAction;
import cell.orchestration.service.IOrchestrationBuildService;
import cell.orchestration.service.IOrchestrationRuntimeService;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import octo.cm.dto.ErrorDto;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.util.*;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计LLM支撑服务实现类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-30", updateTime = "2025-06-30"
)
public class CPanelDesignLLMSupportService extends BasicCell implements IPanelDesignLLMSupportService {

    public static final EasyOperation Op = EasyOperation.get();

    // 智能体-面板设计生成器-信息管理
    public static final String AgentCode_PanelDesignGenerator_InformationMgr = "PANEL_DESIGN_GENERATOR_INFORMATION_MGR";
    // 智能体-面板设计生成器-流程处理
    public static final String AgentCode_PanelDesignGenerator_BusProcess = "PANEL_DESIGN_GENERATOR_BUS_PROCESS";


    @Override
    public Form llmInitPanelDesignAndPublish(Progress<?> progress, OctoDomainOpObserver observer,
                                             String panelCode, String userNeed) throws Exception {
        if (observer == null || StrUtil.isBlank(panelCode)) return null;
        if (progress == null) progress = Progress.newOutput();

        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_PanelDesign);
            cnd.where().andEquals(getFieldCode("面板编号"), panelCode);

            ResultSet<Form> pdRs = IFormMgr.get().queryFormPage(dao, FormModelId_PanelDesign, cnd, 1, 1, true, true);

            if (pdRs.isEmpty()) throw PanelDesignException.Builder.notFoundWithCode(panelCode);


            Form panelDesignForm = pdRs.getDataList().get(0);

            try {
                panelDesignForm = llmInitPanelDesign(progress, observer, panelDesignForm, userNeed);
            } catch (Exception e) {
                progress.sendProcess(100,
                        Progress_FLAG_WorkBench + StrUtil.format("初始化面板[{}]失败", panelCode),
                        true
                );

                throw e;
            }

            // 发布到应用
            try {
                PanelDesignPublishUtil.publish(progress,
                        null, observer, panelDesignForm);
            } catch (Exception e) {
                Op.logf(
                        "LLM初始化面板设计成功，不过发布出现报错:" + e.getMessage()
                );
                throw e;
            }


            return panelDesignForm;


        }

    }

    @Override
    public List<Form> llmInitPanelDesignAndPublishBatch(Progress<?> progress, OctoDomainOpObserver observer,
                                                        List<String> panelCodes, String userNeed) throws Exception {
        if (observer == null || Op.isEmpty(panelCodes)) return new ArrayList<>();
        if (progress == null) progress = Progress.newOutput();

        int currExecNo = 1;
        int taskTotalNo = panelCodes.size();
        Op.sendProgressMsg(progress, 0, StrUtil.format("批量初始化面板设计，任务数量:[{}]", taskTotalNo));

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        List<Form> resultList = new ArrayList<>();

        for (String panelCode : panelCodes) {

            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);

            childProgress.sendProcess(0,
                    Progress_FLAG_WorkBench + StrUtil.format("正在初始化 [{}/{}]", currExecNo++, taskTotalNo),
                    true
            );

            try {
                Form newPanelDesign = llmInitPanelDesignAndPublish(childProgress, observer, panelCode, userNeed);
                if (newPanelDesign != null) {
                    resultList.add(newPanelDesign);
                }
            } catch (Exception e) {
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("标准交付失败, 面板编号[{}]", panelCode),
                                ExceptionUtils.getFullStackTrace(e),
                                e
                        )
                );
                Op.logException(e);
            }

        }

        return resultList;

    }

    @Override
    public List<Form> llmInitPanelDesignBatch(Progress<?> progress, OctoDomainOpObserver observer, List<Form> publishedPanelDesigns, String userNeed) throws Exception {
        if (CollUtil.isEmpty(publishedPanelDesigns)) throw PanelDesignException.Builder.noPanelToInit();

        if (progress == null) progress = Progress.newOutput();

        int currExecNo = 1;
        int taskTotalNo = publishedPanelDesigns.size();
        Op.sendProgressMsg(progress, 0, StrUtil.format("批量初始化面板设计，任务数量:[{}]", taskTotalNo));

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        List<Form> resultList = new ArrayList<>();
        for (Form panelDesign : publishedPanelDesigns) {

            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);

            String panelName = panelDesign.getString("面板名称");
            childProgress.sendProcess(0,
                    Progress_FLAG_WorkBench + StrUtil.format("正在初始化[{}] [{}/{}]", panelName, currExecNo++, taskTotalNo),
                    true
            );


            try {

                Form newPanelDesign = llmInitPanelDesign(childProgress, observer, panelDesign, userNeed);
                if (newPanelDesign != null) {
                    resultList.add(newPanelDesign);
                }


            } catch (Throwable e) {
                String fullStackTrace = ExceptionUtils.getFullStackTrace(e);
                Op.logf(fullStackTrace);
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("大模型初始化失败, 面板名称[{}]", panelName),
                                fullStackTrace,
                                e
                        )
                );

                childProgress.sendProcess(100,
                        Progress_FLAG_WorkBench + StrUtil.format("大模型初始化失败: {}", e.getMessage()),
                        true
                );
            }

        }

        return resultList;
    }

    @Override
    public Form llmInitPanelDesign(Progress<?> progress,
                                   OctoDomainOpObserver observer, Form panelDesignForm, String userNeed) throws Exception {

        if (progress == null) progress = Progress.newOutput();

        if (observer == null) throw DomainException.Builder.observerNotFound();

        if (panelDesignForm == null) throw PanelDesignException.Builder.formEmpty();

        String panelCode = panelDesignForm.getString("面板编号");
        if (StrUtil.isBlank(panelCode)) throw PanelDesignException.Builder.panelCodeEmpty();

        // 目标初始化的场景
        boolean hasScene = true;
        AssociationData targetSceneAc = panelDesignForm.getAssociation("所属场景");
        if (targetSceneAc == null) hasScene = false;

        // 目标场景的面板分类
        AssociationData panelCategoryAc = panelDesignForm.getAssociation("面板分类");
        if (panelCategoryAc == null) throw PanelDesignException.Builder.categoryEmpty();
        Form categoryForm = panelCategoryAc.getForm();
        if (categoryForm == null)
            throw PanelDesignException.Builder.categoryNotFoundWithCode(panelCategoryAc.getValue());


        // 要初始化的目标场景
        String sceneCode = hasScene ? targetSceneAc.getValue() : "";
        String categoryName = categoryForm.getString("分类名称");
        try {

            progress.sendProcess(0, Progress_FLAG_WorkBench + "[大模型初始化] 开始调用大模型初始化能力..", true);
            progress.sendProcess(10, Progress_FLAG_WorkBench + "[大模型初始化] 耗时较长，请耐心等待", true);


            String outputCallMethodStrTemplate = "[大模型初始化] 用户初始化的面板分类为[{}], 调用[{}]";

            // 根据类型进行调用不同的智能体
            switch (categoryName) {
                case PanelCategoryUtil.CategoryType_InformationMgr:
                    // 调用【面板设计生成】
                    Op.sendProgressMsg(progress, 15, StrUtil.format(outputCallMethodStrTemplate, categoryName, "面板设计生成"));
                    doCallPanelDesignGenerator(progress, observer, sceneCode, AgentCode_PanelDesignGenerator_InformationMgr, userNeed);
                    break;
                case PanelCategoryUtil.CategoryType_ProcessHandle:
                    // 调用【面板设计生成】
                    Op.sendProgressMsg(progress, 15, StrUtil.format(outputCallMethodStrTemplate, categoryName, "面板设计生成"));
                    doCallPanelDesignGenerator(progress, observer, sceneCode, AgentCode_PanelDesignGenerator_BusProcess, userNeed);
                    break;
                case PanelCategoryUtil.CategoryType_DataBoard:
                    // 调用【数据看板生成】
                    Op.sendProgressMsg(progress, 15, StrUtil.format(outputCallMethodStrTemplate, categoryName, "数据看板生成"));
                    doCallDataBoardPageGenerator(progress, observer, panelDesignForm, userNeed);
                    break;
                default:
                    throw PanelDesignException.Builder.categoryNotSupported(categoryName);
//                    break;
            }


            progress.sendProcess(100, Progress_FLAG_WorkBench + "[大模型初始化] 初始化成功", true);


            try (IDao dao = IDaoService.newIDao()) {

                Form newPanelDesignForm = IFormMgr.get().queryFormByCode(dao, panelDesignForm.getFormModelId(),
                        panelDesignForm.getString(Form.Code));

                PanelDesignDependCleaningUtil.cleaning(dao, newPanelDesignForm);

                dao.commit();

                return newPanelDesignForm;

            }


        } catch (Exception e) {
            logf("大模型初始化失败，所属场景：{}, 面板名称:{}", sceneCode,
                    panelDesignForm == null ? "未知面板名称" : panelDesignForm.getString("面板名称"));
            throw e;
        }

    }

    // 调用【数据看板生成】
    private static void doCallDataBoardPageGenerator(Progress<?> progress, OctoDomainOpObserver observer, Form panelDesignForm, String userNeed) throws Exception {

        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");
        String panelDescription = panelDesignForm.getString("面板描述");

        String panelBusDescription = StrUtil.format(
                "面板名称:{}\n" +
                        "面板描述:{}\n",
                panelName, panelDescription
        );

        String generatedCode = IHtmlGeneratorAction.get()
                .generateCDPDashBoard(observer.getDomainCode(), panelCode, panelBusDescription, userNeed);


        try (IDao dao = IDaoService.newIDao()) {
            String webPageName = panelName;

            TableData webPageTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
            Form webPageForm = Op.newForm(webPageTd.getFormModelId());
            webPageForm.setAttrValue("页面名称", webPageName);
            webPageForm.setAttrValue("页面代码", generatedCode);


            webPageTd.add(webPageForm);

            panelDesignForm.setAttrValue("面板表格", null);
            panelDesignForm.setAttrValue("面板表单", null);
            panelDesignForm.setAttrValue("面板数据", null);
            panelDesignForm.setAttrValue("面板按钮", null);
            panelDesignForm.setAttrValue("面板网页", webPageTd);
            panelDesignForm.setAttrValue("页面入口", webPageName);

            IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);

            dao.commit();

        }


    }

    // 调用【面板设计生成】
    private static void doCallPanelDesignGenerator(Progress<?> progress, OctoDomainOpObserver observer, String sceneCode, String targetAgentCode, String userNeed) throws Exception {

        IOrchestrationBuildService orchestrationBuildService = IOrchestrationBuildService.get();
        Form agentForm = orchestrationBuildService.queryAgentForm(targetAgentCode);
        Map<String, String> paramMap = orchestrationBuildService.getDefaultParamMap(agentForm);
        if (paramMap != null) {
            paramMap.put("目标场景", sceneCode);
            paramMap.put(ParamKey_BUS_DOMAIN_UUID, observer.getDomainUuid());
            paramMap.put(ParamKey_BUS_DOMAIN_CODE, observer.getDomainCode());
        }
        Form instanceSync = IOrchestrationRuntimeService.get()
                .createInstanceSync(
                        progress,
                        targetAgentCode,
                        userNeed,
                        IdUtil.fastUUID(),
                        paramMap,
                        IdUtil.fastUUID(),
                        null,
                        5 * 60 * 1000
                );
    }


    // ========================= 支撑方法 =========================


}
