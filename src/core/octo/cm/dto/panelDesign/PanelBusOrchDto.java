package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;

@Comment("面板业务编排")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-26", updateTime = "2025-08-26"
)
public class PanelBusOrchDto implements Serializable {
    @JSONField(name = "节点名称")
    private String nodeName;
    @JSONField(name = "流转动作")
    private String flowingAction;
    @JSONField(name = "下游节点")
    private String nextNode;
    @JSONField(name = "离开规则")
    private String leaveRule;

    public String getNodeName() {
        return nodeName;
    }

    public PanelBusOrchDto setNodeName(String nodeName) {
        this.nodeName = nodeName;
        return this;
    }

    public String getFlowingAction() {
        return flowingAction;
    }

    public PanelBusOrchDto setFlowingAction(String flowingAction) {
        this.flowingAction = flowingAction;
        return this;
    }

    public String getNextNode() {
        return nextNode;
    }

    public PanelBusOrchDto setNextNode(String nextNode) {
        this.nextNode = nextNode;
        return this;
    }

    public String getLeaveRule() {
        return leaveRule;
    }

    public PanelBusOrchDto setLeaveRule(String leaveRule) {
        this.leaveRule = leaveRule;
        return this;
    }
}

