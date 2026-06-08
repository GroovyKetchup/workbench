package cell.octo.cm.service;

import bap.cells.BasicCell;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.leavay.dfc.gui.LvUtil;
import com.leavay.ms.tool.CmnUtil;
import fe.cmn.panel.PanelContext;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import gpf.adur.role.Role;
import gpf.adur.user.User;
import gpf.exception.VerifyException;
import octo.cm.constant.WorkBenchConst;
import octo.cm.dto.ErrorDto;
import octo.cm.enums.DefaultSystemModule;
import octo.cm.dto.panelDesign.PanelButtonActionDto;
import octo.cm.dto.panelDesign.PanelCustomButtonDef;
import octo.cm.dto.panelDesign.PanelPublishOption;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.exception.business.SceneException;
import octo.cm.util.*;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;

import static octo.cm.constant.WorkBenchConst.*;

@Comment("面板设计服务实现类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-16", updateTime = "2025-06-16"
)
public class CPanelDesignService extends BasicCell implements IPanelDesignService {

    private static final EasyOperation Op = EasyOperation.get();

    // 一般情况下面板发布不需要关注要不要初始化角色，但是目前先临时这样做
    // 帮助所有角色去当前的组织下去创建一个组织，然后将admin移动过去，之后设置匹配规则
    private static final boolean IsInitRole = true;

    // 预判一下设置一个最大值
    public static final int MAX_PANEL_DESCRIPTION_LENGTH = 10_000;

    // 逻辑删除标志位
    String LogicalDeleteFlag = "#del#";


    @Override
    public void loadDefaultPanelDesign(OctoDomainOpObserver observer, DefaultSystemModule module) throws Exception {

        if (module == null) return;
        if (StrUtil.hasBlank(module.getPanelName(), module.getContentPath()))
            throw new VerifyException("系统模块的枚举类错误，内容和CDP配置路径不得为空");

        Form form = addCdpSystemModulesPanelDesign(observer, module);


        // TODO 抽成策略模式
        boolean isCustomShell = module.getPanelName().equals(DefaultSystemModule.CUSTOM_SHELL.getPanelName());
        if (isCustomShell) {
            // 如果是自定义外壳的话就不需要发布了
            return;
        }

        // 直接发布这个面板
        publishPanelDesignToDefaultApplicationBatch(
                null, observer,
                CollUtil.newArrayList(form)
        );

        if (PanelDesignPublishErrorContext.hasError()) {
            throw PanelDesignException.Builder.batchPublishError(PanelDesignPublishErrorContext.getErrorsAndClear());
        }


    }


    @Override
    public Form createPanelDesign(OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        return createPanelDesign(observer, panelDesignForm, null);
    }

