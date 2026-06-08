package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板按钮实现")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-22", updateTime = "2025-08-22"
)
public class PanelButtonImplDto implements Serializable {
    @JSONField(name = "按钮名称")
    private String buttonName;
    @JSONField(name = "按钮说明")
    private String buttonDescription;
    @JSONField(name = "分类标签")
    private String categoryLabel;
    @JSONField(name = "按钮动作")
    private List<PanelButtonActionDto> buttonAction;

    public String getButtonName() {
        return buttonName;
    }

    public PanelButtonImplDto setButtonName(String buttonName) {
        this.buttonName = buttonName;
        return this;
    }

    public String getButtonDescription() {
        return buttonDescription;
    }

    public PanelButtonImplDto setButtonDescription(String buttonDescription) {
        this.buttonDescription = buttonDescription;
        return this;
    }

    public String getCategoryLabel() {
        return categoryLabel;
    }

    public PanelButtonImplDto setCategoryLabel(String categoryLabel) {
        this.categoryLabel = categoryLabel;
        return this;
    }

    public List<PanelButtonActionDto> getButtonAction() {
        return buttonAction;
    }

    public PanelButtonImplDto setButtonAction(List<PanelButtonActionDto> buttonAction) {
        this.buttonAction = buttonAction;
        return this;
    }
}
