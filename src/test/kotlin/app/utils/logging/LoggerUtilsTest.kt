package app.utils.logging

/**
Please see notes in the file under test (LoggerUtils) and it's class LoggerLayoutAppender.
 */

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import com.nhaarman.mockitokotlin2.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit4.SpringRunner


@RunWith(SpringRunner::class)
@ActiveProfiles("aesCipherService", "httpDataKeyService", "unitTest", "outputToConsole")
@SpringBootTest
@TestPropertySource(properties = [
    "data.table.name=ucfs-data",
    "data.key.service.url=dummy.com:8090",
    "column.family=topic",
    "topic.name=db.a.b",
    "identity.keystore=resources/identity.jks",
    "trust.keystore=resources/truststore.jks",
    "identity.store.password=changeit",
    "identity.key.password=changeit",
    "trust.store.password=changeit",
    "identity.store.alias=cid",
    "hbase.zookeeper.quorum=hbase",
    "aws.region=eu-west-2"
])
class LoggerUtilsTest {

    @Before
    fun setup() {
        overrideLoggerStaticFieldsForTests("topic.name", "test-host", "test-env", "my-app", "v1", "tests")
    }

    @After
    fun tearDown() {
        resetLoggerStaticFieldsForTests()
    }

    fun catchMe1(): Throwable {
        try {
            MakeStacktrace1().callMe1()
        } catch (ex: Exception) {
            return ex
        }
        return RuntimeException("boom")
    }

    fun catchMe2(): Throwable {
        try {
            MakeStacktrace2().callMe2()
        } catch (ex: Exception) {
            return ex
        }
        return RuntimeException("boom")
    }

    fun catchMe3(): Throwable {
        try {
            MakeStacktrace3().callMe3()
        } catch (ex: Exception) {
            return ex
        }
        return RuntimeException("boom")
    }

    class MakeStacktrace1 {
        fun callMe1() {
            throw RuntimeException("boom1 - /:'!@£\$%^&*()")
        }
    }

    class MakeStacktrace2 {
        fun callMe2() {
            throw RuntimeException("boom2")
        }
    }

    class MakeStacktrace3 {
        fun callMe3() {
            throw RuntimeException("boom3")
        }
    }

    @Test
    fun testFormattedTimestamp_WillUseDefaultFormat_WhenCalled() {
        assertEquals("1970-01-01T00:00:00.000", formattedTimestamp(0))
        assertEquals("1973-03-01T23:29:03.210", formattedTimestamp(99876543210))
        assertEquals("A long time ago in a galaxy far, far away....", "292278994-08-17T07:12:55.807", formattedTimestamp(Long.MAX_VALUE))
    }

    @Test
    fun testSemiFormattedTuples_WillFormatAsPartialJson_WhenCalledWithoutMatchingKeyValuePairs() {
        assertEquals(
            "my-message",
            semiFormattedTuples("my-message"))
    }

    @Test
    fun testSemiFormattedTuples_WillFormatAsPartialJson_WhenCalledWithMatchingKeyValuePairs() {
        assertEquals(
            "my-message\", \"key1\":\"value1\", \"key2\":\"value2",
            semiFormattedTuples("my-message", "key1", "value1", "key2", "value2"))
    }

    @Test
    fun testSemiFormattedTuples_WillEscapeJsonInMessageAndTupleValues_WhenCalled() {
        assertEquals(
            "This is almost unreadable, but a necessary test, sorry!",
            "message-\\/:'!@\\u00A3\$%^&*()\\n\\t\\r\", \"key-unchanged\":\"value-\\/:!@\\u00A3\$%^&*()\\n\\t\\r",
            semiFormattedTuples("message-/:'!@£\$%^&*()\n\t\r", "key-unchanged", "value-/:!@£\$%^&*()\n\t\r"))
    }

    @Test
    fun testSemiFormattedTuples_WillFailWithException_WhenCalledWithoutMatchingKeyValuePairs() {
        try {
            semiFormattedTuples("my-message", "key1")
            fail("Expected an IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals(
                "Must have matched key-value pairs but had 1 argument(s)",
                expected.message)
        }
    }

