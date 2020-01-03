import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.PatternLayout
import spock.lang.Specification

class FileWriterIntegrationTest extends Specification {

    Logger log
    String expected_content = "{\"_id\":{\"someId\":\"RANDOM_GUID\",\"declarationId\":1234},\"@type\":\"V4\",\"type\":\"addressDeclaration\",\"contractId\":1234,\"addressNumber\":{\"type\":\"AddressLine\",\"cryptoId\":1234},\"addressLine2\":null,\"townCity\":{\"type\":\"AddressLine\",\"cryptoId\":1234},\"postcode\":\"SM5 2LE\",\"processId\":1234,\"effectiveDate\":{\"type\":\"SPECIFIC_EFFECTIVE_DATE\",\"date\":20150320,\"knownDate\":20150320},\"paymentEffectiveDate\":{\"type\":\"SPECIFIC_EFFECTIVE_DATE\",\"date\":20150320,\"knownDate\":20150320},\"createdDateTime\":{\"d_date\":\"2015-03-20T12:23:25.183Z\",\"_removedDateTime\":\"should be replaced by _removedDateTime\"},\"_version\":2,\"_removed\":\"should be replaced by _removed\",\"unicodeNull\":\"\",\"unicodeNullwithText\":\"sometext\",\"lineFeedChar\":\"\",\"lineFeedCharWithText\":\"sometext\",\"carriageReturn\":\"\",\"carriageReturnWithText\":\"sometext\",\"carriageReturnLineFeed\":\"\",\"carriageReturnLineFeedWithText\":\"sometext\",\"_lastModifiedDateTime\":{\"d_date\":\"2018-12-14T15:01:02.000+0000\"},\"timestamp\":10}"
    String fileName = System.getenv("FILE_NAME")
    Integer expectedTimestamp = Integer.valueOf(System.getenv("EXPECTED_TIMESTAMP"))
    Integer expectedLineCount = Integer.valueOf(System.getenv("EXPECTED_LINE_COUNT"))

    def setup() {
        def appender = new ConsoleAppender()
        appender.with {
            layout = new PatternLayout("{ timestamp=\"%d\", log_level=\"%p\", test_name=\"%c\", logger=\"%C{1}\", application:\"HTME\", message=\"%m\"}%n")
            threshold = Level.INFO
            activateOptions()
        }
        Logger.getRootLogger().addAppender appender
        log = Logger.getLogger(FileWriterIntegrationTest.class)
    }

    def "Writes the correct records"() {
        given: "hbase is up"
        when: "the table has been populated"
        and: "the process has run"

        log.info(fileName)
        def list = []
        def dir = new File(fileName)
        dir.eachFileRecurse(FileType.FILES) { file ->
            list << file
        }

        list.each {
            log.info(it.path)
        }

        File outputFile = File.createTempFile("temp", ".txt")
        // Get a writer to your new file
        outputFile.withWriter { w ->
            // For each input file path
            list.each { f ->
                // Get a reader for the input file
                new File(f.path).withReader { r ->
                    // And write data from the input into the output
                    w << r
                }
            }
        }
        log.info(outputFile.text)
        int attempts = 0
        log.info("${outputFile}: is file: ${outputFile.isFile()}")
        while (!outputFile.isFile() && ++attempts < 10) {
            log.info("Waiting for population process of file " + fileName)
            sleep(3000)
        }
        assert (outputFile.isFile())

        then: "the latest records have been written"

        def line

        def reader = new BufferedReader(new FileReader(outputFile))
        def lineCount = 0
        while ((line = reader.readLine()) != null) {
            println(line)
            def jsonSlurper = new JsonSlurper()
            def object = jsonSlurper.parse(line.getBytes())
            println(object)
            println(object.getClass())
            def timestamp = object.get('timestamp')
            assert line == expected_content
            lineCount++
            log.info("lineCount = " + lineCount)
            log.info("timestamp = " + timestamp)
            log.info("Checking timestamp = " + expectedTimestamp)
            assert timestamp == expectedTimestamp
        }
        log.info("Checking lineCount = " + expectedLineCount)
        lineCount == expectedLineCount
    }
}