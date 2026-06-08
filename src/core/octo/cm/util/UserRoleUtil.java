package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.workbench.service.IPanelXProService;
import cmn.anotation.ClassDeclare;
import cmn.util.TraceUtil;
import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leavay.dfc.gui.LvUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.user.User;
import octo.cm.dto.panelDesign.PanelRoleDto;
import octo.cm.exception.business.DomainException;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.mianban.MianBanJueSeDto;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static octo.cm.constant.WorkBenchConst.FormModelId_Axis_Role;

@Comment("用户-角色工具类")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-01-20", updateTime = "2026-01-26"
)
public class UserRoleUtil {

    public static final EasyOperation Op = EasyOperation.get();

    // 用户角色缓存的超时时间
    // 目前默认2分钟，即2分钟内获取的用户角色的缓存视为是有效的
    public static final int USER_ROLE_CACHE_TIMEOUT = 2 * 60 * 1000;

    // 用户角色缓存
    private static final LRUCache<String, List<MianBanJueSeDto>> USER_ROLE_CACHE = new LRUCache<>(20_000);
    private static final LRUCache<String, Long> USER_ROLE_UPDATE_TIME_CACHE = new LRUCache<>(20_000);


    // 是否存在某个角色
    public static boolean hasRole(OctoDomainOpObserver observer, String userCode, String roleCode) {

        if (observer == null) throw DomainException.Builder.notFound();
        if (StrUtil.isBlank(userCode)) throw new RuntimeException("用户编号不得为空");
        if (StrUtil.isBlank(roleCode)) throw new RuntimeException("角色编号不得为空");

        try {
            List<MianBanJueSeDto> roles = queryRoles(observer, userCode);
            if (CollUtil.isEmpty(roles)) return false;
            return roles.stream().anyMatch(role -> roleCode.equals(role.getCode()));

        } catch (Exception e) {
            TraceUtil.getCurrentTracer().error(ExceptionUtils.getFullStackTrace(e));
            return false;
        }
    }

    // 是否存在某个角色
    public static boolean hasRoleByName(OctoDomainOpObserver observer, String userCode, String roleName) {

        if (observer == null) throw DomainException.Builder.notFound();
        if (StrUtil.isBlank(userCode)) throw new RuntimeException("用户编号不得为空");
        if (StrUtil.isBlank(roleName)) throw new RuntimeException("角色名称不得为空");

        try {
            List<MianBanJueSeDto> roles = queryRoles(observer, userCode);
            if (CollUtil.isEmpty(roles)) return false;
            return roles.stream().anyMatch(role -> roleName.equals(role.getName()));

        } catch (Exception e) {
            LvUtil.trace(ExceptionUtils.getFullStackTrace(e));
            return false;
        }
    }

    // 是否存在某个角色
    public static List<MianBanJueSeDto> queryRoles(OctoDomainOpObserver observer, String userCode) {

        if (observer == null) throw DomainException.Builder.notFound();
        if (StrUtil.isBlank(userCode)) throw new RuntimeException("用户编号不得为空");
        try {

            // 先看在不在缓存里
            String cacheKey = StrUtil.format("{}_{}", observer.getDomainCode(), userCode);


            List<MianBanJueSeDto> roles = USER_ROLE_CACHE.get(cacheKey);
            Long lastUpdateTime = USER_ROLE_UPDATE_TIME_CACHE.get(cacheKey);

            // 存在缓存 && 是一分钟内的数据
            if (CollUtil.isNotEmpty(roles) && lastUpdateTime != null &&
                    System.currentTimeMillis() - lastUpdateTime < USER_ROLE_CACHE_TIMEOUT)
                return roles;


            roles = IPanelXProService.get().listRoleOfUser(observer.getDomainCode(), userCode);

            USER_ROLE_CACHE.put(cacheKey, roles);
            USER_ROLE_UPDATE_TIME_CACHE.put(cacheKey, System.currentTimeMillis());

            return roles;
        } catch (Exception e) {
            TraceUtil.getCurrentTracer().error(ExceptionUtils.getFullStackTrace(e));
            return null;
        }
    }


    public static List<User> queryUserCodesByRoleCodes(OctoDomainOpObserver observer, List<String> roleCodes) {
       if(observer == null || CollUtil.isEmpty(roleCodes)) return Collections.emptyList();
       List<User> totalUsers = new ArrayList<>();
        for (String roleCode : roleCodes) {
            try {
                List<User> users = IPanelXProService.get().listUserOfRole(observer.getDomainCode(), roleCode);
                if(CollUtil.isNotEmpty(users)){
                    totalUsers.addAll(users);
                }
            } catch (Exception ignored) {
            }

        }
        return totalUsers;
    }


    // ========================= 之前遗留的获取角色的方法 =========================

    // 获取角色
    @Deprecated
    public static List<PanelRoleDto> queryRoles(OctoDomainOpObserver observer) throws Exception {
        if (observer == null) throw DomainException.Builder.busDomainCodeEmpty();

        List<PanelRoleDto> roles = new ArrayList<>();
        try (IDao dao = IDaoService.newIDao()) {
            Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_Axis_Role);
            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FormModelId_Axis_Role, cnd,
                    1, Integer.MAX_VALUE, false, false);
            if (!queryRs.isEmpty()) {
                for (Form roleForm : queryRs.getDataList()) {
                    String roleCode = roleForm.getString(Form.Code);
                    String roleName = roleForm.getString("角色名称");
                    String roleDescription = roleForm.getString("角色描述");
                    String orgMatchRule = roleForm.getString("组织匹配");
                    String category = roleForm.getString("分类标签");
                    if (StrUtil.hasBlank(roleCode, roleName)) continue;
                    roles.add(new PanelRoleDto(roleCode, roleName,
                            roleDescription, orgMatchRule, category));
                }
            }


        }

        return roles;


    }


}
