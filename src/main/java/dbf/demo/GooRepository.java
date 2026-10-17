package dbf.demo;

import dbf.demo.domain.Goo;

import javax.sql.DataSource;
import java.util.Set;

public class GooRepository {

    private final DataSource dataSource;

    public GooRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Set<Goo> getAll() {
        return Set.of();
    }

}
