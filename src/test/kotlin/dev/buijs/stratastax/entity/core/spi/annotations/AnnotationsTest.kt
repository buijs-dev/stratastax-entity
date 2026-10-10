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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import kotlin.reflect.KClass
import java.lang.annotation.Repeatable as JavaRepeatable
import java.lang.annotation.Retention as JavaRetention
import java.lang.annotation.Target as JavaTarget

/**
 * The annotations have `SOURCE` retention and are only read at build time, so what can be verified
 * here is the contract the README documents: retention, target, defaults and attribute values.
 */
class AnnotationsTest {

    private class Anything

    private fun KClass<*>.javaRetention() =
        this.java.getAnnotation(JavaRetention::class.java)?.value

    private fun KClass<*>.javaTargets() =
        this.java
            .getAnnotation(JavaTarget::class.java)
            ?.value
            ?.toList()

    companion object {

        @JvmStatic
        fun parameterAnnotations() =
            listOf(
                DomainCreate::class,
                DomainUpdate::class,
                DomainCreateGenerated::class,
                DomainUpdateGenerated::class,
                PersistenceColumn::class,
                PersistenceId::class,
                PersistenceVersion::class,
                PersistenceSoftDelete::class,
                PersistenceGenerated::class,
                PersistenceJoinOne::class,
                PersistenceJoinMany::class,
                PersistenceJoinManyThrough::class,
                PersistenceManageLinks::class,
                PersistenceManageChildren::class,
                SearchFilter::class,
                SearchFixedFilter::class,
                SearchSort::class,
                CsvColumn::class,
                CsvIgnore::class,
            )

        @JvmStatic
        fun classAnnotations() =
            listOf(
                DomainEntity::class,
                DomainEntityProjection::class,
                PersistenceEntity::class,
                RestEntity::class,
                BuildTypeConverter::class,
                BuildTypeGenerator::class,
                CsvImport::class,
            )

        @JvmStatic
        fun allAnnotations() = (parameterAnnotations() + classAnnotations() + RestProperty::class)
    }

    @ParameterizedTest
    @MethodSource("allAnnotations")
    fun `every annotation has SOURCE retention`(annotation: KClass<out Annotation>) {
        assertThat(annotation.javaRetention()).isEqualTo(RetentionPolicy.SOURCE)
    }

    @ParameterizedTest
    @MethodSource("parameterAnnotations")
    fun `constructor parameter annotations target VALUE_PARAMETER only`(
        annotation: KClass<out Annotation>,
    ) {
        assertThat(annotation.javaTargets()).containsExactly(ElementType.PARAMETER)
    }

    @ParameterizedTest
    @MethodSource("classAnnotations")
    fun `class annotations target CLASS only`(annotation: KClass<out Annotation>) {
        assertThat(annotation.javaTargets()).containsExactly(ElementType.TYPE)
    }

    @Test
    fun `RestProperty targets parameters and properties`() {
        // PROPERTY has no Java ElementType, so only PARAMETER is visible here.
        assertThat(RestProperty::class.javaTargets()).containsExactly(ElementType.PARAMETER)
    }

    @Test
    fun `RestEntity and BuildTypeConverter are repeatable`() {
        assertThat(RestEntity::class.java.getAnnotation(JavaRepeatable::class.java)).isNotNull()
        assertThat(BuildTypeConverter::class.java.getAnnotation(JavaRepeatable::class.java))
            .isNotNull()
    }

    @Test
    fun `annotations without arguments default to blank`() {
        assertThat(DomainCreateGenerated().key).isEmpty()
        assertThat(DomainUpdateGenerated().key).isEmpty()
        assertThat(PersistenceEntity().table).isEmpty()
        assertThat(RestProperty().name).isEmpty()
        assertThat(DomainCreate().property).isEmpty()
        assertThat(DomainUpdate().property).isEmpty()
        assertThat(DomainCreate().type).isEqualTo(Unit::class)
        assertThat(DomainUpdate().type).isEqualTo(Unit::class)
        assertThat(BuildTypeGenerator(Anything::class).key).isEmpty()
    }

    @Test
    fun `RestProperty converts to no type by default`() {
        assertThat(RestProperty().type).isEqualTo(Unit::class)
    }

