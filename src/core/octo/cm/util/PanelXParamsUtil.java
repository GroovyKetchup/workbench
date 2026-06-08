package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.constant.WorkBenchConst;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

@Comment("PanelX参数管理工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-25", updateTime = "2025-08-25"
)
public class PanelXParamsUtil {

    private static final String FormModelId_Params = WorkBenchConst.FormModelId_Params;

    private static final EasyOperation Op = EasyOperation.get();

    // 获取参数
    public static String getParamOrDefault(OctoDomainOpObserver observer,
                                           String paramKey, String defaultVal) throws Exception {
        if (observer == null || StrUtil.isBlank(paramKey)) return defaultVal;


        Cnd cnd = Op.getBusDomainFilterCondition(observer, FormModelId_Params);

        cnd.where().andEquals(Op.getFieldCode("参数名"), paramKey);

        try (IDao dao = IDaoService.newIDao()) {
            ResultSet<Form> rs = IFormMgr.get().queryFormPage(dao, FormModelId_Params, cnd, 1, 1, false, false);
            if (rs.isEmpty()) return defaultVal;
            String val = rs.getDataList().get(0).getString("参数值");
            if (StrUtil.isBlank(val)) return defaultVal;
            return val;

        }


    }

    // 设置参数
    public static void setParam(OctoDomainOpObserver observer, String paramKey, String paramVal) throws Exception {


        try (IDao dao = IDaoService.newIDao()) {

            boolean isCreate = false;
            Form form = Op.queryFormByCondition(dao, FormModelId_Params, "参数名", paramKey, cnd -> {
                cnd.where().and(
                        Op.getBusDomainFilterExpr(observer, FormModelId_Params)
                );
                return cnd;
            });
            if (form == null) {
                isCreate = true;
                form = Op.newForm(FormModelId_Params);
            }

            form.setAttrValue("参数名", paramKey);
            form.setAttrValue("参数值", paramVal);

            if (isCreate) {
                IFormMgr.get().createForm(null, dao, form, observer);
            } else {
                IFormMgr.get().updateForm(null, dao, form, observer);
            }
            dao.commit();
        }

    }

}
