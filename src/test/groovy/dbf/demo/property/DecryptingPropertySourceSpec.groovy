package dbf.demo.property

import dfb.di.property.CompositePropertySource
import dfb.di.property.DecryptingPropertySource
import dfb.di.property.FilePropertySource
import dfb.di.property.PollingPropertySource
import dfb.di.property.PropertyChange
import dfb.di.property.PropertyChangeEvent
import dfb.di.property.PropertyEncryptor
import dfb.di.property.PropertySource
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

class DecryptingPropertySourceSpec extends Specification {

    @TempDir
    Path tempDir

    def "decrypts ENC(...) values using key file specified by property.encryption.key.path"() {
        given:
        Path keyFile = tempDir.resolve("master.key")
        Files.writeString(keyFile, PropertyEncryptor.generateKeyString())

        String encryptedPassword = PropertyEncryptor.encrypt("s3cr3t-db-pass", keyFile)

        Path propsFile = tempDir.resolve("app.properties")
        Files.writeString(propsFile, """\
property.encryption.key.path=${keyFile.toString().replace('\\', '/')}
database.url=jdbc://localhost/mydb
database.username=admin
database.password=${encryptedPassword}
""")

        def source = new DecryptingPropertySource(new FilePropertySource(propsFile))

        expect:
        source.getProperty("database.url") == "jdbc://localhost/mydb"
        source.getProperty("database.username") == "admin"
        source.getProperty("database.password") == "s3cr3t-db-pass"
        source.properties["database.password"] == "s3cr3t-db-pass"
    }

    def "propagates decrypted PropertyChangeEvents when independently polled child source updates an encrypted property"() {
        given:
        Path keyFile = tempDir.resolve("master.key")
        Files.writeString(keyFile, PropertyEncryptor.generateKeyString())

        def defaultMap = [
                (DecryptingPropertySource.DEFAULT_KEY_PATH_PROPERTY): keyFile.toString(),
                "database.url"                                      : "jdbc://default/db",
                "database.password"                                 : PropertyEncryptor.encrypt("default-pass", keyFile)
        ]
        def localMap = [
                "database.password": PropertyEncryptor.encrypt("initial-local-pass", keyFile)
        ]

        PropertySource defaultSource = { -> defaultMap } as PropertySource
        PollingPropertySource localPollingSource = new PollingPropertySource(
                { -> new LinkedHashMap<>(localMap) } as PropertySource,
                Duration.ofMinutes(1)
        )
        def composite = new CompositePropertySource(defaultSource, localPollingSource)
        def decrypting = new DecryptingPropertySource(composite)

        List<PropertyChangeEvent> events = []
        decrypting.registerListener { PropertyChangeEvent event -> events.add(event) }

        expect:
        decrypting.getProperty("database.password") == "initial-local-pass"

        when: "the encrypted value in the local source is rotated and polled"
        localMap["database.password"] = PropertyEncryptor.encrypt("rotated-local-pass", keyFile)
        localPollingSource.poll()

        then: "listeners receive the decrypted old and new values"
        events.size() == 1
        events[0].changes() == [
                new PropertyChange("database.password", "initial-local-pass", "rotated-local-pass")
        ]
        decrypting.getProperty("database.password") == "rotated-local-pass"

        cleanup:
        decrypting.close()
    }

    def "passes through plain properties when no encrypted values are present even if key path is not set"() {
        given:
        PropertySource plainSource = { -> ["app.name": "di-demo", "app.port": "8080"] } as PropertySource
        def decrypting = new DecryptingPropertySource(plainSource)

        expect:
        decrypting.getProperty("app.name") == "di-demo"
        decrypting.properties == ["app.name": "di-demo", "app.port": "8080"]
    }

    def "throws IllegalStateException when an ENC(...) property is encountered without property.encryption.key.path configured"() {
        given:
        Path keyFile = tempDir.resolve("master.key")
        Files.writeString(keyFile, PropertyEncryptor.generateKeyString())
        String encrypted = PropertyEncryptor.encrypt("secret", keyFile)

        PropertySource sourceWithoutKeyPath = { -> ["secret.value": encrypted] } as PropertySource

        when:
        new DecryptingPropertySource(sourceWithoutKeyPath)

        then:
        thrown(IllegalStateException)
    }
}
