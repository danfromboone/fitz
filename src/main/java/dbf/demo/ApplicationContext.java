package dbf.demo;

import dbf.di.Controller;
import dbf.di.DispatcherServlet;
import dbf.di.LazyValue;
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

    LazyValue<List<Controller>> lazyControllers = new LazyValue<>(() -> {
        log.info("Initializing controllers in ApplicationContext");
        return List.of(new FooController());
    });

    LazyValue<DispatcherServlet> lazyDispatcherServlet = new LazyValue<>(() -> {
        log.info("Initializing DispatcherServlet in ApplicationContext");
        return new DispatcherServlet(getControllers());
    });

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletRegistration.Dynamic registration = sce.getServletContext()
                .addServlet("dispatcherServlet", getDispatcherServlet());
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }

    public List<Controller> getControllers() {
        return lazyControllers.get();
    }

    public DispatcherServlet getDispatcherServlet() {
        return lazyDispatcherServlet.get();
    }
}
