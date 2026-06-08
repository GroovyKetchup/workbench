package cell.octo.cm.expr;


import ai.webPage.dto.WebPageSessionInfo;
import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.fe.progress.CFeProgressCtrlWithTextArea;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.octo.cm.service.IPanelDesignLLMSupportService;
import cell.octo.cm.service.IPanelDesignService;
import cell.octocm.domain.service.IDomainService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.dto.Progress;
import cmn.exception.MultiException;
import cmn.utils.FormValueUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.common.util.ToolUtilities;
import fe.cmn.app.ability.PopToast;
import fe.cmn.panel.PanelContext;
import fe.cmn.progress.ProgressBarDecorationDto;
import fe.util.LoadingMask;
import fe.util.component.Component;
import fe.util.component.ProgressDialog;
import fe.util.component.param.ProgressDialogParam;
import gpf.adur.data.*;
import gpf.dc.basic.fe.component.view.AbsFormView;
import gpf.dc.basic.fe.component.view.AbsTableView;
import gpf.dc.basic.param.view.BaseFeActionParameter;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.ErrorDto;
import octo.cm.dto.RelationFieldMapping;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.exception.business.SceneException;
import octo.cm.util.*;
import octocm.domain.dto.DomainDto;
import octocm.domain.filter.OctoDomainDataFilter;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计操作函数")
@ClassDeclare(label = "", what = "", why = "", how = "", developer = "裴硕", version = "1.0", createTime = "2025-10-22", updateTime = "2025-10-22")
public interface PanelDesignExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();

    // 面板设计关联字段映射集合
    List<RelationFieldMapping> PANEL_DESIGN_RELATION_FIELD_MAPPINGS = CollUtil.newArrayList(

            new RelationFieldMapping("面板分类", FormModelId_PanelDesign_Category, true, false, "分类名称"), new RelationFieldMapping("权限角色", FormModelId_Axis_Role, true, false, "角色名称"),

            new RelationFieldMapping("面板角色", FormModelId_Axis_Role, false, true, "角色名称")
                    // 根据指定字段进行复用已有的（在保存的情况下）
                    .setReUseByAssignFormFieldName(true).setCustomFormCode("角色编号"),

            new RelationFieldMapping("属性实现", FormModelId_Axis_Data, false, true, null),

            new RelationFieldMapping("面板按钮", FormModelId_Axis_Button, false, true, null)
                    // 允许查询嵌套数据
                    .setAllowQueryNestingData(true),

            new RelationFieldMapping("面板状态", FormModelId_PanelDesign_Status, false, true, null)
                    // 允许查询嵌套数据
                    .setAllowQueryNestingData(true).setCustomFormCode("状态编号"),

            new RelationFieldMapping("事件实现", FormModelId_PanelDesign_Event, false, true, null)
                    // 允许查询嵌套数据
                    .setAllowQueryNestingData(true).setCustomFormCode("事件编号"),

            new RelationFieldMapping("权限实现", FormModelId_Axis_Permission, false, true, null)
                    // 允许查询嵌套数据
                    .setAllowQueryNestingData(true).setCustomFormCode("权限编号"),

            new RelationFieldMapping("事件集合", FormModelId_PanelDesign_Event, false, false, null)
                    // 使用Form.Code映射机制
                    .setUseCodeMapping(true)

    );

    String SESSION_PARAM_KEY_BUS_DOMAIN_CODE = "busDomainCode";
    String SESSION_PARAM_KEY_TARGET_SCENE_CODES = "targetSceneCodes";
    String SESSION_PARAM_KEY_TARGET_PANEL_DESIGN_CODES = "targetPanelDesignCodes";


    @MethodDeclare(label = "AI初始化", how = "", what = "", why = "", inputs = {@InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"), @InputDeclare(name = "form", label = "form", desc = "", exampleValue = "$form$")})
    default void llmInitPanelDesign(BaseFeActionParameter input) throws Exception {
        PanelContext panelContext = input.getPanelContext();
        Component currentComponent = input.getCurrentComponent();
        boolean isUseLlmInit = Op.showYesOrNoDialog(panelContext, "提示", "请确认是否使用大模型辅助初始化，" + "大模型能够优化发布质量，但需要花费更多时间。");

        if (!isUseLlmInit) return;

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(panelContext);
        CFeProgressCtrlWithTextArea progressImpl = ProgressDialog.showProgressDialog(panelContext, "AI初始化", false, false, ProgressDialogParam.TIME_FORMATTER_HMS, new ProgressBarDecorationDto().setShowCancelButton(false).setShowMessage(true).setShowPercentage(false));
        Progress<?> progress = Progress.wrap(progressImpl);


        try {

            if (currentComponent instanceof AbsFormView) {
                // 如果是表单调用
                Form form = Op.getCurrFormByFormView(input);
                doLlmInitPanelDesignOnFormView(input, observer, progress, form);

            } else if (currentComponent instanceof AbsTableView) {
                List<Form> forms = Op.getTableCurrBeSelectedFormAndQueryFullForm(input);
                if (Op.isEmpty(forms)) throw PanelDesignException.Builder.atLeastSelectOne();
                // 如果是表格上调用（批量）
                doLlmInitPanelDesignOnTableView(input, observer, progress, forms);

            } else {
                throw PanelDesignException.Builder.unknownViewType("应用AI初始化");
            }
            if (PanelDesignPublishErrorContext.hasError()) {
                PopHtmlView.popErrorViewWhenHasErrorWithUserConfirm(panelContext);
            } else {
                Op.popOperateSuccess(panelContext);
                Thread.sleep(500);
                PopToast.info(panelContext.getChannel(), "大模型偶尔也会犯错，请确保面板设计正常，然后再进行发布");

            }


        } catch (Exception e) {
            progressImpl.finishError(ToolUtilities.getFullExceptionStack(e));
//            throw new RuntimeException(e);
        }


    }


    @MethodDeclare(label = "场景发布", how = "", what = "", why = "", inputs = {@InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),})
    default List<JSONObject> publishScene(String busDomainCode, List<String> targetSceneCodes) throws Exception {

        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.cannotDetermine();
        if (Op.isEmpty(targetSceneCodes)) throw SceneException.Builder.cannotDetermineWhichToPublish();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);

        List<JSONObject> errItems = new ArrayList<>();

        for (String targetSceneCode : targetSceneCodes) {

            try {
                Form panelDesign = IPanelDesignService.get().publishSceneLayer(observer, targetSceneCode, true);
                if (panelDesign == null || StrUtil.isBlank(panelDesign.getString("面板编号"))) {
                    throw PanelDesignException.Builder.notFoundAnyToPublish();
                }

            } catch (Exception e) {
                errItems.add(new JSONObject().set("sceneCode", targetSceneCode).set("errReason", ExceptionUtils.getFullStackTrace(e)));
            }


        }

        return errItems;


    }


    @MethodDeclare(label = "发布面板", how = "", what = "", why = "", inputs = {@InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),})
    default void publishPanelDesign(BaseFeActionParameter input) throws Exception {


        PanelContext panelContext = input.getPanelContext();
        Component currentComponent = input.getCurrentComponent();
        if (panelContext != null && currentComponent != null) {
            publishPanelDesignOnJDF(input, panelContext, currentComponent);
        } else {
            publishPanelDesignOnNoJDF(input);

        }


    }

    @MethodDeclare(label = "发布面板", how = "", what = "", why = "", inputs = {@InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),})
    default List<JSONObject> publishPanelDesign(String busDomainCode, List<String> targetPanelDesignCodes) throws Exception {
        return doPublishPanelDesign(busDomainCode, null, targetPanelDesignCodes);

    }


    @MethodDeclare(label = "获取面板设计列表", how = "", what = "", why = "", inputs = {@InputDeclare(name = "busDomainCode", label = "业务域编号", desc = "", exampleValue = ""),})
    default List<Map<String, Object>> getPanelDesignList(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();

        List<Map<String, Object>> panelCMListMap = queryPanelCMList(busDomainCode, 1, Integer.MAX_VALUE, true);
        if (Op.isEmpty(panelCMListMap)) panelCMListMap = new ArrayList<>();

        // FIXME 统一封装或找宗智的
        String userModelId = StrUtil.format("gpf.md.user.{}_User", busDomainCode);

        FormModel userFormModel = IFormMgr.get().queryFormModel(userModelId);
        if (userFormModel != null) {
            Map<String, Object> userFormModelMap = new HashMap<>();
            List<String> attrList = new ArrayList<>();
            for (FormField formField : userFormModel.getNotHiddenFieldList()) {
                attrList.add(formField.getName());
            }

            userFormModelMap.put("面板编号", "$用户模型$");
            userFormModelMap.put("面板名称", "用户模型");
            userFormModelMap.put("面板描述", "用户模型");
            userFormModelMap.put("属性列表", attrList);
            userFormModelMap.put("系统模型", true);

            panelCMListMap.add(userFormModelMap);

        }


        return panelCMListMap;
    }


    @MethodDeclare(label = "获取面板设计数据", how = "", what = "", why = "", inputs = {})
    default JSONObject getPanelDesignData(String busDomainCode, String panelCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(panelCode)) throw PanelDesignException.Builder.panelCodeEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        try (IDao dao = IDaoService.newIDao()) {


            List<Form> forms = doQueryPanelDesignByPanelCodes(dao, observer, CollUtil.newArrayList(panelCode));
            if (Op.isEmpty(forms)) throw PanelDesignException.Builder.notFoundWithCode(panelCode);
            Form form = forms.get(0);
            if (form == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            JSONObject converted = FormToJsonConversionUtil.convert(dao, form, PANEL_DESIGN_RELATION_FIELD_MAPPINGS);
            converted.remove("编排需求");
            converted.remove("面板行为");
            converted.remove("所属场景");
            return converted;

        }


    }

    @MethodDeclare(label = "获取面板设计数据列表", how = "", what = "", why = "", inputs = {})
    default List<JSONObject> getPanelDesignDataList(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        List<JSONObject> resultList = new ArrayList<>();
        try (IDao dao = IDaoService.newIDao()) {


            List<Form> forms = IPanelDesignService.get().queryPanelDesigns(dao, observer, true);
            if (Op.isEmpty(forms)) return resultList;

            for (Form form : forms) {
                JSONObject converted = FormToJsonConversionUtil.convert(dao, form, PANEL_DESIGN_RELATION_FIELD_MAPPINGS);
                converted.remove("编排需求");
                converted.remove("面板行为");
                converted.remove("所属场景");
                resultList.add(converted);
            }

            return resultList;

        }


    }

    @MethodDeclare(label = "保存面板设计数据", how = "", what = "", why = "", inputs = {})
    default void savePanelDesignData(String busDomainCode, String panelCode, String jsonData,
                                     List<String> skipModuleNames) throws Exception {

        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(panelCode)) throw PanelDesignException.Builder.panelCodeEmpty();
        if (StrUtil.isBlank(jsonData)) throw CommonException.Builder.jsonDataEmpty();


        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        JSONObject finalObj;
        try {
            finalObj = JSONUtil.parseObj(jsonData);
        } catch (Exception e) {
            throw CommonException.Builder.invalidJsonData();
        }

