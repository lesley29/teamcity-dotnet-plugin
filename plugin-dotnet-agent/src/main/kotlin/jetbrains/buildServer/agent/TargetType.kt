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

package jetbrains.buildServer.agent

enum class TargetType(val priority: Int) {  // Examples:
    NotApplicable(0),
    SystemDiagnostics(1),           // `dotnet --version` to determine .NET SDK ....
    Tool(100),                      // `dotnet`, `msBuild`, `nuget` ...
    Host(200),                      // `java`, `dotnet`, `mono` ...
    PerformanceProfiler(300),        // `dotTrace` ...
    MemoryProfiler(310),             // `dotMemory` ...
    CodeCoverageProfiler(320),       // `dotCover` ...
}