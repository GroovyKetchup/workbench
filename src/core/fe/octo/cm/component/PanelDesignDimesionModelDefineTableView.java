package fe.octo.cm.component;

import cell.gpf.adur.data.IFormMgr;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import fe.cmn.app.ability.PopToast;
import fe.cmn.panel.PanelContext;
import fe.cmn.table.TableRowDto;
import fe.cmn.table.listener.TableRowListener;
import fe.cmn.widget.WidgetDto;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.FormModel;
import gpf.adur.data.ResultSet;
import gpf.dc.basic.fe.component.param.BaseTableViewParam;
import gpf.dc.basic.fe.component.view.FormTable;
import gpf.dc.basic.intf.FormActionIntf;
import gpf.dc.concrete.RefActionConfig;
import gpf.dc.fe.util.GpfEventUtil;
import jit.dto.DataModelDefineDto;
import jit.fe.component.JitModelDataManageView;
import org.nutz.dao.Cnd;
import octo.cm.exception.business.ViewException;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.util.List;
import java.util.Locale;

@Comment("面板设计-维表模型列表")
public class PanelDesignDimesionModelDefineTableView extends FormTable<BaseTableViewParam> {

    /**
     *
     */
    private static final long serialVersionUID = -6775794099321852810L;

    public static List<String> No_SHOW_WORK_SPEACES = CollUtil.newArrayList(
            "common"
    );

    @Override
    public <T extends Form> ResultSet<T> doQueryDataPage(PanelContext context, FormActionIntf formActionIntf,
                                                         String formModelID, Cnd cnd, int pageNo, Integer pageSize, SqlExpressionGroup defaultPrivExpr,
                                                         List<RefActionConfig> dataPrivilegeFuncs, List<RefActionConfig> customQueryFuncs) throws Exception {
        if (cnd == null)
            cnd = Cnd.NEW();
        cnd.where().andNotInStrList(IFormMgr.get().getFieldCode(DataModelDefineDto.sWorkspace), No_SHOW_WORK_SPEACES);
        cnd.and(new SqlExpressionGroup().andEquals(IFormMgr.get().getFieldCode(DataModelDefineDto.sIsDimensionTable), true));
        return super.doQueryDataPage(context, formActionIntf, formModelID, cnd, pageNo, pageSize, defaultPrivExpr,
                dataPrivilegeFuncs, customQueryFuncs);
    }

    @Override
    public void onRowClick(TableRowListener listener, PanelContext panelContext, WidgetDto source) throws Exception {
        TableRowDto row = listener.getRow();
        Form form = (Form) row.getBinaryData();

        AssociationData workSpeaceAc = form.getAssociation("工作空间");
        if (workSpeaceAc == null) throw ViewException.Builder.workspaceNotFound();
        String workSpaceCode = workSpeaceAc.getValue();
        ;
        String enName = form.getString("英文名称");
        if (StrUtil.hasBlank(workSpaceCode, enName)) throw ViewException.Builder.workspaceCodeOrEnNameEmpty();
        String formModelId = StrUtil.format("gpf.md.{}.{}", workSpaceCode.toLowerCase(Locale.ROOT), enName);

        FormModel formModel = IFormMgr.get().queryFormModel(formModelId);
        if (formModel == null) {
            PopToast.warning(panelContext.getChannel(), StrUtil.format("模型[{}]未生效！", formModelId));
            return;
        }
//        PopToast.info(panelContext.getChannel(), "123");
        GpfEventUtil.fireGlobalEvent(panelContext, JitModelDataManageView.CMD_OPEN_MODEL_TAB, JitModelDataManageView.getOpenTabIdentifyCode(widgetParam), formModel.getId(), formModel.getLabel());
    }
}
