package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("面板按钮")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-22", updateTime = "2025-08-22"
)
public class PanelButtonDto implements Serializable {

    @JSONField(name = "最终按钮名称")
    private String finalButtonName;
    @JSONField(name = "来源场景行为编号")
    private String sourceSceneBehaviorCode;
    @JSONField(name = "来源场景行为名称")
    private String sourceSceneBehaviorName;
    @JSONField(name = "复用按钮实现编号")
    private String reuseButtonImplCode;
    @JSONField(name = "新增按钮实现")
    private PanelButtonImplDto newCreateButtonImpl;

    public String getFinalButtonName() {
        return finalButtonName;
    }

    public PanelButtonDto setFinalButtonName(String finalButtonName) {
        this.finalButtonName = finalButtonName;
        return this;
    }

    public String getSourceSceneBehaviorCode() {
        return sourceSceneBehaviorCode;
    }

    public PanelButtonDto setSourceSceneBehaviorCode(String sourceSceneBehaviorCode) {
        this.sourceSceneBehaviorCode = sourceSceneBehaviorCode;
        return this;
    }

    public String getSourceSceneBehaviorName() {
        return sourceSceneBehaviorName;
    }

    public PanelButtonDto setSourceSceneBehaviorName(String sourceSceneBehaviorName) {
        this.sourceSceneBehaviorName = sourceSceneBehaviorName;
        return this;
    }

    public String getReuseButtonImplCode() {
        return reuseButtonImplCode;
    }

    public PanelButtonDto setReuseButtonImplCode(String reuseButtonImplCode) {
        this.reuseButtonImplCode = reuseButtonImplCode;
        return this;
    }

    public PanelButtonImplDto getNewCreateButtonImpl() {
        return newCreateButtonImpl;
    }

    public PanelButtonDto setNewCreateButtonImpl(PanelButtonImplDto newCreateButtonImpl) {
        this.newCreateButtonImpl = newCreateButtonImpl;
        return this;
    }
}
