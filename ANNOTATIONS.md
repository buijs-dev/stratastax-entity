# Annotations by example

Every section starts with something you want to do, shows the annotations that do it and, where code is generated,
what you get. See the [README](README.md#terminology) for the terminology used here and the
[reference](#reference) at the end for a one-line summary of every annotation. 
The [stratastax-codegen › codegen-gradle-plugin](https://github.com/buijs-dev/stratastax-codegen/tree/main/codegen-gradle-plugin) is responsible
for the actual code generation (without it these annotations are worthless).

1. [Define an entity](#define-an-entity)
2. [Let callers create and update it](#let-callers-create-and-update-it)
3. [Protect against concurrent edits and keep deleted rows](#protect-against-concurrent-edits-and-keep-deleted-rows)
4. [Generate values instead of asking for them](#generate-values-instead-of-asking-for-them)
5. [Relate entities](#relate-entities)
6. [Read a lighter view](#read-a-lighter-view)
7. [Expose it over REST](#expose-it-over-rest)
8. [Make it searchable](#make-it-searchable)
9. [Convert enums and sealed types](#convert-enums-and-sealed-types)
10. [Import it from CSV](#import-it-from-csv)
11. [Reference](#reference)

## Define an entity

A data class with `@DomainEntity` is an entity. `@PersistenceEntity` names its table, `@PersistenceColumn` maps each
property to a column and `@PersistenceId` marks the identity.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")        // PLAYSET.PUBLIC_ID
    val id: PlaysetId,

    @PersistenceColumn("DISPLAY_NAME")     // PLAYSET.DISPLAY_NAME
    val displayName: String,
)
```

Good to know:
- Without a table name, `@PersistenceEntity` uses the uppercased class name: `Playset` → `PLAYSET`.
- A bare column name is resolved against that table; a name containing `.` (`"GAME.NAME"`) is used as written. Bare
  names only work on a `@PersistenceEntity`; elsewhere, such as on a [projection](#read-a-lighter-view), write
  fully qualified names.
- A class that is the target type of a [relation](#relate-entities), or a
  [projection](#read-a-lighter-view), is never a root entity, even with `@DomainEntity`.

## Let callers create and update it

Mark the properties a caller may set with `@DomainCreate` and/or `@DomainUpdate`. You get a `Create{Entity}` and an
`Update{Entity}` command: plain data classes, in the package and with the visibility of the entity.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
internal data class Game(
    @PersistenceId
    @DomainCreateGenerated
    @PersistenceColumn("PUBLIC_ID")
    val id: GameId,

    @DomainCreate
    @DomainUpdate
    @PersistenceColumn("NAME")
    val name: String,

    @DomainCreate
    @DomainUpdate
    @PersistenceColumn("STATUS")
    val status: ApprovalStatus = ApprovalStatus.Pending,

    @DomainCreate
    @PersistenceColumn("PUBLISHER")
    val publisher: String?,
)

// generated
internal data class CreateGame(
    val name: String,
    val status: ApprovalStatus = ApprovalStatus.Pending,
    val publisher: String?,
)

internal data class UpdateGame(
    val name: PatchField<String> = PatchField.Unset,
    val status: PatchField<ApprovalStatus> = PatchField.Unset,
)
```

Every update property is a `PatchField`, so a caller only sends what changes:

| Caller sends                 | Effect                                                                      |
|------------------------------|-----------------------------------------------------------------------------|
| `PatchField.Unset` (default) | the property is left as it is                                               |
| `PatchField.Value(x)`        | the property is set to `x`                                                  |
| `PatchField.Value(null)`     | the property is cleared; for a collection, it is set to an empty collection |

Good to know:
- Nullability follows the entity property, and a Kotlin default is copied to the create command so the caller may
  leave it out.
- An entity without `@DomainCreate` properties has no create command; the same goes for `@DomainUpdate`.
- `property` renames the command property: `@DomainCreate(property = "matchId")`. Not on a
  `@PersistenceJoinManyThrough` relation, which always keeps the name of the entity property.
- `type` sets the command property type when it can't be derived: `@DomainCreate(type = FooId::class)`.
- Generated values, `@PersistenceVersion`, `@PersistenceSoftDelete` and read-only relations can't be in a command.
- Relations are in a command too, see [Relate entities](#relate-entities).
- Commands are always generated; there are no hand-written commands.

## Protect against concurrent edits and keep deleted rows

`@PersistenceVersion` marks the optimistic-locking version. `@PersistenceSoftDelete` marks a nullable timestamp that
is set on delete instead of deleting the row.

```kotlin
@DomainEntity
@PersistenceEntity("COMMUNITY")
data class Community(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: CommunityId,

    @PersistenceVersion
    @PersistenceColumn("VERSION")
    val version: Long,

    @PersistenceSoftDelete
    @PersistenceColumn("DELETED_AT")
    val deletedAt: OffsetDateTime? = null,
)
```

Good to know:
- Soft-deleted rows are left out of every search: `@PersistenceSoftDelete` implies `@SearchFixedFilter("null")`.
  Don't combine it with `@SearchFilter`, `@SearchFixedFilter` or `@SearchSort`.

## Generate values instead of asking for them

Ids, timestamps and the current user are not supplied by the caller but generated.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceId
    @DomainCreateGenerated                       // on create, by the generator for PlaysetId
    @PersistenceColumn("PUBLIC_ID")
    val id: PlaysetId,

    @DomainCreateGenerated                       // on create, by the generator for OffsetDateTime
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,

    @DomainUpdateGenerated                       // on every update
    @PersistenceColumn("UPDATED_AT")
    val updatedAt: OffsetDateTime? = null,

    @DomainCreateGenerated("currentPrincipal")   // on create and update, by the "currentPrincipal" generator
    @DomainUpdateGenerated("currentPrincipal")
    @PersistenceJoinOne("LAST_EDITED_BY_USER_ID")
    val lastEditedBy: UserRef?,
)
```

The values come from `TypeGenerator` beans. Without a key, the generator is found by type; with a key, by that key:

```kotlin
@ApplicationScoped
class OffsetDateTimeGenerator : TypeGenerator<OffsetDateTime>() {
    override fun generate(): OffsetDateTime = OffsetDateTime.now()
}

internal class CurrentPrincipalGenerator : TypeGenerator<UserRef>("currentPrincipal") {
    override fun generate(): UserRef = ...
}
```

For a wrapper type such as `PlaysetId`, let `@BuildTypeGenerator` write the generator. It wraps every value of the
`source` generator:

```kotlin
@BuildTypeGenerator(StandardUuidGenerator::class)
@JvmInline
value class PlaysetId(val value: UUID)

// generated
@ApplicationScoped
class PlaysetIdGenerator(private val source: StandardUuidGenerator) : TypeGenerator<PlaysetId>() {
    override fun generate(): PlaysetId = PlaysetId(source.generate())
}
```

A value the database generates, such as an identity column, is marked with `@PersistenceGenerated`:

```kotlin
@DomainEntity
@PersistenceEntity("APP_USER_IDENTITY")
data class UserIdentity(
    @PersistenceId
    @PersistenceGenerated
    @PersistenceColumn("ID")
    val id: UserIdentityId,
)
```

Good to know:
- Without a key, the key is `generate{Type}` (`generatePlaysetId`), so all properties of one type share one
  generator. The key of a `TypeGenerator` must be a string literal: it is read from source at build time.
- A property with both `@DomainCreateGenerated` and `@DomainUpdateGenerated` uses the same key in both.
- `@DomainCreateGenerated` and `@PersistenceGenerated` can't be combined.
- A `@BuildTypeGenerator` bean has the package and visibility of the wrapper type and the key `generate{Type}`. Set
  `key` to serve a keyed property instead: `@BuildTypeGenerator(StandardUuidGenerator::class, key = "correlationId")`.
  `source` is only matched by name. A hand-written `TypeGenerator` with the same key is a build error.

## Relate entities

Pick the annotations by what the entity should do with the other side:

| I want to …                                         | Use                                                       |
|-----------------------------------------------------|-----------------------------------------------------------|
| [refer to another entity](#refer-to-another-entity) | `@PersistenceJoinOne` + `@PersistenceManageLinks`         |
| [own one child](#own-one-child)                     | `@PersistenceJoinOne` + `@PersistenceManageChildren`      |
| [own a list of children](#own-a-list-of-children)   | `@PersistenceJoinMany` + `@PersistenceManageChildren`     |
| [link many to many](#link-many-to-many)             | `@PersistenceJoinManyThrough` + `@PersistenceManageLinks` |
| [only read the relation](#only-read-the-relation)   | any `@PersistenceJoin…`, without a `@PersistenceManage…`  |

A relation only names foreign-key columns; the key columns they reference are resolved from the database schema.
`@PersistenceManageLinks` writes only the link (a foreign-key value or junction rows). `@PersistenceManageChildren`
also creates, updates and deletes the rows on the other side, so only use it when no other parent shares them.

### Refer to another entity

`foreignKey` is a column of this entity. Only that value is written; the command takes the id of the target.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @DomainCreate
    @PersistenceManageLinks
    @PersistenceJoinOne("GAME_ID")
    val game: GameRef,
)

// generated
data class CreatePlayset(val gameId: GameId)
```

The id type is the `@PersistenceId` type of the entity a `GameRef` [projection](#read-a-lighter-view) reads, else
`GameId` next to `GameRef`; set `@DomainCreate(type = …)` when neither applies.

### Own one child

The child row is created, updated and deleted together with this entity. The command takes the child's own create
command, built from the child's `@DomainCreate` properties.

```kotlin
@DomainEntity
@PersistenceEntity("APP_USER")
data class User(
    @DomainCreate
    @PersistenceColumn("DISPLAY_NAME")
    val displayName: String,

    @DomainCreate
    @PersistenceManageChildren
    @PersistenceJoinOne("CANONICAL_PLAYER_ID")
    val canonicalPlayer: CanonicalPlayer,     // may also be nullable: an optional child
)

// generated
data class CreateUser(
    val displayName: String,
    val canonicalPlayer: CreateCanonicalPlayer,
)
```

A child owned through `@PersistenceJoinOne` can't be changed by the update command.

### Own a list of children

`foreignKey` is the column of the child table that references this entity; a bare name is resolved against the
child's table.

```kotlin
@DomainEntity
@PersistenceEntity("MATCH")
data class Match(
    @DomainCreate
    @DomainUpdate
    @PersistenceManageChildren(OneToManyUpdateStrategy.REPLACE_ALL)
    @PersistenceJoinMany("MATCH_PARTICIPANT.MATCH_ID")
    val participants: List<MatchParticipant>,
)

// generated
data class CreateMatch(val participants: List<CreateMatchParticipant>)

data class UpdateMatch(
    val participants: PatchField<List<CreateMatchParticipant>?> = PatchField.Unset,
)
```

The strategy decides what happens to the children when an update supplies the list:

| Strategy           | On update                                                                                 |
|--------------------|-------------------------------------------------------------------------------------------|
| `UPSERT` (default) | matches children by identity: updates matches, inserts new elements, deletes missing rows |
| `REPLACE_ALL`      | deletes all children and inserts every element as a new row with a generated identity     |

Only `REPLACE_ALL` can be in an update command.

### Link many to many

`foreignKey` is the junction column that references this entity, `targetForeignKey` the one that references the
target; both are fully qualified. Only junction rows are written; the command takes the target ids.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @DomainCreate
    @DomainUpdate
    @PersistenceManageLinks
    @PersistenceJoinManyThrough("PLAYSET_EXPANSION.PLAYSET_ID", "PLAYSET_EXPANSION.EXPANSION_ID")
    val expansions: Set<ExpansionRef>,
)

// generated
data class CreatePlayset(val expansions: List<ExpansionId>)

data class UpdatePlayset(
    val expansions: PatchField<List<ExpansionId>> = PatchField.Unset,
)
```

An update always replaces all junction rows. With `@PersistenceManageChildren` instead, the target rows are deleted
as well.

### Only read the relation

Without `@PersistenceManageLinks` or `@PersistenceManageChildren`, a relation is read but never written by a command.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @PersistenceJoinMany("EXPANSION.GAME_ID")
    val expansions: List<ExpansionRef>,
)
```

A read-only `@PersistenceJoinOne` can still be filled on create by a
[generator](#generate-values-instead-of-asking-for-them):

```kotlin
@DomainCreateGenerated("currentPrincipal")
@PersistenceJoinOne("OWNER_USER_ID")
val owner: UserRef,
```

### Summary

Supported combinations:

|                               | read-only | `@PersistenceManageLinks` | `@PersistenceManageChildren` | in update command  |
|-------------------------------|-----------|---------------------------|------------------------------|--------------------|
| `@PersistenceJoinOne`         | ✓         | ✓                         | ✓                            | links only         |
| `@PersistenceJoinMany`        | ✓         |                           | ✓                            | `REPLACE_ALL` only |
| `@PersistenceJoinManyThrough` | ✓         | ✓                         | ✓                            | links only         |

Command properties:

| Entity property                                                      | Create command                | Update command                             |
|----------------------------------------------------------------------|-------------------------------|--------------------------------------------|
| scalar `x: T`                                                        | `x: T`                        | `x: PatchField<T>`                         |
| `@PersistenceJoinOne` + `@PersistenceManageLinks`, `foo: FooRef`     | `fooId: FooId`                | `fooId: PatchField<FooId>`                 |
| `@PersistenceJoinManyThrough` + `@PersistenceManageLinks`            | `foos: List<FooId>`           | `foos: PatchField<List<FooId>>`            |
| `@PersistenceJoinOne` + `@PersistenceManageChildren`, `child: Child` | `child: CreateChild`          | not supported                              |
| `@PersistenceJoinMany` + `@PersistenceManageChildren(REPLACE_ALL)`   | `children: List<CreateChild>` | `children: PatchField<List<CreateChild>?>` |

## Read a lighter view

`@DomainEntityProjection` reads only some columns of an entity into another data class. The read access of the
entity then gets `findAll{Projection}` and `findAll{Projection}List` methods.

```kotlin
@DomainEntityProjection(Game::class)
data class GameRef(
    @PersistenceColumn("GAME.PUBLIC_ID")
    val id: GameId,

    @PersistenceColumn("GAME.NAME")
    val displayName: String,
)

suspend fun getGameSuggestions(query: String): List<GameRef> =
    readAccess.findAllGameRef { filter { and(search.name.like(query)) } }
```

Good to know:
- The target (`Game`) must be a `@DomainEntity`; the projection itself is not.
- Columns of a projection are always fully qualified.

## Expose it over REST

`@RestEntity` names the DTOs of the OpenAPI spec loaded for a codegen run. For every entity with a matching DTO, a
`{Entity}RestMapper` is generated that fills response DTOs from the entity and reads request DTOs into the
[commands](#let-callers-create-and-update-it).

```kotlin
@DomainEntity
@RestEntity(GameV1::class, GameListV1::class)                  // read, readAll
data class Game(
    @DomainCreate
    @param:RestProperty("displayName")                         // "displayName" in the DTOs
    @PersistenceColumn("NAME")
    val name: String,
)
```

The four DTOs are `read` (one entity), `readAll` (a page), `create` and `update`. Leave one out to use the schema
named by convention (`game`, `game_list`, `create_game`, `update_game`), or pass `Nothing::class` when there is no
DTO to map, e.g. a create the generated mapping can't express; the endpoint's delegate then gets the request DTO as is:

```kotlin
@RestEntity(PlaysetRefV1::class)                                                    // only read
@RestEntity(ClaimRequestV1::class, ClaimRequestListV1::class, create = Nothing::class)
```

`@RestProperty` on a computed property adds it to the response DTO:

```kotlin
@DomainEntity
@RestEntity(MatchV1::class, MatchListV1::class)
data class Match(
    @PersistenceColumn("LOCKED_AT")
    val lockedAt: OffsetDateTime?,
) {
    @RestProperty
    val status: MatchStatus = if (lockedAt == null) MatchStatus.Draft else MatchStatus.Finalized
}
```

`type` converts the value to a DTO type, matched by name only:

```kotlin
@DomainEntity
@RestEntity(MatchBatchResultItemV1::class)
data class MatchBatchResultItem(
    @param:RestProperty(type = MatchBatchResultItemV1.StatusEnum::class)
    val status: MatchBatchResultStatus,
    val match: Match? = null,
)
```

Good to know:
- DTO classes are only matched by name against the spec; they are never used as types.
- A response DTO property matches `@RestProperty(name)`, else the entity property name. A request DTO property
  matches `@RestProperty(name)`, else the command property name (`gameId` for `game`).
- Every DTO property must be mapped, or the build fails.
- Use the `@param:` use-site target on a constructor parameter.
- `@RestEntity` is repeatable, one per spec.
- `@SearchFilter(alias)` plays no part in the DTO mapping.

## Make it searchable

`@SearchFilter` lets callers filter on a property, `@SearchSort` sort on it, and `@SearchFixedFilter` applies a
filter callers can't see or change.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @SearchSort(tiebreaker = true)
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: GameId,

    @SearchFilter(alias = "title")                 // ?filter=title==foo
    @PersistenceColumn("NAME")
    val name: String,

    @SearchSort(direction = SortDirection.DESC)
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,

    @SearchFixedFilter("true")                     // only published games, always
    @PersistenceColumn("PUBLISHED")
    val published: Boolean,
)
```

Good to know:
- Without `alias`, callers filter on the property name (`?filter=name==foo`).
- `direction` is the default sort direction, `ASC` unless set.
- Cursor pagination needs exactly one `tiebreaker`: the field that makes the order total.
- A fixed filter value is parsed like an RSQL value: `null`, `true`/`false`, a plain number, or else a string.
  Timestamps and lists can't be expressed; use the DSL's `fixedValue(...)` for those.
- A fixed filter can't be combined with `@SearchFilter` or `@SearchSort`, and a `@PersistenceSoftDelete` property
  needs none.

## Convert enums and sealed types

`@BuildTypeConverter` generates a pair of `TypeConverter`s, one per direction, between an enum (or a sealed
interface of `data object`s) and `target`.

```kotlin
@BuildTypeConverter(String::class)          // WinLoss <-> "WIN_LOSS"
@BuildTypeConverter(MatchResultV1::class)   // WinLoss <-> MatchResultV1.WIN_LOSS
sealed interface MatchResult {
    data object WinLoss : MatchResult
    data object Draw : MatchResult
}
```

Good to know:
- Members are mapped by name, ignoring case and underscores.
- `target` is `String` or another closed type, matched by name only.
- The annotation is repeatable, so a type can convert to several targets.

## Import it from CSV

`@CsvImport` generates a `CsvRowMapper` (from `persistence-csv`) that reads one CSV row. On an entity it reads the
[create command](#let-callers-create-and-update-it).

```kotlin
@CsvImport
@DomainEntity
@PersistenceEntity("BGG_RANKING")
internal data class BggRankingEntry(
    @DomainCreate
    @CsvColumn("id")                        // read from column "id"
    @PersistenceColumn("BGG_ID")
    val bggId: Long,

    @DomainCreate                           // read from column "name"
    @PersistenceColumn("NAME")
    val name: String,

    @DomainCreate
    @CsvIgnore                              // not read, uses the default
    @PersistenceColumn("SOURCE")
    val source: String = "bgg",
)

// generated
internal class CreateBggRankingEntryCsvRowMapper : CsvRowMapper<CreateBggRankingEntry>
```

Good to know:
- Column names are matched ignoring case and underscores.
- On an entity, every `@DomainCreate` property is read unless it is `@CsvIgnore`, which then needs a default.
- On any other class, such as a plain data class, every constructor property is read unless it is `@CsvIgnore`.

## Reference

All annotations are in `dev.buijs.stratastax.entity.core.spi.annotations`. See the
[API docs](https://buijs-dev.github.io/stratastax-entity/) for every parameter.

| Annotation                    | Does                                                      | Example                                                                             |
|-------------------------------|-----------------------------------------------------------|-------------------------------------------------------------------------------------|
| `@DomainEntity`               | marks a data class as an entity                           | [Define an entity](#define-an-entity)                                               |
| `@DomainEntityProjection`     | reads part of an entity into another class                | [Read a lighter view](#read-a-lighter-view)                                         |
| `@DomainCreate`               | adds a property to the create command                     | [Create and update](#let-callers-create-and-update-it)                              |
| `@DomainUpdate`               | adds a property to the update command, as a `PatchField`  | [Create and update](#let-callers-create-and-update-it)                              |
| `@DomainCreateGenerated`      | generates the value on create, by a `TypeGenerator`       | [Generated values](#generate-values-instead-of-asking-for-them)                     |
| `@DomainUpdateGenerated`      | generates the value on every update, by a `TypeGenerator` | [Generated values](#generate-values-instead-of-asking-for-them)                     |
| `@PersistenceEntity`          | sets the table of an entity                               | [Define an entity](#define-an-entity)                                               |
| `@PersistenceColumn`          | maps a property to a column                               | [Define an entity](#define-an-entity)                                               |
| `@PersistenceId`              | marks the identity                                        | [Define an entity](#define-an-entity)                                               |
| `@PersistenceVersion`         | marks the optimistic-locking version                      | [Versions and soft delete](#protect-against-concurrent-edits-and-keep-deleted-rows) |
| `@PersistenceSoftDelete`      | marks the soft-delete timestamp                           | [Versions and soft delete](#protect-against-concurrent-edits-and-keep-deleted-rows) |
| `@PersistenceGenerated`       | lets the database generate the value                      | [Generated values](#generate-values-instead-of-asking-for-them)                     |
| `@PersistenceJoinOne`         | to-one relation through a foreign key of this entity      | [Refer to another entity](#refer-to-another-entity)                                 |
| `@PersistenceJoinMany`        | to-many relation through a foreign key of the target      | [Own a list of children](#own-a-list-of-children)                                   |
| `@PersistenceJoinManyThrough` | many-to-many relation through a junction table            | [Link many to many](#link-many-to-many)                                             |
| `@PersistenceManageLinks`     | writes only the link of a relation                        | [Relate entities](#relate-entities)                                                 |
| `@PersistenceManageChildren`  | writes the link and the rows on the other side            | [Relate entities](#relate-entities)                                                 |
| `@RestEntity`                 | names the REST DTOs of an entity                          | [Expose it over REST](#expose-it-over-rest)                                         |
| `@RestProperty`               | renames, adds or converts a DTO property                  | [Expose it over REST](#expose-it-over-rest)                                         |
| `@SearchFilter`               | lets callers filter on a property                         | [Make it searchable](#make-it-searchable)                                           |
| `@SearchFixedFilter`          | applies a filter callers can't change                     | [Make it searchable](#make-it-searchable)                                           |
| `@SearchSort`                 | lets callers sort on a property                           | [Make it searchable](#make-it-searchable)                                           |
| `@BuildTypeConverter`         | generates `TypeConverter`s for an enum or sealed type     | [Convert enums and sealed types](#convert-enums-and-sealed-types)                   |
| `@BuildTypeGenerator`         | generates a `TypeGenerator` for a wrapper type            | [Generated values](#generate-values-instead-of-asking-for-them)                     |
| `@CsvImport`                  | generates a `CsvRowMapper`                                | [Import it from CSV](#import-it-from-csv)                                           |
| `@CsvColumn`                  | reads a property from another CSV column                  | [Import it from CSV](#import-it-from-csv)                                           |
| `@CsvIgnore`                  | leaves a property out of the CSV row                      | [Import it from CSV](#import-it-from-csv)                                           |
