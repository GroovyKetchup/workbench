package cell.octo.cm.expr;


import ai.agent.engine.groupChat.model.definition.LLMConfig;
import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.AssociationData;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import octo.cm.util.EasyOperation;
import org.nutz.dao.entity.annotation.Comment;

import java.util.ArrayList;
import java.util.List;

@Comment("大模型操作函数")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-10-22", updateTime = "2025-10-22"
)
public interface LlmModelExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    // Orch的大模型配置
    String FORM_MODEL_ID_LLM_CONFIG = "gpf.md.orch.LLM_pei_zhi";


    @MethodDeclare(
            label = "获取可用的LLM模型", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "input", label = "输入", desc = "", exampleValue = "$ActionParameter$"),
            }
    )
    default List<LLMConfig> getUsableModels() throws Exception {

        List<LLMConfig> usableModels = new ArrayList<>();

        try (IDao dao = IDaoService.newIDao()) {

            ResultSet<Form> queryRs = IFormMgr.get().queryFormPage(dao, FORM_MODEL_ID_LLM_CONFIG, null, 1, Integer.MAX_VALUE,
                    false, false);

            for (Form form : queryRs.getDataList()) {
                String configCode = form.getString(Form.Code);
                String configName = form.getString("名称");
                String baseUrl = form.getString("baseUrl");
                String apiKey = form.getString("apiKey");
                if (StrUtil.hasBlank(baseUrl, apiKey)) continue;
                AssociationData model = form.getAssociation("model");
                if (model == null) continue;

                String modelName = model.getForm().getString("名称");
                if (StrUtil.isBlank(modelName)) continue;

                if (modelName.contains("OhMyGPT-")) {
                    modelName = modelName.replaceAll("OhMyGPT-", "");
                }

                usableModels.add(
                        new LLMConfig(
                                configCode,
                                configName,
                                baseUrl,
                                apiKey,
                                modelName
                        )
                );


            }


            return usableModels;
        }


    }

}
