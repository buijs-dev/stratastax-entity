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

import kotlin.reflect.KClass

/**
 * Declares the REST DTO types of a [DomainEntity]: the response DTO of one entity ([read]) and of a
 * page of entities ([readAll]), and the request DTO of its creation ([create]) and update ([update]).
 *
 * None of them is used as a type: only their names are matched against the OpenAPI spec loaded for
 * a codegen run. Each is optional; [Unit] falls back to the schema named by convention: `playset`,
 * `playset_list`, `create_playset` and `update_playset` for `Playset`. [Nothing] declares there is
 * no DTO to map in that direction: the endpoint's delegate then receives the request DTO as is. The
 * annotation is repeatable, one per spec.
 *
 * A response DTO is filled from the entity, a request DTO is read into the generated create or
 * update command (see [DomainCreate]). Every property of either must be mapped, or the build fails.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
@Repeatable
annotation class RestEntity(
    val read: KClass<*> = Unit::class,
    val readAll: KClass<*> = Unit::class,
    val create: KClass<*> = Unit::class,
    val update: KClass<*> = Unit::class,
)

/**
 * The REST DTO property of this property or parameter, when it differs from the domain: in the
 * response DTO and in the request DTOs alike. Without it, a response DTO property has the name of
 * the entity property, and a request DTO property the name of the command property it is written
 * to.
 *
 * On a property in the class body, it also adds the property to the response DTO.
 *
 * @property name The DTO property name; blank means the domain name.
 * @property type The REST DTO type to convert the value to. Like [RestEntity], only the name of the
 *   type is used. [Unit] means no conversion.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
annotation class RestProperty(
    val name: String = "",
    val type: KClass<*> = Unit::class,
)
