package cell.octo.cm.llm;


import ai.webPage.dto.RespondDto;
import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.cdp.http.IWebPageCDPController;
import cell.gpf.adur.data.IFormMgr;
import cell.jit.ActionIntf;
import cell.rapidView.function.CommonFunctions;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.utils.FormValueUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.common.util.GsonUtil;
import gpf.adur.data.*;
import gpf.dc.runtime.PDCForm;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.panelDesign.*;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.exception.business.SceneException;
import octo.cm.util.*;
import octocm.basic.consts.OctoCM2BasicConst;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.consts.OctoCM2WorkBenchConst;
import orchestration.dto.OrchestrationActionResultDto;
import orchestration.dto.OrchestrationRuntimeContextDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;

import java.util.*;
import java.util.stream.Collectors;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计LLM动作")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-21", updateTime = "2025-08-21"
)
public interface IPanelDesignGeneratorAction extends ActionIntf, CommonFunctions {

    EasyOperation Op = EasyOperation.get();

    // 节点名
    String NodeName_Context_Initial = "上下文初始化";

    // 字段名
    String FieldName_BusDomain_FieldStyle = "业务域_属性样式";
    String FieldName_BusDomain_OpFunction = "业务域_操作函数";
    String FieldName_BusDomain_FormModel = "业务域_模型列表";
    String FieldName_BusDomain_PanelButton = "业务域_面板按钮列表";

    String FieldName_Scene_Introduce = "场景_介绍";
    String FieldName_Scene_Data = "场景_数据";
    String FieldName_Scene_Behavior = "场景_行为";
    String FieldName_Scene_Constraint = "场景_约束";
    String FieldName_Scene_UseRole = "场景_使用角色";
    String FieldName_Scene_Orchestration = "场景_编排";


    // 用于子场景与父场景，或场景构成（数据、行为、编排）与场景的关系
    String FieldName_UpperSceneCode = "上层场景编号";
    // 【最终数据处理】节点存储的最终的数据
    String FieldName_FinalData = "最终数据";


    // 想要发布的场景编号
    // 注意：该编号是场景的Form.Code，而非“场景编号”
    String ParamKey_TargetSceneCode = "目标场景";
    String ParamKey_TargetPanelCode = "目标面板";
    String ParamKey_WebPageGeneratedCode = "代码生成.输出内容";
    String ParamKey_DataPlanDemoData = "数据规划.输出内容";


    @MethodDeclare(
            label = "动态加载提示词",
            what = "", why = "", how = "从参数表【提示词_${节点名称}】动态取提示词，并自动加载变量",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-21", updateTime = "2025-08-21",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default String dynamicLoadPrompt(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();
        String nodeName = contextDto.getNodeName();
        String prompt = getParamValueFromLlmDataForm(llmDataForm,
                StrUtil.format("提示词_{}", nodeName));

        if (StrUtil.isBlank(prompt)) throw new RuntimeException(StrUtil.format("无法获取节点{}的提示词\n{}", nodeName, JSONUtil.toJsonStr(llmDataForm)));

        // 获取上下文数据Map
        // 所有的上下文都会存储到 @see NodeName_Context_Initial 对应的表单里
        Map<String, String> contextMap = getContextDataMap(llmDataForm);
//        if (true) throw new RuntimeException("contextMap:\n" + JSONUtil.toJsonStr(contextMap));
        // 将上下文的内容注入到提示词中（如果提示词中有使用双花括号（{{变量名}}）声明变量！）
        prompt = TextTemplateUtil.fillInParams(prompt, contextMap);

        return getBasicPrompt() + prompt;


    }


    @MethodDeclare(
            label = "加载业务域上下文",
            what = "", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-21", updateTime = "2025-08-21",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default OrchestrationActionResultDto loadBusDomainContext(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();

        // 加载业务域-属性样式
        loadBusDomainFieldStyle(llmDataForm);
        // 加载业务域-操作函数
        loadBusDomainOpFunction(llmDataForm);
        // 加载业务域-模型列表
        loadBusDomainModel(llmDataForm);
        // 加载业务域-面板按钮列表
        loadBusDomainPanelButton(llmDataForm);


        // ...

        return new OrchestrationActionResultDto((PDCForm) llmDataForm);


    }

    @MethodDeclare(
            label = "加载场景上下文",
            what = "", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-21", updateTime = "2025-08-21",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default OrchestrationActionResultDto loadSceneContext(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();

        // 要发布的目标场景
        String targetSceneCode = getParamValueFromLlmDataForm(
                llmDataForm,
                ParamKey_TargetSceneCode
        );

        // 目标场景从参数传递进来
        if (StrUtil.isBlank(targetSceneCode)) throw SceneException.Builder.cannotDetermineTarget();

        // 加载场景-介绍
        loadSceneIntroduce(llmDataForm, targetSceneCode);
        // 加载场景-数据
        loadSceneData(llmDataForm, targetSceneCode);
        // 加载场景-行为
        loadSceneBehavior(llmDataForm, targetSceneCode);
        // 加载场景-约束
        loadSceneConstraint(llmDataForm, targetSceneCode);
        // 加载场景-使用角色
        loadSceneUseRole(llmDataForm, targetSceneCode);
        // 加载场景-编排
        loadSceneOrchestration(llmDataForm, targetSceneCode);


        // ...

        return new OrchestrationActionResultDto((PDCForm) llmDataForm);


    }


    @MethodDeclare(
            label = "最终数据处理",
            what = "将之前节点汇总出来的数据都填充到【最终数据】中", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-22", updateTime = "2025-08-22",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default OrchestrationActionResultDto finalDataHandle(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();

        // 要发布的目标场景
        String targetSceneCode = getParamValueFromLlmDataForm(
                llmDataForm,
                ParamKey_TargetSceneCode
        );

        // 目标场景从参数传递进来
        if (StrUtil.isBlank(targetSceneCode)) throw SceneException.Builder.cannotDetermineTarget();

        // 从llmDataForm中取出【xxx.响应内容】
        Map<String, JSON> collectRespondMap = doCollectResponds(llmDataForm);

        if (collectRespondMap == null || collectRespondMap.isEmpty())
            throw CommonException.Builder.llmCollectFailed();


        // 用FastJson的注解标注了Dto，所以这里也要用FastJson进行处理
        // 注意：系统底层默认并不提供FastJson，需要自行引入
        String finalDataJsonStr = fastJsonToJsonString(collectRespondMap);

        // 不管后面会不会用到，先存一下
        setLlmFormFieldValue(llmDataForm, contextDto.getNodeName(),
                FieldName_FinalData, finalDataJsonStr);

        PanelDesignDto panelDesignDto = fastJsonParseObject(
                finalDataJsonStr,
                PanelDesignDto.class
        );

        panelDesignDto.setSourceSceneCode(targetSceneCode);


        // 已经构建了Dto，那么要进行更新到PanelDesign了
        doUpdatePanelDesign(llmDataForm, panelDesignDto);


        return new OrchestrationActionResultDto((PDCForm) llmDataForm);


    }


