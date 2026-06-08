package fe.octo.cm.component.catalog;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.fe.progress.CFeProgressCtrlWithTextArea;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IWorkBenchFeService;
import cell.octo.cm.service.IPanelDesignService;
import cell.rapidView.function.RapidViewBasicFunc;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leavay.common.util.ProgressCtrl.crpc.IProgressUtil;
import com.leavay.ms.tool.CmnUtil;
import fe.cmn.app.ability.PopToast;
import fe.cmn.panel.BoxDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.ability.FireEvent;
import fe.cmn.panel.ability.QuitPopup;
import fe.cmn.panel.ability.RebuildChild;
import fe.cmn.progress.ProgressBarDecorationDto;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.*;
import fe.cmn.tree.listener.OnButtonBarClick;
import fe.cmn.tree.listener.TreeDropListener;
import fe.cmn.widget.ButtonDto;
import fe.cmn.widget.InsetDto;
import fe.cmn.widget.ListenerDto;
import fe.cmn.widget.WidgetDto;
import fe.octo.cm.component.catalog.proc.*;
import fe.octo.cm.page.WorkBenchPage;
import fe.util.LoadingMask;
import fe.util.component.AbsTreePanel;
import fe.util.component.ProgressDialog;
import fe.util.component.builder.ButtonBuilder;
import fe.util.component.dto.FeCmnEvent;
import fe.util.component.dto.FeDeliverData;
import fe.util.component.dto.TreeNodeExtraInfo;
import fe.util.component.param.ProgressDialogParam;
import fe.util.component.param.TreeParam;
import fe.util.component.tree.EmptyFeNodeProc;
import fe.util.component.tree.FeTreeNodeFactory;
import fe.util.component.tree.FeTreeNodeProcessor;
import fe.util.intf.ServiceIntf;
import gpf.adur.data.Form;
import gpf.dc.basic.fe.component.app.AppCacheUtil;
import gpf.dc.basic.fe.component.param.ConfirmCallbackParam;
import gpf.dc.intf.FormOpObserver;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.dto.ErrorDto;
import octo.cm.util.*;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fe.octo.cm.component.catalog.proc.AbsOctoCmTreeProc.CMD_ON_PUBLISH_TO_APPLICATION;
import static fe.octo.cm.component.catalog.proc.AbsOctoCmTreeProc.CMD_ON_PUBLISH_TO_PANEL;
import static fe.octo.cm.component.catalog.proc.WorkBenchSceneLayerProc.CMD_ON_CREATE_SCENE_IMPL;
import static fe.octo.cm.page.WorkBenchPage.CMD_REBUILD_CONTENT_VIEW;
import static fe.util.component.tree.AbsTreeFeNodeProc.CMD_CREATE_NODE;
import static fe.util.component.tree.AbsTreeFeNodeProc.CMD_DELETE_NODE;
import static octo.cm.constant.WorkBenchConst.*;

