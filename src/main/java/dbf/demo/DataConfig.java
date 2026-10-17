package dbf.demo;

import lombok.Getter;

import java.sql.Connection;
import java.util.function.Supplier;

@Getter
public class DataConfig {

    private final Supplier<Connection> connectionSupplier;

    public DataConfig() {
        this.connectionSupplier = () -> null;
    }

}
