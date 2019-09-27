package jetbrains.buildServer.dotnet.test.agent.runner

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.runner.*
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class LoggerServiceTest {
    @MockK private lateinit var _buildStepContext: BuildStepContext
    @MockK private lateinit var _colorTheme: ColorTheme
    @MockK private lateinit var _buildProgressLogger: BuildProgressLogger

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { _buildStepContext.runnerContext } returns
            mockk<BuildRunnerContext>() {
                every { build } returns
                        mockk<AgentRunningBuild>() {
                            every { buildLogger } returns _buildProgressLogger
                        }
            }

        every { _colorTheme.getAnsiColor(Color.Header) } returns "#Header#"
        every { _colorTheme.getAnsiColor(Color.Default) } returns "#Default#"
        every { _colorTheme.getAnsiColor(Color.Success) } returns "#Success#"
    }

    @DataProvider
    fun testWriteStandardOutput(): Array<Array<out Any?>> {
        return arrayOf(
                arrayOf(listOf(StdOutText("")), listOf("")),
                arrayOf(listOf(StdOutText("text")), listOf("text")),
                arrayOf(listOf(StdOutText("text"), StdOutText("abc", Color.Header)), listOf("text\u001B[#Header#mabc")),
                arrayOf(listOf(StdOutText("abc", Color.Header), StdOutText("text")), listOf("\u001B[#Header#mabc\u001B[0mtext")),
                arrayOf(listOf(StdOutText("abc", Color.Header), StdOutText("text"), StdOutText(" xyz")), listOf("\u001B[#Header#mabc\u001B[0mtext xyz")),
                arrayOf(listOf(StdOutText("text", Color.Header)), listOf("\u001B[#Header#mtext")),
                arrayOf(listOf(StdOutText("text", Color.Header), StdOutText(" abc", Color.Success)), listOf("\u001B[#Header#mtext\u001B[#Success#m abc")),
                arrayOf(listOf(StdOutText("text", Color.Header), StdOutText(" abc", Color.Header)), listOf("\u001B[#Header#mtext abc")),
                arrayOf(listOf(StdOutText("text", Color.Header), StdOutText(" abc", Color.Header), StdOutText(" xyz")), listOf("\u001B[#Header#mtext abc\u001B[0m xyz")))
    }

    @Test(dataProvider = "testWriteStandardOutput")
    fun shouldWriteStandardOutput(output: List<StdOutText>, expectedBuildLog: List<StdOutText>) {
        // Given
        val actualBuildLog = mutableListOf<String>()
        var logger = createInstance()
        every { _buildProgressLogger.message(any()) } answers { actualBuildLog.add(arg(0)) }

        // When
        logger.writeStandardOutput(*output.toTypedArray())

        // Then
        Assert.assertEquals(actualBuildLog, expectedBuildLog)
    }

    private fun createInstance() =
            LoggerServiceImpl(_buildStepContext, _colorTheme)
}