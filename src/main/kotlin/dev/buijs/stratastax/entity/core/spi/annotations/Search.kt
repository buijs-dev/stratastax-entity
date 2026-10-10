/**
 * Copyright (c) 2021 - 2026 Buijs Software
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.buijs.stratastax.entity.core.spi.annotations

import dev.buijs.stratastax.entity.core.api.SortDirection

/**
 * Marks a constructor property as filterable by the caller.
 *
 * Mutually exclusive with [SearchFixedFilter].
 *
 * @property alias When non-blank, overrides the field's search-facing name (e.g.
 *   `?filter=name==foo` instead of `?filter=displayName==foo`). It plays no part in the REST DTO
 *   mapping; use [RestProperty] for that.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SearchFilter(
    val alias: String = "",
)

/**
 * Applies an always-on filter `property == value` that the caller cannot see or override.
 *
 * Mutually exclusive with [SearchFilter] and [SearchSort]: a field with a single fixed value cannot
 * meaningfully be filtered or sorted by the caller. Not needed on a [PersistenceSoftDelete] field,
 * which already implies `SearchFixedFilter("null")`.
 *
 * @property value Parsed by the field type: `null`, `true`/`false`, a number, an ISO-8601 UTC
 *   date-time such as `2026-01-01T00:00:00Z` for a date or timestamp, a UUID, an enum constant, JSON,
 *   or else a string. One value only: the filter is always `property == value`.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SearchFixedFilter(
    val value: String,
)

/**
 * Marks a constructor property as sortable.
 *
 * @property tiebreaker Whether this is the entity's single required tiebreaker field. Cursor
 *   pagination needs exactly one per entity.
 * @property direction The field's default sort direction.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class SearchSort(
    val tiebreaker: Boolean = false,
    val direction: SortDirection = SortDirection.ASC,
)
