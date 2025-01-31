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

package jetbrains.buildServer.dotnet.commands.resolution.resolvers

import jetbrains.buildServer.agent.CommandLineArgument
import jetbrains.buildServer.agent.CommandLineArgumentType
import jetbrains.buildServer.dotnet.DotnetBuildContext
import jetbrains.buildServer.dotnet.DotnetCommand
import jetbrains.buildServer.dotnet.commands.resolution.DotnetCommandStreamResolverBase
import jetbrains.buildServer.dotnet.commands.resolution.DotnetCommandsStream
import jetbrains.buildServer.dotnet.commands.resolution.DotnetCommandsStreamResolvingStage

class ComposedDotnetCommandStreamResolver : DotnetCommandStreamResolverBase() {
    override val stage: DotnetCommandsStreamResolvingStage = DotnetCommandsStreamResolvingStage.FinalComposition

    override fun shouldBeApplied(commands: DotnetCommandsStream) = true

    override fun apply(commands: DotnetCommandsStream) = commands.map { ComposedDotnetCommand(it) }

    class ComposedDotnetCommand constructor(
        private val _originalCommonCommand: DotnetCommand
    ) : DotnetCommand by _originalCommonCommand {
        private val commandCommandLineArguments get() =
            when {
                toolResolver.isCommandRequired -> commandWords.map { CommandLineArgument(it, CommandLineArgumentType.Mandatory) }
                else -> emptySequence()
            }

        override fun getArguments(context: DotnetBuildContext): Sequence<CommandLineArgument> = sequence {
            // command
            yieldAll(commandCommandLineArguments)

            // targets e.g. project files or directories
            yieldAll(targetArguments.flatMap { it.arguments })

            // command specific arguments
            yieldAll(_originalCommonCommand.getArguments(context))
        }
    }
}