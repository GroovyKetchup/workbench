package octo.cm.util;

import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.dfc.gui.LvUtil;
import com.leavay.ms.tool.CmnUtil;
import gpf.adur.data.*;
import octo.cm.dto.RelationFieldMapping;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Comment("Json转换为Form的工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-12", updateTime = "2025-08-12"
)
public class JsonToFormConversionUtil {

    // 是否开启日志
    private static final Boolean isStartLogging = false;
    // 简单操作
    private static final EasyOperation Op = EasyOperation.get();
    public static final String FIELD_NAME_CN_CODE = "编号";

    // 转换JSONObject为Form
    public static Form convert(Form originalForm, JSONObject jsonObject) throws Exception {
        return convert(null, null, originalForm, jsonObject, null, null);
    }

    // 转换JSONObject为Form
    public static Form convert(IDao dao, OctoDomainOpObserver observer, Form originalForm, JSONObject jsonObject,
                               List<RelationFieldMapping> relationFieldMappings) throws Exception {
        return convert(dao, observer, originalForm, jsonObject, relationFieldMappings, null);
    }


    public static Form convert(IDao dao, OctoDomainOpObserver observer, Form originalForm, JSONObject jsonObject,
                               List<RelationFieldMapping> relationFieldMappings,
                               Map<String, Map<String, String>> formCodeMapping) throws Exception {

        if (jsonObject == null) return originalForm;
        if (originalForm == null) return null;

        // Json to Form 本质是基于json的数据进行补全，而并非是直接进行转换
        String formModelId = originalForm.getFormModelId();
        FormModel formModel = IFormMgr.get().queryFormModel(formModelId);

        if (formModel == null) return null;

        // 当有form新增的时候将自己老的Code和新的Code都传递进来
        // 然后所有关联字段如果开启了“isUseCodeMapping”就可以拿到最新的Code
        // 不过后面可能会遇到顺序的问题，关联条件优先于实际创建
        // 分为几个阶段：
        // 1、先直接观察有没有问题，没问题就不处理了；
        // 2、如果有问题就加一个排序机制
        // 3、后续如果需要就将这个设计调整为两阶段保存
        if (formCodeMapping == null) formCodeMapping = new LinkedHashMap<>();

        for (FormField formField : formModel.getNotHiddenFieldList()) {
            String fieldName = formField.getName();

            // 英文名默认放在了description中
            // 最终的变量名（优先使用英文名，否则使用code）
            Object originalFormAttrValue = originalForm.getAttrValue(fieldName);

            // 从JSON中获取对应的值
            if (jsonObject.containsKey(fieldName)) {
                Object jsonValue = jsonObject.get(fieldName);
                Object formValue = convertJsonValueToFormValue(dao, observer, originalFormAttrValue,
                        jsonValue, formField, formModel, relationFieldMappings, formCodeMapping);

                originalForm.setAttrValue(fieldName, formValue);

                if (originalFormAttrValue instanceof TableData && formValue == null) {
                    originalForm.setAttrValue(fieldName,
                            new TableData(((TableData) originalFormAttrValue).getFormModelId()));
                }
            }
        }

        // FIXME 后续调整
        String uuid = jsonObject.getStr("_uuid");
        if (StrUtil.isNotBlank(uuid)) {
            originalForm.setUuid(uuid);
        }


        return originalForm;

    }

// ========================= 转换方法 =========================


