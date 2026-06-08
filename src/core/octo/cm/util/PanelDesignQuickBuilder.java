package octo.cm.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import octo.cm.dto.panelDesign.PanelCustomButtonDef;
import octo.cm.enums.PanelDesignCategory;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;

import static octo.cm.constant.WorkBenchConst.*;

/**
 * 面板设计快速构建工具类（纯内存 Form 组装）
 *
 * <p>只负责把用户输入拼装成 {@link Form}；真正的创建/持久化/属性实现的生成交由
 * {@code IPanelDesignService.createPanelDesign} 负责。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * Form panelForm = PanelDesignQuickBuilder
 *     .create("订单管理")
 *     .description("订单相关管理面板")
 *     .category(PanelDesignCategory.INFORMATION_MGR)
 *     // 字段，可指定属性样式
 *     .addField("订单编号")                          // 默认「文本」
 *     .addField("订单金额", "整数")
 *     .addField(new FieldDef("下单时间", "时间"))
 *     // 自定义按钮（可选，别名将被 TableDef.menu / FormDef.buttons 引用）
 *     .addButton(new PanelCustomButtonDef("订单管理_提交", "提交", "提交表单('订单编辑表单')"))
 *     // 面板表格
 *     .addTable(new TableDef("订单列表")
 *             .columns(Arrays.asList("订单编号", "订单金额"))
 *             .menu("刷新", "订单管理_新增", "删除")
 *             .rowOps("删除"))
 *     // 面板表单
 *     .addForm(new FormDef("订单编辑表单")
 *             .columns(Arrays.asList("订单编号", "订单金额"))
 *             .buttons("保存", "取消", "提交")
 *             .resetLayout(true))
 *     // 面板网页
 *     .addWebPage(new WebPageDef("订单看板", "<html>...</html>"))
 *     // 页面入口（通常为表格名）
 *     .pageEntry("订单列表")
 *     .build();
 *
 * Form created = IPanelDesignService.get().createPanelDesign(observer, panelForm);
 * }</pre>
 */
@Comment("面板设计快速构建工具类")
@ClassDeclare(
        label = "",
        what = "面板设计Form的内存组装工具",
        why = "简化面板设计Form的拼装；不接管持久化，仅负责构造Form",
        how = "收集面板基础信息、字段、表格、表单、网页、入口配置，在内存中拼装Form",
        developer = "裴硕", version = "1.0",
        createTime = "2026-04-21", updateTime = "2026-04-23"
)
public class PanelDesignQuickBuilder {

    // 属性样式常量
    public static final String ATTR_STYLE_TEXT = "文本";
    public static final String ATTR_STYLE_NUMBER = "整数";
    public static final String ATTR_STYLE_DATE = "时间";

    private String panelName;
    private String panelDescription = "";
    private PanelDesignCategory category = PanelDesignCategory.INFORMATION_MGR;

    private final List<FieldDef> fields = new ArrayList<>();
    private final List<TableDef> tables = new ArrayList<>();
    private final List<FormDef> forms = new ArrayList<>();
    private final List<WebPageDef> webPages = new ArrayList<>();
    private final List<PanelCustomButtonDef> customButtons = new ArrayList<>();
    private String pageEntry;


    private PanelDesignQuickBuilder(String panelName) {
        this.panelName = panelName;
    }


    // ========================= 静态入口 =========================

    public static PanelDesignQuickBuilder create(String panelName) {
        return new PanelDesignQuickBuilder(panelName);
    }


    // ========================= 链式配置：基础信息 =========================

    public PanelDesignQuickBuilder description(String description) {
        this.panelDescription = description;
        return this;
    }

    public PanelDesignQuickBuilder category(PanelDesignCategory category) {
        this.category = category;
        return this;
    }

    public PanelDesignQuickBuilder pageEntry(String pageEntry) {
        this.pageEntry = pageEntry;
        return this;
    }


    // ========================= 链式配置：字段 =========================

    public PanelDesignQuickBuilder addField(String fieldName) {
        return addField(new FieldDef(fieldName, ATTR_STYLE_TEXT));
    }

    public PanelDesignQuickBuilder addField(String fieldName, String attrStyle) {
        return addField(new FieldDef(fieldName, attrStyle));
    }

