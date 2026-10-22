package dfb.fitz.proxy

import spock.lang.Specification

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

class SupplierProxySpec extends Specification {

    interface SimpleService {
        String greet(String name)
        int calculate(int a, int b)
        void performAction()
        void throwChecked() throws IOException
        void throwRuntime()
    }

    static class SimpleServiceImpl implements SimpleService {
        int actionCount = 0

        @Override
        String greet(String name) {
            return "Hello, ${name}!"
        }

        @Override
        int calculate(int a, int b) {
            return a + b
        }

        @Override
        void performAction() {
            actionCount++
        }

        @Override
        void throwChecked() throws IOException {
            throw new IOException("simulated io error")
        }

        @Override
        void throwRuntime() {
            throw new IllegalStateException("simulated runtime error")
        }

        @Override
        String toString() {
            return "SimpleServiceImpl[actions=${actionCount}]"
        }
    }

    def "delegates method calls with arguments and return values"() {
        given:
        def target = new SimpleServiceImpl()
        def proxy = SupplierProxy.create(SimpleService, { target })

        expect:
        proxy.greet("World") == "Hello, World!"
        proxy.calculate(10, 20) == 30

        when:
        proxy.performAction()

        then:
        target.actionCount == 1
    }

    def "of factory method behaves identically to create"() {
        given:
        def target = new SimpleServiceImpl()
        def proxy = SupplierProxy.of(SimpleService, { target })

        expect:
        proxy.greet("Dan") == "Hello, Dan!"
    }

    def "evaluates supplier on every invocation to support dynamic switching"() {
        given:
        def serviceA = Mock(SimpleService) {
            greet(_) >> "from-A"
        }
        def serviceB = Mock(SimpleService) {
            greet(_) >> "from-B"
        }
        def current = new AtomicReference<SimpleService>(serviceA)
        def proxy = SupplierProxy.create(SimpleService, { current.get() })

        expect:
        proxy.greet("test") == "from-A"

        when:
        current.set(serviceB)

        then:
        proxy.greet("test") == "from-B"
    }

    def "unwraps and rethrows checked exceptions directly"() {
        given:
        def target = new SimpleServiceImpl()
        def proxy = SupplierProxy.create(SimpleService, { target })

        when:
        proxy.throwChecked()

        then:
        def ex = thrown(IOException)
        ex.message == "simulated io error"
    }

    def "unwraps and rethrows unchecked exceptions directly"() {
        given:
        def target = new SimpleServiceImpl()
        def proxy = SupplierProxy.create(SimpleService, { target })

        when:
        proxy.throwRuntime()

        then:
        def ex = thrown(IllegalStateException)
        ex.message == "simulated runtime error"
    }

    def "throws NullPointerException when supplier returns null on method call"() {
        given:
        def proxy = SupplierProxy.create(SimpleService, { (SimpleService) null })

        when:
        proxy.greet("fail")

        then:
        def ex = thrown(NullPointerException)
        ex.message.contains("greet")
    }

    def "validates constructor / factory parameters"() {
        when:
        SupplierProxy.create(null, { Mock(SimpleService) })

        then:
        thrown(NullPointerException)

        when:
        SupplierProxy.create(SimpleService, null)

        then:
        thrown(NullPointerException)

        when:
        SupplierProxy.create(String, { "not-an-interface" })

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("is not an interface")
    }

    def "handles Object methods equals, hashCode, and toString"() {
        given:
        def target = new SimpleServiceImpl()
        def proxy = SupplierProxy.create(SimpleService, { target })

        expect:
        proxy.equals(proxy)
        proxy.toString() == "SimpleServiceImpl[actions=0]"
        proxy.hashCode() == target.hashCode()
        proxy.equals(target)
    }
}
