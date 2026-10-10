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

/**
 * Generates a value for a field.
 *
 * The generator is matched to a `@DomainCreateGenerated`/`@DomainUpdateGenerated` property by
 * [key]. The [key] is read from source by the gradle-plugin, never at runtime, so it must be a
 * string literal. When `null`, the key is derived from [T] as `"generate{T}"`, which is also what
 * an annotation with a blank key resolves to.
 *
 * @param key The key of the annotations this generator serves, or `null` to serve every property of
 *   type [T] that has no explicit key.
 */
abstract class TypeGenerator<T>(
    key: String? = null,
) {

    /** Returns a newly generated value. */
    abstract fun generate(): T
}
