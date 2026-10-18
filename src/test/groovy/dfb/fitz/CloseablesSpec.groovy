package dfb.fitz

import spock.lang.Specification

import java.io.Closeable
import java.io.IOException

class CloseablesSpec extends Specification {

    def "closes a Closeable and an AutoCloseable cleanly"() {
        given:
        def closeable = Mock(Closeable)
        def autoCloseable = Mock(AutoCloseable)

        when:
        Closeables.closeQuietly(closeable)
        Closeables.closeQuietly(autoCloseable)

        then:
        1 * closeable.close()
        1 * autoCloseable.close()
    }

    def "catches and logs exceptions thrown by close() without rethrowing"() {
        given:
        def failingCloseable = Mock(Closeable) {
            close() >> { throw new IOException("disk error") }
        }
        def failingAutoCloseable = Mock(AutoCloseable) {
            close() >> { throw new RuntimeException("unexpected failure") }
        }

        when:
        Closeables.closeQuietly(failingCloseable)
        Closeables.closeQuietly(failingAutoCloseable)

        then:
        noExceptionThrown()
    }

    def "handles null and non-closeable objects as a no-op"() {
        given:
        Object plainObject = "not-closeable"
        Object closeableAsObject = Mock(AutoCloseable)

        when:
        Closeables.closeQuietly((AutoCloseable) null)
        Closeables.closeQuietly((Object) null)
        Closeables.closeQuietly(plainObject)
        Closeables.closeQuietly(closeableAsObject)

        then:
        1 * (closeableAsObject as AutoCloseable).close()
        noExceptionThrown()
    }

    def "only closes LazyValue if it has already been initialized"() {
        given:
        def uninitializedResource = Mock(AutoCloseable)
        def initializedResource = Mock(AutoCloseable)

        def uninitializedLazy = LazyValue.of { uninitializedResource }
        def initializedLazy = LazyValue.of { initializedResource }
        initializedLazy.get()

        when:
        Closeables.closeQuietly(uninitializedLazy)
        Closeables.closeQuietly(initializedLazy)

        then:
        !uninitializedLazy.isInitialized()
        0 * uninitializedResource.close()
        1 * initializedResource.close()
    }

    def "closeAllQuietly closes all resources in order even when earlier resources throw"() {
        given:
        List<String> closedOrder = []
        AutoCloseable first = { ->
            closedOrder << "first"
            throw new IOException("first failed")
        }
        Closeable second = { ->
            closedOrder << "second"
        }
        Object nonCloseable = "ignored"
        AutoCloseable third = { ->
            closedOrder << "third"
        }

        when:
        Closeables.closeAllQuietly(first, null, second, nonCloseable, third)

        then:
        noExceptionThrown()
        closedOrder == ["first", "second", "third"]
    }

    def "closeAllQuietly supports Iterable of resources and null inputs"() {
        given:
        def r1 = Mock(AutoCloseable) {
            close() >> { throw new Exception("boom") }
        }
        def r2 = Mock(Closeable)

        when:
        Closeables.closeAllQuietly((AutoCloseable[]) null)
        Closeables.closeAllQuietly((Object[]) null)
        Closeables.closeAllQuietly((Iterable<?>) null)
        Closeables.closeAllQuietly([r1, "plain", null, r2])

        then:
        1 * r1.close()
        1 * r2.close()
        noExceptionThrown()
    }
}
