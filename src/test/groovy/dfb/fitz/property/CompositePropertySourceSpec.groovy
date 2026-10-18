package dfb.fitz.property

import spock.lang.Specification

import java.time.Duration

class CompositePropertySourceSpec extends Specification {

    def "polls individual child sources independently and only emits events when effective merged values change"() {
        given:
        def lowPriorityMap = [
                "database.url"     : "jdbc://default/db",
                "database.username": "default_user",
                "app.timeout"      : "30"
        ]
        def highPriorityMap = [
                "database.url": "jdbc://local/db"
        ]

        def lowPolling = new PollingPropertySource(
                { -> new LinkedHashMap<>(lowPriorityMap) } as PropertySource,
                Duration.ofMinutes(1)
        )
        def highPolling = new PollingPropertySource(
                { -> new LinkedHashMap<>(highPriorityMap) } as PropertySource,
                Duration.ofMinutes(1)
        )

        def composite = new CompositePropertySource(lowPolling, highPolling)
        List<PropertyChangeEvent> receivedEvents = []
        composite.registerListener { PropertyChangeEvent e -> receivedEvents.add(e) }

        when: "a property changes in the low-priority source, but is overridden by the high-priority source"
        lowPriorityMap["database.url"] = "jdbc://changed-default/db"
        lowPolling.poll()

        then: "no change event is emitted because the effective value is still overridden by highPolling"
        receivedEvents.isEmpty()
        composite.getProperty("database.url") == "jdbc://local/db"

        when: "a non-overridden property changes in the low-priority source"
        lowPriorityMap["app.timeout"] = "60"
        lowPolling.poll()

        then: "a change event is emitted for the non-overridden property"
        receivedEvents.size() == 1
        receivedEvents[0].changes.collect { [it.key, it.oldValue, it.newValue] } == [
                ["app.timeout", "30", "60"]
        ]
        composite.getProperty("app.timeout") == "60"

        when: "the high-priority source removes its override"
        highPriorityMap.remove("database.url")
        highPolling.poll()

        then: "the composite source falls back to the low-priority source value and emits a change event"
        receivedEvents.size() == 2
        receivedEvents[1].changes.collect { [it.key, it.oldValue, it.newValue] } == [
                ["database.url", "jdbc://local/db", "jdbc://changed-default/db"]
        ]
        composite.getProperty("database.url") == "jdbc://changed-default/db"

        cleanup:
        composite.close()
    }
}
