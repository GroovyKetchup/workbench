package octo.cm.util;

import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;

import java.util.*;

import static octo.cm.constant.WorkBenchConst.*;

/**
 * 流程设计发布到面板设计的处理器（一次性实例化使用）。
 *
 * <p>区别于 {@link JsonToFormConversionUtil}：本类不做名称模糊匹配/复用兜底，
 * 完全信任前端按编号传递的角色/按钮/属性/权限/状态编号，按编号直查；查不到则按前端字段新建。</p>
 *
 * <p>数据覆盖语义：所有子表整表覆盖；关联列表整体替换；被取消引用的旧底层 Form 不做物理删除。</p>
 */
@Comment("  流程设计发布到面板设计的处理器")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-02-03", updateTime = "2026-02-03"
)
public class FlowToPanelDesignPublisher {

    private static final EasyOperation Op = EasyOperation.get();

    private final IDao dao;
    private final OctoDomainOpObserver observer;
    private final String modelType;
    private final Form flowDef;
    private final JSONObject src;

    /** 处理过程中得到的目标面板 Form */
    private Form panel;

    /** 本次发布是否走的是新建分支（包括前端给了关联面板但后台找不到的情况） */
    private boolean isNewPanel = false;

    /**
     * 本次新建分支创建出来的默认"新增"按钮 Form。
     * <p>必须缓存——因为 {@link PanelDesignCommonFormUtil#getOrCreateDefaultCreateButton} 内部
     * 会先 deleteForm 同名旧按钮再重建，如果重复调用第二次，第一次挂到面板按钮子表里的关联就指向了被删除的编号，落库会报
     * APP_DAT_0013-关联模型不存在该编号。</p>
     */
    private Form defaultCreateBtnForm;

    /** 本次处理过的角色实现：角色编号 -> Form */
    private final Map<String, Form> roleImplByCode = new HashMap<>();
    /** 本次处理过的角色实现：角色名称 -> Form（备用） */
    private final Map<String, Form> roleImplByName = new HashMap<>();

    public FlowToPanelDesignPublisher(IDao dao, OctoDomainOpObserver observer, String modelType, Form flowDef, JSONObject src) {
        this.dao = dao;
        this.observer = observer;
        this.modelType = modelType;
        this.flowDef = flowDef;
        this.src = src;
    }


    /**
     * 执行发布。
     *
     * @param existedPanelCode 已有的面板编号；为空或后台查无此面板时均走新建分支
     * @return 已落库的面板设计 Form
     */
    public Form publish(String existedPanelCode) throws Exception {
        locateOrCreatePanel(existedPanelCode);

        boolean isProcess = "流程".equals(this.modelType);

        fillTopFields();
        fillPanelRoles();
        fillPanelData();
        fillPanelButtons();
        ensureDefaultCreateButton();
        ensureSystemDefaultButtons();
        if (isProcess) {
            fillPanelStatus();
            fillBusOrchestration();
            fillPanelPermissions();
            ensureCreateBtnPermissionInDefaultStatus();

        }

        buildDefaultViews();

        panel = IFormMgr.get().updateForm(null, dao, panel, observer);
        dao.commit();
        return panel;
    }

    /** 本次是否走的是新建分支（含"关联面板找不到"的回退） */
    public boolean isNewPanel() {
        return isNewPanel;
    }


    // ========================= 各模块 =========================

    /** 定位或新建面板设计 Form：关联面板查无此记录时按用户要求回退到新建 */
    private void locateOrCreatePanel(String existedPanelCode) throws Exception {
        if (StrUtil.isNotBlank(existedPanelCode)) {


            Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_PanelDesign);
            cnd.where().andEquals(Op.getFieldCode("面板编号"), existedPanelCode);

            ResultSet<Form> pdRs = IFormMgr.get().queryFormPage(dao, FormModelId_PanelDesign, cnd, 1, 1, true, true);
//
//            Form existed = PanelDesignCommonFormUtil.queryFormByAssignField(
//                    observer, FormModelId_PanelDesign, "面板编号", existedPanelCode);
            if (!pdRs.isEmpty()) {
                this.panel = pdRs.getDataList().get(0);
                this.isNewPanel = false;
                return;
            }
            // 流程定义声明了关联面板，但后台不存在 → 回退到新建路径
            Op.logf("流程定义声明的关联面板[{}]不存在，回退到新建路径", existedPanelCode);
        }

