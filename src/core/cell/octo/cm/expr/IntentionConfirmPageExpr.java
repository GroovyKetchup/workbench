package cell.octo.cm.expr;

import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.dfc.gui.LvUtil;
import fe.orchestration.component.OrchestrationNode2GptPanel;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.RelationFieldMapping;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.DomainException;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octo.cm.util.JsonToFormConversionUtil;
import octo.cm.util.PanelCategoryUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;

import static octo.cm.constant.WorkBenchConst.FormModelId_PanelDesign_Category;

@Comment("意图确认操作函数")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-17", updateTime = "2025-09-17"
)
public interface IntentionConfirmPageExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();

    // 意图确认-关联字段映射
    List<RelationFieldMapping> INTENTION_CONFIRM_RELATION_FIELD_MAPPINGS = CollUtil.newArrayList(new RelationFieldMapping()
            .setMappingAssignFormField(true)
            .setFieldName("使用角色")
            .setFieldFormModelId(WorkBenchConst.FormModelId_Axis_Role)
            .setTargetMappingAssignFormFieldName("角色名称")
            .setCustomFormCode("角色编号")
    );

    // ========================= 全量数据操作 =========================

    @MethodDeclare(
            label = "获取意图数据", how = "", what = "", why = "",
            inputs = {}
    )
    default JSONObject getIntentionData(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        JSONObject finalObj = new JSONObject();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        try (IDao dao = IDaoService.newIDao()) {

            finalObj.put("系统", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SystemLayer));
            finalObj.put("模块", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SubGoalLayer));
            finalObj.put("场景", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer));
            finalObj.put("数据", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Data));
            finalObj.put("行为", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Behavior));
            finalObj.put("约束", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Constraint));
            finalObj.put("编排", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Orchestration));
            finalObj.put("展示", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Display));
            finalObj.put("验证", doQueryFormAndConvertToJson(dao, observer, WorkBenchConst.FormModelId_SceneLayer_Verification));


        }


        return finalObj;


    }


    @MethodDeclare(
            label = "保存意图数据", how = "", what = "", why = "",
            inputs = {}
    )
    default void saveIntentionData(String busDomainCode, String jsonData) throws Exception {

//        JsonToFormConversionUtil.convert()
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(jsonData)) throw CommonException.Builder.jsonDataEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        JSONObject finalObj;
        try {
            finalObj = JSONUtil.parseObj(jsonData);
        } catch (Exception e) {
            throw CommonException.Builder.invalidJsonData();
        }


        try (IDao dao = IDaoService.newIDao()) {
            // 前端维护了Code的关系，后端需要重新做映射
            Map<String, String> newFormCodeMapping = new HashMap<>();

            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SystemLayer, finalObj.getJSONArray("系统"));

            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SubGoalLayer, finalObj.getJSONArray("模块"));

            List<Form> scenePublishResultForms = doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer, finalObj.getJSONArray("场景"));

            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Data, finalObj.getJSONArray("数据"));
            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Behavior, finalObj.getJSONArray("行为"));
            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Constraint, finalObj.getJSONArray("约束"));
            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Orchestration, finalObj.getJSONArray("编排"));
            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Display, finalObj.getJSONArray("展示"));
            doUpdateFormByJson(dao, observer, newFormCodeMapping, WorkBenchConst.FormModelId_SceneLayer_Verification, finalObj.getJSONArray("验证"));


            if (!Op.isEmpty(scenePublishResultForms)) {

                try {
                    // 如果节点没有场景类型，则设置默认值
                    setDefaultSceneTypeIfNull(dao, scenePublishResultForms, observer);

                } catch (Exception ignored) {
                }


            }


            dao.commit();

        }


    }


    // ========================= 单节点操作 =========================


    @MethodDeclare(
            label = "保存意图节点数据", how = "", what = "单独去保存单个节点的数据并返回最新版本", why = "",
            inputs = {}
    )
    default void saveIntentionNodeData(String busDomainCode, String nodeType, Object jsonData) throws Exception {

        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(nodeType)) throw CommonException.Builder.nodeTypeEmpty();
        if (jsonData == null || (jsonData instanceof String && StrUtil.isBlank((String) jsonData)))
            throw CommonException.Builder.jsonDataEmpty();


        try (IDao dao = IDaoService.newIDao()) {

            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
            if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

            String jsonDataStr = (jsonData instanceof String) ? (String) jsonData :
                    JSONUtil.toJsonStr(jsonData);

            doSaveIntentionNodeData(dao, observer, nodeType, jsonDataStr);

            dao.commit();


        }


    }


    @MethodDeclare(
            label = "批量保存意图节点数据", how = "", what = "单独去保存单个节点的数据并返回最新版本", why = "",
            inputs = {}
    )
    default void batchSaveIntentionNodeData(String busDomainCode, String nodeType,
                                                  List<Object> jsonDatas) throws Exception {

        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(nodeType)) throw CommonException.Builder.nodeTypeEmpty();
        if (Op.isEmpty(jsonDatas)) throw CommonException.Builder.jsonDataEmpty();


        try (IDao dao = IDaoService.newIDao()) {


            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
            if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

            for (Object jsonDataObj : jsonDatas) {
                String jsonData = (jsonDataObj instanceof String) ? (String) jsonDataObj :
                        JSONUtil.toJsonStr(jsonDataObj);
                doSaveIntentionNodeData(dao, observer, nodeType, jsonData);
            }


            dao.commit();


        }


    }

    @MethodDeclare(
            label = "删除意图节点数据", how = "", what = "单独去删除单个节点的数据", why = "",
            inputs = {}
    )
    default void deleteIntentionNodeData(String busDomainCode, String nodeType, String nodeCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(nodeType)) throw CommonException.Builder.nodeTypeEmpty();
        if (StrUtil.isBlank(nodeCode)) throw CommonException.Builder.nodeCodeEmpty();


        try (IDao dao = IDaoService.newIDao()) {

            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
            if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

            doDeleteIntentionNodeData(dao, nodeType, nodeCode);

            dao.commit();

        }


    }

    @MethodDeclare(
            label = "批量删除意图节点数据", how = "", what = "单独去删除单个节点的数据", why = "",
            inputs = {}
    )
    default void batchDeleteIntentionNodeData(String busDomainCode, String nodeType, List<String> nodeCodes) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(nodeType)) throw CommonException.Builder.nodeTypeEmpty();
        if (Op.isEmpty(nodeCodes)) throw CommonException.Builder.nodeCodeEmpty();


        try (IDao dao = IDaoService.newIDao()) {

            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
            if (observer == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);
            for (String nodeCode : nodeCodes) {
                doDeleteIntentionNodeData(dao, nodeType, nodeCode);
            }

            dao.commit();

        }


    }

    // ========================= 支撑方法 =========================


    // 删除意图节点数据
    default void doDeleteIntentionNodeData(IDao dao, String nodeType, String nodeCode) throws Exception {
        String formModelId = getNodeFormModelIdByNodeType(nodeType);
        if (StrUtil.isBlank(formModelId)) throw CommonException.Builder.nodeTypeNotSupported(nodeType);

        Form form = Op.queryFormByCondition(dao, formModelId, Form.Code, nodeCode, null);
        if (form == null)
            throw CommonException.Builder.nodeNotFoundWithCode(nodeCode);

        IFormMgr.get().deleteForm(dao, form.getFormModelId(), form.getUuid());
    }

    // 保存意图节点数据
    default void doSaveIntentionNodeData(IDao dao, OctoDomainOpObserver observer, String nodeType, String jsonData) throws Exception {
        JSONObject finalObj;
        try {
            finalObj = JSONUtil.parseObj(jsonData);
        } catch (Exception e) {
            throw CommonException.Builder.invalidJsonData();
        }

        boolean isCreate = false;
        String formCode = finalObj.getStr("编号");
        if (StrUtil.isBlank(formCode)) formCode = finalObj.getStr("code");
        if (StrUtil.isBlank(formCode)) {
            isCreate = true;
            formCode = IdUtil.fastSimpleUUID();
        }

        // 节点类型: 系统、模块、场景、实现、行为、约束、编排、展示、验证
        String formModelId = getNodeFormModelIdByNodeType(nodeType);
        if (StrUtil.isBlank(formModelId)) throw CommonException.Builder.nodeTypeNotSupported(nodeType);


        Form form = Op.queryFormByAc(dao, new AssociationData(formModelId, formCode));
        if (form == null) {
            isCreate = true;
            form = Op.newForm(formModelId);
        }

        form = JsonToFormConversionUtil.convert(dao, observer, form, finalObj,
                INTENTION_CONFIRM_RELATION_FIELD_MAPPINGS);

        if (isCreate) {
            form = IFormMgr.get().createForm(null, dao, form, observer);
        } else {
            form = IFormMgr.get().updateForm(null, dao, form, observer);
        }

//            return FormToJsonConversionUtil.convert(dao, form, INTENTION_CONFIRM_RELATION_FIELD_MAPPINGS);
    }


    // 设置场景默认类型
    default void setDefaultSceneTypeIfNull(IDao dao, List<Form> scenePublishResultForms, OctoDomainOpObserver observer) throws Exception {
        Form infoMgrType = Op.queryFormByCondition(dao, FormModelId_PanelDesign_Category, "分类名称", PanelCategoryUtil.CategoryType_InformationMgr, null);
        if (infoMgrType != null) {
            AssociationData infoMgrTypeAc = Op.toAssociationData(infoMgrType);
            for (Form sceneForm : scenePublishResultForms) {
                AssociationData sceneType = sceneForm.getAssociation("场景类型");
                if (sceneType != null) continue;
                sceneForm.setAttrValue("场景类型", infoMgrTypeAc);
                IFormMgr.get().updateForm(null, dao, sceneForm, observer);
            }
        }
    }


    // 执行保存Form，通过JSON
    default List<Form> doUpdateFormByJson(IDao dao, OctoDomainOpObserver observer, Map<String, String> newFormCodeMapping, String formModelId, JSONArray jsonArray) throws Exception {
        if (jsonArray == null || jsonArray.isEmpty()) return null;
        String domainCode = observer.getDomainCode();


        List<Form> waitCreateForms = new ArrayList<>();
        List<Form> waitUpdateForms = new ArrayList<>();

        // 当前不存在的 Form
        Set<String> nonexistentFormCodes = doQueryAllFormCodes(dao, observer, formModelId);

        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String formCode = jsonObject.getStr("编号");
            if (StrUtil.isBlank(formCode)) formCode = jsonObject.getStr("code");
            if (StrUtil.isBlank(formCode)) {
                // 按道理前端都会自己设置，但是现在没有设置，后端做下补救
                formCode = IdUtil.fastSimpleUUID();
//                LvUtil.trace(StrUtil.format("跳过保存意图数据，因为不存在code:\n{}", jsonObject));
//                continue;
            }

            // 从“不存在的编号集合”中移除，标志它依然存在
            nonexistentFormCodes.remove(formCode);

            boolean isCreate = false;
            Form form = Op.queryFormByAc(dao, new AssociationData(formModelId, formCode));
            if (form == null) {
                isCreate = true;
                form = Op.newForm(formModelId);
                LvUtil.trace(StrUtil.format("系统中不存在数据，准备新增:\n{}", jsonObject));
            }

            Form finalForm = JsonToFormConversionUtil.convert(dao, observer, form, jsonObject, INTENTION_CONFIRM_RELATION_FIELD_MAPPINGS);

            if (isCreate) {
                waitCreateForms.add(finalForm);
            } else {
                waitUpdateForms.add(finalForm);

            }

        }


        // 先增加
        for (Form waitCreateForm : waitCreateForms) {
            String targetFmId = waitCreateForm.getFormModelId();
            String targetFormCode = waitCreateForm.getString(Form.Code);

            Form newForm = new Form(targetFmId);
            newForm.setUuid(waitCreateForm.getUuid());
            newForm.setAttrValue(Form.Code, targetFormCode);

            LvUtil.trace("创建前CODE=" + targetFormCode);
            Form result = IFormMgr.get().createForm(null, dao, newForm, observer);

            String newFormCode = result.getString(Form.Code);

            LvUtil.trace("创建后CODE=" + newFormCode);
            newFormCodeMapping.put(targetFormCode, newFormCode);

            waitCreateForm.setAttrValue(Form.UUID, result.getUuid());
            waitCreateForm.setAttrValue(Form.Code, newFormCode);

            waitUpdateForms.add(waitCreateForm);

        }

        List<Form> finalUpdateForms = new ArrayList<>();

        // 再更新
        for (Form waitUpdateForm : waitUpdateForms) {

            // 从“不存在的编号集合”中移除，标志它依然存在
            nonexistentFormCodes.remove(waitUpdateForm.getString(Form.Code));

            for (String key : waitUpdateForm.getData().keySet()) {
                Object attrValue = waitUpdateForm.getAttrValue(key);
                if (attrValue instanceof List) {
                    Object object = ((List<?>) attrValue).get(0);
                    if (object instanceof AssociationData) {
                        for (AssociationData associationData : ((List<AssociationData>) attrValue)) {
                            String value = associationData.getValue();
                            if (newFormCodeMapping.containsKey(value)) {
                                associationData.setValue(newFormCodeMapping.get(value));
                            }
                        }
                    }
                }

                if (attrValue instanceof AssociationData) {
                    AssociationData associationData = (AssociationData) attrValue;
                    String value = associationData.getValue();
                    if (newFormCodeMapping.containsKey(value)) {
                        associationData.setValue(newFormCodeMapping.get(value));
                    }
                }


            }

            try {
                Form finalForm = IFormMgr.get().updateForm(null, dao, waitUpdateForm, observer);
                finalUpdateForms.add(finalForm);

            } catch (Exception ignored) {
                System.out.println(ignored);
            }

        }


        // 最后移除不再存在的节点
        if (!CollUtil.isEmpty(nonexistentFormCodes)) {
            Cnd deleteCnd = Cnd.NEW();
            deleteCnd.where().andInStrList(Form.Code, new ArrayList<>(nonexistentFormCodes));
            IFormMgr.get().deleteForm(dao, formModelId, deleteCnd);

        }

        return finalUpdateForms;


    }


    // 执行查询Form并转换为JSON
    default JSONArray doQueryFormAndConvertToJson(IDao dao, OctoDomainOpObserver observer, String formModelId) throws Exception {
        if (StrUtil.hasBlank(formModelId)) throw DomainException.Builder.formModelIdEmpty();

        Cnd cnd = Op.getBusDomainFilterCondition(observer, formModelId);

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, Integer.MAX_VALUE,
                false, true);

        JSONArray result = new JSONArray();


        if (!queryRs.isEmpty()) {
            for (Form form : queryRs.getDataList()) {
                result.add(
                        FormToJsonConversionUtil.convert(dao, form, INTENTION_CONFIRM_RELATION_FIELD_MAPPINGS)
                );
            }
        }

        return result;


    }

    // 获取表单所有编号
    default Set<String> doQueryAllFormCodes(IDao dao, OctoDomainOpObserver observer, java.lang.String formModelId) {
        Set<String> resultSet = new HashSet<>();

        try {
            if (StrUtil.hasBlank(formModelId)) throw new RuntimeException("空的模型ID");

            Cnd cnd = Op.getBusDomainFilterCondition(observer, formModelId);

            // FIXME 后面改为 queryFormFieldValue
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, Integer.MAX_VALUE,
                    false, false);
            if (queryRs.isEmpty()) throw new RuntimeException(StrUtil.format("没有找到任何数据[{}]", formModelId));

            for (Form form : queryRs.getDataList()) {
                String formCode = form.getString(Form.Code);
                if (StrUtil.isBlank(formCode)) continue;
                resultSet.add(formCode);
            }

            return resultSet;
        } catch (Exception e) {
            return resultSet;
        }

    }


    // 根据节点类型获取对应的FormModelId
    // 节点类型: 系统、模块、场景、数据、行为、约束、编排、展示、验证
    default String getNodeFormModelIdByNodeType(String nodeType) {

        switch (nodeType) {
            case "系统":
                return WorkBenchConst.FormModelId_SystemLayer;
            case "模块":
                return WorkBenchConst.FormModelId_SubGoalLayer;
            case "场景":
                return WorkBenchConst.FormModelId_SceneLayer;
            case "数据":
                return WorkBenchConst.FormModelId_SceneLayer_Data;
            case "行为":
                return WorkBenchConst.FormModelId_SceneLayer_Behavior;
            case "约束":
                return WorkBenchConst.FormModelId_SceneLayer_Constraint;
            case "编排":
                return WorkBenchConst.FormModelId_SceneLayer_Orchestration;
            case "展示":
                return WorkBenchConst.FormModelId_SceneLayer_Display;
            case "验证":
                return WorkBenchConst.FormModelId_SceneLayer_Verification;
            default:
                return "";

        }

    }


}
