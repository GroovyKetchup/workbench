package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.PanelDesignException;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

@Comment("面板分类工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-28", updateTime = "2025-08-28"
)
public class PanelCategoryUtil {

    public static final EasyOperation Op = EasyOperation.get();

    public static final String CategoryType_SystemModule = "系统模块";
    public static final String CategoryType_ProcessHandle = "流程处理";
    public static final String CategoryType_InformationMgr = "信息管理";
    public static final String CategoryType_DataBoard = "数据看板";


    // 获取系统模块面板分类
    public static AssociationData getSystemModuleCategoryAc() {
        return getAssignCategoryAc(CategoryType_SystemModule);
    }

    // 获取流程处理面板分类
    public static AssociationData getProcessCategoryAc() {
        return getAssignCategoryAc(CategoryType_ProcessHandle);
    }

    // 获取信息管理面板分类
    public static AssociationData getInformationMgrCategoryAc() {
        return getAssignCategoryAc(CategoryType_InformationMgr);
    }


    // 获取指定类型的面板分类
    public static AssociationData getAssignCategoryAc(String categoryName) {

        try (IDao dao = IDaoService.newIDao()) {
            Form form = Op.queryFormByCondition(dao, WorkBenchConst.FormModelId_PanelDesign_Category,
                    "分类名称", categoryName, null);
            if (form == null)
                throw new RuntimeException(StrUtil.format("无法找到面板分类[{}]建议联系开发人员", categoryName));
            return Op.toAssociationData(form);

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

    }

    // 该PanelDesign是【系统模块】
    public static boolean isSystemModuleCategory(Form panelDesignForm) throws Exception {
        String categoryName = getPanelCategory(panelDesignForm);
        if (StrUtil.isBlank(categoryName)) throw PanelDesignException.Builder.categoryEmpty();
        return isSystemModuleCategory(categoryName);

    }

    // 该PanelDesign是【流程处理】
    public static boolean isProcessHandleCategory(Form panelDesignForm) throws Exception {
        String categoryName = getPanelCategory(panelDesignForm);
        if (StrUtil.isBlank(categoryName)) throw PanelDesignException.Builder.categoryEmpty();
        return isProcessHandle(categoryName);

    }

    // 判断这个类型是不是【流程处理】
    public static boolean isProcessHandle(String categoryName) {
        return StrUtil.equalsAny(categoryName, CategoryType_ProcessHandle);
    }

    // 判断这个类型是不是【系统模块】
    public static boolean isSystemModuleCategory(String categoryName) {
        return StrUtil.equalsAny(categoryName, CategoryType_SystemModule);
    }

    // 判断这个类型是不是【信息管理】
    public static boolean isInformationMgrCategory(String categoryName) {
        return StrUtil.equalsAny(categoryName, CategoryType_InformationMgr);
    }

    // 判断这个类型是不是【数据看板】
    public static boolean isDashBoard(Form panelDesignForm) throws Exception {
        String categoryName = getPanelCategory(panelDesignForm);
        if (StrUtil.isBlank(categoryName)) throw PanelDesignException.Builder.categoryEmpty();
        return StrUtil.equalsAny(categoryName, "数据看板");

    }


    // ========================= 支撑方法 =========================


    // 获取场景分类（面板分类的来源）
    public static String getSceneCategory(IDao dao, OctoDomainOpObserver observer, String categoryCode) throws Exception {
        String formModelId = WorkBenchConst.FormModelId_SceneLayer;
        Cnd queryCnd = Op.getBusDomainFilterCondition(observer, formModelId);
        queryCnd.where().andEquals(Form.Code, categoryCode);
        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, queryCnd, 1, 1, false, false);
        if (queryRs.isEmpty()) return null;

        AssociationData sceneTypeAc = queryRs.getDataList().get(0).getAssociation("场景类型");
        if (sceneTypeAc == null) return null;
        Form sceneForm = Op.queryFormByAc(dao, sceneTypeAc);
        if (sceneForm == null) return null;
        return sceneForm.getString("分类名称");


    }

    // 获取面板分类
    public static String getPanelCategory(Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return null;
        // 目标场景的面板分类
        AssociationData panelCategoryAc = panelDesignForm.getAssociation("面板分类");
        if (panelCategoryAc == null) throw PanelDesignException.Builder.categoryEmpty();
        Form categoryForm = panelCategoryAc.getForm();
        if (categoryForm == null)
            throw PanelDesignException.Builder.categoryNotFoundWithCode(panelCategoryAc.getValue());
        return categoryForm.getString("分类名称");
    }


}
