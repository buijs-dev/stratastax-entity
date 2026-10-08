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
package dev.buijs.stratastax.entity.core.spi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TypeGeneratorTest {

    @Test
    fun `generate returns the value produced by the implementation`() {
        // given
        val generator =
            object : TypeGenerator<Long>("ownerId") {

                override fun generate(): Long = 7L
            }

        // expect
        assertThat(generator.generate()).isEqualTo(7L)
    }

    @Test
    fun `key is optional`() {
        // given
        val generator =
            object : TypeGenerator<String>() {

                override fun generate(): String = "x"
            }

        // expect
        assertThat(generator.generate()).isEqualTo("x")
    }

    @Test
    fun `generate is called for every value`() {
        // given
        var counter = 0
        val generator =
            object : TypeGenerator<Int>() {

                override fun generate(): Int = ++counter
            }

        // expect
        assertThat(generator.generate()).isEqualTo(1)
        assertThat(generator.generate()).isEqualTo(2)
    }
}
