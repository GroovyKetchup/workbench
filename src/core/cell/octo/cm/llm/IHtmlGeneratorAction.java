package cell.octo.cm.llm;


import ai.agent.dto.groupChat.admin.ModelCallingLogDto;
import ai.agent.engine.groupChat.model.definition.LLMConfig;
import ai.agent.service.groupChat.manager.LLMConfigManager;
import ai.agent.service.groupChat.manager.OrchManager;
import ai.agent.service.llmCalling.HttpLlmClient;
import bap.cells.Cells;
import cell.ai.agent.IGroupChatModelCallingService;
import cell.bap.servlet.CHttpServlet;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.cdp.http.IWebPageCDPController;
import cell.gpf.adur.data.IFormMgr;
import cell.jit.ActionIntf;
import cell.octo.cm.expr.PanelDesignExpr;
import cmn.anotation.ClassDeclare;
import cmn.http.servlet.mapping.RequestMappingContext;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.dfc.gui.LvUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.panelDesign.PanelDataFieldDto;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.util.EasyOperation;
import octo.cm.util.TextTemplateUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.util.*;

@Comment("Html页面生成动作")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-11-11", updateTime = "2025-11-11"
)
public interface IHtmlGeneratorAction extends ActionIntf {

    EasyOperation Op = EasyOperation.get();

    static IHtmlGeneratorAction get() {
        return Cells.get(IHtmlGeneratorAction.class);
    }

    String FormModelId_Prompt = "octocm.md.MutiAgentGroupChat.iML_00009_CM";
    String PromptName_GenerateDataDashBoard = "数据大屏生成";
    String PromptName_CDP_GenerateDataDashBoard = "CDP_数据看板生成";
    String PromptName_CDP_GenerateCustomPage_Normal = "CDP_个性交付生成_通用";
    String PromptName_CDP_GenerateCustomPage_DashBoard = "CDP_个性交付生成_数据看板";
    String PromptName_BusRelevantPanelCodeDetermine = "业务有关面板判定";

    String LLMModelConfigName = "【工具专用】数据看板类面板简装";

    // 生成CDP数据大屏
    default String generateCDPDashBoard(String busDomainCode, String panelCode,
                                        String panelBusDescription, String userNeed) {
        return generateEveryThing(busDomainCode, panelCode, panelBusDescription,
                userNeed, PromptName_CDP_GenerateDataDashBoard, true);
    }

    // 生成CDP个性交付-标准版本
    // 不需要获取依赖面板编号，就是自己
    default String generateCDPCustomWithNormalVersion(String busDomainCode, String panelCode,
                                                      String panelBusDescription, String userNeed) {
        return generateEveryThing(busDomainCode, panelCode, panelBusDescription,
                userNeed, PromptName_CDP_GenerateCustomPage_Normal, false);
    }

    // 生成CDP个性交付-数据看板版本
    // 需要获取依赖的面板编号
    default String generateCDPCustomWithDashBoardVersion(String busDomainCode, String panelCode,
                                                         String panelBusDescription, String userNeed) {
        return generateEveryThing(busDomainCode, panelCode, panelBusDescription,
                userNeed, PromptName_CDP_GenerateCustomPage_DashBoard, true);
    }


    // ========================= 支撑方法 =========================