    @MethodDeclare(
            label = "面板网页最终处理",
            what = "处理大模型初始化的面板网页代码", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-28", updateTime = "2025-08-28",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default OrchestrationActionResultDto finalPanelWebPageHandle(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();
        OctoDomainOpObserver observer = getOctoDomainOpObserver(llmDataForm);

        // 要发布的目标场景
        String targetSceneCode = getParamValueFromLlmDataForm(
                llmDataForm,
                ParamKey_TargetSceneCode
        );

        // 目标场景从参数传递进来
        if (StrUtil.isBlank(targetSceneCode)) throw SceneException.Builder.cannotDetermineTarget();

        try (IDao dao = IDaoService.newIDao()) {

            // 首先查询面板设计Form
            Form panelDesignForm = doQueryPanelDesignFormBySceneCode(dao, targetSceneCode);

            // 查看生成出来的代码
            String generatedCode = getLlmFormFieldStringValue(llmDataForm,
                    ParamKey_WebPageGeneratedCode, String.class);

            // 网页所使用的样例数据, 后续开发事件的时候以这个作为标准
            String usedDemoData = getLlmFormFieldStringValue(llmDataForm,
                    ParamKey_DataPlanDemoData, String.class);

            if (StrUtil.isBlank(generatedCode)) throw new RuntimeException("生成的代码为空");

            generatedCode = clearLlmRespondTag(generatedCode);

            String panelName = panelDesignForm.getString("面板名称");
            String webPageName = panelName;

            TableData webPageTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
            Form webPageForm = Op.newForm(webPageTd.getFormModelId());
            webPageForm.setAttrValue("页面名称", webPageName);
            webPageForm.setAttrValue("页面说明", StrUtil.format("网页所使用的样例数据:\n{}", usedDemoData));
            webPageForm.setAttrValue("页面代码", generatedCode);

            // 预制好数据查询的事件
            Form dataQueryEventForm = PanelDesignCommonFormUtil.createQuicklySimpleEvent(dao, observer, panelDesignForm,
                    "自定义查询", "实现该事件进行数据查询", "自定义查询",
                    "常量文本('{}')"
            );

            // 这里还要注册一下事件
            PanelDesignCommonFormUtil.addPanelEvent(panelDesignForm, dataQueryEventForm);

            webPageForm.setAttrValue("事件集合",
                    CollUtil.newArrayList(
                            Op.toAssociationData(dataQueryEventForm)
                    )
            );


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


        return new OrchestrationActionResultDto((PDCForm) llmDataForm);


    }


    @MethodDeclare(
            label = "当不需要业务编排时打断",
            what = "", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-28", updateTime = "2025-08-28",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default OrchestrationActionResultDto interruptWhenNoNeedPanelBusOrch(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();

        // 要发布的目标场景
        String targetSceneCode = getParamValueFromLlmDataForm(
                llmDataForm,
                ParamKey_TargetSceneCode
        );

        try (IDao dao = IDaoService.newIDao()) {

            // 首先查询面板设计Form
            Form panelDesignForm = doQueryPanelDesignFormBySceneCode(dao, targetSceneCode);

            // 目标场景的面板分类
            String categoryName = PanelCategoryUtil.getPanelCategory(panelDesignForm);

            if (!"流程处理".equals(categoryName)) {
                return new OrchestrationActionResultDto()
                        .setForm((PDCForm) llmDataForm)
                        .setInterrupt(true);
            }


        }


        return new OrchestrationActionResultDto()
                .setForm((PDCForm) llmDataForm)
                .setInterrupt(false);


    }


    @MethodDeclare(
            label = "加载数据大屏初始化代码",
            what = "处理大模型初始化的面板网页代码", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-08-28", updateTime = "2025-08-28",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
            }
    )
    default String loadDashBoardPrompt(OrchestrationRuntimeContextDto contextDto) throws Exception {

        Form llmDataForm = contextDto.getDataForm();
        OctoDomainOpObserver observer = getOctoDomainOpObserver(llmDataForm);

        // 提示词
        String prompt = getParamValueFromLlmDataForm(llmDataForm, "提示词_代码生成");
        if (StrUtil.isBlank(prompt)) throw CommonException.Builder.llmPromptNotFound();

        // 要发布的目标面板
        String targetPanelCode = getParamValueFromLlmDataForm(llmDataForm, ParamKey_TargetPanelCode);
        if (StrUtil.isBlank(targetPanelCode)) throw PanelDesignException.Builder.cannotDetermineTarget();


        Map<String, String> contextMap = new HashMap<>();

        String apiBaseUrl = "http://14.18.100.250:14089/";
        contextMap.put("panelCode", targetPanelCode);
        contextMap.put("apiBaseUrl", apiBaseUrl);
        contextMap.put("busDomainCode", observer.getDomainCode());
        contextMap.put("需求规划.输出内容", getLlmFormFieldStringValue(llmDataForm,
                "需求规划.输出内容", String.class)
        );


        List<Object> sourceDataSchemas = new ArrayList<>();

//        CWebPageCDPController cdp = Cells.get(CWebPageCDPController.class);
        IWebPageCDPController cdp = Cells.get(IWebPageCDPController.class);
        RespondDto<Object> respondDto = cdp.getPanelConfig(observer.getDomainCode(), targetPanelCode);

        if (respondDto != null && respondDto.getData() != null) {
            sourceDataSchemas.add(respondDto.getData());
        }

        contextMap.put("来源数据结构", JSONUtil.toJsonStr(sourceDataSchemas));


        prompt = TextTemplateUtil.fillInParams(prompt, contextMap);


        return getBasicPrompt() + prompt;


    }


    // ========================= 支撑方法 =========================


