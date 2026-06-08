package fe.octo.cm.page.component;

import bap.cells.Cells;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.fe.cmn.IFeCmnService;
import cell.fe.progress.CFeProgressCtrlWithTextArea;
import cell.gpf.adur.action.IActionMgr;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.gpf.dc.runtime.IPDFRuntimeMgr;
import cell.octo.cm.ContextModelMgr;
import cell.octo.cm.IWorkBenchFeService;
import cell.octocm.workbench.gui.Workbench2PanelDesignGuiAction;
import cmn.anotation.ClassDeclare;
import cmn.dto.PreloadTreeNode;
import cmn.dto.Progress;
import cmn.util.JsonUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.google.common.collect.Lists;
import fe.cmn.app.ability.PopToast;
import fe.cmn.data.CColor;
import fe.cmn.menu.MenuDto;
import fe.cmn.menu.MenuItemDto;
import fe.cmn.panel.*;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.panel.ability.PopMenu;
import fe.cmn.panel.ability.QuitPopup;
import fe.cmn.res.JDFICons;
import fe.cmn.tree.TreeMenuItemDto;
import fe.cmn.tree.TreeNodeDto;
import fe.cmn.tree.TreeNodeQuerier;
import fe.cmn.tree.TreeNodeQuerierContext;
import fe.cmn.widget.*;
import fe.cmn.widget.decoration.ButtonDecorationDto;
import fe.cmn.widget.listener.OnClickListener;
import fe.util.FeDebugUtil;
import fe.util.component.AbsComponent;
import fe.util.component.ProgressDialog;
import fe.util.component.dto.FeDeliverData;
import fe.util.intf.ServiceIntf;
import fe.util.style.FeStyleConst;
import gpf.adur.action.Action;
import gpf.adur.data.Form;
import gpf.dc.basic.dto.view.PDCFormViewDto;
import gpf.dc.basic.dto.view.PDFInstanceTableViewDto;
import gpf.dc.basic.fe.component.PopupCallbackBuilder;
import gpf.dc.basic.fe.component.app.AppCacheUtil;
import gpf.dc.basic.fe.component.app.AppTreePanel;
import gpf.dc.basic.fe.component.param.BaseTreeViewParam;
import gpf.dc.basic.fe.component.param.PopupCallbackParam;
import gpf.dc.basic.fe.component.view.AbsFormView;
import gpf.dc.basic.fe.intf.PopupCallback;
import gpf.dc.basic.fe.util.GpfViewActionUtil;
import gpf.dc.basic.param.view.FormParameter;
import gpf.dc.basic.param.view.dto.ApplicationSetting;
import gpf.dc.basic.param.view.dto.MenuNodeDto;
import gpf.dc.basic.param.view.dto.SubjectStyle;
import gpf.exception.VerifyException;
import jit.i18n.JITI18n;
import octo.cm.dto.ContextModel;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.exception.business.ViewException;
import octo.cm.util.UniformStyleUtil;
import octocm.design.consts.Octomica2DesignConst;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.consts.OctoCM2WorkBenchConst;
import org.apache.commons.lang3.StringUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.Exps;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Comment("应用-菜单树")
@ClassDeclare(
  label = "",
  what = "", why = "", how = "",
  developer = "裴硕", version = "1.0",
  createTime = "2025-08-18", updateTime = "2025-08-18"
)
public class PanelXTreePanel<T extends BaseTreeViewParam> extends AppTreePanel<T> {

 // 菜单树Item-右键单击
 public static final String CMD_MENU_TREE_ITEM_SECONDARY_CLICK = "CMD_MENU_ITEM_SECONDARY_CLICK";

 // 查看业务上下文
 public static final String CMD_SEE_BUS_CTX = "CMD_SEE_BUS_CTX";

 public static final String CMD_EDIT_CM_META = "CMD_EDIT_CM_META";


 public static final String CMD_ON_TURNING_EFFECT = "CMD_ON_TURNING_EFFECT";

 public static final String CMD_ON_TURNING_CONFIRM = "CMD_ON_TURNING_CONFIRM";


 // 如果为空的时候，设置一下高亮，否则会报错..
 // 这里修复的是底层的问题
 private static void _setNoHighlightIfNull(PanelContext panelContext, TreeNodeDto treeNodeDto) throws Exception {
  if (treeNodeDto == null) return;
  try {
   boolean highLighted = treeNodeDto.isHighLighted();
  } catch (Exception e) {
//            PopToast.info(panelContext.getChannel(), ExceptionUtils.getFullStackTrace(e));
   treeNodeDto.setHighLighted(false);

  }

 }

