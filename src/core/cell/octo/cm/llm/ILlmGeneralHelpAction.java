package cell.octo.cm.llm;


import cell.jit.ActionIntf;
import cell.rapidView.function.CommonFunctions;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.utils.FormValueUtil;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import gpf.dc.runtime.PDCForm;
import orchestration.consts.OrchestrationConsts;
import orchestration.dto.OrchestrationActionResultDto;
import orchestration.dto.OrchestrationRuntimeContextDto;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;

@Comment("LLM通用帮助动作")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-08-21", updateTime = "2025-08-21"
)
public interface ILlmGeneralHelpAction extends ActionIntf, CommonFunctions {


    @MethodDeclare(
            label = "回写大模型响应", what = "", why = "", how = "", developer = "", version = "", createTime = "",
            updateTime = "",
            inputs = {
                    @InputDeclare(name = "contextDto", label = "运行上下文", desc = ""),
                    @InputDeclare(name = "targetFieldExpr", label = "目标字段表达式", desc = "")
            }
    )

    default OrchestrationActionResultDto writeRespond(OrchestrationRuntimeContextDto contextDto, String targetFieldExpr) throws Exception {
        try {
            Form dataForm = contextDto.getDataForm();
            String nodeName = contextDto.getNodeName();

            String expression = String.format("%s.%s",
                    nodeName,
                    OrchestrationConsts.编排节点_生成历史
            );
            TableData chatHistoryTable = FormValueUtil.getExpressionValue(dataForm, expression, TableData.class);

            if (chatHistoryTable != null) {
                List<Form> historyTableRows = chatHistoryTable.getRows();

                // 更改为按最新对话时间取出记录
                TableData chatRecordTable = null;
                long newestDate = -1;
                for (Form historyTableRow : historyTableRows) {
                    Long startTime = historyTableRow.getLong(OrchestrationConsts.生成历史_对话开始时间);
                    if (startTime != null && startTime > newestDate) {
                        newestDate = startTime;
                        chatRecordTable = historyTableRow.getTable(OrchestrationConsts.生成历史_对话记录);
                    }
                }

                if (chatRecordTable != null) {
                    List<Form> rows = chatRecordTable.getRows();

                    String json = null;
                    for (int i = rows.size() - 1; i >= 0; i--) {
                        Form form = rows.get(i);
                        String role = form.getString(OrchestrationConsts.对话记录_角色);
                        if (StringUtils.equals(role, "assistant")) {
                            json = form.getString(OrchestrationConsts.对话记录_内容);
                            break;
                        }
                    }



//                    if (true) {
//                        throw new RuntimeException("targetFieldExpr:" + targetFieldExpr);
//                    }
                    FormValueUtil.setExpressionValue(dataForm, targetFieldExpr, json);

                }
            }
            return new OrchestrationActionResultDto((PDCForm) dataForm);

        } catch (Exception e) {
            throw new RuntimeException(ExceptionUtils.getFullStackTrace(e));
        }
    }

}
