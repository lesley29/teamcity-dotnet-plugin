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

package jetbrains.buildServer.dotnet.test.dotnet.commands

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.agent.CommandLineArgument
import jetbrains.buildServer.agent.Path
import jetbrains.buildServer.agent.ToolPath
import jetbrains.buildServer.dotnet.*
import jetbrains.buildServer.dotnet.commands.PublishCommand
import jetbrains.buildServer.dotnet.test.agent.runner.ParametersServiceStub
import jetbrains.buildServer.dotnet.test.dotnet.ArgumentsProviderStub
import jetbrains.buildServer.dotnet.test.dotnet.commands.targeting.TargetServiceStub
import jetbrains.buildServer.dotnet.test.dotnet.toolResolvers.ToolResolverStub
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class PublishCommandTest {
    @MockK private lateinit var _resultsAnalyzer: ResultsAnalyzer
    @MockK private lateinit var _toolStateWorkflowComposer: ToolStateWorkflowComposer

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
    }


    @DataProvider
    fun testPublishArgumentsData(): Array<Array<Any>> {
        return arrayOf(
                arrayOf(mapOf(Pair(DotnetConstants.PARAM_PATHS, "path/")),
                        listOf("customArg1")),
                arrayOf(mapOf(
                        Pair(DotnetConstants.PARAM_FRAMEWORK, "dotcore"),
                        Pair(DotnetConstants.PARAM_CONFIG, "Release")),
                        listOf("--framework", "dotcore", "--configuration", "Release", "customArg1")),
                arrayOf(mapOf(
                        DotnetConstants.PARAM_RUNTIME to " active"),
                        listOf("--runtime", "active", "customArg1")),
                arrayOf(mapOf(
                        Pair(DotnetConstants.PARAM_OUTPUT_DIR, "out"),
                        Pair(DotnetConstants.PARAM_CONFIG, "Release")),
                        listOf("--configuration", "Release", "--output", "out", "customArg1")),
                arrayOf(mapOf(
                        DotnetConstants.PARAM_OUTPUT_DIR to "c:\\build\\out",
                        DotnetConstants.PARAM_PATHS to "project.csproj",
                        DotnetConstants.PARAM_CONFIG to "Release"),
                        listOf("--configuration", "Release", "--output", "c:\\build\\out", "customArg1"))
        )
    }

    @Test(dataProvider = "testPublishArgumentsData")
    fun shouldGetArguments(
            parameters: Map<String, String>,
            expectedArguments: List<String>) {
        // Given
        val command = createCommand(parameters = parameters, targets = sequenceOf("my.csproj"), arguments = sequenceOf(CommandLineArgument("customArg1")))

        // When
        val actualArguments = command.getArguments(DotnetBuildContext(ToolPath(Path("wd")), command)).map { it.value }.toList()

        // Then
        Assert.assertEquals(actualArguments, expectedArguments)
    }

    @DataProvider
    fun projectsArgumentsData(): Array<Array<Any>> {
        return arrayOf(
                arrayOf(listOf("my.csproj") as Any, listOf(listOf("my.csproj"))),
                arrayOf(emptyList<String>() as Any, emptyList<List<String>>()),
                arrayOf(listOf("my.csproj", "my2.csproj") as Any, listOf(listOf("my.csproj"), listOf("my2.csproj"))))
    }

    @Test(dataProvider = "projectsArgumentsData")
    fun shouldProvideProjectsArguments(targets: List<String>, expectedArguments: List<List<String>>) {
        // Given
        val command = createCommand(targets = targets.asSequence())

        // When
        val actualArguments = command.targetArguments.map { it.arguments.map { it.value }.toList() }.toList()

        // Then
        Assert.assertEquals(actualArguments, expectedArguments)
    }

    @Test
    fun shouldProvideCommandType() {
        // Given
        val command = createCommand()

        // When
        val actualCommand = command.commandType

        // Then
        Assert.assertEquals(actualCommand, DotnetCommandType.Publish)
    }

    @Test
    fun shouldProvideToolExecutableFile() {
        // Given
        val command = createCommand()

        // When
        val actualExecutable = command.toolResolver.executable

        // Then
        Assert.assertEquals(actualExecutable, ToolPath(Path("dotnet")))
    }

    fun createCommand(
            parameters: Map<String, String> = emptyMap(),
            targets: Sequence<String> = emptySequence(),
            arguments: Sequence<CommandLineArgument> = emptySequence()): DotnetCommand =
            PublishCommand(
                    ParametersServiceStub(parameters),
                    _resultsAnalyzer,
                    TargetServiceStub(targets.map { CommandTarget(Path(it)) }.asSequence()),
                    ArgumentsProviderStub(arguments),
                    ToolResolverStub(ToolPlatform.CrossPlatform, ToolPath(Path("dotnet")), true, _toolStateWorkflowComposer)
            )
}