 @Override
 public Class<? extends ServiceIntf> getService() {
  return IWorkBenchFeService.class;
 }

 @Override
 public List<TreeNodeDto> queryChild(TreeNodeQuerier querier, TreeNodeQuerierContext context) throws Exception {
  List<TreeNodeDto> treeNodeDtos = super.queryChild(querier, context);


  return treeNodeDtos;
 }


 // ========================= 支撑方法 =========================

 @Override
 public List<TreeNodeDto> convert2TreeNodeList(PanelContext context, List<PreloadTreeNode<Form>> preloadTree) throws Exception {
  List<TreeNodeDto> treeNodeDtos = super.convert2TreeNodeList(context, preloadTree);
  if (CollUtil.isNotEmpty(treeNodeDtos)) {
   for (TreeNodeDto treeNodeDto : treeNodeDtos) {
    GestureDetectorDto detectorDto = treeNodeDto.getGestureDetector();
    if (detectorDto == null) detectorDto = new GestureDetectorDto();
    detectorDto.setOnSecondaryClick(
      newOnClickListener(treeNodeDto, CMD_MENU_TREE_ITEM_SECONDARY_CLICK)

    );

   }
  }
  return treeNodeDtos;
 }


 // 当菜单树Item被右键单击
 public void onMenuItemSecondaryClick(PanelContext panelContext, ListenerDto listener) throws Exception {
  if (FeDebugUtil.isEnableDebug(panelContext)) {
   if (listener == null || !(listener.getBinaryData() instanceof FeDeliverData<?>)) return;
   FeDeliverData<TreeNodeDto> deliverData = (FeDeliverData<TreeNodeDto>) listener.getBinaryData();
   TreeNodeDto treeNodeDto = deliverData.getData();

   List<MenuItemDto> menuItems = new ArrayList<>(CollUtil.newArrayList(
     createMenuItem(treeNodeDto, "编辑面板", JDFICons.modify, CMD_EDIT_CM_META)
//     , createMenuItem(treeNodeDto, "查看业务上下文", CMD_SEE_BUS_CTX)
   ));

   MenuDto menuDto = new MenuDto();
   menuDto.setMenuItems(menuItems);
   UniformStyleUtil.setMenuStyle(menuDto);

   menuDto.getMenutDecoration()
     .setMargin(InsetDto.all(0d))
     .setBackgroundColor(Color.WHITE)
   ;

   PopMenu.show(panelContext, menuDto);
  }
 }

 // 统一的创建菜单Item的方法
 public TreeMenuItemDto createMenuItem(TreeNodeDto treeNodeDto, String label, String icon, String cmd) throws Exception {
  TreeMenuItemDto itemDto = new TreeMenuItemDto().setNode(treeNodeDto);
  itemDto.setLabel(label).setIcon(icon);
  itemDto.setOnClick(
    newOnClickListener(treeNodeDto, cmd)
  );
  // 设置样式
  UniformStyleUtil.setMenuItemStyle(itemDto);
  return itemDto;

 }

 public OnClickListener newOnClickListener(TreeNodeDto node, String cmd) throws IOException {
  OnClickListener lsnr = new OnClickListener<>(getService(), cmd, true);
  FeDeliverData<TreeNodeDto> data = new FeDeliverData<>(PanelXTreePanel.class);
  data.setData(node);
  lsnr.setBinaryData(data);
  return lsnr;
 }

 // 查看业务上下文
 public void onSeeBusinessContext(PanelContext panelContext, ListenerDto listener) throws Exception {
//        PopToast.info(panelContext.getChannel(), "done1", JSONUtil.toJsonStr(listener));
  if (listener == null || !(listener.getBinaryData() instanceof FeDeliverData<?>)) return;
  FeDeliverData<TreeNodeDto> deliverData = (FeDeliverData<TreeNodeDto>) listener.getBinaryData();
  TreeNodeDto treeNodeDto = deliverData.getData();

  _setNoHighlightIfNull(panelContext, treeNodeDto);

  SinglePanelDto panelDto = SinglePanelDto.wrap(
    BoxDto.vbar(
        new LabelDto("ℹ️业务上下文"),
        new LabelDto("1、treeNodeDto：" + JSONUtil.toJsonStr(treeNodeDto))
      )
      .setCrossAxisAlignment(CrossAxisAlign.center)
      .setMainAxisAlignment(MainAxisAlign.center)
  );

  panelDto.setPreferHeightByWindowSize(0.6)
    .setPreferWidthByWindowSize(0.6);

  PopDialog.show(panelContext, "BUS CTX", panelDto);
 }

