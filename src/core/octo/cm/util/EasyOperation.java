package octo.cm.util;

import cell.cdao.IDao;
import cell.cmn.role.IRoleService;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.adur.role.IRoleMgr;
import cell.gpf.adur.user.IUserMgr;
import cell.octocm.domain.service.IDomainService;
import cell.rapidView.function.CommonFunctions;
import cell.rapidView.function.NoExceptionFunction;
import cmn.anotation.ClassDeclare;
import cmn.dto.Progress;
import cmn.user.dto.UserDto;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.common.util.ProgressCtrl.crpc.IProgressUtil;
import com.leavay.common.util.ToolUtilities;
import com.leavay.common.util.javac.ClassFactory;
import com.leavay.dfc.gui.LvUtil;
import fe.cmn.callbackWidget.popWidget.DialogDto;
import fe.cmn.callbackWidget.popWidget.PopWidgetDto;
import fe.cmn.panel.PanelContext;
import fe.cmn.panel.PanelValue;
import fe.cmn.panel.SinglePanelDto;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.widget.EscapeButtonDto;
import fe.cmn.widget.LabelDto;
import fe.cmn.widget.SizeDto;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.Password;
import gpf.adur.data.ResultSet;
import gpf.adur.role.Org;
import gpf.adur.role.Role;
import gpf.adur.user.User;
import gpf.adur.user.UserGender;
import gpf.adur.user.UserStatus;
import gpf.dc.basic.fe.component.app.AppCacheUtil;
import gpf.dc.basic.fe.component.param.BaseDataViewParam;
import gpf.dc.basic.fe.component.view.AbsFormView;
import gpf.dc.basic.intf.AppDefaultFilterIntf;
import gpf.dc.intf.FormOpObserver;
import octo.cm.exception.business.UserException;
import octocm.domain.dto.DomainDto;
import octocm.domain.filter.OctoDomainDataFilter;
import octocm.domain.observer.OctoDomainOpObserver;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;
import org.nutz.dao.util.cri.SqlExpressionGroup;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Comment("便捷操作")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-04", updateTime = "2025-08-29"
)
public class EasyOperation implements CommonFunctions {

    public static final String ViewCode_PdcFormView = "gpf.md.udf.view.PDCFormView";


    private static final EasyOperation self = new EasyOperation();

    public static EasyOperation get() {
        return self;
    }


    @Override
    public Form newForm(String formModel) throws Exception {
        String uuid = IdUtil.fastSimpleUUID();
        Form form = new Form(formModel);
        return form.setUuid(uuid).setAttrValue(Form.Code, uuid);
    }

    public boolean isEmpty(Set set) {
        return set == null || set.isEmpty();
    }

