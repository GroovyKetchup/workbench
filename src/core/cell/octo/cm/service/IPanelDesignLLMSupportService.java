package cell.octo.cm.service;

import bap.cells.Cells;
import cell.CellIntf;
import cell.rapidView.function.CommonFunctions;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import gpf.adur.data.Form;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("面板设计LLM支撑服务接口")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-30", updateTime = "2025-06-30"
)
public interface IPanelDesignLLMSupportService extends CellIntf, CommonFunctions {


    static IPanelDesignLLMSupportService get() {
        return Cells.get(IPanelDesignLLMSupportService.class);
    }


    // 大模型初始化面板设计
    Form llmInitPanelDesignAndPublish(Progress<?> progress, OctoDomainOpObserver observer, String panelCode, String userNeed) throws Exception;

    // 大模型初始化面板设计
    List<Form> llmInitPanelDesignAndPublishBatch(Progress<?> progress, OctoDomainOpObserver observer, List<String> panelCodes, String userNeed) throws Exception;


    // 大模型初始化面板设计
    Form llmInitPanelDesign(Progress<?> progress, OctoDomainOpObserver observer, Form panelDesignForm, String userNeed) throws Exception;

    // 大模型初始化面板设计
    List<Form> llmInitPanelDesignBatch(Progress<?> progress, OctoDomainOpObserver observer, List<Form> publishedPanelDesigns, String userNeed) throws Exception;

}