 public PanelDto buildPopPanel(IDao dao, PanelContext context, Form form) throws Exception {
  ApplicationSetting setting = AppCacheUtil.getSetting(context);
  SubjectStyle subjectStyle = setting.getSubjectStyle();
  CColor mainColor = subjectStyle.getMainColor();

  String viewModelId = PDCFormViewDto.FormModelId;
  String viewInstCode = "OctoCM_workbench_PanelDesignerCM";  // 常量


  IActionMgr actionMgr = IActionMgr.get();
  IPDFRuntimeMgr runtimeMgr = IPDFRuntimeMgr.get();


  Action viewAction = actionMgr.queryActionByCode(dao, viewModelId, viewInstCode);
  if (viewAction == null) {
   throw ViewException.Builder.notExist(viewModelId, viewInstCode);
  }


  IDCRuntimeContext rtx = runtimeMgr.newRuntimeContext();
  FormParameter.prepareFeActionParameter(rtx, context, (ListenerDto) null, null, form);
  rtx.setDao(dao);

  PanelDto panel = (PanelDto) actionMgr.executeAction(viewAction, rtx);
  AbsFormView editPanel = GpfViewActionUtil.getFormViewComponentByWidget(panel);

  //设置弹窗的底部栏按钮
  ButtonDto effect_btn = new ButtonDto("生效");
  effect_btn.setConfirmStyle()
    .setDecoration(new ButtonDecorationDto().setBackground(CColor.rgba(160, 133, 92, 255)))
    .setStyleName(FeStyleConst.common_form_submit_btn).setWidgetId(CMD_ON_TURNING_EFFECT);
  ButtonDto confirm_btn = new ButtonDto("确定");
  confirm_btn.setConfirmStyle()
    .setDecoration(new ButtonDecorationDto().setBackground(mainColor))
    .setStyleName(FeStyleConst.common_form_submit_btn)
    .setWidgetId(CMD_ON_TURNING_CONFIRM);

  List<ButtonDto> buttons = Lists.newArrayList(effect_btn, confirm_btn);
  String bottomBarStyle = FeStyleConst.common_form_bottom_bar_center;

  //通过设置表单弹窗的底部按钮栏和回调函数,在当前方法获取提交后的表单并继续执行
  PopupCallbackParam callbackParam = new PopupCallbackParam();
  CallbackClass<Object> callback = new CallbackClass<>(null, context);
  callbackParam
    .setCallback(callback)
    .setButtons(buttons)
    .setBottomBarStyle(bottomBarStyle)
    .setServiceProxy(IFeCmnService.class)
  ;
  return PopupCallbackBuilder.buildProxyPanel(context, editPanel, panel, callbackParam);

 }


 public void onEditCMMeta(PanelContext panelContext, ListenerDto listener) throws Exception {
  Object binaryData = listener.getBinaryData();

  FeDeliverData<TreeNodeDto> feDeliverData = (FeDeliverData<TreeNodeDto>) binaryData;
  TreeNodeDto treeNode = feDeliverData.getData();

  String menuUuid = treeNode.getKey();
  MenuNodeDto menu = AppCacheUtil.getMenuNode(panelContext, menuUuid);

  Action viewAction = menu.getViewAction();
  if (viewAction != null) {
   String actionModelId = viewAction.getActionModelId();

   String modelId;
   switch (actionModelId) {
    case PDFInstanceTableViewDto.FormModelId:
     modelId = viewAction.getString(PDFInstanceTableViewDto.sModelId);
     break;
    case PDCFormViewDto.FormModelId:
     modelId = viewAction.getString(PDCFormViewDto.sModelId);
     break;
    default:
     modelId = viewAction.getString("模型ID");
     break;
   }

   if (StringUtils.isEmpty(modelId)) {
    throw ViewException.Builder.cannotGetModelId(menu.getViewActionModel());
   }


   if (!StringUtils.startsWith(modelId, Octomica2DesignConst.PACKAGE_PATH)) {
    throw ViewException.Builder.notCMModel(modelId);
   }

   try (IDao dao = IDaoService.newIDao()) {
    IFormMgr formMgr = IFormMgr.get();

    String[] split = modelId.split("\\.");
    List<String> list = new ArrayList<>(Arrays.asList(split));
    String domain = list.get(2);
    if (split.length == 5) {  // octocm.md.CRM.process.IML_00020_CM 移除可能存在的第四个元素 [process]
     list.remove(3);
    }
    int lastIndex = list.size() - 1;
    list.set(lastIndex, formMgr.getFieldCode(list.get(lastIndex)));
    modelId = String.join(".", list);

    ContextModelMgr contextModelMgr = ContextModelMgr.get();
    ContextModel cm = contextModelMgr.queryCMByCode(dao, modelId);

    if (cm == null) {
     throw ViewException.Builder.cmNotFound(modelId);
    }
    String cnName = cm.getCnName().replaceAll("_CM$", "");

    String prefix = Octomica2DesignConst.OCTO_CM_PREFIX + domain;
    Cnd cnd = Cnd.where(Exps.eq(formMgr.getFieldCode(OctoCM2WorkBenchConst.面板设计器_面板编号), cnName))
      .and(Exps.like(Form.Code, prefix).left(""));
    Form designForm = formMgr.queryFormPage(dao, OctoCM2WorkBenchConst.ModelId_面板设计器, cnd, 1, 1
        , false, true).getDataList().stream()
      .findFirst().orElse(null);

    if (designForm == null) {
     throw PanelDesignException.Builder.notFoundWithName(cnName);
    }

    PanelDto wrap = buildPopPanel(dao, panelContext, designForm);
    wrap.setPreferSize(new WindowSizeDto(0.8, 0.8));
    PopDialog.show(panelContext, "面板设计器", wrap);
   }
  }
 }

