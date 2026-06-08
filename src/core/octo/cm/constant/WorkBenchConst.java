package octo.cm.constant;

import cmn.anotation.ClassDeclare;
import org.nutz.dao.entity.annotation.Comment;

@Comment("OctoCM工作台常量类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-06-13", updateTime = "2025-06-13"
)
public class WorkBenchConst {

    // ========================= 基础 =========================
    public static final String FormModelId_FieldStyle = "octocm.md.workbench.shu3Sing4Yang4Shih4";
    public static final String FormModelId_OperateFunction = "gpf.md.basic.CaculateValueRuleDo";
    public static final String FormModelId_Params = "octocm.md.workbench.can1Shu4Guan3Li3CM";

    // ========================= 面板设计 =========================

    // 面板设计器
    public static final String FormModelId_PanelDesign = "octocm.md.workbench.mian4Ban3She4Ji4Ci4";
    // 面板-事件
    public static final String FormModelId_PanelDesign_Event = "octocm.md.workbench.mian4Ban3Shih4Jian4";
    // 面板-状态
    public static final String FormModelId_PanelDesign_Status = "octocm.md.workbench.mian4Ban3Jhuang4Tai4";

    // 面板-分类
    public static final String FormModelId_PanelDesign_Category = "octocm.md.workbench.mian4Ban3Fen1Lei4";

    // 面板-状态-值（TableData）
    public static final String SlaveFormModelId_PanelDesign_Status_value = "octocm.md.workbench.slave.PanelStatusValue";

    public static final String SlaveFormModelId_PanelDesign_Data = "octocm.md.workbench.slave.PanelDesignerData";
    public static final String SlaveFormModelId_PanelDesign_Behavior_Button = "octocm.md.workbench.slave.PanelDesignerAction";
    public static final String SlaveFormModelId_PanelDesign_Constraint_Permission = "octocm.md.workbench.slave.PanelDesignerPermission";
    public static final String SlaveFormModelId_PanelDesign_Constraint_Event = "octocm.md.workbench.slave.PanelDesignerEvent";
    public static final String SlaveFormModelId_PanelDesign_View_Orchestration_Table = "octocm.md.workbench.slave.PanelDesignerTableFM";
    public static final String SlaveFormModelId_PanelDesign_View_Orchestration_Form = "octocm.md.workbench.slave.PanelDesignerFormFM";
    public static final String SlaveFormModelId_PanelDesign_View_Orchestration_WebPage = "octocm.md.workbench.slave.PanelDesignerUDFFM";
    public static final String SlaveFormModelId_PanelDesign_View_Orchestration_Card = "octocm.md.workbench.slave.PanelDesignerCardFM";
    public static final String SlaveFormModelId_PanelDesign_Bus_Orchestration = "octocm.md.workbench.slave.PanelDesignerFlow";


    // 角色定义
    public static final String FormModelId_RoleDefine = "octocm.md.workbench.mian4Ban3Jiao3Se4";

    // 似乎废弃了


    // ========================= 需求实现-轴 =========================
    public static final String FormModelId_Axis_Data = "octocm.md.workbench.mian4Ban3Shu4Jyu4";
    public static final String FormModelId_Axis_Button = "octocm.md.workbench.mian4Ban3An4Niou3";
    public static final String FormModelId_Axis_Role = "octocm.md.workbench.mian4Ban3Jiao3Se4";
    public static final String FormModelId_Axis_Permission = "octocm.md.workbench.mian4Ban3Cyuan2Sian4";
    public static final String FormModelId_Axis_Permission_Data = "octocm.md.workbench.slave.PanelPermissionData";
    public static final String FormModelId_Axis_Permission_Button = "octocm.md.workbench.slave.PanelPermissioAction";
    public static final String FormModelId_PanelDesign_Action_Orchestration = "octocm.md.workbench.slave.PanelActionFlow";

    // 似乎废弃了
    public static final String FormModelId_Axis_Rule = "octocm.md.panel.WK_AM_0007";
    public static final String FormModelId_Axis_Event = "octocm.md.workbench.mian4Ban3Shih4Jian4";
    public static final String FormModelId_Axis_Timing = "octocm.md.panel.WK_AM_0005";