        // 新建：复用 IPanelDesignService 的标准流程，会自动分配 IML_xxxxx、挂默认按钮、初始化属性实现
        Form shell = new Form(FormModelId_PanelDesign);

        // 设置依赖，到时候流程定义删除底下面板也一起联动删掉
        if (flowDef != null) {
            shell.setAttrValue(Form.Owner, flowDef.getUuid());

        }

        // 提前塞一下面板名称，便于服务端创建默认按钮（XX_新增）时使用真实名字
        String panelName = src.getStr("面板名称");
        if (StrUtil.isNotBlank(panelName)) shell.setAttrValue("面板名称", panelName);
        shell.setAttrValue("面板分类", PanelCategoryUtil.getProcessCategoryAc());

        this.panel = IPanelDesignService.get().createPanelDesign(observer, shell);
        this.isNewPanel = true;
    }


    /** 新建分支专用：把默认"新增"按钮挂到面板按钮子表，复用面板按钮工具 */
    private void ensureDefaultCreateButton() throws Exception {
        Form createBtn = obtainDefaultCreateBtn();
        if (createBtn == null) return;

        TableData td = panel.getTable("面板按钮");
        if (td == null) td = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);

        // 已存在则不重复挂载
        String createBtnUuid = createBtn.getUuid();
        String createBtnCode = createBtn.getString(Form.Code);
        for (Form row : td.getRows()) {
            AssociationData ac = row.getAssociation("面板按钮");
            if (ac == null) continue;
            String acVal = ac.getValue();
            if (acVal != null && (acVal.equals(createBtnUuid) || acVal.equals(createBtnCode))) return;
        }

        Form line = Op.newForm(td.getFormModelId());
        line.setAttrValue("按钮别名", createBtn.getString("别名"));
        line.setAttrValue("按钮说明", createBtn.getString("按钮说明"));
        line.setAttrValue("面板按钮", Op.toAssociationData(createBtn));
        td.add(line);

        panel.setAttrValue("面板按钮", td);
    }

    /**
     * 补充系统默认按钮（刷新/删除/保存/取消等）：查询系统中分类标签为"系统按钮"的预制按钮，
     * 对于面板按钮子表中尚未挂载的，按别名去重后补充挂上去。
     */
    private void ensureSystemDefaultButtons() throws Exception {
        SqlExpression domainFilterExpr = Op.getBusDomainFilterExpr(observer, FormModelId_Axis_Button);
        ResultSet<Form> systemBtnRs = Op.queryFormPageByCondition(dao, FormModelId_Axis_Button,
                FieldName_CategoryLabel, Text_SystemDefaultButton, cnd -> {
                    if (domainFilterExpr != null) {
                        cnd.where().and(domainFilterExpr);
                    }
                    return cnd;
                });
        if (systemBtnRs.isEmpty()) return;

        TableData td = panel.getTable("面板按钮");
        if (td == null) td = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);

        // 收集已有别名，避免重复挂载
        Set<String> existingAliases = new HashSet<>();
        for (Form row : td.getRows()) {
            String alias = row.getString("按钮别名");
            if (StrUtil.isNotBlank(alias)) existingAliases.add(alias);
        }

        for (Form systemBtn : systemBtnRs.getDataList()) {
            String btnAlias = systemBtn.getString("别名");
            if (StrUtil.isBlank(btnAlias)) continue;
            if (existingAliases.contains(btnAlias)) continue;

            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("按钮别名", btnAlias);
            line.setAttrValue("按钮说明", systemBtn.getString("按钮说明"));
            line.setAttrValue("面板按钮", Op.toAssociationData(systemBtn));
            td.add(line);
        }

        panel.setAttrValue("面板按钮", td);
    }

    /**
     * 新建分支专用：确保每个权限实现的[操作权限]子表里都包含"新增"按钮的一行，
     * 且"默认"状态位置写为可执行权限。
     */
    private void ensureCreateBtnPermissionInDefaultStatus() throws Exception {
        Form createBtn = obtainDefaultCreateBtn();
        if (createBtn == null) return;
        String createBtnName = createBtn.getString("按钮名称");
        if (StrUtil.isBlank(createBtnName)) return;

        TableData permTd = panel.getTable("面板权限");
        if (Op.isEmpty(permTd)) return;

        for (Form permLine : permTd.getRows()) {
            AssociationData permAc = permLine.getAssociation("权限实现");
            if (permAc == null) continue;
            Form permImpl = Op.queryFormByAc(dao, permAc);
            if (permImpl == null) continue;

            TableData opPermTd = permImpl.getTable("操作权限");
            if (opPermTd == null) opPermTd = new TableData(FormModelId_Axis_Permission_Button);

            // 已存在该按钮的行就跳过
            String referenceStatus = null;
            boolean exists = false;
            for (Form row : opPermTd.getRows()) {
                if (createBtnName.equals(row.getString("操作"))) {
                    exists = true;
                    break;
                }
                if (referenceStatus == null) referenceStatus = row.getString("状态");
            }
            if (exists) continue;

            // 状态字符串：复用同表其它行的格式，否则从面板状态构造（前置"默认"）
            String statusStr = StrUtil.isNotBlank(referenceStatus)
                    ? referenceStatus
                    : buildStatusStrWithDefault();
            if (StrUtil.isBlank(statusStr)) continue;

            // 权限字符串："默认"位置给可执行，其余位置无权限
            String[] statuses = statusStr.split(PermissionStatus_Delimiter);
            StringJoiner permJoiner = new StringJoiner(PermissionStatus_Delimiter);
            for (String s : statuses) {
                permJoiner.add(PanelDesignCommonFormUtil.DefaultPermissionType.equals(s.trim())
                        ? PermissionStatus_ReadAndExecute
                        : PermissionStatus_NoReadAndWriteAndExecute);
            }

            Form newRow = Op.newForm(opPermTd.getFormModelId());
            newRow.setAttrValue("操作", createBtnName);
            newRow.setAttrValue("状态", statusStr);
            newRow.setAttrValue("权限", permJoiner.toString());
            opPermTd.add(newRow);

            permImpl.setAttrValue("操作权限", opPermTd);
            IFormMgr.get().updateForm(null, dao, permImpl, observer);
        }
    }

    /**
     * 每次发布都完整重建[面板表格] + [面板表单] + [页面入口] + [面板事件](行点击)，Publisher 是唯一数据源。
     *
     * <p>命名/字段填充策略参考 {@code CPanelDesignService.initPanelDesignViewOrchestrationTable/Form}：
     * <ul>
     *   <li>表格名称 = 面板名称（去除特殊字符）</li>
     *   <li>表单名称 = {@code PanelDesignCommonFormUtil.buildPanelFormName(panelName)}</li>
     *   <li>列名/属性 = 面板数据子表里的全部"场景属性名称"</li>
     *   <li>表格菜单 = 面板按钮里名称包含 "刷新" / "&lt;面板名&gt;_新增" / "删除" 的</li>
     *   <li>表格操作列 = 面板按钮里名称包含 "删除" 的</li>
     *   <li>表单按钮 = 面板按钮里名称包含 "保存" / "取消" 的</li>
     *   <li>行点击 = 复用 {@code PanelDesignCommonFormUtil.createDefaultTableRowClickEvent}</li>
     *   <li>页面入口 = 表格名称</li>
     * </ul>
     */
    private void buildDefaultViews() throws Exception {
        String panelName = panel.getString("面板名称");
        if (StrUtil.isBlank(panelName)) return;

        // 列字段名串：从面板数据子表抽取场景属性名称
        String columnNamesStr = joinSceneAttrNames(panel.getTable("面板数据"));

        // 默认行点击事件（弹出编辑表单）
        Form rowClickEvent = PanelDesignCommonFormUtil.createDefaultTableRowClickEvent(dao, observer, panel);
        AssociationData rowClickEventAc = rowClickEvent != null ? Op.toAssociationData(rowClickEvent) : null;

        // ----- 默认表格 -----
        String tableName = removeSpecialChar(panelName);
        TableData tableTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Table);
        Form tableRow = Op.newForm(tableTd.getFormModelId());
        tableRow.setAttrValue("表格名称", tableName);
        tableRow.setAttrValue("列名", columnNamesStr);

        TableData panelBtnTd = panel.getTable("面板按钮");
        Set<String> menuKeywords = new HashSet<>();
        menuKeywords.add("刷新");
        menuKeywords.add(StrUtil.format("{}_新增", panelName));
        menuKeywords.add("删除");
        List<String> menuBtnNames = pickButtonNamesByKeyword(panelBtnTd, menuKeywords);

        Set<String> rowOpKeywords = new HashSet<>();
        rowOpKeywords.add("删除");
        List<String> rowBtnNames = pickButtonNamesByKeyword(panelBtnTd, rowOpKeywords);

        if (!menuBtnNames.isEmpty()) {
            tableRow.setAttrValue("菜单", CollUtil.join(menuBtnNames, ","));
        }
        tableRow.setAttrValue("操作列", CollUtil.join(rowBtnNames, ","));

        if (rowClickEventAc != null) {
            tableRow.setAttrValue("事件集合", CollUtil.newArrayList(rowClickEventAc));

            TableData eventTd = new TableData(SlaveFormModelId_PanelDesign_Constraint_Event);
            Form eventRow = Op.newForm(eventTd.getFormModelId());
            eventRow.setAttrValue("事件实现", rowClickEventAc);
            eventTd.add(eventRow);
            panel.setAttrValue("面板事件", eventTd);
        }
        tableTd.add(tableRow);
        panel.setAttrValue("面板表格", tableTd);

        // ----- 默认表单 -----
        TableData formTd = new TableData(SlaveFormModelId_PanelDesign_View_Orchestration_Form);
        Form formRow = Op.newForm(formTd.getFormModelId());
        formRow.setAttrValue("表单名称", PanelDesignCommonFormUtil.buildPanelFormName(panelName));
        formRow.setAttrValue("属性", columnNamesStr);
        formRow.setAttrValue("重置布局", true);

        Set<String> formBtnKeywords = new HashSet<>();
        formBtnKeywords.add("*");
        List<String> formBtnNames = pickButtonNamesByKeyword(panelBtnTd, formBtnKeywords);
        formRow.setAttrValue("按钮", CollUtil.join(formBtnNames, ","));
        formTd.add(formRow);
        panel.setAttrValue("面板表单", formTd);

        // ----- 页面入口 -----
        panel.setAttrValue("页面入口", tableName);
    }

    /**
     * 新建分支专用：获取/创建默认"新增"按钮，本次发布内只会真正建一次。
     * <p>必须走这个方法访问，不要直接调 {@link PanelDesignCommonFormUtil#getOrCreateDefaultCreateButton}，
     * 因为后者每次都会 delete 同名旧按钮再重建，重复调会使之前挂出去的关联变成"找不到的编号"。</p>
     */
    private Form obtainDefaultCreateBtn() throws Exception {
        if (defaultCreateBtnForm == null) {
            defaultCreateBtnForm = PanelDesignCommonFormUtil.getOrCreateDefaultCreateButton(dao, observer, panel);
        }
        return defaultCreateBtnForm;
    }

    /** 从[面板数据]子表抽取"场景属性名称"列，逗号拼接 */
    private String joinSceneAttrNames(TableData dataTd) throws Exception {
        StringJoiner sj = new StringJoiner(",");
        if (Op.isEmpty(dataTd)) return sj.toString();
        for (Form row : dataTd.getRows()) {
            String name = row.getString("场景属性名称");
            if (StrUtil.isNotBlank(name)) sj.add(name);
        }
        return sj.toString();
    }

    /** 按关键字（"按钮名称"包含即命中）从面板按钮子表挑出按钮名 */
    private List<String> pickButtonNamesByKeyword(TableData btnTd, Set<String> keywords) throws Exception {
        List<String> result = new ArrayList<>();
        if (Op.isEmpty(btnTd) || keywords == null || keywords.isEmpty()) return result;
        for (Form row : btnTd.getRows()) {
            AssociationData ac = row.getAssociation("面板按钮");
            if (ac == null) continue;
            Form btnImpl = IFormMgr.get().queryFormByCode(dao, ac.getFormModelId(), ac.getValue());
            if (btnImpl == null) continue;
            String btnName = btnImpl.getString("按钮名称");
            if (StrUtil.isBlank(btnName)) continue;
            for (String kw : keywords) {
                if ("*".equals(kw) || (StrUtil.isNotBlank(kw) && btnName.contains(kw))) {
                    result.add(btnName);
                    break;
                }
            }
        }
        return result;
    }

    /** 表格名称去除特殊字符（与现有面板设计规则保持一致） */
    private String removeSpecialChar(String s) {
        if (StrUtil.isBlank(s)) return s;
        return StrUtil.removeAll(s, '/', '\\', '|', ':', '*', '?', '"', '<', '>', ' ', ';', '&');
    }


    /** 从面板状态拼出 "默认;状态1;状态2;..." 的状态字段值 */
    private String buildStatusStrWithDefault() throws Exception {
        StringJoiner sj = new StringJoiner(PermissionStatus_Delimiter);
        sj.add(PanelDesignCommonFormUtil.DefaultPermissionType);

        AssociationData statusAc = panel.getAssociation("面板状态");
        if (statusAc != null) {
            Form statusForm = Op.queryFormByAc(dao, statusAc);
            if (statusForm != null) {
                TableData valTd = statusForm.getTable("状态值");
                if (!Op.isEmpty(valTd)) {
                    for (Form row : valTd.getRows()) {
                        String s = row.getString("状态");
                        if (StrUtil.isNotBlank(s)) sj.add(s);
                    }
                }
            }
        }
        return sj.toString();
    }


    /** 顶层字段：面板名称/面板描述/面板分类 */
    private void fillTopFields() throws Exception {
        boolean isProcess = "流程".equals(this.modelType);

        panel.setAttrValue("面板名称", src.getStr("面板名称"));
        panel.setAttrValue("面板描述", StrUtil.blankToDefault(src.getStr("面板描述"), ""));
        panel.setAttrValue("面板分类",
                isProcess ? PanelCategoryUtil.getProcessCategoryAc()
                        : PanelCategoryUtil.getInformationMgrCategoryAc()
        );
    }


    /** 面板角色：关联列表（直接挂在面板上，不是子表） */
    private void fillPanelRoles() throws Exception {
        roleImplByCode.clear();
        roleImplByName.clear();

        List<AssociationData> acs = new ArrayList<>();
        JSONArray arr = src.getJSONArray("面板角色");
        if (arr == null) {
            panel.setAttrValue("面板角色", acs);
            return;
        }

        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            String roleCode = row.getStr("角色编号");
            String roleName = row.getStr("角色名称");
            String orgMatch = row.getStr("组织匹配");

            if (StrUtil.isBlank(roleCode)) {
                // 前端约定一定会传，缺失视为脏数据
                throw new RuntimeException(StrUtil.format("面板角色[{}]缺少角色编号", roleName));
            }

            Form roleImpl = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_Axis_Role, "角色编号", roleCode);

            if (roleImpl == null) {
                roleImpl = Op.newForm(FormModelId_Axis_Role);
                roleImpl.setAttrValue("角色编号", roleCode);
                roleImpl.setAttrValue("角色名称", roleName);
                roleImpl.setAttrValue("组织匹配", orgMatch);
                roleImpl = IFormMgr.get().createForm(null, dao, roleImpl, observer);
            } else {
                roleImpl.setAttrValue("角色名称", roleName);
                roleImpl.setAttrValue("组织匹配", orgMatch);
                roleImpl = IFormMgr.get().updateForm(null, dao, roleImpl, observer);
            }

            roleImplByCode.put(roleCode, roleImpl);
            if (StrUtil.isNotBlank(roleName)) roleImplByName.put(roleName, roleImpl);

            acs.add(Op.toAssociationData(roleImpl));
        }

        panel.setAttrValue("面板角色", acs);
    }


    /** 面板数据：子表 + 属性实现（按属性编号查/建） */
    private void fillPanelData() throws Exception {
        TableData td = new TableData(SlaveFormModelId_PanelDesign_Data);

        JSONArray arr = src.getJSONArray("面板数据");
        if (arr == null) {
            panel.setAttrValue("面板数据", td);
            return;
        }

        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            JSONObject impl = row.getJSONObject("属性实现");

            Form attrImpl = resolveOrCreateAttrImpl(impl);

            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("场景属性名称", row.getStr("场景属性名称"));
            line.setAttrValue("场景属性样式", row.getStr("场景属性样式"));
            line.setAttrValue("属性别名", row.getStr("属性别名"));
            line.setAttrValue("是否必填", row.getBool("是否必填", false));
            line.setAttrValue("属性样式", row.getStr("属性样式"));
            line.setAttrValue("默认值", row.getStr("默认值"));
            line.setAttrValue("提示文字", row.getStr("提示文字"));
            if (attrImpl != null) {
                line.setAttrValue("属性实现", Op.toAssociationData(attrImpl));
            }
            td.add(line);
        }

        panel.setAttrValue("面板数据", td);
    }

    private Form resolveOrCreateAttrImpl(JSONObject impl) throws Exception {
        if (impl == null) return null;
        String attrCode = impl.getStr("属性编号");
        String attrName = impl.getStr("属性名称");

        Form attrImpl = null;
        if (StrUtil.isNotBlank(attrCode)) {
            attrImpl = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_Axis_Data, "属性编号", attrCode);
        }
        if (attrImpl == null && StrUtil.isNotBlank(attrName)) {
            // 前端在过渡期可能没传属性编号，按名称兜底一次（不创建副本）
            attrImpl = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_Axis_Data, "属性名称", attrName);
        }

        if (attrImpl == null) {
            attrImpl = Op.newForm(FormModelId_Axis_Data);
            attrImpl.setAttrValue(Form.Owner, panel.getUuid());
            attrImpl.setAttrValue("属性编号", StrUtil.blankToDefault(attrCode, ""));
            attrImpl.setAttrValue("属性名称", attrName);
            attrImpl.setAttrValue("属性别名", impl.getStr("属性别名"));
            attrImpl.setAttrValue("是否必填", impl.getBool("是否必填", false));
            attrImpl.setAttrValue("属性样式", impl.getStr("属性样式"));
            attrImpl.setAttrValue("默认值", impl.getStr("默认值"));
            attrImpl.setAttrValue("提示文字", impl.getStr("提示文字"));
            attrImpl = IFormMgr.get().createForm(null, dao, attrImpl, observer);
        } else {
            attrImpl.setAttrValue("属性名称", attrName);
            attrImpl.setAttrValue("属性别名", impl.getStr("属性别名"));
            attrImpl.setAttrValue("是否必填", impl.getBool("是否必填", false));
            attrImpl.setAttrValue("属性样式", impl.getStr("属性样式"));
            attrImpl.setAttrValue("默认值", impl.getStr("默认值"));
            attrImpl.setAttrValue("提示文字", impl.getStr("提示文字"));
            attrImpl = IFormMgr.get().updateForm(null, dao, attrImpl, observer);
        }
        return attrImpl;
    }


    /** 面板按钮：子表 + 按钮实现（按按钮编号查/建，含按钮动作子表，允许动作为空） */
    private void fillPanelButtons() throws Exception {
        TableData td = new TableData(SlaveFormModelId_PanelDesign_Behavior_Button);

        JSONArray arr = src.getJSONArray("面板按钮");
        if (arr == null) {
            panel.setAttrValue("面板按钮", td);
            return;
        }

        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            JSONObject impl = row.getJSONObject("面板按钮");
            Form btnImpl = resolveOrCreateButtonImpl(impl);

            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("按钮别名", row.getStr("按钮别名"));
            line.setAttrValue("按钮说明", row.getStr("按钮说明"));
            if (btnImpl != null) {
                line.setAttrValue("面板按钮", Op.toAssociationData(btnImpl));
            }
            td.add(line);
        }

        panel.setAttrValue("面板按钮", td);
    }

    private Form resolveOrCreateButtonImpl(JSONObject impl) throws Exception {
        if (impl == null) return null;
        String btnCode = impl.getStr("按钮编号");
        String btnName = impl.getStr("按钮名称");

        Form btnImpl = null;
        if (StrUtil.isNotBlank(btnCode)) {
            btnImpl = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_Axis_Button, "按钮编号", btnCode);
        }

        TableData actionTd = buildBtnActionTd(impl.getJSONArray("按钮动作"));

        if (btnImpl == null) {
            btnImpl = Op.newForm(FormModelId_Axis_Button);
            btnImpl.setAttrValue(Form.Owner, panel.getUuid());
            btnImpl.setAttrValue("按钮编号", StrUtil.blankToDefault(btnCode, ""));
            btnImpl.setAttrValue("按钮名称", btnName);
            btnImpl.setAttrValue("别名", impl.getStr("别名"));
            btnImpl.setAttrValue("按钮说明", impl.getStr("按钮说明"));
            btnImpl.setAttrValue("分类标签", impl.getStr("分类标签"));
            btnImpl.setAttrValue("按钮动作", actionTd);
            btnImpl = IFormMgr.get().createForm(null, dao, btnImpl, observer);
        } else {
            btnImpl.setAttrValue("按钮名称", btnName);
            btnImpl.setAttrValue("别名", impl.getStr("别名"));
            btnImpl.setAttrValue("按钮说明", impl.getStr("按钮说明"));
            btnImpl.setAttrValue("分类标签", impl.getStr("分类标签"));
            btnImpl.setAttrValue("按钮动作", actionTd);
            btnImpl = IFormMgr.get().updateForm(null, dao, btnImpl, observer);
        }
        return btnImpl;
    }

    private TableData buildBtnActionTd(JSONArray actions) throws Exception {
        TableData td = new TableData(FormModelId_PanelDesign_Action_Orchestration);
        if (actions != null) {
            for (Object o : actions) {
                JSONObject a = (JSONObject) o;
                Form action = new Form(td.getFormModelId());
                action.setAttrValue("操作函数", a.getStr("操作函数"));
                action.setAttrValue("操作说明", a.getStr("操作说明"));
                td.add(action);
            }
        }
        // 用户未配置动作时，默认给一个「表单保存()」
        if (Op.isEmpty(td)) {
            Form action = new Form(td.getFormModelId());
            action.setAttrValue("操作函数", "表单保存()");
            td.add(action);
        }
        return td;
    }


    /** 面板状态：单关联（含状态值子表） */
    private void fillPanelStatus() throws Exception {
        JSONObject statusJson = src.getJSONObject("面板状态");
        if (statusJson == null) return;

        String statusCode = statusJson.getStr("状态编号");
        String statusName = statusJson.getStr("状态名称");

        Form statusForm = null;
        if (StrUtil.isNotBlank(statusCode)) {
            statusForm = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_PanelDesign_Status, "状态编号", statusCode);
        }

        TableData valueTd = new TableData(SlaveFormModelId_PanelDesign_Status_value);
        JSONArray values = statusJson.getJSONArray("状态值");
        if (values != null) {
            for (Object o : values) {
                JSONObject v = (JSONObject) o;
                Form vRow = Op.newForm(valueTd.getFormModelId());
                vRow.setAttrValue("状态", v.getStr("状态"));
                valueTd.add(vRow);
            }
        }

        if (statusForm == null) {
            statusForm = Op.newForm(FormModelId_PanelDesign_Status);
            statusForm.setAttrValue(Form.Owner, panel.getUuid());
            statusForm.setAttrValue("状态编号", StrUtil.blankToDefault(statusCode, ""));
            statusForm.setAttrValue("状态名称", statusName);
            statusForm.setAttrValue("状态值", valueTd);
            statusForm = IFormMgr.get().createForm(null, dao, statusForm, observer);
        } else {
            statusForm.setAttrValue("状态名称", statusName);
            statusForm.setAttrValue("状态值", valueTd);
            statusForm = IFormMgr.get().updateForm(null, dao, statusForm, observer);
        }

        panel.setAttrValue("面板状态", Op.toAssociationData(statusForm));
    }


    /** 业务编排：纯文本子表，无关联 */
    private void fillBusOrchestration() throws Exception {
        TableData td = new TableData(SlaveFormModelId_PanelDesign_Bus_Orchestration);

        JSONArray arr = src.getJSONArray("业务编排");
        if (arr == null) {
            panel.setAttrValue("业务编排", td);
            return;
        }

        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("开始节点", row.getStr("开始节点"));
            line.setAttrValue("操作按钮", row.getStr("操作按钮"));
            line.setAttrValue("下游节点", row.getStr("下游节点"));
            line.setAttrValue("进入规则", row.getStr("进入规则"));
            line.setAttrValue("离开规则", row.getStr("离开规则"));
            td.add(line);
        }

        panel.setAttrValue("业务编排", td);
    }


    /** 面板权限：子表 + 权限实现（含数据权限/操作权限两个内嵌子表） */
    private void fillPanelPermissions() throws Exception {
        TableData td = new TableData(SlaveFormModelId_PanelDesign_Constraint_Permission);

        JSONArray arr = src.getJSONArray("面板权限");
        if (arr == null) {
            panel.setAttrValue("面板权限", td);
            return;
        }

        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            JSONObject impl = row.getJSONObject("权限实现");
            Form permImpl = resolveOrCreatePermissionImpl(impl);

            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("约束名称", row.getStr("约束名称"));
            if (permImpl != null) {
                line.setAttrValue("权限实现", Op.toAssociationData(permImpl));
            }
            td.add(line);
        }

        panel.setAttrValue("面板权限", td);
    }

    private Form resolveOrCreatePermissionImpl(JSONObject impl) throws Exception {
        if (impl == null) return null;
        String permCode = impl.getStr("权限编号");
        String permName = impl.getStr("权限名称");

        Form permImpl = null;
        if (StrUtil.isNotBlank(permCode)) {
            permImpl = PanelDesignCommonFormUtil.queryFormByAssignField(
                    observer, FormModelId_Axis_Permission, "权限编号", permCode);
        }

        // 权限角色：前端按约定传角色编号数组
        List<AssociationData> roleAcs = resolvePermissionRoleAcs(impl.getJSONArray("权限角色"));

        TableData dataPermTd = buildDataPermissionTd(impl.getJSONArray("数据权限"));
        TableData btnPermTd = buildBtnPermissionTd(impl.getJSONArray("操作权限"));

        if (permImpl == null) {
            permImpl = Op.newForm(FormModelId_Axis_Permission);
            permImpl.setAttrValue(Form.Owner, panel.getUuid());
            permImpl.setAttrValue("权限编号", StrUtil.blankToDefault(permCode, ""));
            permImpl.setAttrValue("权限名称", permName);
            permImpl.setAttrValue("权限角色", roleAcs);
            permImpl.setAttrValue("数据权限", dataPermTd);
            permImpl.setAttrValue("操作权限", btnPermTd);
            permImpl = IFormMgr.get().createForm(null, dao, permImpl, observer);
        } else {
            permImpl.setAttrValue("权限名称", permName);
            permImpl.setAttrValue("权限角色", roleAcs);
            permImpl.setAttrValue("数据权限", dataPermTd);
            permImpl.setAttrValue("操作权限", btnPermTd);
            permImpl = IFormMgr.get().updateForm(null, dao, permImpl, observer);
        }
        return permImpl;
    }

    /** 解析权限实现.权限角色：前端传角色编号数组 */
    private List<AssociationData> resolvePermissionRoleAcs(JSONArray roleCodeArr) throws Exception {
        List<AssociationData> acs = new ArrayList<>();
        if (roleCodeArr == null) return acs;

        for (Object o : roleCodeArr) {
            String roleKey = (o == null) ? null : o.toString();
            if (StrUtil.isBlank(roleKey)) continue;

            // 优先按编号取本次发布刚处理过的角色，避免再查库
            Form roleForm = roleImplByCode.get(roleKey);

            // 兼容：万一前端在过渡期仍传了角色名称
            if (roleForm == null) roleForm = roleImplByName.get(roleKey);

            // 兜底：跨面板复用的角色，本次没处理到，去库里查
            if (roleForm == null) {
                roleForm = PanelDesignCommonFormUtil.queryFormByAssignField(
                        observer, FormModelId_Axis_Role, "角色编号", roleKey);
            }

            if (roleForm == null) {
                throw new RuntimeException(StrUtil.format(
                        "权限实现引用的角色[{}]不存在，请确认面板角色已正确配置", roleKey));
            }
            acs.add(Op.toAssociationData(roleForm));
        }
        return acs;
    }

    private TableData buildDataPermissionTd(JSONArray arr) throws Exception {
        TableData td = new TableData(FormModelId_Axis_Permission_Data);
        if (arr == null) return td;
        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("属性", row.getStr("属性"));
            line.setAttrValue("状态", row.getStr("状态"));
            line.setAttrValue("权限", row.getStr("权限"));
            td.add(line);
        }
        return td;
    }

    private TableData buildBtnPermissionTd(JSONArray arr) throws Exception {
        TableData td = new TableData(FormModelId_Axis_Permission_Button);
        if (arr == null) return td;
        for (Object o : arr) {
            JSONObject row = (JSONObject) o;
            Form line = Op.newForm(td.getFormModelId());
            line.setAttrValue("操作", row.getStr("操作"));
            line.setAttrValue("状态", row.getStr("状态"));
            line.setAttrValue("权限", row.getStr("权限"));
            td.add(line);
        }
        return td;
    }

}