    @Override
    public Form createPanelDesign(OctoDomainOpObserver observer, Form panelDesignForm, List<PanelCustomButtonDef> customButtons) throws Exception {
        if (observer == null) throw DomainException.Builder.observerNotFound();
        if (panelDesignForm == null) throw PanelDesignException.Builder.formEmpty();

        // 预检：如果用户传递了面板编号，检查是否已存在
        String inputPanelCode = panelDesignForm.getString("面板编号");
        if (StrUtil.isNotBlank(inputPanelCode)) {
            try (IDao checkDao = IDaoService.newIDao()) {
                Form existed = queryFormByAssignField(checkDao, FormModelId_PanelDesign, "面板编号", inputPanelCode);
                if (existed != null) {
                    throw PanelDesignException.Builder.panelCodeAlreadyExisted(inputPanelCode);
                }
            }
        }

        try (IDao dao = IDaoService.newIDao()) {

            // 无脑生成新的面板编号
            Form newForm = createPanelDesignForm(dao, observer);
            String newPanelCode = newForm.getString("面板编号");

            // 将用户传入的属性覆盖到新Form上（保留系统生成的编号和uuid）
            panelDesignForm.setUuid(newForm.getUuid());
            panelDesignForm.setAttrValue(Form.Code, newForm.getString(Form.Code));
            panelDesignForm.setAttrValue("面板编号", newPanelCode);
            panelDesignForm.setFormModelId(FormModelId_PanelDesign);

            panelDesignForm = IFormMgr.get().createForm(null, dao, panelDesignForm, observer);

            // 1、为面板数据行补齐属性实现（发布校验必需）
            initPanelDesignData(dao, observer, panelDesignForm);

            // 2、挂载系统默认按钮（刷新/删除/保存/取消/<面板名>_新增）
            initPanelDesignBehaviorButton(dao, observer, panelDesignForm, false);

            // 3、创建用户自定义按钮实现并追加到面板按钮表
            appendCustomButtons(dao, observer, panelDesignForm, customButtons);

            panelDesignForm = IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);

            dao.commit();

            return panelDesignForm;
        }
    }

    // 创建用户自定义按钮实现，并追加到面板按钮表
    private void appendCustomButtons(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm,
                                     List<PanelCustomButtonDef> customButtons) throws Exception {
        if (CollUtil.isEmpty(customButtons)) return;

        TableData panelBtnTd = panelDesignForm.getTable("面板按钮");
        if (panelBtnTd == null) panelBtnTd = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);

        for (PanelCustomButtonDef def : customButtons) {
            if (def == null || StrUtil.hasBlank(def.getButtonName(), def.getAlias(), def.getOperateFunction())) continue;

            Form btnImplForm = PanelDesignCommonFormUtil.createBtnImpl(
                    dao, observer, panelDesignForm.getUuid(),
                    def.getButtonName(), def.getAlias(),
                    CollUtil.newArrayList(
                            new PanelButtonActionDto().setOperateFunction(def.getOperateFunction())
                    ),
                    def.getDescription(), def.getCategory());

            if (btnImplForm == null) continue;

            Form slaveRow = Op.newForm(panelBtnTd.getFormModelId());
            slaveRow.setAttrValue("按钮别名", def.getAlias());
            slaveRow.setAttrValue("按钮说明", def.getDescription());
            slaveRow.setAttrValue("面板按钮", Op.toAssociationData(btnImplForm));
            panelBtnTd.add(slaveRow);
        }

        panelDesignForm.setAttrValue("面板按钮", panelBtnTd);
    }

    @Override
    public Form getPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode) throws Exception {
        return getPanelDesign(dao, observer, panelCode, true);
    }

    @Override
    public Form getPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelCode, boolean needCompoundField) throws Exception {
        if (observer == null) throw DomainException.Builder.observerNotFound();
        if (StrUtil.isBlank(panelCode)) return null;


        Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_PanelDesign);
        cnd.where().andEquals(getFieldCode("面板编号"), panelCode);

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_PanelDesign, cnd, 1, 1,
                true, needCompoundField);
        if (queryRs.isEmpty()) return null;

        return queryRs.getDataList().get(0);
    }

    @Override
    public List<Form> queryPanelDesigns(IDao dao, OctoDomainOpObserver observer, boolean needCompoundField) throws Exception {
        if (observer == null) throw DomainException.Builder.observerNotFound();

        Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_PanelDesign);

        ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_PanelDesign, cnd, 1, Integer.MAX_VALUE,
                true, needCompoundField);
        if (queryRs.isEmpty()) return null;

        return queryRs.getDataList();
    }

    @Override
    public long countExistedPanelDesign(OctoDomainOpObserver observer) throws Exception {
        if (observer == null) throw DomainException.Builder.observerNotFound();

        Cnd queryCnd = Op.getBusDomainFilterCondition(observer, FormModelId_PanelDesign);
        try (IDao dao = IDaoService.newIDao()) {
            return IFormMgr.get().countForm(dao, FormModelId_PanelDesign, queryCnd);

        }

    }

    @Override
    public Form publishSceneLayer(OctoDomainOpObserver observer, String sceneCode, boolean isOverwriteExisted) throws Exception {
        if (StrUtil.isBlank(sceneCode)) throw SceneException.Builder.sceneCodeEmpty();

        // 这个方法分为两个步骤,一个是发布,一个是初始化
        // 从场景相关的模型转换到面板设计,但是面板设计有的是可以直接转的,有的需要特殊逻辑添加一些预制的,有的需要大模型写
        // 直接转的归入发布,需要特殊逻辑添加/预制的归入初始化,此时不考虑大模型

        // 是否直接替换之前生成的数据
        // 目前有两个东西应该取[并集]：1、面板数据 2、面板约束（场景约束一一对应到面板约束）

        try (IDao dao = IDaoService.newIDao()) {


            Form sceneForm = queryFormByAssignField(dao, FormModelId_SceneLayer, Form.Code, sceneCode);
            if (sceneForm == null) throw SceneException.Builder.notFoundWithCode(sceneCode);


            // FIXME 这块逻辑有、问题
            AssociationData sceneCategoryAc = sceneForm.getAssociation("场景类型");
            if (sceneCategoryAc != null && "系统模块".equals(sceneCategoryAc.getForm().getString("分类名称"))) {
                throw PanelDesignException.Builder.systemModulePublishNotSupported();
            }

            // 面板设计Form
            Form panelDesignForm = getOrCreatePanelDesignForm(dao, observer, sceneCode);
            // 将场景数据转换到面板设计
            convertSceneDataToPanelDesign(dao, observer, sceneCode, null, isOverwriteExisted);
            // 将场景行为转换到面板设计
            convertSceneBehaviorToPanelDesign(dao, observer, sceneCode, null, isOverwriteExisted);
            // 将场景约束转换到面板设计
            convertSceneConstraintToPanelDesign(dao, observer, sceneCode, null, isOverwriteExisted);
            // 将场景编排转换到面板设计
            convertSceneOrchestrationToPanelDesign(dao, observer, sceneCode, null, isOverwriteExisted);

            // 将场景展示转换到面板设计
            // 这里简单设计了，把页面名称都塞到“页面入口”里面，以英文逗号分割
            convertSceneDisplayToPanelDesign(dao, observer, sceneCode, null);

            String panelDesignCode = panelDesignForm.getString(Form.Code);

            // 初始化面板设计
            initPanelDesign(dao, observer, panelDesignCode, isOverwriteExisted);

            dao.commit();

            // 返回最新版本的面板设计
            return queryFormByAssignField(
                    FormModelId_PanelDesign,
                    Form.Code,
                    panelDesignCode
            );
        }


    }

    @Override
    public List<Form> publishSceneBatch(Progress<?> progress, OctoDomainOpObserver observer, List<String> sceneCodes, boolean isOverwriteExisted) throws Exception {
        if (progress == null) progress = Progress.newOutput();

        int currExecNo = 1;
        int taskTotalNo = sceneCodes.size();

        progress.sendProcess(0, Progress_FLAG_WorkBench + StrUtil.format("批量发布场景，任务数量:[{}]", taskTotalNo), true);

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        List<Form> publishedPanelDesigns = new ArrayList<>();
        for (String sceneCode : sceneCodes) {

            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);


            childProgress.sendProcess(0, Progress_FLAG_WorkBench + StrUtil.format("正在执行初始化任务 [{}/{}]",
//                            sceneCode,
                            currExecNo++, taskTotalNo),
                    true
            );

            // 发布场景
            try {

                Form panelDesign = IPanelDesignService.get().publishSceneLayer(observer, sceneCode, isOverwriteExisted);
                if (panelDesign != null) {
                    publishedPanelDesigns.add(panelDesign);
                }

                childProgress.sendProcess(100, Progress_FLAG_WorkBench + StrUtil.format("面板初始化成功"), true);

            } catch (Exception e) {
                childProgress.sendProcess(100, Progress_FLAG_WorkBench + StrUtil.format("初始化失败: {}", e.getMessage()), true);


                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("面板初始化失败, 来源场景编号[{}]", sceneCode),
                                ExceptionUtils.getFullStackTrace(e),
                                e
                        )
                );
                Op.logException(e);

            }

        }

        return publishedPanelDesigns;


    }


    @Override
    public void publishPanelDesignToDefaultApplicationBatch(Progress<?> progress, OctoDomainOpObserver observer, List<Form> publishedPanelDesigns) throws Exception {

        if (CollUtil.isEmpty(publishedPanelDesigns)) throw PanelDesignException.Builder.noPanelToPublish();
        if (progress == null) progress = Progress.newOutput();


        int currExecNo = 1;
        int taskTotalNo = publishedPanelDesigns.size();
//        sendProcess(progress, 0, "\n");
        progress.sendProcess(0, Progress_FLAG_WorkBench + StrUtil.format("批量生效场景，任务数量:[{}]", taskTotalNo), true,
                progress.getDefaultLevel());

        int currentProcessValue = 0;
        int unitProcessValue = 100 / taskTotalNo;

        for (Form panelDesign : publishedPanelDesigns) {
            String panelName = panelDesign.getString("面板名称");
            Progress childProgress = progress.newChildProgress(currentProcessValue, currentProcessValue += unitProcessValue);

            childProgress.sendProcess(currentProcessValue += unitProcessValue, Progress_FLAG_WorkBench + StrUtil.format("正在发布[{}] [{}/{}]",
                    panelName, currExecNo++, taskTotalNo), true);
            try {

                PanelDesignPublishUtil.publish(childProgress, null, observer, panelDesign);

            } catch (Exception e) {
                childProgress.sendProcess(100, Progress_FLAG_WorkBench + StrUtil.format("生效失败: {}", e.getMessage()), true);
                Op.logException(e);
                PanelDesignPublishErrorContext.addError(
                        new ErrorDto(
                                StrUtil.format("面板生效失败, 面板名称[{}]", panelName),
                                e.getMessage(),
//                                ExceptionUtils.getFullStackTrace(e),
                                e

                        )
                );

            }
        }

        progress.sendProcess(100, Progress_FLAG_WorkBench + StrUtil.format("面板生效成功"), true);


    }


    @Override
    public void publishPanelDesign(OctoDomainOpObserver observer, String panelCode) throws Exception {
        publishPanelDesign(observer, panelCode, PanelPublishOption.publishOnly());
    }

    @Override
    public void publishPanelDesign(OctoDomainOpObserver observer, String panelCode, PanelPublishOption option) throws Exception {
        if (observer == null) throw DomainException.Builder.observerNotFound();
        if (StrUtil.isBlank(panelCode)) throw PanelDesignException.Builder.panelCodeEmpty();
        if (option == null) option = PanelPublishOption.publishOnly();

        try (IDao dao = IDaoService.newIDao()) {
            Form panelDesignForm = getPanelDesign(dao, observer, panelCode);
            if (panelDesignForm == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            // 1、纯发布
            PanelDesignPublishUtil.doPublishOnly(null, observer, panelDesignForm);

            // 2、按需挂载到指定应用菜单
            if (option.isAttachToMenu()) {
                String appCode = option.getApplicationCode();
                if (StrUtil.isBlank(appCode)) {
                    appCode = ApplicationUtil.getDefaultPublishApplicationCode(observer);
                }
                if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.defaultAppNotSet();

                PanelDesignPublishUtil.attachPanelToAppMenu(null, null, observer, panelDesignForm, appCode);
            }
        }
    }


    @Override
    public List<Form> publishSceneToDefaultApplicationBatch(Progress<?> progress, OctoDomainOpObserver observer, List<String> sceneCodes, boolean isOverwriteExisted) throws Exception {
        if (progress == null) progress = Progress.newOutput();

        // 查看
        boolean isExitePanelDesign = IPanelDesignService.get().countExistedPanelDesign(observer) > 0;

        try {

            Progress publishProgress = progress.newChildProgress(0, 50);

            // 发布面板
            List<Form> publishedPanelDesigns = publishSceneBatch(publishProgress, observer, sceneCodes, isExitePanelDesign);


            Progress takeEffectProgress = progress.newChildProgress(50, 100);

            // 生效面板设计
            publishPanelDesignToDefaultApplicationBatch(takeEffectProgress, observer, publishedPanelDesigns);


            // 由于这里是直接从场景发布到应用，所以把生成的面板给用户返回出去
            return publishedPanelDesigns;


        } catch (Exception e) {
            Op.logException(e);
        } finally {
            PanelDesignPublishErrorContext.clear();
            progress.finish();
        }
        return null;
    }

    @Override
    @Deprecated
    public void takeEffectPanelDesign(PanelContext panelContext, String panelCode) throws Exception {
        Form panelDesignForm = queryFormByAssignField(FormModelId_PanelDesign, Form.Code, panelCode);
        if (panelDesignForm == null)
            throw PanelDesignException.Builder.notFoundWithCode(panelCode);

//        PanelDesignTakeEffectUtil.doTakeEffect(panelContext, panelDesignForm,
//                false, false, false);

    }

    @Override
    public void convertSceneDataToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String dataCode, boolean isOverwriteExisted) throws Exception {
        if (StrUtil.isAllBlank(sceneCode, dataCode)) throw SceneException.Builder.sceneAndDataCodeBothEmpty();
        List<Form> dataForms = null;

        if (StrUtil.isNotBlank(dataCode)) {
            dataForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Data, "数据编号", dataCode);
        } else {
            dataForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Data, "上层场景编号", sceneCode);
        }

        if (CollUtil.isEmpty(dataForms)) {
            LvUtil.trace(StrUtil.format("无法找到[场景编号:{}, 数据编号:{}]的场景数据", sceneCode, dataCode));
            return;
        }


        TableData needsImplDataTd = new TableData(SlaveFormModelId_PanelDesign_Data);

        // 将[场景-数据]转译到面板描述里
        StringBuilder sceneDataDescSb = new StringBuilder("场景数据:\n");

        Set<String> sceneNames = new HashSet<>();
        for (Form dataForm : dataForms) {
            TableData sceneDataTd = dataForm.getTable("数据内容");

            if (sceneDataTd != null && !sceneDataTd.isEmtpy()) {
                for (Form sceneDataForm : sceneDataTd.getRows()) {
                    Form needsImplDataForm = new Form(needsImplDataTd.getFormModelId());
                    needsImplDataForm.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, needsImplDataForm.getUuid());

                    // 直接跟场景数据绑定，而非：字段-字段
                    needsImplDataForm.setAttrValue("场景数据",
                            new AssociationData(dataForm.getFormModelId(), dataForm.getString(Form.Code)));

                    String attrName = sceneDataForm.getString("属性名称");
                    if (StrUtil.isBlank(attrName) || sceneNames.contains(attrName)) continue;
                    String attrStyle = sceneDataForm.getString("样式类型");

                    // 标准化属性名称
                    attrName = PanelDesignCommonFormUtil.standardAttrName(attrName);

                    sceneNames.add(attrName);
                    needsImplDataForm.setAttrValue("场景属性名称", attrName);
                    needsImplDataForm.setAttrValue("场景属性样式", attrStyle);

                    needsImplDataTd.add(needsImplDataForm);

                    sceneDataDescSb.append(StrUtil.format("场景属性名称: {}, 属性样式: {}\n", attrName, attrStyle));

                }

            }
        }

        updateFieldValueToPanelDesignForm(dao, observer, sceneCode, "面板数据", needsImplDataTd);
        appendTextToPanelDescription(dao, observer, sceneCode, sceneDataDescSb.toString());

    }


    @Override
    public void convertSceneBehaviorToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String behaviorCode, boolean isOverwriteExisted) throws Exception {
        if (StrUtil.isAllBlank(sceneCode, behaviorCode)) throw SceneException.Builder.sceneAndBehaviorCodeBothEmpty();

        List<Form> sceneBehivorForms = null;

        if (StrUtil.isNotBlank(behaviorCode)) {
            sceneBehivorForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Behavior, "行为编号", behaviorCode);
        } else {
            sceneBehivorForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Behavior, "上层场景编号", sceneCode);
        }

        if (CollUtil.isEmpty(sceneBehivorForms)) {
            LvUtil.trace(StrUtil.format("无法找到[场景编号:{}, 行为编号:{}]的场景行为", sceneCode, behaviorCode));
            return;
        }

        // 将[场景-数据]转译到面板描述里
        StringBuilder sceneBehaviorDescSb = new StringBuilder("场景行为:\n");
        List<AssociationData> sceneBehaviorAcs = new ArrayList<>();
        for (Form sceneBehivorForm : sceneBehivorForms) {
            String behaviorName = sceneBehivorForm.getString("行为名称");
            String behaviorDesc = sceneBehivorForm.getString("描述");

            sceneBehaviorDescSb.append(StrUtil.format("行为名称: {}, 描述: {}\n", behaviorName, behaviorDesc));

            TableData sceneBehaviorTd = sceneBehivorForm.getTable("行为内容");
            if (!Op.isEmpty(sceneBehaviorTd)) {
                sceneBehaviorDescSb.append("行为内容:\n");

                for (Form sceneBehaviorForm : sceneBehaviorTd.getRows()) {
                    // 操作名称、行为主体、行为操作、行为客体、前置条件、执行结果
                    sceneBehaviorDescSb.append(StrUtil.format("操作名称: {}, 行为主体: {}, 行为操作: {}, 行为客体: {}, 前置条件: {}, 执行结果: {}\n",
                            CmnUtil.getString(sceneBehaviorForm.getString("操作名称"), "无"),
                            CmnUtil.getString(sceneBehaviorForm.getString("行为主体"), "无"),
                            CmnUtil.getString(sceneBehaviorForm.getString("行为操作"), "无"),
                            CmnUtil.getString(sceneBehaviorForm.getString("行为客体"), "无"),
                            CmnUtil.getString(sceneBehaviorForm.getString("前置条件"), "无"),
                            CmnUtil.getString(sceneBehaviorForm.getString("执行结果"), "无")

                    ));

                }


            }

            sceneBehaviorAcs.add(Op.toAssociationData(sceneBehivorForm));

        }

        updateFieldValueToPanelDesignForm(dao, observer, sceneCode, "面板行为", sceneBehaviorAcs);
        appendTextToPanelDescription(dao, observer, sceneCode, sceneBehaviorDescSb.toString());

    }

    @Override
    public void convertSceneConstraintToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String constraintCode, boolean isOverwriteExisted) throws Exception {

        if (StrUtil.isAllBlank(sceneCode, constraintCode))
            throw SceneException.Builder.sceneAndConstraintCodeBothEmpty();
        List<Form> sceneConstraintForms = null;

        if (StrUtil.isNotBlank(constraintCode)) {
            sceneConstraintForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Constraint, "约束编号", constraintCode);
        } else {
            sceneConstraintForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Constraint, "上层场景编号", sceneCode);

        }

        if (CollUtil.isEmpty(sceneConstraintForms)) {
            LvUtil.trace(StrUtil.format("无法找到[场景编号:{}, 约束编号:{}]的场景约束", sceneCode, constraintCode));
            return;
        }

        StringBuilder sceneConstraintDescSb = new StringBuilder("场景约束:\n");
        List<AssociationData> sceneConstraintAcs = new ArrayList<>();
        for (Form sceneConstraintForm : sceneConstraintForms) {
            String constraintName = sceneConstraintForm.getString("约束名称");
            String constraintDesc = sceneConstraintForm.getString("描述");
            sceneConstraintDescSb.append(StrUtil.format("约束名称: {}, 描述: {}\n", constraintName, constraintDesc));
            sceneConstraintAcs.add(Op.toAssociationData(sceneConstraintForm));

            TableData sceneConstraintTd = sceneConstraintForm.getTable("约束内容");
            if (!Op.isEmpty(sceneConstraintTd)) {
                sceneConstraintDescSb.append("约束内容:\n");

                for (Form constraintContentForm : sceneConstraintTd.getRows()) {
                    // 约束名称、对象类型、对象名称、规则描述、约束规则
                    sceneConstraintDescSb.append(StrUtil.format("约束名称: {}, 对象类型: {}, 对象名称: {}, 规则描述: {}, 约束规则: {}\n",
                            CmnUtil.getString(constraintContentForm.getString("约束名称"), "无"),
                            CmnUtil.getString(constraintContentForm.getString("对象类型"), "无"),
                            CmnUtil.getString(constraintContentForm.getString("对象名称"), "无"),
                            CmnUtil.getString(constraintContentForm.getString("规则描述"), "无"),
                            CmnUtil.getString(constraintContentForm.getString("约束规则"), "无")
                    ));
                }
            }
        }

        updateFieldValueToPanelDesignForm(dao, observer, sceneCode, "面板约束", sceneConstraintAcs);
        appendTextToPanelDescription(dao, observer, sceneCode, sceneConstraintDescSb.toString());


    }

    @Override
    public void convertSceneOrchestrationToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String orchestrationCode, boolean isOverwriteExisted) throws Exception {
        if (StrUtil.isAllBlank(sceneCode, orchestrationCode))
            throw SceneException.Builder.sceneAndOrchestrationCodeBothEmpty();

        List<Form> sceneOrchestrationForms = null;

        if (StrUtil.isNotBlank(orchestrationCode)) {
            sceneOrchestrationForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Orchestration, "编排编号", orchestrationCode);
        } else {
            sceneOrchestrationForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Orchestration, "上层场景编号", sceneCode);
        }

        if (CollUtil.isEmpty(sceneOrchestrationForms)) {
            LvUtil.trace(StrUtil.format("无法找到[场景编号:{}, 编排编号:{}]的场景编排", sceneCode, orchestrationCode));
            return;
        }

        StringBuilder sceneOrchestrationDescSb = new StringBuilder("场景编排:\n");
        List<AssociationData> sceneOrchestrationAcs = new ArrayList<>();

        for (Form sceneOrchestrationForm : sceneOrchestrationForms) {
            String orchestrationName = sceneOrchestrationForm.getString("编排名称");
            String orchestrationDesc = sceneOrchestrationForm.getString("描述");

            sceneOrchestrationDescSb.append(StrUtil.format("编排名称: {}, 描述: {}\n", orchestrationName, orchestrationDesc));

            TableData orchestrationContentTd = sceneOrchestrationForm.getTable("编排内容");
            if (!Op.isEmpty(orchestrationContentTd)) {
                sceneOrchestrationDescSb.append("编排内容:\n");

                for (Form orchestrationContentForm : orchestrationContentTd.getRows()) {
                    // 编排编号、编排名称、正常流程
                    String normalProcess = orchestrationContentForm.getString("正常流程");
                    sceneOrchestrationDescSb.append(StrUtil.format("编排编号: {}, 编排名称: {}, 正常流程: {}\n",
                            CmnUtil.getString(orchestrationContentForm.getString("编排编号"), "无"),
                            CmnUtil.getString(orchestrationContentForm.getString("编排名称"), "无"),
                            CmnUtil.getString(normalProcess, "无")
                    ));
                }
            }

            sceneOrchestrationAcs.add(Op.toAssociationData(sceneOrchestrationForm));
        }

        updateFieldValueToPanelDesignForm(dao, observer, sceneCode, "编排需求", sceneOrchestrationAcs);
        appendTextToPanelDescription(dao, observer, sceneCode, sceneOrchestrationDescSb.toString());

    }

    @Override
    public void convertSceneDisplayToPanelDesign(IDao dao, OctoDomainOpObserver observer, String sceneCode, String displayCode) throws Exception {
        if (StrUtil.isAllBlank(sceneCode, displayCode))
            throw SceneException.Builder.sceneAndDisplayCodeBothEmpty();
        List<Form> sceneDisplayForms = null;

        if (StrUtil.isNotBlank(displayCode)) {
            sceneDisplayForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Display, "展示编号", displayCode);
        } else {
            sceneDisplayForms = queryFormsByAssignField(dao, FormModelId_SceneLayer_Display, "上层场景编号", sceneCode);
        }

        if (CollUtil.isEmpty(sceneDisplayForms)) {
            LvUtil.trace(StrUtil.format("无法找到[场景编号:{}, 展示编号:{}]的场景展示", sceneCode, displayCode));
            return;
        }

        Set<String> displayPageNames = new TreeSet<>();
        StringBuilder sceneDisplayDescSb = new StringBuilder("场景展示:\n");

        for (Form sceneDisplayForm : sceneDisplayForms) {
            String displayName = sceneDisplayForm.getString("展示名称");
            String displayDesc = sceneDisplayForm.getString("描述");

            sceneDisplayDescSb.append(StrUtil.format("展示名称: {}, 描述: {}\n", displayName, displayDesc));

            TableData displayContentTd = sceneDisplayForm.getTable("展示内容");
            if (!Op.isEmpty(displayContentTd)) {
                sceneDisplayDescSb.append("展示内容:\n");

                for (Form displayContentForm : displayContentTd.getRows()) {
                    String pageName = displayContentForm.getString("页面名称");
                    String pageDesc = displayContentForm.getString("页面说明");
                    String displayContent = displayContentForm.getString("展示内容");

                    sceneDisplayDescSb.append(StrUtil.format("页面名称: {}, 页面说明: {}, 展示内容: {}\n",
                            CmnUtil.getString(pageName, "无"),
                            CmnUtil.getString(pageDesc, "无"),
                            CmnUtil.getString(displayContent, "无")
                    ));
                }
            }
        }

        // 存储页面入口信息
        updateFieldValueToPanelDesignForm(dao, observer, sceneCode, "页面入口", String.join(",", displayPageNames));
        // 追加展示描述信息
        appendTextToPanelDescription(dao, observer, sceneCode, sceneDisplayDescSb.toString());
    }


    // ========================= 支撑方法 =========================


    // 保存面板网页
    public Form savePanelWebPage(IDao dao, OctoDomainOpObserver observer, String panelCode, String content,
                                 boolean isStandardConfig) throws Exception {
        try {


            Form panelDesignForm = getPanelDesign(dao, observer, panelCode);
            if (panelDesignForm == null)
                throw PanelDesignException.Builder.notFoundWithCode(panelCode);

            String panelName = panelDesignForm.getString("面板名称");
            TableData panelPageTd = panelDesignForm.getTable("面板网页");

            // 从TD中找到这个类型的页面，并保存进去
            if (panelPageTd == null)
                panelPageTd = new TableData(WorkBenchConst.SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
            Form webPageForm = findWebPageByContentType(panelPageTd.getRows(), isStandardConfig);
            if (webPageForm == null) {
                webPageForm = Op.newForm(panelPageTd.getFormModelId());
                panelPageTd.add(webPageForm);
            }
            webPageForm.setAttrValue("页面代码", content);
            panelDesignForm.setAttrValue("面板网页", panelPageTd);

            // 将当前页面设置为页面入口，同时防止任何情况下的页面名称为空
            String pageName = webPageForm.getString("页面名称");
            if (pageName == null) {
                pageName = StrUtil.format("{}_{}", panelName,
                        !isStandardConfig ? "个性" : "标准");
                webPageForm.setAttrValue("页面名称", pageName);
            }

            panelDesignForm.setAttrValue("页面入口", pageName);

            return IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);


        } catch (Exception e) {
            Op.logException(e);
        }
        return null;
    }


    // 添加[CDP]的系统面板
    private Form addCdpSystemModulesPanelDesign(OctoDomainOpObserver observer,
                                                DefaultSystemModule module) throws Exception {
        if (module == null) throw new VerifyException("系统模块不得为空");
        if (StrUtil.hasBlank(module.getContentPath(), module.getPanelName()))
            throw new VerifyException("CDP配置路径和面板名称不得为空");

        String panelName = module.getPanelName();
        String panelDescription = "";
        String contentPath = module.getContentPath();


        if (StrUtil.hasBlank(contentPath, panelName)) throw new VerifyException("CDP配置路径和面板名称不得为空");

        // 从resource获取内容
        String content = Op.getResourceStr(contentPath);
        if (StrUtil.isBlank(content)) throw new VerifyException(StrUtil.format("路径[{}]的CDP配置为空",
                contentPath));

        // 填充变量
        content = fillInSystemModuleBasicVariable(observer, content);

        // TODO 抽成策略模式
        boolean isCustomShell = module.getPanelName().equals(DefaultSystemModule.CUSTOM_SHELL.getPanelName());
        if (isCustomShell) {
            Map<String, Object> params = module.getParams();
            String shellName = (String) params.getOrDefault("shellName", "");
            String shellDescription = (String) params.getOrDefault("shellDescription", "");
            if (StrUtil.isNotBlank(shellName)) {
                panelName = shellName;
                panelDescription = shellDescription;
            }

        }


        try (IDao dao = IDaoService.newIDao()) {
            // 创建面板
            Form panelDesignForm = createPanelDesignForm(dao, observer);

            panelDesignForm = IFormMgr.get().createForm(null, dao, panelDesignForm, observer);
            // 设置为系统分类
            panelDesignForm.setAttrValue("面板分类",
                    PanelCategoryUtil.getSystemModuleCategoryAc());

            panelDesignForm.setAttrValue("面板名称", panelName);
            panelDesignForm.setAttrValue("面板描述", panelDescription);
            panelDesignForm.setAttrValue("页面入口", panelName);

            TableData panelPageTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_WebPage);
            Form panelPageForm = Op.newForm(panelPageTd.getFormModelId());
            panelPageForm.setAttrValue("页面名称", panelName)
                    .setAttrValue("页面代码", content);
            panelPageTd.add(panelPageForm);
            panelDesignForm.setAttrValue("面板网页", panelPageTd);


            panelDesignForm = IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);

            dao.commit();


            return panelDesignForm;

        }


    }

    private String fillInSystemModuleBasicVariable(OctoDomainOpObserver observer, String content) {
        if (StrUtil.isBlank(content)) return content;

        if (observer != null) {
            content = content.replace(Variable_Name_BusDomainCode, observer.getDomainCode());
        }

        return content;


    }


    // 初始化面板设计
    public void initPanelDesign(IDao dao, OctoDomainOpObserver observer, String panelDesignCode, boolean isOverwriteExisted) throws Exception {
        Form panelDesignForm = IFormMgr.get().queryFormByCode(dao, FormModelId_PanelDesign, panelDesignCode);
        if (panelDesignForm == null) {
            LvUtil.trace(StrUtil.format("初始化面板设计失败，面板设计不得为空[code={}]", panelDesignCode));
            return;
        }

        boolean isWebPage = PanelCategoryUtil.isDashBoard(panelDesignForm);

        // 初始化【数据构面-面板数据】
        initPanelDesignData(dao, observer, panelDesignForm);

        // 初始化【行为构面-面板角色】
        initPanelDesignBehaviorRole(dao, observer, panelDesignForm);

        if (!isWebPage) {
            // 初始化【行为构面-面板按钮】
            initPanelDesignBehaviorButton(dao, observer, panelDesignForm, isOverwriteExisted);

            // 初始化【约束构面-面板权限】
            initPanelDesignPermission(dao, observer, panelDesignForm, isOverwriteExisted);

            // 初始化【页面编排-面板表格】
            initPanelDesignViewOrchestrationTable(dao, observer, panelDesignForm, isOverwriteExisted);

            // 初始化【页面编排-面板表单】
            // TODO 行为那边也有预制的，可以绑定一下
            initPanelDesignViewOrchestrationForm(dao, observer, panelDesignForm, isOverwriteExisted);

            // 设置页面入口
            setPanelPageEntry(dao, observer, panelDesignForm, isOverwriteExisted);


        }


        IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);


    }

    // 设置页面入口
    private void setPanelPageEntry(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, boolean isOverwriteExisted) throws Exception {

        TableData tableTd = panelDesignForm.getTable("面板表格");
        if (Op.isEmpty(tableTd)) {
            LvUtil.trace("没有任何表格，因此无法设置面板入口");
            return;
        }

        String firstTableName = tableTd.getRows().get(0).getString("表格名称");
        panelDesignForm.setAttrValue("页面入口", firstTableName);

    }

    // 初始化【数据构面-面板数据】
    private void initPanelDesignData(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        TableData dataTd = panelDesignForm.getTable("面板数据");
        // 面板数据跟其他的构面不同，只是补充用户的数据
        if (dataTd == null) return;

        for (Form row : dataTd.getRows()) {

            String sceneAttrName = row.getString("场景属性名称");
            String sceneAttrStyle = row.getString("场景属性样式");
            if (StrUtil.hasBlank(sceneAttrName)) continue;
            if (StrUtil.isBlank(sceneAttrStyle)) sceneAttrStyle = "文本";

            AssociationData attrImplAc = row.getAssociation("属性实现");
            // 如果已经有属性实现了，就不处理了
            if (attrImplAc != null) continue;

            // 可能需要在这里留个口子检查样式是否规范
            Form attrImplForm = PanelDesignCommonFormUtil.getOrCreateAttrImpl(dao, observer,
                    panelDesignForm.getUuid(), sceneAttrName, sceneAttrStyle);
            if (attrImplForm == null) continue;


            row.setAttrValue("属性样式", attrImplForm.getString("属性样式"));
            row.setAttrValue("属性实现", toAssociationData(attrImplForm));

        }


        panelDesignForm.setAttrValue("面板数据", dataTd);


    }

    // 初始化【行为构面-面板角色】
    private void initPanelDesignBehaviorRole(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm) throws Exception {
        if (!IsInitRole) return;
        List<AssociationData> panelRoles = panelDesignForm.getAssociations("面板角色");
        if (isEmpty(panelRoles)) return;
        List<Form> panelRoleForms = queryFormsByAcs(dao, panelRoles);
        if (isEmpty(panelRoleForms)) return;

        for (Form panelRoleForm : panelRoleForms) {
            String roleName = panelRoleForm.getString("角色名称");
            if (StrUtil.isBlank(roleName)) continue;
            String domainCode = observer.getDomainCode();

//            if (true) {
//                throw new RuntimeException("domainCode:" + domainCode);
//            }

            String orgModelId = StrUtil.format("gpf.md.org.{}_Org", domainCode);
            String userModelId = StrUtil.format("gpf.md.user.{}_User", domainCode);


            String targetOrgPath = StrUtil.format("/平台初始化组织/{}", roleName);

            // 获取/创建这个组织下的角色
            Role targetRole = Op.getOrCreateSimpleRole(dao, observer, orgModelId, targetOrgPath,
                    "成员");


            // 新增用户到这个角色
            // FIXME 这里的逻辑后续应该调整
            User adminUser = Op.getOrCreateUserQuickly(dao, observer, userModelId, "admin", "123456");
            if (adminUser == null) throw new RuntimeException("创建默认用户[admin]失败");
            Op.addUserToAssignRole(dao, orgModelId, userModelId, targetRole, adminUser.getUserName());

            panelRoleForm.setAttrValue("组织匹配", StrUtil.format("指定范围('{}')", targetOrgPath));

            IFormMgr.get().updateForm(null, dao, panelRoleForm, observer);


        }


    }

    // 初始化【行为构面-面板权限】
    private void initPanelDesignPermission(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, boolean isOverwriteExisted) throws Exception {
        if (panelDesignForm == null) return;
        TableData panelPermissionTd = panelDesignForm.getTable("面板权限");
        // 如果不为空,代表用户自定义处理了,或者已经初始化了,那就不处理了
        if (!isEmpty(panelPermissionTd)) return;


    }

    private void initPanelDesignBehaviorButton(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, boolean isOverwriteExisted) throws Exception {

        // 不为空就不处理了，代表用户已经自己设计了
        // 但是如果是直接替换，则无视
        if (!Op.isEmpty(panelDesignForm.getTable("面板按钮")) && !isOverwriteExisted) {
            Op.logf("面板[{}]未添加默认的面板按钮，因为面板本身已存在按钮", panelDesignForm.getString(Form.Code));
            return;
        }

        TableData newBtnTd = PanelDesignCommonFormUtil.appendDefaultPanelButtonTd(dao, observer, panelDesignForm);
        if (newBtnTd == null) return;
        panelDesignForm.setAttrValue("面板按钮", newBtnTd);


    }


    // 初始化【页面编排-面板表单】
    private void initPanelDesignViewOrchestrationForm(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, boolean isOverwriteExisted) throws Exception {
        TableData formTd = panelDesignForm.getTable("面板表单");
        // 不为空就不处理了，代表用户已经自己设计了
        if (!isEmpty(formTd) && !isOverwriteExisted) return;


//        Set<String> displayPageNames = getDisplayPageNamesFromPageEntry(panelDesignForm);
//        if (Op.isEmpty(displayPageNames)) return;

        formTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Form);

        String panelName = panelDesignForm.getString("面板名称");

