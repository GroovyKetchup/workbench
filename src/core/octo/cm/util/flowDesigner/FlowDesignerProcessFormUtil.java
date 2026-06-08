package octo.cm.util.flowDesigner;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.gpf.dc.runtime.IDCRuntimeContext;
import cell.gpf.dc.runtime.IPDFRuntimeMgr;
import cmn.enums.NestingTableUpdateMode;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import org.nutz.dao.Cnd;

public class FlowDesignerProcessFormUtil {

    public static Form queryTotalForm(IDao dao, IDCRuntimeContext rtx, Form currentForm) throws Exception {
        if (dao == null) throw new IllegalArgumentException("查询流程主表单缺少dao");
        if (rtx == null) throw new IllegalArgumentException("查询流程主表单缺少运行上下文");
        if (currentForm == null || StrUtil.isBlank(currentForm.getUuid())) throw new IllegalArgumentException("查询流程主表单缺少当前表单uuid");
        String pdfUuid = rtx.getPdfInstance().getPdfUuid();
        if (StrUtil.isBlank(pdfUuid)) throw new IllegalArgumentException("查询流程主表单缺少流程模型ID");
        Cnd cnd = Cnd.where(Form.UUID, "=", currentForm.getUuid());
        Form totalForm = IPDFRuntimeMgr.get().queryTotalForm(dao, pdfUuid, cnd);
        if (totalForm == null) throw new IllegalArgumentException("未找到流程主表单：pdfUuid=" + pdfUuid + ", formUuid=" + currentForm.getUuid());
        return totalForm;
    }

    public static Object getTotalFormAttrValue(IDCRuntimeContext rtx, Form currentForm, String fieldName) throws Exception {
        if (StrUtil.isBlank(fieldName)) throw new IllegalArgumentException("查询流程主表单字段缺少字段名");
        try (IDao dao = IDaoService.newIDao()) {
            Form totalForm = queryTotalForm(dao, rtx, currentForm);
            return totalForm.getAttrValue(fieldName);
        }
    }

    public static void updateTotalFormAttrValue(IDCRuntimeContext rtx, Form currentForm, String fieldName, Object fieldValue) throws Exception {
        if (StrUtil.isBlank(fieldName)) throw new IllegalArgumentException("更新流程主表单字段缺少字段名");
        try (IDao dao = IDaoService.newIDao()) {
            Form totalForm = queryTotalForm(dao, rtx, currentForm);
            totalForm.setAttrValue(fieldName, fieldValue);
            String[] updateFields = new String[]{IFormMgr.get().getFieldCode(fieldName)};
            IFormMgr.get().updateForm(dao, totalForm, NestingTableUpdateMode.IncrementUpdate, updateFields, null);
            dao.commit();
        }
    }
}