@Comment("OctoCM目录树")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchCatalog<T extends TreeParam> extends AbsTreePanel<T> implements ButtonBuilder {

    public static final String WIDGET_ID_WORK_BENCH_CATALOG_VIEW = "WIDGET_ID_WORK_BENCH_CATALOG_VIEW";

    public static final String CMD_CREATE_SYSTEM_LAYER_NODE = "CMD_CREATE_SYSTEM_LAYER_NODE";
    public static final String CMD_NODE_VIEW_CONFIRM = "CMD_NODE_VIEW_CONFIRM";
    public static final String CMD_NODE_VIEW_CANCEL = "CMD_NODE_VIEW_CANCEL";
    public static final String CMD_GLOBAL_REFRESH = "CMD_GLOBAL_REFRESH";
    public static final String WIDGET_ID_USE_LLM_INIT = "WIDGET_ID_USE_LLM_INIT";

    // 简单操作
    public static final EasyOperation Op = EasyOperation.get();


    @Override
    public FeTreeNodeFactory getFactory() {
        FeTreeNodeFactory factory = new FeTreeNodeFactory();
        FeTreeNodeProcessor[] nodeProcessors = {
                // 系统层处理类
                new WorkBenchSystemLayerProc(),
                // 模块层处理类
                new WorkBenchSubGoalLayerProc(),
                // 场景层处理类
                new WorkBenchSceneLayerProc(),
                // 场景实现层处理类
                new WorkBenchSceneImplLayerProc(),
                // 空节点处理类
                new EmptyFeNodeProc()
        };
        factory.setNodeProcessors(nodeProcessors);
        return factory;
    }

    @Override
    public Class<? extends ServiceIntf> getService() {
        return IWorkBenchFeService.class;
    }

    private AbsOctoCmTreeProc getTreeNodeProcessor(PanelContext panelContext, TreeNodeDto node) throws Exception {
        return (AbsOctoCmTreeProc) getFactory().getTreeNodeProcessor(panelContext, node, this, getBuilderService());
    }

    public AbsOctoCmTreeProc getTreeNodeProcessor(String nodeType) throws Exception {
        return (AbsOctoCmTreeProc) getFactory().getTreeNodeProcessor(null, nodeType, this, null);
    }

    public AbsOctoCmTreeProc getTreeNodeProcessorByFormModelId(String formModelId) throws Exception {
        if (StrUtil.isBlank(formModelId)) return null;

        switch (formModelId) {
            case FormModelId_SystemLayer:
                return getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SYSTEM);
            case FormModelId_SubGoalLayer:
                return getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SUBGOAL);
            case FormModelId_SceneLayer:
                return getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SCENE);
            case FormModelId_SceneLayer_Data:
            case FormModelId_SceneLayer_Behavior:
            case FormModelId_SceneLayer_Constraint:
            case FormModelId_SceneLayer_Orchestration:
            case FormModelId_SceneLayer_Verification:
                return getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SCENE_IMPL);
            default:
                return null;
        }

    }


    @Override
    public List<TreeNodeDto> queryChild(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {

        return doQueryChild(querier, context);


    }

    // 实际的执行查询的方法
    private List<TreeNodeDto> doQueryChild(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {

        // 本次要查询的父节点key
        String parentKey = querier.getParentKey();


        // 系统默认的根节点type
        String rootNodeType = widgetParam.getRootNodeType();
        // 系统默认的根节点key
        String rootNodeKey = widgetParam.getRootNodeKey();


        boolean isRoot = StrUtil.isBlank(parentKey);
        boolean isAssignRoot = StrUtil.isAllNotBlank(rootNodeKey, rootNodeType);

        List<TreeNodeDto> childs = new ArrayList<>();

        // 如果是进入页面第一次查询
        if (isRoot && isAssignRoot) {
            List<TreeNodeDto> nodes = doQueryAssignRoot(querier, context, rootNodeType, rootNodeKey);
            childs.addAll(nodes);

        } else if (isRoot) {
            // 系统层
            List<TreeNodeDto> systems = getTreeNodeProcessor(LAYER_NAME_SYSTEM).queryTree(querier, context);
            // 模块层
            List<TreeNodeDto> childSubGoals = getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SUBGOAL).queryTree(querier, context);
            childs.addAll(systems);
            childs.addAll(childSubGoals);

        } else {
            // 模块层
            List<TreeNodeDto> childSubGoals = getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SUBGOAL).queryTree(querier, context);
            // 场景层
            List<TreeNodeDto> childScenes = getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SCENE).queryTree(querier, context);
            // 场景实现层
            List<TreeNodeDto> childSceneImpls = getTreeNodeProcessor(WorkBenchConst.LAYER_NAME_SCENE_IMPL).queryTree(querier, context);

            childs.addAll(childSubGoals);
            childs.addAll(childScenes);
            childs.addAll(childSceneImpls);
        }

        // 这里统一根据序号进行排序
        return sortBySerialNo(childs);
    }

    public List<TreeNodeDto> doQueryAssignRoot(TreeNodeQuerier querier, TreeNodeQuerierContext context, String rootNodeType, String rootNodeKey) throws Exception {
        TreeNodeQuerier assignRootQuerier = new TreeNodeQuerier().setParentKey(rootNodeKey);
        assignRootQuerier.setUserObject(WorkBenchConst.SIGN_QUERY_SELF);
        return getTreeNodeProcessor(rootNodeType).queryTree(assignRootQuerier, context);
    }

    @Override
    public WidgetDto getWidget(PanelContext panelContext) throws Exception {
        TreeDto tree = (TreeDto) super.getWidget(panelContext);
        tree.setWidgetId(WIDGET_ID_WORK_BENCH_CATALOG_VIEW);
        tree.setAlwaysShowButtonBar(true);
        // 设置样式
        UniformStyleUtil.setTreeStyle(tree);
        return tree;
    }

    @Override
    public BoxDto getTopBar(PanelContext context, T widgetParam) throws Exception {
        BoxDto topBar = BoxDto.hbar();
        topBar.addChild(new ButtonDto("刷新", JDFICons.refresh)
                .setOnClick(newOnClick(getService(), CMD_GLOBAL_REFRESH, true, null))
                .setMargin(new InsetDto().setRight(5d))
        );
        topBar.addChild(new ButtonDto("新增系统", JDFICons.system_Icon)
                .setOnClick(newOnClick(getService(), CMD_CREATE_SYSTEM_LAYER_NODE, true, null))
                .setMargin(new InsetDto().setRight(5d))
        );

        topBar.setMargin(InsetDto.topBottom(10d));

        return topBar;
    }

    @Override
    public void onClickTreeNode(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {

        String nodeKey = (String) listener.getData();
        if (StrUtil.isBlank(nodeKey)) return;
        TreeNodeDto node = queryNode(panelContext, nodeKey);
        if (node == null) return;
        AbsOctoCmTreeProc proc = getTreeNodeProcessor(panelContext, node);

        Form form = null;
        Object binaryData = node.getBinaryData();
        if (binaryData instanceof TreeNodeExtraInfo) {
            TreeNodeExtraInfo<Form> extraInfo = (TreeNodeExtraInfo<Form>) binaryData;
            form = extraInfo.getData();
        }

        try (IDao dao = IDaoService.newIDao()) {
            // 刷新一下
            form = IFormMgr.get().queryForm(dao, form.getFormModelId(), form.getUuid());
        }
        WidgetDto nodeView = proc.buildNodeFormView(panelContext, node, form, false);

        fireRebuildContentViewEvent(panelContext, nodeView);


    }


    private void onNodeViewConfirm(ListenerDto listener, PanelContext panelContext) throws Exception {
        LoadingMask.showCircularProgress(panelContext);
        FormOpObserver observer = AppCacheUtil.getFormOpObserver(panelContext);

        FeDeliverData feDeliverData = (FeDeliverData) listener.getBinaryData();
        Object feObj = feDeliverData.getData();
        Form data = null;

        if (feObj instanceof ConfirmCallbackParam) {
            ConfirmCallbackParam confirmCallbackParam = (ConfirmCallbackParam) feObj;
            data = confirmCallbackParam.getForm();
        } else {
            throw CommonException.Builder.invalidCallback();
        }

        IFormMgr formMgr = IFormMgr.get();
        boolean isUpdate = !Op.isNewForm(data);
        try (IDao dao = IDaoService.newIDao()) {
            if (isUpdate) {
                formMgr.updateForm(null, dao, data, observer);
            } else {
                formMgr.createForm(null, dao, data, observer);
            }
            dao.commit();

        } finally {
            LoadingMask.hide(panelContext);
        }

        // 更新到树
        try {
            TreeNodeDto treeNodeDto = getTreeNodeProcessorByFormModelId(data.getFormModelId()).convert2FeTreeNodeDto(data);
            if (isUpdate) {
                updateNode(panelContext, treeNodeDto);
            } else {
                addNode(panelContext, treeNodeDto);
            }
        } catch (Exception ignored) {
        }
        try {
            QuitPopup.quit(panelContext);
        } catch (Exception ignored) {
        }

        PopToast.success(panelContext.getChannel(), "操作成功");

    }

    private void onNodeViewCancel(ListenerDto listener, PanelContext panelContext) throws Exception {
        fireRebuildContentViewEvent(panelContext, null);
    }


    private void onClickCreateButton(ListenerDto listener, PanelContext panelContext) throws Exception {
        if (!(listener instanceof OnButtonBarClick)) return;
        TreeNodeDto node = ((OnButtonBarClick<?>) listener).getNode();
        Form form = getFormByTreeNodeDto(node);
        if (form == null) return;

        AbsOctoCmTreeProc proc = getTreeNodeProcessorByFormModelId(form.getFormModelId());

        if (proc == null) throw CommonException.Builder.unrecognizedNodeType();

        proc.onClickCreateButton(
                listener, panelContext, null, node, null
        );

    }

    private void onCreateSystemLayerNode(ListenerDto listener, PanelContext panelContext) throws Exception {
        AbsOctoCmTreeProc proc = getTreeNodeProcessorByFormModelId(FormModelId_SystemLayer);
        proc.onCreateNode(listener, panelContext, null, null, null);
    }

    private void onCreateSceneImplNode(ListenerDto listener, PanelContext panelContext) throws Exception {
        String nodeKey = (String) listener.getData();
        if (StrUtil.isBlank(nodeKey)) return;
        TreeNodeDto node = queryNode(panelContext, nodeKey);

        // 从cmd中取出数据模型ID
        String formModelId = listener.getServiceCommand().split("\\+")[1];
        AbsOctoCmTreeProc proc = getTreeNodeProcessorByFormModelId(formModelId);
        if (proc == null) {
            proc = getTreeNodeProcessor(LAYER_NAME_SCENE_IMPL);
        }

        proc.onCreateNode(listener, panelContext, null, node, null);


    }


    private void onDeleteNode(ListenerDto listener, PanelContext panelContext) throws Exception {
        if (!(listener instanceof OnButtonBarClick)) return;

        if (!RapidViewBasicFunc.get().showConfirm(panelContext, "提示", "确定要执行删除操作？")) return;

        LoadingMask.showLinearProgress(panelContext);

        try {
            TreeNodeDto node = ((OnButtonBarClick<?>) listener).getNode();
            Form form = getFormByTreeNodeDto(node);
            if (form == null) return;

            try (IDao dao = IDaoService.newIDao()) {
                IFormMgr.get().deleteForm(dao, form.getFormModelId(), form.getUuid());
                dao.commit();
            }

            deleteNode(panelContext, node.getKey());
        } finally {
            LoadingMask.hide(panelContext);
        }


    }

    @Override
    public void onDropTreeNode(TreeDropListener listener, PanelContext panelContext, WidgetDto source) throws Exception {
        // 被拖到的节点
        TreeNodeDto targetNode = listener.getTargetNode();
        if (targetNode == null) return;
        // 来源的节点
        TreeNodeDto sourceNode = (TreeNodeDto) listener.getData();
        if (sourceNode == null) return;

        TreeNodeExtraInfo targetNodeExtraInfo = (TreeNodeExtraInfo) targetNode.getBinaryData();
        TreeNodeExtraInfo sourceNodeExtraInfo = (TreeNodeExtraInfo) sourceNode.getBinaryData();
        if (targetNodeExtraInfo == null || sourceNodeExtraInfo == null) return;

        // 双方类型不同不允许拖动
        if (!targetNodeExtraInfo.getNodeType().equals(sourceNodeExtraInfo.getNodeType())) {
            PopToast.info(panelContext.getChannel(), "非同一层级不允许拖动");
            return;
        }

        // 1、底层数据重排序
        // ...
        // 2、前端展示 删除-添加-更新位置
        MoveNodeDto moveNodeDto = new MoveNodeDto();
        moveNodeDto.setMoveKey(sourceNode.getKey())
                .setTargetKey(targetNode.getKey())
                .setPosition(listener.getDropPosition());

        moveNode(panelContext, moveNodeDto);


//        FeTreeNodeProcessor proc = getFactory().getTreeNodeProcessor(panelContext, targetNode, this, getBuilderService());
//        proc.dropNode(listener, panelContext);

    }

    // 当发布面板设计时
    // isPublishToApplication = 发布到面板 + 生效到应用
    private void onPublishPanelDesign(ListenerDto listener, PanelContext panelContext, boolean isPublishToApplication) throws Exception {

        // 发布应用，二次确认
        if (isPublishToApplication &&
                !Op.showYesOrNoDialog(panelContext, "提示",
                        "发布到应用可能会影响正在使用的用户，是否确定？")) {
            return;
        }


        String key = (String) listener.getData();
        TreeNodeDto beClickNodeDto = queryNode(panelContext, key);

        int currentNo = 0;
        List<TreeNodeDto> allSceneNodes = new ArrayList<>();


        // FIXME 太粗暴了
        if (beClickNodeDto.getLabel().contains("场景")) {
            allSceneNodes = CollUtil.newArrayList(beClickNodeDto);
        } else {
            Set<String> existedKeys = new HashSet<>();

            List<TreeNodeDto> tmp = queryRecursiveAllNodes(beClickNodeDto, currentNo).stream().filter(node -> {
                        try {
                            TreeNodeExtraInfo extraInfo = (TreeNodeExtraInfo) node.getBinaryData();
                            return extraInfo.getNodeType().equals(LAYER_NAME_SCENE);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            for (TreeNodeDto treeNodeDto : tmp) {
                if (existedKeys.contains(treeNodeDto.getKey())) continue;
                existedKeys.add(treeNodeDto.getKey());
                allSceneNodes.add(treeNodeDto);
            }
        }


        // 开始【发布-生效】场景
        doPublishAndTakeEffectPanelDesign(panelContext, allSceneNodes, isPublishToApplication);

    }


    // 发布并生效面板
    private void doPublishAndTakeEffectPanelDesign(PanelContext panelContext, List<TreeNodeDto> targetSceneNodes
            , boolean isTakeEffect) throws Exception {

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(panelContext);

        CFeProgressCtrlWithTextArea progressImpl = ProgressDialog.showProgressDialog(panelContext, "发布场景", false, false,
                ProgressDialogParam.TIME_FORMATTER_HMS, new ProgressBarDecorationDto().setShowCancelButton(false).setShowMessage(true).setShowPercentage(false));


        Progress progress = Progress.wrap(progressImpl);

        // 查看
        boolean isExitePanelDesign = IPanelDesignService.get().countExistedPanelDesign(observer) > 0;
        boolean isOverwriteExisted = false;
        if (isExitePanelDesign) {
            isOverwriteExisted = Op.showYesOrNoDialog(panelContext, "提示", "当前业务域中已存在面板设计，请确认是否使用覆盖模式");
        }

        try {

            Progress publishProgress = progress.newChildProgress(0, isTakeEffect ? 50 : 100);

            // 发布面板
            // 发布面板之前的版本也具备简单的初始化功能，不过后面逐渐在移除
            List<Form> publishedPanelDesigns = doPublishPanelDesign(publishProgress, panelContext, observer, targetSceneNodes, isOverwriteExisted);

            if (CollUtil.isEmpty(publishedPanelDesigns)) {
                PopToast.error(panelContext.getChannel(), "没有实际发布出任何场景！");
            }

            if (isTakeEffect) {

                Progress takeEffectProgress = progress.newChildProgress(50, 100);

                // 设置默认发布应用
                ApplicationUtil.getOrSetDefaultPublishAppCode(panelContext, observer);
                // 生效面板设计
                doTakeEffect(takeEffectProgress, panelContext, observer, publishedPanelDesigns);

            }

            List<ErrorDto> errors = PanelDesignPublishErrorContext.getErrors();
            if (Op.isEmpty(errors)) {
                PopToast.success(panelContext.getChannel(), "操作成功");
                return;
            } else {
                if (Op.showYesOrNoDialog(panelContext, "提示", "有部分面板发布失败，是否查看错误日志？")) {
                    PopHtmlView.popErrorListView(panelContext, errors);
                }

            }


        } catch (Exception e) {
            Op.logException(e);
        } finally {
            PanelDesignPublishErrorContext.clear();
            progress.finish();
        }

    }

    // 生效面板设计
    private static void doTakeEffect(Progress<?> progress, PanelContext panelContext,
                                     OctoDomainOpObserver observer, List<Form> publishedPanelDesigns) throws Exception {

        if (CollUtil.isEmpty(publishedPanelDesigns)) throw PanelDesignException.Builder.noPanelToPublish();

        int currExecNo = 1;
        int taskTotalNo = publishedPanelDesigns.size();
//        sendProcess(progress, 0, "\n");
        progress.sendProcess(0, StrUtil.format("批量生效场景，任务数量:[{}]", taskTotalNo), true);

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        for (Form panelDesign : publishedPanelDesigns) {
            String panelName = panelDesign.getString("面板名称");
            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);

            childProgress.sendProcess(currentProcessValue += unitProcessValue, StrUtil.format("正在生效[{}] [{}/{}]",
                    panelName, currExecNo++, taskTotalNo), true);
            try {

                PanelDesignPublishUtil.publish(childProgress, panelContext, observer, panelDesign);

            } catch (Exception e) {
                childProgress.sendProcess(100, StrUtil.format("生效失败: {}", e.getMessage()), true);
//                throw e;
                Op.logException(e);
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("面板生效失败, 面板名称[{}]", panelName),
                                ExceptionUtils.getFullStackTrace(e),
                                e
                        )
                );

            }
        }

        progress.sendProcess(100, StrUtil.format("面板生效成功"), true);


    }

    // 发布面板设计
    private static List<Form> doPublishPanelDesign(Progress<?> progress, PanelContext panelContext,
                                                   OctoDomainOpObserver observer, List<TreeNodeDto> targetSceneNodes, boolean isOverwriteExisted) throws Exception {

        int currExecNo = 1;
        int taskTotalNo = targetSceneNodes.size();

        progress.sendProcess(0, StrUtil.format("批量发布场景，任务数量:[{}]", taskTotalNo), true);

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        List<Form> publishedPanelDesigns = new ArrayList<>();
        for (TreeNodeDto sceneNode : targetSceneNodes) {

            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);


            childProgress.sendProcess(0, StrUtil.format("正在初始化[{}] [{}/{}]",
                            sceneNode.getLabel(), currExecNo++, taskTotalNo),
                    true
            );

            // 发布场景
            try {

                Form panelDesign = IPanelDesignService.get().publishSceneLayer(observer, sceneNode.getKey(), isOverwriteExisted);
                if (panelDesign != null) {
                    publishedPanelDesigns.add(panelDesign);
                }

                childProgress.sendProcess(100, StrUtil.format("面板初始化成功"), true);

            } catch (Exception e) {
                childProgress.sendProcess(100, StrUtil.format("初始化失败: {}", e.getMessage()), true);
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("面板初始化失败, 来源场景编号[{}]", sceneNode.getKey()),
                                ExceptionUtils.getFullStackTrace(e),
                                e
                        )
                );
                Op.logException(e);
            }

        }

        return publishedPanelDesigns;
    }

    private void onGlobalRefresh(ListenerDto listener, PanelContext panelContext) throws Exception {

        // 直接ReBuild
        try {

            WidgetDto widget = getWidget(panelContext);
            RebuildChild.rebuild(panelContext, widget);


//            FeCmnEvent event = new FeCmnEvent();
//            event
//                    .setInvokeClass(WorkBenchPage.class)
//                    .setInvokeParams(new Object[]{})
//                    .setCommand(CMD_REBUILD_GLOBAL_VIEW)
//                    .setSelfBinaryData();
//
//            FireEvent.fire(panelContext, event);
        } catch (Exception ignored) {
        }

    }

    @Override
    public Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
        String serviceCommand = listener.getServiceCommand();
