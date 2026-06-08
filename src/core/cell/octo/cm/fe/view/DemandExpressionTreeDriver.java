package cell.octo.cm.fe.view;


import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.octo.cm.IContext;
import cn.hutool.core.util.StrUtil;
import fe.cmn.panel.PanelContext;
import fe.cmn.sys.QueryUrl;
import fe.cmn.widget.WidgetDto;
import fe.octo.cm.page.WorkBenchPage;
import gpf.adur.action.Action;
import gpf.dc.basic.fe.component.param.BaseViewParam;
import octo.cm.enums.FeContextSystemVarKey;
import octo.cm.enums.GpfContextSystemVarKey;
import octo.cm.fe.anotation.EventDefine;
import octo.cm.fe.dto.ViewModelDefineDto;
import octo.cm.fe.enums.BaseEventType;
import octo.cm.util.EasyOperation;
import org.nutz.dao.entity.annotation.Comment;

import static octo.cm.constant.WorkBenchConst.PanelCtxKey_UserAssignBusDomainCode;

@Comment("需求表达树驱动")

@EventDefine({
        BaseEventType.class,
})
public class DemandExpressionTreeDriver implements ViewDriver {

    public static final EasyOperation Op = EasyOperation.get();

    @Override
    public WidgetDto getWidget(IContext context, ViewModelDefineDto vm,
                               Action actionInst) throws Exception {
        PanelContext panelContext = (PanelContext) FeContextSystemVarKey.$feContext$.getContextValue(context);
        IDCRuntimeContext rtx = (IDCRuntimeContext) GpfContextSystemVarKey.$IDCRuntimeContext$.getContextValue(context);

        String userAssignDomainCode = getDomainCode(panelContext);
        if (StrUtil.isNotBlank(userAssignDomainCode)) {
            Op.putContextVal(panelContext, PanelCtxKey_UserAssignBusDomainCode, userAssignDomainCode);
        }
        WorkBenchPage<BaseViewParam> workBenchPage = new WorkBenchPage<>();
        BaseViewParam param = new BaseViewParam();
        workBenchPage.setWidgetParam(param);
        return workBenchPage.getWidget(panelContext);


    }


    // 从url中取domainCode
    public static String getDomainCode(PanelContext panelContext) throws Exception {
        String currentUrl = QueryUrl.query(panelContext.getChannel());
        String tempParamName = "busDomain=";
        if (StrUtil.isBlank(currentUrl) || !currentUrl.contains(tempParamName)) return null;
        return currentUrl.split(tempParamName)[1];

    }
}
