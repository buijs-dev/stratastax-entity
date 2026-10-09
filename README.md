# Stratastax Entity
[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/stratastax-entity?color=black)](https://github.com/buijs-dev/stratastax-entity/blob/main/LICENSE)
[![CodeScene Average Code Health](https://codescene.io/projects/85873/status-badges/average-code-health)](https://codescene.io/projects/85873)
[![CodeScene System Mastery](https://codescene.io/projects/85873/status-badges/system-mastery)](https://codescene.io/projects/85873)

Building blocks for annotation-driven entity modelling on the JVM (Kotlin). This library (`entity-core`) contains
no runtime logic of its own for persistence or REST: it defines the annotations and small runtime types that the
Stratastax tools read and use to generate code.

## Purpose
Describe an entity once, in plain Kotlin data classes, and let the Stratastax tooling derive the rest:

- **Domain**: which classes are entities, and which commands create or update them.
- **Persistence**: how properties map to columns, ids, versions, soft deletes, generated values and relations.
- **REST**: which REST DTOs an entity maps to, and under which property names.
- **Search**: which properties callers may filter and sort on and which filters always apply.
- **Type conversion**: which enums and sealed hierarchies get a generated `TypeConverter` (`@BuildTypeConverter`).
- **Type generation**: which wrapper types get a generated `TypeGenerator` (`@BuildTypeGenerator`).
- **Partial updates**: `PatchField` distinguishes a field that was not supplied from one that was explicitly `null`.

## Terminology
The annotations describe different aspects of the same Kotlin model. The following terms have specific
meanings throughout this project.

| Term                      | Meaning                                                                                                                                                                                                     |
|---------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Constructor parameter** | The Kotlin constructor parameter on which an annotation is placed. Almost all annotations target `VALUE_PARAMETER`; the exceptions target a class, and `@RestProperty` also a property.                     |
| **Property**              | The domain/entity property represented by a constructor parameter. This is the name used when matching domain commands and entity properties.                                                               |
| **Column**                | A database column to which a persistence property is mapped. A column is identified by `@PersistenceColumn` or resolved from the entity's default table.                                                    |
| **Entity**                | A domain object that is persisted. `@DomainEntity` describes the domain model; `@PersistenceEntity` describes its database representation.                                                                  |
| **Relation**              | An association from one entity to another entity or collection of entities. Relations are described by `@PersistenceJoinOne`, `@PersistenceJoinMany` and `@PersistenceJoinManyThrough`.                     |
| **Link**                  | The database representation of a relation: either a foreign-key value or rows in a junction table. `@PersistenceManageLinks` manages links without managing the target rows.                                |
| **Child**                 | A target row that is owned by its parent and whose lifecycle is managed together with the parent. `@PersistenceManageChildren` manages the child rows as part of the parent.                                |
| **Command**               | A data class used to create or update an entity, generated from the entity's `@DomainCreate`/`@DomainUpdate` properties.                                                                                    |
| **Target row**            | The database row on the other side of a relation. Whether it is merely linked or managed as a child determines how relation updates behave.                                                                 |

### Mapping concepts
The same value can be described at different levels:

```text
Kotlin model
    │
    ├── constructor parameter
    │       │
    │       └── property
    │               │
    │               └── persistence mapping
    │                       │
    │                       └── database column
    │
    └── relation property
            │
            ├── link
            │     └── foreign key or junction-table rows
            │
            └── child
                  └── target rows managed with the parent
```

## Tools
The annotations in this library have `SOURCE` retention: they are consumed at build time and are not available at
runtime. The following Stratastax tools use them:

| Tool                    | Uses                                                                                            |
|-------------------------|-------------------------------------------------------------------------------------------------|
| `codegen-gradle-plugin` | Scans the annotations in source and generates code. Also reads the `key` of a `TypeGenerator`.  |
| `persistence-core`      | The jOOQ persistence layer; consumes `PatchField` in `toUpdateEntry` and friends.               |
| `search-core`           | Sorting, filtering and pagination on top of `EntityField`, `Sort` and `FieldMapping`.           |
| `persistence-csv`       | Runs the `CsvRowMapper` generated for `@CsvImport`.                                             |

## Contents
Package `dev.buijs.stratastax.entity.core`:

### `api`

| Type                       | Description                                                                                      |
|----------------------------|--------------------------------------------------------------------------------------------------|
| `PatchField<T>`            | `Unset` or `Value(x)`; use it for optional properties of an update command (PATCH semantics).    |
| `isProvided`               | `true` for `Value`, `false` for `Unset`.                                                         |
| `orKeep`, `orKeepRequired` | Merge a `PatchField` with the current value; the latter rejects `Value(null)`.                   |
| `nullAs`                   | Replaces `Value(null)` with a value, e.g. an empty collection; `Unset` stays `Unset`.            |
| `toTargetIdsOrNull`        | Target-id set for a many-to-many sync, or `null` when the relation must not be touched.          |
| `orNullIfUnset`            | Child collection of a managed 1:N group; an empty collection means "remove all children".        |
| `EntityField`              | A named field with its `EntityFieldType`, whether it is sortable/filterable and its enum values. |
| `EntityFieldType`          | The data type of an `EntityField`.                                                               |
| `Sort`                     | An ordered, non-empty list of `SortField`s.                                                      |
| `SortField`, `asc`, `desc` | A single sort criterion: an `EntityField` and a `SortDirection`.                                 |
| `SortDirection`            | `ASC` or `DESC`; `SortDirection.of` parses case-insensitively.                                   |

### `spi`

| Type                 | Description                                                                                                                                              |
|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `FieldMapping<F>`    | Maps search field names to a backend-specific field `F`; unknown names throw `UnknownFieldException`.                                                    |
| `TypeConverter<T,R>` | Converts a raw value to another type, `null` included. Generated by `@BuildTypeConverter`.                                                               |
| `TypeGenerator<T>`   | Generates a value for a `@DomainCreateGenerated`/`@DomainUpdateGenerated` field. Its `key` must be a string literal. Generated by `@BuildTypeGenerator`. |

### `spi.annotations`

| Group       | Annotations                                                                                                                                                       |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Domain      | `@DomainEntity`, `@DomainEntityProjection`                                                                                                                        |
| Persistence | `@PersistenceEntity`, `@PersistenceColumn`, `@PersistenceId`, `@PersistenceVersion`, `@PersistenceSoftDelete`                                                     |
| Generated   | `@DomainCreateGenerated`, `@DomainUpdateGenerated` (application), `@PersistenceGenerated` (database)                                                              |
| Relations   | `@PersistenceJoinOne`, `@PersistenceJoinMany`, `@PersistenceJoinManyThrough`, `@PersistenceManageLinks`, `@PersistenceManageChildren` (`OneToManyUpdateStrategy`) |
| Commands    | `@DomainCreate`, `@DomainUpdate`                                                                                                                                  |
| REST        | `@RestEntity`, `@RestProperty`                                                                                                                                    |
| Search      | `@SearchFilter`, `@SearchFixedFilter`, `@SearchSort`                                                                                                              |
| Types       | `@BuildTypeConverter`, `@BuildTypeGenerator`                                                                                                                      |
| CSV         | `@CsvImport`, `@CsvColumn`, `@CsvIgnore`                                                                                                                          |

See [ANNOTATIONS.md](ANNOTATIONS.md) for an example of every annotation.

## Example

```kotlin
@DomainEntity
@PersistenceEntity(table = "PLAYSET")
data class Playset(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: PlaysetId,

    @PersistenceVersion
    @PersistenceColumn("VERSION")
    val version: Long,

    @DomainCreate
    @DomainUpdate
    @PersistenceColumn("NAME")
    val name: String,
)

// generated
data class CreatePlayset(val name: String)

data class UpdatePlayset(val name: PatchField<String> = PatchField.Unset)
```

### Generated values
A property that is not supplied by a command but generated is marked with one of:

| Annotation                    | Generated by                                                     | When            |
|-------------------------------|------------------------------------------------------------------|-----------------|
| `@DomainCreateGenerated(key)` | the application, via the `TypeGenerator` with the matching `key` | on create       |
| `@DomainUpdateGenerated(key)` | the application, via the `TypeGenerator` with the matching `key` | on every update |
| `@PersistenceGenerated`       | the database (e.g. an identity column or column default)         | on create       |

The `key` is optional. When blank, it is derived from the property type as `generate${Type}`, so all properties of
one type share one generator (a `TypeGenerator` without a key serves those). Set a key to use a dedicated generator 
and pass the same literal to the `TypeGenerator` constructor. `@DomainCreateGenerated` and
`@PersistenceGenerated` are mutually exclusive; when a property has both `@DomainCreateGenerated` and
`@DomainUpdateGenerated`, they must use the same key.

```kotlin
@DomainEntity
@PersistenceEntity(table = "MATCH")
data class Match(
    @PersistenceId
    @PersistenceGenerated
    @PersistenceColumn("ID")
    val id: Long,

    @DomainCreateGenerated
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,

    @DomainUpdateGenerated
    @PersistenceColumn("UPDATED_AT")
    val updatedAt: OffsetDateTime?,

    @DomainCreateGenerated("currentPrincipal")
    @DomainUpdateGenerated("currentPrincipal")
    @PersistenceJoinOne("LAST_EDITED_BY_USER_ID")
    val lastEditedBy: UserRef?,
)

internal class CurrentPrincipalGenerator : TypeGenerator<UserRef>("currentPrincipal") {
    override fun generate(): UserRef = ...
}
```

Gotcha: `@PersistenceManageLinks` only manages the link between entities, `@PersistenceManageChildren` also creates,
updates and deletes the target rows. Choose the latter only if the target is not shared with other parents.

## Build
Requires Java 25 (see `.sdkmanrc`).

```shell
./gradlew build
```

## License
[MIT](LICENSE)

## Related Stratastax projects

- [stratastax-codegen › codegen-gradle-plugin](https://github.com/buijs-dev/stratastax-codegen/tree/main/codegen-gradle-plugin) - runs the code generator over the entity annotations of this library
- [stratastax-search › search-core](https://github.com/buijs-dev/stratastax-search/tree/main/search-core) - implements the `@Search*` annotations: sorting, RSQL filtering and cursor pagination
- [stratastax-persistence › persistence-core](https://github.com/buijs-dev/stratastax-persistence/tree/main/persistence-core) - implements the `@Persistence*` annotations on top of jOOQ
- [stratastax-http › problem-core](https://github.com/buijs-dev/stratastax-http/tree/main/problem-core) - RFC 9457 problems behind the `@Rest` error handling
- [stratastax-quarkus › quarkus-starter](https://github.com/buijs-dev/stratastax-quarkus/tree/main/quarkus-starter) - wires the generated entity code into a Quarkus application
- [stratastax-style](https://github.com/buijs-dev/stratastax-style) - the Kotlin code style this library and the generated code are formatted with
