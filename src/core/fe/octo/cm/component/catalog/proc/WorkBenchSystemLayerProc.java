package fe.octo.cm.component.catalog.proc;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IWorkBenchFeService;
import cell.octocm.domain.service.IDomainService;
import cell.rapidView.function.BasicFunc;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import fe.cmn.menu.MenuDto;
import fe.cmn.menu.MenuItemDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.ability.PopMenu;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.*;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.util.component.dto.TreeNodeExtraInfo;
import fe.util.component.param.WidgetParam;
import fe.util.intf.ServiceIntf;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.dc.basic.fe.component.app.AppCacheUtil;
import gpf.dc.basic.intf.AppDefaultFilterIntf;
import octo.cm.constant.WorkBenchConst;
import octo.cm.util.EasyOperation;
import octocm.domain.dto.DomainDto;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("工作台目录系统层操作类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchSystemLayerProc extends AbsOctoCmTreeProc {

    public static final EasyOperation Op = EasyOperation.get();

    public static final String CurrentProcessTargetType = WorkBenchConst.LAYER_NAME_SYSTEM;
    public static final String CurrentProcessNodeViewCode = "OctoCM_workbench_系统业务需求";


    @Override
    public boolean enableCreate(TreeNodeDto node) {
        return true;
    }

    @Override
    public boolean enableUpdate(TreeNodeDto node) {
        return false;
    }

    @Override
    public boolean enableDelete(TreeNodeDto node) {
        return true;
    }

    @Override
    public String getIcon() {
        return JDFICons.system_Icon;
    }

    @Override
    public List<TreeNodeDto> queryTree(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        try (IDao dao = IDaoService.newIDao()) {


            SqlExpression busDomainFilterExpression = null;

            //对业务域的过滤
            AppDefaultFilterIntf appDefaultFilter = AppCacheUtil.getAppDefaultFilter(context);
            if (appDefaultFilter != null) {
                Op.logf("查询系统层数据时，应用下没有配置默认筛选器");

                busDomainFilterExpression = appDefaultFilter.buildDefaultFilter(FormModelId_SystemLayer);
            } else {
                // 用户在缓存中指定业务域，那么就不用系统默认的了
                Object userAssignBusDomainCode = Op.getContextStringVal(context, PanelCtxKey_UserAssignBusDomainCode);
                if (userAssignBusDomainCode instanceof String) {
                    DomainDto userAssignDomain = IDomainService.get().getDomainByCode((String) userAssignBusDomainCode);
                    if (userAssignDomain != null) {
                        busDomainFilterExpression = Op.getBusDomainFilterExpr(userAssignDomain.getDomainUuid(), userAssignDomain.getDomainCode()
                                , WorkBenchConst.FormModelId_SystemLayer
                        );
                    }
                }
            }

            if (busDomainFilterExpression == null) {
                Op.logf("不允许无任何过滤的情况下访问意图确认树");

                return new ArrayList<>();
            }


            ResultSet<Form> queried = IFormMgr.get().queryFormPage(dao,
                    WorkBenchConst.FormModelId_SystemLayer,
                    Cnd.where(busDomainFilterExpression),
                    1, Integer.MAX_VALUE, false, false);

            if (queried.isEmpty()) return Collections.emptyList();
            List<TreeNodeDto> resultList = new ArrayList<>();
            for (Form form : queried.getDataList()) {
                resultList.add(
                        convert2FeTreeNodeDto(form)
                );
            }

            return resultList;

        }
    }

    @Override
    public List<TreeNodeDto> queryAndConvert2TreeNodeDto(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public TreeHighlightListener buildOnHighlightListener(TreeNodeDto node, WidgetParam widgetParam) {
        return null;
    }

    @Override
    public boolean isMatch(String nodeType) {
        return CurrentProcessTargetType.equals(nodeType);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }


    @Override
    public Class<? extends ServiceIntf> getServiceCell() {
        return IWorkBenchFeService.class;
    }

    @Override
    public TreeNodeDto convert2FeTreeNodeDto(Object node) throws Exception {
        Form form = (Form) node;
        String key = form.getString(Form.Code);
        String label = form.getString("系统名称");
        TreeNodeDto feNode = new TreeNodeDto(key, null, "[系统] " + label, getIcon(), isLeaf());
        feNode.setButtonBar(getButtonBar(feNode));
        feNode.setGestureDetector(buildGestureDetectorDto(feNode, null));
        TreeNodeExtraInfo<Form> extraInfo = new TreeNodeExtraInfo<>();
        extraInfo.setData(form).setNodeType(CurrentProcessTargetType).setInvokeClass(getTreePanel().getClass()).setRealDataUuid(form.getUuid());
        feNode.setBinaryData(extraInfo);

        return feNode;
    }

    @Override
    public TreeMenuDto getContextMenu(TreeNodeDto feNode, List<TreeNodeDto> otherSelected, TreeNodeQuerierContext context) throws Exception {
        TreeMenuDto contextMenu = super.getContextMenu(feNode, otherSelected, context);
        contextMenu.getMenuItems().addAll(
                CollUtil.newArrayList(
                        createMenuItem(feNode, JDFICons.design, Text_PublishToPanel, CMD_ON_PUBLISH_TO_PANEL),
                        createMenuItem(feNode, JDFICons.control, Text_PublishToApplication, CMD_ON_PUBLISH_TO_APPLICATION)
                )
        );
        return contextMenu;


    }

    @Override
    public void onClickCreateButton(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        // 这里要打开菜单，究竟是建立同级别的，还是低下的？

        List<MenuItemDto> menuItems = new ArrayList<>(CollUtil.newArrayList(
//                createMenuItem(feNode, "新增系统", FormModelId_SystemLayer),
                createMenuItem(feNode, "新增模块", FormModelId_SubGoalLayer)
        ));

        MenuDto menuDto = new MenuDto();
        menuDto.setMenuItems(menuItems);

        PopMenu.show(panelContext, menuDto);

    }

    @Override
    public void onCreateNode(ListenerDto listener, PanelContext panelContext, WidgetDto source, TreeNodeDto feNode, Consumer<Object> callback) throws Exception {
        popNodeFormView(panelContext, null, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);

    }


    @Override
    public WidgetDto buildNodeFormView(PanelContext panelContext, TreeNodeDto node, Form form, boolean isWriteable) throws Exception {
        return buildNodeFormView(panelContext, form, BasicFunc.PDCFormView, CurrentProcessNodeViewCode);
    }


}