 @Override
 public Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
  String serviceCommand = listener.getServiceCommand();


  switch (serviceCommand) {
   case CMD_MENU_TREE_ITEM_SECONDARY_CLICK:
    // 对着菜单右键
    onMenuItemSecondaryClick(panelContext, listener);
    break;
   case CMD_SEE_BUS_CTX:
    onSeeBusinessContext(panelContext, listener);
    break;
   case CMD_EDIT_CM_META:
    onEditCMMeta(panelContext, listener);
    break;
   default:
    return super.onListener(listener, panelContext, source);
  }
  return null;
 }


 class CallbackClass<T> extends PopupCallback<T> {

  public CallbackClass(AbsComponent srcComponent, PanelContext srcPanelContext) {
   super(srcComponent, srcPanelContext);
  }


  private void _doOnTurningOperate(PanelContext panelContext, String serviceCommand, Form form) throws Exception {
   try (IDao dao = IDaoService.newIDao()) {
    IFormMgr formMgr = IFormMgr.get();

    // TODO 查询业务域
    String code = form.getString(Form.Code);
    String[] split = code.split("_");
    String domainCode = Octomica2DesignConst.OCTO_CM_PREFIX + split[1];

    Form domianForm = formMgr.queryFormByCode(dao, Octomica2DesignConst.业务域_ModelId, domainCode);

    if (domianForm == null) {
     throw DomainException.Builder.notFoundWithCode(domainCode);
    }

    OctoDomainOpObserver observer = new OctoDomainOpObserver(domainCode, domianForm.getUuid());
    form = formMgr.updateForm(null, dao, form, observer);
    dao.commit();

    if (StringUtils.equals(serviceCommand, CMD_ON_TURNING_EFFECT)) {
     CFeProgressCtrlWithTextArea prog = ProgressDialog.showProgressDialog(panelContext, "生效...", true, true);
     try {
      Progress progress = Progress.wrap(prog);

      Workbench2PanelDesignGuiAction panelDesignGuiAction = Cells.get(Workbench2PanelDesignGuiAction.class);
      panelDesignGuiAction.publish(progress, domainCode, form);

      progress.finish();
      QuitPopup.quit(panelContext);
      PopToast.success(panelContext.getChannel(), "生效完成");
     } catch (Exception e) {
      prog.finishError(com.leavay.common.util.ToolUtilities.getFullExceptionMessage(e));
     }
    } else {
     PopToast.success(panelContext.getChannel(), "保存成功", JsonUtil.toJson(form));
     QuitPopup.quit(panelContext);
    }
   }
  }

  @Override
  public Object onListener(ListenerDto listener, PanelContext panelContext, WidgetDto source) throws Exception {
   String serviceCommand = listener.getServiceCommand();

   AbsComponent popupCmp = getPopupComponent();
   if (popupCmp instanceof AbsFormView) {
    AbsFormView absFormView = (AbsFormView) popupCmp;
    Form data = absFormView.getDataFormGui(panelContext, false, null);

    switch (serviceCommand) {
     case CMD_ON_TURNING_EFFECT:
     case CMD_ON_TURNING_CONFIRM: 
      _doOnTurningOperate(panelContext, serviceCommand, data);
      break;
    }
   }
   return null;
  }

 }

}
