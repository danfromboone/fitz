package dfb.fitz.jdbc

import spock.lang.Specification

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class JdbcSpec extends Specification {

    Connection connection

    def setup() {
        // In-memory H2 connection for testing
        connection = DriverManager.getConnection("jdbc:h2:mem:jdbctest_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1", "sa", "")
    }

    def cleanup() {
        if (connection != null && !connection.isClosed()) {
            connection.close()
        }
    }

    record User(int id, String name, Integer age) {}

    def "execute runs DDL statements and does not close the connection"() {
        when:
        boolean result = Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64), age INT)")

        then:
        !result // DDL returns false
        !connection.isClosed()

        when: "checking table existence"
        boolean tableExists = Jdbc.execute(connection, "ALTER TABLE users ADD COLUMN email VARCHAR(128)")

        then:
        !tableExists
        !connection.isClosed()
    }

    def "update inserts, updates, and deletes rows and does not close the connection"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64), age INT)")

        when: "inserting rows with varargs parameters including null"
        int rows1 = Jdbc.update(connection, "INSERT INTO users (id, name, age) VALUES (?, ?, ?)", 1, "Alice", 30)
        int rows2 = Jdbc.update(connection, "INSERT INTO users (id, name, age) VALUES (?, ?, ?)", 2, "Bob", null)

        then:
        rows1 == 1
        rows2 == 1
        !connection.isClosed()

        when: "updating with List parameters"
        int updated = Jdbc.update(connection, "UPDATE users SET age = ? WHERE name = ?", [31, "Alice"])

        then:
        updated == 1
        !connection.isClosed()

        when: "deleting row"
        int deleted = Jdbc.update(connection, "DELETE FROM users WHERE id = ?", 2)

        then:
        deleted == 1
        !connection.isClosed()
    }

    def "batchUpdate executes multiple parameterized updates in a single batch"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64), age INT)")

        when: "batch updating with List of Object arrays"
        def batchData = [
                [1, "Alice", 30] as Object[],
                [2, "Bob", 25] as Object[],
                [3, "Charlie", null] as Object[]
        ]
        int[] counts = Jdbc.batchUpdate(connection, "INSERT INTO users (id, name, age) VALUES (?, ?, ?)", batchData)

        then:
        counts.length == 3
        counts.every { it == 1 || it == -2 } // Statement.SUCCESS_NO_INFO is -2 in some drivers
        !connection.isClosed()

        and: "rows are in table"
        List<User> users = Jdbc.query(connection, "SELECT id, name, age FROM users ORDER BY id",
                rs -> new User(rs.getInt("id"), rs.getString("name"), rs.getObject("age") as Integer))
        users.size() == 3
        users[0].name() == "Alice"
        users[1].name() == "Bob"
        users[2].age() == null
    }

    def "batchUpdate supports List of Lists"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE items (id INT PRIMARY KEY, val VARCHAR(32))")

        when:
        def batch = [
                [10, "ten"],
                [20, "twenty"]
        ]
        int[] counts = Jdbc.batchUpdate(connection, "INSERT INTO items (id, val) VALUES (?, ?)", batch)

        then:
        counts.length == 2
        !connection.isClosed()

        and:
        def count = Jdbc.queryOne(connection, "SELECT COUNT(*) FROM items", rs -> rs.getInt(1)).orElse(0)
        count == 2
    }

    def "batchUpdate executes batch of raw SQL statements"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE logs (msg VARCHAR(64))")

        when:
        def statements = [
                "INSERT INTO logs VALUES ('msg1')",
                "INSERT INTO logs VALUES ('msg2')",
                "INSERT INTO logs VALUES ('msg3')"
        ]
        int[] results = Jdbc.batchUpdate(connection, statements)

        then:
        results.length == 3
        !connection.isClosed()

        and:
        def items = Jdbc.query(connection, "SELECT msg FROM logs", rs -> rs.getString("msg"))
        items == ["msg1", "msg2", "msg3"]
    }

    def "query maps multiple rows using RowMapper"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64), age INT)")
        Jdbc.update(connection, "INSERT INTO users VALUES (1, 'Alice', 30), (2, 'Bob', 25)")

        when:
        List<User> users = Jdbc.query(
                connection,
                "SELECT id, name, age FROM users WHERE age >= ? ORDER BY id",
                rs -> new User(rs.getInt("id"), rs.getString("name"), rs.getInt("age")),
                25
        )

        then:
        users.size() == 2
        users[0].name() == "Alice"
        users[1].name() == "Bob"
        !connection.isClosed()
    }

    def "query returns empty list when no rows match"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64))")

        when:
        List<String> names = Jdbc.query(connection, "SELECT name FROM users WHERE id = ?", rs -> rs.getString(1), 999)

        then:
        names.isEmpty()
        !connection.isClosed()
    }

    def "queryOne returns Optional with single mapped object"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64))")
        Jdbc.update(connection, "INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob')")

        when:
        Optional<String> name = Jdbc.queryOne(connection, "SELECT name FROM users WHERE id = ?", rs -> rs.getString("name"), 1)

        then:
        name.isPresent()
        name.get() == "Alice"
        !connection.isClosed()

        when: "querying non-existent row"
        Optional<String> missing = Jdbc.queryOne(connection, "SELECT name FROM users WHERE id = ?", rs -> rs.getString("name"), 99)

        then:
        missing.isEmpty()
        !connection.isClosed()
    }

    def "queryOne throws IllegalStateException when multiple rows are returned"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64))")
        Jdbc.update(connection, "INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob')")

        when:
        Jdbc.queryOne(connection, "SELECT name FROM users", rs -> rs.getString("name"))

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("Expected at most one row")
        !connection.isClosed()
    }

    def "queryFirst returns first row when multiple rows are present without error"() {
        given:
        Jdbc.execute(connection, "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(64))")
        Jdbc.update(connection, "INSERT INTO users VALUES (1, 'Alice'), (2, 'Bob')")

        when:
        Optional<String> first = Jdbc.queryFirst(connection, "SELECT name FROM users ORDER BY id", rs -> rs.getString("name"))

        then:
        first.isPresent()
        first.get() == "Alice"
        !connection.isClosed()

        when: "query returns no rows"
        Optional<String> empty = Jdbc.queryFirst(connection, "SELECT name FROM users WHERE id = 99", rs -> rs.getString("name"))

        then:
        empty.isEmpty()
        !connection.isClosed()
    }

    def "validates required parameters and throws NullPointerException"() {
        when:
        Jdbc.execute((Connection) null, "SELECT 1")

        then:
        thrown(NullPointerException)

        when:
        Jdbc.execute((javax.sql.DataSource) null, "SELECT 1")

        then:
        thrown(NullPointerException)

        when:
        Jdbc.query((Connection) null, "SELECT 1", rs -> rs.getString(1))

        then:
        thrown(NullPointerException)

        when:
        Jdbc.query((javax.sql.DataSource) null, "SELECT 1", rs -> rs.getString(1))

        then:
        thrown(NullPointerException)

        when:
        Jdbc.query(connection, null, rs -> rs.getString(1))

        then:
        thrown(NullPointerException)

        when:
        Jdbc.query(connection, "SELECT 1", null)

        then:
        thrown(NullPointerException)
    }

    def "wraps SQLException in WrappedSqlException on SQL syntax error"() {
        when:
        Jdbc.execute(connection, "INVALID SQL SYNTAX HERE")

        then:
        def ex = thrown(WrappedSqlException)
        ex.cause instanceof SQLException
        !connection.isClosed()
    }

    def "WrappedSqlException preserves message and cause and validates nulls"() {
        given:
        def sqlEx = new SQLException("table not found", "42S02", 42)

        when:
        def wrapped = new WrappedSqlException(sqlEx)

        then:
        wrapped.cause.is(sqlEx)
        wrapped.cause.SQLState == "42S02"
        wrapped.cause.errorCode == 42

        when:
        def wrappedWithMsg = new WrappedSqlException("custom msg", sqlEx)

        then:
        wrappedWithMsg.message == "custom msg"
        wrappedWithMsg.cause.is(sqlEx)

        when:
        new WrappedSqlException(null)

        then:
        thrown(NullPointerException)
    }

    def "DataSource methods execute operations and safely close the connection"() {
        given:
        def dbName = "dstest_" + System.nanoTime()
        def opened = []
        def ds = [
                getConnection: {
                    def conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                    opened << conn
                    return conn
                }
        ] as javax.sql.DataSource

        when: "execute DDL with DataSource"
        Jdbc.execute(ds, "CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(64), price INT)")

        then:
        opened.size() == 1
        opened[-1].isClosed()

        when: "update with DataSource varargs and list"
        Jdbc.update(ds, "INSERT INTO products VALUES (?, ?, ?)", 1, "Book", 20)
        Jdbc.update(ds, "INSERT INTO products VALUES (?, ?, ?)", [2, "Pen", 5])

        then:
        opened.size() == 3
        opened[1].isClosed()
        opened[2].isClosed()

        when: "batchUpdate with DataSource"
        Jdbc.batchUpdate(ds, "INSERT INTO products VALUES (?, ?, ?)", [
                [3, "Notebook", 10] as Object[],
                [4, "Ruler", 3] as Object[]
        ])

        then:
        opened.size() == 4
        opened[3].isClosed()

        when: "batchUpdate with DataSource raw SQLs"
        Jdbc.batchUpdate(ds, ["INSERT INTO products VALUES (5, 'Eraser', 2)"])

        then:
        opened.size() == 5
        opened[4].isClosed()

        when: "query with DataSource"
        List<String> names = Jdbc.query(ds, "SELECT name FROM products ORDER BY id", rs -> rs.getString("name"))

        then:
        names == ["Book", "Pen", "Notebook", "Ruler", "Eraser"]
        opened.size() == 6
        opened[5].isClosed()

        when: "queryOne with DataSource"
        Optional<String> book = Jdbc.queryOne(ds, "SELECT name FROM products WHERE id = ?", rs -> rs.getString(1), 1)

        then:
        book.isPresent()
        book.get() == "Book"
        opened.size() == 7
        opened[6].isClosed()

        when: "queryFirst with DataSource"
        Optional<String> first = Jdbc.queryFirst(ds, "SELECT name FROM products ORDER BY id", rs -> rs.getString(1))

        then:
        first.isPresent()
        first.get() == "Book"
        opened.size() == 8
        opened[7].isClosed()
    }

    def "DataSource methods wrap SQLException and safely close connection on error"() {
        given:
        def dbName = "dstest_err_" + System.nanoTime()
        def opened = []
        def ds = [
                getConnection: {
                    def conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                    opened << conn
                    return conn
                }
        ] as javax.sql.DataSource

        when:
        Jdbc.execute(ds, "INVALID SQL STATEMENT")

        then:
        thrown(WrappedSqlException)
        opened.size() == 1
        opened[0].isClosed()
    }

    def "inTransaction commits all operations on success and closes connection"() {
        given:
        def dbName = "tx_commit_" + System.nanoTime()
        def opened = []
        def ds = [
                getConnection: {
                    def conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                    opened << conn
                    return conn
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)")

        when: "running in transaction"
        def initialOpenedCount = opened.size()
        Jdbc.inTransaction(ds, { ->
            assert Jdbc.isTransactionActive()
            assert Jdbc.isTransactionActive(ds)
            assert Jdbc.currentConnection().isPresent()

            Jdbc.update(ds, "INSERT INTO accounts VALUES (1, 100)")
            Jdbc.update(ds, "INSERT INTO accounts VALUES (2, 200)")
        } as TransactionAction)

        then: "only 1 connection was used for the entire transaction"
        opened.size() == initialOpenedCount + 1
        opened[-1].isClosed()
        !Jdbc.isTransactionActive()

        and: "records were committed"
        def balances = Jdbc.query(ds, "SELECT balance FROM accounts ORDER BY id", rs -> rs.getInt("balance"))
        balances == [100, 200]
    }

    def "inTransaction rolls back all changes when an exception is thrown"() {
        given:
        def dbName = "tx_rollback_" + System.nanoTime()
        def opened = []
        def ds = [
                getConnection: {
                    def conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                    opened << conn
                    return conn
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)")
        Jdbc.update(ds, "INSERT INTO accounts VALUES (1, 100)")

        when:
        Jdbc.inTransaction(ds, { ->
            Jdbc.update(ds, "UPDATE accounts SET balance = balance + 50 WHERE id = 1")
            throw new IllegalStateException("simulated failure")
        } as TransactionAction)

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "simulated failure"
        !Jdbc.isTransactionActive()

        and: "connection was closed"
        opened[-1].isClosed()

        and: "change was rolled back"
        def balance = Jdbc.queryOne(ds, "SELECT balance FROM accounts WHERE id = 1", rs -> rs.getInt("balance")).orElse(0)
        balance == 100
    }

    def "service layer holding DataSource participates in transaction invisibly"() {
        given:
        def dbName = "tx_service_" + System.nanoTime()
        def ds = [
                getConnection: {
                    DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE bank (id INT PRIMARY KEY, amount INT)")
        Jdbc.update(ds, "INSERT INTO bank VALUES (1, 100), (2, 200)")

        // Simulated service that only knows about DataSource and doesn't pass Connection
        def bankService = new Object() {
            void transfer(javax.sql.DataSource dataSource, int fromId, int toId, int amount) {
                Jdbc.update(dataSource, "UPDATE bank SET amount = amount - ? WHERE id = ?", amount, fromId)
                if (amount > 100) {
                    throw new IllegalArgumentException("Transfer limit exceeded")
                }
                Jdbc.update(dataSource, "UPDATE bank SET amount = amount + ? WHERE id = ?", amount, toId)
            }
        }

        when: "transfer succeeds"
        Jdbc.inTransaction(ds, { ->
            bankService.transfer(ds, 1, 2, 40)
        } as TransactionAction)

        then:
        Jdbc.queryOne(ds, "SELECT amount FROM bank WHERE id = 1", rs -> rs.getInt(1)).get() == 60
        Jdbc.queryOne(ds, "SELECT amount FROM bank WHERE id = 2", rs -> rs.getInt(1)).get() == 240

        when: "transfer fails and rolls back"
        Jdbc.inTransaction(ds, { ->
            bankService.transfer(ds, 1, 2, 150)
        } as TransactionAction)

        then:
        thrown(IllegalArgumentException)
        Jdbc.queryOne(ds, "SELECT amount FROM bank WHERE id = 1", rs -> rs.getInt(1)).get() == 60
        Jdbc.queryOne(ds, "SELECT amount FROM bank WHERE id = 2", rs -> rs.getInt(1)).get() == 240
    }

    def "ambient operations without DataSource or Connection work inside inTransaction"() {
        given:
        def dbName = "tx_ambient_" + System.nanoTime()
        def ds = [
                getConnection: {
                    DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE logs (id INT PRIMARY KEY, message VARCHAR(64))")

        when: "calling ambient methods outside transaction"
        Jdbc.update("INSERT INTO logs VALUES (1, 'fail')")

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("No active transaction found")

        when: "calling ambient methods inside inTransaction returning a result"
        String result = Jdbc.inTransaction(ds, { ->
            Jdbc.update("INSERT INTO logs VALUES (?, ?)", 1, "created")
            Jdbc.update("INSERT INTO logs VALUES (?, ?)", 2, "updated")
            return Jdbc.queryOne("SELECT message FROM logs WHERE id = ?", rs -> rs.getString("message"), 1).orElse(null)
        } as TransactionCallback<String>)

        then:
        result == "created"

        and:
        def count = Jdbc.queryOne(ds, "SELECT COUNT(*) FROM logs", rs -> rs.getInt(1)).get()
        count == 2
    }

    def "nested inTransaction joins existing transaction without premature commit"() {
        given:
        def dbName = "tx_nested_" + System.nanoTime()
        def opened = []
        def ds = [
                getConnection: {
                    def conn = DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                    opened << conn
                    return conn
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE tasks (id INT PRIMARY KEY, name VARCHAR(64))")

        when:
        def initialCount = opened.size()
        Jdbc.inTransaction(ds, { ->
            Jdbc.update(ds, "INSERT INTO tasks VALUES (1, 'task1')")

            // Nested inTransaction call on the same DataSource
            Jdbc.inTransaction(ds, { ->
                Jdbc.update(ds, "INSERT INTO tasks VALUES (2, 'task2')")
            } as TransactionAction)

            Jdbc.update(ds, "INSERT INTO tasks VALUES (3, 'task3')")
        } as TransactionAction)

        then: "only 1 connection was acquired from DataSource"
        opened.size() == initialCount + 1
        opened[-1].isClosed()

        and: "all tasks were committed"
        def tasks = Jdbc.query(ds, "SELECT name FROM tasks ORDER BY id", rs -> rs.getString("name"))
        tasks == ["task1", "task2", "task3"]
    }

    def "markRollbackOnly causes rollback and throws WrappedSqlException"() {
        given:
        def dbName = "tx_ro_" + System.nanoTime()
        def ds = [
                getConnection: {
                    DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE items (id INT PRIMARY KEY)")

        when:
        Jdbc.inTransaction(ds, { ->
            Jdbc.update(ds, "INSERT INTO items VALUES (1)")
            Jdbc.markRollbackOnly()
        } as TransactionAction)

        then:
        def ex = thrown(WrappedSqlException)
        ex.message.contains("rollback-only")

        and:
        def count = Jdbc.queryOne(ds, "SELECT COUNT(*) FROM items", rs -> rs.getInt(1)).get()
        count == 0
    }

    def "child thread attempting to join transaction throws IllegalStateException"() {
        given:
        def dbName = "tx_child_join_" + System.nanoTime()
        def ds = [
                getConnection: {
                    DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)")

        Throwable childError = null

        when:
        Jdbc.inTransaction(ds, { ->
            Jdbc.update(ds, "INSERT INTO accounts VALUES (1, 100)")

            // Spawn child thread that attempts to join inTransaction
            Thread child = new Thread({
                try {
                    Jdbc.inTransaction(ds, { ->
                        Jdbc.update(ds, "INSERT INTO accounts VALUES (2, 200)")
                    } as TransactionAction)
                } catch (Throwable t) {
                    childError = t
                }
            })
            child.start()
            child.join()
        } as TransactionAction)

        then:
        childError instanceof IllegalStateException
        childError.message.contains("Child threads (or other threads) must not join or access an existing transaction")

        and: "only parent's write succeeded"
        def ids = Jdbc.query(ds, "SELECT id FROM accounts ORDER BY id", rs -> rs.getInt("id"))
        ids == [1]
    }

    def "child thread attempting to access transaction via DataSource or ambient methods throws IllegalStateException"() {
        given:
        def dbName = "tx_child_access_" + System.nanoTime()
        def ds = [
                getConnection: {
                    DriverManager.getConnection("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1", "sa", "")
                }
        ] as javax.sql.DataSource
        Jdbc.execute(ds, "CREATE TABLE accounts (id INT PRIMARY KEY, balance INT)")

        Throwable childDsError = null
        Throwable childAmbientError = null

        when:
        Jdbc.inTransaction(ds, { ->
            // Child thread tries DataSource update
            Thread child1 = new Thread({
                try {
                    Jdbc.update(ds, "INSERT INTO accounts VALUES (1, 100)")
                } catch (Throwable t) {
                    childDsError = t
                }
            })
            child1.start()
            child1.join()

            // Child thread tries ambient update
            Thread child2 = new Thread({
                try {
                    Jdbc.update("INSERT INTO accounts VALUES (2, 200)")
                } catch (Throwable t) {
                    childAmbientError = t
                }
            })
            child2.start()
            child2.join()
        } as TransactionAction)

        then:
        childDsError instanceof IllegalStateException
        childDsError.message.contains("Child threads (or other threads) must not join or access an existing transaction")

        childAmbientError instanceof IllegalStateException
        childAmbientError.message.contains("Child threads (or other threads) must not join or access an existing transaction")
    }
}
