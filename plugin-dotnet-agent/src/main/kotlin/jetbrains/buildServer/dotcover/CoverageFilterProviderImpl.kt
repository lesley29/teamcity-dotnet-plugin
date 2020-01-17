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

package jetbrains.buildServer.dotcover

import jetbrains.buildServer.agent.runner.ParameterType
import jetbrains.buildServer.agent.runner.ParametersService
import jetbrains.buildServer.dotnet.CoverageConstants
import java.util.*

class CoverageFilterProviderImpl(
        private val _parametersService: ParametersService,
        private val _coverageFilterConverter: DotCoverFilterConverter)
    : CoverageFilterProvider {

    override val filters: Sequence<CoverageFilter>
        get() {
            val filters = ArrayList<CoverageFilter>()
            _parametersService.tryGetParameter(ParameterType.Runner, CoverageConstants.PARAM_DOTCOVER_FILTERS)?.let {
                for (filter in _coverageFilterConverter.convert(it).map { toModuleFilter(it) }) {
                    filters.add(filter)
                }
            }

            if (filters.size == 0) {
                filters.addAll(0, DefaultIncludeFilters)
            }

            addAdditionalAnyFilterWhenHasOutdatedAnyFilter(filters, CoverageFilter.CoverageFilterType.Include)
            addAdditionalAnyFilterWhenHasOutdatedAnyFilter(filters, CoverageFilter.CoverageFilterType.Exclude)
            filters.addAll(DefaultExcludeFilters)
            return filters.asSequence()
        }

    override val attributeFilters: Sequence<CoverageFilter>
        get() = sequence {
            _parametersService.tryGetParameter(ParameterType.Runner, CoverageConstants.PARAM_DOTCOVER_ATTRIBUTE_FILTERS)?.let {
                for (filter in _coverageFilterConverter.convert(it).map { toAttributeFilter(it) }) {
                    if (filter.type == CoverageFilter.CoverageFilterType.Exclude && CoverageFilter.Any != filter.classMask) {
                        yield(filter)
                    }
                }
            }

            yieldAll(DefaultExcludeAttributeFilters)
        }

    private fun addAdditionalAnyFilterWhenHasOutdatedAnyFilter(filters: MutableList<CoverageFilter>, type: CoverageFilter.CoverageFilterType) {
        val outdatedFilter = CoverageFilter(type, CoverageFilter.Any, "*.*", CoverageFilter.Any, CoverageFilter.Any)
        val additionalFilter = CoverageFilter(type, CoverageFilter.Any, CoverageFilter.Any, CoverageFilter.Any, CoverageFilter.Any)
        val anyIncludeFilter = filters.indexOf(outdatedFilter)
        if (anyIncludeFilter >= 0) {
            filters.add(anyIncludeFilter, additionalFilter)
        }
    }

    private fun getMask(mask: String, defaultMask: String): String {
        if (CoverageFilter.Any == mask) {
            return defaultMask
        }

        return mask
    }

    private fun toModuleFilter(filter: CoverageFilter): CoverageFilter {
        return CoverageFilter(
                filter.type,
                CoverageFilter.Any,
                getMask(filter.moduleMask, filter.defaultMask),
                filter.classMask,
                filter.functionMask)
    }

    private fun toAttributeFilter(filter: CoverageFilter): CoverageFilter {
        return CoverageFilter(
                filter.type,
                CoverageFilter.Any,
                CoverageFilter.Any,
                getMask(filter.classMask, filter.defaultMask),
                CoverageFilter.Any)
    }

    companion object {
        internal val DefaultIncludeFilters = listOf(CoverageFilter(CoverageFilter.CoverageFilterType.Include, CoverageFilter.Any, CoverageFilter.Any, CoverageFilter.Any, CoverageFilter.Any))
        internal val DefaultExcludeFilters = listOf(
                CoverageFilter(CoverageFilter.CoverageFilterType.Exclude, CoverageFilter.Any, "TeamCity.VSTest.TestAdapter", CoverageFilter.Any, CoverageFilter.Any),
                CoverageFilter(CoverageFilter.CoverageFilterType.Exclude, CoverageFilter.Any, "TeamCity.MSBuild.Logger", CoverageFilter.Any, CoverageFilter.Any),
                CoverageFilter(CoverageFilter.CoverageFilterType.Exclude, CoverageFilter.Any, CoverageFilter.Any, "AutoGeneratedProgram", CoverageFilter.Any))
        internal val DefaultExcludeAttributeFilters = listOf(
                CoverageFilter(CoverageFilter.CoverageFilterType.Exclude, CoverageFilter.Any, CoverageFilter.Any, "System.Diagnostics.CodeAnalysis.ExcludeFromCodeCoverageAttribute", CoverageFilter.Any))
    }
}