//        LvUtil.trace(StrUtil.format("finalObj:{}", JSONUtil.toJsonStr(finalObj)));


        try (IDao dao = IDaoService.newIDao()) {


            Form form = doQueryPanelDesignByPanelCodes(dao, observer, CollUtil.newArrayList(panelCode)).get(0);
            if (form == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            // 收集被跳过的数据
            Map<String, TableData> skipModuleDatas = doCollectNeedSkipModuleDatas(form, skipModuleNames);


            // 删除重名的
            doClearDuplicateRoleDefinition(dao, observer, form);

            form = JsonToFormConversionUtil.convert(dao, observer, form, finalObj, PANEL_DESIGN_RELATION_FIELD_MAPPINGS);

            // 将被跳过填充的数据写回去
            doFillInBeSkippedModuleDatas(form, skipModuleDatas);

            IFormMgr.get().updateForm(null, dao, form, observer);
            dao.commit();

        } catch (Exception e) {
            Op.logException(e);
        }

        return;
    }




    // ========================= 支撑方法 =========================


    // 收集被跳过的数据
    default Map<String, TableData> doCollectNeedSkipModuleDatas(Form form,
                                                                List<String> skipModuleNames) throws Exception {
        Map<String, TableData> result = new HashMap<>();
        if (form == null || skipModuleNames == null) return result;

        for (String moduleName : skipModuleNames) {
            TableData td = form.getTable(moduleName);
            result.put(moduleName, td);

        }

        return result;


    }

    // 将被跳过填充的数据写回去
    default void doFillInBeSkippedModuleDatas(Form form, Map<String, TableData> skipModuleDatas) throws Exception {
        if (form == null || Op.isEmpty(skipModuleDatas)) return;
        for (Map.Entry<String, TableData> entry : skipModuleDatas.entrySet()) {
            String moduleName = entry.getKey();
            if (StrUtil.isBlank(moduleName)) continue;
            TableData moduleData = entry.getValue();
            form.setAttrValue(moduleName, moduleData);
        }
    }

    // 之前宗智写的获取面板的方法，复用一下
    default List<Map<String, Object>> queryPanelCMList(String domainCode, long pageNo, long pageSize, boolean withSystem) throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            IDomainService domainService = IDomainService.get();
            DomainDto domainDto = domainService.getDomainByCode(domainCode);

            String modelId = WorkBenchConst.FormModelId_PanelDesign;
            OctoDomainDataFilter octoDomainDataFilter = new OctoDomainDataFilter(domainDto);
            SqlExpression sqlExpression = octoDomainDataFilter.buildInDomainCondition(modelId, withSystem);

            IFormMgr formMgr = IFormMgr.get();
            ResultSet<Form> formResultSet = null;
            formResultSet = formMgr.queryFormPage(dao, modelId, Cnd.where(sqlExpression), (int) pageNo, (int) pageSize, true, true);
            List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < formResultSet.getDataList().size(); i++) {
                Map formMap = new HashMap();
                Form form = formResultSet.getDataList().get(i);
                String panelCode = FormValueUtil.getNestingValue(form, String.class, "面板编号");
                String panelName = FormValueUtil.getNestingValue(form, String.class, "面板名称");
                String panelDesc = FormValueUtil.getNestingValue(form, String.class, "面板描述");
                TableData tableData = form.getTable("面板数据");
                List attrList = new ArrayList();
                if (tableData != null) {
                    for (int j = 0; j < tableData.getRows().size(); j++) {
                        Form subform = tableData.getRows().get(j);
                        String attrName = FormValueUtil.getNestingValue(subform, String.class, "属性实现", "属性名称");
                        attrList.add(attrName);
                    }
                    formMap.put("面板编号", panelCode);
                    formMap.put("面板名称", panelName);
                    formMap.put("面板描述", panelDesc);
                    formMap.put("属性列表", attrList);
                    resultList.add(formMap);
                }
            }
            return resultList;
        }
    }


    // 清理重复的角色定义
    default void doClearDuplicateRoleDefinition(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return;

        List<Form> roleForms = Op.queryFormsByAcs(dao, panelDesignForm.getAssociations("面板角色"));
        if (Op.isEmpty(roleForms)) return;


        for (Form roleForm : roleForms) {
            String roleName = roleForm.getString("角色名称");
            String roleDesc = roleForm.getString("角色描述");
            String orgMatchRule = roleForm.getString("组织匹配");

            Cnd queryCnd = Op.getBusDomainFilterCondition(observer, FormModelId_Axis_Role);
            SqlExpressionGroup where = queryCnd.where();
            where.andNotEquals(Form.Code, roleForm.getString(Form.Code));

            if (StrUtil.isNotBlank(roleName)) where.andEquals(Op.getFieldCode("角色名称"), roleName);
            if (StrUtil.isNotBlank(roleDesc)) where.andEquals(Op.getFieldCode("角色描述"), roleDesc);
            if (StrUtil.isNotBlank(orgMatchRule)) where.andEquals(Op.getFieldCode("组织匹配"), orgMatchRule);

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_Axis_Role, queryCnd, 1, Integer.MAX_VALUE, false, false);

            List<String> removeTargetCodes = new ArrayList<>();
            if (!queryRs.isEmpty()) {
                for (Form form : queryRs.getDataList()) {
                    String code = form.getString(Form.Code);
                    if (StrUtil.isNotBlank(code)) {
                        removeTargetCodes.add(code);
                    }
                }
            }
            if (!Op.isEmpty(removeTargetCodes)) {
                Cnd deleteCnd = Op.getBusDomainFilterCondition(observer, FormModelId_Axis_Role);
                deleteCnd.where().andInStrList(Form.Code, removeTargetCodes);
                IFormMgr.get().deleteForm(null, dao, FormModelId_Axis_Role, deleteCnd, observer);

            }


        }


    }


    // 非JDK进行发布
    default void publishPanelDesignOnNoJDF(BaseFeActionParameter input) throws Exception {

        IDCRuntimeContext rtx = input.getRtx();
        WebPageSessionInfo sessionInfo = WebPageSessionInfo.read(rtx);

        Object busDomainCodeObj = sessionInfo.getUserDefineParams().get(SESSION_PARAM_KEY_BUS_DOMAIN_CODE);
        Object targetSceneCodesObj = sessionInfo.getUserDefineParams().get(SESSION_PARAM_KEY_TARGET_SCENE_CODES);
        Object targetPanelDesignCodesObj = sessionInfo.getUserDefineParams().get(SESSION_PARAM_KEY_TARGET_PANEL_DESIGN_CODES);

        if (!(busDomainCodeObj instanceof String)) throw DomainException.Builder.cannotDetermine();

        List<String> targetSceneCodes = null, targetPanelDesignCodes = null;

        // 如果提供了目标场景编号...
        if (targetSceneCodesObj instanceof List) targetSceneCodes = (List<String>) targetSceneCodesObj;

        // 如果提供了目标面板编号...
        if (targetPanelDesignCodesObj instanceof List)
            targetPanelDesignCodes = (List<String>) targetPanelDesignCodesObj;

        if (targetSceneCodes == null && targetPanelDesignCodes == null)
            throw PanelDesignException.Builder.sceneOrPanelCodeRequired();

        String busDomainCode = String.valueOf(busDomainCodeObj);

        doPublishPanelDesign(busDomainCode, targetSceneCodes, targetPanelDesignCodes);

    }

    default List<JSONObject> doPublishPanelDesign(String busDomainCode, List<String> targetSceneCodes, List<String> targetPanelDesignCodes) throws Exception {
        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);

        // 目标要发布面板的面板设计Form
        List<Form> targetPublishPanelDesignForms = new ArrayList<>();

        try (IDao dao = IDaoService.newIDao()) {
            if (!Op.isEmpty(targetSceneCodes)) {
                targetPublishPanelDesignForms.addAll(doQueryPanelDesignBySceneCodes(dao, observer, targetSceneCodes));
            }
            if (!Op.isEmpty(targetPanelDesignCodes)) {
                targetPublishPanelDesignForms.addAll(doQueryPanelDesignByPanelCodes(dao, observer, targetPanelDesignCodes));
            }

            if (Op.isEmpty(targetPublishPanelDesignForms)) throw PanelDesignException.Builder.notFoundAnyToPublish();


            // 实际的发布代码
            PanelDesignPublishUtil.publishBatch(null, null, observer, targetPublishPanelDesignForms);


            List<ErrorDto> errors = PanelDesignPublishErrorContext.getErrorsAndClear();

            // 现在直接抛出去，然后晓斌那边做转换
            if (!Op.isEmpty(errors)) {
                MultiException me = PanelDesignException.Builder.batchPublishError(errors);
                me.ifExceptionThrow();
            }

            return new ArrayList<>();


            // 后面这个是之前的逻辑，返回自己捕获的异常

//              List<JSONObject> errItems = new ArrayList<>();
//            if (!Op.isEmpty(errors)) {
//                for (ErrorDto error : errors) {
//                    errItems.add(
//                            new JSONObject()
//                                    .set("panelCode", error.getErrorKey())
//                                    .set("errReason", error.getErrorStack())
//                    );
//                }
//
//
//            }
//
//            return errItems;


        }
    }

    // 在JDF的设计上进行发布
    default void publishPanelDesignOnJDF(BaseFeActionParameter input, PanelContext panelContext, Component currentComponent) throws Exception {
        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(panelContext);
        CFeProgressCtrlWithTextArea progressImpl = ProgressDialog.showProgressDialog(panelContext, "发布面板", false, false, ProgressDialogParam.TIME_FORMATTER_HMS, new ProgressBarDecorationDto().setShowCancelButton(false).setShowMessage(true).setShowPercentage(false));

        Progress<?> progress = Progress.wrap(progressImpl);
        try {

            if (currentComponent instanceof AbsFormView) {
                // 如果是表单调用
                Form form = Op.getCurrFormByFormView(input);

                doPublishPanelDesignOnFormView(input, observer, progress, form);

            } else if (currentComponent instanceof AbsTableView) {
                List<Form> forms = Op.getTableCurrBeSelectedFormAndQueryFullForm(input);
                if (Op.isEmpty(forms)) throw PanelDesignException.Builder.atLeastSelectOne();

                // 如果是表格上调用（批量）
                doPublishPanelDesignOnTableView(input, observer, progress, forms);

            } else {
                throw PanelDesignException.Builder.unknownViewType("发布面板");
            }

            if (PanelDesignPublishErrorContext.hasError()) {
                PopHtmlView.popErrorViewWhenHasErrorWithUserConfirm(panelContext);
            } else {
                Op.popOperateSuccess(panelContext);
            }


        } catch (Throwable e) {
            progressImpl.finishError(ToolUtilities.getFullExceptionStack(e));
//            throw new RuntimeException();
        }
    }


    // 在表格上执行发布面板
    default void doPublishPanelDesignOnTableView(BaseFeActionParameter input, OctoDomainOpObserver observer, Progress<?> progress, List<Form> forms) throws Exception {
        PanelContext panelContext = input.getPanelContext();
        LoadingMask.showLinearProgress(panelContext);
        try {

            PanelDesignPublishUtil.publishBatch(progress, panelContext, observer, forms);

        } finally {
            progress.finish();
            LoadingMask.hide(panelContext);
        }
    }

    // 在表单上执行发布面板
    default void doPublishPanelDesignOnFormView(BaseFeActionParameter input, OctoDomainOpObserver observer, Progress<?> progress, Form form) throws Exception {

        Component currentComponent = input.getCurrentComponent();
        if (!(currentComponent instanceof AbsFormView)) {
            throw PanelDesignException.Builder.onlyRunOnFormView();
        }
        AbsFormView formView = (AbsFormView) currentComponent;

        PanelContext panelContext = input.getPanelContext();
        if (Op.isNewForm(form)) throw PanelDesignException.Builder.saveFirst();


        // 获取当前默认应用，当不存在时弹出选择面板
        ApplicationUtil.getOrSetDefaultPublishAppCode(panelContext, observer);


        LoadingMask.showLinearProgress(panelContext);

        try {

            PanelDesignPublishUtil.publish(progress, panelContext, observer, form);

        } finally {
            progress.finish();
            LoadingMask.hide(panelContext);
        }


    }

    // 在表格上执行大模型初始化
    default void doLlmInitPanelDesignOnTableView(BaseFeActionParameter input, OctoDomainOpObserver observer, Progress<?> progress, List<Form> forms) throws Exception {
        PanelContext panelContext = input.getPanelContext();
        LoadingMask.showLinearProgress(panelContext);
        try {

            IPanelDesignLLMSupportService.get().llmInitPanelDesignBatch(progress, observer, forms, null);

        } finally {
            progress.finish();
            LoadingMask.hide(panelContext);
        }
    }

    // 在表单上执行大模型初始化
    default void doLlmInitPanelDesignOnFormView(BaseFeActionParameter input, OctoDomainOpObserver observer, Progress<?> progress, Form form) throws Exception {
        AbsFormView formView = (AbsFormView) input.getCurrentComponent();
        PanelContext panelContext = input.getPanelContext();
        if (Op.isNewForm(form)) throw PanelDesignException.Builder.saveFirst();


        LoadingMask.showLinearProgress(panelContext);


        try {

            IPanelDesignLLMSupportService.get().llmInitPanelDesign(progress, observer, form, null);


            try (IDao dao = IDaoService.newIDao()) {
                form = IFormMgr.get().queryFormByCode(dao, form.getFormModelId(), form.getString(Form.Code));
                Op.refreshFormViewData(panelContext, formView, form);

            }
        } finally {
            progress.finish();
            LoadingMask.hide(panelContext);
        }
    }


    // 根据面板编号查询面板Form
    default List<Form> doQueryPanelDesignByPanelCodes(IDao dao, OctoDomainOpObserver observer, List<String> panelCodes) throws Exception {
        String targetFormModelId = WorkBenchConst.FormModelId_PanelDesign;
        Cnd cnd = Op.getBusDomainFilterCondition(observer, targetFormModelId);

        cnd.where().and(new SqlExpressionGroup().or(new SqlExpressionGroup().andInStrList(Op.getFieldCode("面板编号"), panelCodes)).or(new SqlExpressionGroup().andInStrList(Form.Code, panelCodes)));


        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, targetFormModelId, cnd, 1, 1, false, true);

        if (queryRs.isEmpty()) return CollUtil.newArrayList();

        return queryRs.getDataList();
    }

    // 根据来源场景查询面板Form
    default List<Form> doQueryPanelDesignBySceneCodes(IDao dao, OctoDomainOpObserver observer, List<String> sceneCodes) throws Exception {
        String targetFormModelId = WorkBenchConst.FormModelId_PanelDesign;
        Cnd cnd = Op.getBusDomainFilterCondition(observer, targetFormModelId);

        cnd.where().and(new SqlExpressionGroup().andInStrList(Op.getFieldCode("所属场景"), sceneCodes));


        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, targetFormModelId, cnd, 1, 1, false, true);

        if (queryRs.isEmpty()) return new ArrayList<>();

        return queryRs.getDataList();

    }


}
