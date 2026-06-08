package fe.octo.cm.page;

import cell.octo.cm.IWorkBenchFeService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.ArrayUtil;
import fe.cmn.event.EventDto;
import fe.cmn.event.EventInterface;
import fe.cmn.event.EventSubscriberDto;
import fe.cmn.panel.*;
import fe.cmn.panel.ability.RebuildChild;
import fe.cmn.widget.*;
import fe.cmn.widget.decoration.*;
import fe.octo.cm.component.catalog.WorkBenchCatalog;
import fe.util.component.AbsComponent;
import fe.util.component.dto.FeCmnEvent;
import fe.util.component.param.TreeParam;
import fe.util.intf.ServiceIntf;
import gpf.dc.basic.fe.component.param.BaseViewParam;
import org.nutz.dao.entity.annotation.Comment;

import java.awt.*;

@Comment("OctoCM工作台")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchPage<T extends BaseViewParam> extends AbsComponent<T> implements EventInterface, ListenerInterface {

    public static final String WIDGET_ID_WORK_BENCH_SPLIT_DTO = "WIDGET_ID_WORK_BENCH_SPLIT_DTO";
    public static final String WIDGET_ID_WORK_BENCH_CONTENT_VIEW = "WIDGET_ID_WORK_BENCH_CONTENT_VIEW";
    public static final String CMD_REBUILD_GLOBAL_VIEW = "CMD_REBUILD_GLOBAL_VIEW";
    public static final String CMD_REBUILD_CONTENT_VIEW = "CMD_REBUILD_CONTENT_VIEW";

    @Override
    public WidgetDto getWidget(PanelContext panelContext) throws Exception {
        // 默认的树
        // 可能会使用自定义的树
        return getWidget(panelContext, buildCatalogView(panelContext));

    }

    public WidgetDto getWidget(PanelContext panelContext, WidgetDto catalogView) throws Exception {
        SplitViewDto workBenchSplitDto = new SplitViewDto()
                .setWidgetId(WIDGET_ID_WORK_BENCH_SPLIT_DTO)
                .setDividRatio(0.4)
                .setVertical(false)
                .setSplitViewChildShowType(SplitViewChildShowType.both);

        workBenchSplitDto.setLeft(catalogView);
        workBenchSplitDto.setRight(
                buildContentView(panelContext, null)
        );

        workBenchSplitDto
                .addSubscribeEvent(new EventSubscriberDto(FeCmnEvent.class).setCommand(CMD_REBUILD_CONTENT_VIEW).setService(getService()))
                .addSubscribeEvent(new EventSubscriberDto(FeCmnEvent.class).setCommand(CMD_REBUILD_GLOBAL_VIEW).setService(getService()));

        workBenchSplitDto.setMargin(0d).setDecoration(
                new DecorationDto().setBorderRadius(BorderRadiusDto.all(RadiusDto.circular(10d)))
        );

        SinglePanelDto panelDto = SinglePanelDto.wrap(workBenchSplitDto);
        panelDto.setExpandInBox(true);
//        panelDto.setPreferHeightByWindowSize(0.8).setPreferWidthByWindowSize(0.8);
        return panelDto;
    }


    @Override
    public void onEvent(EventDto event, PanelContext panelContext, WidgetDto source) throws Exception {
//        PopToast.info(panelContext.getChannel(), event.getCommand());
        if (CMD_REBUILD_CONTENT_VIEW.equals(event.getCommand())) {
            FeCmnEvent feEvent = (FeCmnEvent) event;
            WidgetDto contentView = null;
            Object[] params = feEvent.getInvokeParams();
            if (ArrayUtil.isNotEmpty(params) && (params[0] instanceof WidgetDto)) {
                contentView = (WidgetDto) params[0];
            }

            WidgetDto newContentView = buildContentView(panelContext, contentView);
            RebuildChild.rebuild(panelContext, newContentView);
        } else if (CMD_REBUILD_GLOBAL_VIEW.equals(event.getCommand())) {
            try {
                WidgetDto newWorkBenchPage = getWidget(panelContext);
                RebuildChild.rebuild(panelContext, newWorkBenchPage);
            } catch (Exception ignored) {

            }
        }


    }


    private WidgetDto buildCatalogView(PanelContext panelContext) throws Exception {
        WorkBenchCatalog<TreeParam> workBenchCatalog = new WorkBenchCatalog<>();
        TreeParam treeParam = new TreeParam();
        workBenchCatalog.setWidgetParam(treeParam);
        return workBenchCatalog
                .getWidget(panelContext)
                .setPadding(new InsetDto(30d, 15d, 30d, 30d))
                ;


    }

    private WidgetDto buildContentView(PanelContext panelContext, WidgetDto content) {
        BoxDto mainBox = BoxDto.hbar();
        mainBox.setWidgetId(WIDGET_ID_WORK_BENCH_CONTENT_VIEW);
        mainBox.setDecoration(new DecorationDto().setBorder(BorderDto.all(new BorderSideDto().setWidth(1).setColor(new Color(227, 225, 225)))));
        if (content != null) {
            BoxDto wrapContentBox = BoxDto.hbar(content);
            wrapContentBox.setPadding(30d);
            mainBox.addChild(wrapContentBox);
        } else {
            LabelDto labelDto = new LabelDto("ℹ️ 单击左侧CM树进行预览");
            BoxDto noContentBox = BoxDto.hbar(labelDto);
            noContentBox.setCrossAxisAlignment(CrossAxisAlign.center).setMainAxisAlignment(MainAxisAlign.center);
            mainBox.addChild(
                    noContentBox
            );

        }

        return mainBox.setMargin(InsetDto.topBottom(20d));

    }


    @Override
    public Class<? extends ServiceIntf> getService() {
        return IWorkBenchFeService.class;
    }


    @Override
    public Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        return null;
    }
}