    @Test
    fun testLoggerUtils_Debug_WillFormatAsPartialJson_WhenCalled() {
        val mockLogger: org.slf4j.Logger = mock()
        whenever(mockLogger.isDebugEnabled).thenReturn(true)

        logDebug(mockLogger, "main-message", "key1", "value1", "key2", "value2")

        verify(mockLogger, times(1)).isDebugEnabled
        verify(mockLogger, times(1)).debug("main-message\", \"key1\":\"value1\", \"key2\":\"value2")
        verifyNoMoreInteractions(mockLogger)
    }

    @Test
    fun testLoggerUtils_Info_WillFormatAsPartialJson_WhenCalled() {
        val mockLogger: org.slf4j.Logger = mock()
        whenever(mockLogger.isInfoEnabled).thenReturn(true)

        logInfo(mockLogger, "main-message", "key1", "value1", "key2", "value2")

        verify(mockLogger, times(1)).isInfoEnabled
        verify(mockLogger, times(1)).info("main-message\", \"key1\":\"value1\", \"key2\":\"value2")
        verifyNoMoreInteractions(mockLogger)
    }

    @Test
    fun testLoggerUtils_Error_WillFormatAsPartialJson_WhenCalled() {
        val mockLogger: org.slf4j.Logger = mock()
        whenever(mockLogger.isErrorEnabled).thenReturn(true)

        logError(mockLogger, "main-message", "key1", "value1", "key2", "value2")

        verify(mockLogger, times(1)).isErrorEnabled
        verify(mockLogger, times(1)).error("main-message\", \"key1\":\"value1\", \"key2\":\"value2")
        verifyNoMoreInteractions(mockLogger)
    }

    @Test
    fun testLoggerUtils_Error_WillFormatAsPartialJson_WhenCalledWithKeyValuePairsAndException() {
        val mockLogger: org.slf4j.Logger = mock()
        whenever(mockLogger.isErrorEnabled).thenReturn(true)
        val exception = RuntimeException("boom")

        logError(mockLogger, "main-message", exception, "key1", "value1", "key2", "value2")

        verify(mockLogger, times(1)).isErrorEnabled
        verify(mockLogger, times(1)).error(eq("main-message\", \"key1\":\"value1\", \"key2\":\"value2"), same(exception))
        verifyNoMoreInteractions(mockLogger)
    }

    @Test
    fun testInlineStackTrace_WillRemoveTabsAndNewlinesAndEscapeJsonChars_WhenCalled() {
        val stubThrowable = ThrowableProxy(catchMe1())
        ThrowableProxyUtil.build(stubThrowable, catchMe2(), ThrowableProxy(catchMe3()))

        val trace = "java.lang.RuntimeException: boom1 - /:'!@£\$%^&*()\n" +
            "\tat app.utils.logging.LoggerUtilsTest\$MakeStacktrace2.callMe2(LoggerUtilsTest.kt:87)\n" +
            "\tat app.utils.logging.LoggerUtilsTest.catchMe2(LoggerUtilsTest.kt:63)\n" +
            "\t... 58 common frames omitted\n"

        val throwableStr = ThrowableProxyUtil.asString(stubThrowable)
        assertEquals(trace,
            throwableStr)

        val result = inlineStackTrace(throwableStr)
        assertEquals("java.lang.RuntimeException: boom1 - \\/:'!@\\u00A3\$%^&*() |  at app.utils.logging.LoggerUtilsTest\$MakeStacktrace2.callMe2(LoggerUtilsTest.kt:87) |  at app.utils.logging.LoggerUtilsTest.catchMe2(LoggerUtilsTest.kt:63) |  ... 58 common frames omitted | ", result)
    }

    @Test
    fun testMakeLoggerStaticDataTuples_WillCreatePartialJson_WhenCalled() {
        overrideLoggerStaticFieldsForTests("the.topic","a-host", "b-env", "c-app", "d-version", "e-component")
        assertEquals("\"topic_name\":\"the.topic\", \"hostname\":\"a-host\", \"environment\":\"b-env\", \"application\":\"c-app\", \"app_version\":\"d-version\", \"component\":\"e-component\"", makeLoggerStaticDataTuples())
    }

