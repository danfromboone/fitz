package dbf.demo

import dfb.di.property.CompositePropertySource
import dfb.di.property.FilePropertySource
import dfb.di.property.PollingPropertySource
import dfb.di.property.PropertySource
import spock.lang.Specification

import java.time.Duration

class DataContextSpec extends Specification {

    def "initializes MutableDataSource with DatabaseConfig from local.properties overriding default.properties"() {
        given:
        def composite = new CompositePropertySource(
                new FilePropertySource("default.properties"),
                new FilePropertySource("local.properties")
        )
        def pollingSource = new PollingPropertySource(composite, Duration.ofMinutes(1))

        when:
        def dataContext = new DataContext(pollingSource)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource

        then:
        dataSource.databaseConfig.url == "jdbc://goo/bar"
        dataSource.databaseConfig.userName == "goo"
        dataSource.databaseConfig.password == "goopassword"

        cleanup:
        pollingSource.close()
    }

    def "atomically swaps DatabaseConfig on MutableDataSource when database.* properties change"() {
        given:
        def dynamicOverrides = [
                "database.url"     : "jdbc://goo/bar",
                "database.username": "goo",
                "database.password": "goopassword"
        ]
        PropertySource mutableMapSource = { -> new LinkedHashMap<>(dynamicOverrides) } as PropertySource
        def composite = new CompositePropertySource(
                new FilePropertySource("default.properties"),
                mutableMapSource
        )
        def pollingSource = new PollingPropertySource(composite, Duration.ofMinutes(1))
        def dataContext = new DataContext(pollingSource)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource
        DatabaseConfig initialConfig = dataSource.databaseConfig

        when: "database properties are updated and polling occurs"
        dynamicOverrides["database.url"] = "jdbc://prod/bar"
        dynamicOverrides["database.username"] = "prod_user"
        dynamicOverrides["database.password"] = "prod_secret"
        pollingSource.poll()

        then: "the same DataSource instance has a new immutable DatabaseConfig swapped in"
        dataContext.dataSource.is(dataSource)
        !dataSource.databaseConfig.is(initialConfig)
        dataSource.databaseConfig.url == "jdbc://prod/bar"
        dataSource.databaseConfig.userName == "prod_user"
        dataSource.databaseConfig.password == "prod_secret"

        cleanup:
        pollingSource.close()
    }

    def "does not trigger database reconfiguration when non-database properties change"() {
        given:
        def props = [
                "database.url"     : "jdbc://goo/bar",
                "database.username": "goo",
                "database.password": "goopassword",
                "app.theme"        : "light"
        ]
        PropertySource mapSource = { -> new LinkedHashMap<>(props) } as PropertySource
        def pollingSource = new PollingPropertySource(mapSource, Duration.ofMinutes(1))

        def dataContext = new DataContext(pollingSource)
        MutableDataSource dataSource = dataContext.dataSource as MutableDataSource
        DatabaseConfig initialConfig = dataSource.databaseConfig

        when: "an unrelated property changes"
        props["app.theme"] = "dark"
        pollingSource.poll()

        then: "the DatabaseConfig instance is untouched"
        dataSource.databaseConfig.is(initialConfig)

        when: "a database property changes"
        props["database.password"] = "rotated_password"
        pollingSource.poll()

        then: "a new DatabaseConfig instance is swapped in"
        !dataSource.databaseConfig.is(initialConfig)
        dataSource.databaseConfig.password == "rotated_password"

        cleanup:
        pollingSource.close()
    }
}