    // 根据业务域编号查询
    public OctoDomainOpObserver getOctoDomainOpObserver(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) return null;
        DomainDto domain = IDomainService.get().getDomainByCode(busDomainCode);
        if (domain == null) return null;
        return new OctoDomainOpObserver(domain.getDomainCode(), domain.getDomainUuid());

    }

    // 获取Octo业务域Observer
    public OctoDomainOpObserver getOctoDomainOpObserver(PanelContext panelContext) throws Exception {
        FormOpObserver baseObserver = AppCacheUtil.getFormOpObserver(panelContext);
        if (!(baseObserver instanceof OctoDomainOpObserver)) {
            return null;
        }
        return (OctoDomainOpObserver) baseObserver;
    }

    // 便捷-获取业务域过滤条件
    public Cnd getBusDomainFilterCondition(OctoDomainOpObserver observer, String opFunctionModelId) throws Exception {
        SqlExpression expr = getBusDomainFilterExpr(observer, opFunctionModelId);
        if (expr == null) return null;
        return Cnd.where(expr);
    }

    // 获取业务域过滤表达式
    public SqlExpression getBusDomainFilterExpr(OctoDomainOpObserver observer, String targetFormModelId) throws Exception {
        if (observer == null) return null;
        return getBusDomainFilterExpr(observer.getDomainUuid(), observer.getDomainCode(), targetFormModelId);
    }

    // 获取业务域过滤表达式
    public SqlExpression getBusDomainFilterExpr(String domainUuid, String domainCode, String targetFormModelId) throws Exception {
        if (StrUtil.hasBlank(domainUuid, domainCode, targetFormModelId)) return null;

        if (StrUtil.hasBlank(domainUuid, domainCode)) return null;
        OctoDomainDataFilter octoDomainDataFilter = new OctoDomainDataFilter(domainCode, domainUuid);

        return octoDomainDataFilter.buildInDomainCondition(targetFormModelId, true);

    }


    // 解析字符串到JSONObject
    public JSONObject parseJsonObject(String jsonStr) {
        // 先智能的做一些预处理，比如可能JSON下面会有乱七八糟的脏东西
        // 使用IndexOf取出第一个{的位置，移除掉之前的字符，然后LastIndexOf取出最后一个}，移除掉之后的字符
        if (StrUtil.isBlank(jsonStr)) return null;
        try {
            String s = jsonStr;
            // 去除BOM与首尾空白
            s = s.replace("\uFEFF", "").trim();

            // 处理代码块围栏 ``` 和 ```json
            if (s.startsWith("```")) {
                int startFence = s.indexOf("```");
                int endFence = s.indexOf("```", startFence + 3);
                if (endFence > startFence) {
                    s = s.substring(startFence + 3, endFence).trim();
                } else {
                    // 若未找到成对围栏，去掉所有反引号
                    s = s.replace("```", "");
                }
            } else {
                // 非围栏，仍清理潜在的多余反引号
                s = s.replace("```", "");
            }

            // 若前缀为 json / JSON 语言标识，去除
            if (s.regionMatches(true, 0, "json", 0, Math.min(4, s.length()))) {
                s = s.substring(Math.min(4, s.length())).trim();
            }

            // 截取最外层花括号包裹的内容
            int first = s.indexOf('{');
            int last = s.lastIndexOf('}');
            if (first >= 0 && last >= first) {
                s = s.substring(first, last + 1);
            }

            // 再次trim，以防截取后仍有空白
            s = s.trim();

            return JSONUtil.parseObj(s);
        } catch (Exception e) {
            logException(e);
            return null;
        }
    }

    // 根据关联属性获取Form
    public Form queryFormByAc(IDao dao, AssociationData ac) throws Exception {
        if (ac == null) return null;
        String formModelId = ac.getFormModelId();
        String value = ac.getValue();
        if (StrUtil.hasBlank(formModelId, value)) return null;
        return IFormMgr.get().queryFormByCode(dao, formModelId, value);
    }


    // 相同的值匹配到任意指定的字段
    public Form queryFormByValueMatchAnyField(IDao dao, String formModel,
                                              Set<String> fieldNames, String fieldValue, NoExceptionFunction<Cnd, Cnd> extra) throws Exception {
        try {
            if (StrUtil.hasBlank(formModel, fieldValue)) return null;
            if (CollUtil.isEmpty(fieldNames)) return null;
            Cnd cnd = Cnd.NEW();
            SqlExpressionGroup matchAnyField = new SqlExpressionGroup();
            for (String fieldName : fieldNames) {
                matchAnyField.orEquals(getFieldCode(fieldName), fieldValue);
            }
            cnd.where().and(matchAnyField);

            if (extra != null) {
                cnd = extra.run(cnd);
            }

            ResultSet<Form> formRs = IFormMgr.get().queryFormPage(dao, formModel, cnd, 1, 1, true, true);
            if (formRs.isEmpty()) return null;
            return formRs.getDataList().get(0);
        } catch (Exception e) {
            LvUtil.trace(ExceptionUtils.getFullStackTrace(e));
            return null;
        }
    }

    // 根据工作空间进行过滤的语句
    public SqlExpression getWorkSpaceFilter(PanelContext panelContext, String formModelId) throws Exception {
        AppDefaultFilterIntf appDefaultFilter = AppCacheUtil.getAppDefaultFilter(panelContext);
        if (appDefaultFilter == null) return null;
        SqlExpression sqlExpression = appDefaultFilter.buildDefaultFilter(formModelId);
        if (sqlExpression == null) return null;
        return sqlExpression;


    }

    // 获取字段值集合
    public Set<String> getFieldsByForms(List<Form> forms, String fieldName) {
        Set<String> resultSet = new LinkedHashSet<>();
        if (isEmpty(forms)) return resultSet;
        if (StrUtil.isBlank(fieldName)) return resultSet;
        for (Form form : forms) {
            try {
                String value = form.getString(fieldName);
                if (StrUtil.isBlank(value)) continue;
                resultSet.add(value);
            } catch (Exception e) {
                // 无需关注！
                continue;
            }
        }

        return resultSet;
    }


    // 展示是或否的对话框
    public boolean showYesOrNoDialog(PanelContext panelContext, String title, String message) throws Exception {

        EscapeButtonDto ok = (EscapeButtonDto) new EscapeButtonDto().setText("是").setWidgetId("_BUTTON_YES")
                .setConfirmStyle();
        EscapeButtonDto cancel = (EscapeButtonDto) new EscapeButtonDto().setText("否").setCancelStyle();

        SinglePanelDto panel = new SinglePanelDto(new LabelDto(message));
        panel.setPreferSize(SizeDto.all(300, 120));

        DialogDto dlg = PopDialog.buildDialog(title, panel, ok, cancel, true).setDecoration(null)
                .setBarrierDismissible(true).setTitleIcon(null);
        PanelValue panelValue = PopDialog.pop(panelContext, dlg, PopWidgetDto.DEFAULT_TIME_OUT);
        if (panelValue == null)
            return false;

        int clickOK = ToolUtilities.getInteger(panelValue.getValue("_BUTTON_YES"), -1);
        if (clickOK > 0)
            return true;


        return false;
    }

    // 当字段为空时
    public boolean isFieldEmpty(Form form, String fieldName) throws Exception {
        if (form == null || StrUtil.isBlank(fieldName)) return true;
        Object val = form.getAttrValue(fieldName);
        if (val == null) return true;
        if (val instanceof String && StrUtil.isBlank((String) val)) return true;
        return false;

    }

    // 获取或创建用户
    public User getOrCreateUserQuickly(IDao dao, OctoDomainOpObserver observer, String userModelId, String userName, String password) throws Exception {
        if (StrUtil.hasBlank(userModelId, userName))
            throw UserException.Builder.userModelIdOrNameEmpty();
        if (StrUtil.isBlank(password)) password = "123456";
        User user = IUserMgr.get().queryUserByName(dao, userModelId, userName);
        if (user != null) return user;

        user = new User();
        user.setFormModelId(userModelId);
        user
                .setCode(IdUtil.fastUUID())
                .setUserName(userName)
                .setPassword(new Password().setValue(password))
                .setStatus(UserStatus.UnLocked)
                .setFullName("")
                .setGender(UserGender.Male)
                .setEmail("")
                .setPhone("")
        ;

        return IUserMgr.get().createUser(null, dao, user, observer);

    }

    // 将用户添加到指定角色
    // eg: addUserToAssignRole(dao, orgModelId, userModelId, role, "admin");
    public void addUserToAssignRole(IDao dao, String orgModeId, String userModelId, Role role, String userName) throws Exception {
        if (StrUtil.hasBlank(orgModeId, userModelId, userName))
            throw UserException.Builder.orgOrUserModelIdOrCodeEmpty();
        if (role == null) throw UserException.Builder.roleEmpty();

        User targetuUser = IUserMgr.get().queryUserByName(dao, userModelId, userName);
        if (targetuUser == null) throw UserException.Builder.notFoundWithName(userName);

        // 1. 先查询看是不是已经在这个角色里面了
        List<UserDto> userDtos = IRoleService.get().listMountedUser(dao, role.getUuid());
        if (!userDtos.isEmpty()) {
            for (UserDto userDto : userDtos) {
                String username = userDto.getUsername();
                String alias = userDto.getAlias();
                if (username.equals(userName)) {
                    return;
                }
            }
        }

        // 2. 将用户插入到这个角色里面
        IRoleMgr.get().mountRoleToUser(dao, role.getUuid(), userModelId, CollUtil.newArrayList(targetuUser.getUuid()));


    }

    // 获取或创建（如果为空）角色
    // eg: getOrCreateSimpleRole(dao, orgModelId, "平台初始化组织/公司领导", "成员")
    public Role getOrCreateSimpleRole(IDao dao, OctoDomainOpObserver observer, String orgModelId, String orgPath, String roleName) throws Exception {
        if (StrUtil.hasBlank(orgPath, roleName)) return null;

        // 1、循环创建组织
        String[] orgNames = orgPath.split("/");
        Org prevOrg = null;
        for (String orgName : orgNames) {
            if (StrUtil.isBlank(orgName)) continue;

            Cnd orgQueryCnd =
//                    Cnd.NEW();
                    getBusDomainFilterCondition(observer, orgModelId);
            orgQueryCnd.where().andEquals(Org.Label, orgName);
            if (prevOrg != null) {
                // 如果前面不为空，则要加上父节点过滤条件
                orgQueryCnd.where().andEquals(Org.ParentUuid, prevOrg.getUuid());
            }

            ResultSet<Org> orgRs = IRoleMgr.get().queryOrgPage(dao, orgModelId, orgQueryCnd, 1, 1);
            if (!orgRs.isEmpty()) {
                // 代表这个组织存在
                prevOrg = orgRs.getDataList().get(0);
                continue;
            } else {
                // 这个组织不存在，要创建
                String uuid = IdUtil.fastUUID();
                Org org = new Org(orgModelId);
                org.setName(orgName).setLabel(orgName).setUuid(uuid).setAttrValue(Form.Code, uuid);

                if (prevOrg != null) org.setParentUuid(prevOrg.getUuid());
                prevOrg = IRoleMgr.get().createOrg(null, dao, org, observer);
            }
        }


        if (prevOrg == null) {
            // 在这个时候组织已经确定全部都创建出来了，prevOrg是用户提供的最底下的组织（根组织的反义..）
            LvUtil.trace(StrUtil.format("获取或查询失败，PrevOrg为空,组织模型[{}], 组织路径:[{}], 角色姓名:[{}]", orgModelId, orgPath, roleName));
            return null;
        }


        // 2、给最后一个组织创建一个角色

        List<Role> roles = IRoleMgr.get().queryRoleListOfOrg(dao, orgModelId, prevOrg.getUuid());
        if (!roles.isEmpty()) {
            for (Role role : roles) {
                if (role.getLabel().equals(roleName)) {
                    return role;
                }
            }
        } else {
            LvUtil.trace("role is null");
        }

        Role role = new Role();
        String uuid = IdUtil.fastUUID();
        role.setUuid(uuid).setCode(uuid).setLabel(roleName).setOwner(prevOrg.getUuid());

        return IRoleMgr.get().createRole(null, dao, orgModelId, prevOrg.getUuid(), role, observer);

    }


    public void refreshFormViewData(PanelContext panelContext, AbsFormView formView, Form form) throws Exception {
        if (formView == null || form == null) return;
        BaseDataViewParam viewParam = (BaseDataViewParam) formView.getWidgetParam();
        viewParam.setData(form);
        formView.onRefresh(panelContext);
    }

    // 发送消息
    public void sendProgressMsg(Progress<?> progress, int v, String s) {
        if (progress == null) return;
        IProgressUtil.sendProcess(progress.getProg(), v, s, true);
    }    // 发送消息


    // 指定资源路径获取内容
    public String getResourceStr(String resourcePath) {
        try {
            if (resourcePath == null) return null;
            URL viewFileUrl = ClassFactory.getResourceURL(resourcePath);
            if (viewFileUrl == null) return null;
            return FileUtil.readString(viewFileUrl, Charset.defaultCharset());
        } catch (Exception e) {
            return null;
        }

    }

}
