# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `PatchField` (`Unset`/`Value`, `isProvided`) for PATCH semantics in update commands, with `orKeep`,
  `orKeepRequired`, `nullAs`, `toTargetIdsOrNull` and `orNullIfUnset`.
- `EntityField` and `EntityFieldType` to describe a searchable field, and `Sort`, `SortField`, `asc`/`desc` and
  `SortDirection` (`SortDirection.of` parses case-insensitively).
- `FieldMapping` and `UnknownFieldException` to map search field names to a backend-specific field.
- `TypeConverter` and `TypeGenerator`; a `TypeGenerator` is matched to generated properties by its string-literal
  `key`, or by type when it has none.
- Domain: `@DomainEntity` and `@DomainEntityProjection(entity)`. `@DomainCreate` and `@DomainUpdate` generate the
  `Create${Entity}` and `Update${Entity}` commands from the entity's properties (`PatchField` in the update command),
  with `type` and `property` to override the derived command property.
- Generated values: `@DomainCreateGenerated` and `@DomainUpdateGenerated` (by the application, through a
  `TypeGenerator`) and `@PersistenceGenerated` (by the database).
- Persistence: `@PersistenceEntity`, `@PersistenceColumn`, `@PersistenceId`, `@PersistenceVersion` and
  `@PersistenceSoftDelete`, which implies `@SearchFixedFilter("null")`.
- Relations: `@PersistenceJoinOne(foreignKey)` (N:1, 1:1), `@PersistenceJoinMany(foreignKey)` (1:N) and
  `@PersistenceJoinManyThrough(foreignKey, targetForeignKey)` (M:N through a junction table); referenced key columns
  are resolved from the database schema. `@PersistenceManageLinks` writes only the link, `@PersistenceManageChildren`
  also the target rows, reconciled by `OneToManyUpdateStrategy` (`UPSERT`, `REPLACE_ALL`).
- REST: `@RestEntity(read, readAll, create, update)` declares the DTOs of an entity, falling back to the schema named
  by convention, with `Nothing::class` meaning none; `@RestProperty(name, type)` names and converts a DTO property.
- Search: `@SearchFilter(alias)`, `@SearchFixedFilter(value)` and `@SearchSort(tiebreaker, direction)`.
- Types: `@BuildTypeConverter(target)` generates a `TypeConverter` pair for an enum or sealed hierarchy, and
  `@BuildTypeGenerator(source, key)` a `TypeGenerator` for a wrapper type.
- CSV: `@CsvImport`, `@CsvColumn` and `@CsvIgnore`; `@CsvImport` on a `@DomainEntity` reads its create command.
- README.md and ANNOTATIONS.md with the terminology and an example of every annotation.