    // 将JSONArray转换为TableData
    private static Object convertJsonArray(IDao dao, OctoDomainOpObserver observer, Object originalFormAttrValue,
                                           JSONArray jsonArray, FormField formField, FormModel parentFormModel, List<RelationFieldMapping> relationFieldMappings,
                                           Map<String, Map<String, String>> formCodeMapping) throws Exception {
        if (jsonArray == null || jsonArray.isEmpty()) {
            return null;
        }

        // 1、如果是关联多选
        String assocFormModel = formField.getAssocFormModel();
        if (StrUtil.isNotBlank(assocFormModel)) {
            List<AssociationData> acs = new ArrayList<>();
            for (Object object : jsonArray) {
                try {
                    Object result = convertAttrValue(dao, observer, object, formField, null, relationFieldMappings, formCodeMapping);
                    if (result instanceof AssociationData) {
                        acs.add((AssociationData) result);
                    }
                    continue;
                } catch (Exception e) {
                    // 报错即使内部不匹配，那么走原本的逻辑
                }

                if (object instanceof String) {
                    // 原本的逻辑
                    acs.add(new AssociationData(assocFormModel, (String) object));

                }

            }

            return acs;

        }

        // 如果是嵌套表格
        String tableFormModel = formField.getTableFormModel();
        if (StrUtil.isNotBlank(tableFormModel)) {
            TableData tableData = new TableData(tableFormModel);
            List<Form> rows = new ArrayList<>();
            for (Object object : jsonArray) {
                if (object instanceof JSONObject) {
                    // 这里还要匹配已经有的..
                    Form currentForm = findOrCreateOriginalForm(originalFormAttrValue, (JSONObject) object, tableFormModel);
                    // FIXME 都是新增的，是否需要复用？
                    Form form = convert(dao, observer, currentForm, (JSONObject) object, relationFieldMappings, formCodeMapping);
                    rows.add(form);
                }
            }
            tableData.setRows(rows);
            return tableData;

        }

        // 这里就是未知的类型了
        log(StrUtil.format("无法处理JSONArray数据，formField为：{}, json数据为:{}", JSONUtil.toJsonStr(formField),
                JSONUtil.toJsonStr(jsonArray)));
        return null;


    }


    // 将JSON值转换为Form字段值
    private static Object convertJsonValueToFormValue(IDao dao, OctoDomainOpObserver observer, Object originalFormAttrValue, Object jsonValue, FormField formField,
                                                      FormModel formModel, List<RelationFieldMapping> relationFieldMappings, Map<String, Map<String, String>> formCodeMapping) throws Exception {
        if (jsonValue == null) return null;
        // 根据字段类型进行转换
        // 如果是JSONArray，可能是嵌套表格数据
        if (jsonValue instanceof JSONArray) {
            return convertJsonArray(dao, observer, originalFormAttrValue, (JSONArray) jsonValue,
                    formField, formModel, relationFieldMappings, formCodeMapping);
        }

        // 如果是字符串，可能是关联数据或普通字符串
        return convertAttrValue(dao, observer, jsonValue, formField, formModel, relationFieldMappings, formCodeMapping);

    }

    // 将JSON值转换为Form字段值
    private static Object convertAttrValue(IDao dao, OctoDomainOpObserver observer, Object jsonValue, FormField formField,
                                           FormModel formModel, List<RelationFieldMapping> relationFieldMappings,
                                           Map<String, Map<String, String>> formCodeMapping) throws Exception {
        if (jsonValue == null) return null;

//        log(StrUtil.format("jsonValue:{}", JSONUtil.toJsonStr(jsonValue)));

        String assocFormModel = formField.getAssocFormModel();
        if (StrUtil.isBlank(assocFormModel)) return jsonValue;

        RelationFieldMapping relationFieldMapping = getRelationFieldMapping(relationFieldMappings, formField);
//        log(StrUtil.format("准备映射:{}, 映射规则:{}", formField.getName(), JSONUtil.toJsonStr(relationFieldMapping)));

        if (relationFieldMapping == null) {
            return new AssociationData(assocFormModel, (String) jsonValue);
        }

        Object result = null;
        try {
            result = executeRelationFieldMapping(dao, observer, relationFieldMappings,
                    relationFieldMapping, formCodeMapping, formField, jsonValue);
        } catch (Exception e) {
            log(StrUtil.format("映射失败:{}", ExceptionUtils.getFullStackTrace(e)));
        }
//        log(StrUtil.format("映射结果:{}", JSONUtil.toJsonStr(result)));
        return result;


    }


    // ========================= 支撑方法 =========================