    public PanelDesignQuickBuilder addField(FieldDef field) {
        if (field != null && StrUtil.isNotBlank(field.fieldName)) {
            this.fields.add(field);
        }
        return this;
    }

    public PanelDesignQuickBuilder addFields(String... fieldNames) {
        if (fieldNames != null) {
            for (String fieldName : fieldNames) {
                addField(fieldName);
            }
        }
        return this;
    }

    public PanelDesignQuickBuilder addFields(Collection<FieldDef> fieldDefs) {
        if (fieldDefs != null) {
            fieldDefs.forEach(this::addField);
        }
        return this;
    }


    // ========================= 链式配置：表格 / 表单 / 网页 =========================

    public PanelDesignQuickBuilder addTable(TableDef table) {
        if (table != null && StrUtil.isNotBlank(table.tableName)) this.tables.add(table);
        return this;
    }

    public PanelDesignQuickBuilder addTable(String tableName) {
        return addTable(new TableDef(tableName));
    }

    public PanelDesignQuickBuilder addForm(FormDef form) {
        if (form != null && StrUtil.isNotBlank(form.formName)) this.forms.add(form);
        return this;
    }

    public PanelDesignQuickBuilder addForm(String formName) {
        return addForm(new FormDef(formName));
    }

    public PanelDesignQuickBuilder addWebPage(WebPageDef webPage) {
        if (webPage != null && StrUtil.isNotBlank(webPage.pageName)) this.webPages.add(webPage);
        return this;
    }

    public PanelDesignQuickBuilder addWebPage(String pageName, String pageContent) {
        return addWebPage(new WebPageDef(pageName, pageContent));
    }


    // ========================= 链式配置：自定义按钮 =========================

    public PanelDesignQuickBuilder addButton(PanelCustomButtonDef button) {
        if (button != null && StrUtil.isNotBlank(button.getButtonName())) {
            this.customButtons.add(button);
        }
        return this;
    }

    public PanelDesignQuickBuilder addButton(String buttonName, String alias, String operateFunction) {
        return addButton(new PanelCustomButtonDef(buttonName, alias, operateFunction));
    }

    public List<PanelCustomButtonDef> getCustomButtons() {
        return customButtons;
    }


    // ========================= 构建 =========================

    /**
     * 在内存中拼装 PanelDesign Form，<b>不</b>持久化。
     */
    public Form build() throws Exception {
        Form panelDesignForm = new Form(FormModelId_PanelDesign);
        panelDesignForm.setUuid(IdUtil.fastUUID())
                .setAttrValue(Form.Code, panelDesignForm.getUuid());

        panelDesignForm.setAttrValue("面板名称", panelName);
        panelDesignForm.setAttrValue("面板描述", StrUtil.blankToDefault(panelDescription, ""));
        if (category != null) {
            panelDesignForm.setAttrValue("面板分类", category.getAssociationData());
        }

        if (CollUtil.isNotEmpty(fields)) {
            panelDesignForm.setAttrValue("面板数据", buildFieldTd());
        }

        if (CollUtil.isNotEmpty(tables)) {
            panelDesignForm.setAttrValue("面板表格", buildTableTd());
        }

        if (CollUtil.isNotEmpty(forms)) {
            panelDesignForm.setAttrValue("面板表单", buildFormTd());
        }

        if (CollUtil.isNotEmpty(webPages)) {
            panelDesignForm.setAttrValue("面板网页", buildWebPageTd());
        }

        if (StrUtil.isNotBlank(pageEntry)) {
            panelDesignForm.setAttrValue("页面入口", pageEntry);
        }

        return panelDesignForm;
    }


    // ========================= 内部拼装 =========================

