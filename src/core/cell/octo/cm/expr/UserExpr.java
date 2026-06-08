package cell.octo.cm.expr;


import ai.webPage.dto.RespondDto;
import ai.webPage.utils.attach.AttachUtils;
import cell.CellIntf;
import cell.ai.webPage.http.IGeneralFileController;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.adur.user.IUserMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.octo.cm.service.IPanelDesignService;
import cell.octocm.domain.service.IDomainService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AttachData;
import gpf.adur.data.Form;
import gpf.adur.data.Password;
import gpf.adur.data.ResultSet;
import gpf.adur.user.User;
import gpf.exception.VerifyException;
import octo.cm.exception.business.DomainException;
import octo.cm.util.EasyOperation;
import octo.cm.util.UserRoleUtil;
import octocm.domain.dto.DomainDto;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.*;

import static ai.webPage.utils.ModelDataUtils.CODE_NAME;
import static octo.cm.constant.WorkBenchConst.FormModelId_Axis_Role;

@Comment("用户操作函数")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-10-22", updateTime = "2025-10-22"
)
public interface UserExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    @MethodDeclare(
            label = "获取角色名称列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "busDomainCode", label = "业务域编号", desc = ""),
            }
    )
    default Set<String> getRoleNames(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);

        Set<String> roleNames = new TreeSet<>();
        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_Axis_Role);
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_Axis_Role, cnd,
                    1, Integer.MAX_VALUE, false, false);
            if (!queryRs.isEmpty()) {
                for (Form roleForm : queryRs.getDataList()) {
                    String roleName = roleForm.getString("角色名称");
                    if (StrUtil.isNotBlank(roleName)) {
                        roleNames.add(roleName);
                    }
                }
            }


        }

        return roleNames;


    }


    @MethodDeclare(label = "根据指定角色查询用户列表", how = "", what = "", why = "", inputs = {
            @InputDeclare(desc = "", label = "运行上下文", name = "rtx", exampleValue = "$IDCRuntimeContext$"),
            @InputDeclare(name = "ruleNamespace", label = "业务域编号", desc = "", exampleValue = "$ruleNamespace$"),
            @InputDeclare(desc = "", label = "角色列表", name = "roleListStr")
    }
    )
    default ResultSet<Form> queryUserListByRoleNames(IDCRuntimeContext rtx, Set<String> ruleNamespace, String roleListStr) throws Exception {
        List<String> targetRoleNames = new ArrayList<>();
        if (StrUtil.isNotBlank(roleListStr)) {
            targetRoleNames.addAll(Arrays.asList(roleListStr.split(",")));
        }

        IDao dao = rtx.getDao();
        String userModelId = rtx.getUserModelId();

        // FIXME 后续晓斌提供业务域变量写法后优化
        String busDomainCode = new ArrayList<>(ruleNamespace).get(1);
        DomainDto domainDto = IDomainService.get().getDomainByCode(busDomainCode);
        if (domainDto == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);
        OctoDomainOpObserver octoDomainOpObserver = new OctoDomainOpObserver(domainDto);

        ResultSet<Form> resultSet = IFormMgr.get().queryFormPage(dao, userModelId, null,
                1, Integer.MAX_VALUE, false, true);
        if (resultSet.isEmpty()) return resultSet;

        List<Form> finalUserForms = new ArrayList<>();

        // FIXME 后续宗智提供写法后优化
        for (Form userForm : resultSet.getDataList()) {
            String userCode = userForm.getString(User.Code);
            for (String targetRoleName : targetRoleNames) {
                if (UserRoleUtil.hasRoleByName(octoDomainOpObserver, userCode,
                        targetRoleName)) {
                    finalUserForms.add(userForm);
                    break;
                }

            }

        }


        return new ResultSet<Form>().setDataList(finalUserForms);


    }


    @MethodDeclare(label = "查询用户列表", how = "", what = "", why = "", inputs = {
            @InputDeclare(desc = "", label = "运行上下文", name = "rtx", exampleValue = "$IDCRuntimeContext$"),
            @InputDeclare(name = "ruleNamespace", label = "业务域编号", desc = "", exampleValue = "$ruleNamespace$"),
            @InputDeclare(name = "pageNo", label = "页码", desc = "", exampleValue = "$pageNo$", nullable = true),
            @InputDeclare(name = "pageSize", label = "页数", desc = "", exampleValue = "$pageSize$", nullable = true),
            @InputDeclare(name = "condition", label = "条件", desc = "", exampleValue = "$condition$", nullable = true),
    }
    )
    default ResultSet queryUserList(IDCRuntimeContext rtx, Set<String> ruleNamespace,
                                    Integer pageNo, Integer pageSize,
                                    Map<String, Object> condition
    ) throws Exception {

        IDao dao = rtx.getDao();
        String userModelId = rtx.getUserModelId();

        String busDomainCode = new ArrayList<>(ruleNamespace).get(1);
        DomainDto domainDto = IDomainService.get().getDomainByCode(busDomainCode);
        if (domainDto == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        if (pageNo == null || pageNo < 0) pageNo = 1;
        if (pageSize == null || pageSize < 0) pageSize = 20;


        Cnd cnd = Cnd.NEW();
        if (!Op.isEmpty(condition)) cnd = convertEqualsMapToCnd(condition);

        ResultSet<Form> resultSet = IFormMgr.get().queryFormPage(dao, userModelId, cnd,
                pageNo, pageSize, false, true);
        if (resultSet.isEmpty()) return resultSet;

        resultSet.getDataList().forEach(
                form -> {
                    try {
                        // 特殊处理一下头像，转换为附件（底下因为CK那边的设计是image）
                        Object profileByteArrObj = form.getAttrValue(User.ProfilePhoto);
                        if (profileByteArrObj == null) profileByteArrObj = form.getAttrValue("头像");
                        if (profileByteArrObj instanceof byte[]) {
                            form.setAttrValue("头像", CollUtil.newArrayList(new AttachData("profile.png",
                                    (byte[]) profileByteArrObj)));
                        }
                        // 特殊处理一下下拉框选项
                        form.setAttrValue(User.Gender, getSelectOptionValueMapping(form.getString(User.Gender)));
                        form.setAttrValue(User.Status, getSelectOptionValueMapping(form.getString(User.Status)));


                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                }
        );


        return resultSet;
    }


    @MethodDeclare(
            label = "保存用户", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(desc = "", label = "运行上下文", name = "rtx", exampleValue = "$IDCRuntimeContext$"),
                    @InputDeclare(name = "ruleNamespace", label = "业务域编号", desc = "", exampleValue = "$ruleNamespace$"),
                    @InputDeclare(name = "formData", label = "表单信息", desc = "", exampleValue = "$formData$"),

            }
    )
    default RespondDto saveUser(IDCRuntimeContext rtx, Set<String> ruleNamespace, Map<String, Object> formData

    ) throws Exception {

        String userModelId = rtx.getUserModelId();

        String busDomainCode = new ArrayList<>(ruleNamespace).get(1);
        DomainDto domainDto = IDomainService.get().getDomainByCode(busDomainCode);
        if (domainDto == null) throw DomainException.Builder.notFoundWithCode(busDomainCode);

        try (IDao dao = IDaoService.newIDao()) {

            // 0、将Map转换为UserDto
            User user = convertFormDataToUser(userModelId, formData);
            String userName = user.getUserName();

            if (StrUtil.isBlank(userName)) throw new VerifyException("用户名不得为空");
            if (StrUtil.isBlank(user.getFullName())) throw new VerifyException("姓名不得为空");
            if (StrUtil.isBlank(user.getGender())) throw new VerifyException("性别不得为空");
            if (user.getPassword() == null ||
                    StrUtil.isBlank(user.getPassword().getValue())) throw new VerifyException("密码不得为空");
            if (StrUtil.isBlank(user.getStatus())) throw new VerifyException("状态不得为空");

            // 1、根据用户名检查是否存在
            User existingUser = IUserMgr.get().queryUserByName(dao, userModelId, userName);
            if (existingUser != null) {
                user.setUuid(existingUser.getUuid());
                User updatedUser = IUserMgr.get().updateUser(dao, user);
                dao.commit();
                return RespondDto.newSuccess("更新成功", updatedUser);
            } else {
                // 2、如果不存在就创建
                User createdUser = IUserMgr.get().createUser(dao, user);
                dao.commit();
                return RespondDto.newSuccess("创建成功", createdUser);

            }


        } catch (Exception e) {
            return RespondDto.newError(e.getMessage());
        }


    }

    // 将FormData转换为用户（Dto）
    default User convertFormDataToUser(String userModelId, Map<String, Object> formData) throws Exception {
        if (Op.isEmpty(formData)) return null;
        User user = new User(userModelId);

        // 处理密码
        String passwordStr = formData.getOrDefault("密码", "").toString();
        if (StrUtil.isNotBlank(passwordStr)) {
            user.setPassword(new Password().setValue(passwordStr));
        }

        // 处理头像
        byte[] profileBytes = null;
        // 附件内部传递过来是一个数组，所以需要解一下
        List<Map<String, String>> profileDataMapList = (List<Map<String, String>>) formData.getOrDefault("头像",
                new ArrayList<>());
        if (!Op.isEmpty(profileDataMapList)) {
            // 常见的格式是会携带三个参数：downloadUrl\附件名称\附件编号
            Map<String, String> profileDataMap = (Map<String, String>) profileDataMapList.get(0);
            if (!Op.isEmpty(profileDataMap)) {
                String profileName = profileDataMap.get("附件名称");
                String profileCode = profileDataMap.get("附件编号");
                if (StrUtil.isNotBlank(profileCode)) {
                    try {
                        if (AttachUtils.isFileCodePath(profileCode)) {
                            AttachData attachment = AttachUtils.getAttachment(profileCode);
                            if (attachment != null) {
                                profileBytes = attachment.getContent();
                            }

                        } else {
                            profileBytes = IGeneralFileController.get().readAndDelete(profileCode);
                        }

                    } catch (Exception e) {
                        Op.logf("用户保存时处理用户头像出错");
                        Op.logException(e);
                    }

                }


            }
        }

        user.setProfilePhoto(profileBytes);
//        user.setExtFields(MapUtil.of("头像", profiles));


        return user
                .setCode(formData.getOrDefault("编号", "").toString())
                .setUserName(formData.getOrDefault("用户名", "").toString())
                .setFullName(formData.getOrDefault("姓名", "").toString())
                .setGender(getSelectOptionValueMapping(formData.getOrDefault("性别", "").toString()))
                .setStatus(getSelectOptionValueMapping(formData.getOrDefault("状态", "").toString()))
                .setPhone(formData.getOrDefault("电话", "").toString())
                .setEmail(formData.getOrDefault("邮箱", "").toString())
                ;


    }


    // Cnd中均为Equals
    default Cnd convertEqualsMapToCnd(Map<String, Object> condition) throws Exception {
        Cnd cnd = Cnd.NEW();
        if (condition == null || condition.isEmpty())
            return null;

        boolean hasCondition = false;
        for (String fieldName : condition.keySet()) {
            if (StrUtil.isBlank(fieldName))
                continue;
            if (CODE_NAME.equals(fieldName))
                fieldName = Form.Code;
            Object fieldValue = condition.get(fieldName);
            if (fieldValue == null)
                continue;

            if (fieldValue instanceof String) {
                String val = StrUtil.trim((String) fieldValue);

                val = getSelectOptionValueMapping(val);

                if (StrUtil.isBlank(val)) {
                    // 空字符期望匹配 空字符值或者null值
                    cnd.where().and(Cnd.exps(getUserFieldCode(fieldName), "=", "").orIsNull(Op.getFieldCode(fieldName)));
                } else {
//                    cnd.where().andLike(getUserFieldCode(fieldName), "%" + val + "%");
                    cnd.where().and(Cnd.exps(getUserFieldCode(fieldName), "like", StrUtil.format("%{}%", val)));


                }
            } else {
                cnd.where().andEquals(getUserFieldCode(fieldName), fieldValue);
            }
            hasCondition = true;
        }
        return hasCondition ? cnd : null;
    }

    default String getUserFieldCode(String fieldName) {
        if (StrUtil.isBlank(fieldName)) return null;
        switch (fieldName) {

            case "编号":
                return User.Code;
            case "用户名":
                return User.FullName;
            case "性别":
                return User.Gender;
            case "状态":
                return User.Status;
            case "电话":
                return User.Phone;
            case "邮箱":
                return User.Email;
            default:
                throw new VerifyException(StrUtil.format("用户模型不存在字段[{}]", fieldName));
        }


    }

    default String getSelectOptionValueMapping(String value) {
        if (StrUtil.isBlank(value)) return value;

        if (value.equals("Locked")) return "锁定";
        if (value.equals("锁定")) return "Locked";
        if (value.equals("UnLocked")) return "解锁";
        if (value.equals("解锁")) return "UnLocked";

        if (value.equals("Male")) return "男";
        if (value.equals("男")) return "Male";
        if (value.equals("Female")) return "女";
        if (value.equals("女")) return "Female";

        return value;


    }


}