//        PopToast.info(panelContext.getChannel(), serviceCommand);
        if (serviceCommand.startsWith(CMD_ON_CREATE_SCENE_IMPL)) {
            onCreateSceneImplNode(listener, panelContext);
            return null;
        } else if (CMD_CREATE_SYSTEM_LAYER_NODE.equals(serviceCommand)) {
            onCreateSystemLayerNode(listener, panelContext);
            return null;
        } else if (CMD_CREATE_NODE.equals(serviceCommand)) {
            onClickCreateButton(listener, panelContext);
            return null;
        } else if (CMD_DELETE_NODE.equals(serviceCommand)) {
            onDeleteNode(listener, panelContext);
            return null;
        } else if (CMD_NODE_VIEW_CONFIRM.equals(serviceCommand)) {
            onNodeViewConfirm(listener, panelContext);
            return null;
        } else if (CMD_NODE_VIEW_CANCEL.equals(serviceCommand)) {
            onNodeViewCancel(listener, panelContext);
            return null;
        } else if (CMD_ON_PUBLISH_TO_PANEL.equals(serviceCommand)) {
            onPublishPanelDesign(listener, panelContext, false);
            return null;
        } else if (CMD_ON_PUBLISH_TO_APPLICATION.equals(serviceCommand)) {
            onPublishPanelDesign(listener, panelContext, true);
            return null;
        } else if (CMD_GLOBAL_REFRESH.equals(serviceCommand)) {
            onGlobalRefresh(listener, panelContext);
            return null;


        }

        return super.onListener(listener, panelContext, source);
    }


    // ========================= 支撑方法 =========================


    private static void sendProcess(CFeProgressCtrlWithTextArea progress, int processNo, String text) {
        IProgressUtil.sendProcess(progress, processNo, text, true);
    }

    private List<TreeNodeDto> queryRecursiveAllNodes(TreeNodeDto beClickNodeDto, int currentNo) throws Exception {
        if (currentNo > 100) return new ArrayList<>();
        TreeNodeQuerier querier = new TreeNodeQuerier();
        querier.setParentKey(beClickNodeDto.getKey());
        List<TreeNodeDto> treeNodeDtos = queryChild(querier, null);
        if (CollUtil.isEmpty(treeNodeDtos)) return new ArrayList<>();

        List<TreeNodeDto> allNodes = new ArrayList<>();
        for (TreeNodeDto treeNodeDto : treeNodeDtos) {
            allNodes.addAll(treeNodeDtos);
            List<TreeNodeDto> childNodes = queryRecursiveAllNodes(treeNodeDto, currentNo + 1);
            if (CollUtil.isNotEmpty(childNodes)) {
                allNodes.addAll(childNodes);
            }
        }

        return allNodes;
    }


    private static void fireRebuildContentViewEvent(PanelContext panelContext, WidgetDto nodeView) throws Exception {
        FeCmnEvent event = new FeCmnEvent();
        event
                .setInvokeClass(WorkBenchPage.class)
                .setInvokeParams(nodeView != null ? new Object[]{nodeView} : new Object[]{})
                .setCommand(CMD_REBUILD_CONTENT_VIEW)
                .setSelfBinaryData();

        FireEvent.fire(panelContext, event);
    }

    // 根据序号排序
    private List<TreeNodeDto> sortBySerialNo(List<TreeNodeDto> children) {
        if (CollUtil.isEmpty(children)) return children;

        return children.stream().sorted((o1, o2) -> {
                    try {
                        TreeNodeExtraInfo<Form> e1 = (TreeNodeExtraInfo) o1.getBinaryData();
                        TreeNodeExtraInfo<Form> e2 = (TreeNodeExtraInfo) o2.getBinaryData();
                        Form f1 = e1.getData();
                        Form f2 = e2.getData();
                        Long n1 = CmnUtil.getLong(f1.getLong("序号"), Long.MAX_VALUE);
                        Long n2 = CmnUtil.getLong(f2.getLong("序号"), Long.MAX_VALUE);

                        return n1.compareTo(n2);

                    } catch (Exception e) {
                        return 0;
                    }

                })
                .collect(Collectors.toList());

    }

    private static Form getFormByTreeNodeDto(TreeNodeDto beClickNodeDto) throws ClassNotFoundException, IOException {
        if (beClickNodeDto == null) return null;
        Object binaryData = beClickNodeDto.getBinaryData();
        if (!(binaryData instanceof TreeNodeExtraInfo)) return null;
        TreeNodeExtraInfo<Form> extraInfo = (TreeNodeExtraInfo<Form>) binaryData;
        return extraInfo.getData();
    }


}
