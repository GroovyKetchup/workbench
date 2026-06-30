package cell.octo.cm.expr;

import cell.CellIntf;
import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cell.cmn.jdbc.IJDBCService;
import cell.gpf.adur.data.IFormMgr;
import cell.octo.cm.IContext;
import cell.octo.cm.service.IPanelDesignService;
import cmn.anotation.ClassDeclare;
import cmn.anotation.InputDeclare;
import cmn.anotation.MethodDeclare;
import com.leavay.nio.crpc.RpcMap;
import cmn.dto.PairDto;
import cmn.dto.SqlStatementDto;
import cmn.dto.sql.dql.JdbcMetaInfoDto;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import gpf.adur.data.Form;
import gpf.adur.data.TableData;
import octo.cm.constant.ReportDesignConst;
import octo.cm.constant.WorkBenchConst;
import octo.cm.exception.business.CommonException;
import octo.cm.exception.business.DomainException;
import octo.cm.exception.business.PanelDesignException;
import octo.cm.util.EasyOperation;
import octo.cm.util.PanelDesignPublishUtil;
import octo.cm.util.ReportJdbcDataSource;
import octo.cm.util.ReportQueryHelper;
import octo.cm.util.ReportToPanelDesignPublisher;
import octocm.domain.observer.OctoDomainOpObserver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表设计器-操作函数集合（镜像 {@code FlowDesignerExpr} 的接口/注解风格）。
 *
 * <p>本接口为报表设计器 Phase 3 提供后端操作函数：</p>
 * <ul>
 *   <li>{@link #publishToPanelDesign}：把报表定义一次性物化/发布到目标面板，并建立双向绑定：
 *   {@code 面板.Owner = 报表定义.uuid}，{@code 面板.描述 += “<!--REPORT_DEF_MODEL_ID:FormModelId-->”}，
 *   {@code 报表定义.”关联面板” = 面板编号}。</li>
 *   <li>{@link #takeEffectPanelDesign}：报表面板生效（镜像流程）。</li>
 *   <li>数据连接 CRUD：{@link #saveDataConnection} / {@link #deleteDataConnection} / {@link #listDataConnections}。</li>
 *   <li>{@link #testConnection}：连接探活（rebuildDbPool + 轻量探活查询）。</li>
 *   <li>{@link #executeReportQuery}：运行态真参数化取数（通过 Owner + 描述字段隐藏标记查询，零硬编码面板编号）。</li>
 * </ul>
 *
 * <p><b>运行态取数（Owner + 描述字段隐藏标记查询）：</b>按面板编号查面板设计 Form → 从”面板描述”提取 {@code <!--REPORT_DEF_MODEL_ID:xxx-->} 得 FormModelId →
 * 读 {@code Form.Owner} 得 uuid → 直接查询报表定义 → 选”数据连接”子表中”连接标识”匹配行 →
 * 构建自定义 {@link ReportJdbcDataSource} → {@link SqlStatementDto} 真参数化（{@code $name} → {@code ?} 绑定，
 * 集合自动展开 IN 列表，绝不拼 SQL 文本）。</p>
 *
 * @author Devin
 * @version 1.0
 */
@ClassDeclare(
        label = "报表设计器-操作函数",
        what = "报表设计器后端操作函数：发布/生效/数据连接CRUD/测试连接/真参数化取数",
        why = "镜像流程设计器，提供报表 Phase3 后端能力",
        how = "CellIntf 接口 + default 方法 + @MethodDeclare/@InputDeclare",
        developer = "Devin", version = "1.0",
        createTime = "2026-06-25", updateTime = "2026-06-25"
)
public interface ReportDesignerExpr extends CellIntf {

    /** 公共操作辅助单例。 */
    EasyOperation Op = EasyOperation.get();

    // ========================= 发布 / 生效 =========================

    /**
     * 报表设计发布到面板。
     *
     * <p>报表定义这条 Form 取自上一步「表单保存」操作函数的返回（镜像流程，经 {@code output.get(“表单保存”)}），
     * 不再从 {@link IContext#getCmInstance()} 取。面板名称/面板描述由本函数内部从报表定义映射，前端不再传 mappedData。
     * 处理器 {@link ReportToPanelDesignPublisher} 把数据一次性物化进目标面板并落库，建立双向绑定：
     * {@code 面板.Owner = 报表定义.uuid}，{@code 面板.描述 += “<!--REPORT_DEF_MODEL_ID:FormModelId-->”}（发布器内设置），
     * {@code 报表定义.”关联面板” = 面板编号}（新建分支时本函数回写）。</p>
     *
     * @param context 操作上下文
     * @param output  运行输出（取上一步「表单保存」返回的报表定义 Form）
     * @param domain  业务域编号
     * @return 处理后的报表定义 Form
     * @throws Exception 业务域/数据非法或发布失败
     */
    @MethodDeclare(
            label = "报表设计发布到面板", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "context", label = "", desc = "", exampleValue = "$context$"),
                    @InputDeclare(name = "output", label = "运行输出", desc = "运行输出", exampleValue = "$output$", nullable = true),
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$"),
            }
    )
    default Form publishToPanelDesign(IContext context, RpcMap<Object> output, String domain) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();

        // 报表定义取上一步「表单保存」的返回（镜像流程），不再从 context 取
        Object formObj = output == null ? null : output.get("表单保存");
        if (!(formObj instanceof Form)) throw new RuntimeException("未拿到上一个操作函数的返回值（表单保存）");
        Form reportDef = (Form) formObj;

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(domain);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(domain);

        // 面板名称/面板描述映射收进函数内部：从报表定义取，前端不再承担该职责
        JSONObject src = new JSONObject();
        src.set(ReportDesignConst.FieldName_PanelName, reportDef.getString(ReportDesignConst.FieldName_ReportName));
        src.set(ReportDesignConst.FieldName_PanelDesc,
                StrUtil.blankToDefault(reportDef.getString(ReportDesignConst.FieldName_ReportDesc), ""));

        try (IDao dao = IDaoService.newIDao()) {
            String existedPanelCode = reportDef.getString(ReportDesignConst.FieldName_LinkedPanel);

            ReportToPanelDesignPublisher publisher =
                    new ReportToPanelDesignPublisher(dao, observer, ReportDesignConst.ModelType_Report, reportDef, src);
            Form panelDesign = publisher.publish(existedPanelCode);

            // 新建分支（含“关联面板找不到”的回退）：把新分配的面板编号回写到报表定义
            if (publisher.isNewPanel()) {
                reportDef.setAttrValue(ReportDesignConst.FieldName_LinkedPanel,
                        panelDesign.getString(ReportDesignConst.FieldName_PanelCode));
                reportDef = IFormMgr.get().updateForm(null, dao, reportDef, observer);
                dao.commit();
            }
        }

        return reportDef;
    }

    /**
     * 报表设计面板生效（镜像流程）。
     *
     * @param domain          业务域编号
     * @param targetPanelCode 目标面板编号
     * @throws Exception 业务域为空或面板不存在
     */
    @MethodDeclare(
            label = "报表设计面板生效", how = "", what = "", why = "",
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

    // ========================= 数据连接 CRUD =========================

    /**
     * 新增或更新报表定义”数据连接”子表中的一行（按”连接标识”幂等 upsert）。
     *
     * <p>连接配置收敛为单个 JSON 入参，键用中文字段名（与”数据连接”子表/{@link ReportDesignConst} 对齐）：
     * 连接标识/连接地址/用户名/密码/数据库驱动/数据库类型（预留 鉴权方式/请求头/连接配置）。
     * 其中”密码”为平台”密码”类型，由平台落库时透明加密；本方法按原值写入即可，不在日志/返回中输出明文。
     * 报表定义经 Owner + 描述字段隐藏标记查询加载，不依赖操作上下文 CM。</p>
     *
     * @param domain          业务域编号
     * @param targetPanelCode 目标面板编号（经 Owner + 描述字段隐藏标记查询报表定义）
     * @param connectionJson  数据连接配置 JSON（键为中文字段名；连接标识/连接地址必填）
     * @return 更新后的报表定义 Form
     * @throws Exception 上下文/子表缺失、JSON 非法或落库失败
     */
    @MethodDeclare(
            label = "保存数据连接", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$"),
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
                    @InputDeclare(name = "connectionJson", label = "数据连接配置", desc = "中文键 JSON"),
            }
    )
    default Form saveDataConnection(String domain, String targetPanelCode, String connectionJson) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(targetPanelCode)) throw new RuntimeException("目标面板编号不能为空");
        if (StrUtil.isBlank(connectionJson)) throw CommonException.Builder.jsonDataEmpty();

        JSONObject conn;
        try {
            conn = JSONUtil.parseObj(connectionJson);
        } catch (Exception e) {
            throw CommonException.Builder.invalidJsonData();
        }
        String connectionId = conn.getStr(ReportDesignConst.FieldName_ConnectionId);
        String url = conn.getStr(ReportDesignConst.FieldName_ConnectionUrl);
        String userName = conn.getStr(ReportDesignConst.FieldName_UserName);
        String password = conn.getStr(ReportDesignConst.FieldName_Password);
        String driver = conn.getStr(ReportDesignConst.FieldName_DbDriver);
        String dbType = conn.getStr(ReportDesignConst.FieldName_DbType);
        if (StrUtil.isBlank(connectionId)) throw new RuntimeException("连接标识不能为空");
        if (StrUtil.isBlank(url)) throw new RuntimeException("连接地址不能为空");

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(domain);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(domain);

        try (IDao dao = IDaoService.newIDao()) {
            Form reportDef = loadReportDefByPanelCode(dao, targetPanelCode);

            TableData td = reportDef.getTable(ReportDesignConst.Table_DataConnection);
            if (td == null) {
                throw new RuntimeException("报表定义不含“" + ReportDesignConst.Table_DataConnection + "”子表");
            }

            Form row = null;
            for (Form r : td.getRows()) {
                if (connectionId.equals(r.getString(ReportDesignConst.FieldName_ConnectionId))) {
                    row = r;
                    break;
                }
            }
            boolean isNew = (row == null);
            if (isNew) {
                row = Op.newForm(td.getFormModelId());
                row.setAttrValue(ReportDesignConst.FieldName_ConnectionId, connectionId);
            }
            row.setAttrValue(ReportDesignConst.FieldName_ConnectionUrl, url);
            row.setAttrValue(ReportDesignConst.FieldName_UserName, StrUtil.blankToDefault(userName, ""));
            // 密码字段需要转为 Password 对象
            if (StrUtil.isNotBlank(password)) {
                row.setAttrValue(ReportDesignConst.FieldName_Password, new gpf.adur.data.Password().setValue(password));
            }
            row.setAttrValue(ReportDesignConst.FieldName_DbDriver, StrUtil.blankToDefault(driver, ""));
            row.setAttrValue(ReportDesignConst.FieldName_DbType, StrUtil.blankToDefault(dbType, ""));
            if (isNew) td.add(row);

            reportDef.setAttrValue(ReportDesignConst.Table_DataConnection, td);

            reportDef = IFormMgr.get().updateForm(null, dao, reportDef, observer);
            dao.commit();
            return reportDef;
        }
    }

    /**
     * 删除报表定义”数据连接”子表中”连接标识”匹配的行。
     *
     * @param domain          业务域编号
     * @param targetPanelCode 目标面板编号（经 Owner + 描述字段隐藏标记查询报表定义）
     * @param connectionId    连接标识
     * @return 更新后的报表定义 Form
     * @throws Exception 面板/报表定义/子表缺失或落库失败
     */
    @MethodDeclare(
            label = "删除数据连接", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "domain", label = "业务域编号", desc = "", exampleValue = "$domain$"),
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
                    @InputDeclare(name = "connectionId", label = "连接标识", desc = ""),
            }
    )
    default Form deleteDataConnection(String domain, String targetPanelCode, String connectionId) throws Exception {
        if (StrUtil.isBlank(domain)) throw DomainException.Builder.busDomainCodeEmpty();
        if (StrUtil.isBlank(targetPanelCode)) throw new RuntimeException("目标面板编号不能为空");
        if (StrUtil.isBlank(connectionId)) throw new RuntimeException("连接标识不能为空");

        OctoDomainOpObserver observer = Op.getOctoDomainOpObserver(domain);
        if (observer == null) throw DomainException.Builder.notFoundWithCode(domain);

        try (IDao dao = IDaoService.newIDao()) {
            Form reportDef = loadReportDefByPanelCode(dao, targetPanelCode);

            TableData td = reportDef.getTable(ReportDesignConst.Table_DataConnection);
            if (td == null || Op.isEmpty(td)) return reportDef;

            List<Form> remain = new ArrayList<>();
            for (Form r : td.getRows()) {
                if (!connectionId.equals(r.getString(ReportDesignConst.FieldName_ConnectionId))) {
                    remain.add(r);
                }
            }
            TableData newTd = new TableData(td.getFormModelId());
            for (Form r : remain) newTd.add(r);
            reportDef.setAttrValue(ReportDesignConst.Table_DataConnection, newTd);

            reportDef = IFormMgr.get().updateForm(null, dao, reportDef, observer);
            dao.commit();
            return reportDef;
        }
    }

    /**
     * 列出报表定义”数据连接”子表的所有连接（密码字段脱敏，不回传明文）。
     *
     * @param targetPanelCode 目标面板编号（经 Owner + 描述字段隐藏标记查询报表定义）
     * @return 连接列表，每项含 连接标识/连接地址/用户名/数据库驱动/数据库类型/密码(脱敏)
     * @throws Exception 面板/报表定义加载失败
     */
    @MethodDeclare(
            label = "获取数据连接列表", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
            }
    )
    default List<Map<String, Object>> listDataConnections(String targetPanelCode) throws Exception {
        if (StrUtil.isBlank(targetPanelCode)) throw new RuntimeException("目标面板编号不能为空");

        List<Map<String, Object>> result = new ArrayList<>();
        try (IDao dao = IDaoService.newIDao()) {
            Form reportDef = loadReportDefByPanelCode(dao, targetPanelCode);
            TableData td = reportDef.getTable(ReportDesignConst.Table_DataConnection);
            if (td == null || Op.isEmpty(td)) return result;

            for (Form r : td.getRows()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put(ReportDesignConst.FieldName_ConnectionId, r.getString(ReportDesignConst.FieldName_ConnectionId));
                item.put(ReportDesignConst.FieldName_ConnectionUrl, r.getString(ReportDesignConst.FieldName_ConnectionUrl));
                item.put(ReportDesignConst.FieldName_UserName, r.getString(ReportDesignConst.FieldName_UserName));
                item.put(ReportDesignConst.FieldName_DbDriver, r.getString(ReportDesignConst.FieldName_DbDriver));
                item.put(ReportDesignConst.FieldName_DbType, r.getString(ReportDesignConst.FieldName_DbType));
                // 密码字段脱敏：从 Password 对象获取值后脱敏
                gpf.adur.data.Password passwordObj = r.getPassword(ReportDesignConst.FieldName_Password);
                String rawPwd = (passwordObj != null) ? passwordObj.getValue() : "";
                item.put(ReportDesignConst.FieldName_Password, maskPassword(rawPwd));
                result.add(item);
            }
            return result;
        }
    }

    // ========================= 测试连接 / 取数 =========================

    /**
     * 测试数据连接：经 Owner + 描述字段隐藏标记查询报表定义（含真实密码）→
     * 构建数据源 → 重建连接池 + 轻量探活查询（{@code SELECT 1}）。
     *
     * <p>报表设计器恒为编辑态（报表已发布、已有 Owner + 描述字段隐藏标记绑定），故按面板编号通过 Owner + 描述字段隐藏标记查询报表定义，
     * 与取数一致；需先”保存连接”（saveDataConnection 已把连接落入报表定义子表）后再测试。</p>
     *
     * @param targetPanelCode 目标面板编号（经 Owner + 描述字段隐藏标记查询报表定义）
     * @param connectionId    连接标识
     * @return {@code {success:boolean, message:String}}
     * @throws Exception 解析报表定义/数据连接失败
     */
    @MethodDeclare(
            label = "测试数据连接", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
                    @InputDeclare(name = "connectionId", label = "连接标识", desc = ""),
            }
    )
    default Map<String, Object> testConnection(String targetPanelCode, String connectionId) throws Exception {
        if (StrUtil.isBlank(targetPanelCode)) throw new RuntimeException("目标面板编号不能为空");
        if (StrUtil.isBlank(connectionId)) throw new RuntimeException("连接标识不能为空");

        Map<String, Object> result = new LinkedHashMap<>();
        try (IDao dao = IDaoService.newIDao()) {
            Form reportDef = loadReportDefByPanelCode(dao, targetPanelCode);
            Form connRow = findConnectionRow(reportDef, connectionId);
            ReportJdbcDataSource ds = buildDataSource(connRow, reportDef.getUuid(), connectionId);

            IJDBCService svc = IJDBCService.get();
            svc.rebuildDbPool(ds, false);
            svc.queryDataWithStatement(ds, new SqlStatementDto(ReportDesignConst.ProbeSql));

            result.put("success", true);
            result.put("message", "连接成功");
        } catch (Exception e) {
            // 不输出密码：仅回传异常消息
            result.put("success", false);
            result.put("message", StrUtil.blankToDefault(e.getMessage(), "连接失败"));
        }
        return result;
    }

    /**
     * 运行态真参数化取数（Owner + 描述字段隐藏标记查询，零硬编码面板编号）。
     *
     * <p>流程：按面板编号通过 Owner + 描述字段隐藏标记查询报表定义 → 选”数据连接”子表中”连接标识”匹配行构建数据源 → 用 {@link ReportQueryHelper#buildSqlStatement}
     * 组装真参数化语句（{@code $name} → {@code ?} 绑定，集合自动展开 IN 列表，绝不拼 SQL 文本，
     * 含基础 LIMIT/OFFSET 分页）→ {@link IJDBCService#queryDataWithStatement} 执行 →
     * 规整为 {@code {columns, rows, total}}。</p>
     *
     * @param targetPanelCode 面板编号（运行态/设计态统一，零硬编码）
     * @param connectionId 连接标识
     * @param sqlTemplate  SQL 模板（保留 {@code $name} 占位）
     * @param params       运行态绑定值（可空）；集合/数组展开为 IN 列表
     * @param pageNo       页码（从 1 开始，可空）
     * @param pageSize     页大小（可空）
     * @return {@code {columns, rows, total}}
     * @throws Exception 解析报表定义/数据连接或查询失败
     */
    @MethodDeclare(
            label = "执行报表取数", how = "", what = "", why = "",
            inputs = {
                    @InputDeclare(name = "targetPanelCode", label = "目标面板编号", desc = ""),
                    @InputDeclare(name = "connectionId", label = "连接标识", desc = ""),
                    @InputDeclare(name = "sqlTemplate", label = "SQL模板", desc = ""),
                    @InputDeclare(name = "params", label = "绑定参数", desc = "", nullable = true),
                    @InputDeclare(name = "pageNo", label = "页码", desc = "", nullable = true),
                    @InputDeclare(name = "pageSize", label = "页大小", desc = "", nullable = true),
            }
    )
    default Map<String, Object> executeReportQuery(String targetPanelCode, String connectionId, String sqlTemplate,
                                                   Map<String, Object> params, Integer pageNo, Integer pageSize) throws Exception {
        if (StrUtil.isBlank(targetPanelCode)) throw new RuntimeException("目标面板编号不能为空");
        if (StrUtil.isBlank(connectionId)) throw new RuntimeException("连接标识不能为空");
        if (StrUtil.isBlank(sqlTemplate)) throw new RuntimeException("SQL 模板不能为空");

        try (IDao dao = IDaoService.newIDao()) {
            Form reportDef = loadReportDefByPanelCode(dao, targetPanelCode);
            Form connRow = findConnectionRow(reportDef, connectionId);
            ReportJdbcDataSource ds = buildDataSource(connRow, targetPanelCode, connectionId);

            SqlStatementDto stmt = ReportQueryHelper.buildSqlStatement(sqlTemplate, params, pageNo, pageSize);
            PairDto<List<JdbcMetaInfoDto>, List<List<String>>> pair =
                    IJDBCService.get().queryDataWithStatement(ds, stmt);
            return ReportQueryHelper.buildQueryResult(pair);
        }
    }

    // ========================= 支撑方法 =========================

    /**
     * 按面板编号通过 Owner + 描述字段隐藏标记查询报表定义 Form。
     *
     * <p>查询步骤：
     * <ol>
     *   <li>按面板编号查面板设计 Form</li>
     *   <li>从"面板描述"字段提取隐藏标记 {@code <!--REPORT_DEF_MODEL_ID:xxx-->} 获取 FormModelId</li>
     *   <li>读取 {@code Form.Owner} 得报表定义的 uuid</li>
     *   <li>查询报表定义 Form</li>
     * </ol>
     * </p>
     *
     * @param dao       数据访问会话
     * @param panelCode 面板编号
     * @return 报表定义 Form
     * @throws Exception 面板不存在 / 未绑定报表定义 / 报表定义加载失败
     */
    default Form loadReportDefByPanelCode(IDao dao, String panelCode) throws Exception {
        Form panel = Op.queryFormByValueMatchAnyField(dao, WorkBenchConst.FormModelId_PanelDesign,
                CollUtil.newHashSet(ReportDesignConst.FieldName_PanelCode), panelCode, null);
        if (panel == null) throw PanelDesignException.Builder.notFoundWithCode(panelCode);

        // 从面板描述提取 FormModelId
        String panelDesc = panel.getString("面板描述");
        String reportDefFormModelId = extractReportDefModelId(panelDesc);
        if (StrUtil.isBlank(reportDefFormModelId)) {
            throw new RuntimeException("面板[" + panelCode + "]描述中未找到报表定义模型标记");
        }

        // 从 Owner 获取 uuid
        String reportDefUuid = panel.getStringByCode(Form.Owner);
        if (StrUtil.isBlank(reportDefUuid)) {
            throw new RuntimeException("面板[" + panelCode + "]未绑定报表定义（Form.Owner 为空）");
        }

        Form reportDef = IFormMgr.get().queryForm(dao, reportDefFormModelId, reportDefUuid);
        if (reportDef == null) {
            throw new RuntimeException("未找到面板[" + panelCode + "]关联的报表定义");
        }
        return reportDef;
    }

    /**
     * 从面板描述中提取报表定义模型 ID。
     *
     * @param panelDesc 面板描述
     * @return FormModelId，未找到返回 null
     */
    default String extractReportDefModelId(String panelDesc) {
        if (StrUtil.isBlank(panelDesc)) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<!--REPORT_DEF_MODEL_ID:(.*?)-->");
        java.util.regex.Matcher matcher = pattern.matcher(panelDesc);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 从报表定义“数据连接”子表中找“连接标识”匹配的行。
     *
     * @param reportDef    报表定义 Form
     * @param connectionId 连接标识
     * @return 匹配的连接行 Form
     * @throws Exception 子表为空或无匹配行
     */
    default Form findConnectionRow(Form reportDef, String connectionId) throws Exception {
        TableData td = reportDef.getTable(ReportDesignConst.Table_DataConnection);
        if (td == null || Op.isEmpty(td)) {
            throw new RuntimeException("报表定义不含数据连接配置");
        }
        for (Form r : td.getRows()) {
            if (connectionId.equals(r.getString(ReportDesignConst.FieldName_ConnectionId))) {
                return r;
            }
        }
        throw new RuntimeException("未找到连接标识[" + connectionId + "]对应的数据连接");
    }

    /**
     * 用一行”数据连接”配置构建自定义数据源（连接池稳定唯一 key = panelCode.connectionId）。
     *
     * @param connRow      数据连接行 Form
     * @param panelCode    面板编号
     * @param connectionId 连接标识
     * @return 自定义数据源
     * @throws Exception 读取字段失败
     */
    default ReportJdbcDataSource buildDataSource(Form connRow, String panelCode, String connectionId) throws Exception {
        String url = connRow.getString(ReportDesignConst.FieldName_ConnectionUrl);
        String user = connRow.getString(ReportDesignConst.FieldName_UserName);
        // 密码字段是 Password 类型，需要用 getPassword() 获取对象再调用 getValue() 获取明文
        gpf.adur.data.Password passwordObj = connRow.getPassword(ReportDesignConst.FieldName_Password);
        String pwd = (passwordObj != null) ? passwordObj.getValue() : "";
        String driver = connRow.getString(ReportDesignConst.FieldName_DbDriver);
        String dbType = connRow.getString(ReportDesignConst.FieldName_DbType);
        return ReportJdbcDataSource.of(panelCode, connectionId, url, user, pwd, driver, dbType);
    }

    /**
     * 密码脱敏：非空一律返回固定掩码，绝不回传明文。
     *
     * @param raw 原始密码
     * @return 脱敏后的展示值
     */
    default String maskPassword(String raw) {
        return StrUtil.isBlank(raw) ? "" : "******";
    }
}
