package octo.cm.util;

import cell.cdao.IDao;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import static octo.cm.constant.WorkBenchConst.*;

/**
 * 流程设计发布到面板设计的处理器（一次性实例化使用）。
 *
 * <p>本类继承 {@link AbstractToPanelDesignPublisher}，复用其与定义类别无关的面板物化逻辑
 * （locateOrCreatePanel / fillTopFields / fillPanelRoles / fillPanelData / fillPanelButtons /
 * ensureDefaultCreateButton / ensureSystemDefaultButtons / buildDefaultViews 等），
 * 本类仅保留“流程”特有的面板状态、业务编排、面板权限处理。</p>
 *
 * <p>区别于 {@code JsonToFormConversionUtil}：本类不做名称模糊匹配/复用兜底，
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
public class FlowToPanelDesignPublisher extends AbstractToPanelDesignPublisher {

    /**
     * 构造流程发布器。
     *
     * @param dao       数据访问会话
     * @param observer  业务域观察者
     * @param modelType 模型类别（“流程”等）
     * @param flowDef   流程定义 Form（其 uuid 写入目标面板 Owner）
     * @param src       发布源数据（前端映射过来的面板 JSON）
     */
    public FlowToPanelDesignPublisher(IDao dao, OctoDomainOpObserver observer, String modelType, Form flowDef, JSONObject src) {
        super(dao, observer, modelType, flowDef, src);
    }

    /**
     * 流程面板分类：模型类别为“流程”时取“流程处理”，否则取“信息管理”
     * （与抽取前 {@code fillTopFields} 的判定保持一致）。
     *
     * @return 面板分类关联值
     */
    @Override
    protected AssociationData getPanelCategoryAc() throws Exception {
        return "流程".equals(this.modelType)
                ? PanelCategoryUtil.getProcessCategoryAc()
                : PanelCategoryUtil.getInformationMgrCategoryAc();
    }

    /**
     * 流程新建面板 shell 的分类：始终“流程处理”，与抽取前 {@code locateOrCreatePanel} 的硬编码完全一致，
     * 保证重构对流程发布零行为变化（最终分类仍由 {@link #getPanelCategoryAc()} 决定）。
     *
     * @return 面板分类关联值
     */
    @Override
    protected AssociationData getNewPanelShellCategoryAc() throws Exception {
        return PanelCategoryUtil.getProcessCategoryAc();
    }

    /**
     * 执行发布。
     *
     * @param existedPanelCode 已有的面板编号；为空或后台查无此面板时均走新建分支
     * @return 已落库的面板设计 Form
     * @throws Exception 发布失败
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

    // ========================= 流程特有模块 =========================

    /**
     * 新建分支专用：确保每个权限实现的[操作权限]子表里都包含“新增”按钮的一行，
     * 且“默认”状态位置写为可执行权限。
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

            // 状态字符串：复用同表其它行的格式，否则从面板状态构造（前置“默认”）
            String statusStr = StrUtil.isNotBlank(referenceStatus)
                    ? referenceStatus
                    : buildStatusStrWithDefault();
            if (StrUtil.isBlank(statusStr)) continue;

            // 权限字符串：“默认”位置给可执行，其余位置无权限
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