    @Test
    fun testThrowableProxyEventToString_EmbedsAsJsonKey_WhenCalled() {

        val mockEvent = mock<ILoggingEvent>()
        whenever(mockEvent.timeStamp).thenReturn(9876543210)
        whenever(mockEvent.level).thenReturn(Level.WARN)
        whenever(mockEvent.threadName).thenReturn("my.thread.is.betty")
        whenever(mockEvent.loggerName).thenReturn("logger.name.is.mavis")
        whenever(mockEvent.formattedMessage).thenReturn("some message about stuff")

        val stubThrowable = ThrowableProxy(catchMe1())
        ThrowableProxyUtil.build(stubThrowable, catchMe2(), ThrowableProxy(catchMe3()))
        whenever(mockEvent.throwableProxy).thenReturn(stubThrowable as IThrowableProxy)

        val result = throwableProxyEventToString(mockEvent)

        assertEquals("\"exception\":\"java.lang.RuntimeException: boom1 - \\/:'!@\\u00A3\$%^&*() |  at app.utils.logging.LoggerUtilsTest\$MakeStacktrace2.callMe2(LoggerUtilsTest.kt:87) |  at app.utils.logging.LoggerUtilsTest.catchMe2(LoggerUtilsTest.kt:63) |  ... 58 common frames omitted | \", ", result)
    }

    @Test
    fun testLoggerLayoutAppender_WillReturnEmpty_WhenCalledWithNothing() {
        val result = LoggerLayoutAppender().doLayout(null)
        assertEquals("", result)
    }

    @Test
    fun testLoggerLayoutAppender_WillReturnSkinnyJson_WhenCalledWithEmptyEvent() {
        val result = LoggerLayoutAppender().doLayout(mock<ILoggingEvent>())
        assertEquals(
            "{ \"timestamp\":\"1970-01-01T00:00:00.000\", \"log_level\":\"null\", \"message\":\"null\", \"thread\":\"null\", \"logger\":\"null\", \"topic_name\":\"topic.name\", \"hostname\":\"test-host\", \"environment\":\"test-env\", \"application\":\"my-app\", \"app_version\":\"v1\", \"component\":\"tests\" }\n",
            result)
    }

    @Test
    fun testLoggerLayoutAppender_WillFormatAsJson_WhenCalledWithVanillaMessage() {
        val mockEvent = mock<ILoggingEvent>()
        whenever(mockEvent.timeStamp).thenReturn(9876543210)
        whenever(mockEvent.level).thenReturn(Level.WARN)
        whenever(mockEvent.threadName).thenReturn("my.thread.is.betty")
        whenever(mockEvent.loggerName).thenReturn("logger.name.is.mavis")
        whenever(mockEvent.formattedMessage).thenReturn("some message about stuff")
        whenever(mockEvent.hasCallerData()).thenReturn(false)

        val result = LoggerLayoutAppender().doLayout(mockEvent)
        assertEquals(
            "{ \"timestamp\":\"1970-04-25T07:29:03.210\", \"log_level\":\"WARN\", \"message\":\"some message about stuff\", \"thread\":\"my.thread.is.betty\", \"logger\":\"logger.name.is.mavis\", \"topic_name\":\"topic.name\", \"hostname\":\"test-host\", \"environment\":\"test-env\", \"application\":\"my-app\", \"app_version\":\"v1\", \"component\":\"tests\" }\n",
            result)
    }

