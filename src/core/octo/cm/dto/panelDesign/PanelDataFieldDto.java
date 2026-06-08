package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("面板数据-字段")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-21", updateTime = "2025-08-21"
)
public class PanelDataFieldDto implements Serializable {
    @JSONField(name = "属性名称")
    private String fieldName;
    @JSONField(name = "属性样式")
    private String fieldStyle;
    @JSONField(name = "来源场景数据编号")
    private String sourceSceneDataCode;
    @JSONField(name = "来源场景属性名称")
    private String sourceSceneDataFieldName;
    @JSONField(name = "来源场景属性样式")
    private String sourceSceneDataFieldStyle;

    public String getFieldName() {
        return fieldName;
    }

    public PanelDataFieldDto setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public String getFieldStyle() {
        return fieldStyle;
    }

    public PanelDataFieldDto setFieldStyle(String fieldStyle) {
        this.fieldStyle = fieldStyle;
        return this;
    }

    public String getSourceSceneDataCode() {
        return sourceSceneDataCode;
    }

    public PanelDataFieldDto setSourceSceneDataCode(String sourceSceneDataCode) {
        this.sourceSceneDataCode = sourceSceneDataCode;
        return this;
    }

    public String getSourceSceneDataFieldName() {
        return sourceSceneDataFieldName;
    }

    public PanelDataFieldDto setSourceSceneDataFieldName(String sourceSceneDataFieldName) {
        this.sourceSceneDataFieldName = sourceSceneDataFieldName;
        return this;
    }

    public String getSourceSceneDataFieldStyle() {
        return sourceSceneDataFieldStyle;
    }

    public PanelDataFieldDto setSourceSceneDataFieldStyle(String sourceSceneDataFieldStyle) {
        this.sourceSceneDataFieldStyle = sourceSceneDataFieldStyle;
        return this;
    }
}















