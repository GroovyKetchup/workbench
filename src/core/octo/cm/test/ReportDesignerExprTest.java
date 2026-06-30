package octo.cm.test;

import cmn.dto.PairDto;
import cmn.dto.SqlStatementDto;
import cmn.dto.sql.dql.JdbcMetaInfoDto;
import cmn.enums.sql.DBTypeEnum;
import octo.cm.util.ReportJdbcDataSource;
import octo.cm.util.ReportQueryHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表设计器 Phase3 纯逻辑单元测试（不依赖 BAP 运行时，可直接 java 运行）。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>{@link ReportQueryHelper#buildSqlStatement}：真参数化（{@code $name}→{@code ?}）、
 *   IN 列表自动展开、绝不把 params 拼进 SQL 文本、基础分页 LIMIT/OFFSET 走绑定参数。</li>
 *   <li>{@link ReportQueryHelper#buildQueryResult}：表头+二维数据规整为 columns/rows/total。</li>
 *   <li>{@link ReportJdbcDataSource}：连接池稳定唯一 key = panelCode.connectionId；
 *   数据库类型按名称/驱动解析。</li>
 * </ul>
 *
 * <p>需 BAP 运行时的方法（publishToPanelDesign / takeEffectPanelDesign / 数据连接 CRUD /
 * testConnection / executeReportQuery 的 DB 执行段）依赖 Form/IFormMgr/IJDBCService，
 * 这些在 fork 单测中无法启动 BAP，故此处只对纯逻辑做断言，运行态方法标注“需 BAP 运行时”。</p>
 */
public class ReportDesignerExprTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testRealParameterizationNoConcat();
        testInListExpansion();
        testPaginationBound();
        testBuildQueryResult();
        testDataSourceName();
        testDataSourceDbType();

        System.out.println("==== RESULT: passed=" + passed + ", failed=" + failed + " ====");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** $param 必须变成 ? 绑定，敏感值绝不出现在 SQL 文本中。 */
    private static void testRealParameterizationNoConcat() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", "SECRET_VALUE_123");
        params.put("age", 30);
        SqlStatementDto stmt = ReportQueryHelper.buildSqlStatement(
                "select * from t where name = $name and age > $age", params, null, null);

        String sql = stmt.toSql();
        List<Object> values = stmt.buildParamValueList();

        check("toSql 含 ? 占位", sql.contains("?"));
        check("toSql 不含明文参数值(无SQL拼接)", !sql.contains("SECRET_VALUE_123"));
        check("绑定值按序包含 name", values.contains("SECRET_VALUE_123"));
        check("绑定值按序包含 age", values.contains(30));
        check("绑定值数量=2", values.size() == 2);
    }

    /** 集合参数自动展开为 ?,? 的 IN 列表。 */
    private static void testInListExpansion() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("ids", Arrays.asList("a", "b", "c"));
        SqlStatementDto stmt = ReportQueryHelper.buildSqlStatement(
                "select * from t where id in ($ids)", params, null, null);

        String sql = stmt.toSql();
        List<Object> values = stmt.buildParamValueList();

        check("IN 列表展开为 ?,?,?", sql.contains("?,?,?"));
        check("IN 绑定值=3", values.size() == 3);
        check("IN 绑定值顺序正确", "a".equals(values.get(0)) && "c".equals(values.get(2)));
    }

    /** 基础分页：LIMIT/OFFSET 同样以绑定参数下发，不拼字面量。 */
    private static void testPaginationBound() {
        SqlStatementDto stmt = ReportQueryHelper.buildSqlStatement(
                "select * from t", null, 2, 10);
        String sql = stmt.toSql();
        List<Object> values = stmt.buildParamValueList();

        check("分页追加 LIMIT", sql.toUpperCase().contains("LIMIT"));
        check("分页追加 OFFSET", sql.toUpperCase().contains("OFFSET"));
        check("分页值走绑定(?)", sql.contains("LIMIT ? OFFSET ?"));
        check("limit=10", values.contains(10));
        check("offset=(2-1)*10=10", values.contains(10));
    }

    /** 表头+二维数据规整为 columns/rows/total。 */
    private static void testBuildQueryResult() {
        List<JdbcMetaInfoDto> metas = new ArrayList<>();
        metas.add(meta("id", "编号", "INT"));
        metas.add(meta("name", "名称", "VARCHAR"));

        List<List<String>> data = new ArrayList<>();
        data.add(Arrays.asList("1", "张三"));
        data.add(Arrays.asList("2", "李四"));

        PairDto<List<JdbcMetaInfoDto>, List<List<String>>> pair = new PairDto<>(metas, data);
        Map<String, Object> result = ReportQueryHelper.buildQueryResult(pair);

        Object columns = result.get("columns");
        Object rows = result.get("rows");
        check("total=2", Integer.valueOf(2).equals(result.get("total")));
        check("columns 为列表且=2", columns instanceof List && ((List<?>) columns).size() == 2);
        check("rows 为列表且=2", rows instanceof List && ((List<?>) rows).size() == 2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rowList = (List<Map<String, Object>>) rows;
        check("行按列名映射", "张三".equals(rowList.get(0).get("name")) && "2".equals(rowList.get(1).get("id")));
    }

    /** 连接池稳定唯一 key = panelCode + "." + connectionId。 */
    private static void testDataSourceName() {
        ReportJdbcDataSource ds = ReportJdbcDataSource.of(
                "IML_00018", "connA", "jdbc:mysql://h/db", "u", "p", "com.mysql.cj.jdbc.Driver", "MySQL");
        check("getName=panelCode.connectionId", "IML_00018.connA".equals(ds.getName()));
        check("buildDataSourceName 同样规则", "IML_00018.connA".equals(
                ReportJdbcDataSource.buildDataSourceName("IML_00018", "connA")));
        check("url 透传", "jdbc:mysql://h/db".equals(ds.getUrl()));
    }

    /** 数据库类型：优先按名称解析，名称空时按驱动推断。 */
    private static void testDataSourceDbType() {
        DBTypeEnum byName = ReportJdbcDataSource.of(
                "p", "c", "url", "u", "pw", "drv", "MySQL").getDBType();
        check("按名称解析类型非空", byName != null);

        DBTypeEnum byDriver = ReportJdbcDataSource.of(
                "p", "c", "url", "u", "pw", "com.mysql.cj.jdbc.Driver", null).getDBType();
        check("按驱动推断类型非空", byDriver != null);
    }

    private static JdbcMetaInfoDto meta(String name, String label, String typeName) {
        JdbcMetaInfoDto m = new JdbcMetaInfoDto();
        m.setName(name);
        m.setLabel(label);
        m.setTypeName(typeName);
        return m;
    }

    private static void check(String desc, boolean cond) {
        if (cond) {
            passed++;
            System.out.println("[PASS] " + desc);
        } else {
            failed++;
            System.out.println("[FAIL] " + desc);
        }
    }
}
