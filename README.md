# Stratastax Entity

[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/klutter?color=black)](https://github.com/buijs-dev/stratastax-entity/blob/main/LICENSE)


Building blocks for annotation-driven entity modelling on the JVM (Kotlin). This library (`entity-core`) contains
no runtime logic of its own for persistence or REST: it defines the annotations and small runtime types that the
Stratastax tools read and use to generate code.

## Purpose

Describe an entity once, in plain Kotlin data classes, and let the Stratastax tooling derive the rest:

- **Domain**: which classes are entities, and which commands create or update them.
- **Persistence**: how properties map to columns, ids, versions, soft deletes, generated values and relations.
- **API**: which REST DTOs an entity maps to.
- **Partial updates**: `PatchField` distinguishes a field that was not supplied from one that was explicitly `null`.

## Terminology

The annotations describe different aspects of the same Kotlin model. The following terms have specific
meanings throughout this project.

| Term                      | Meaning                                                                                                                                                                      |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Constructor parameter** | The Kotlin constructor parameter on which an annotation is placed. All persistence annotations target `VALUE_PARAMETER`.                                                     |
| **Property**              | The domain/entity property represented by a constructor parameter. This is the name used when matching domain commands and entity properties.                                |
| **Column**                | A database column to which a persistence property is mapped. A column is identified by `@PersistenceColumn` or resolved from the entity's default table.                     |
| **Entity**                | A domain object that is persisted. `@DomainEntity` describes the domain model; `@PersistenceEntity` describes its database representation.                                   |
| **Relation**              | An association from one entity to another entity or collection of entities. Relations are described by `@PersistenceJoin` and `@PersistenceJoinMany`.                        |
| **Link**                  | The database representation of a relation: either a foreign-key value or rows in a junction table. `@PersistenceManageLinks` manages links without managing the target rows. |
| **Child**                 | A target row that is owned by its parent and whose lifecycle is managed together with the parent. `@PersistenceManageChildren` manages the child rows as part of the parent. |
| **Command**               | A domain type used to create or update an entity, typically annotated with `@DomainCreateProperty` or `@DomainUpdateProperty`.                                               |
| **Target row**            | The database row on the other side of a relation. Whether it is merely linked or managed as a child determines how relation updates behave.                                  |

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

| Tool               | Uses                                                                                           |
|--------------------|------------------------------------------------------------------------------------------------|
| `gradle-plugin`    | Scans the annotations in source and generates code. Also reads the `key` of a `TypeGenerator`. |
| `persistence-jooq` | Generates the jOOQ persistence layer; consumes `PatchField` in `toUpdateEntry` and friends.    |

## Contents

Package `dev.buijs.stratastax.entity.core`:

### `api`

| Type                       | Description                                                                                   |
|----------------------------|-----------------------------------------------------------------------------------------------|
| `PatchField<T>`            | `Unset` or `Value(x)`; use it for optional properties of an update command (PATCH semantics). |
| `orKeep`, `orKeepRequired` | Merge a `PatchField` with the current value; the latter rejects `Value(null)`.                |
| `toTargetIdsOrNull`        | Target-id set for a many-to-many sync, or `null` when the relation must not be touched.       |
| `orNullIfUnset`            | Child collection of a managed 1:N group; an empty collection means "remove all children".     |
| `EntityField`              | A named field with its `EntityFieldType`, and whether it is sortable/filterable.              |
| `EntityFieldType`          | The data type of an `EntityField`.                                                            |

### `spi`

| Type                 | Description                                                                                                        |
|----------------------|--------------------------------------------------------------------------------------------------------------------|
| `FieldMapping<F>`    | Maps search field names to a backend-specific field `F`; unknown names throw `UnknownFieldException`.              |
| `TypeConverter<T,R>` | Converts a raw value to another type.                                                                              |
| `TypeGenerator<T>`   | Generates a value for a `@DomainCreateFunction`/`@DomainUpdateFunction` field. Its `key` must be a string literal. |

### `spi.annotations`

| Group       | Annotations                                                                                                                     |
|-------------|---------------------------------------------------------------------------------------------------------------------------------|
| Domain      | `@DomainEntity`, `@CreatesDomainEntity`, `@UpdatesDomainEntity`, `@DomainEntityProjection`                                      |
| Persistence | `@PersistenceEntity`, `@PersistenceColumn`, `@PersistenceId`, `@PersistenceVersion`, `@PersistenceSoftDelete`                   |
| Generated   | `@DomainCreateFunction`, `@DomainUpdateFunction` (application), `@PersistenceGenerated` (database)                              |
| Relations   | `@PersistenceJoin`, `@PersistenceJoinMany`, `@PersistenceManageLinks`, `@PersistenceManageChildren` (`OneToManyUpdateStrategy`) |
| Commands    | `@DomainCreateProperty`, `@DomainUpdateProperty`                                                                                |
| API         | `@ApiEntity`, `@ApiRead`, `@ApiType`                                                                                            |

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
    
    @PersistenceColumn("NAME") 
    val name: String,
)

@UpdatesDomainEntity(Playset::class)
data class UpdatePlayset(val name: PatchField<String> = PatchField.Unset)
```

### Generated values

A property that is not supplied by a command but generated is marked with one of:

| Annotation                   | Generated by                                                     | When            |
|------------------------------|------------------------------------------------------------------|-----------------|
| `@DomainCreateFunction(key)` | the application, via the `TypeGenerator` with the matching `key` | on create       |
| `@DomainUpdateFunction(key)` | the application, via the `TypeGenerator` with the matching `key` | on every update |
| `@PersistenceGenerated`      | the database (e.g. an identity column or column default)         | on create       |

The `key` is optional. When blank, it is derived from the property type as `generate${Type}`, so all properties of
one type share one generator. Set a key to use a dedicated generator. `@DomainCreateFunction` and
`@PersistenceGenerated` are mutually exclusive; when a property has both `@DomainCreateFunction` and
`@DomainUpdateFunction`, they must use the same key.

```kotlin
@DomainEntity
@PersistenceEntity(table = "MATCH")
data class Match(
    @PersistenceId
    @PersistenceGenerated
    @PersistenceColumn("ID")
    val id: Long,

    @DomainCreateFunction
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,

    @DomainUpdateFunction
    @PersistenceColumn("UPDATED_AT")
    val updatedAt: OffsetDateTime?,

    @DomainCreateFunction("currentPrincipal")
    @DomainUpdateFunction("currentPrincipal")
    @PersistenceJoin("LAST_EDITED_BY_USER_ID")
    val lastEditedBy: UserRef,
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