//        for (String pageName : displayPageNames) {

        Form form = newForm(formTd.getFormModelId());

        String formName = PanelDesignCommonFormUtil.buildPanelFormName(panelName);

        form.setAttrValue("表单名称", formName);
        form.setAttrValue("属性", getDataAxisAttrNamesByPanelDesignForm(panelDesignForm));
        form.setAttrValue("重置布局", true);

        List<String> btnNames = getButtonNamesByPanelButton(dao, panelDesignForm.getTable("面板按钮"),
                true, false, CollUtil.newHashSet("保存", "取消"));
        form.setAttrValue("按钮", CollUtil.join(btnNames, ","));

        formTd.add(form);

//        }


        panelDesignForm.setAttrValue("面板表单", formTd);

    }


    // 初始化【页面编排-面板表格】
    private void initPanelDesignViewOrchestrationTable(IDao dao, OctoDomainOpObserver observer, Form panelDesignForm, boolean isOverwriteExisted) throws Exception {
        TableData tableTd = panelDesignForm.getTable("面板表格");
        // 不为空就不处理了，代表用户已经自己设计了
        if (!isEmpty(tableTd) && !isOverwriteExisted) return;

        String panelCode = panelDesignForm.getString("面板编号");
        String panelName = panelDesignForm.getString("面板名称");


        Set<String> displayPageNames = getDisplayPageNamesFromPageEntry(panelDesignForm);
        if (Op.isEmpty(displayPageNames)) {
            panelDesignForm.getString("面板名称");
            displayPageNames = CollUtil.newHashSet(panelName);
        }


        tableTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Table);


        // 默认行点击事件, 各个表格可以复用
        // 表单名称走的是默认的 PanelDesignCommonFormUtil.buildPanelFormName
        Form defaultTableRowClickEvent = PanelDesignCommonFormUtil
                .createDefaultTableRowClickEvent(dao, observer, panelDesignForm);

        AssociationData rowClickEventAc = null;
        if (defaultTableRowClickEvent != null) {
            LvUtil.trace(StrUtil.format("defaultTableRowClickEvent: {}",
                    defaultTableRowClickEvent.getString(Form.Code)));
            rowClickEventAc = Op.toAssociationData(defaultTableRowClickEvent);

        }

        for (String pageName : displayPageNames) {

            String tableName = StrUtil.format("{}", pageName);

            tableName = removeSpecialChar(tableName);

            Form form = newForm(tableTd.getFormModelId());

            form.setAttrValue("表格名称", tableName);


            TableData panelBtnTd = panelDesignForm.getTable("面板按钮");

            List<String> menuBtnNames = getButtonNamesByPanelButton(dao, panelBtnTd,
                    true, false, CollUtil.newHashSet("刷新", StrUtil.format("{}_新增", panelName), "删除"));

            List<String> rowBtnNames = getButtonNamesByPanelButton(dao, panelBtnTd,
                    true, false, CollUtil.newHashSet("删除"));


            if (CollUtil.isNotEmpty(menuBtnNames)) {
                form.setAttrValue("菜单", CollUtil.join(menuBtnNames, ","));
            }

            form.setAttrValue("操作列", CollUtil.join(rowBtnNames, ","));

            form.setAttrValue("列名", getDataAxisAttrNamesByPanelDesignForm(panelDesignForm));


            if (defaultTableRowClickEvent != null) {
                form.setAttrValue("事件集合",
                        CollUtil.newArrayList(rowClickEventAc)
                );

                TableData eventTd = new TableData(SlaveFormModelId_PanelDesign_Constraint_Event);
                Form slaveEventForm = Op.newForm(eventTd.getFormModelId());
                slaveEventForm.setAttrValue("事件实现", rowClickEventAc);

                eventTd.add(slaveEventForm);
                panelDesignForm.setAttrValue("面板事件", eventTd);
            }


            tableTd.add(form);


        }

        panelDesignForm.setAttrValue("面板表格", tableTd);

    }


    // ========================= 支撑方法 =========================


    private Form findWebPageByContentType(List<Form> rows, boolean isStandardConfig) throws Exception {
        if (Op.isEmpty(rows))
            return null;

        boolean isHtml = !isStandardConfig;
        boolean isJson = isStandardConfig;

        // 错误的情况下，优先使用html
        if (isHtml && isJson)
            isJson = false;
        if (!isHtml && !isJson)
            isHtml = true;

        for (Form row : rows) {
            String pageCode = row.getString("页面代码");
            if (StrUtil.isBlank(pageCode))
                continue;
            boolean hasHtml = pageCode.contains("/html>");
            if (isHtml && hasHtml) {
                return row;
            } else if (isJson && !hasHtml) {
                return row;
            }
        }
        return null;
    }


    // 移除特殊字符: "/" "\\" "|" ":" "*" "?" "\"" "<" ">" " " ";"  "&"
    private String removeSpecialChar(String tableName) {
        return StrUtil.removeAll(tableName, '/', '\\', '|', ':', '*', '?',
                '"', '<', '>', ' ', ';', '&');
    }


    // 从页面入口取出要展示的页面名称
    private Set<String> getDisplayPageNamesFromPageEntry(Form panelDesignForm) {
        try {
            String pageEntry = panelDesignForm.getString("页面入口");
            if (StrUtil.isBlank(pageEntry)) return new TreeSet<>();
            return new TreeSet<>(Arrays.asList(pageEntry.split(",")));
        } catch (Exception e) {
            return new TreeSet<>();
        }
    }


    private List<String> getButtonNamesByPanelButton(IDao dao, TableData btnTd, boolean useKeywordForGet, boolean useKeywordForIgnore, Set<String> keywords) throws Exception {
        List<String> resultSet = new ArrayList<>();
        if (btnTd == null || btnTd.isEmtpy()) return resultSet;

        for (Form row : btnTd.getRows()) {
            AssociationData btnImplAc = row.getAssociation("面板按钮");
            if (btnImplAc == null) continue;
            Form btnImplForm = IFormMgr.get().queryFormByCode(dao, btnImplAc.getFormModelId(),
                    btnImplAc.getValue());
            if (btnImplForm == null) continue;
            String btnName = btnImplForm.getString("按钮名称");
            if (StrUtil.isBlank(btnName)) continue;

            boolean shouldIgnore = false;
            if (useKeywordForIgnore && CollUtil.isNotEmpty(keywords)) {
                for (String keyword : keywords) {
                    if (btnName.contains(keyword)) {
                        shouldIgnore = true;
                        break;
                    }
                }
            }
            if (shouldIgnore) continue;

            if (useKeywordForGet && CollUtil.isNotEmpty(keywords)) {
                boolean matched = false;
                for (String keyword : keywords) {
                    if (btnName.contains(keyword)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) continue;
            }

            resultSet.add(btnName);
        }


        return resultSet;
    }


    // 追加文本到【需求实现】的描述中
    private void appendTextToPanelDescription(IDao dao, OctoDomainOpObserver observer,
                                              String sceneCode, String text) throws Exception {

        // FIXME 裴硕： 我不太想要这个东西了
        if(true) return;


        Form sceneForm = queryFormByAssignField(dao, FormModelId_SceneLayer, Form.Code, sceneCode);
        if (sceneForm == null) throw SceneException.Builder.notFoundWithCode(sceneCode);
        Form panelDesignForm = getOrCreatePanelDesignForm(dao, null, sceneForm);

        String panelDescription = panelDesignForm.getString("面板描述");
        if (StrUtil.isBlank(panelDescription)) panelDescription = "";

//        if (panelDescription.length() > MAX_PANEL_DESCRIPTION_LENGTH) {
        panelDescription = StrUtil.format("{}\n{}", panelDescription, text);
//        } else {
//            panelDescription = StrUtil.format("{}\n超出最大字符({})已忽略后续内容。"
//                    , panelDescription, MAX_PANEL_DESCRIPTION_LENGTH);
//
//        }


        panelDesignForm.setAttrValue("面板描述", panelDescription);

//        LvUtil.trace("appendTextToPanelDescription:" + panelDescription);
        IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);


    }

    // 更新字段值到需求实现Form
    private void updateFieldValueToPanelDesignForm(IDao dao, OctoDomainOpObserver observer,
                                                   String sceneCode, String fieldName, Object value) throws Exception {
        Form sceneForm = queryFormByAssignField(dao, FormModelId_SceneLayer, Form.Code, sceneCode);
        if (sceneForm == null) throw SceneException.Builder.notFoundWithCode(sceneCode);

        Form panelDesignForm = getOrCreatePanelDesignForm(dao, null, sceneForm);

        panelDesignForm.setAttrValue(fieldName, value);

        IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);
    }

    // 通过场景编号获取【需求实现】的Form
    private Form getOrCreatePanelDesignForm(IDao dao, OctoDomainOpObserver observer, String sceneCode) throws Exception {
        Form sceneForm = queryFormByAssignField(dao, FormModelId_SceneLayer, Form.Code, sceneCode);
        if (sceneForm == null) throw SceneException.Builder.notFoundWithCode(sceneCode);
        return getOrCreatePanelDesignForm(dao, observer, sceneForm);
    }

    // 通过场景Form获取【需求实现】的Form，
    private Form getOrCreatePanelDesignForm(IDao dao, OctoDomainOpObserver observer, Form sceneForm) throws Exception {

        boolean isCreate = false;

        Form panelDesignForm = queryFormByAssignField(dao, WorkBenchConst.FormModelId_PanelDesign,
                "所属场景", sceneForm.getString(Form.Code));

        if (panelDesignForm == null) {
            isCreate = true;
            panelDesignForm = createPanelDesignForm(dao, observer);

        } else {
            String panelCode = panelDesignForm.getString("面板编号");
            if (StrUtil.isNotBlank(panelCode) && panelCode.contains(LogicalDeleteFlag)) {
                panelDesignForm.setAttrValue("面板编号", panelCode.replace(LogicalDeleteFlag, ""));
                panelDesignForm.setAttrValue("面板描述", "");

            }
        }

        AssociationData sceneCategoryAc = sceneForm.getAssociation("场景类型");
        if (sceneCategoryAc == null) {
            sceneCategoryAc = PanelCategoryUtil.getInformationMgrCategoryAc();
        }

        panelDesignForm.setAttrValue("所属场景", toAssociationData(sceneForm));
        panelDesignForm.setAttrValue("面板分类", sceneCategoryAc);
        panelDesignForm.setAttrValue("面板角色", sceneForm.getAssociations("使用角色"));

        String panelDesignDescription = panelDesignForm.getString("面板描述");
        if (StrUtil.isBlank(panelDesignDescription)) {
            String panelDesignDescTemplate = "建设动机：{}\n业务目标：{}\n验证标准：{}\n触发条件：{}\n";
            String panelDesignDesc = StrUtil.format(panelDesignDescTemplate,
                    sceneForm.getString("建设动机"),
                    sceneForm.getString("业务目标"),
                    sceneForm.getString("验证标准"),
                    sceneForm.getString("触发条件")
            );

            panelDesignForm.setAttrValue("面板描述", panelDesignDesc);

        }

        panelDesignForm.setAttrValue("面板名称", sceneForm.getString("场景名称"));


        try {

            if (isCreate) {
                IFormMgr.get().createForm(null, dao, panelDesignForm, observer);
            } else {
                IFormMgr.get().updateForm(null, dao, panelDesignForm, observer);
            }
            dao.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return panelDesignForm;

    }

    private void deletePanelDesignByPanelCode(IDao dao, OctoDomainOpObserver observer, String customPanelCode) throws Exception {
        Cnd cnd = Cnd.NEW();
        cnd.where().andEquals(getFieldCode("面板编号"), customPanelCode);
        IFormMgr.get().deleteForm(null, dao, FormModelId_PanelDesign, cnd, observer);
    }

    private static synchronized Form createPanelDesignForm(IDao dao, OctoDomainOpObserver observer) throws Exception {

        String needsImplCode = PanelDesignCommonFormUtil.generateCode(dao, observer, FormModelId_PanelDesign, "IML", 5);

        Form needsImplForm = new Form(FormModelId_PanelDesign);
        needsImplForm.setUuid(IdUtil.fastUUID()).setAttrValue(Form.Code, needsImplForm.getUuid());
        needsImplForm.setAttrValue("面板编号", needsImplCode);
        return needsImplForm;

    }

    public Form queryFormByAssignField(IDao dao, String formModel, String fieldName, String fieldValue) throws Exception {
        return queryFormsByAssignField(dao, formModel, fieldName, fieldValue).stream()
                .findFirst()
                .orElse(null);
    }

    public List<AssociationData> queryAcsByAssignField(IDao dao, String formModel, String fieldName, String fieldValue) throws Exception {
        List<Form> forms = queryFormsByAssignField(dao, formModel, fieldName, fieldValue);
        if (CollUtil.isEmpty(forms)) return new ArrayList<>();
        List<AssociationData> acs = new ArrayList<>();
        for (Form form : forms) {
            acs.add(
                    toAssociationData(form)
            );
        }
        return acs;
    }

    public List<Form> queryFormsByAssignField(IDao dao, String formModel, String fieldName, String fieldValue) throws Exception {
        try {
            List<Form> resultList = new ArrayList<>();
            if (StrUtil.hasBlank(formModel, fieldName, fieldValue)) return resultList;
            Cnd cnd = Cnd.NEW();
            cnd.where().andEquals(getFieldCode(fieldName), fieldValue);
            ResultSet<Form> formRs = IFormMgr.get().queryFormPage(dao, formModel, cnd, 1, Integer.MAX_VALUE, true, true);
            if (formRs.isEmpty()) return resultList;
            return formRs.getDataList();
        } catch (Exception e) {
            LvUtil.trace(ExceptionUtils.getFullStackTrace(e));
            return new ArrayList<>();
        }
    }


    // 从面板设计中拿到所有的属性名称
    private String getDataAxisAttrNamesByPanelDesignForm(Form panelDesignForm) throws Exception {
        StringJoiner nameSj = new StringJoiner(",");
        TableData dataTd = panelDesignForm.getTable("面板数据");
        if (isEmpty(dataTd)) return nameSj.toString();

        for (Form dataForm : dataTd.getRows()) {
            String attrName = dataForm.getString("场景属性名称");
            nameSj.add(attrName);
        }
        return nameSj.toString();
    }


}
