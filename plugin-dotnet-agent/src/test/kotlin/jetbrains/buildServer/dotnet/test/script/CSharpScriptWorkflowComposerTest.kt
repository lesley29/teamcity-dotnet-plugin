package jetbrains.buildServer.dotnet.test.script

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.runner.*
import jetbrains.buildServer.rx.Observer
import jetbrains.buildServer.rx.emptyDisposable
import jetbrains.buildServer.script.CSharpScriptWorkflowComposer
import jetbrains.buildServer.script.CommandLineFactory
import jetbrains.buildServer.script.ScriptConstants
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class CSharpScriptWorkflowComposerTest {
    @MockK private lateinit var _buildInfo: BuildInfo
    @MockK private lateinit var _commandLineFactory: CommandLineFactory
    @MockK private lateinit var _context: WorkflowContext
    @MockK private lateinit var _buildOptions: BuildOptions
    @MockK private lateinit var _loggerService: LoggerService
    private val _events = mutableListOf<CommandResultEvent>()

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()

        every { _buildOptions.failBuildOnExitCode } returns true
        every { _context.subscribe(any()) } answers {
            val observer = arg<Observer<CommandResultEvent>>(0)
            for (e in _events) {
                observer.onNext(e)
            }

            emptyDisposable()
        }
    }

    @Test
    fun shouldComposeWokflow() {
        // Given
        val composer = createInstance()
        every { _buildInfo.runType } returns ScriptConstants.RUNNER_TYPE
        val commandLine = CommandLine(null, TargetType.Tool, Path("Abc"), Path("Wd"))
        every { _commandLineFactory.create() } returns commandLine

        // When
        val actualWorkflow = composer.compose(_context, Unit, Workflow())

        // Then
        Assert.assertEquals(actualWorkflow.commandLines.single(), commandLine)
        verify(exactly = 0) { _context.abort(any()) }
    }

    @Test
    fun shouldNotComposeWokflowWhenUnsupportedRunnerType() {
        // Given
        val composer = createInstance()

        // When
        every { _buildInfo.runType } returns "Abc"
        val actualWorkflow = composer.compose(_context, Unit, Workflow())

        // Then
        Assert.assertEquals(actualWorkflow.commandLines.count(), 0)
    }

    @Test
    fun shouldSendBuildProblemWhenExitCodeIsNotZero() {
        // Given
        val composer = createInstance()
        every { _buildInfo.runType } returns ScriptConstants.RUNNER_TYPE
        val commandLine = CommandLine(null, TargetType.Tool, Path("Abc"), Path("Wd"))
        every { _commandLineFactory.create() } returns commandLine
        every{ _loggerService.writeBuildProblem(any(), any(), any()) } returns Unit

        // When
        _events.add(CommandResultExitCode(33, commandLine.Id))
        val actualWorkflow = composer.compose(_context, Unit, Workflow())

        // Then
        Assert.assertEquals(actualWorkflow.commandLines.single(), commandLine)
        verify{ _loggerService.writeBuildProblem("csi_exit_code33", BuildProblemData.TC_EXIT_CODE_TYPE, "Process exited with code 33") }
    }

    @Test
    fun shouldNotSendBuildProblemWhenExitCodeIsNotZeroButFailBuildOnExitCodeIsFalse() {
        // Given
        val composer = createInstance()
        every { _buildInfo.runType } returns ScriptConstants.RUNNER_TYPE
        val commandLine = CommandLine(null, TargetType.Tool, Path("Abc"), Path("Wd"))
        every { _commandLineFactory.create() } returns commandLine

        // When
        _events.add(CommandResultExitCode(33, commandLine.Id))
        every { _buildOptions.failBuildOnExitCode } returns false
        val actualWorkflow = composer.compose(_context, Unit, Workflow())

        // Then
        Assert.assertEquals(actualWorkflow.commandLines.single(), commandLine)
        verify(exactly = 0){ _loggerService.writeBuildProblem(any(), any(), any()) }
    }

    private fun createInstance() = CSharpScriptWorkflowComposer(_buildInfo, _commandLineFactory, _buildOptions, _loggerService)
}