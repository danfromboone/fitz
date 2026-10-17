package dbf.demo

import jakarta.servlet.ServletContext
import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletRegistration
import spock.lang.Specification

class ApplicationContextSpec extends Specification {

    def "wires application components and controllers in ApplicationContext"() {
        given:
        def context = new ApplicationContext()

        expect:
        context.dataConfig != null
        context.fooRepository != null
        context.fooService != null
        context.gooRepository != null
        context.gooService != null
        context.fooController != null
        context.controllers == [context.fooController]
    }

    def "initializes DispatcherServlet with controllers from context"() {
        given:
        def context = new ApplicationContext()

        when:
        def servlet = context.dispatcherServlet

        then:
        servlet != null
        servlet.controllers == context.controllers
        servlet.is(context.dispatcherServlet)
    }

    def "registers DispatcherServlet with ServletContext on contextInitialized"() {
        given:
        def context = new ApplicationContext()
        def registration = Mock(ServletRegistration.Dynamic)
        def servletContext = Mock(ServletContext)
        def event = new ServletContextEvent(servletContext)

        when:
        context.contextInitialized(event)

        then:
        1 * servletContext.addServlet("dispatcherServlet", { it.is(context.dispatcherServlet) }) >> registration
        1 * registration.setLoadOnStartup(1)
        1 * registration.addMapping("/")
    }
}
