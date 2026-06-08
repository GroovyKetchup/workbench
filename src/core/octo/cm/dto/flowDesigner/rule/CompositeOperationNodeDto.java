package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;
import java.util.List;

public class CompositeOperationNodeDto implements Serializable {
    private String op;
    private String expr;
    private List<CompositeOperationNodeDto> children;

    public String getOp() {
        return op;
    }

    public CompositeOperationNodeDto setOp(String op) {
        this.op = op;
        return this;
    }

    public String getExpr() {
        return expr;
    }

    public CompositeOperationNodeDto setExpr(String expr) {
        this.expr = expr;
        return this;
    }

    public List<CompositeOperationNodeDto> getChildren() {
        return children;
    }

    public CompositeOperationNodeDto setChildren(List<CompositeOperationNodeDto> children) {
        this.children = children;
        return this;
    }
}
