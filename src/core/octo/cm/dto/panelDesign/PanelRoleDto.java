package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板角色")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-20", updateTime = "2026-01-20"
)
public class PanelRoleDto implements Serializable {
    @JSONField(name = "编号")
    private String code;
    @JSONField(name = "角色名称")
    private String roleName;
    @JSONField(name = "角色描述")
    private String roleDescription;
    @JSONField(name = "组织匹配")
    private String orgMatchRule;
    @JSONField(name = "分类标签")
    private String category;

    public PanelRoleDto() {
    }

    public PanelRoleDto(String code, String roleName, String roleDescription, String orgMatchRule, String category) {
        this.code = code;
        this.roleName = roleName;
        this.roleDescription = roleDescription;
        this.orgMatchRule = orgMatchRule;
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public PanelRoleDto setCode(String code) {
        this.code = code;
        return this;
    }

    public String getRoleName() {
        return roleName;
    }

    public PanelRoleDto setRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public PanelRoleDto setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
        return this;
    }

    public String getOrgMatchRule() {
        return orgMatchRule;
    }

    public PanelRoleDto setOrgMatchRule(String orgMatchRule) {
        this.orgMatchRule = orgMatchRule;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public PanelRoleDto setCategory(String category) {
        this.category = category;
        return this;
    }
}

