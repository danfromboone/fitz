package dbf.demo;

import dbf.demo.domain.Foo;

import java.sql.Connection;
import java.util.Set;
import java.util.function.Supplier;

public class FooRepository {

    private final Supplier<Connection> connectionSupplier;

    public FooRepository(Supplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    Set<Foo> getAll() {
        return Set.of();
    }

}
