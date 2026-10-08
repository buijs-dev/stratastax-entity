# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `@SearchFilter`, `@SearchSort` and `@SearchFixedFilter`, replacing `@Filter`/`@Sort` from `search-core`.
- `@PersistenceJoinManyThrough` for M:N relations through a junction table.
- `RestProperty.type`, replacing `@ApiType`.
- `@BuildTypeGenerator` to generate a `TypeGenerator` bean for a wrapper type.
- `@DomainCreate` and `@DomainUpdate` to generate the create and update command from the entity's properties.
- `PatchField.nullAs(replacement)`, so an explicit `null` can mean e.g. an empty collection.
- `@CsvImport`, `@CsvColumn` and `@CsvIgnore`, moved from `persistence-csv`. `@CsvImport` may be put on a `@DomainEntity`.

### Changed

- `@GenerateTypeConverter` is renamed to `@BuildTypeConverter`.
- `@DomainCreateFunction`/`@DomainUpdateFunction` are renamed to `@DomainCreateGenerated`/`@DomainUpdateGenerated`.
- `@DomainEntityProjection(target)` is renamed to `@DomainEntityProjection(entity)`.
- `@PersistenceJoin(from)` is renamed to `@PersistenceJoinOne(foreignKey)`.
- `@PersistenceJoinMany` takes only the target's `foreignKey`; the junction form moved to `@PersistenceJoinManyThrough`.
  Referenced key columns are resolved from the database schema.
- `@PersistenceSoftDelete` implies `@SearchFixedFilter("null")`.
- `@PersistenceColumn.reference` is required.
- `@ApiEntity`/`@ApiRead` are renamed to `@RestEntity`/`@RestProperty`. `@RestEntity` also declares the `create` and
  `update` request DTOs, `Nothing::class` meaning none; it requires at least one of its four DTOs. `@RestProperty(name)`
  names the DTO property in requests too, and `@SearchFilter(alias)` no longer binds DTO properties.

### Removed

- `@ApiType`; use `@RestProperty(type = ...)`.
- `@CreatesDomainEntity`/`@UpdatesDomainEntity` and `@DomainCreateProperty`/`@DomainUpdateProperty`: hand-written commands are
  no longer supported. Commands are generated from `@DomainCreate`/`@DomainUpdate`, and `property = ...` names a
  command property.