    private static Object executeRelationFieldMapping(IDao dao, OctoDomainOpObserver observer, List<RelationFieldMapping> relationFieldMappings,
                                                      RelationFieldMapping relationFieldMapping,
                                                      Map<String, Map<String, String>> formCodeMapping,
                                                      FormField formField, Object jsonValue) throws Exception {

        if (relationFieldMapping == null) {
            log("没有字段映射");
            return null;
        }
        if (formField == null) {
            log("formField不存在");
            return null;
        }
        if (jsonValue == null) {
            log("数据不存在");
            return null;
        }


        String fieldName = relationFieldMapping.getFieldName();
        String assocFormModel = formField.getAssocFormModel();

        // 1、如果是简单复用CodeMapping，直接处理就好
        if (relationFieldMapping.isUseCodeMapping()) {
            String code = getCodeFromCodeMapping(formCodeMapping, assocFormModel, (String) jsonValue);
            return new AssociationData(assocFormModel, code);
        }

        // 2、数值是映射到某个关联的字段身上，那么jsonValue就是目标字段
        boolean isMappingAssignFormField = CmnUtil.getBoolean(relationFieldMapping.isMappingAssignFormField(), false);
        if (isMappingAssignFormField && jsonValue instanceof String) {
            String targetMappingAssignFormFieldName = relationFieldMapping.getTargetMappingAssignFormFieldName();

            Form form = Op.queryFormByCondition(dao, assocFormModel,
                    targetMappingAssignFormFieldName, (String) jsonValue, cnd -> {
                        SqlExpression sql = Op.getBusDomainFilterExpr(observer, assocFormModel);
                        cnd.where().and(sql);
                        return cnd;
                    });

            if (form == null) {
                log(StrUtil.format("未找到指定字段对应的Form, 准备创建, 字段名:{}, 被映射的字段名:{}, 实际在字段值:{}",
                        fieldName, targetMappingAssignFormFieldName));


                String finalFormCode = IdUtil.fastSimpleUUID();
                form = new Form(assocFormModel).setAttrValue(Form.Code, finalFormCode);
                form.setAttrValue(targetMappingAssignFormFieldName, jsonValue);
                String customFormCode = relationFieldMapping.getCustomFormCode();
                if (StrUtil.isNotBlank(customFormCode)) {
                    form.setAttrValue(customFormCode, StrUtil.format("{}_{}", observer.getDomainCode(), finalFormCode));
                }
                form = IFormMgr.get().createForm(null, dao, form, observer);
                return Op.toAssociationData(form);

            }
            return Op.toAssociationData(form);


        }


        // 3、是直接映射为某个表单，那么jsonValue就是表单对应的对象
        boolean isMappingTotalForm = CmnUtil.getBoolean(relationFieldMapping.isMappingTotalForm(), false);
        boolean isReUseByAssignFormFieldName = CmnUtil.getBoolean(relationFieldMapping.isReUseByAssignFormFieldName(), false);
        if (isMappingTotalForm && jsonValue instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) jsonValue;
            String formCode = getFormCodeFromJsonObj(jsonObject);

            Form form = null;
            if (StrUtil.isNotBlank(formCode)) {
                form = Op.queryFormByCondition(dao, assocFormModel, Form.Code, formCode, null);
            }

            // 指定字段进行复用的情况
            // 如果之前找到了自己对应的form那就算了
            if (form == null && isReUseByAssignFormFieldName) {
                String targetMappingAssignFormFieldName = relationFieldMapping.getTargetMappingAssignFormFieldName();
                String beReUsedFieldValue = jsonObject.getStr(targetMappingAssignFormFieldName);
                if (StrUtil.isNotBlank(beReUsedFieldValue)) {

                    form = Op.queryFormByCondition(dao, assocFormModel, cnd -> {
                        SqlExpression sql = Op.getBusDomainFilterExpr(observer, assocFormModel);
                        cnd.where().and(sql)
                                .andEquals(Op.getFieldCode(targetMappingAssignFormFieldName),
                                        beReUsedFieldValue
                                );
                        cnd.orderBy(Form.Code, "asc");

                        ;
                        return cnd;
                    });
                    if (form != null) {
                        formCode = form.getString(Form.Code);
                    }


                }
            }

            boolean isCreate = form == null;
            if (isCreate) {

                String oldFormCode = formCode;

                form = new Form(assocFormModel).setAttrValue(Form.Code, IdUtil.fastSimpleUUID());
                form = IFormMgr.get().createForm(null, dao, form, observer);
                formCode = form.getString(Form.Code);

                // 更新Code映射
                updateCodeMappingItemWhenFormCreated(formCodeMapping, assocFormModel, oldFormCode, formCode);

            }

            // 防止使用前端构建的编号（可能存在非法字符）
            setFormCodeFromJsonObj(jsonObject, formCode);
            String customFormCode = relationFieldMapping.getCustomFormCode();

            // 允许前端自行构建定制的编号，所以加一个判空
            if (StrUtil.isNotBlank(customFormCode) &&
                    StrUtil.isBlank(jsonObject.getStr(customFormCode))) {
                jsonObject.set(customFormCode, formCode);
            }

            form = convert(dao, observer, form, jsonObject, relationFieldMappings, formCodeMapping);

            // 只更新自己业务域的Form
            if (formCode.startsWith(observer.getDomainCode())) {
                form = IFormMgr.get().updateForm(null, dao, form, observer);
            }
            return Op.toAssociationData(form);

        }

