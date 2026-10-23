package dfb.fitz.jdbc

import com.zaxxer.hikari.HikariDataSource
import spock.lang.Specification

import java.sql.Connection
import java.sql.ResultSet
import java.time.Duration

class MutableDataSourceSpec extends Specification {

    def "DatabaseConfig builder applies defaults and validates constraints"() {
        when:
        def config = DatabaseConfig.builder()
                .url("  jdbc:h2:mem:cfgtest  ")
                .userName("  sa  ")
                .password("  secret  ")
                .maximumPoolSize(5)
                .build()

        then:
        config.url == "jdbc:h2:mem:cfgtest"
        config.userName == "sa"
        config.password == "secret"
        config.driverClassName == "org.h2.Driver"
        config.maximumPoolSize == 5
        config.minimumIdle == 5
        config.connectionTimeout == Duration.ofSeconds(30)
        config.validationTimeout == Duration.ofSeconds(5)
        config.idleTimeout == Duration.ofMinutes(10)
        config.maxLifetime == Duration.ofMinutes(30)

        when: "minimumIdle exceeds maximumPoolSize"
        DatabaseConfig.builder()
                .url("jdbc:h2:mem:cfgtest")
                .userName("sa")
                .password("secret")
                .maximumPoolSize(4)
                .minimumIdle(5)
                .build()

        then:
        thrown(IllegalArgumentException)
    }

    def "DatabaseConfig auto-detects driver class name or accepts explicit driver"() {
        expect:
        DatabaseConfig.autoDetectDriverClassName("jdbc:h2:mem:test") == "org.h2.Driver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:postgresql://localhost/db") == "org.postgresql.Driver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:mysql://localhost/db") == "com.mysql.cj.jdbc.Driver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:mariadb://localhost/db") == "org.mariadb.jdbc.Driver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:oracle:thin:@localhost:1521:xe") == "oracle.jdbc.OracleDriver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:sqlserver://localhost:1433") == "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:sqlite:test.db") == "org.sqlite.JDBC"
        DatabaseConfig.autoDetectDriverClassName("jdbc:derby:memory:test") == "org.apache.derby.jdbc.EmbeddedDriver"
        DatabaseConfig.autoDetectDriverClassName("jdbc:custom:foo") == null

        when:
        def explicit = DatabaseConfig.builder()
                .url("jdbc:custom:foo")
                .userName("sa")
                .password("sa")
                .driverClassName("org.custom.MyDriver")
                .build()

        then:
        explicit.driverClassName == "org.custom.MyDriver"
    }

    def "MutableDataSource delegates JDBC connections to HikariDataSource and updates settings live in-place"() {
        given:
        def initialConfig = DatabaseConfig.builder()
                .url("jdbc:h2:mem:livedb1;DB_CLOSE_DELAY=-1")
                .userName("sa")
                .password("sa")
                .maximumPoolSize(4)
                .minimumIdle(1)
                .build()
        def dataSource = new MutableDataSource(initialConfig)

        when: "obtaining a connection and executing SQL"
        HikariDataSource initialHikari = dataSource.hikariDataSource
        String queryResult
        try (Connection conn = dataSource.connection;
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE items (id INT PRIMARY KEY, name VARCHAR(64))")
            stmt.execute("INSERT INTO items VALUES (1, 'fitz')")
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM items WHERE id = 1")) {
                rs.next()
                queryResult = rs.getString(1)
            }
        }

        then:
        queryResult == "fitz"
        initialHikari.maximumPoolSize == 4
        dataSource.isWrapperFor(MutableDataSource)
        dataSource.isWrapperFor(HikariDataSource)
        dataSource.unwrap(HikariDataSource).is(initialHikari)

        when: "live-updating pool size and timeouts on the same JDBC URL"
        def updatedSameUrlConfig = DatabaseConfig.builder()
                .url("jdbc:h2:mem:livedb1;DB_CLOSE_DELAY=-1")
                .userName("sa")
                .password("sa")
                .maximumPoolSize(8)
                .minimumIdle(2)
                .connectionTimeout(Duration.ofSeconds(15))
                .build()
        dataSource.setDatabaseConfig(updatedSameUrlConfig)

        then: "the same HikariDataSource instance is updated in-place without closing the pool"
        !initialHikari.closed
        dataSource.hikariDataSource.is(initialHikari)
        initialHikari.maximumPoolSize == 8
        initialHikari.minimumIdle == 2
        initialHikari.connectionTimeout == 15000L

        cleanup:
        dataSource.close()
        assert initialHikari.closed
    }

    def "MutableDataSource closes old HikariDataSource and creates a new pool when JDBC URL changes"() {
        given:
        def db1Config = DatabaseConfig.builder()
                .url("jdbc:h2:mem:urlswap1;DB_CLOSE_DELAY=-1")
                .userName("sa")
                .password("sa")
                .maximumPoolSize(2)
                .minimumIdle(1)
                .build()
        def dataSource = new MutableDataSource(db1Config)

        when: "initializing pool against db1"
        try (Connection conn = dataSource.connection;
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE db_id (name VARCHAR(32))")
            stmt.execute("INSERT INTO db_id VALUES ('db1')")
        }
        HikariDataSource db1Hikari = dataSource.hikariDataSource

        and: "switching DatabaseConfig to a different JDBC URL (db2)"
        def db2Config = DatabaseConfig.builder()
                .url("jdbc:h2:mem:urlswap2;DB_CLOSE_DELAY=-1")
                .userName("sa")
                .password("sa")
                .maximumPoolSize(3)
                .minimumIdle(1)
                .build()
        dataSource.setDatabaseConfig(db2Config)

        and: "querying the reconfigured DataSource"
        String db2Name
        try (Connection conn = dataSource.connection;
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE db_id (name VARCHAR(32))")
            stmt.execute("INSERT INTO db_id VALUES ('db2')")
            try (ResultSet rs = stmt.executeQuery("SELECT name FROM db_id")) {
                rs.next()
                db2Name = rs.getString(1)
            }
        }
        HikariDataSource db2Hikari = dataSource.hikariDataSource

        then: "previous HikariDataSource was closed and a new one was created for db2"
        db1Hikari.closed
        !db2Hikari.is(db1Hikari)
        !db2Hikari.closed
        db2Name == "db2"

        cleanup:
        dataSource.close()
    }
}
