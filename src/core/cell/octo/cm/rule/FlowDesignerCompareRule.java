package cell.octo.cm.rule;

import cell.CellIntf;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import octo.cm.util.flowDesigner.FlowDesignerCompareUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("流程设计器比较规则函数")
@ClassDeclare(
        label = "流程设计器比较规则函数",
        what = "比较当前表单字段与固定值或另一个字段",
        why = "支撑流程设计器控制节点和路由条件中的比较表达式",
        how = "在规则配置中使用 比较(字段, 运算符, 比较模式, 比较值)",
        developer = "裴硕", version = "1.0",
        createTime = "2026-05-13", updateTime = "2026-05-13"
)
public interface FlowDesignerCompareRule extends CellIntf {

    @MethodDeclare(
            label = "比较",
            what = "比较表单字段值",
            why = "根据前端预制比较函数生成的四个参数计算布尔结果",
            how = "字段从form读取；mode=value时比较固定值，mode=field时比较另一个form字段",
            inputs = {
                    @InputDeclare(desc = "GPF运行上下文", name = "rtx", label = "GPF运行上下文", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(desc = "表单数据", name = "form", label = "表单数据", exampleValue = "$form$"),
                    @InputDeclare(desc = "左值字段", name = "field", label = "左值字段"),
                    @InputDeclare(desc = "比较运算符", name = "operator", label = "比较运算符"),
                    @InputDeclare(desc = "比较模式", name = "mode", label = "比较模式"),
                    @InputDeclare(desc = "比较值", name = "target", label = "比较值")
            }
    )
    default Boolean compare(IDCRuntimeContext rtx, Form form, String field, String operator, String mode, String target) throws Exception {
        if (form == null) throw new IllegalArgumentException("比较函数缺少表单数据");
        if (StrUtil.isBlank(field)) throw new IllegalArgumentException("比较函数缺少左值字段");
        FlowDesignerCompareUtil.validateOperator(operator);
        String normalizedMode = FlowDesignerCompareUtil.normalizeMode(mode);
        Object leftValue = form.getAttrValue(field);
        Object rightValue;
        if (FlowDesignerCompareUtil.MODE_FIELD.equals(normalizedMode)) {
            if (StrUtil.isBlank(target)) throw new IllegalArgumentException("比较函数field模式缺少右值字段");
            rightValue = form.getAttrValue(target);
        } else {
            rightValue = target;
        }
        return FlowDesignerCompareUtil.compare(leftValue, operator, rightValue);
    }
}
