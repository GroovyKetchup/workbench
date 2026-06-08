package octo.cm.dto.flowDesigner.rule;

import java.io.Serializable;
import java.util.List;

public class CompositeRouteNodeDto implements Serializable {
    private String op;
    private String expr;
    private List<CompositeRouteNodeDto> children;

    public String getOp() {
        return op;
    }

    public CompositeRouteNodeDto setOp(String op) {
        this.op = op;
        return this;
    }

    public String getExpr() {
        return expr;
    }

    public CompositeRouteNodeDto setExpr(String expr) {
        this.expr = expr;
        return this;
    }

    public List<CompositeRouteNodeDto> getChildren() {
        return children;
    }

    public CompositeRouteNodeDto setChildren(List<CompositeRouteNodeDto> children) {
        this.children = children;
        return this;
    }
}
