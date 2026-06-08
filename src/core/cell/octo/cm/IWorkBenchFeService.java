package cell.octo.cm;


import bap.cells.Cells;
import cell.CellIntf;
import cell.fe.gpf.dc.basic.CommandCallbackIntf;
import cmn.anotation.ClassDeclare;
import fe.cmn.panel.PanelContext;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.util.intf.ServiceIntf;
import org.nutz.dao.entity.annotation.Comment;

@Comment("OctoCM工作台前端渲染服务")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public interface IWorkBenchFeService extends CellIntf, ServiceIntf, CommandCallbackIntf {
    static IWorkBenchFeService get() {
        return Cells.get(IWorkBenchFeService.class);
    }

    @Override
    default Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        return ServiceIntf.super.onListener(listener, panelContext, source);
    }


}
