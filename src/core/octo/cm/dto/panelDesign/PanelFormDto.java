package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板表单")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-22", updateTime = "2025-08-22"
)
public class PanelFormDto implements Serializable {
    @JSONField(name = "属性")
    private List<String> properties;
    @JSONField(name = "按钮")
    private List<String> buttons;

    public List<String> getProperties() {
        return properties;
    }

    public PanelFormDto setProperties(List<String> properties) {
        this.properties = properties;
        return this;
    }

    public List<String> getButtons() {
        return buttons;
    }

    public PanelFormDto setButtons(List<String> buttons) {
        this.buttons = buttons;
        return this;
    }
}