    // 生成所有东西
    default String generateEveryThing(String busDomainCode, String panelCode,
                                      String panelBusDescription, String userNeed,
                                      String targetPrompt, boolean enableMatchRelevantPanelCode) {

        if (StrUtil.hasBlank(busDomainCode, panelCode)) throw DomainException.Builder.codeAndPanelCodeEmpty();


        String prompt = getPrompt(targetPrompt);
        if (StrUtil.isBlank(prompt))
            throw new RuntimeException(StrUtil.format("无法找到提示词[{}]", targetPrompt));

        try (IDao dao = IDaoService.newIDao()) {

            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);

            List<String> relevantPanelCodes = null;
            // 如果开启了就走一次调用
            if (enableMatchRelevantPanelCode) {
                relevantPanelCodes = getRelevantPanelCodesByBusInfo(busDomainCode, panelCode, panelBusDescription, userNeed);
            }

            // 如果没有开启相关性匹配或匹配失败/没有相关面板编号，
            // 那么默认相关面板编号就是自己
            if (relevantPanelCodes == null || relevantPanelCodes.isEmpty()) {
                relevantPanelCodes = CollUtil.newArrayList(panelCode);
            }

            log("relevantPanelCodes:{}", relevantPanelCodes);


            Map<String, String> contextMap = new HashMap<>();
            contextMap.put("busDomainCode", busDomainCode);
            contextMap.put("业务相关信息", panelBusDescription);


            List<Object> sourceDataSchemas = new ArrayList<>();

            IWebPageCDPController cdp = Cells.get(IWebPageCDPController.class);
            cdp.setContext(new RequestMappingContext().setHttpServlet(new CHttpServlet(null, null)));

            for (String relevantPanelCode : relevantPanelCodes) {
                PanelDesignExpr panelDesignExpr = Cells.get(PanelDesignExpr.class);
                JSONObject panelDesignObj = panelDesignExpr
                        .getPanelDesignData(busDomainCode, relevantPanelCode);
                if (panelDesignObj != null) {
                    panelDesignObj.set("面板网页", null);
                    sourceDataSchemas.add(panelDesignObj);
                }
            }


            String panelSchemasStr = JSONUtil.toJsonStr(sourceDataSchemas);
            contextMap.put("来源数据结构", panelSchemasStr);
            contextMap.put("current_panel_config", panelSchemasStr);

            contextMap.put("面板数据", panelSchemasStr);
            contextMap.put("用户需求", userNeed);

            prompt = TextTemplateUtil.fillInParams(prompt, contextMap);

//            LLMConfig llmConfig = LLMConfigManager.getAutoLlmConfig();
            LLMConfig llmConfig = getLLMConfig();
            assert llmConfig != null;

            String resp = new HttpLlmClient().callLlm(llmConfig, prompt, userNeed);
            resp = clearLlmRespondTag(resp);
            // 添加生成调用日志
            IGroupChatModelCallingService.get().addFullTracebackLog(
                    new ModelCallingLogDto()
                            .setCaller(StrUtil.format("系统[{}({})]", llmConfig.getConfigName(), llmConfig.getModel()))
                            .setMessageWindow(prompt)
                            .setResponseContent(resp)
                            .setCreateTime(System.currentTimeMillis())
                            .setTrigger(targetPrompt)
            );

            dao.commit();

            return resp;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }


    // ========================= 支撑方法 =========================


    default void doUpdateNewRelevantFieldToPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode, List<String> relevantPanelCodes) {

        if (Op.isEmpty(relevantPanelCodes)) return;

        try {
            Form paneldesignForm = doQueryPanelDesign(dao, observer, panelCode);
            if (paneldesignForm == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            IPanelDesignGeneratorAction generatorAction = Cells.get(IPanelDesignGeneratorAction.class);


            List<PanelDataFieldDto> fields = new ArrayList<>();
            for (String relevantPanelCode : relevantPanelCodes) {

                String fieldStyle = StrUtil.format("表格('{}_CM')", relevantPanelCode);
                PanelDataFieldDto fieldDto = new PanelDataFieldDto();
                fieldDto.setFieldName("业务信息")
                        .setFieldName(relevantPanelCode)
                        .setFieldStyle(fieldStyle)
                        .setSourceSceneDataCode(relevantPanelCode)
                        .setSourceSceneDataFieldName(relevantPanelCode)
                        .setSourceSceneDataFieldStyle(fieldStyle);

                fields.add(fieldDto);

            }

            log("doUpdateNewRelevantFieldToPanelDesign:{}", JSONUtil.toJsonStr(fields));

            generatorAction.doUpdateDataDtoToPanelDesignForm(dao, observer, paneldesignForm, fields);


        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }


    }

    default List<String> tryGetRelevantPanelCodesByPanelDesignForm(IDao dao, OctoDomainOpObserver observer, String panelCode) {

        try {
            Form paneldesignForm = doQueryPanelDesign(dao, observer, panelCode);
            if (paneldesignForm == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            TableData panelDataTd = paneldesignForm.getTable("面板数据");
            if (Op.isEmpty(panelDataTd)) throw PanelDesignException.Builder.panelDataEmpty();

            List<String> relevantPanelCodes = new ArrayList<>();
            for (Form row : panelDataTd.getRows()) {
                String fieldStyle = row.getString("属性样式");
                if (StrUtil.isBlank(fieldStyle)) continue;
                if (!fieldStyle.contains("表格")) continue;
                // 使用正则提取从“表格('IML_00001_CM')”提取出IML_00001
                String matchResult = fieldStyle.replaceAll("表格\\('(.*?)_CM'\\)", "$1");
                if (StrUtil.isBlank(matchResult)) continue;
                relevantPanelCodes.add(matchResult);

            }


            return relevantPanelCodes;

        } catch (Exception e) {
            return null;
        }


    }


    default List<String> getRelevantPanelCodesByBusInfo(String busDomainCode, String panelCode, String panelBusDescription, String userNeed) {

        try {
            String prompt = getPrompt(PromptName_BusRelevantPanelCodeDetermine);


            Object panelList = doQueryAllPanelDesigns(busDomainCode, panelCode);


            Map<String, String> contextMap = new HashMap<>();
            contextMap.put("业务信息", panelBusDescription);
            contextMap.put("用户需求", StrUtil.isBlank(userNeed) ? "" : userNeed);
            contextMap.put("面板列表", JSONUtil.toJsonStr(panelList));


            prompt = TextTemplateUtil.fillInParams(prompt, contextMap);

            LLMConfigManager.loadModels();
//            LLMConfig llmConfig = LLMConfigManager.getAutoLlmConfig(CollUtil.newArrayList("gemini-2.5-flash"));
            LLMConfig llmConfig = getLLMConfig();
            assert llmConfig != null;

            String resp = new HttpLlmClient().callLlm(llmConfig, prompt, userNeed);

            // 添加生成调用日志
            IGroupChatModelCallingService.get().addFullTracebackLog(
                    new ModelCallingLogDto()
                            .setCaller("系统")
                            .setMessageWindow(prompt)
                            .setResponseContent(resp)
                            .setCreateTime(System.currentTimeMillis())
                            .setTrigger(PromptName_GenerateDataDashBoard)
            );

            return Arrays.asList(resp.split(","));
        } catch (Exception e) {
            LvUtil.trace("getRelevantPanelCodesByBusInfo:" + ExceptionUtils.getFullStackTrace(e));
            return null;
        }

    }


    // 获取提示词
    default String getPrompt(String promptName) {

        if (StrUtil.isBlank(promptName)) return null;
        try (IDao dao = IDaoService.newIDao()) {
            Form promptForm = Op.queryFormByCondition(dao, FormModelId_Prompt,
                    "提示词名称", promptName, null);
            if (promptForm == null) return null;
            return promptForm.getString("提示词内容");


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    default Object doQueryAllPanelDesigns(String busDomainCode, String panelCode) {
        try (IDao dao = IDaoService.newIDao()) {
            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
            String formModelId = WorkBenchConst.FormModelId_PanelDesign;
            Cnd cnd = Op.getBusDomainFilterCondition(observer, formModelId);

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, Integer.MAX_VALUE, true, false);


            List<JSONObject> resultList = new ArrayList<>();

            for (Form form : queryRs.getDataList()) {

                JSONObject jsonObject = new JSONObject();
                String panelCode1 = form.getString("面板编号");
                if (panelCode.equals(panelCode1)) continue;
                jsonObject.set("面板编号", panelCode1);
                jsonObject.set("面板名称", form.getString("面板名称"));
                jsonObject.set("面板描述", form.getString("面板描述"));

                resultList.add(jsonObject);

            }


            return resultList;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    default LLMConfig getLLMConfig() {

        try (IDao dao = IDaoService.newIDao()) {
            List<LLMConfig> llmConfigs = OrchManager.queryAllLLMConfig(dao);
            if (CollUtil.isEmpty(llmConfigs)) throw new RuntimeException("系统内没有任何可用的大模型");


            for (LLMConfig llmConfig : llmConfigs) {
                if (LLMModelConfigName.equals(llmConfig.getConfigName())) {
                    return llmConfig;
                }
            }

            return llmConfigs.get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    // 简单移除标记
    default String clearLlmRespondTag(String generateResult) {
        if (StrUtil.isBlank(generateResult)) return generateResult;
        generateResult = generateResult.replaceAll("```html", "");
        generateResult = generateResult.replaceAll("```json", "");
        generateResult = generateResult.replaceAll("```", "");
        return generateResult.trim();
    }


    default Form doQueryPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode) {
        try {
            String targetFormModelId = WorkBenchConst.FormModelId_PanelDesign;
            Cnd cnd = Op.getBusDomainFilterCondition(observer, targetFormModelId);

            cnd.where().and(
                    new SqlExpressionGroup()
                            .or(new SqlExpressionGroup().andEquals(Op.getFieldCode("面板编号"), panelCode))
                            .or(new SqlExpressionGroup().andEquals(Form.Code, panelCode))
            );


            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, targetFormModelId, cnd, 1, 1,
                    false, true);

            if (queryRs.isEmpty()) return null;

            return queryRs.getDataList().get(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    default void log(CharSequence template, Object... params) {
        LvUtil.trace(StrUtil.format(template, params));
    }


}
