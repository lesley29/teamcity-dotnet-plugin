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

package jetbrains.buildServer.dotnet.test.dotnet.commands.msbuild

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.agent.Path
import jetbrains.buildServer.agent.ToolPath
import jetbrains.buildServer.agent.VirtualContext
import jetbrains.buildServer.agent.runner.PathType
import jetbrains.buildServer.agent.runner.PathsService
import jetbrains.buildServer.dotnet.*
import jetbrains.buildServer.dotnet.commands.msbuild.MSBuildParameter
import jetbrains.buildServer.dotnet.commands.msbuild.MSBuildParameterType
import jetbrains.buildServer.dotnet.commands.msbuild.MSBuildVSTestLoggerParametersProvider
import jetbrains.buildServer.dotnet.commands.test.TestReportingParameters
import jetbrains.buildServer.dotnet.logging.LoggerParameters
import jetbrains.buildServer.dotnet.test.dotnet.logging.LoggerResolverStub
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File
import java.util.*

class MSBuildVSTestLoggerParametersProviderTest {
    @MockK private lateinit var _pathsService: PathsService
    @MockK private lateinit var _testReportingParameters: TestReportingParameters
    @MockK private lateinit var _msBuildVSTestLoggerParameters: LoggerParameters
    @MockK private lateinit var _virtualContext: VirtualContext

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this)
        clearAllMocks()
        every { _virtualContext.resolvePath(any()) } answers { "v_" + arg<String>(0)}
    }

    @DataProvider
    fun testLoggerArgumentsData(): Array<Array<Any?>> {
        return arrayOf(
                // Success scenario
                arrayOf(
                        File("loggerPath", "vstestlogger.dll") as File?,
                        EnumSet.of(TestReportingMode.On),
                        listOf(
                                MSBuildParameter("VSTestLogger", "logger://teamcity", MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestTestAdapterPath", "v_" + File("checkoutDir").canonicalPath, MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestVerbosity", Verbosity.Detailed.id.lowercase(Locale.getDefault()), MSBuildParameterType.Predefined)
                        )),

                // Supports mult VSTestTestAdapterPath (.NET Core SDK 2.1.102)
                arrayOf(
                        File("loggerPath", "vstestlogger.dll") as File?,
                        EnumSet.of(TestReportingMode.MultiAdapterPath),
                        listOf(
                                MSBuildParameter("VSTestLogger", "logger://teamcity", MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestTestAdapterPath", "v_" + "${File("loggerPath").canonicalPath};.", MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestVerbosity", Verbosity.Detailed.id.lowercase(Locale.getDefault()), MSBuildParameterType.Predefined)
                        )),

                // Supports mult VSTestTestAdapterPath (.NET SDK 5.0.103)
                arrayOf(
                        File("loggerPath", "vstestlogger.dll") as File?,
                        EnumSet.of(TestReportingMode.MultiAdapterPath_5_0_103),
                        listOf(
                                MSBuildParameter("VSTestLogger", "logger://teamcity", MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestTestAdapterPath", ".;v_" + "${File("loggerPath").canonicalPath}", MSBuildParameterType.Predefined),
                                MSBuildParameter("VSTestVerbosity", Verbosity.Detailed.id.lowercase(Locale.getDefault()), MSBuildParameterType.Predefined)
                        )),

                // Reporting is off
                arrayOf(
                        File("loggerPath", "vstestlogger.dll") as File?,
                        EnumSet.of(TestReportingMode.Off),
                        emptyList<MSBuildParameter>())
        )
    }

    @Test(dataProvider = "testLoggerArgumentsData")
    fun shouldGetArguments(
            loggerFile: File,
            testReportingMode: EnumSet<TestReportingMode>,
            expectedParameters: List<MSBuildParameter>) {
        // Given
        var checkoutDirection =  File("checkoutDir")
        val context = DotnetBuildContext(ToolPath(Path("wd")), mockk<DotnetCommand>())
        val argumentsProvider = MSBuildVSTestLoggerParametersProvider(_pathsService, LoggerResolverStub(File("msbuildlogger"), loggerFile), _testReportingParameters, _msBuildVSTestLoggerParameters, _virtualContext)
        every { _testReportingParameters.getMode(context) } returns testReportingMode
        every { _pathsService.getPath(PathType.Checkout) } returns checkoutDirection
        every { _msBuildVSTestLoggerParameters.vsTestVerbosity } returns Verbosity.Detailed

        // When
        val actualParameters = argumentsProvider.getParameters(context).toList()

        // Then
        if (expectedParameters.any()) {
            verify { _virtualContext.resolvePath(any()) }
        }

        Assert.assertEquals(actualParameters, expectedParameters)
    }
}