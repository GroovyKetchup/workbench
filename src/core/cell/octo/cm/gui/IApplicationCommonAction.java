package cell.octo.cm.gui;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.jit.ActionIntf;
import cell.rapidView.function.BasicFunc;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cn.hutool.core.util.StrUtil;
import fe.cmn.panel.ability.PopDialog;
import fe.cmn.widget.SizeDto;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import gpf.dc.basic.form.define.ApplicationDefine;
import gpf.exception.VerifyException;
import octo.cm.exception.business.ApplicationException;
import jit.param.view.action.ViewActionParameter;
import org.nutz.dao.entity.annotation.Comment;

import java.util.List;
import java.util.StringJoiner;

@Comment("应用动作集合")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2025-07-04", updateTime = "2025-07-04"
)
public interface IApplicationCommonAction extends ActionIntf, BasicFunc {

    @MethodDeclare(
            label = "清空选中应用的菜单",
            what = "", why = "", how = "",
            developer = "裴硕", version = "1.0",
            createTime = "2025-07-04", updateTime = "2025-07-04",
            inputs = {
                    @InputDeclare(
                            name = "input",
                            label = "输入",
                            desc = ""
                    )
            }
    )
    default void clearAssignApplicationsMenu(ViewActionParameter input) throws Exception {
        List<Form> appForms = getTableCurrBeSelectedForm(input);
        if (isEmpty(appForms)) throw ApplicationException.Builder.selectAppToClear();

        String appNames = getAppNames(appForms);
        if (!PopDialog.showConfirm(input.getPanelContext(), "提示", StrUtil.format("确定要清空:\n{}\n这些应用的菜单吗？", appNames),
                SizeDto.all(300, 200), true, null, true)) {
            return;
        }
        if (!PopDialog.showConfirm(input.getPanelContext(), "提示", StrUtil.format("第二次确认，你要清空:\n{}\n这些应用的菜单吗？", appNames),
                SizeDto.all(300, 200), true, null, true)) {
            return;
        }

        try (IDao dao = IDaoService.newIDao()) {
            // 直接从前端取的表格会省略掉嵌套表格
            for (Form app : appForms) {
                Form wholeForm = IFormMgr.get().queryForm(dao, app.getFormModelId(), app.getUuid());
                if (wholeForm == null) continue;
                TableData menus = wholeForm.getTable(ApplicationDefine.sMenu);
                if (isEmpty(menus)) continue;
                menus = new TableData(menus.getFormModelId());
                wholeForm.setAttrValue(ApplicationDefine.sMenu, menus);
                IFormMgr.get().updateForm(dao, wholeForm);
            }
            dao.commit();

        }

        doRefreshTable(input);
        popOperateSuccess(input.getPanelContext());


    }

    default String getAppNames(List<Form> appForms) throws Exception {
        StringJoiner stringJoiner = new StringJoiner("\n");
        int idx = 0;
        for (Form appForm : appForms) {
            stringJoiner.add(
                    StrUtil.format(
                            "{}.{}[{}]",
                            ++idx,
                            appForm.getString(ApplicationDefine.sLabel),
                            appForm.getString(Form.Code)
                    )
            );
        }

        return stringJoiner.toString();

    }


}
