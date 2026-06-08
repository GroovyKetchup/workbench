package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板表格")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-22", updateTime = "2025-08-22"
)
public class PanelTableDto implements Serializable {
    @JSONField(name = "表格名称")
    private String tableName;
    @JSONField(name = "菜单")
    private List<String> menuNames;
    @JSONField(name = "列名")
    private List<String> columnNames;
    @JSONField(name = "搜索")
    private List<String> searchFields;
    @JSONField(name = "操作列")
    private List<String> actionColumns;

    public String getTableName() {
        return tableName;
    }

    public PanelTableDto setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public List<String> getMenuNames() {
        return menuNames;
    }

    public PanelTableDto setMenuNames(List<String> menuNames) {
        this.menuNames = menuNames;
        return this;
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public PanelTableDto setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
        return this;
    }

    public List<String> getSearchFields() {
        return searchFields;
    }

    public PanelTableDto setSearchFields(List<String> searchFields) {
        this.searchFields = searchFields;
        return this;
    }

    public List<String> getActionColumns() {
        return actionColumns;
    }

    public PanelTableDto setActionColumns(List<String> actionColumns) {
        this.actionColumns = actionColumns;
        return this;
    }
}