    // 加载场景-编排
    default void loadSceneOrchestration(Form llmDataForm, String targetSceneCode) throws Exception {
        Cnd searchCnd = Cnd.NEW();
        searchCnd.where().andEquals(getFieldCode(FieldName_UpperSceneCode), targetSceneCode);

        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_SceneLayer_Orchestration,
                searchCnd,
                FieldName_Scene_Orchestration
        );
    }

    // 加载场景-使用角色
    default void loadSceneUseRole(Form llmDataForm, String targetSceneCode) throws Exception {

        try (IDao dao = IDaoService.newIDao()) {
            // 1、拿到场景
            Form sceneForm = queryFormByAssignField(dao, WorkBenchConst.FormModelId_SceneLayer,
                    Form.Code, targetSceneCode);
            if (sceneForm == null) return;

            // 2、取出使用角色
            List<Form> beUsedRoleForms = queryFormsByAcs(dao,
                    sceneForm.getAssociations("使用角色"));
            if (isEmpty(beUsedRoleForms)) return;

            JSONArray useRoleArr = convertFormsToJsonArray(beUsedRoleForms);

            setLlmFormFieldValue(llmDataForm, NodeName_Context_Initial, FieldName_Scene_UseRole,
                    useRoleArr.toString());

        }


    }

    // 加载场景-约束
    default void loadSceneConstraint(Form llmDataForm, String targetSceneCode) throws Exception {
        Cnd searchCnd = Cnd.NEW();
        searchCnd.where().andEquals(getFieldCode(FieldName_UpperSceneCode), targetSceneCode);

        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_SceneLayer_Constraint,
                searchCnd,
                FieldName_Scene_Constraint
        );
    }

    // 加载场景-行为
    default void loadSceneBehavior(Form llmDataForm, String targetSceneCode) throws Exception {
        Cnd searchCnd = Cnd.NEW();
        searchCnd.where().andEquals(getFieldCode(FieldName_UpperSceneCode), targetSceneCode);

        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_SceneLayer_Behavior,
                searchCnd,
                FieldName_Scene_Behavior
        );
    }

    // 加载场景-数据
    default void loadSceneData(Form llmDataForm, String targetSceneCode) throws Exception {
        Cnd searchCnd = Cnd.NEW();
        searchCnd.where().andEquals(getFieldCode(FieldName_UpperSceneCode), targetSceneCode);

        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_SceneLayer_Data,
                searchCnd,
                FieldName_Scene_Data
        );
    }

    // 加载场景-介绍
    // FIXME 后面可能需要调整，现在比较粗暴，直接把场景的全部信息塞了进去
    default void loadSceneIntroduce(Form llmDataForm, String targetSceneCode) throws Exception {

        Cnd searchCnd = Cnd.NEW();
        searchCnd.where().andEquals(Form.Code, targetSceneCode);

        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_SceneLayer,
                searchCnd,
                FieldName_Scene_Introduce
        );
    }


    // 加载业务域-面板按钮列表
    default void loadBusDomainPanelButton(Form llmDataForm) throws Exception {
        Cnd cnd = Cnd.NEW();
        cnd.where().andEquals(
                getFieldCode(FieldName_CategoryLabel), Text_SystemDefaultButton
        );
        doQueryAndFillInFormToContext(
                llmDataForm,
                WorkBenchConst.FormModelId_Axis_Button,
                cnd,
                FieldName_BusDomain_PanelButton
        );

    }

    // 加载业务域-模型列表
    default void loadBusDomainModel(Form llmDataForm) throws Exception {
        // 1、根据指定的模型ID+业务域过滤条件+用户自定义的条件，进行查询
        List<Form> existedPanelDesignForms = queryFormsWithBusDomainFilter(llmDataForm,
                FormModelId_PanelDesign, null);
        if (isEmpty(existedPanelDesignForms)) return;

        List<JSONObject> models = new ArrayList<>();
        try (IDao dao = IDaoService.newIDao()) {
            for (Form panelDesignForm : existedPanelDesignForms) {
                String panelCode = panelDesignForm.getString("面板编号");
                String panelName = panelDesignForm.getString("面板名称");
                String panelDesc = panelDesignForm.getString("面板描述");
                if (StrUtil.isBlank(panelCode)) continue;
                TableData panelDataTd = panelDesignForm.getTable("面板数据");
                // 模型对象
                JSONObject modelObj = new JSONObject();
                // 模型可供显示的属性列表
                JSONArray panelDataArr = new JSONArray();

                modelObj.set("CM模型名称", StrUtil.format("{}_CM", panelCode));
                modelObj.set("面板介绍", StrUtil.format("名称:{}\n介绍:{}", panelName, panelDesc));
                if (!Op.isEmpty(panelDataTd)) {
                    List<AssociationData> attrImplAcs = new ArrayList<>();
                    for (Form panelData : panelDataTd.getRows()) {
                        AssociationData attrImplAc = panelData.getAssociation("属性实现");
                        if (attrImplAc == null) continue;
                        attrImplAcs.add(attrImplAc);
                    }

                    List<Form> attrImpls = Op.queryFormsByAcs(dao, attrImplAcs);
                    if (Op.isEmpty(attrImpls)) continue;
                    for (Form attrImpl : attrImpls) {
                        String attrName = attrImpl.getString("属性名称");
                        String attrStyle = attrImpl.getString("属性样式");
                        if (StrUtil.hasBlank(attrName, attrStyle)) continue;

                        // 只接受文本样式的关联显示！
                        if (!attrStyle.equals("文本")) continue;
                        panelDataArr.add(attrName);

                    }

                }

                modelObj.set("可供选择的显示属性", panelDataArr);

                models.add(modelObj);


            }
        }


        setLlmFormFieldValue(llmDataForm, NodeName_Context_Initial, FieldName_BusDomain_FormModel,
                JSONUtil.toJsonStr(models)
        );


        return;
    }

    // 加载业务域-操作函数
    default void loadBusDomainOpFunction(Form llmDataForm) throws Exception {

        doQueryAndFillInFormToContext(
                llmDataForm,
                OctoCM2BasicConst.MODEL_ID_操作轴,
                null,
                FieldName_BusDomain_OpFunction
        );


    }

    // 加载业务域-属性样式
    default void loadBusDomainFieldStyle(Form llmDataForm) throws Exception {

        doQueryAndFillInFormToContext(
                llmDataForm,
                OctoCM2WorkBenchConst.MODEL_ID_属性样式数据,
                null,
                FieldName_BusDomain_FieldStyle
        );


    }


    // 更新PanelDesign
    // 到这一步PanelDesign一定已经初始化了，如果没有初始化就抛异常
    default void doUpdatePanelDesign(Form llmDataForm, PanelDesignDto panelDesignDto) throws Exception {
        if (llmDataForm == null || panelDesignDto == null) return;
        String sourceSceneCode = panelDesignDto.getSourceSceneCode();
        if (StrUtil.isBlank(sourceSceneCode)) return;

        try (IDao dao = IDaoService.newIDao()) {

            // 首先查询面板设计Form
            Form panelDesignForm = doQueryPanelDesignFormBySceneCode(dao, sourceSceneCode);

            assert panelDesignForm != null;

            boolean isProcessHandleCategory = PanelCategoryUtil.isProcessHandleCategory(panelDesignForm);

            OctoDomainOpObserver observer = getOctoDomainOpObserver(llmDataForm);

            // 将PanelDataDto更新到表单
            // 实际插入的字段
            Set<String> actualFieldNames = doUpdateDataDtoToPanelDesignForm(dao, observer, panelDesignForm, panelDesignDto.getPanelData());

            // 将PanelButtonDto更新到表单
            Set<String> actualBtnNames = doUpdateButtonDtoToPanelDesignForm(dao, observer, panelDesignForm, panelDesignDto.getPanelButton(), isProcessHandleCategory);

            // 将PanelBusOrchDto更新到表单
            doUpdateBusOrchDtoToPanelDesignForm(dao, observer, panelDesignForm, panelDesignDto.getBusOrch(), isProcessHandleCategory);

            // 将PanelTableDto更新到表单
            // FIXME 副作用：这里会更新表格名称为页面入口
            doUpdateTableDtoToPanelDesignForm(dao, observer, panelDesignForm, panelDesignDto.getPanelButton(),
                    panelDesignDto.getPanelTable(), actualFieldNames, actualBtnNames, isProcessHandleCategory);

            // 补充新增操作按钮
            // 临时逻辑,后续移除
            if (isProcessHandleCategory) {
                String createBtnName = doSetDefaultCreateOperate(dao, observer, panelDesignForm);
                actualBtnNames.add(createBtnName);
            }


            // 将PanelFormDto更新到表单
            doUpdateFormDtoToPanelDesignForm(dao, observer, panelDesignForm, panelDesignDto.getPanelButton(),
                    panelDesignDto.getPanelForm(), actualFieldNames, actualBtnNames, isProcessHandleCategory);


            // 基于PanelBusOrchDto简单构建权限
            // 临时逻辑,后续移除
            doSetSimplePanelPermissionByBusOrch(dao, observer, panelDesignForm,
                    panelDesignDto.getBusOrch(), actualFieldNames, actualBtnNames, isProcessHandleCategory);


            // ...

            IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);

            dao.commit();

        }


    }

    // 补充新增操作按钮
    default String doSetDefaultCreateOperate(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return null;

        // 补充默认的新增按钮
        Form createButton = PanelDesignCommonFormUtil.getOrCreateDefaultCreateButton(dao, observer, panelDesignForm);
        if (createButton == null) throw new RuntimeException("未能成功的创建【创建】按钮");

        String createBtnName = createButton.getString("按钮名称");

        TableData panelBtnTd = panelDesignForm.getTable("面板按钮");
        if (!isEmpty(panelBtnTd)) {

            String btnAlias = createButton.getString("别名");
            String btnDesc = createButton.getString("按钮说明");

            Form newBtnForm = Op.newForm(panelBtnTd.getFormModelId());
            newBtnForm.setAttrValue("按钮别名", btnAlias);
            newBtnForm.setAttrValue("按钮说明", btnDesc);
            newBtnForm.setAttrValue("面板按钮", Op.toAssociationData(createButton));

            panelBtnTd.add(newBtnForm);

            panelDesignForm.setAttrValue("面板按钮", panelBtnTd);
        }

        TableData panelTableTd = panelDesignForm.getTable("面板表格");
        if (!isEmpty(panelTableTd)) {
            for (Form tableForm : panelTableTd.getRows()) {
                String menuBtnStr = tableForm.getString("菜单");
                if (StrUtil.isBlank(menuBtnStr)) menuBtnStr = createBtnName;
                else menuBtnStr = menuBtnStr + "," + createBtnName;
                tableForm.setAttrValue("菜单", menuBtnStr);
            }
            panelDesignForm.setAttrValue("面板表格", panelTableTd);

        }

        return createBtnName;


    }

    // 基于PanelBusOrchDto简单构建权限
    default void doSetSimplePanelPermissionByBusOrch(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm,
                                                     List<PanelBusOrchDto> busOrchDtos, Set<String> actualFieldNames, Set<String> actualBtnNames, boolean isProcessHandleCategory) throws Exception {
        if (panelDesignForm == null || isEmpty(busOrchDtos) || !isProcessHandleCategory) return;

        // 当前初始化逻辑是,为每个角色创建一个默认读的权限
        List<Form> roleForms = Op.queryFormsByAcs(dao, panelDesignForm.getAssociations("面板角色"));
        if (isEmpty(roleForms)) return;


        // 流程的“节点-操作”映射
        Map<String, Set<String>> nodeOperationMapping = new LinkedHashMap<>();
        for (PanelBusOrchDto busOrchDto : busOrchDtos) {
            String nodeName = busOrchDto.getNodeName();
            String operationName = busOrchDto.getFlowingAction();
            String nextNode = busOrchDto.getNextNode();
            if (StrUtil.hasBlank(nodeName, operationName)) continue;
            if (StrUtil.isNotBlank(nextNode) && !nodeOperationMapping.containsKey(nextNode)) {
                nodeOperationMapping.put(nextNode, new LinkedHashSet<>());
            }
            Set<String> nodeOperations = nodeOperationMapping.get(nodeName);
            if (Op.isEmpty(nodeOperations)) nodeOperations = new LinkedHashSet<>();
            nodeOperations.add(operationName);

            nodeOperationMapping.put(nodeName, nodeOperations);

        }


        TableData permissionTd = new TableData(SlaveFormModelId_PanelDesign_Constraint_Permission);

        for (Form roleForm : roleForms) {
            Form permissionForm = Op.newForm(permissionTd.getFormModelId());

            permissionForm.setAttrValue("场景约束", null);
            permissionForm.setAttrValue("约束名称", StrUtil.format("{}_面板权限", roleForm.getString("角色名称")));

            // 这里没有做具体的实现,在init的时候会加载出默认的,然后后续可以考虑让大模型初始化
            Form permissionImpl = PanelDesignCommonFormUtil
                    .createPanelPermission(dao, observer, panelDesignForm,
                            roleForm, nodeOperationMapping, new ArrayList<>(actualFieldNames), new ArrayList<>(actualBtnNames));

            if (permissionImpl == null) continue;
            permissionForm.setAttrValue("权限实现", toAssociationData(permissionImpl));

            permissionTd.add(permissionForm);

        }


        panelDesignForm.setAttrValue("面板权限", permissionTd);

    }

    // 将PanelBusOrchDto更新到表单
    default void doUpdateBusOrchDtoToPanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, List<PanelBusOrchDto> busOrchDtos, boolean isProcessHandleCategory) throws Exception {
        if (panelDesignForm == null || isEmpty(busOrchDtos) || !isProcessHandleCategory) return;

        // 添加【面板状态】
        List<String> panelStatus = new ArrayList<>();
        for (PanelBusOrchDto busOrchDto : busOrchDtos) {
            panelStatus.add(busOrchDto.getNodeName());
            panelStatus.add(busOrchDto.getNextNode());
        }
        Form panelStatusForm = PanelDesignCommonFormUtil.createPanelStatus(dao, observer, panelDesignForm, panelStatus);
        if (panelStatusForm == null) {
            throw PanelDesignException.Builder.panelStatusEmpty();
        }
        panelDesignForm.setAttrValue("面板状态", toAssociationData(panelStatusForm));

        // 添加【业务编排】
        TableData busOrchTd = new TableData(SlaveFormModelId_PanelDesign_Bus_Orchestration);
        for (PanelBusOrchDto panelBusOrchDto : busOrchDtos) {
            Form busOrchForm = newForm(busOrchTd.getFormModelId());
            busOrchForm.setAttrValue("开始节点", panelBusOrchDto.getNodeName());
            busOrchForm.setAttrValue("操作按钮", panelBusOrchDto.getFlowingAction());
            busOrchForm.setAttrValue("下游节点", panelBusOrchDto.getNextNode());
//            busOrchForm.setAttrValue("进入规则", "");
//            busOrchForm.setAttrValue("跳过规则", "");
            busOrchForm.setAttrValue("离开规则", panelBusOrchDto.getLeaveRule());
            busOrchTd.add(busOrchForm);

        }

        panelDesignForm.setAttrValue("业务编排", busOrchTd);


    }

    default void doUpdateFormDtoToPanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, List<PanelButtonDto> panelButton, PanelFormDto panelForm, Set<String> actualFieldNames, Set<String> actualBtnNames, boolean isProcessHandleCategory) throws Exception {
        if (panelDesignForm == null || panelForm == null) return;

        String panelName = panelDesignForm.getString("面板名称");
        String formName = PanelDesignCommonFormUtil.buildPanelFormName(panelName);

        TableData formTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Form);

        Form form = newForm(formTd.getFormModelId());

        form.setAttrValue("表单名称", formName);
        form.setAttrValue("重置布局", true);

        form.setAttrValue("属性", CollUtil.join(removeNonExistentItem(actualFieldNames, panelForm.getProperties()), ","));

        // 如果不是流程就不处理这个，比如信息管理还是使用之前毛坯生成的系统按钮
        // 这块逻辑很多问题，请使用AI问答
        if (!isProcessHandleCategory) {
            TableData oldFormTd = panelDesignForm.getTable("面板表单");
            if (!Op.isEmpty(oldFormTd)) {
                Form oldForm = oldFormTd.getRows().get(0);
                if (oldForm != null) {
                    form.setAttrValue("按钮", oldForm.getAttrValue("按钮"));
                }

            }

        } else {
            form.setAttrValue("按钮", CollUtil.join(matchExistedButtons(panelButton, panelForm.getButtons()), ","));

        }


        formTd.add(form);

        panelDesignForm.setAttrValue("面板表单", formTd);
    }

    default void doUpdateTableDtoToPanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, List<PanelButtonDto> panelButton, PanelTableDto panelTable,
                                                   Set<String> actualFieldNames, Set<String> actualBtnNames, boolean isProcessHandleCategory) throws Exception {
        if (panelDesignForm == null || panelTable == null) return;
        String panelName = panelDesignForm.getString("面板名称");

        String defaultTableName = panelTable.getTableName();
        if (StrUtil.isBlank(defaultTableName)) {
            defaultTableName = PanelDesignCommonFormUtil.buildPanelTableName(panelName);
        }

        Set<String> tableNames = getOldTableNames(panelDesignForm);
        if (Op.isEmpty(tableNames)) tableNames = CollUtil.newHashSet(defaultTableName);

        TableData tableTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Table);

        for (String tableName : tableNames) {
            Form form = newForm(tableTd.getFormModelId());

            form.setAttrValue("表格名称", tableName);

            // 菜单项（按钮）
            List<String> menuNames = removeNonExistentItem(actualBtnNames, panelTable.getMenuNames());
            if (menuNames == null) menuNames = new ArrayList<>();

            // 搜索字段
            List<String> searchFields = removeNonExistentItem(actualFieldNames, panelTable.getSearchFields());
            if (searchFields == null) searchFields = new ArrayList<>();

            // 表格列
            List<String> columnNames = removeNonExistentItem(actualFieldNames, panelTable.getColumnNames());
            if (columnNames == null) columnNames = new ArrayList<>();

            // FIXME 之前他们的设计是面板状态要加进去，记作字段，现在又移除了这个逻辑
            // FIXME 但后续可能还会反复，因此先简单屏蔽这个逻辑
            if(false){
                if (null != panelDesignForm.getAttrValue("面板状态")) {
                    columnNames.add(0, PanelDesignCommonFormUtil.buildPanelStatusName(
                            panelName
                    ));
                }
            }



            form.setAttrValue("搜索", CollUtil.join(searchFields, ","));
            form.setAttrValue("列名", CollUtil.join(columnNames, ","));

            // 如果不是流程就不处理这个，比如信息管理还是使用之前毛坯生成的系统按钮
            // 这块逻辑很多问题，请使用AI问答
            if (!isProcessHandleCategory) {
                TableData oldTableTd = panelDesignForm.getTable("面板表格");
                if (!Op.isEmpty(oldTableTd)) {
                    Form oldForm = oldTableTd.getRows().get(0);
                    if (oldForm != null) {
                        form.setAttrValue("操作列", oldForm.getAttrValue("操作列"));
                        form.setAttrValue("菜单", oldForm.getAttrValue("菜单"));
                    }

                }

            } else {
                form.setAttrValue("操作列", CollUtil.join(matchExistedButtons(panelButton,
                        panelTable.getActionColumns()), ","));
//        form.setAttrValue("菜单", CollUtil.join(menuNames, ","));

            }

            tableTd.add(form);
        }


        panelDesignForm.setAttrValue("面板表格", tableTd);
        panelDesignForm.setAttrValue("页面入口", tableNames.iterator().next());


    }


    // 将PanelButtonDto更新到表单
    default Set<String> doUpdateButtonDtoToPanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, List<PanelButtonDto> panelButton, boolean isProcessHandleCategory) throws Exception {

        Set<String> actualBtnNames = new HashSet<>();
        if (panelDesignForm == null || isEmpty(panelButton)) return actualBtnNames;

        TableData btnTd = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);


        // 如果不是【流程处理】，那么可以直接拿过来原先的按钮（一般都是一堆系统按钮）
        if (!isProcessHandleCategory) {
            TableData oldBtnTd = panelDesignForm.getTable("面板按钮");
            if (!Op.isEmpty(oldBtnTd)) {
                btnTd = oldBtnTd;
            }
        }


        // 将大模型生成的设置进去
        for (PanelButtonDto buttonDto : panelButton) {
//            String sourceSceneBehaviorCode = buttonDto.getSourceSceneBehaviorCode();
            String sourceSceneBehaviorOptName = buttonDto.getSourceSceneBehaviorName();
            if (StrUtil.isBlank(sourceSceneBehaviorOptName)) sourceSceneBehaviorOptName = "大模型补充";
            String reuseBtnImplCode = buttonDto.getReuseButtonImplCode();
            PanelButtonImplDto newCreateButtonImpl = buttonDto.getNewCreateButtonImpl();

            // 有时候大模型返回的内容这俩是空的
            if (StrUtil.isBlank(reuseBtnImplCode) && newCreateButtonImpl == null) {
                continue;
            }
            Form form = new Form(btnTd.getFormModelId());
            form.setAttrValue("按钮别名", "");

            Form btnImplForm = null;
            // 如果复用按钮实现编号不为空，则复用
            if (StrUtil.isNotBlank(reuseBtnImplCode) &&
                    // 有时候JSON会返回{}单独的花括号，先临时这样处理
                    reuseBtnImplCode.length() > 5) {

                // 查找要复用的按钮
                btnImplForm =
                        Op.queryFormByValueMatchAnyField(dao, FormModelId_Axis_Button,
                                CollUtil.newHashSet(Form.Code, "按钮编号"),
                                reuseBtnImplCode, cnd -> {
                                    cnd.where().andLike(Form.Code, SystemDomain_Prefix);
                                    if (observer != null) {
                                        SqlExpression domainFilterExpr = Op.getBusDomainFilterExpr(observer, FormModelId_Axis_Button);
                                        if (domainFilterExpr != null) {
                                            cnd.where().and(domainFilterExpr);
                                        }
                                    }
                                    return cnd;
                                });
            } else {
                // 自己创建新的按钮
                btnImplForm = PanelDesignCommonFormUtil.createBtnImpl(
                        dao,
                        observer,
                        panelDesignForm.getUuid(),
                        newCreateButtonImpl.getButtonName(),
                        newCreateButtonImpl.getButtonName(),
                        newCreateButtonImpl.getButtonAction(),
                        newCreateButtonImpl.getButtonDescription(),
                        ""
//                        newCreateButtonImpl.getCategoryLabel()
                );


            }
            if (btnImplForm != null) {
                buttonDto.setFinalButtonName(
                        btnImplForm.getString("按钮名称")
                );
                AssociationData btnAc = toAssociationData(btnImplForm);
                boolean isExisted = btnAc.getForm() != null;
                form.setAttrValue("面板按钮", btnAc);

                actualBtnNames.add(buttonDto.getFinalButtonName());
            }

            btnTd.add(form);

        }

        panelDesignForm.setAttrValue("面板按钮",
                PanelDesignCommonFormUtil.cleanDuplicatePanelButtonImpl(btnTd)
//                btnTd
        );


        return actualBtnNames;
    }

    // 将PanelDataDto更新到表单
    default Set<String> doUpdateDataDtoToPanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm,
                                                         List<PanelDataFieldDto> panelData) throws Exception {

        Set<String> actualFieldNames = new HashSet<>();

        if (panelDesignForm == null || isEmpty(panelData)) return actualFieldNames;


        TableData dataTd = new TableData(SlaveFormModelId_PanelDesign_Data);
        Set<String> existedFieldNames = new HashSet<>();
        for (PanelDataFieldDto dataFieldDto : panelData) {

            String attrName = dataFieldDto.getFieldName();
            String attrStyle = dataFieldDto.getFieldStyle();

            if (StrUtil.hasBlank(attrName, attrStyle)) continue;

            // 做下去重
            if(existedFieldNames.contains(attrName)) continue;
            existedFieldNames.add(attrName);

            String sourceSceneDataCode = dataFieldDto.getSourceSceneDataCode();
            String sourceSceneAttrName = dataFieldDto.getSourceSceneDataFieldName();
            String sourceSceneAttrStyle = dataFieldDto.getSourceSceneDataFieldStyle();

            Form form = new Form(dataTd.getFormModelId());

            // 去匹配场景数据，但是可能为空
            form.setAttrValue("场景数据", queryAssociationByAssignField(dao, FormModelId_SceneLayer_Data,
                    Form.Code, sourceSceneDataCode));

            form.setAttrValue("场景属性名称", sourceSceneAttrName);
            form.setAttrValue("场景属性样式", attrStyle);
            form.setAttrValue("属性样式", attrStyle);
            // FIXME 这里创建样式的时候，会去判断样式有没有问题，而有没有问题的查询没有增加业务域过滤！
            Form attrImplForm = PanelDesignCommonFormUtil.createAttrImpl(dao, observer, panelDesignForm.getUuid(),
                    attrName, attrStyle);
            if (attrImplForm == null) continue;

            form.setAttrValue("属性实现", toAssociationData(attrImplForm));


            actualFieldNames.add(attrName);
            dataTd.add(form);


        }

        panelDesignForm.setAttrValue("面板数据", dataTd);

        return actualFieldNames;

    }

    // 从llmDataForm中取出【xxx.响应内容】
    default Map<String, JSON> doCollectResponds(Form llmDataForm) throws Exception {

        Map<String, JSON> respondMap = new HashMap<>();

        FormModel llmDataFormModel = IFormMgr.get().queryFormModel(llmDataForm.getFormModelId());
        for (FormField llmDataFormField : llmDataFormModel.getNotHiddenFieldList()) {
            String fieldCode = llmDataFormField.getCode();
            Object tdObj = llmDataForm.getAttrValue(fieldCode);
            if (!(tdObj instanceof TableData)) continue;
            TableData td = (TableData) tdObj;

            if (isEmpty(td)) continue;
            Form form = td.getRows().get(0);
            String defaultRespondFieldName = "响应内容";
            String attrValue = form.getString(defaultRespondFieldName);
            if (StrUtil.isNotBlank(attrValue)) {
                // 1、解析【响应内容】的JSON数据
                JSONObject jsonObject = Op.parseJsonObject(attrValue);
                // 2、存入 respondMap
                if (jsonObject == null || jsonObject.isEmpty()) continue;
                Map<String, Object> jsonObjectRaw = jsonObject.getRaw();
                // 根据现有的设计，只取第一个属性
                Map.Entry<String, Object> entry = jsonObjectRaw.entrySet().iterator().next();

                Object valObj = entry.getValue();
                if (!(valObj instanceof JSON)) continue;

                String key = entry.getKey();
                respondMap.put(key, (JSON) valObj);

            }

        }

        return respondMap;
    }

    // 根据场景编号查询PanelDesignForm
    default Form doQueryPanelDesignFormBySceneCode(IDao dao, String sourceSceneCode) throws Exception {
        // 到这一步PanelDesign一定已经初始化了，如果没有初始化就抛异常
        Form panelDesignForm = queryFormByAssignField(dao, WorkBenchConst.FormModelId_PanelDesign,
                "所属场景", sourceSceneCode);
        if (panelDesignForm == null)
            throw PanelDesignException.Builder.notFoundByScene(sourceSceneCode);
        return panelDesignForm;
    }

    // 查询 targetQueryFormModelId 并自动将结果填充到上下文中
    // queryCnd 用户可自定义查询条件
    default void doQueryAndFillInFormToContext(Form llmDataForm, String targetQueryFormModelId,
                                               Cnd queryCnd, String contextKey) throws Exception {

        // 1、根据指定的模型ID+业务域过滤条件+用户自定义的条件，进行查询
        List<Form> forms = queryFormsWithBusDomainFilter(llmDataForm, targetQueryFormModelId, queryCnd);

        if (CollUtil.isNotEmpty(forms)) {
            // 2、将数据转换为JSON
            // 注：使用FormToJsonConversionUtil工具类进行转换，亦可JSON反转为Form
            // 可查看：JsonToFormConversionUtil
            JSONArray jsonArray = convertFormsToJsonArray(forms);

            setLlmFormFieldValue(llmDataForm, NodeName_Context_Initial, contextKey,
                    jsonArray.toString());
        } else {
            logf("queryAndFillInFormToContext: 在填充上下文[{}]时, 没有查询到任何[{}]模型的数据", contextKey, targetQueryFormModelId);
        }


    }

    // 获取上下文数据Map
    // 所有的上下文都会存储到 #NodeName_Context_Initial 对应的表单里
    default Map<String, String> getContextDataMap(Form llmDataForm) throws Exception {
        Map<String, String> dataMap = new HashMap<>();
        if (llmDataForm == null) return dataMap;


        // 处理第一个节点【上下文初始化】
        TableData ctxTd = llmDataForm.getTable(NodeName_Context_Initial);
        if (!isEmpty(ctxTd)) {
            Form ctxForm = ctxTd.getRows().get(0);
            FormModel ctxFormModel = IFormMgr.get().queryFormModel(ctxForm.getFormModelId());
            for (FormField formField : ctxFormModel.getNotHiddenFieldList()) {
                String fieldCode = formField.getCode();
                String fieldName = formField.getName();
                if (fieldName.equals("系统_生成历史")) continue;
                Object attrValue = ctxForm.getAttrValue(fieldCode);
                if (!(attrValue instanceof String)) continue;
                dataMap.put(fieldName, (String) attrValue);

            }
        }

        // 处理其他节点
        // 将其他节点的数据写进去
        FormModel llmDataFormModel = IFormMgr.get().queryFormModel(llmDataForm.getFormModelId());
        for (FormField llmDataFormField : llmDataFormModel.getNotHiddenFieldList()) {
            String fieldCode = llmDataFormField.getCode();
            String fieldName = llmDataFormField.getName();
            Object tdObj = llmDataForm.getAttrValue(fieldCode);
            if (!(tdObj instanceof TableData)) continue;
            TableData td = (TableData) tdObj;

            if (isEmpty(td)) continue;
            Form form = td.getRows().get(0);
            List<String> defaultRespondFieldNames = CollUtil.newArrayList("响应内容", "输出内容");
            for (String defaultRespondFieldName : defaultRespondFieldNames) {
                String attrValue = form.getString(defaultRespondFieldName);
                if (StrUtil.isNotBlank(attrValue)) {
                    dataMap.put(StrUtil.format("{}.{}", fieldName, defaultRespondFieldName), (String) attrValue);

                }
            }

        }


        return dataMap;


    }


    // ========================= 通用方法 =========================

    // 获取旧面板表格名称
    default Set<String> getOldTableNames(Form panelDesignForm) throws Exception {
        Set<String> oldTableNames = new LinkedHashSet<>();
        if (panelDesignForm == null) return oldTableNames;

        TableData tableData = panelDesignForm.getTable("面板表格");
        if (tableData == null) return oldTableNames;

        for (Form row : tableData.getRows()) {
            String tableName = row.getString("表格名称");
            if (StrUtil.isNotBlank(tableName)) {
                oldTableNames.add(tableName);
            }
        }

        return oldTableNames;


    }


    // 移除不存在项
    default List<String> removeNonExistentItem(Set<String> actualExistentItems,
                                               List<String> items) {
        if (Op.isEmpty(actualExistentItems) || Op.isEmpty(items)) return items;
        return items.stream()
                .filter(actualExistentItems::contains)
                .collect(Collectors.toList());


    }


    // 只匹配存在的按钮, 可能大模型返回的是之前想当然的名字，比如自己起了一个名字，但是复用了已有的按钮
    default List<String> matchExistedButtons(List<PanelButtonDto> panelExistedButtons, List<String> expectBtnNames) {
        if (CollUtil.isEmpty(panelExistedButtons) || CollUtil.isEmpty(expectBtnNames)) return new ArrayList<>();
        List<String> matchedButtons = new ArrayList<>();
        for (String expectBtnName : expectBtnNames) {
            for (PanelButtonDto existedButton : panelExistedButtons) {
                String name1 = existedButton.getSourceSceneBehaviorName();
                String name2 = "";
                if (null != existedButton.getNewCreateButtonImpl()) {
                    name2 = existedButton.getNewCreateButtonImpl().getButtonName();
                }
                if (StrUtil.equalsAny(expectBtnName, name1, name2)) {
                    matchedButtons.add(existedButton.getFinalButtonName());
                }
            }
        }
        // 过滤空值
        return matchedButtons.stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    // 将List<Form>转换为JSONArray
    default JSONArray convertFormsToJsonArray(List<Form> forms) throws Exception {
        if (CollUtil.isEmpty(forms)) return new JSONArray();
        JSONArray jsonArray = new JSONArray();
        for (Form form : forms) {
            JSONObject converted = FormToJsonConversionUtil.convert(form);
            if (converted == null) {
                // ... 如有必要，添加日志
                continue;
            }
            jsonArray.add(converted);
        }
        return jsonArray;

    }

    // 便捷-快速查询所有数据-并应用业务域过滤条件
    default List<Form> queryFormsWithBusDomainFilter(Form llmDataForm, String formModelId, Cnd queryCnd) throws Exception {
        if (llmDataForm == null || StrUtil.isBlank(formModelId)) return new ArrayList<>();
        try (IDao dao = IDaoService.newIDao()) {
            // 拿到业务域的过滤条件
            SqlExpression busDomainFilterExpr = getBusDomainFilterExpr(llmDataForm, formModelId);
            if (queryCnd == null) queryCnd = Cnd.NEW();
            // 应用业务域的过滤条件
            queryCnd.where().and(busDomainFilterExpr);

            ResultSet<Form> resultSet = IFormMgr.get().queryFormPage(dao, formModelId, queryCnd, 1, Integer.MAX_VALUE, true, true);
            return resultSet.getDataList();
        }

    }


    // 获取业务域过滤表达式
    default SqlExpression getBusDomainFilterExpr(Form llmDataForm, String opFunctionModelId) throws Exception {
        if (llmDataForm == null) return null;
        String paramMapStr = llmDataForm.getString("参数表");
        if (StrUtil.isBlank(paramMapStr)) return null;

        Map<String, String> paramMap = GsonUtil.fromJson(paramMapStr, HashMap.class);
        if (paramMap == null || paramMap.isEmpty()) return null;

        String domainUuid = paramMap.get(ParamKey_BUS_DOMAIN_UUID);
        String domainCode = paramMap.get(ParamKey_BUS_DOMAIN_CODE);

        return Op.getBusDomainFilterExpr(domainUuid, domainCode, opFunctionModelId);

    }

    // 获取业务域观察类
    default OctoDomainOpObserver getOctoDomainOpObserver(Form llmDataForm) throws Exception {
        if (llmDataForm == null) return null;
        String paramMapStr = llmDataForm.getString("参数表");
        if (StrUtil.isBlank(paramMapStr)) return null;

        Map<String, String> paramMap = GsonUtil.fromJson(paramMapStr, HashMap.class);
        if (paramMap == null || paramMap.isEmpty()) return null;

        String domainUuid = paramMap.get(ParamKey_BUS_DOMAIN_UUID);
        String domainCode = paramMap.get(ParamKey_BUS_DOMAIN_CODE);
        // 默认数据
        // String domainUuid = "58868f38_a7cb_42f3_9311_33c19b55243f";
        // String domainCode = "OctoCM_Panel";


        if (StrUtil.hasBlank(domainUuid, domainCode)) return null;

        return new OctoDomainOpObserver(domainCode, domainUuid);

    }

    // 从LLM表单的参数表中获取参数值
    // @see llmDataForm.参数表
    default String getParamValueFromLlmDataForm(Form llmDataForm, String paramKey) throws Exception {
        String paramMapStr = llmDataForm.getString("参数表");
        if (StrUtil.isBlank(paramMapStr)) return null;

        Map<String, String> paramMap = GsonUtil.fromJson(paramMapStr,
                HashMap.class);
        if (paramMap == null || !paramMap.containsKey(paramKey)) return null;

        return paramMap.get(paramKey);

    }


    // 快捷-设置LLM表单中单个节点的属性值
    default void setLlmFormFieldValue(Form llmDataForm, String nodeName,
                                      String fieldName, Object value) throws Exception {
        FormValueUtil.setExpressionValue(
                llmDataForm,
                String.format("%s.%s", nodeName, fieldName),
                value
        );
    }

    // 快捷-获取LLM表单中单个节点的属性值
    default <T> T getLlmFormFieldStringValue(Form llmDataForm, String expression, Class<T> clazz) throws Exception {
        return FormValueUtil.getExpressionValue(
                llmDataForm,
                expression,
                clazz
        );
    }

    // 基础的提示词
    default String getBasicPrompt() {
        String basicPrompt = "[智能体基础信息]\n";
        basicPrompt += StrUtil.format("[当前时间:{}]\n",
                DateTime.now().toString(DatePattern.NORM_DATETIME_FORMAT));
        basicPrompt += StrUtil.format("[使用语言:中文简体，请过滤UTF-8无法解析的字符]\n");

        return basicPrompt;
    }

    // 便捷-把FastJson的功能拉了过来
    default String fastJsonToJsonString(Object obj) {
        return com.alibaba.fastjson2.JSON.toJSONString(obj);
    }

    // 便捷-把FastJson的功能拉了过来
    default <T> T fastJsonParseObject(String string, Class<T> objectClass) {
        return com.alibaba.fastjson2.JSON.parseObject(string, objectClass);
    }

    // 简单移除标记
    default String clearLlmRespondTag(String generateResult) {
        if (StrUtil.isBlank(generateResult)) return generateResult;
        generateResult = generateResult.replaceAll("```html", "");
        generateResult = generateResult.replaceAll("```json", "");
        generateResult = generateResult.replaceAll("```", "");
        return generateResult.trim();
    }

}
