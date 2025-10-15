/*
 * Copyright 2025 Karma Krafts & associates
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

package dev.karmakrafts.fluently.reactive

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
open class CachedLocalizationScope( // @formatter:off
    override val localizationManager: LocalizationManager,
    override val coroutineScope: CoroutineScope = CoroutineScope(localizationManager.coroutineContext)
) : LocalizationScope { // @formatter:on
    private val cache: HashMap<ReactiveLocalizationKey, SharedFlow<String>> = HashMap()
    private val cacheMutex: Mutex = Mutex()

    override fun format(name: String, context: ReactiveEvaluationContext): Flow<String> {
        return ReactiveLocalizationKey.fromContext(name, context).flatMapLatest { key ->
            cacheMutex.withLock {
                cache.getOrPut(key) {
                    localizationManager.format(name, context)
                        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
                }
            }
        }
    }

    override fun format(name: String, attribName: String, context: ReactiveEvaluationContext): Flow<String> {
        return ReactiveLocalizationKey.fromContext(name, attribName, context).flatMapLatest { key ->
            cacheMutex.withLock {
                cache.getOrPut(key) {
                    localizationManager.format(name, attribName, context)
                        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(), 1)
                }
            }
        }
    }
}