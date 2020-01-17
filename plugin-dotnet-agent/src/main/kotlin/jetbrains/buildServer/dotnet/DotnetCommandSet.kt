/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.dotnet

import jetbrains.buildServer.agent.CommandLineArgument
import jetbrains.buildServer.agent.CommandLineArgumentType
import jetbrains.buildServer.agent.CommandResultEvent
import jetbrains.buildServer.agent.runner.ParameterType
import jetbrains.buildServer.agent.runner.ParametersService
import jetbrains.buildServer.rx.Observer

class DotnetCommandSet(
        private val _parametersService: ParametersService,
        commands: List<DotnetCommand>)
    : CommandSet {

    private val _knownCommands: Map<String, DotnetCommand> = commands.associateBy({ it.commandType.id }, { it })

    override val commands: Sequence<DotnetCommand>
        get() = _parametersService.tryGetParameter(ParameterType.Runner, DotnetConstants.PARAM_COMMAND)?.let {
            _knownCommands[it]?.let { command ->
                getTargetArguments(command).asSequence().map {
                    val targetArguments = TargetArguments(it.arguments.toList().asSequence())
                    CompositeCommand(command, targetArguments)
                }
            }
        } ?: emptySequence()

    private fun getTargetArguments(command: DotnetCommand) = sequence {
        var hasTargets = false
        for (targetArguments in command.targetArguments) {
            yield(targetArguments)
            hasTargets = true
        }

        if (!hasTargets) {
            yield(TargetArguments(emptySequence()))
        }
    }

    class CompositeCommand(
            private val _command: DotnetCommand,
            private val _targetArguments: TargetArguments)
        : DotnetCommand {

        override val commandType: DotnetCommandType
            get() = _command.commandType

        override val toolResolver: ToolResolver
            get() = _command.toolResolver

        override fun getArguments(context: DotnetBuildContext): Sequence<CommandLineArgument> =
                sequence {
                    if (_command.toolResolver.isCommandRequired) {
                        // command
                        yieldAll(_command.commandType.id.split('-')
                                .filter { it.isNotEmpty() }
                                .map { CommandLineArgument(it,  CommandLineArgumentType.Mandatory) })
                    }

                    // projects
                    yieldAll(_targetArguments.arguments)
                    // command specific arguments
                    yieldAll(_command.getArguments(context))
                }

        override val targetArguments: Sequence<TargetArguments>
            get() = sequenceOf(_targetArguments)

        override val environmentBuilders: Sequence<EnvironmentBuilder>
            get() = _command.environmentBuilders

        override val resultsAnalyzer: ResultsAnalyzer
            get() = _command.resultsAnalyzer

        override val resultsObserver: Observer<CommandResultEvent>
            get() = _command.resultsObserver
    }
}