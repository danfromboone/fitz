package dbf.demo;

import dbf.demo.domain.Foo;

import javax.sql.DataSource;
import java.util.Set;

public class FooRepository {

    private final DataSource dataSource;

    public FooRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Set<Foo> getAll() {
        return Set.of();
    }

}
