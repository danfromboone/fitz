package dfb.fitz.proxy

import spock.lang.Specification

import java.io.IOException
import java.lang.reflect.Method
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class InterceptingProxySpec extends Specification {

    interface CalculatorService {
        int add(int a, int b)
        String describe(String topic)
        void ping()
        void failWithChecked() throws IOException
    }

    static class CalculatorServiceImpl implements CalculatorService {
        int pingCount = 0

        @Override
        int add(int a, int b) {
            return a + b
        }

        @Override
        String describe(String topic) {
            return "Topic: " + topic
        }

        @Override
        void ping() {
            pingCount++
        }

        @Override
        void failWithChecked() throws IOException {
            throw new IOException("simulated failure")
        }
    }

    def "executes before advice before targeted method"() {
        given:
        def target = new CalculatorServiceImpl()
        def invoked = new AtomicBoolean(false)
        def capturedMethod = ""
        def capturedArgs = []

        CalculatorService proxy = InterceptingProxy.builder(CalculatorService, target)
                .before("add", { tgt, method, args ->
                    invoked.set(true)
                    capturedMethod = method.name
                    capturedArgs = args as List
                })
                .build()

        when:
        def result = proxy.add(3, 4)

        then:
        result == 7
        invoked.get()
        capturedMethod == "add"
        capturedArgs == [3, 4]

        when: "non-targeted method is called"
        invoked.set(false)
        def desc = proxy.describe("Math")

        then:
        desc == "Topic: Math"
        !invoked.get()
    }

    def "executes after advice after targeted method returns"() {
        given:
        def target = new CalculatorServiceImpl()
        def capturedResult = null
        def capturedArgs = []

        CalculatorService proxy = InterceptingProxy.builder(CalculatorService, target)
                .after("add", { tgt, method, args, res ->
                    capturedResult = res
                    capturedArgs = args as List
                })
                .build()

        when:
        def result = proxy.add(10, 20)

        then:
        result == 30
        capturedResult == 30
        capturedArgs == [10, 20]

        when: "void method with after advice on all methods"
        def voidCalled = false
        def voidProxy = Proxy.after(CalculatorService, target, { tgt, method, args, res ->
            if (method.name == "ping") {
                voidCalled = true
                assert res == null
            }
        })
        voidProxy.ping()

        then:
        voidCalled
        target.pingCount == 1
    }

    def "executes around advice and allows modifying arguments and return value"() {
        given:
        def target = new CalculatorServiceImpl()

        CalculatorService proxy = InterceptingProxy.builder(CalculatorService, target)
                .around("describe", { invocation ->
                    // Inspect and modify argument
                    def origArg = invocation.arguments[0] as String
                    invocation.arguments = ["Modified " + origArg] as Object[]

                    // Proceed
                    def originalResult = invocation.proceed()

                    // Modify return value
                    return originalResult + " (intercepted)"
                })
                .build()

        when:
        def result = proxy.describe("Fitz")

        then:
        result == "Topic: Modified Fitz (intercepted)"
    }

    def "around advice can bypass target execution or handle exceptions"() {
        given:
        def target = new CalculatorServiceImpl()

        CalculatorService proxy = InterceptingProxy.builder(CalculatorService, target)
                .around("add", { invocation ->
                    if (invocation.arguments[0] == 0) {
                        return 999 // short-circuit
                    }
                    return invocation.proceed()
                })
                .around("failWithChecked", { invocation ->
                    try {
                        return invocation.proceed()
                    } catch (IOException e) {
                        return null // swallowed
                    }
                })
                .build()

        expect:
        proxy.add(0, 5) == 999
        proxy.add(2, 3) == 5

        when:
        proxy.failWithChecked()

        then:
        noExceptionThrown()
    }

    def "executes combined before, around, and after advice in correct order"() {
        given:
        def target = new CalculatorServiceImpl()
        def order = []

        CalculatorService proxy = Proxy.intercept(CalculatorService, target)
                .before("add", { tgt, m, args -> order << "before" })
                .around("add", { invocation ->
                    order << "around-pre"
                    def res = invocation.proceed()
                    order << "around-post"
                    return res
                })
                .after("add", { tgt, m, args, res -> order << "after" })
                .build()

        when:
        def result = proxy.add(5, 5)

        then:
        result == 10
        order == ["before", "around-pre", "after", "around-post"]
    }

    def "chains multiple around advices"() {
        given:
        def target = new CalculatorServiceImpl()
        def trail = []

        CalculatorService proxy = Proxy.intercept(CalculatorService, target)
                .around("add", { invocation ->
                    trail << "around1-pre"
                    def res = invocation.proceed()
                    trail << "around1-post"
                    return res
                })
                .around("add", { invocation ->
                    trail << "around2-pre"
                    def res = invocation.proceed()
                    trail << "around2-post"
                    return res
                })
                .build()

        when:
        def sum = proxy.add(1, 2)

        then:
        sum == 3
        trail == ["around1-pre", "around2-pre", "around2-post", "around1-post"]
    }

    def "targets methods by Method instance or Predicate"() {
        given:
        def target = new CalculatorServiceImpl()
        Method addMethod = CalculatorService.getMethod("add", int, int)
        def beforeAddCount = new AtomicInteger(0)
        def afterStringCount = new AtomicInteger(0)

        CalculatorService proxy = InterceptingProxy.builder(CalculatorService, target)
                .before(addMethod, { t, m, a -> beforeAddCount.incrementAndGet() })
                .after({ m -> m.returnType == String }, { t, m, a, r -> afterStringCount.incrementAndGet() })
                .around({ m -> m.name == "add" }, { inv -> inv.proceed() })
                .build()

        when:
        proxy.add(1, 1)
        proxy.describe("test")

        then:
        beforeAddCount.get() == 1
        afterStringCount.get() == 1
    }

    def "convenience static methods on Proxy and InterceptingProxy work"() {
        given:
        def target = new CalculatorServiceImpl()
        def beforeHit = false
        def afterHit = false
        def aroundHit = false

        def beforeProxy = Proxy.before(CalculatorService, target, "ping", { t, m, a -> beforeHit = true })
        def afterProxy = Proxy.after(CalculatorService, target, "ping", { t, m, a, r -> afterHit = true })
        def aroundProxy = Proxy.around(CalculatorService, target, "ping", { inv ->
            aroundHit = true
            return inv.proceed()
        })

        when:
        beforeProxy.ping()
        afterProxy.ping()
        aroundProxy.ping()

        then:
        beforeHit
        afterHit
        aroundHit
        target.pingCount == 3
    }

    def "propagates exceptions when advice or target throws"() {
        given:
        def target = new CalculatorServiceImpl()
        CalculatorService proxy = Proxy.before(CalculatorService, target, { t, m, a ->
            throw new IllegalStateException("advice failed")
        })

        when:
        proxy.add(1, 2)

        then:
        def e = thrown(IllegalStateException)
        e.message == "advice failed"

        when: "target throws checked exception without around handling"
        CalculatorService testProxy = InterceptingProxy.builder(CalculatorService, target).build()
        testProxy.failWithChecked()

        then:
        thrown(IOException)
    }

    def "validates inputs"() {
        when:
        InterceptingProxy.builder(null, new CalculatorServiceImpl())
        then:
        thrown(NullPointerException)

        when:
        InterceptingProxy.builder(CalculatorService, null)
        then:
        thrown(NullPointerException)

        when:
        InterceptingProxy.builder(String, "test")
        then:
        thrown(IllegalArgumentException)

        when:
        InterceptingProxy.builder(CalculatorService, new CalculatorServiceImpl()).before((BeforeAdvice) null)
        then:
        thrown(NullPointerException)

        when:
        InterceptingProxy.builder(CalculatorService, new CalculatorServiceImpl()).after((AfterAdvice) null)
        then:
        thrown(NullPointerException)

        when:
        InterceptingProxy.builder(CalculatorService, new CalculatorServiceImpl()).around((AroundAdvice) null)
        then:
        thrown(NullPointerException)
    }
}
