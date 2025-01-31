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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.agent.CommandLineArgument
import jetbrains.buildServer.agent.Path
import jetbrains.buildServer.agent.ToolPath
import jetbrains.buildServer.dotnet.*
import jetbrains.buildServer.dotnet.commands.MSBuildCommand
import jetbrains.buildServer.dotnet.commands.msbuild.MSBuildParameter
import jetbrains.buildServer.dotnet.commands.responseFile.ResponseFileFactory
import jetbrains.buildServer.dotnet.commands.targeting.TargetsParser
import jetbrains.buildServer.dotnet.test.agent.runner.ParametersServiceStub
import jetbrains.buildServer.dotnet.test.dotnet.ArgumentsProviderStub
import jetbrains.buildServer.dotnet.test.dotnet.commands.targeting.TargetServiceStub
import jetbrains.buildServer.dotnet.test.dotnet.commands.test.TestsResultsAnalyzerStub
import jetbrains.buildServer.dotnet.test.dotnet.toolResolvers.ToolResolverStub
import org.jmock.Mockery
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

class MSBuildCommandTest {
    @MockK private lateinit var _toolStateWorkflowComposer: ToolStateWorkflowComposer
    @MockK private lateinit var _targetsParser: TargetsParser
    @MockK private lateinit var _filterFactory: DotnetFilterFactory
    @MockK private lateinit var _responseFileFactory: ResponseFileFactory

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { _targetsParser.parse(any()) } answers { arg<String>(0).split(' ').joinToString(";") }
    }

    @DataProvider
    fun argumentsData(): Array<Array<Any>> {
        return arrayOf(
                arrayOf(mapOf(Pair(DotnetConstants.PARAM_PATHS, "path/")),
                        listOf("respArgs", "customArg1")
                ),
                arrayOf(mapOf(
                        Pair(DotnetConstants.PARAM_TARGETS, "restore build"),
                        Pair(DotnetConstants.PARAM_RUNTIME, "osx.10.11-x64"),
                        Pair(DotnetConstants.PARAM_CONFIG, "Release")),
                        listOf("-t:restore;build", "-p:Configuration=Release", "-p:RuntimeIdentifiers=osx.10.11-x64", "respArgs", "customArg1")
                ),
                arrayOf(mapOf(Pair(DotnetConstants.PARAM_TARGETS, "clean restore build pack")),
                        listOf("-t:clean;restore;build;pack", "respArgs", "customArg1")
                )
        )
    }

    @Test(dataProvider = "argumentsData")
    fun shouldGetArguments(
            parameters: Map<String, String>,
            expectedArguments: List<String>) {
        // Given
        every { _filterFactory.createFilter(DotnetCommandType.MSBuild) } returns DotnetFilter("", null, false)
        val command = createCommand(parameters = parameters, targets = sequenceOf("my.csproj"), respArguments = sequenceOf(CommandLineArgument("respArgs")), customArguments = sequenceOf(CommandLineArgument("customArg1")))

        // When
        val actualArguments = command.getArguments(DotnetBuildContext(ToolPath(Path("wd")), command)).map { it.value }.toList()

        // Then
        Assert.assertEquals(actualArguments, expectedArguments)
    }

    @Test
    fun shouldSupportFilterArgWhenSplitting() {
        // Given
        every { _filterFactory.createFilter(DotnetCommandType.MSBuild) } returns DotnetFilter("myFilter", null, true)
        val command = createCommand(parameters = mapOf(Pair(DotnetConstants.PARAM_PATHS, "path/")), targets = sequenceOf("my.csproj"), respArguments = sequenceOf(CommandLineArgument("respArgs")), customArguments = sequenceOf(CommandLineArgument("customArg1")))
        val filterRspPath = Path("1.rsp")
        every { _responseFileFactory.createResponeFile("Filter", emptySequence(), match { MSBuildParameter("VSTestTestCaseFilter", "myFilter").equals(it.singleOrNull()) }) } returns filterRspPath

        // When
        val actualArguments = command.getArguments(DotnetBuildContext(ToolPath(Path("wd")), command)).map { it.value }.toList()

        // Then
        Assert.assertEquals(actualArguments, listOf("respArgs", "@1.rsp", "customArg1"))
    }

    @Test
    fun shouldSupportSettingsFileWhenSplitting() {
        // Given
        every { _filterFactory.createFilter(DotnetCommandType.MSBuild) } returns DotnetFilter("", File("My.runsettings"), true)
        val command = createCommand(parameters = mapOf(Pair(DotnetConstants.PARAM_PATHS, "path/")), targets = sequenceOf("my.csproj"), respArguments = sequenceOf(CommandLineArgument("respArgs")), customArguments = sequenceOf(CommandLineArgument("customArg1")))
        val filterRspPath = Path("1.rsp")
        every { _responseFileFactory.createResponeFile("Filter", emptySequence(), match { MSBuildParameter("VSTestSetting", "My.runsettings").equals(it.singleOrNull()) }) } returns filterRspPath

        // When
        val actualArguments = command.getArguments(DotnetBuildContext(ToolPath(Path("wd")), command)).map { it.value }.toList()

        // Then
        Assert.assertEquals(actualArguments, listOf("respArgs", "@1.rsp", "customArg1"))
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
        Assert.assertEquals(actualCommand, DotnetCommandType.MSBuild)
    }

    @Test
    fun shouldProvideToolExecutableFile() {
        // Given
        val command = createCommand()

        // When
        val actualExecutable = command.toolResolver.executable

        // Then
        Assert.assertEquals(actualExecutable, ToolPath(Path("msbuild.exe")))
    }

    fun createCommand(
            parameters: Map<String, String> = emptyMap(),
            targets: Sequence<String> = emptySequence(),
            respArguments: Sequence<CommandLineArgument> = emptySequence(),
            customArguments: Sequence<CommandLineArgument> = emptySequence(),
            testsResultsAnalyzer: ResultsAnalyzer = TestsResultsAnalyzerStub()
    ): DotnetCommand {
        val ctx = Mockery()
        return MSBuildCommand(
                ParametersServiceStub(parameters),
                testsResultsAnalyzer,
                TargetServiceStub(targets.map { CommandTarget(Path(it)) }.asSequence()),
                ArgumentsProviderStub(respArguments),
                ArgumentsProviderStub(customArguments),
                ToolResolverStub(ToolPlatform.Windows, ToolPath(Path("msbuild.exe")), true, _toolStateWorkflowComposer),
                ctx.mock<EnvironmentBuilder>(EnvironmentBuilder::class.java),
                _targetsParser,
                _filterFactory,
                _responseFileFactory)
    }
}