    // 系统层
    public static final String FormModelId_SystemLayer = "octocm.md.workbench.si4Tong3Ye4Wu4Syu1Ciou2";
    // 模块层
    public static final String FormModelId_SubGoalLayer = "octocm.md.workbench.mo2Kuai4Ye4Wu4Syu1Ciou2";
    // 场景层
    public static final String FormModelId_SceneLayer = "octocm.md.workbench.chang3Jing3Ye4Wu4Syu1Ciou2";

    // 场景层的实现
    public static final String FormModelId_SceneLayer_Data = "octocm.md.workbench.chang3Jing3Shu4Jyu4Ye4Wu4Syu1Ciou2";
    public static final String FormModelId_SceneLayer_Behavior = "octocm.md.workbench.chang3Jing3Sing2Wei2Ye4Wu4Syu1Ciou2";
    public static final String FormModelId_SceneLayer_Constraint = "octocm.md.workbench.chang3Jing3Yue1Shu4Ye4Wu4Syu1Ciou2";
    public static final String FormModelId_SceneLayer_Orchestration = "octocm.md.workbench.chang3Jing3Bian1Pai2Ye4Wu4Syu1Ciou2";
    public static final String FormModelId_SceneLayer_Display = "octocm.md.workbench.chang3Jing3Jhan3Shih4Ye4Wu4Syu1Ciou2";
    public static final String FormModelId_SceneLayer_Verification = "octocm.md.workbench.chang3Jing3Yan4Jheng4Ye4Wu4Syu1Ciou2";

    // 视图-表单编号
    public static final String FormViewCode_PanelDesign = "实现_面板设计_实现_面板设计_表单";


    public static final String LAYER_NAME_SCENE_IMPL = "SCENE_IMPL";
    public static final String LAYER_NAME_SCENE = "SCENE";
    public static final String LAYER_NAME_SUBGOAL = "SUBGOAL";
    public static final String LAYER_NAME_SYSTEM = "SYSTEM";

    // 系统业务域前缀
    public static final String SystemDomain_Prefix = "OctoCM_System";

    // EXCEL模板
    public static final String EXCEL_TEMPLATE_OCTOCM_PANEL_DESIGN = "template/TEMPLATE_OCTOCM_PANEL_DESIGN.xlsx";


    // 标志位
    public static final String SIGN_QUERY_SELF = "querySelf";
    public static final String PermissionStatus_NoReadAndWriteAndExecute = "N";
    public static final String PermissionStatus_Execute = "X";
    public static final String PermissionStatus_Read = "R";
    public static final String PermissionStatus_Write = "W";
    public static final String PermissionStatus_ReadAndWrite = "R,W";
    public static final String PermissionStatus_ReadAndExecute = "R,X";
    public static final String PermissionStatus_Delimiter = ";";

    public static final String Text_SystemDefaultButton = "系统按钮";
    public static final String FieldName_CategoryLabel = "按钮说明";
    public static final String Progress_FLAG_WorkBench = "[WorkBench]";


    // ========================= 参数 =========================
    public static final String ParamKey_DefaultPublishApplication = "当前业务域默认应用";
    // 业务域-UUID
    public static final String ParamKey_BUS_DOMAIN_UUID = "domainUuid";
    // 业务域-编号
    public static final String ParamKey_BUS_DOMAIN_CODE = "domainCode";


    // ========================= 面板缓存Key =========================

    // 用户指定的业务域（目前用于显示意图确认界面，提供给AgentForge）
    public static final String PanelCtxKey_UserAssignBusDomainCode = "UserAssignBusDomainCode";


    // ========================= 默认面板的CDP配置 =========================

    // 默认登录页
    public static final String DefaultPanelDesign_DefaultLoginPage = "cdp/html/DefaultLoginPage.html";

    // 默认工作台配置
    public static final String DefaultPanelDesign_DefaultDashboard = "cdp/config/DefaultDashboard.json";

    // 默认组织架构配置
    public static final String DefaultPanelDesign_OrganizationManagement = "cdp/config/OrganizationManagement.json";

    // 自定义Shell
    public static final String DefaultPanelDesign_CustomShell = "cdp/html/CustomShell.html";

    // 常见的变量名，默认面板的CDP配置中会用到
    public static final String Variable_Name_BusDomainCode = "{{BUS_DOMAIN_CODE}}";



}