    @Test
    fun testLoggerLayoutAppender_WillFormatAsJson_WhenCalledWithEmbeddedTuplesInMessage() {
        val mockEvent = mock<ILoggingEvent>()
        whenever(mockEvent.timeStamp).thenReturn(9876543210)
        whenever(mockEvent.level).thenReturn(Level.WARN)
        whenever(mockEvent.threadName).thenReturn("my.thread.is.betty")
        whenever(mockEvent.loggerName).thenReturn("logger.name.is.mavis")
        val embeddedTokens = semiFormattedTuples("some message about stuff", "key1", "value1", "key2", "value2")
        whenever(mockEvent.formattedMessage).thenReturn(embeddedTokens)
        whenever(mockEvent.hasCallerData()).thenReturn(false)

        val result = LoggerLayoutAppender().doLayout(mockEvent)
        assertEquals(
            "{ \"timestamp\":\"1970-04-25T07:29:03.210\", \"log_level\":\"WARN\", \"message\":\"some message about stuff\", \"key1\":\"value1\", \"key2\":\"value2\", \"thread\":\"my.thread.is.betty\", \"logger\":\"logger.name.is.mavis\", \"topic_name\":\"topic.name\", \"hostname\":\"test-host\", \"environment\":\"test-env\", \"application\":\"my-app\", \"app_version\":\"v1\", \"component\":\"tests\" }\n",
            result)
    }

    @Test
    fun testLoggerLayoutAppender_ShouldNotEscapeTheJsonMessage_AsThatWouldMessWithOurCustomStaticLogMethodsWhichDo() {
        val mockEvent = mock<ILoggingEvent>()
        whenever(mockEvent.timeStamp).thenReturn(9876543210)
        whenever(mockEvent.level).thenReturn(Level.WARN)
        whenever(mockEvent.threadName).thenReturn("my.thread.is.betty")
        whenever(mockEvent.loggerName).thenReturn("logger.name.is.mavis")
        whenever(mockEvent.formattedMessage).thenReturn("message-/:'!@")
        whenever(mockEvent.hasCallerData()).thenReturn(false)

        val result = LoggerLayoutAppender().doLayout(mockEvent)
        assertEquals(
            "The standard logger should not escape json characters that Spring or AWS-utils might send it, sorry",
            "{ \"timestamp\":\"1970-04-25T07:29:03.210\", \"log_level\":\"WARN\", \"message\":\"message-/:'!@\", \"thread\":\"my.thread.is.betty\", \"logger\":\"logger.name.is.mavis\", \"topic_name\":\"topic.name\", \"hostname\":\"test-host\", \"environment\":\"test-env\", \"application\":\"my-app\", \"app_version\":\"v1\", \"component\":\"tests\" }\n",
            result)
    }

    @Test
    fun testLoggerLayoutAppender_ShouldAddExceptions_WhenProvided() {
        val mockEvent = mock<ILoggingEvent>()
        whenever(mockEvent.timeStamp).thenReturn(9876543210)
        whenever(mockEvent.level).thenReturn(Level.WARN)
        whenever(mockEvent.threadName).thenReturn("my.thread.is.betty")
        whenever(mockEvent.loggerName).thenReturn("logger.name.is.mavis")
        whenever(mockEvent.formattedMessage).thenReturn("some message about stuff")

        val stubThrowable = ThrowableProxy(catchMe1())
        ThrowableProxyUtil.build(stubThrowable, catchMe2(), ThrowableProxy(catchMe3()))
        whenever(mockEvent.throwableProxy).thenReturn(stubThrowable as IThrowableProxy)

        val expected = "{ \"timestamp\":\"1970-04-25T07:29:03.210\", \"log_level\":\"WARN\", \"message\":\"some message about stuff\", \"exception\":\"java.lang.RuntimeException: boom1 - \\/:'!@\\u00A3\$%^&*() |  at app.utils.logging.LoggerUtilsTest\$MakeStacktrace2.callMe2(LoggerUtilsTest.kt:87) |  at app.utils.logging.LoggerUtilsTest.catchMe2(LoggerUtilsTest.kt:63) |  ... 58 common frames omitted | \", \"thread\":\"my.thread.is.betty\", \"logger\":\"logger.name.is.mavis\", \"topic_name\":\"topic.name\", \"hostname\":\"test-host\", \"environment\":\"test-env\", \"application\":\"my-app\", \"app_version\":\"v1\", \"component\":\"tests\" }\n"

        val result = LoggerLayoutAppender().doLayout(mockEvent)
        assertEquals(
            "The standard logger should add exception messages",
            expected,
            result)
    }

}