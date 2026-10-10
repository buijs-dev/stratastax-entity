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

/**
 * Generates a `CsvRowMapper` for the annotated class, which reads one CSV row into it.
 *
 * On a [DomainEntity], the mapper reads its generated create command instead: every [DomainCreate]
 * property that isn't [CsvIgnore]. On any other class, every constructor property that isn't
 * [CsvIgnore].
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class CsvImport

/**
 * Reads this property from the CSV column [name] instead of the column named after the property.
 *
 * Column names are matched ignoring case and underscores, so this is only needed when the header
 * is a different name, such as an abbreviation:
 * ```
 * @CsvImport
 * @DomainEntity
 * data class BggRankingEntry(
 *     @DomainCreate
 *     @CsvColumn("id")
 *     @PersistenceColumn("BGG_ID")
 *     val bggId: Long,
 * )
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CsvColumn(
    val name: String,
)

/**
 * Leaves this property out of the CSV row; on a [DomainEntity], it must have a default value, which
 * the create command then uses.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class CsvIgnore
