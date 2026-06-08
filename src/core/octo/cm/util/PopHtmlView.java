package octo.cm.util;

import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.leavay.common.util.javac.ClassFactory;
import fe.cmn.embedPage.InitialSourceType;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.PanelDto;
import fe.cmn.panel.SinglePanelDto;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.widget.EmbedPageDto;
import fe.cmn.widget.WidgetDto;
import fe.rapidView.util.EmbedPageUtil;
import octo.cm.dto.ErrorDto;
import org.nutz.dao.entity.annotation.Comment;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Comment("弹出Html页面")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-02", updateTime = "2025-09-02"
)
public class PopHtmlView {

    public static final EasyOperation Op = EasyOperation.get();

    // 资源文件路径：错误展示Html页面
    public static final String FilePath_HtmlView_ErrorDisplay = "view/ErrorDisplay.html";

    // 默认的JSON数据占位符
    public static final String DefaultJsonPlaceholder = "jsondata";


    // 当存在错误的时候，询问用户是否需要查看错误列表
    public static void popErrorViewWhenHasErrorWithUserConfirm(PanelContext panelContext) throws Exception {
        List<ErrorDto> errors = PanelDesignPublishErrorContext.getErrorsAndClear();
        if(CollUtil.isEmpty(errors)) return;
        if (Op.showYesOrNoDialog(panelContext, "提示", "有部分任务执行失败，是否查看错误日志？")) {
            PopHtmlView.popErrorListView(panelContext, errors);
        }
    }

    // 弹出错误页面
    public static void popErrorListView(PanelContext panelContext, List<ErrorDto> errors) throws Exception {
        String htmlCode = getResourceHtmlContent(FilePath_HtmlView_ErrorDisplay);
        Map<String, String> dataMap = new HashMap<>();
        String errorsStr = JSONUtil.toJsonStr(errors);
        dataMap.put(DefaultJsonPlaceholder, errorsStr);
        htmlCode = EmbedPageUtil.fillInParamsToTemplate(htmlCode, dataMap);

        PanelDto panelDto = newEmbedViewPanel(panelContext, htmlCode);
        PopDialog.show(panelContext, "", panelDto);
    }


    private static PanelDto newEmbedViewPanel(PanelContext panelContext, String htmlCode) throws Exception {
        WidgetDto widgetDto = new EmbedPageDto()
                .setOnMessage(null)
                .setContent(htmlCode)
                .setInitialSourceType(InitialSourceType.html)
                .setWidgetId("WidgetIdEmbedPage_" + IdUtil.fastUUID())
                .setExpandInBox(true);
        SinglePanelDto panelDto = SinglePanelDto.wrap(widgetDto).setExpandInBox(true);
        panelDto.setPadding(0d).setMargin(0d);
        panelDto.setPreferWidthByWindowSize(0.6)
                .setPreferHeightByWindowSize(0.8);
        return panelDto;
    }

    // 指定资源路径获取内容
    private static String getResourceHtmlContent(String resourcePath) {
        try {
            if (resourcePath == null) return null;
            URL viewFileUrl = ClassFactory.getResourceURL(resourcePath);
            if (viewFileUrl == null) return null;
            return FileUtil.readString(viewFileUrl, Charset.defaultCharset());
        } catch (Exception e) {
            Op.logException(e);
            return null;
        }

    }
}
