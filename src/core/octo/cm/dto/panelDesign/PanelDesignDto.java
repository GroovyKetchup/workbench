package octo.cm.dto.panelDesign;

import cmn.anotation.ClassDeclare;
import com.alibaba.fastjson2.annotation.JSONField;
import org.nutz.dao.entity.annotation.Comment;

import java.io.Serializable;
import java.util.List;

@Comment("面板设计Dto")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-21", updateTime = "2025-08-21"
)
public class PanelDesignDto implements Serializable {

    @JSONField(name = "来源场景编号")
    private String sourceSceneCode;

    @JSONField(name = "面板数据")
    private List<PanelDataFieldDto> panelData;

    @JSONField(name = "面板按钮")
    private List<PanelButtonDto> panelButton;

    @JSONField(name = "面板表格")
    private PanelTableDto panelTable;

    @JSONField(name = "面板表单")
    private PanelFormDto panelForm;

    @JSONField(name = "业务编排")
    private List<PanelBusOrchDto> busOrch;

    public String getSourceSceneCode() {
        return sourceSceneCode;
    }

    public PanelDesignDto setSourceSceneCode(String sourceSceneCode) {
        this.sourceSceneCode = sourceSceneCode;
        return this;
    }

    public List<PanelDataFieldDto> getPanelData() {
        return panelData;
    }

    public PanelDesignDto setPanelData(List<PanelDataFieldDto> panelData) {
        this.panelData = panelData;
        return this;
    }

    public List<PanelButtonDto> getPanelButton() {
        return panelButton;
    }

    public PanelDesignDto setPanelButton(List<PanelButtonDto> panelButton) {
        this.panelButton = panelButton;
        return this;
    }

    public PanelTableDto getPanelTable() {
        return panelTable;
    }

    public PanelDesignDto setPanelTable(PanelTableDto panelTable) {
        this.panelTable = panelTable;
        return this;
    }

    public PanelFormDto getPanelForm() {
        return panelForm;
    }

    public PanelDesignDto setPanelForm(PanelFormDto panelForm) {
        this.panelForm = panelForm;
        return this;
    }

    public List<PanelBusOrchDto> getBusOrch() {
        return busOrch;
    }

    public PanelDesignDto setBusOrch(List<PanelBusOrchDto> busOrch) {
        this.busOrch = busOrch;
        return this;
    }
}
