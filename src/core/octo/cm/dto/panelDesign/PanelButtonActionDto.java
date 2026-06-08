package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("面板按钮动作")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-22", updateTime = "2025-08-22"
)
public class PanelButtonActionDto implements Serializable {
    @JSONField(name = "操作函数")
    private String operateFunction;
    @JSONField(name = "操作说明")
    private String operateDescription;
    @JSONField(name = "进入规则")
    private String entryRule;
    @JSONField(name = "跳过规则")
    private String skipRule;
    @JSONField(name = "离开规则")
    private String leaveRule;

    public String getOperateFunction() {
        return operateFunction;
    }

    public PanelButtonActionDto setOperateFunction(String operateFunction) {
        this.operateFunction = operateFunction;
        return this;
    }

    public String getOperateDescription() {
        return operateDescription;
    }

    public PanelButtonActionDto setOperateDescription(String operateDescription) {
        this.operateDescription = operateDescription;
        return this;
    }

    public String getEntryRule() {
        return entryRule;
    }

    public PanelButtonActionDto setEntryRule(String entryRule) {
        this.entryRule = entryRule;
        return this;
    }

    public String getSkipRule() {
        return skipRule;
    }

    public PanelButtonActionDto setSkipRule(String skipRule) {
        this.skipRule = skipRule;
        return this;
    }

    public String getLeaveRule() {
        return leaveRule;
    }

    public PanelButtonActionDto setLeaveRule(String leaveRule) {
        this.leaveRule = leaveRule;
        return this;
    }
}
