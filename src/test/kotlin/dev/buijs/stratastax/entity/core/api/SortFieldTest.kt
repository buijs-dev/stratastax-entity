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
package dev.buijs.stratastax.entity.core.api

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SortFieldTest {

    private val title =
        EntityField(
            name = "title",
            type = EntityFieldType.STRING,
            sortable = true,
        )

    @Test
    fun `asc creates an ascending SortField`() {
        assertThat(title.asc()).isEqualTo(SortField(title, SortDirection.ASC))
    }

    @Test
    fun `desc creates a descending SortField`() {
        assertThat(title.desc()).isEqualTo(SortField(title, SortDirection.DESC))
    }

    @Test
    fun `Sort keeps the order of its fields`() {
        // given
        val sort = Sort(listOf(title.desc(), title.asc()))

        // expect
        assertThat(sort.fields).containsExactly(title.desc(), title.asc())
    }

    @Test
    fun `Sort without fields throws`() {
        assertThatThrownBy { Sort(emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("at least one field")
    }

    @ParameterizedTest
    @ValueSource(strings = ["ASC", "asc", "Asc"])
    fun `SortDirection of parses ASC case-insensitively`(value: String) {
        assertThat(SortDirection.of(value, "title")).isEqualTo(SortDirection.ASC)
    }

    @ParameterizedTest
    @ValueSource(strings = ["DESC", "desc", "Desc"])
    fun `SortDirection of parses DESC case-insensitively`(value: String) {
        assertThat(SortDirection.of(value, "title")).isEqualTo(SortDirection.DESC)
    }

    @Test
    fun `SortDirection of rejects anything else and names the value and field`() {
        assertThatThrownBy { SortDirection.of("up", "title") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid sort direction 'up' for field 'title', expected 'ASC' or 'DESC'")
    }

    @Test
    fun `SortDirection declares ASC and DESC`() {
        assertThat(SortDirection.entries).containsExactly(SortDirection.ASC, SortDirection.DESC)
    }
}
