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
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

class PatchFieldTest {

    @Test
    fun `Unset is not provided`() {
        assertThat(PatchField.Unset.isProvided).isFalse()
    }

    @Test
    fun `Value is provided, even when its own value is null`() {
        assertThat(PatchField.Value("x").isProvided).isTrue()
        assertThat(PatchField.Value<String>(null).isProvided).isTrue()
    }

    @Test
    fun `orKeep on Unset returns current`() {
        assertThat(PatchField.Unset.orKeep("current")).isEqualTo("current")
    }

    @Test
    fun `orKeep on Value returns the new value, including null`() {
        assertThat(PatchField.Value("new").orKeep("current")).isEqualTo("new")
        assertThat(PatchField.Value<String>(null).orKeep("current")).isNull()
    }

    @Test
    fun `orKeepRequired on Unset returns current`() {
        assertThat(PatchField.Unset.orKeepRequired("current", "name")).isEqualTo("current")
    }

    @Test
    fun `orKeepRequired on Value returns the new value`() {
        assertThat(PatchField.Value("new").orKeepRequired("current", "name")).isEqualTo("new")
    }

    @Test
    fun `orKeepRequired on Value(null) throws`() {
        val error =
            catchThrowableOfType(IllegalArgumentException::class.java) {
                PatchField.Value<String>(null).orKeepRequired("current", "name")
            }
        assertThat(error.message).contains("name")
    }

    @Test
    fun `toTargetIdsOrNull on Unset returns null`() {
        assertThat(PatchField.Unset.toTargetIdsOrNull<String>()).isNull()
    }

    @Test
    fun `toTargetIdsOrNull on Value returns the set, deduplicated`() {
        assertThat(PatchField.Value(listOf("a", "b", "a")).toTargetIdsOrNull())
            .isEqualTo(setOf("a", "b"))
    }

    @Test
    fun `toTargetIdsOrNull on Value(null) or Value(emptyList) both return an empty set`() {
        assertThat(PatchField.Value<List<String>>(null).toTargetIdsOrNull())
            .isEqualTo(emptySet<Any?>())
        assertThat(PatchField.Value(emptyList<String>()).toTargetIdsOrNull())
            .isEqualTo(emptySet<Any?>())
    }

    private data class Wrapper(val value: String)

    @Test
    fun `toTargetIdsOrNull with a transform unwraps each element`() {
        assertThat(
                PatchField.Value(listOf(Wrapper("a"), Wrapper("b"))).toTargetIdsOrNull { it.value }
            )
            .isEqualTo(setOf("a", "b"))
    }

    @Test
    fun `toTargetIdsOrNull with a transform on Unset still returns null`() {
        assertThat(PatchField.Unset.toTargetIdsOrNull<Wrapper, String> { it.value }).isNull()
    }

    @Test
    fun `toTargetIdsOrNull also resolves when the receiver's own collection type argument is declared nullable`() {
        val nullable: PatchField<List<String>?> = PatchField.Value(listOf("a", "b"))
        assertThat(nullable.toTargetIdsOrNull()).isEqualTo(setOf("a", "b"))
        assertThat(PatchField.Unset.toTargetIdsOrNull<String>()).isNull()
    }

    @Test
    fun `orNullIfUnset on Unset returns null`() {
        assertThat(PatchField.Unset.orNullIfUnset<String>()).isNull()
    }

    @Test
    fun `orNullIfUnset on Value returns the collection unchanged`() {
        assertThat(PatchField.Value(listOf("a", "b")).orNullIfUnset()).isEqualTo(listOf("a", "b"))
    }

    @Test
    fun `orNullIfUnset on Value(null) or Value(emptyList) both return an empty collection`() {
        assertThat(PatchField.Value<List<String>>(null).orNullIfUnset())
            .isEqualTo(emptyList<String>())
        assertThat(PatchField.Value(emptyList<String>()).orNullIfUnset())
            .isEqualTo(emptyList<String>())
    }

    @Test
    fun `orNullIfUnset also resolves when the receiver's own collection type argument is declared nullable`() {
        val nullable: PatchField<List<String>?> = PatchField.Value(listOf("a", "b"))
        assertThat(nullable.orNullIfUnset()).isEqualTo(listOf("a", "b"))
        assertThat(PatchField.Unset.orNullIfUnset<String>()).isNull()
    }
}
