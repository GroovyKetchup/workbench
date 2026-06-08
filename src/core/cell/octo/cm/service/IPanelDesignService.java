package cell.octo.cm.service;

import bap.cells.Cells;
import cell.CellIntf;
import cell.cdao.IDao;
import cell.rapidView.function.BasicFunc;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.Form;
import octo.cm.dto.panelDesign.PanelCustomButtonDef;
import octo.cm.dto.panelDesign.PanelPublishOption;
import octo.cm.enums.DefaultSystemModule;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("面板设计服务接口")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-16", updateTime = "2025-06-16"
)
public interface IPanelDesignService extends CellIntf, BasicFunc {

    static IPanelDesignService get() {
        return Cells.get(IPanelDesignService.class);
    }

    // 创建面板设计（不依赖场景）
    Form createPanelDesign(OctoDomainOpObserver observer, Form panelDesignForm) throws Exception;

    // 创建面板设计，同时为面板挂载用户自定义按钮
    Form createPanelDesign(OctoDomainOpObserver observer, Form panelDesignForm, List<PanelCustomButtonDef> customButtons) throws Exception;

    // 加载默认面板设计
    void loadDefaultPanelDesign(OctoDomainOpObserver observer, DefaultSystemModule module) throws Exception;

    Form getPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode) throws Exception;

    Form getPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode, boolean needCompoundField) throws Exception;

    List<Form> queryPanelDesigns(IDao dao, OctoDomainOpObserver observer, boolean needCompoundField) throws Exception;

    long countExistedPanelDesign(OctoDomainOpObserver observer) throws Exception;

    // 场景层发布到面板设计
    Form publishSceneLayer(OctoDomainOpObserver observer, String sceneCode, boolean isOverwriteExisted) throws Exception;

    List<Form> publishSceneBatch(Progress<?> progress, OctoDomainOpObserver observer, List<String> sceneCodes, boolean isOverwriteExisted) throws Exception;

    List<Form> publishSceneToDefaultApplicationBatch(Progress<?> progress, OctoDomainOpObserver observer, List<String> sceneCodes, boolean isOverwriteExisted) throws Exception;

    void publishPanelDesignToDefaultApplicationBatch(Progress<?> progress,
                                                     OctoDomainOpObserver observer, List<Form> publishedPanelDesigns) throws Exception;


    // ========================= 面板设计-发布 =========================

    // 按面板编号发布面板设计（纯发布，不挂菜单）
    void publishPanelDesign(OctoDomainOpObserver observer, String panelCode) throws Exception;

    // 按面板编号发布面板设计，并按选项决定是否挂载到指定应用菜单
    void publishPanelDesign(OctoDomainOpObserver observer, String panelCode, PanelPublishOption option) throws Exception;


    // ========================= 面板设计-生效 =========================

    // 生效面板设计
    void takeEffectPanelDesign(PanelContext panelContext, String panelCode) throws Exception;

    // 场景-数据转换到需求实现层
    void convertSceneDataToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String dataCode, boolean isReplaceDirectly) throws Exception;

    // 场景-行为转换到需求实现层
    void convertSceneBehaviorToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String behaviorCode, boolean isReplaceDirectly) throws Exception;

    // 场景-约束转换到需求实现层
    void convertSceneConstraintToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String constraintCode, boolean isReplaceDirectly) throws Exception;

    // 场景-编排转换到需求实现层
    void convertSceneOrchestrationToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String orchestrationCode, boolean isReplaceDirectly) throws Exception;

    // 场景-展示转换到需求实现层
    void convertSceneDisplayToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String verificationCode) throws Exception;


}
