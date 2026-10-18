package dfb.fitz

import dfb.fitz.LazyValue
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

class LazyValueSpec extends Specification {

    def "evaluates supplier lazily and only once"() {
        given:
        def supplier = Mock(Supplier)
        def lazyValue = new LazyValue<>(supplier)

        expect:
        !lazyValue.isInitialized()

        when:
        def first = lazyValue.get()
        def second = lazyValue.get()

        then:
        1 * supplier.get() >> "computed-value"
        lazyValue.isInitialized()
        first == "computed-value"
        second == "computed-value"
    }

    def "caches null return value without calling supplier again"() {
        given:
        def supplier = Mock(Supplier)
        def lazyValue = LazyValue.of(supplier)

        when:
        def first = lazyValue.get()
        def second = lazyValue.get()

        then:
        1 * supplier.get() >> null
        lazyValue.isInitialized()
        first == null
        second == null
    }

    def "is thread-safe under concurrent access and invokes supplier exactly once"() {
        given:
        int threadCount = 16
        def invocations = new AtomicInteger(0)
        def startLatch = new CountDownLatch(1)
        def readyLatch = new CountDownLatch(threadCount)
        def lazyValue = new LazyValue<>({
            invocations.incrementAndGet()
            return "shared-instance"
        })
        def executor = Executors.newFixedThreadPool(threadCount)

        when:
        def futures = (1..threadCount).collect {
            executor.submit({
                readyLatch.countDown()
                startLatch.await()
                return lazyValue.get()
            } as Callable<String>)
        }
        readyLatch.await()
        startLatch.countDown()
        def results = futures.collect { it.get() }
        executor.shutdown()

        then:
        invocations.get() == 1
        results.every { it == "shared-instance" }
    }

    def "throws NullPointerException when constructed with null supplier"() {
        when:
        new LazyValue<>(null)

        then:
        thrown(NullPointerException)
    }
}
