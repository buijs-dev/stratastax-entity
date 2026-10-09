# Annotations

Examples of every annotation in `dev.buijs.stratastax.entity.core.spi.annotations`. The examples are taken
(simplified) from `stratastax-engine`. See the [README](README.md#terminology) for the terminology used here.

- [Domain](#domain)
    - [`@DomainEntity`](#domainentity)
    - [`@DomainCreate` and `@DomainUpdate`](#domaincreate-and-domainupdate)
    - [`@DomainEntityProjection`](#domainentityprojection)
- [Persistence](#persistence)
    - [`@PersistenceEntity` and `@PersistenceColumn`](#persistenceentity-and-persistencecolumn)
    - [`@PersistenceId`, `@PersistenceVersion` and `@PersistenceSoftDelete`](#persistenceid-persistenceversion-and-persistencesoftdelete)
- [Generated values](#generated-values)
    - [`@DomainCreateGenerated`](#domaincreategenerated)
    - [`@DomainUpdateGenerated`](#domainupdategenerated)
    - [`@PersistenceGenerated`](#persistencegenerated)
- [Relations](#relations)
    - [`@PersistenceJoinOne` with `@PersistenceManageLinks`](#persistencejoinone-with-persistencemanagelinks)
    - [`@PersistenceJoinOne` with `@PersistenceManageChildren`](#persistencejoinone-with-persistencemanagechildren)
    - [`@PersistenceJoinMany` with `@PersistenceManageChildren`](#persistencejoinmany-with-persistencemanagechildren)
    - [`@PersistenceJoinManyThrough` with `@PersistenceManageLinks`](#persistencejoinmanythrough-with-persistencemanagelinks)
    - [Read-only relations](#read-only-relations)
- [REST](#rest)
    - [`@RestEntity`](#restentity)
    - [`@RestProperty`](#restproperty)
- [Search](#search)
    - [`@SearchFilter`](#searchfilter)
    - [`@SearchFixedFilter`](#searchfixedfilter)
    - [`@SearchSort`](#searchsort)
- [Types](#types)
    - [`@BuildTypeConverter`](#buildtypeconverter)
    - [`@BuildTypeGenerator`](#buildtypegenerator)
- [CSV](#csv)
    - [`@CsvImport`](#csvimport)
    - [`@CsvColumn` and `@CsvIgnore`](#csvcolumn-and-csvignore)

## Domain

### `@DomainEntity`

Marks a data class as a root entity.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: PlaysetId,

    @PersistenceColumn("DISPLAY_NAME")
    val displayName: String,
)
```

A class that is also used as the group type of a [relation](#relations), or that is a `@DomainEntityProjection`,
is never a root.

### `@DomainCreate` and `@DomainUpdate`

Mark the entity properties a command may set. The create and update command are then generated as
`Create${Entity}` and `Update${Entity}`, in the package and with the visibility of the entity:

```kotlin
@DomainEntity
@PersistenceEntity("CLAIM_REQUEST")
internal data class ClaimRequest(
    @PersistenceId
    @DomainCreateGenerated
    @PersistenceColumn("PUBLIC_ID")
    val id: ClaimRequestId,

    @DomainCreate
    @PersistenceManageLinks
    @PersistenceJoinOne("CANONICAL_PLAYER_ID")
    val canonicalPlayer: CanonicalPlayerRef,

    @DomainCreate
    @DomainUpdate
    @PersistenceColumn("STATUS")
    val status: ApprovalStatus = ApprovalStatus.Pending,

    @DomainCreate
    @DomainUpdate
    @PersistenceManageLinks
    @PersistenceJoinManyThrough("CLAIM_REQUEST_TARGET.CLAIM_REQUEST_ID", "CLAIM_REQUEST_TARGET.GHOST_PLAYER_ID")
    val ghostPlayers: List<GhostPlayerRef>,
)
```

Generates plain data classes, without annotations:

```kotlin
internal data class CreateClaimRequest(
    val canonicalPlayerId: CanonicalPlayerId,
    val status: ApprovalStatus = ApprovalStatus.Pending,
    val ghostPlayers: List<GhostPlayerId>,
)

internal data class UpdateClaimRequest(
    val status: PatchField<ApprovalStatus> = PatchField.Unset,
    val ghostPlayers: PatchField<List<GhostPlayerId>> = PatchField.Unset,
)
```

How a command property is derived from the entity property:

| Entity property                                                     | Create command                 | Update command                             |
|---------------------------------------------------------------------|--------------------------------|--------------------------------------------|
| scalar `x: T`                                                       | `x: T`                         | `x: PatchField<T>`                         |
| `@PersistenceJoinOne` + `@PersistenceManageLinks`, `foo: FooRef`    | `fooId: FooId`                 | `fooId: PatchField<FooId>`                 |
| `@PersistenceJoinManyThrough` + `@PersistenceManageLinks`           | `foos: List<FooId>`            | `foos: PatchField<List<FooId>>`            |
| `@PersistenceJoinOne` + `@PersistenceManageChildren`, `child: Child` | `child: CreateChild`          | not supported                              |
| `@PersistenceJoinMany` + `@PersistenceManageChildren(REPLACE_ALL)`  | `children: List<CreateChild>`  | `children: PatchField<List<CreateChild>?>` |

- Nullability follows the entity property. A Kotlin default on the entity property is copied to the create command,
  so the caller may leave it out.
- The id type of a relation target is the `@PersistenceId` type of the entity a `@DomainEntityProjection` reads, else
  `FooId` next to a `FooRef`. Set `type` when neither applies: `@DomainCreate(type = FooId::class)`.
- Set `property` to name the command property differently: `@DomainCreate(property = "matchId")`.
- A managed child's create command is generated from the child's own `@DomainCreate` properties.
- Generated values (`@DomainCreateGenerated`/`@DomainUpdateGenerated`, `@PersistenceGenerated`), `@PersistenceVersion`
  and `@PersistenceSoftDelete` can't be set by a command, and neither can a read-only relation.
- An update property is a `PatchField`: `Unset` leaves the property as it is, `Value` sets it, `Value(null)`
  included. For a collection that is never `null`, `Value(null)` is written as an empty collection.
- Commands are only generated; there are no hand-written commands.
- [`@CsvImport`](#csvimport) on the entity reads the generated create command from CSV: every `@DomainCreate`
  property, by its `@CsvColumn` name or its own name. A `@CsvIgnore` property is not read and needs a default.

### `@DomainEntityProjection`

Links a data class to a `@DomainEntity` as a read-only projection. The generated code then contains read/readAll
methods that fetch only the projected data from the database. The target must be a `@DomainEntity`; the
projection itself is not annotated with `@DomainEntity`.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: GameId,

    @PersistenceColumn("NAME")
    val name: String,

    @PersistenceColumn("DESCRIPTION")
    val description: String,

    // ...
)

@DomainEntityProjection(Game::class)
data class GameRef(
    @PersistenceColumn("GAME.PUBLIC_ID")
    val id: GameId,

    @PersistenceColumn("GAME.NAME")
    val displayName: String,
)
```

Using the generated projection method:

```kotlin
suspend fun getGameSuggestions(query: String): List<GameRef> =
    readAccess.findAllGameRef { filter { and(search.name.like(query)) } }
```

## Persistence

### `@PersistenceEntity` and `@PersistenceColumn`

`@PersistenceEntity` declares the default table of a class. `@PersistenceColumn` maps a constructor parameter
to a column. A bare column name is resolved against the default table; a reference containing `.` is used as
written.

```kotlin
@DomainEntity
@PersistenceEntity("MATCH_LOADOUT")
data class MatchLoadout(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")            // MATCH_LOADOUT.PUBLIC_ID
    val id: MatchLoadoutId,

    @PersistenceColumn("ATTRIBUTES")           // MATCH_LOADOUT.ATTRIBUTES
    val attributes: Map<String, Any?>,
)
```

When `table` is blank, it is derived from the uppercased simple class name:

```kotlin
@DomainEntity
@PersistenceEntity                             // table "PLAYSET"
data class Playset(
    @PersistenceColumn("PUBLIC_ID")            // PLAYSET.PUBLIC_ID
    val id: PlaysetId,
)
```

Bare names are only resolved on a `@PersistenceEntity`; elsewhere, such as on a projection, use fully qualified
references:

```kotlin
@DomainEntityProjection(Game::class)
data class GameRef(
    @PersistenceColumn("GAME.PUBLIC_ID")
    val id: GameId,
)
```

### `@PersistenceId`, `@PersistenceVersion` and `@PersistenceSoftDelete`

Mark the identity field, the optimistic-locking version field and the soft-delete column of an entity.

```kotlin
@DomainEntity
@PersistenceEntity("COMMUNITY")
data class Community(
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: CommunityId,

    @PersistenceSoftDelete
    @PersistenceColumn("DELETED_AT")
    val deletedAt: OffsetDateTime? = null,

    @PersistenceVersion
    @PersistenceColumn("VERSION")
    val version: Long,
)
```

The soft-delete column is a nullable timestamp that is set to the current time on delete. It implies
`@SearchFixedFilter("null")`, so soft-deleted rows are excluded from search results without further annotations.
Do not combine it with `@SearchFilter`, `@SearchFixedFilter` or `@SearchSort`.

## Generated values

### `@DomainCreateGenerated`

The value is generated by the application on create, by the `TypeGenerator` with the matching `key`. When the
key is blank, it is derived from the property type as `generate${Type}`, so all properties of one type share one
generator.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceId
    @DomainCreateGenerated                      // key "generatePlaysetId"
    @PersistenceColumn("PUBLIC_ID")
    val id: PlaysetId,

    @DomainCreateGenerated                      // key "generateOffsetDateTime"
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,
)

@ApplicationScoped
class PlaysetIdGenerator(private val uuidGenerator: StandardUuidGenerator) :
    TypeGenerator<PlaysetId>() {
    override fun generate(): PlaysetId = PlaysetId(uuidGenerator.generate())
}
```

A generator for a wrapper type like this one can also be generated with
[`@BuildTypeGenerator`](#buildtypegenerator); then don't write it by hand.

Set a key to use a dedicated generator, for example to fill a relation with the current user:

```kotlin
@DomainCreateGenerated("currentPrincipal")
@PersistenceJoinOne("OWNER_USER_ID")
val owner: UserRef,
```

```kotlin
internal class CurrentPrincipalGenerator : TypeGenerator<UserRef>("currentPrincipal") {
    override fun generate(): UserRef = ...
}
```

### `@DomainUpdateGenerated`

Works like `@DomainCreateGenerated`, but the value is regenerated on every update.

```kotlin
@DomainUpdateGenerated
@PersistenceColumn("UPDATED_AT")
val updatedAt: OffsetDateTime? = null,
```

When a property has both, they must use the same key:

```kotlin
@DomainCreateGenerated("currentPrincipal")
@DomainUpdateGenerated("currentPrincipal")
@PersistenceJoinOne("LAST_EDITED_BY_USER_ID")
val lastEditedBy: UserRef?,
```

### `@PersistenceGenerated`

The value is generated by the database on insert (e.g. an identity column). Mutually exclusive with
`@DomainCreateGenerated`.

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

## Relations

Each relation names only foreign-key columns; the key columns they reference are resolved from the database schema.

| Annotation                    | Relation                                         | Arguments                                  |
|-------------------------------|--------------------------------------------------|--------------------------------------------|
| `@PersistenceJoinOne`         | to-one (N:1 or 1:1), foreign key on this entity  | `foreignKey`: column of this entity        |
| `@PersistenceJoinMany`        | to-many (1:N), foreign key on the target         | `foreignKey`: column of the target         |
| `@PersistenceJoinManyThrough` | to-many (M:N) through a junction table           | `foreignKey`, `targetForeignKey`: columns of the junction table |

How the relation is written is decided by `@PersistenceManageLinks` (only the link) or `@PersistenceManageChildren`
(the link and the target rows). The two are mutually exclusive, and not every combination is supported:

|                               | read-only | `@PersistenceManageLinks` | `@PersistenceManageChildren` | `REPLACE_ALL` |
|-------------------------------|-----------|---------------------------|------------------------------|---------------|
| `@PersistenceJoinOne`         | ✓         | ✓                         | ✓                            |               |
| `@PersistenceJoinMany`        | ✓         |                           | ✓                            | ✓             |
| `@PersistenceJoinManyThrough` | ✓         | ✓                         | ✓                            |               |

### `@PersistenceJoinOne` with `@PersistenceManageLinks`

`foreignKey` is the foreign-key column of this entity. Only the foreign-key value is written; the target row is not
touched. The command supplies the id of the target.

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

### `@PersistenceJoinOne` with `@PersistenceManageChildren`

The target row is created, updated and deleted together with this entity. The create command contains a property
for the relation, typed as the target's create command.

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
    val canonicalPlayer: CanonicalPlayer,
)

// generated
data class CreateUser(
    val displayName: String,
    val canonicalPlayer: CreateCanonicalPlayer,
)
```

A managed child may be optional:

```kotlin
@PersistenceManageChildren
@PersistenceJoinOne("LOADOUT_ID")
val loadout: MatchLoadout?,
```

Only manage a target as a child when it is not shared with other parents; otherwise rows that are still
referenced elsewhere may be deleted.

### `@PersistenceJoinMany` with `@PersistenceManageChildren`

`foreignKey` is the column of the target table that references this entity. A bare column name is resolved
against the default table of the target type.

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

`updateStrategy` decides how children are reconciled on update when the collection is supplied (not `Unset`). It
applies only to `@PersistenceJoinMany`:

| Strategy           | Behaviour                                                                                         |
|--------------------|---------------------------------------------------------------------------------------------------|
| `UPSERT` (default) | Matches children by identity: updates matches, inserts new elements, deletes missing rows.        |
| `REPLACE_ALL`      | Deletes all existing children and inserts every element as a new row with a generated identity. |

### `@PersistenceJoinManyThrough` with `@PersistenceManageLinks`

`foreignKey` is the junction column referencing this entity and `targetForeignKey` the junction column
referencing the target; both are fully qualified. With `@PersistenceManageLinks` only the junction rows are
written; the target rows are not touched. The command supplies the target ids.

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

Junction-table relations always replace their link rows completely. With `@PersistenceManageChildren` on a
`@PersistenceJoinManyThrough`, the target rows are deleted as well.

### Read-only relations

A relation without `@PersistenceManageLinks` or `@PersistenceManageChildren` is read-only: it is not written by a
command.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @PersistenceJoinMany("EXPANSION.GAME_ID")
    val expansions: List<ExpansionRef>,
)
```

The link of a read-only `@PersistenceJoinOne` can still be set on create by a generator, see
[`@DomainCreateGenerated`](#domaincreategenerated):

```kotlin
@DomainCreateGenerated("currentPrincipal")
@PersistenceJoinOne("OWNER_USER_ID")
val owner: UserRef,
```

## REST

The REST annotations link domain types to the DTOs of the OpenAPI spec loaded for a codegen run. The referenced DTO
classes are not used as types: only their names are matched against the spec. For every `@DomainEntity` with a
matching DTO, a `${Entity}RestMapper` is generated:

- a response DTO (`read`, `readAll`) is filled from the entity;
- a request DTO (`create`, `update`) is read into the generated create or update command
  (see [`@DomainCreate` and `@DomainUpdate`](#domaincreate-and-domainupdate)).

Every property of a DTO must be mapped, or the build fails: a response property no entity property fills, and a
request property no command property receives, are both errors.

A DTO property matches:

| DTO                  | Matched to                                                                                               |
|----------------------|----------------------------------------------------------------------------------------------------------|
| response             | `@RestProperty(name)`, else the entity property name                                                     |
| request              | `@RestProperty(name)`, else the command property name (`gameId` for `game`, `groupIds` for `groups` too) |

`@SearchFilter(alias)` only names a search field; it plays no part in the DTO mapping.

### `@RestEntity`

Declares the DTOs of an entity: one entity (`read`), a page of entities (`readAll`), and the request of its create
(`create`) and update (`update`). Each is optional and falls back to the schema named by convention (`playset`,
`playset_list`, `create_playset`, `update_playset` for `Playset`). The annotation is repeatable, one per spec.

```kotlin
@DomainEntity
@RestEntity(PlaysetV1::class, PlaysetListV1::class)
data class Playset(...)
```

Only `read`:

```kotlin
@DomainEntity
@RestEntity(PlaysetRefV1::class)
@PersistenceEntity("PLAYSET")
data class PlaysetRef(...)
```

`Nothing::class` declares there is no DTO to map in that direction: the endpoint's delegate receives the request DTO
as is, for a create the generated mapping can't express.

```kotlin
@DomainEntity
@RestEntity(ClaimRequestV1::class, ClaimRequestListV1::class, create = Nothing::class)
data class ClaimRequest(...)
```

### `@RestProperty`

Names the DTO property of a property when it differs from the domain, in the response and the request DTOs alike.
Use the `@param:` use-site target on a constructor parameter:

```kotlin
@DomainEntity
@RestEntity(GameV1::class, GameListV1::class)
data class Game(
    @DomainCreate
    @param:RestProperty("displayName")
    @PersistenceColumn("NAME")
    val name: String,
)
```

On a computed property in the class body, it also adds the property to the response DTO:

```kotlin
@DomainEntity
@RestEntity(MatchV1::class, MatchListV1::class)
data class Match(
    @PersistenceColumn("LOCKED_AT")
    val lockedAt: OffsetDateTime?,
) {
    @RestProperty
    val status: MatchStatus =
        when {
            lockedAt == null -> MatchStatus.Draft
            else -> MatchStatus.Finalized
        }
}
```

`type` declares the DTO type to convert a property to. Like `@RestEntity`, only the name of the type is used.

```kotlin
@DomainEntity
@RestEntity(MatchBatchResultItemV1::class)
data class MatchBatchResultItem(
    @param:RestProperty(type = MatchBatchResultItemV1.StatusEnum::class)
    val status: MatchBatchResultStatus,
    val match: Match? = null,
)
```

## Search

### `@SearchFilter`

Marks a property as filterable by callers.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @SearchFilter
    @PersistenceColumn("NAME")
    val name: String,
)
```

`alias` overrides the name callers use: `?filter=title==foo` instead of `?filter=name==foo`. It plays no part in
the REST DTO mapping; use [`@RestProperty`](#restproperty) for that.

```kotlin
@SearchFilter(alias = "title")
@PersistenceColumn("NAME")
val name: String,
```

### `@SearchFixedFilter`

Applies a filter `property == value` to every search. Callers cannot see or override it. `value` is parsed like an
RSQL value token: `null`, `true`/`false`, a plain number, or else a literal string. Timestamps and lists cannot be
expressed this way; use the DSL's `fixedValue(...)` for those.

```kotlin
@DomainEntity
@PersistenceEntity("ARTICLE")
data class Article(
    @SearchFixedFilter("true")
    @PersistenceColumn("PUBLISHED")
    val published: Boolean,
)
```

A fixed filter is mutually exclusive with `@SearchFilter` and `@SearchSort`. A `@PersistenceSoftDelete` field needs
no fixed filter: it already implies `@SearchFixedFilter("null")`.

### `@SearchSort`

Marks a property as sortable. `direction` is the default direction (`SortDirection.ASC` unless set).
`tiebreaker` marks the single field that makes the order total; cursor pagination needs exactly one per entity.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @SearchSort(tiebreaker = true)
    @PersistenceId
    @PersistenceColumn("PUBLIC_ID")
    val id: GameId,

    @SearchSort(direction = SortDirection.DESC)
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,
)
```

## Types

### `@BuildTypeConverter`

Generates a `TypeConverter` pair between the annotated enum (or sealed interface of `data object`s) and `target`,
one per direction, mapped member by member. `target` is `String` (`WinLoss` ↔ `"WIN_LOSS"`) or another closed type
whose members match by name, ignoring case and underscores. Only the name of the target is used. The annotation is
repeatable, so one type can convert to several targets.

```kotlin
@BuildTypeConverter(String::class)
@BuildTypeConverter(MatchResultV1::class)
sealed interface MatchResult {
    data object WinLoss : MatchResult
    data object Draw : MatchResult
}
```

### `@BuildTypeGenerator`

Generates a `TypeGenerator` bean for a wrapper type: a class with a single constructor property, such as a
`@JvmInline value class`. The bean injects `source`, a `TypeGenerator` of the wrapped type, and wraps every value it
generates. Only the name of `source` is used.

```kotlin
@BuildTypeGenerator(StandardUuidGenerator::class)
@JvmInline
value class PlaysetId(val value: UUID)
```

Generates:

```kotlin
@ApplicationScoped
class PlaysetIdGenerator(private val source: StandardUuidGenerator) : TypeGenerator<PlaysetId>() {
    override fun generate(): PlaysetId = PlaysetId(source.generate())
}
```

The bean has the same package and visibility as the annotated type. Its key is derived from the type
(`generatePlaysetId`), so it serves every [`@DomainCreateGenerated`](#domaincreategenerated) and
[`@DomainUpdateGenerated`](#domainupdategenerated) property of that type.
Set `key` to serve a keyed property instead:

```kotlin
@BuildTypeGenerator(StandardUuidGenerator::class, key = "correlationId")
@JvmInline
value class CorrelationId(val value: UUID)
```

A hand-written `TypeGenerator` with the same key is a build error.

## CSV

### `@CsvImport`

Generates a `CsvRowMapper` (from `persistence-csv`) that reads one CSV row. On a `@DomainEntity`, it reads the entity's
generated create command: every [`@DomainCreate`](#domaincreate-and-domainupdate) property, unless it is `@CsvIgnore`.

```kotlin
@CsvImport
@DomainEntity
@PersistenceEntity("BGG_RANKING")
internal data class BggRankingEntry(
    @DomainCreate
    @CsvColumn("id")
    @PersistenceColumn("BGG_ID")
    val bggId: Long,

    @DomainCreate
    @PersistenceColumn("NAME")
    val name: String,
)
```

Generates `CreateBggRankingEntryCsvRowMapper : CsvRowMapper<CreateBggRankingEntry>`. On any other class, such as a
plain data class, it reads every constructor property that isn't `@CsvIgnore`.

### `@CsvColumn` and `@CsvIgnore`

A property is read from the column with its own name; column names are matched ignoring case and underscores.
`@CsvColumn(name)` reads it from another column. `@CsvIgnore` leaves it out; on a `@DomainEntity`, it needs a default
value.

```kotlin
@DomainCreate
@CsvIgnore
@PersistenceColumn("SOURCE")
val source: String = "bgg",
```
