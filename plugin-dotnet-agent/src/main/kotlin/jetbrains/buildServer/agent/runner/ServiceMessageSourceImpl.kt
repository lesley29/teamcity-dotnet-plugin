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

package jetbrains.buildServer.agent.runner

import jetbrains.buildServer.messages.serviceMessages.ServiceMessage
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageHandler
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes
import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesRegister
import jetbrains.buildServer.rx.*

class ServiceMessageSourceImpl(
        private val _serviceMessagesRegister: ServiceMessagesRegister)
    : ServiceMessageSource, ServiceMessageHandler {
    private val _subject: Subject<ServiceMessage> = subjectOf()
    private val _sharedSource: Observable<ServiceMessage> = _subject
            .track(
                    { if (it) activate() },
                    { if (!it) deactivate() })
            .share()

    override fun subscribe(observer: Observer<ServiceMessage>): Disposable =
            _sharedSource.subscribe(observer)

    override fun handle(serviceMessage: ServiceMessage) =
            _subject.onNext(serviceMessage)

    private fun activate() =
            serviceMessages.forEach { _serviceMessagesRegister.registerHandler(it, this) }

    private fun deactivate() =
            serviceMessages.forEach { _serviceMessagesRegister.removeHandler(it) }

    companion object {
        internal val serviceMessages = sequenceOf(ServiceMessageTypes.TEST_FAILED)
    }
}