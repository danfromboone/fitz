package dbf.demo;

import dfb.di.property.MutablePropertySource;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Getter
public class DataContext {
    private static final Logger log = LoggerFactory.getLogger(DataContext.class);

    private final MutableDataSource dataSource;
    private final MutablePropertySource propertySource;

    public DataContext(MutablePropertySource propertySource) {
        this.propertySource = propertySource;

        DatabaseConfig databaseConfig = getDatabaseConfig();
        this.dataSource = new MutableDataSource(databaseConfig);

        propertySource.registerListener("database\\..*", () -> {
            log.info("code=DATA_CONFIG_PROPERTY_CHANGE");
            DatabaseConfig updatedDatabaseConfig = getDatabaseConfig();

            dataSource.setDatabaseConfig(updatedDatabaseConfig);
        });
    }

    public DatabaseConfig getDatabaseConfig(){
        return new DatabaseConfig(
                propertySource.getProperty("database.url"),
                propertySource.getProperty("database.username"),
                propertySource.getProperty("database.password")
        );
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

}
