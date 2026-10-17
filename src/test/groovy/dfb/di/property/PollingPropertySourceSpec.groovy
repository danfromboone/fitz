package dfb.di.property

import dfb.di.property.FilePropertySource
import dfb.di.property.PollingPropertySource
import dfb.di.property.PropertyChange
import dfb.di.property.PropertyChangeEvent
import dfb.di.property.PropertyChangeListener
import dfb.di.property.PropertySource
import spock.lang.Specification
import spock.lang.TempDir
import spock.util.concurrent.PollingConditions

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class PollingPropertySourceSpec extends Specification {

    @TempDir
    Path tempDir

    def "emits a single PropertyChangeEvent with added, modified, and removed properties on poll"() {
        given:
        def currentProps = [a: "1", b: "2", c: "3"]
        PropertySource delegate = { -> currentProps } as PropertySource
        def pollingSource = new PollingPropertySource(delegate, Duration.ofHours(1))

        List<PropertyChangeEvent> receivedEvents = []
        pollingSource.addListener { event -> receivedEvents << event }

        when: "properties are modified, added, and removed"
        currentProps = [a: "1", b: "2-updated", d: "4"]
        pollingSource.poll()

        then: "cached properties are updated and a single event with all changes is emitted"
        pollingSource.getProperty("a") == "1"
        pollingSource.getProperty("b") == "2-updated"
        pollingSource.getProperty("c") == null
        pollingSource.getProperty("d") == "4"

        receivedEvents.size() == 1
        receivedEvents[0].changes() == [
                new PropertyChange("b", "2", "2-updated"),
                new PropertyChange("c", "3", null),
                new PropertyChange("d", null, "4")
        ]

        cleanup:
        pollingSource.close()
    }

    def "does not emit an event when properties have not changed"() {
        given:
        def currentProps = [a: "1", b: "2"]
        PropertySource delegate = { -> new LinkedHashMap<>(currentProps) } as PropertySource
        def pollingSource = new PollingPropertySource(delegate, Duration.ofHours(1))

        int eventCount = 0
        pollingSource.addListener { eventCount++ }

        when:
        pollingSource.poll()

        then:
        eventCount == 0

        cleanup:
        pollingSource.close()
    }

    def "detects changes when polling a FilePropertySource and file is modified on disk"() {
        given:
        Path propFile = tempDir.resolve("app.properties")
        Files.writeString(propFile, "host=localhost\nport=8080\n")

        def fileSource = new FilePropertySource(propFile)
        def pollingSource = new PollingPropertySource(fileSource, Duration.ofMillis(25))

        List<PropertyChangeEvent> events = Collections.synchronizedList([])
        pollingSource.addListener { events << it }

        expect:
        pollingSource.getProperty("host") == "localhost"
        pollingSource.getProperty("port") == "8080"

        when: "the file on disk is updated"
        Files.writeString(propFile, "host=localhost\nport=9090\ntimeout=30\n")

        then: "the background poller detects the changes and emits an event"
        new PollingConditions(timeout: 2).eventually {
            assert events.size() == 1
            assert events[0].changes().toSet() == [
                    new PropertyChange("port", "8080", "9090"),
                    new PropertyChange("timeout", null, "30")
            ].toSet()
            assert pollingSource.getProperty("port") == "9090"
            assert pollingSource.getProperty("timeout") == "30"
        }

        cleanup:
        pollingSource.close()
    }

    def "retains previous snapshot when delegate throws an exception during poll"() {
        given:
        boolean shouldFail = false
        PropertySource delegate = { ->
            if (shouldFail) {
                throw new RuntimeException("disk read error")
            }
            return [key: "initial"]
        } as PropertySource

        def pollingSource = new PollingPropertySource(delegate, Duration.ofHours(1))
        List<PropertyChangeEvent> events = []
        pollingSource.addListener { events << it }

        when:
        shouldFail = true
        pollingSource.poll()

        then:
        pollingSource.getProperty("key") == "initial"
        events.isEmpty()

        cleanup:
        pollingSource.close()
    }

    def "removeListener unsubscribes listener from future events"() {
        given:
        def currentProps = [a: "1"]
        PropertySource delegate = { -> currentProps } as PropertySource
        def pollingSource = new PollingPropertySource(delegate, Duration.ofHours(1))

        List<PropertyChangeEvent> events = []
        PropertyChangeListener listener = { events << it }
        pollingSource.addListener(listener)
        pollingSource.removeListener(listener)

        when:
        currentProps = [a: "2"]
        pollingSource.poll()

        then:
        events.isEmpty()
        pollingSource.getProperty("a") == "2"

        cleanup:
        pollingSource.close()
    }
}
