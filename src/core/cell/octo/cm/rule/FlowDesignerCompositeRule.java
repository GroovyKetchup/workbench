package cell.octo.cm.rule;

import cell.CellIntf;
import cell.gpf.dc.basic.IExpressionMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.octo.cm.IContext;
import cell.octo.cm.impl.CContext;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import gpf.dc.basic.param.view.BaseFeActionParameter;
import octo.cm.dto.flowDesigner.rule.CallExprDto;
import octo.cm.dto.flowDesigner.rule.CompositeEnvelopeDto;
import octo.cm.dto.flowDesigner.rule.CompositeOperationNodeDto;
import octo.cm.dto.flowDesigner.rule.CompositeRouteNodeDto;
import octo.cm.enums.ContextSystemVarKey;
import octo.cm.enums.GpfContextSystemVarKey;
import octo.cm.enums.WfeContextSystemVarKey;
import octo.cm.util.UserRoleUtil;
import octo.cm.util.flowDesigner.CompositeEnvelopeParser;
import octo.cm.util.flowDesigner.FlowDesignerCallExprUtil;
import octocm.design.consts.Octomica2DesignConst;
import org.nutz.dao.entity.annotation.Comment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Comment("流程设计器编排规则函数")
@ClassDeclare(
        label = "流程设计器编排规则函数",
        what = "解析流程设计器生成的编排协议并执行",
        why = "统一承载流程设计器中操作序列和路由规则的组合表达",
        how = "在规则配置中使用 编排(envelopeJson)，后端按meta.version解析ops和route",
        developer = "裴硕", version = "1.0",
        createTime = "2026-05-13", updateTime = "2026-05-13"
)
public interface FlowDesignerCompositeRule extends CellIntf {

    @MethodDeclare(
            label = "编排",
            what = "执行流程设计器编排协议",
            why = "将副作用操作ops和布尔路由route放在一个规则函数中统一处理",
            how = "先顺序执行ops，再计算route；没有route时默认返回true",
            inputs = {
                    @InputDeclare(desc = "业务域", name = "domain", label = "业务域", exampleValue = "$domain$"),
                    @InputDeclare(desc = "GPF运行上下文", name = "rtx", label = "GPF运行上下文", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(desc = "表单数据", name = "form", label = "表单数据", exampleValue = "$form$"),
                    @InputDeclare(desc = "编排协议JSON", name = "envelopeJson", label = "编排协议JSON")
            }
    )
    default Boolean composite(String domain, IDCRuntimeContext rtx, Form form, String envelopeJson) throws Exception {

        CompositeEnvelopeDto envelope = CompositeEnvelopeParser.parse(envelopeJson);
        if (envelope.getOps() != null) executeOperationNode(domain, rtx, form, envelope.getOps());
        if (envelope.getRoute() == null) return true;
        return executeRouteNode(domain, rtx, form, envelope.getRoute());
    }


    // ========================= 支撑方法 =========================


    default void executeOperationNode(String domain, IDCRuntimeContext rtx, Form form, CompositeOperationNodeDto node) throws Exception {
        String op = node.getOp();
        if (CompositeEnvelopeParser.OP_SEQ.equals(op)) {
            for (CompositeOperationNodeDto child : node.getChildren()) {
                executeOperationNode(domain, rtx, form, child);
            }
            return;
        }
        if (CompositeEnvelopeParser.OP_CALL.equals(op)) {
            executeFunction(domain, rtx, form, FlowDesignerCallExprUtil.parseCall(node.getExpr()), node.getExpr());
            return;
        }
        throw new IllegalArgumentException("ops节点不支持op：" + op);
    }

    default Boolean executeRouteNode(String domain, IDCRuntimeContext rtx, Form form, CompositeRouteNodeDto node) throws Exception {
        String op = node.getOp();
        if (CompositeEnvelopeParser.OP_AND.equals(op)) {
            for (CompositeRouteNodeDto child : node.getChildren()) {
                if (!executeRouteNode(domain, rtx, form, child)) return false;
            }
            return true;
        }
        if (CompositeEnvelopeParser.OP_OR.equals(op)) {
            for (CompositeRouteNodeDto child : node.getChildren()) {
                if (executeRouteNode(domain, rtx, form, child)) return true;
            }
            return false;
        }
        if (CompositeEnvelopeParser.OP_NOT.equals(op)) {
            return !executeRouteNode(domain, rtx, form, node.getChildren().get(0));
        }
        if (CompositeEnvelopeParser.OP_CALL.equals(op)) {
            Object result = executeFunction(domain, rtx, form, FlowDesignerCallExprUtil.parseCall(node.getExpr()), node.getExpr());
            return toBoolean(result);
        }
        throw new IllegalArgumentException("route节点不支持op：" + op);
    }

    default Object executeFunction(String domain, IDCRuntimeContext rtx, Form form, CallExprDto call, String expr) throws Exception {

        System.out.println(StrUtil.format("call:\n{}\n", JSONUtil.toJsonStr(call)));
        System.out.println(StrUtil.format("expr:\n{}\n", expr));

        IContext context = new CContext();
        ContextSystemVarKey.$form$.setContextValue(context, form);
        BaseFeActionParameter input = new BaseFeActionParameter();
        input.setRtx(rtx);
        GpfContextSystemVarKey.$IDCRuntimeContext$.setContextValue(context, rtx);
        GpfContextSystemVarKey.$ActionParameter$.setContextValue(context, input);
        WfeContextSystemVarKey.$behavior$.setContextValue(context, input.getRtx().getActionName());

        Map<String, Object> realEventParam = new HashMap<>(context.getAllParams());
        realEventParam.put(ContextSystemVarKey.$context$.name(), context);

        Set<String> nameSpaces = CollUtil.newHashSet(domain);
        nameSpaces.add(Octomica2DesignConst.DOMAIN_SYSTEM);


        Object result = IExpressionMgr.get().execute(nameSpaces, realEventParam, expr);

        System.out.println(StrUtil.format("result:{}", JSONUtil.toJsonStr(result)));
        return result;


    }

    default Boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0D;
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text)) return true;
        if ("false".equalsIgnoreCase(text)) return false;
        throw new IllegalArgumentException("规则函数返回值不能转换为boolean：" + text);
    }
}