        return null;
    }


    // 获取Code映射
    private static String getCodeFromCodeMapping(
            Map<String, Map<String, String>> formCodeMapping,
            String assocFormModel, String oldFormCode) {
        if (formCodeMapping == null || !formCodeMapping.containsKey(assocFormModel)) {
            return oldFormCode;
        }
        Map<String, String> mapping = formCodeMapping.getOrDefault(assocFormModel, new LinkedHashMap<>());

        String newFormCode = mapping.get(oldFormCode);
        if (StrUtil.isBlank(newFormCode)) return oldFormCode;

        return newFormCode;

    }

    // 更新Code映射
    private static void updateCodeMappingItemWhenFormCreated(
            Map<String, Map<String, String>> formCodeMapping,
            String assocFormModel, String oldFormCode, String newFormCode) {
        Map<String, String> mapping = formCodeMapping.getOrDefault(assocFormModel, new LinkedHashMap<>());
        mapping.put(oldFormCode, newFormCode);
        formCodeMapping.put(assocFormModel, mapping);

    }


    private static Form findOrCreateOriginalForm(Object originalFormAttrValue, JSONObject object, String tableFormModel) throws Exception {

        if (object == null) return null;
        String formCode = object.getStr(Form.Code);


        if (StrUtil.isNotBlank(formCode) &&
                originalFormAttrValue instanceof TableData) {

            for (Form row : ((TableData) originalFormAttrValue).getRows()) {
                if (formCode.equals(row.getString(Form.Code))) {
                    return row;
                }
            }
        }

        if (StrUtil.isBlank(formCode)) formCode = IdUtil.fastSimpleUUID();
        return new Form(tableFormModel).setAttrValue(Form.Code, formCode);
    }


    // 获取关联字段映射
    private static RelationFieldMapping getRelationFieldMapping(List<RelationFieldMapping> relationFieldMappings,
                                                                FormField formField) {
        if (Op.isEmpty(relationFieldMappings)) return null;
        if (formField == null) return null;

        for (RelationFieldMapping relationFieldMapping : relationFieldMappings) {
            if (relationFieldMapping.isMatch(formField)) {
                return relationFieldMapping;
            }
        }

        return null;
    }


    private static String getFormCodeFromJsonObj(JSONObject jsonObject) {
        if (jsonObject == null) return null;
        String formCode = jsonObject.getStr(FIELD_NAME_CN_CODE);
        if (StrUtil.isBlank(formCode)) formCode = jsonObject.getStr(Form.Code);
        return formCode;
    }

    private static void setFormCodeFromJsonObj(JSONObject jsonObject, String formCode) {
        if (jsonObject == null || StrUtil.isBlank(formCode)) return;
        jsonObject.set(Form.Code, formCode);
        jsonObject.set(FIELD_NAME_CN_CODE, formCode);
    }


    private static void log(String str) {
//        if (!isStartLogging) return;
        LvUtil.trace(str);
    }

}
