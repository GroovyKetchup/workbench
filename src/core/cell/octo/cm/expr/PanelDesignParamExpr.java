package cell.octo.cm.expr;


import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octocm.domain.service.IDomainService;
import cell.octocm.workbench.expr.WorkbenchCMEnum;
import cell.octocm.workbench.service.IPanelXProService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.utils.FormValueUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import gpf.adur.data.Form;
import gpf.adur.data.ResultSet;
import gpf.adur.data.TableData;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.DomainException;
import octo.cm.ruleengine.RuleFunctionMeta;
import octo.cm.util.EasyOperation;
import octocm.design.consts.Octomica2DesignConst;
import octocm.domain.dto.DomainDto;
import octocm.domain.filter.OctoDomainDataFilter;
import octocm.workbench.dto.extend.AttributeStyleDto;
import org.apache.commons.lang3.EnumUtils;
import org.nutz.dao.Cnd;
import org.nutz.dao.entity.annotation.Comment;
import org.nutz.dao.util.cri.SqlExpression;

import java.util.*;

@Comment("面板设计器-参数辅助-操作函数")
@ClassDeclare(
        label = "",
        what = "", why = "", how = "",
        developer = "裴硕", version = "1.0",
        createTime = "2026-02-03", updateTime = "2026-02-03"
)
@Deprecated
// 注意：该类已弃用，请使用PanelDesignGeneralExpr，无需声明业务域编号
public interface PanelDesignParamExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    @MethodDeclare(
            label = "获取样式列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "busDomainCode", label = "业务域编号", desc = ""),
            }
    )
    @Deprecated
    default List<AttributeStyleDto> queryAttrStyles(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<AttributeStyleDto> result = new TreeSet<>(Comparator.comparing(AttributeStyleDto::getCode));

            List<AttributeStyleDto> attrs1 = IPanelXProService.get().queryAttributeStyleMetas(dao, busDomainCode, null);
            if (CollUtil.isNotEmpty(attrs1)) result.addAll(attrs1);

            List<AttributeStyleDto> attrs2 = IPanelXProService.get().queryAttributeStyleMetas(dao,
                    Octomica2DesignConst.DOMAIN_SYSTEM, null);

            if (CollUtil.isNotEmpty(attrs2)) result.addAll(attrs2);

            return new ArrayList<>(result);

        }

    }

    @MethodDeclare(
            label = "获取操作函数列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "busDomainCode", label = "业务域编号", desc = ""),
            }
    )
    @Deprecated
    default List<RuleFunctionMeta> queryOperateFunctions(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<RuleFunctionMeta> result = new TreeSet<>(Comparator.comparing(RuleFunctionMeta::getRuleCode));

            List<RuleFunctionMeta> functions1 = IPanelXProService.get().queryOperationFunctionMetas(dao, busDomainCode, null);
            if (CollUtil.isNotEmpty(functions1)) result.addAll(functions1);

            List<RuleFunctionMeta> functions2 = IPanelXProService.get().queryOperationFunctionMetas(dao,
                    Octomica2DesignConst.DOMAIN_SYSTEM, null);

            if (CollUtil.isNotEmpty(functions2)) result.addAll(functions2);

            return new ArrayList<>(result);

        }

    }

    @MethodDeclare(
            label = "获取规则函数列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "busDomainCode", label = "业务域编号", desc = ""),
            }
    )
    @Deprecated
    default List<RuleFunctionMeta> queryRuleFunctions(String busDomainCode) throws Exception {
        if (StrUtil.isBlank(busDomainCode)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<RuleFunctionMeta> result = new TreeSet<>(Comparator.comparing(RuleFunctionMeta::getRuleCode));

            List<RuleFunctionMeta> functions1 = IPanelXProService.get().queryRuleFunctionMetas(dao, busDomainCode, null);
            if (CollUtil.isNotEmpty(functions1)) result.addAll(functions1);

            List<RuleFunctionMeta> functions2 = IPanelXProService.get().queryRuleFunctionMetas(dao,
                    Octomica2DesignConst.DOMAIN_SYSTEM, null);

            if (CollUtil.isNotEmpty(functions2)) result.addAll(functions2);

            return new ArrayList<>(result);

        }

    }




}
