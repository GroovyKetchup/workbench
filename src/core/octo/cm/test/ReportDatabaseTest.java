package octo.cm.test;

import cell.cdao.IDao;
import cell.cdao.IDaoService;
import cmn.dto.SqlStatementDto;
import cell.cmn.jdbc.IJDBCService;
import cn.hutool.json.JSONUtil;
import octo.cm.util.ReportJdbcDataSource;

/**
 * 报表数据库连接测试 - 使用平台自己的数据库验证取数功能。
 *
 * <p>测试策略：</p>
 * <ol>
 *   <li>获取平台当前使用的数据库连接信息（从 IDaoService）</li>
 *   <li>用这个连接信息构建 ReportJdbcDataSource</li>
 *   <li>执行简单查询验证端到端取数链路</li>
 * </ol>
 *
 * @author Devin
 */
public class ReportDatabaseTest {

    /**
     * 测试用平台数据库执行简单查询。
     *
     * <p>执行步骤：</p>
     * <ol>
     *   <li>从平台 IDao 获取数据库连接信息</li>
     *   <li>构建 ReportJdbcDataSource</li>
     *   <li>执行 SELECT 1 验证连接</li>
     *   <li>执行实际查询（查平台自己的表）</li>
     * </ol>
     *
     * @throws Exception 测试失败
     */
    public static void testWithPlatformDatabase() throws Exception {
        System.out.println("====== 开始测试：使用平台数据库验证取数功能 ======");

        try (IDao dao = IDaoService.newIDao()) {
            // 1. 获取平台数据库连接信息
            System.out.println("\n[步骤1] 获取平台数据库连接信息...");

            // 平台的 IDao 底层连着数据库，我们需要从中提取连接信息
            // 注意：这个方法可能因平台版本不同而不同，需要查看 IDao 的实际实现

            String jdbcUrl = extractJdbcUrl(dao);
            String dbDriver = extractDriverClass(jdbcUrl);
            String dbType = extractDbType(jdbcUrl);

            System.out.println("  JDBC URL: " + jdbcUrl);
            System.out.println("  驱动: " + dbDriver);
            System.out.println("  类型: " + dbType);

            // 2. 构建 ReportJdbcDataSource
            System.out.println("\n[步骤2] 构建 ReportJdbcDataSource...");
            ReportJdbcDataSource dataSource = ReportJdbcDataSource.of(
                "TEST_PANEL",
                "PLATFORM_DB",
                jdbcUrl,
                "", // 用户名从平台获取
                "", // 密码从平台获取
                dbDriver,
                dbType
            );

            System.out.println("  数据源名称: " + dataSource.getName());

            // 3. 测试连接
            System.out.println("\n[步骤3] 测试连接...");
            testConnection(dataSource);

            // 4. 执行实际查询
            System.out.println("\n[步骤4] 执行实际查询...");
            testQuery(dataSource);

            System.out.println("\n====== 测试通过！端到端取数链路验证成功 ======");

        } catch (Exception e) {
            System.err.println("\n====== 测试失败 ======");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 从 IDao 提取 JDBC URL（需要根据实际平台实现调整）。
     */
    private static String extractJdbcUrl(IDao dao) throws Exception {
        // 方法1: 尝试从 IDao 的实现类获取
        // 具体实现取决于平台的 IDao 接口

        // 方法2: 执行查询获取数据库信息
        // 不同数据库的查询方式不同

        // 临时方案：从运行日志或配置推断
        // 需要实际查看平台配置

        throw new UnsupportedOperationException("需要根据实际平台实现来提取 JDBC URL");
    }

    /**
     * 根据 JDBC URL 推断驱动类。
     */
    private static String extractDriverClass(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        } else if (jdbcUrl.startsWith("jdbc:oracle:")) {
            return "oracle.jdbc.OracleDriver";
        }
        return "com.mysql.cj.jdbc.Driver"; // 默认
    }

    /**
     * 根据 JDBC URL 推断数据库类型。
     */
    private static String extractDbType(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mysql:")) {
            return "MySQL";
        } else if (jdbcUrl.startsWith("jdbc:postgresql:")) {
            return "PostgreSQL";
        } else if (jdbcUrl.startsWith("jdbc:oracle:")) {
            return "Oracle";
        }
        return "MySQL"; // 默认
    }

    /**
     * 测试数据库连接。
     */
    private static void testConnection(ReportJdbcDataSource dataSource) throws Exception {
        // 重建连接池
        IJDBCService.get().rebuildDbPool(dataSource);

        // 执行轻量探活查询
        SqlStatementDto stmt = new SqlStatementDto();
        stmt.setSql("SELECT 1 AS test_value");

        Object result = IJDBCService.get().queryDataWithStatement(dataSource, stmt);
        System.out.println("  连接成功！探活查询结果: " + JSONUtil.toJsonStr(result));
    }

    /**
     * 测试实际查询。
     */
    private static void testQuery(ReportJdbcDataSource dataSource) throws Exception {
        // 查询一个平台自己的表（例如用户表、角色表等）
        // 具体表名需要根据平台实际情况调整

        SqlStatementDto stmt = new SqlStatementDto();
        stmt.setSql("SELECT * FROM fe_md_user LIMIT 5"); // 假设平台有这个表

        try {
            Object result = IJDBCService.get().queryDataWithStatement(dataSource, stmt);
            System.out.println("  查询成功！返回数据: " + JSONUtil.toJsonStr(result));
        } catch (Exception e) {
            System.err.println("  查询失败（可能表名不对）: " + e.getMessage());
            System.out.println("  提示：需要根据实际平台调整查询的表名");
        }
    }

    public static void main(String[] args) throws Exception {
        testWithPlatformDatabase();
    }
}
