package cell.octo.cm.expr;


import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import gpf.adur.data.AttachData;
import gpf.adur.data.Form;
import octo.cm.exception.business.ApplicationException;
import octo.cm.exception.business.DomainException;
import octo.cm.util.ApplicationUtil;
import octo.cm.util.EasyOperation;
import octo.cm.util.FormToJsonConversionUtil;
import octocm.domain.observer.OctoDomainOpObserver;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Comment("应用操作函数")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-10-22", updateTime = "2025-10-22"
)
public interface ApplicationExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    @MethodDeclare(
            label = "获取默认应用信息", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),
            }
    )
    default JSONObject getDefaultApplication(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        String appCode = ApplicationUtil.getDefaultPublishApplicationCode(observer);
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.domainDefaultAppNotSet();
        try (IDao dao = IDaoService.newIDao()) {
            Form form = ApplicationUtil.queryApplicationFormByAppCode(dao, appCode);
            if (form == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

            // 移除所有附件
            doRemoverAllAttachmentData(form);

            return FormToJsonConversionUtil.convert(form);

        }


    }


    @MethodDeclare(
            label = "获取应用列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),
            }
    )
    default List<JSONObject> queryApplicationList(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);
        try (IDao dao = IDaoService.newIDao()) {
            List<Form> forms = ApplicationUtil.queryApplicationForms(dao, observer, false);
            if (Op.isEmpty(forms)) return CollUtil.newArrayList();

            return forms.stream().map(this::doRemoverAllAttachmentData)
                    .map(form -> {

                        try {
                            return FormToJsonConversionUtil.convert(form);
                        } catch (Exception ignored) {
                        }

                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());


        }

    }

    @MethodDeclare(
            label = "设置默认应用信息", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),
            }
    )
    default void setDefaultApplication(String busDomainCode, String appCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(appCode)) throw ApplicationException.Builder.appCodeEmpty();
        try(IDao dao = IDaoService.newIDao()){
            Form form = ApplicationUtil.queryApplicationFormByAppCode(dao, appCode);
            if (form == null) throw ApplicationException.Builder.notFoundWithCode(appCode);

            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(busDomainCode);

            ApplicationUtil.setDefaultPublishApplicationCode(observer,appCode);
        }


    }



    
    // ========================= 支撑方法 =========================

    default Form doRemoverAllAttachmentData(Form form) {
        for (Map.Entry<String, Object> entry : form.getData().entrySet()) {
            try {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof AttachData) {
                    form.setAttrValue(key, null);
                } else if (value instanceof List) {
                    List<Object> list = (List<Object>) value;
                    if (!list.isEmpty() && list.get(0) instanceof AttachData) {
                        form.setAttrValue(key, null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return form;
    }


}
