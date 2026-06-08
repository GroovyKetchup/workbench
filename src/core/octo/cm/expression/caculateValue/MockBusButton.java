package octo.cm.expression.caculateValue;

import cell.gpf.dc.runtime.IDCRuntimeContext;
import com.googlecode.aviator.runtime.function.FunctionUtils;
import com.googlecode.aviator.runtime.type.AviatorObject;
import com.googlecode.aviator.runtime.type.AviatorRuntimeJavaType;
import com.kwaidoo.ms.tool.ToolUtilities;
import fe.cmn.panel.*;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.text.CTextStyle;
import fe.cmn.widget.EscapeButtonDto;
import fe.cmn.widget.LabelDto;
import fe.cmn.widget.SizeDto;
import fe.cmn.widget.decoration.LabelDecorationDto;
import fe.util.exception.VerifyException;
import gpf.dc.basic.expression.caculateValue.CaculateRuleIntf;
import gpf.dc.basic.param.view.ViewActionParameterIntf;

import java.util.Map;

public class MockBusButton extends CaculateRuleIntf {

    /**
     *
     */
    private static final long serialVersionUID = 6215314713949280762L;

    @Override
    public AviatorObject call(Map<String, Object> env, AviatorObject arg1) {
        try {
            IDCRuntimeContext runtimeContext = getRuntimeContext(env);
            PanelContext panelContext = (PanelContext) runtimeContext.getParam(ViewActionParameterIntf.FeActionParameter_PanelContext);
            String busInfo = FunctionUtils.getStringValue(arg1, env);
            busInfo = busInfo.replaceAll("【N】", "");
            showLogEditDialog(panelContext, "业务模拟", busInfo);

        } catch (VerifyException e) {
            throw new VerifyException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(ToolUtilities.getFullExceptionStack(e));
        }
        return AviatorRuntimeJavaType.valueOf(System.currentTimeMillis());
    }

    public boolean showLogEditDialog(PanelContext ctx, String title, String msg) throws Exception {
        long timeout = 10 * 60 * 1000;

        EscapeButtonDto ok = (EscapeButtonDto) new EscapeButtonDto().setText("确定").setWidgetId("_BUTTON_YES").setConfirmStyle();

        LabelDto contentWidget = new LabelDto(msg);
//        TextEditorDto contentWidget = new TextEditorDto(msg);
//        contentWidget.setWritable(true).setMinRenderLines(20);
//        contentWidget.setDecoration(new TextEditorDecorationDto()
        contentWidget.setDecoration(new LabelDecorationDto()
                .setTextStyle(new CTextStyle()
                        .setFontSize(16D)));
        BoxDto box = BoxDto.hbar(contentWidget);
        box.setMainAxisAlignment(MainAxisAlign.center)
                .setCrossAxisAlignment(CrossAxisAlign.center);
        SinglePanelDto panel = new SinglePanelDto(box);
        panel.setPreferSize(SizeDto.all(450, 400));

        PopDialog dlg = PopDialog.build(title, panel, ok, null, true).setDecoration(null).setBarrierDismissible(true);
        dlg.setWaitForClose(timeout);
        dlg.setTimeout(dlg.getWaitForClose());
        PanelValue panelValue = (PanelValue) ctx.callback(dlg);
        if (panelValue == null)
            return false;

        int clickOK = com.leavay.common.util.ToolUtilities.getInteger(panelValue.getValue("_BUTTON_YES"), -1);
        if (clickOK > 0)
            return true;

        return false;

    }
}
