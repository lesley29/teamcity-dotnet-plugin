package jetbrains.buildServer.cmd

import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.runner.*
import jetbrains.buildServer.util.OSType

class CmdWorkflowComposer(
        private val _argumentsService: ArgumentsService,
        private val _environment: Environment,
        private val _virtualContext: VirtualContext,
        private val _pathResolverWorkflowFactory: PathResolverWorkflowFactory)
    : WorkflowComposer {

    override val target: TargetType = TargetType.Host

    override fun compose(context: WorkflowContext, workflow: Workflow) =
            when (_virtualContext.targetOSType) {
                OSType.WINDOWS -> {
                    Workflow(sequence {
                        var cmdExecutable: Path? = null
                        for (originalCommandLine in workflow.commandLines) {
                            when (originalCommandLine.executableFile.extension().toLowerCase()) {
                                "cmd", "bat" -> {
                                    if (cmdExecutable == null ) {
                                        var state = PathResolverState(Path("cmd"))
                                        yieldAll(_pathResolverWorkflowFactory.create(context, state).commandLines)
                                        cmdExecutable = state.resolvedPath
                                    }

                                    yield(CommandLine(
                                            TargetType.Host,
                                            cmdExecutable ?: Path( "cmd"),
                                            originalCommandLine.workingDirectory,
                                            getArguments(originalCommandLine).toList(),
                                            originalCommandLine.environmentVariables,
                                            originalCommandLine.title,
                                            originalCommandLine.description))
                                }
                                else -> yield(originalCommandLine)
                            }
                        }
                    })
                }
                else -> workflow
            }

    private fun getArguments(commandLine: CommandLine) = sequence {
        yield(CommandLineArgument("/D"))
        yield(CommandLineArgument("/C"))
        val args = sequenceOf(commandLine.executableFile.path).plus(commandLine.arguments.map { it.value }).map { _virtualContext.resolvePath(it) }
        yield(CommandLineArgument("\"${_argumentsService.combine(args)}\"", CommandLineArgumentType.Target))
    }
}