    private TableData buildFieldTd() throws Exception {
        TableData td = new TableData(SlaveFormModelId_PanelDesign_Data);
        Set<String> added = new HashSet<>();
        for (FieldDef f : fields) {
            String standardName = PanelDesignCommonFormUtil.standardAttrName(f.fieldName);
            if (StrUtil.isBlank(standardName) || added.contains(standardName)) continue;
            added.add(standardName);

            Form row = new Form(td.getFormModelId());
            row.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, row.getUuid());
            row.setAttrValue("场景属性名称", standardName);
            row.setAttrValue("场景属性样式", StrUtil.blankToDefault(f.attrStyle, ATTR_STYLE_TEXT));
            td.add(row);
        }
        return td;
    }

    private TableData buildTableTd()  throws Exception{
        TableData td = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Table);
        for (TableDef t : tables) {
            if (CollUtil.isEmpty(t.columns)) throw new RuntimeException("表格[" + t.tableName + "]必须显式声明 columns");
            Form row = new Form(td.getFormModelId());
            row.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, row.getUuid());
            row.setAttrValue("表格名称", t.tableName);
            row.setAttrValue("列名", CollUtil.join(t.columns, ","));
            if (CollUtil.isNotEmpty(t.menuButtons)) {
                row.setAttrValue("菜单", CollUtil.join(t.menuButtons, ","));
            }
            if (CollUtil.isNotEmpty(t.rowButtons)) {
                row.setAttrValue("操作列", CollUtil.join(t.rowButtons, ","));
            }
            td.add(row);
        }
        return td;
    }

    private TableData buildFormTd()  throws Exception{
        TableData td = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Form);
        for (FormDef f : forms) {
            if (CollUtil.isEmpty(f.columns)) throw new RuntimeException("表单[" + f.formName + "]必须显式声明 columns");
            Form row = new Form(td.getFormModelId());
            row.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, row.getUuid());
            row.setAttrValue("表单名称", f.formName);
            row.setAttrValue("属性", CollUtil.join(f.columns, ","));
            row.setAttrValue("重置布局", f.resetLayout);
            if (CollUtil.isNotEmpty(f.buttons)) {
                row.setAttrValue("按钮", CollUtil.join(f.buttons, ","));
            }
            td.add(row);
        }
        return td;
    }

    private TableData buildWebPageTd()  throws Exception{
        TableData td = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
        for (WebPageDef w : webPages) {
            Form row = new Form(td.getFormModelId());
            row.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, row.getUuid());
            row.setAttrValue("页面名称", w.pageName);
            row.setAttrValue("页面代码", StrUtil.blankToDefault(w.pageContent, ""));
            td.add(row);
        }
        return td;
    }

    // ========================= 配置对象 =========================

    // 字段定义
    public static class FieldDef {
        public final String fieldName;
        public final String attrStyle;

        public FieldDef(String fieldName) {
            this(fieldName, ATTR_STYLE_TEXT);
        }

        public FieldDef(String fieldName, String attrStyle) {
            this.fieldName = fieldName;
            this.attrStyle = attrStyle;
        }
    }

    // 面板表格定义
    public static class TableDef {
        public final String tableName;
        public List<String> columns = new ArrayList<>();
        public List<String> menuButtons = new ArrayList<>();
        public List<String> rowButtons = new ArrayList<>();

        public TableDef(String tableName) {
            this.tableName = tableName;
        }

        public TableDef columns(List<String> columns) {
            if (columns != null) this.columns = new ArrayList<>(columns);
            return this;
        }

        public TableDef menu(String... buttonAliases) {
            if (buttonAliases != null) Collections.addAll(this.menuButtons, buttonAliases);
            return this;
        }

        public TableDef rowOps(String... buttonAliases) {
            if (buttonAliases != null) Collections.addAll(this.rowButtons, buttonAliases);
            return this;
        }
    }

    // 面板表单定义
    public static class FormDef {
        public final String formName;
        public List<String> columns = new ArrayList<>();
        public List<String> buttons = new ArrayList<>();
        public boolean resetLayout = true;

        public FormDef(String formName) {
            this.formName = formName;
        }

        public FormDef columns(List<String> columns) {
            if (columns != null) this.columns = new ArrayList<>(columns);
            return this;
        }

        public FormDef buttons(String... buttonNames) {
            if (buttonNames != null) Collections.addAll(this.buttons, buttonNames);
            return this;
        }

        public FormDef resetLayout(boolean resetLayout) {
            this.resetLayout = resetLayout;
            return this;
        }
    }

    // 面板网页定义
    public static class WebPageDef {
        public final String pageName;
        public final String pageContent;             // HTML/JSON 内容

        public WebPageDef(String pageName, String pageContent) {
            this.pageName = pageName;
            this.pageContent = pageContent;
        }
    }

}
