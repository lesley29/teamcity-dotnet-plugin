/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.dotnet.commands

import jetbrains.buildServer.agent.CommandLineArgument
import jetbrains.buildServer.agent.CommandLineArgumentType
import jetbrains.buildServer.agent.runner.ParametersService
import jetbrains.buildServer.dotnet.*
import jetbrains.buildServer.dotnet.commands.targeting.TargetArguments
import jetbrains.buildServer.dotnet.commands.targeting.TargetService
import jetbrains.buildServer.dotnet.toolResolvers.DotnetToolResolver

class BuildCommand(
    _parametersService: ParametersService,
    override val resultsAnalyzer: ResultsAnalyzer,
    private val _targetService: TargetService,
    private val _commonArgumentsProvider: DotnetCommonArgumentsProvider,
    override val toolResolver: DotnetToolResolver,
    private val _vstestLoggerEnvironment: EnvironmentBuilder
) : DotnetCommandBase(_parametersService) {
    override val commandType = DotnetCommandType.Build

    override val commandWords = sequenceOf("build")

    override val targetArguments: Sequence<TargetArguments>
        get() = _targetService.targets.map { TargetArguments(sequenceOf(CommandLineArgument(it.target.path, CommandLineArgumentType.Target))) }

    override fun getArguments(context: DotnetBuildContext): Sequence<CommandLineArgument> = sequence {
        parameters(DotnetConstants.PARAM_FRAMEWORK)?.trim()?.let {
            if (it.isNotBlank()) {
                yield(CommandLineArgument("--framework"))
                yield(CommandLineArgument(it))
            }
        }

        parameters(DotnetConstants.PARAM_CONFIG)?.trim()?.let {
            if (it.isNotBlank()) {
                yield(CommandLineArgument("--configuration"))
                yield(CommandLineArgument(it))
            }
        }

        parameters(DotnetConstants.PARAM_RUNTIME)?.trim()?.let {
            if (it.isNotBlank()) {
                yield(CommandLineArgument("--runtime"))
                yield(CommandLineArgument(it))
            }
        }

        parameters(DotnetConstants.PARAM_OUTPUT_DIR)?.trim()?.let {
            if (it.isNotBlank()) {
                yield(CommandLineArgument("--output"))
                yield(CommandLineArgument(it))
            }
        }

        parameters(DotnetConstants.PARAM_VERSION_SUFFIX)?.trim()?.let {
            if (it.isNotBlank()) {
                yield(CommandLineArgument("--version-suffix"))
                yield(CommandLineArgument(it))
            }
        }

        yieldAll(_commonArgumentsProvider.getArguments(context))
    }

    override val environmentBuilders: Sequence<EnvironmentBuilder>
        get() = sequence { yield(_vstestLoggerEnvironment) }
}