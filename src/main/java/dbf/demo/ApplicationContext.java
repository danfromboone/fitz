package dbf.demo;

import dbf.di.Controller;
import dbf.di.DispatcherServlet;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@WebListener
public class ApplicationContext implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ApplicationContext.class);

    private final DataConfig dataConfig;
    private final FooRepository fooRepository;
    private final FooService fooService;

    private final GooRepository gooRepository;
    private final GooService gooService;

    private final FooController fooController;

    private final List<Controller> controllers;

    private final DispatcherServlet dispatcherServlet;

    public ApplicationContext() {
        this.dataConfig = new DataConfig();

        this.fooRepository = new FooRepository(dataConfig.getConnectionSupplier());
        this.fooService = new FooService(fooRepository);
        this.gooRepository = new GooRepository(dataConfig.getConnectionSupplier());
        this.gooService = new GooService(gooRepository);

        this.fooController = new FooController(fooService);
        this.controllers = List.of(fooController);
        this.dispatcherServlet = new DispatcherServlet(this.controllers);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletRegistration.Dynamic registration = sce.getServletContext()
                .addServlet("dispatcherServlet", dispatcherServlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
