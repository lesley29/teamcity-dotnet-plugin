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

package jetbrains.buildServer.dotnet.test.dotcover

import jetbrains.buildServer.agent.Path
import jetbrains.buildServer.dotcover.ImportDataServiceMessage
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class ImportDataServiceMessageTest {
    @DataProvider(name = "serviceMessageCases")
    fun serviceMessageCases(): Array<Array<Any>> {
        return arrayOf(
                arrayOf("dotcover", Path("dotCoverHome"), "##teamcity[importData type='dotNetCoverage' tool='dotcover' path='dotCoverHome']"),
                arrayOf("dotCover", Path("dotCover Home"), "##teamcity[importData type='dotNetCoverage' tool='dotCover' path='dotCover Home']"),
                arrayOf("", Path(""), "##teamcity[importData type='dotNetCoverage' tool='' path='']"))
    }

    @Test(dataProvider = "serviceMessageCases")
    fun shouldProduceServiceMessage(coverageToolName: String, artifactPath: Path, expectedMessage: String) {
        // Given
        val serviceMessage = ImportDataServiceMessage(coverageToolName, artifactPath)

        // When
        val actualMessage = serviceMessage.toString()

        // Then
        Assert.assertEquals(actualMessage, expectedMessage)
    }
}