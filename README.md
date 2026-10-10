# Stratastax Entity
[![Stratastax](https://img.shields.io/badge/Product-stratastax.dev-4F46E5?style=flat)](https://stratastax.dev/)
[![](https://img.shields.io/badge/Buijs-Software-blue)](https://buijs.dev/)
[![GitHub](https://img.shields.io/github/license/buijs-dev/stratastax-entity?color=black)](https://github.com/buijs-dev/stratastax-entity/blob/main/LICENSE)
[![codecov](https://codecov.io/gh/buijs-dev/stratastax-entity/graph/badge.svg?token=BNKJJFHIcP)](https://codecov.io/gh/buijs-dev/stratastax-entity)
[![CodeScene Average Code Health](https://codescene.io/projects/85873/status-badges/average-code-health)](https://codescene.io/projects/85873)
[![CodeScene System Mastery](https://codescene.io/projects/85873/status-badges/system-mastery)](https://codescene.io/projects/85873)

Building blocks for annotation-driven entity modeling on the JVM (Kotlin). This library (`entity`) contains
no runtime logic of its own for persistence or REST: it defines the annotations and small runtime types that the
Stratastax tools read and use to generate code.

## Purpose
Describe an entity once, in plain Kotlin data classes and let the Stratastax tooling derive the rest:
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

## Annotations
See [ANNOTATIONS.md](ANNOTATIONS.md) for examples by use case.

## Related Stratastax projects
- [stratastax-codegen › codegen-gradle-plugin](https://github.com/buijs-dev/stratastax-codegen/tree/main/codegen-gradle-plugin) - runs the code generator over the entity annotations of this library
- [stratastax-search › search-core](https://github.com/buijs-dev/stratastax-search/tree/main/search-core) - implements the `@Search*` annotations: sorting, RSQL filtering and cursor pagination
- [stratastax-persistence › persistence-core](https://github.com/buijs-dev/stratastax-persistence/tree/main/persistence-core) - implements the `@Persistence*` annotations on top of jOOQ
- [stratastax-http › problem-core](https://github.com/buijs-dev/stratastax-http/tree/main/problem-core) - RFC 9457 problems behind the error handling of the `@RestEntity` endpoints
- [stratastax-quarkus › quarkus-starter](https://github.com/buijs-dev/stratastax-quarkus/tree/main/quarkus-starter) - wires the generated entity code into a Quarkus application
- [stratastax-style](https://github.com/buijs-dev/stratastax-style) - the Kotlin code style this library and the generated code are formatted with
