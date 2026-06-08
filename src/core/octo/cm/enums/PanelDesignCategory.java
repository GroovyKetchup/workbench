package octo.cm.enums;

import cmn.anotation.ClassDeclare;
import gpf.adur.data.AssociationData;
import octo.cm.util.PanelCategoryUtil;
import org.nutz.dao.entity.annotation.Comment;

@Comment("面板设计分类枚举")
@ClassDeclare(
        label = "",
        what = "面板设计的分类类型枚举，对应面板分类表中的分类名称",
        why = "统一面板分类的使用方式，避免硬编码分类名称字符串",
        how = "枚举包装分类名称，通过PanelCategoryUtil获取关联数据",
        developer = "裴硕", version = "1.0",
        createTime = "2026-04-21", updateTime = "2026-04-21"
)
public enum PanelDesignCategory {

    SYSTEM_MODULE(PanelCategoryUtil.CategoryType_SystemModule),
    PROCESS_HANDLE(PanelCategoryUtil.CategoryType_ProcessHandle),
    INFORMATION_MGR(PanelCategoryUtil.CategoryType_InformationMgr),
    DATA_BOARD(PanelCategoryUtil.CategoryType_DataBoard);

    private final String categoryName;

    PanelDesignCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public AssociationData getAssociationData() {
        return PanelCategoryUtil.getAssignCategoryAc(categoryName);
    }

    public static PanelDesignCategory getByCategoryName(String categoryName) {
        for (PanelDesignCategory value : values()) {
            if (value.categoryName.equals(categoryName)) {
                return value;
            }
        }
        return null;
    }

}
