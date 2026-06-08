package octo.cm.util;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.leavay.dfc.gui.LvUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Comment("面板设计依赖清理工具")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-09-18", updateTime = "2025-09-18"
)
public class PanelDesignDependCleaningUtil {

    private static final EasyOperation Op = EasyOperation.get();

    // 清理没有被使用的属性/按钮/事件
    public static void cleaning(Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return;

        try (IDao dao = IDaoService.newIDao()) {
            cleaning(dao, panelDesignForm);
            dao.commit();
        }


    }

    // 清理没有被使用的属性/按钮/事件
    public static void cleaning(IDao dao, Form panelDesignForm) throws Exception {
        if (panelDesignForm == null) return;

        doCleaningByAcInTd(dao, panelDesignForm, "面板数据.属性实现");
        doCleaningByAcInTd(dao, panelDesignForm, "面板按钮.面板按钮");
        doCleaningByAcInTd(dao, panelDesignForm, "面板权限.权限实现");
        doCleaningByAcInTd(dao, panelDesignForm, "面板事件.事件实现");

    }

    private static void doCleaningByAcInTd(IDao dao, Form panelDesignForm, String fieldPath) throws Exception {

        try {
            // 获取当前正在使用者的关联对象
            Object[] result = doGetUsingAcCodes(panelDesignForm, fieldPath);
            if (result == null || result.length != 2) return;
            String formModelId = (String) result[0];
            Set<String> formCodes = (Set<String>) result[1];

            if (StrUtil.isBlank(formModelId) || CollUtil.isEmpty(formCodes)) return;

            String pdUuid = panelDesignForm.getUuid();

            // 接下来要删除Form.owner = pdUuid，但是又没有使用的数据

            Cnd cnd = Cnd.NEW();
            cnd.where()
                    // Owner是当前面板
                    .andEquals(Form.Owner, pdUuid)
                    // 但又不在使用列表里
                    .andNotInStrList(Form.Code, new ArrayList<>(formCodes));

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, formModelId, cnd, 1, Integer.MAX_VALUE, false, false);

            LvUtil.trace(StrUtil.format("准备清理未被使用的数据，模型:[{}], 删除数量:[{}]",
                    formModelId, queryRs.getSize()));

            IFormMgr.get().deleteForm(dao, formModelId, cnd);

        } catch (Exception e) {
            Op.logException(e);
        }


    }

    private static Object[] doGetUsingAcCodes(Form panelDesignForm, String fieldPath) throws Exception {

        String[] split = fieldPath.split("\\.");
        String tdName = split[0], acName = split[1];

        TableData td = panelDesignForm.getTable(tdName);
        if (Op.isEmpty(td)) return null;

        String formModelId = null;
        Set<String> formCodes = new HashSet<>();
        for (Form row : td.getRows()) {
            Object attrValue = row.getAttrValue(acName);
            if (attrValue instanceof AssociationData) {
                AssociationData association = (AssociationData) attrValue;
                String f1 = association.getFormModelId();
                String v1 = association.getValue();
                if (StrUtil.hasBlank(f1, v1)) continue;
                if (formModelId == null) formModelId = f1;
                formCodes.add(v1);
            }

        }

        if (StrUtil.isBlank(formModelId) || formCodes.isEmpty()) {
            return null;
        }

        return new Object[]{formModelId, formCodes};
    }


}
