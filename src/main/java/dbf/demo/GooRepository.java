package dbf.demo;

import dbf.demo.domain.Goo;

import java.sql.Connection;
import java.util.Set;
import java.util.function.Supplier;

public class GooRepository {

    private final Supplier<Connection> connectionSupplier;

    public GooRepository(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    Set<Goo> getAll() {
        return Set.of();
    }

}
