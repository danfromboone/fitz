package dbf.demo;

import dbf.di.Controller;
import dbf.di.DispatcherServlet;
import dfb.di.property.CompositePropertySource;
import dfb.di.property.FilePropertySource;
import dfb.di.property.MutablePropertySource;
import dfb.di.property.PollingPropertySource;
import dfb.di.property.PropertySource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

@Getter
@WebListener
public class ApplicationContext implements ServletContextListener {
    private static final Logger log = LoggerFactory.getLogger(ApplicationContext.class);

    private final MutablePropertySource propertySource;

    private final DataContext dataContext;
    private final FooRepository fooRepository;
    private final FooService fooService;

    private final GooRepository gooRepository;
    private final GooService gooService;

    private final FooController fooController;

    private final List<Controller> controllers;

    private final DispatcherServlet dispatcherServlet;

    public ApplicationContext() {
        PropertySource defaultProperties = new FilePropertySource("default.properties");
        PropertySource localProperties = new FilePropertySource("local.properties");
        CompositePropertySource compositePropertySource = new CompositePropertySource(
                defaultProperties,
                localProperties
        );
        this.propertySource = new PollingPropertySource(compositePropertySource, Duration.ofSeconds(5));

        this.dataContext = new DataContext(this.propertySource);

        this.fooRepository = new FooRepository(dataContext.getDataSource());
        this.fooService = new FooService(fooRepository);
        this.gooRepository = new GooRepository(dataContext.getDataSource());
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

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (propertySource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Failed to close propertySource", e);
            }
        }
    }
}