    @Test
    fun `RestEntity defaults every DTO to Unit`() {
        // given
        val api = RestEntity()

        // expect
        assertThat(api.read).isEqualTo(Unit::class)
        assertThat(api.readAll).isEqualTo(Unit::class)
        assertThat(api.create).isEqualTo(Unit::class)
        assertThat(api.update).isEqualTo(Unit::class)
    }

    @Test
    fun `RestEntity and RestProperty keep their types`() {
        assertThat(RestEntity(String::class, List::class).read)
            .isEqualTo(String::class)
        assertThat(RestEntity(String::class, List::class).readAll)
            .isEqualTo(List::class)
        assertThat(RestEntity(create = Set::class, update = Map::class).create)
            .isEqualTo(Set::class)
        assertThat(RestEntity(create = Set::class, update = Map::class).update)
            .isEqualTo(Map::class)
        assertThat(RestProperty(type = Anything::class).type)
            .isEqualTo(Anything::class)
    }

    @Test
    fun `entity references are kept`() {
        assertThat(DomainEntityProjection(Anything::class).entity).isEqualTo(Anything::class)
        assertThat(BuildTypeConverter(String::class).target).isEqualTo(String::class)
        assertThat(BuildTypeGenerator(Anything::class).source).isEqualTo(Anything::class)
    }

    @Test
    fun `explicit arguments are kept`() {
        assertThat(DomainCreateGenerated("owner").key)
            .isEqualTo("owner")
        assertThat(DomainCreate(Anything::class, "matchId").type)
            .isEqualTo(Anything::class)
        assertThat(DomainCreate(Anything::class, "matchId").property)
            .isEqualTo("matchId")
        assertThat(DomainUpdate(property = "finalizedAt").property)
            .isEqualTo("finalizedAt")
        assertThat(PersistenceColumn("PLAYSET.ID").reference)
            .isEqualTo("PLAYSET.ID")
        assertThat(PersistenceEntity("PLAYSET").table)
            .isEqualTo("PLAYSET")
        assertThat(PersistenceJoinOne("GAME_ID").foreignKey)
            .isEqualTo("GAME_ID")
        assertThat(PersistenceJoinMany("MATCH_PARTICIPANT.MATCH_ID").foreignKey)
            .isEqualTo("MATCH_PARTICIPANT.MATCH_ID")
        assertThat(RestProperty("displayName").name)
            .isEqualTo("displayName")
        assertThat(CsvColumn("id").name)
            .isEqualTo("id")
        assertThat(BuildTypeGenerator(Anything::class, "ownerId").key)
            .isEqualTo("ownerId")
    }

    @Test
    fun `PersistenceJoinManyThrough keeps both junction columns`() {
        // given
        val join =
            PersistenceJoinManyThrough("MATCH_EXPANSION.MATCH_ID", "MATCH_EXPANSION.EXPANSION_ID")

        // expect
        assertThat(join.foreignKey).isEqualTo("MATCH_EXPANSION.MATCH_ID")
        assertThat(join.targetForeignKey).isEqualTo("MATCH_EXPANSION.EXPANSION_ID")
    }

    @Test
    fun `PersistenceManageChildren defaults to UPSERT`() {
        assertThat(PersistenceManageChildren().updateStrategy)
            .isEqualTo(OneToManyUpdateStrategy.UPSERT)
        assertThat(PersistenceManageChildren(OneToManyUpdateStrategy.REPLACE_ALL).updateStrategy)
            .isEqualTo(OneToManyUpdateStrategy.REPLACE_ALL)
    }

    @Test
    fun `SearchFilter has no alias by default`() {
        assertThat(SearchFilter().alias).isEmpty()
    }

    @Test
    fun `SearchFilter keeps its alias`() {
        assertThat(SearchFilter(alias = "name").alias).isEqualTo("name")
    }

    @Test
    fun `SearchFixedFilter keeps its value`() {
        assertThat(SearchFixedFilter("null").value).isEqualTo("null")
    }

    @Test
    fun `SearchSort is ascending and not a tiebreaker by default`() {
        // given
        val sort = SearchSort()

        // expect
        assertThat(sort.tiebreaker).isFalse()
        assertThat(sort.direction).isEqualTo(SortDirection.ASC)
    }

    @Test
    fun `SearchSort keeps tiebreaker and direction`() {
        // given
        val sort = SearchSort(tiebreaker = true, direction = SortDirection.DESC)

        // expect
        assertThat(sort.tiebreaker).isTrue()
        assertThat(sort.direction).isEqualTo(SortDirection.DESC)
    }
}
