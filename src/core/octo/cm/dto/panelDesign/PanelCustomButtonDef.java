package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("面板自定义按钮定义")
@ClassDeclare(
        label = "",
        what = "声明一个用户自定义按钮：别名、按钮名、操作函数、分类",
        why = "承载 QuickBuilder 阶段的按钮意图，交由 Service 层调用 createBtnImpl 创建按钮实现并挂载到面板",
        how = "在 QuickBuilder 里 addButton() 收集；在 createPanelDesign(..., List<PanelCustomButtonDef>) 中落库",
        developer = "裴硕", version = "1.0",
        createTime = "2026-04-23", updateTime = "2026-04-23"
)
public class PanelCustomButtonDef {

    // 按钮原始名称（必填，createBtnImpl 的 btnName）
    private String buttonName;

    // 按钮别名（工具栏上显示 & 被 TableDef.menu / FormDef.buttons 引用）
    private String alias;

    // 操作函数表达式，如 "新增表单('xxx')"
    private String operateFunction;

    // 按钮说明
    private String description = "";

    // 分类标签，默认业务按钮
    private String category = "业务按钮";


    public PanelCustomButtonDef() {
    }

    public PanelCustomButtonDef(String buttonName, String alias, String operateFunction) {
        this.buttonName = buttonName;
        this.alias = alias;
        this.operateFunction = operateFunction;
    }


    public String getButtonName() {
        return buttonName;
    }

    public PanelCustomButtonDef setButtonName(String buttonName) {
        this.buttonName = buttonName;
        return this;
    }

    public String getAlias() {
        return alias;
    }

    public PanelCustomButtonDef setAlias(String alias) {
        this.alias = alias;
        return this;
    }

    public String getOperateFunction() {
        return operateFunction;
    }

    public PanelCustomButtonDef setOperateFunction(String operateFunction) {
        this.operateFunction = operateFunction;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PanelCustomButtonDef setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public PanelCustomButtonDef setCategory(String category) {
        this.category = category;
        return this;
    }
}
