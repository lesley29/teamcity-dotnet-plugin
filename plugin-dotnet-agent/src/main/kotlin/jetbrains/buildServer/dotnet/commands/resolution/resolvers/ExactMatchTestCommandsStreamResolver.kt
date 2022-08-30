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

import jetbrains.buildServer.agent.*
import jetbrains.buildServer.agent.Logger
import jetbrains.buildServer.agent.runner.PathsService
import jetbrains.buildServer.dotnet.*
import jetbrains.buildServer.dotnet.commands.resolution.*
import jetbrains.buildServer.dotnet.commands.test.splitTests.SplitTestsNamesSaver
import jetbrains.buildServer.dotnet.commands.test.splitTests.SplitTestsNamesSessionManager
import jetbrains.buildServer.rx.Observer
import jetbrains.buildServer.rx.use
import java.util.regex.Pattern

// Transforms a `dotnet test` command to exact match filtered command if needed
// It looks like a sequence of dotnet commands: `dotnet test --list-test` � to get all tests list � and then `dotnet test ...` N times
class ExactMatchTestCommandsStreamResolver(
    private val _splitTestsFilterSettings: SplittedTestsFilterSettings,
    private val _listTestsDotnetCommand: DotnetCommand,
    private val _patheService: PathsService,
    private val _testsNamesSessionFactory: SplitTestsNamesSessionManager,
) : DotnetCommandStreamResolverBase() {
    override val stage = DotnetCommandsStreamResolvingStage.Transformation

    override fun shouldBeApplied(commands: DotnetCommandsStream) =
        _splitTestsFilterSettings.isActive && _splitTestsFilterSettings.useExactMatchFilter && commands.any { it.commandType == DotnetCommandType.Test }

    override fun apply(commands: DotnetCommandsStream) =
        commands
            .flatMap {
                when {
                    it.commandType == DotnetCommandType.Test -> transform(it)
                    else -> sequenceOf(it)
                }
            }

    private fun transform(testCommand: DotnetCommand) = sequence<DotnetCommand> {
        _testsNamesSessionFactory.startSession().use { session ->
            session.getSaver().use { saver ->
                // list all target's tests e.g. `dotnet test --list-tests` single command
                yield(ObservingListTestsDotnetCommand(_listTestsDotnetCommand, ExactMatchListTestsCommandResultHandler(saver)))
            }

            val chunksCount = session.chunksCount

            // sequence of `dotnet test` commands for every chunk
            repeat(chunksCount) {
                yield(testCommand)
            }
        }
    }

    private final class ObservingListTestsDotnetCommand constructor(
        private val _originalCommand: DotnetCommand,
        private val _resultObserver: Observer<CommandResultEvent>
    ) : DotnetCommand by _originalCommand {
        override val resultsObserver = _resultObserver
    }

    private final class ExactMatchListTestsCommandResultHandler(
        private val _testNamesSaver: SplitTestsNamesSaver,
    ) : Observer<CommandResultEvent> {
        private val _whitespacePattern = Pattern.compile("\\s+")
        private var _isTestsOutputStarted = false

        override fun onNext(value: CommandResultEvent) {
            if (!(value is CommandResultOutput)) {
                return
            }

            // we don't want to see millions of tests names in build log
            value.attributes.add(CommandResultAttribute.Suppressed)

            val resultLine = value.output.trim()

            if (!_isTestsOutputStarted) {
                if (resultLine.equals(TestsListOutputMarker, ignoreCase = true)) {
                    _isTestsOutputStarted = true
                }
                return
            }

            resultLine
                .let { _whitespacePattern.split(it) }
                .forEach { _testNamesSaver.tryToSave(it) }
        }

        override fun onError(error: Exception) = Unit

        override fun onComplete() = Unit

        companion object {
            private const val TestsListOutputMarker = "The following Tests are available:"
            private val LOG = Logger.getLogger(ExactMatchListTestsCommandResultHandler::class.java)
        }
    }
}