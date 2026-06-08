package octo.cm.dto;

import cn.hutool.core.util.StrUtil;
import gpf.adur.data.FormField;

import java.io.Serializable;

// 关联字段映射
public class RelationFieldMapping implements Serializable {

    // 字段名称
    private String fieldName;
    // 表单模型ID
    private String fieldFormModelId;
    // 父表单模型ID
    private String parentFormModelId;
    // 是否映射关联字段
    private Boolean isMappingAssignFormField;
    // 是否作为一个总表单传入（前端处理的时候是一个对象，然后映射为一个关联字段）
    private Boolean isMappingTotalForm;
    // 是否使用自己的某个字段作为映射条件（比如角色模型使用角色名称作为映射条件）
    private Boolean isReUseByAssignFormFieldName;
    // 是否使用编号的映射规则
    // 适用于在一次保存中某个关联数据的创建的同时建立关联关系
    // 但对应的关联数据的编号由于业务域的关系会自动变化的情况，因此需要使用映射机制
    private Boolean isUseCodeMapping;
    // 目标关联字段名称
    private String targetMappingAssignFormFieldName;
    // 是否允许查询嵌套数据
    private Boolean isAllowQueryNestingData;
    // 自定义某个字段作为Code字段
    // 其实就是把Form.Code的值传过去
    private String customFormCode;

    public RelationFieldMapping() {
    }

    public RelationFieldMapping(String fieldName, String fieldFormModelId, Boolean isMappingAssignFormField,
                                Boolean isMappingTotalForm, String targetMappingAssignFormFieldName) {
        this.fieldName = fieldName;
        this.fieldFormModelId = fieldFormModelId;
        this.isMappingAssignFormField = isMappingAssignFormField;
        this.isMappingTotalForm = isMappingTotalForm;
        this.targetMappingAssignFormFieldName = targetMappingAssignFormFieldName;
    }

    // 是否匹配
    public boolean isMatch(FormField formField) {
        if (formField == null) return false;
        String fieldCode = formField.getCode();
        String fieldName = formField.getName();
        String assocFormModel = formField.getAssocFormModel();
        if (StrUtil.hasBlank(fieldCode, fieldName, assocFormModel)) return false;

        if (!fieldName.equals(this.fieldName)) return false;
        if (!assocFormModel.equals(this.fieldFormModelId)) return false;

        return true;

    }

    public String getFieldName() {
        return fieldName;
    }

    public RelationFieldMapping setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getFieldFormModelId() {
        return fieldFormModelId;
    }

    public RelationFieldMapping setFieldFormModelId(String fieldFormModelId) {
        this.fieldFormModelId = fieldFormModelId;
        return this;
    }

    public String getParentFormModelId() {
        return parentFormModelId;
    }

    public RelationFieldMapping setParentFormModelId(String parentFormModelId) {
        this.parentFormModelId = parentFormModelId;
        return this;
    }

    public Boolean isMappingAssignFormField() {
        return isMappingAssignFormField;
    }

    public RelationFieldMapping setMappingAssignFormField(Boolean mappingAssignFormField) {
        isMappingAssignFormField = mappingAssignFormField;
        return this;
    }

    public Boolean isMappingTotalForm() {
        return isMappingTotalForm;
    }

    public RelationFieldMapping setMappingTotalForm(Boolean mappingTotalForm) {
        isMappingTotalForm = mappingTotalForm;
        return this;
    }

    public String getTargetMappingAssignFormFieldName() {
        return targetMappingAssignFormFieldName;
    }

    public RelationFieldMapping setTargetMappingAssignFormFieldName(String targetMappingAssignFormFieldName) {
        this.targetMappingAssignFormFieldName = targetMappingAssignFormFieldName;
        return this;
    }

    public Boolean isAllowQueryNestingData() {
        return isAllowQueryNestingData;
    }

    public RelationFieldMapping setAllowQueryNestingData(Boolean allowQueryNestingData) {
        isAllowQueryNestingData = allowQueryNestingData;
        return this;
    }

    public Boolean isReUseByAssignFormFieldName() {
        return isReUseByAssignFormFieldName != null && isReUseByAssignFormFieldName;
    }

    public RelationFieldMapping setReUseByAssignFormFieldName(Boolean reUseByAssignFormFieldName) {
        isReUseByAssignFormFieldName = reUseByAssignFormFieldName;
        return this;
    }

    public String getCustomFormCode() {
        return customFormCode;
    }

    public RelationFieldMapping setCustomFormCode(String customFormCode) {
        this.customFormCode = customFormCode;
        return this;
    }

    public Boolean isUseCodeMapping() {
        return isUseCodeMapping != null && isUseCodeMapping;
    }

    public RelationFieldMapping setUseCodeMapping(Boolean useCodeMapping) {
        isUseCodeMapping = useCodeMapping;
        return this;
    }
}