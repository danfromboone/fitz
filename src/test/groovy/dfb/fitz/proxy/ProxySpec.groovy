package dfb.fitz.proxy

import spock.lang.Specification

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

class ProxySpec extends Specification {

    interface Greeter {
        String greet(String name)
    }

    interface Calculator {
        int add(int a, int b)
    }

    static class GreeterImpl implements Greeter {
        @Override
        String greet(String name) {
            return "Hello, ${name}!"
        }
    }

    def "has private constructor"() {
        when:
        def constructor = Proxy.getDeclaredConstructor()

        then:
        Modifier.isPrivate(constructor.modifiers)

        when:
        constructor.accessible = true
        def instance = constructor.newInstance()

        then:
        instance != null
    }

    def "creates typed proxy from InvocationHandler"() {
        given:
        InvocationHandler handler = { proxy, method, args ->
            if (method.name == "greet") {
                return "Hi, " + args[0]
            }
            return null
        }

        when:
        Greeter greeter = Proxy.create(Greeter, handler)

        then:
        greeter != null
        greeter.greet("Dan") == "Hi, Dan"
        Proxy.isProxy(greeter)
        Proxy.isProxyClass(greeter.class)
        Proxy.getInvocationHandler(greeter).is(handler)
    }

    def "creates typed proxy with custom ClassLoader"() {
        given:
        InvocationHandler handler = { proxy, method, args -> "CustomClassLoader: " + args[0] }

        when:
        Greeter greeter = Proxy.create(getClass().classLoader, Greeter, handler)

        then:
        greeter.greet("World") == "CustomClassLoader: World"
    }

    def "creates proxy implementing multiple interfaces"() {
        given:
        InvocationHandler handler = { proxy, method, args ->
            if (method.name == "greet") return "Greetings, " + args[0]
            if (method.name == "add") return ((int) args[0]) + ((int) args[1])
            return null
        }

        when:
        def proxy = Proxy.create(getClass().classLoader, [Greeter, Calculator] as Class<?>[], handler)

        then:
        proxy instanceof Greeter
        proxy instanceof Calculator
        ((Greeter) proxy).greet("Dan") == "Greetings, Dan"
        ((Calculator) proxy).add(2, 3) == 5
    }

    def "creates supplier proxy delegating to supplier"() {
        given:
        def target = new GreeterImpl()
        Greeter greeter = Proxy.supplier(Greeter, { target })

        expect:
        greeter.greet("Fitz") == "Hello, Fitz!"
    }

    def "creates lazy proxy initializing target only once on first invocation"() {
        given:
        def initCount = new AtomicInteger(0)
        Greeter greeter = Proxy.lazy(Greeter, {
            initCount.incrementAndGet()
            return new GreeterImpl()
        })

        expect:
        initCount.get() == 0

        when:
        def res1 = greeter.greet("First")

        then:
        res1 == "Hello, First!"
        initCount.get() == 1

        when:
        def res2 = greeter.greet("Second")

        then:
        res2 == "Hello, Second!"
        initCount.get() == 1
    }

    def "creates delegate proxy pointing to target"() {
        given:
        def target = new GreeterImpl()
        Greeter greeter = Proxy.delegate(Greeter, target)

        expect:
        greeter.greet("Target") == "Hello, Target!"
    }

    def "creates proxy via of() overloads"() {
        given:
        InvocationHandler handler = { proxy, method, args -> "of-handler" }
        Greeter fromHandler = Proxy.of(Greeter, handler)
        Greeter fromSupplier = Proxy.of(Greeter, { new GreeterImpl() })

        expect:
        fromHandler.greet("test") == "of-handler"
        fromSupplier.greet("Fitz") == "Hello, Fitz!"
    }

    def "isProxy correctly identifies proxy objects"() {
        given:
        Greeter proxy = Proxy.delegate(Greeter, new GreeterImpl())
        Greeter regular = new GreeterImpl()

        expect:
        Proxy.isProxy(proxy)
        !Proxy.isProxy(regular)
        !Proxy.isProxy(null)
    }

    def "validates inputs for create and utility methods"() {
        when:
        Proxy.create(null, { p, m, a -> null })
        then:
        thrown(NullPointerException)

        when:
        Proxy.create(Greeter, (InvocationHandler) null)
        then:
        thrown(NullPointerException)

        when:
        Proxy.create(String, { p, m, a -> null })
        then:
        thrown(IllegalArgumentException)

        when:
        Proxy.create(null, [Greeter] as Class<?>[], { p, m, a -> null })
        then:
        notThrown(Throwable)

        when:
        Proxy.create(null, new Class<?>[0], { p, m, a -> null })
        then:
        thrown(IllegalArgumentException)

        when:
        Proxy.lazy(Greeter, null)
        then:
        thrown(NullPointerException)

        when:
        Proxy.delegate(Greeter, null)
        then:
        thrown(NullPointerException)

        when:
        Proxy.isProxyClass(null)
        then:
        thrown(NullPointerException)

        when:
        Proxy.getInvocationHandler(null)
        then:
        thrown(NullPointerException)
    }
}
