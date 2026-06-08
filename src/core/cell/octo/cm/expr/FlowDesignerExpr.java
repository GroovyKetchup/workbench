package cell.octo.cm.expr;


import ai.webPage.utils.FormModelUtil;
import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IContext;
import cell.octo.cm.service.IPanelDesignService;
import cell.octocm.domain.service.IDomainService;
import cell.octocm.workbench.service.IPanelXProService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import cmn.utils.FormValueUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.leavay.nio.crpc.RpcMap;
import gpf.adur.data.*;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.ruleengine.RuleFunctionMeta;
import octo.cm.util.EasyOperation;
import octo.cm.util.FlowToPanelDesignPublisher;
import octo.cm.util.PanelDesignPublishUtil;
import octocm.design.consts.Octomica2DesignConst;
import octocm.domain.dto.DomainDto;
import octocm.domain.filter.OctoDomainDataFilter;
import octocm.domain.observer.OctoDomainOpObserver;
import octocm.workbench.dto.extend.AttributeStyleDto;
import octocm.workbench.dto.mianban.MianBanJueSeDto;
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
public interface FlowDesignerExpr extends CellIntf {

    EasyOperation Op = EasyOperation.get();


    @MethodDeclare(label = "获取面板设计列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$"),
            })
    default List<Map<String, Object>> getPanelDesignList(String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();

        List<Map<String, Object>> panelCMListMap = queryPanelCMList(domain, 1, Integer.MAX_VALUE, true);
        if (Op.isEmpty(panelCMListMap)) panelCMListMap = new ArrayList<>();

        // FIXME 统一封装或找宗智的
        String userModelId = StrUtil.format("gpf.md.user.{}_User", domain);

        FormModel userFormModel = IFormMgr.get().queryFormModel(userModelId);
        if (userFormModel != null) {
            Map<String, Object> userFormModelMap = new HashMap<>();
            List<String> attrList = new ArrayList<>();
            for (FormField formField : userFormModel.getNotHiddenFieldList()) {
                attrList.add(formField.getName());
            }

            userFormModelMap.put("面板编号", "$用户模型$");
            userFormModelMap.put("面板名称", "用户模型");
            userFormModelMap.put("面板描述", "用户模型");
            userFormModelMap.put("属性列表", attrList);
            userFormModelMap.put("系统模型", true);

            panelCMListMap.add(userFormModelMap);

        }


        return panelCMListMap;
    }

    @MethodDeclare(
            label = "获取面板角色列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
            }
    )
    default List<MianBanJueSeDto> queryRoleList(String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            IDomainService domainService = IDomainService.get();
            DomainDto domainDto = domainService.getDomainByCode(domain);

            String modelId = WorkBenchConst.FormModelId_Axis_Role;
            OctoDomainDataFilter octoDomainDataFilter = new OctoDomainDataFilter(domainDto);
            SqlExpression sqlExpression = octoDomainDataFilter.buildDefaultFilter(modelId);

            List<Form> dataList = IFormMgr.get().queryFormPage(dao, modelId,
                    Cnd.where(sqlExpression), 1, Integer.MAX_VALUE, false, true).getDataList();
            List<MianBanJueSeDto> result = new ArrayList<>();
            if (CollUtil.isEmpty(dataList)) return result;

            for (Form form : dataList) {
                result.add(new MianBanJueSeDto()
                        .setCode(form.getString("角色编号"))
                        .setName(form.getString("角色名称"))
                        .setDesc(form.getString("角色描述"))
                        .setMatchRule(form.getString("组织匹配"))
                        .setTag(form.getString("分类标签")));
            }
            return result;
        }
    }


    @MethodDeclare(
            label = "获取样式列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
            }
    )
    default List<AttributeStyleDto> queryAttrStyles(String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<AttributeStyleDto> result = new TreeSet<>(Comparator.comparing(AttributeStyleDto::getCode));

            List<AttributeStyleDto> attrs1 = IPanelXProService.get().queryAttributeStyleMetas(dao, domain, null);
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
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
            }
    )
    default List<RuleFunctionMeta> queryOperateFunctions(String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<RuleFunctionMeta> result = new TreeSet<>(Comparator.comparing(RuleFunctionMeta::getRuleCode));

            List<RuleFunctionMeta> functions1 = IPanelXProService.get().queryOperationFunctionMetas(dao, domain, null);
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
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
            }
    )
    default List<RuleFunctionMeta> queryRuleFunctions(String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        try (IDao dao = IDaoService.newIDao()) {
            Set<RuleFunctionMeta> result = new TreeSet<>(Comparator.comparing(RuleFunctionMeta::getRuleCode));

            List<RuleFunctionMeta> functions1 = IPanelXProService.get().queryRuleFunctionMetas(dao, domain, null);
            if (CollUtil.isNotEmpty(functions1)) result.addAll(functions1);

            List<RuleFunctionMeta> functions2 = IPanelXProService.get().queryRuleFunctionMetas(dao,
                    Octomica2DesignConst.DOMAIN_SYSTEM, null);

            if (CollUtil.isNotEmpty(functions2)) result.addAll(functions2);

            return new ArrayList<>(result);

        }

    }


    @MethodDeclare(
            label = "流程设计发布到面板", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "context", label = "", desc = "", exampleValue = "$context$"),
                    @InputDeclare(desc = "运行输出", name = "output", label = "运行输出", exampleValue = "$output$", nullable = true),
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
                    @InputDeclare(name = "modelType", label = "模型类别", desc = "", nullable = true),
                    @InputDeclare(name = "flowDefPanelCode", label = "流程定义面板编号", desc = "", nullable = true),
                    @InputDeclare(name = "mappedData", label = "映射过来的数据", desc = "", nullable = true),
            }
    )
    default Form publishToPanelDesign(IContext context, RpcMap<Object> output,
                                      String domain, String modelType, String flowDefPanelCode,
                                      String mappedData) throws Exception {

        // 上一个动作维护了流程设计的配置文件，然后保存了起来，这里要拿到上一个流程的这个属性
        Object formObj = output.get("表单保存");
        if (!(formObj instanceof Form)) throw new RuntimeException("未拿到上一个操作函数的返回值（表单保存）");

        Form flowDef = (Form) formObj;
        String flowDefDataCode = flowDef.getString(Form.Code);


        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(flowDefPanelCode) || StrUtil.isBlank(flowDefDataCode))
            throw new RuntimeException("流程定义面板编号/数据编号不能为空");
        if (StrUtil.isBlank(mappedData)) throw CommonException.Builder.jsonDataEmpty();
        if (StrUtil.isBlank(modelType)) throw new RuntimeException("模型类别不能为空");

        JSONObject obj;
        try {
            obj = JSONUtil.parseObj(mappedData);
        } catch (Exception e) {
            throw CommonException.Builder.invalidJsonData();
        }

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(domain);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(domain);

        try (IDao dao = IDaoService.newIDao()) {

            // 1. 通过流程定义记录拿到当前已绑定的面板编号
            String flowDefModelId = FormModelUtil.buildPanelFormModelIdByCmName(domain, flowDefPanelCode);
            flowDef = Op.queryFormByCondition(dao, flowDefModelId, Form.Code, flowDefDataCode, null);
            if (flowDef == null) throw PanelDesignException.Builder.notFoundWithCode(flowDefDataCode);
            String existedPanelCode = flowDef.getString("关联面板");

            // 2. 调度处理器完成 JSON → 面板 Form 的全部转换并落库
            FlowToPanelDesignPublisher publisher = new FlowToPanelDesignPublisher(dao, observer, modelType, flowDef, obj);
            Form panelDesign = publisher.publish(existedPanelCode);

            // 3. 新建分支（含"关联面板找不到"的回退）：把新分配的面板编号回写到流程定义
            if (publisher.isNewPanel()) {
                flowDef.setAttrValue("关联面板", panelDesign.getString("面板编号"));
                flowDef = IFormMgr.get().updateForm(null, dao, flowDef, observer);
                dao.commit();
            }
        }

        return flowDef;
    }


    @MethodDeclare(
            label = "流程设计面板生效", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$", nullable = true),
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
            }
    )
    default void takeEffectPanelDesign(String domain, String targetPanelCode) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();

        try (IDao dao = IDaoService.newIDao()) {
            OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(domain);
            Form panelDesignForm = IPanelDesignService.get().getPanelDesign(dao, observer, targetPanelCode, true);
            if (panelDesignForm == null) throw PanelDesignException.Builder.notFoundWithCode(targetPanelCode);

            PanelDesignPublishUtil.publishBatch(null, null,
                    observer, CollUtil.newArrayList(panelDesignForm));

        }

    }


    // ========================= 支撑方法 =========================


    // 之前宗智写的获取面板的方法，复用一下
    default List<Map<String, Object>> queryPanelCMList(String domainCode, long pageNo, long pageSize, boolean withSystem) throws Exception {
        try (IDao dao = IDaoService.newIDao()) {
            IDomainService domainService = IDomainService.get();
            DomainDto domainDto = domainService.getDomainByCode(domainCode);

            String modelId = WorkBenchConst.FormModelId_PanelDesign;
            OctoDomainDataFilter octoDomainDataFilter = new OctoDomainDataFilter(domainDto);
            SqlExpression sqlExpression = octoDomainDataFilter.buildInDomainCondition(modelId, withSystem);

            IFormMgr formMgr = IFormMgr.get();
            ResultSet<Form> formResultSet = null;
            formResultSet = formMgr.queryFormPage(dao, modelId, Cnd.where(sqlExpression), (int) pageNo, (int) pageSize, true, true);
            List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < formResultSet.getDataList().size(); i++) {
                Map formMap = new HashMap();
                Form form = formResultSet.getDataList().get(i);
                String panelCode = FormValueUtil.getNestingValue(form, String.class, "面板编号");
                String panelName = FormValueUtil.getNestingValue(form, String.class, "面板名称");
                String panelDesc = FormValueUtil.getNestingValue(form, String.class, "面板描述");
                TableData tableData = form.getTable("面板数据");
                List attrList = new ArrayList();
                if (tableData != null) {
                    for (int j = 0; j < tableData.getRows().size(); j++) {
                        Form subform = tableData.getRows().get(j);
                        String attrName = FormValueUtil.getNestingValue(subform, String.class, "属性实现", "属性名称");
                        attrList.add(attrName);
                    }
                    formMap.put("面板编号", panelCode);
                    formMap.put("面板名称", panelName);
                    formMap.put("面板描述", panelDesc);
                    formMap.put("属性列表", attrList);
                    resultList.add(formMap);
                }
            }
            return resultList;
        }
    }


}
