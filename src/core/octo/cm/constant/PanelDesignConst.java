package octo.cm.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

/**
 * 面板设计器【中文字段名】常量集中地。
 *
 * <p>面板设计 Form（{@link WorkBenchConst#FormModelId_PanelDesign}）及其子表、关联事件轴模型用到的
 * 中文字段名统一放这里，避免魔法字符串散落在各发布器/操作函数里。与 {@link ReportDesignConst} 分工：
 * 报表专属的事件名/按钮名/报表定义字段留在 {@link ReportDesignConst}，面板自身字段（编号/名称/描述、
 * 面板事件/事件实现等）归本类。</p>
 *
 * <p><b>注意：</b>这里维护的是【中文字段名】（按 name 访问，配合
 * {@code Form.setAttrValue/getTable/getAssociation}）；模型层的拼音字段 code 由 md 模型类
 * （如 {@code octocm.md.workbench.PanelDesignerFM}、{@code octocm.md.workbench.PanelDesignerEvent}）维护，
 * 两套 key 不可混用。</p>
 *
 * @author Devin
 * @version 1.0
 */
@Comment("面板设计器字段常量")
@ClassDeclare(
        label = "面板设计器字段常量",
        what = "集中面板设计 Form 及其子表/事件用到的中文字段名",
        why = "避免魔法字符串散落；与 ReportDesignConst 按面板/报表职责分家",
        how = "全部 public static final 中文字段名常量，按 name 访问",
        developer = "Devin", version = "1.0",
        createTime = "2026-07-01", updateTime = "2026-07-01"
)
public final class PanelDesignConst {

    private PanelDesignConst() {
    }

    // ========================= 面板主表字段 =========================

    /** 面板设计 Form 的“面板编号”字段名。 */
    public static final String FieldName_PanelCode = "面板编号";

    /** 面板设计 Form 的“面板名称”字段名。 */
    public static final String FieldName_PanelName = "面板名称";

    /** 面板设计 Form 的“面板描述”字段名。 */
    public static final String FieldName_PanelDesc = "面板描述";

    // ========================= 面板事件（子表 + 事件轴模型） =========================

    /** 面板设计 Form 的“面板事件”子表名。 */
    public static final String FieldName_PanelEvent = "面板事件";

    /** “面板事件”子表行里“事件实现”关联字段名（关联具体事件定义）。 */
    public static final String FieldName_EventImpl = "事件实现";

    /** 事件轴模型（{@link WorkBenchConst#FormModelId_Axis_Event}）的“事件名称”字段名。 */
    public static final String FieldName_EventName = "事件名称";
}
