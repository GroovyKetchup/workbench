package octo.cm.util;

import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.kwaidoo.ms.tool.CmnUtil;
import com.leavay.dfc.gui.LvUtil;
import gpf.adur.data.*;
import octo.cm.dto.RelationFieldMapping;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("Form转换为Json的工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-12", updateTime = "2025-08-12"
)
public class FormToJsonConversionUtil {

    private static final EasyOperation Op = EasyOperation.get();
    public static final String PREFIX_NEED_UUID = "_uuid";

    // 转换Form为JSONObject
    public static JSONObject convert(Form form) throws Exception {

        return convert(null, form, null);
    }

    // 转换Form为JSONObject
    public static JSONObject convert(IDao dao, Form form, List<RelationFieldMapping> relationFieldMappings) throws Exception {
        if (form == null) return null;


        JSONObject jsonObject = new JSONObject();


        // FIXME 后续调整
        boolean isNeedUuid = CmnUtil.getBoolean(form.getBoolean(PREFIX_NEED_UUID), false);
        if (isNeedUuid) {
            jsonObject.set(PREFIX_NEED_UUID, form.getString(Form.UUID));
        }

        // 要从formModel中找到对应的英文名，否则使用字段名称
        String formModelId = form.getFormModelId();
        FormModel formModel = IFormMgr.get().queryFormModel(formModelId);
        if (formModel != null) {
            for (FormField formField : formModel.getNotHiddenFieldList()) {
                String fieldCode = formField.getCode();
                String fieldName = formField.getName();
                Object attrValue = form.getAttrValue(fieldCode);
                if (attrValue == null) continue;

                // 最终的变量名
                String variableName = fieldName;

                Object variableValue = attrValue;

                // 如果是嵌套表格，特殊处理为JSONArray
                if (variableValue instanceof TableData) {
                    variableValue = convertTableData(dao, (TableData) variableValue, relationFieldMappings, isNeedUuid);
                } else if (variableValue instanceof AssociationData) {
                    variableValue = convertAssociationData(dao, (AssociationData) variableValue, relationFieldMappings, formField);
                } else if (variableValue instanceof List) {
                    List list = (List) variableValue;
                    if (!list.isEmpty()) {
                        Object tmpValue = list.get(0);
                        if (tmpValue instanceof AssociationData) {
                            variableValue = convertAssociationDatas(dao, (List<AssociationData>) list, relationFieldMappings, formField);
                        } else {
                            variableValue = StrUtil.format("暂时不支持属性类型[List<{}>]", tmpValue.getClass());
                        }
                    }


                }

                jsonObject.put(variableName, variableValue);


            }


        }


        return jsonObject;


    }

    // 转换关联数据列表为JSON字符串
    private static JSONArray convertAssociationDatas(IDao dao, List<AssociationData> tmpValue,
                                                     List<RelationFieldMapping> relationFieldMappings, FormField formField) throws Exception {
        JSONArray jsonArray = new JSONArray();
        for (AssociationData associationData : tmpValue) {
            Object val = convertAssociationData(dao, associationData, relationFieldMappings, formField);
            if (val == null ||
                    (val instanceof String && StrUtil.isBlank((String) val))) {
                continue;
            }

            jsonArray.add(val);
        }
        return jsonArray;
    }

    // 转换关联数据为JSON字符串
    private static Object convertAssociationData(IDao dao, AssociationData variableValue,
                                                 List<RelationFieldMapping> relationFieldMappings, FormField formField) throws Exception {
        if (variableValue == null) return null;

        RelationFieldMapping relationFieldMapping = getRelationFieldMapping(relationFieldMappings, formField);
//        LvUtil.trace(StrUtil.format("准备映射:{}, Mapping:{}", formField.getName(), JSONUtil.toJsonStr(relationFieldMapping)));
        if (relationFieldMapping != null) {
            Object result = executeRelationFieldMapping(dao, relationFieldMappings, relationFieldMapping, variableValue);
//            LvUtil.trace(StrUtil.format("映射结果:{}", JSONUtil.toJsonStr(result)));
            return result;
        }
        return variableValue.getValue();
    }

    // 转换嵌套表格为JSONArray
    private static JSONArray convertTableData(IDao dao, TableData variableValue,
                                              List<RelationFieldMapping> relationFieldMappings, boolean isNeedUuid) throws Exception {
        if (variableValue == null) return null;
        JSONArray nestedForms = new JSONArray();
        for (Form nestedForm : variableValue.getRows()) {
            if (isNeedUuid) {
                nestedForm.setAttrValue(PREFIX_NEED_UUID, true);
            }
            JSONObject nestedJsonObject = convert(dao, nestedForm, relationFieldMappings);
            if (nestedJsonObject != null) {
                nestedForms.add(nestedJsonObject);
            }
        }

        if (!nestedForms.isEmpty()) {
            return nestedForms;
        }

        return null;
    }


    // 执行映射，拿到结果
    private static Object executeRelationFieldMapping(IDao dao, List<RelationFieldMapping> relationFieldMappings, RelationFieldMapping relationFieldMapping,
                                                      AssociationData variableValue) throws Exception {
        if (dao == null) {
            LvUtil.trace("dao不存在或不存活");
            return null;
        }
        if (relationFieldMapping == null) {
            LvUtil.trace("没有字段映射");
            return null;
        }
        if (variableValue == null) {
            LvUtil.trace("AC不存在");
            return null;
        }


        if (relationFieldMapping.isUseCodeMapping()) {
            return variableValue.getValue();
        }


        try {
            Cnd queryCnd = Cnd.NEW();
            queryCnd.where().andEquals(Form.Code, variableValue.getValue());

            Boolean isAllowQueryNestingData = relationFieldMapping.isAllowQueryNestingData();
            if (isAllowQueryNestingData == null) isAllowQueryNestingData = false;

            // FIXME 暂不支持多层嵌套查询，先避免可能出现无限递归的问题，后续再处理
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, variableValue.getFormModelId(), queryCnd, 1, Integer.MAX_VALUE,
                    false, isAllowQueryNestingData);
            if (queryRs.isEmpty()) {
                LvUtil.trace("没有找到AC对应的Form");
                return null;
            }

            Form totalForm = queryRs.getDataList().get(0);

            Boolean isMappingTotalForm = relationFieldMapping.isMappingTotalForm();
            if (isMappingTotalForm == null) isMappingTotalForm = false;
//            LvUtil.trace("isMappingTotalForm:" + isMappingTotalForm);

            if (isMappingTotalForm) {
                return convert(dao, totalForm, relationFieldMappings);
            }

            Boolean isMappingAssignFormField = relationFieldMapping.isMappingAssignFormField();
            if (isMappingAssignFormField == null) isMappingAssignFormField = false;
//            LvUtil.trace("isMappingAssignFormField:" + isMappingAssignFormField);
            if (isMappingAssignFormField) {

                return totalForm.getString(relationFieldMapping
                        .getTargetMappingAssignFormFieldName());
            }

            return null;
        } catch (Exception e) {
            return null;
        }
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


}
