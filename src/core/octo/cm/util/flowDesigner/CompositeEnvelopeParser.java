package octo.cm.util.flowDesigner;

import cn.hutool.json.JSONUtil;
import octo.cm.dto.flowDesigner.rule.CompositeEnvelopeDto;
import octo.cm.dto.flowDesigner.rule.CompositeOperationNodeDto;
import octo.cm.dto.flowDesigner.rule.CompositeRouteNodeDto;

import java.util.List;

public class CompositeEnvelopeParser {
    public static final int VERSION = 1;
    public static final String OP_SEQ = "seq";
    public static final String OP_CALL = "call";
    public static final String OP_AND = "and";
    public static final String OP_OR = "or";
    public static final String OP_NOT = "not";

    public static CompositeEnvelopeDto parse(String envelopeJson) {
        if (isBlank(envelopeJson)) throw new IllegalArgumentException("编排协议JSON不能为空");
        CompositeEnvelopeDto envelope;
        try {
            envelope = JSONUtil.toBean(envelopeJson, CompositeEnvelopeDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("编排协议JSON格式非法", e);
        }
        validate(envelope);
        return envelope;
    }

    public static void validate(CompositeEnvelopeDto envelope) {
        if (envelope == null) throw new IllegalArgumentException("编排协议不能为空");
        if (envelope.getMeta() == null) throw new IllegalArgumentException("编排协议缺少meta");
        if (envelope.getMeta().getVersion() == null) throw new IllegalArgumentException("编排协议缺少meta.version");
        if (envelope.getMeta().getVersion() != VERSION) {
            throw new IllegalArgumentException("不支持的编排协议版本：" + envelope.getMeta().getVersion());
        }
        if (envelope.getOps() == null && envelope.getRoute() == null) {
            throw new IllegalArgumentException("编排协议至少需要ops或route");
        }
        if (envelope.getOps() != null) validateOperationNode(envelope.getOps());
        if (envelope.getRoute() != null) validateRouteNode(envelope.getRoute());
    }

    private static void validateOperationNode(CompositeOperationNodeDto node) {
        if (node == null) throw new IllegalArgumentException("ops节点不能为空");
        String op = node.getOp();
        if (isBlank(op)) throw new IllegalArgumentException("ops节点缺少op");
        if (OP_SEQ.equals(op)) {
            validateChildrenNotEmpty(node.getChildren(), "seq节点缺少children");
            for (CompositeOperationNodeDto child : node.getChildren()) {
                validateOperationNode(child);
            }
            return;
        }
        if (OP_CALL.equals(op)) {
            if (isBlank(node.getExpr())) throw new IllegalArgumentException("ops.call节点缺少expr");
            FlowDesignerCallExprUtil.parseCall(node.getExpr());
            return;
        }
        throw new IllegalArgumentException("ops节点不支持op：" + op);
    }

    private static void validateRouteNode(CompositeRouteNodeDto node) {
        if (node == null) throw new IllegalArgumentException("route节点不能为空");
        String op = node.getOp();
        if (isBlank(op)) throw new IllegalArgumentException("route节点缺少op");
        if (OP_AND.equals(op) || OP_OR.equals(op)) {
            validateChildrenNotEmpty(node.getChildren(), op + "节点缺少children");
            for (CompositeRouteNodeDto child : node.getChildren()) {
                validateRouteNode(child);
            }
            return;
        }
        if (OP_NOT.equals(op)) {
            validateChildrenNotEmpty(node.getChildren(), "not节点缺少children");
            if (node.getChildren().size() != 1) throw new IllegalArgumentException("not节点只能有一个child");
            validateRouteNode(node.getChildren().get(0));
            return;
        }
        if (OP_CALL.equals(op)) {
            if (isBlank(node.getExpr())) throw new IllegalArgumentException("route.call节点缺少expr");
            FlowDesignerCallExprUtil.parseCall(node.getExpr());
            return;
        }
        throw new IllegalArgumentException("route节点不支持op：" + op);
    }

    private static void validateChildrenNotEmpty(List<?> children, String message) {
        if (children == null || children.isEmpty()) throw new IllegalArgumentException(message);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
