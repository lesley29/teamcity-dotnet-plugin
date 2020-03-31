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

package jetbrains.buildServer.agent.runner

import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.agent.*
import jetbrains.buildServer.dotnet.DotnetConstants
import org.springframework.beans.factory.BeanFactory

class WorkflowBuildServiceFactory(
        private val _runnerType: String,
        private val _beanFactory: BeanFactory)
    : MultiCommandBuildSessionFactory, BuildStepContext, DirectoryCleanersProvider {
    private var _runnerContext: BuildRunnerContext? = null

    override fun createSession(runnerContext: BuildRunnerContext): MultiCommandBuildSession {
        _runnerContext = runnerContext
        return _beanFactory.getBean(WorkflowSessionImpl::class.java)
    }

    override fun getCleanerName() = DotnetConstants.CLEANER_NAME

    override fun registerDirectoryCleaners(context: DirectoryCleanersProviderContext, registry: DirectoryCleanersRegistry) {
        _runnerContext = (context.runningBuild as AgentRunningBuildEx).currentRunnerContext
        (_beanFactory.getBean(CacheCleanerSession::class.java) as CacheCleanerSession).create(registry)
    }

    override val isAvailable: Boolean
        get() = _runnerContext != null

    override val runnerContext: BuildRunnerContext
        get() = _runnerContext ?: throw RunBuildException("Runner session was not started")

    override fun getBuildRunnerInfo(): AgentBuildRunnerInfo = object : AgentBuildRunnerInfo {
        override fun getType(): String = _runnerType

        override fun canRun(config: BuildAgentConfiguration): Boolean = true
    }
}
