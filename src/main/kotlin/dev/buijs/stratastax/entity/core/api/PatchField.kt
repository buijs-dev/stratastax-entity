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

/**
 * A property of an update command that tells "not supplied" ([Unset]) apart from "supplied,
 * possibly as `null`" ([Value]).
 *
 * Gives update commands PATCH semantics: only supplied fields are touched and a field can be
 * explicitly cleared. Has no persistence dependencies, so it can also be used by an HTTP layer.
 */
sealed interface PatchField<out T> {
    /** The field was not supplied. */
    data object Unset : PatchField<Nothing>

    /** The field was supplied, [value] may be `null`. */
    data class Value<T>(val value: T?) : PatchField<T>

    /** True for [Value], false for [Unset]. */
    val isProvided: Boolean
        get() = this is Value
}

/** Returns [current] for [Unset], otherwise the supplied value (even when `null`). */
fun <T> PatchField<T>.orKeep(current: T?): T? =
    when (this) {
        PatchField.Unset -> current
        is PatchField.Value -> value
    }

/**
 * Same as [orKeep] for a field that must never be `null`.
 *
 * @throws IllegalArgumentException if this is `Value(null)`; [fieldName] is used in the message.
 */
fun <T> PatchField<T>.orKeepRequired(current: T, fieldName: String): T =
    when (this) {
        PatchField.Unset -> current
        is PatchField.Value ->
            value ?: throw IllegalArgumentException("'$fieldName' must not be null")
    }

/**
 * Converts this field to the set of target ids a junction relation must be synced to, or `null` if
 * the relation must not be touched ([PatchField.Unset]).
 *
 * `Value(null)` and `Value(emptyList())` both result in an empty set. [transform] maps each element
 * to its raw id, which is required for single-property wrapper types since those are not unwrapped
 * automatically. The receiver is declared with a nullable collection, so both `PatchField<List<X>>`
 * and `PatchField<List<X>?>` resolve to this function.
 */
fun <T, R> PatchField<Collection<T>?>.toTargetIdsOrNull(transform: (T) -> R): Set<Any?>? =
    when (this) {
        PatchField.Unset -> null
        is PatchField.Value -> value?.map(transform)?.toSet() ?: emptySet()
    }

/** Same as [toTargetIdsOrNull] for elements that already are raw ids. */
fun <T> PatchField<Collection<T>?>.toTargetIdsOrNull(): Set<Any?>? = toTargetIdsOrNull { it }

/**
 * Returns the supplied collection, or `null` if the group must not be touched ([PatchField.Unset]).
 *
 * `Value(null)` becomes an empty collection, which unlike `null` means "remove all children". The
 * receiver is declared with a nullable collection for the same reason as in [toTargetIdsOrNull].
 */
fun <T> PatchField<Collection<T>?>.orNullIfUnset(): Collection<T>? =
    when (this) {
        PatchField.Unset -> null
        is PatchField.Value -> value ?: emptyList()
    }
