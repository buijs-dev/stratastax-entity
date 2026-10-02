# Annotations

Examples of every annotation in `dev.buijs.stratastax.entity.core.spi.annotations`. The examples are taken
(simplified) from `stratastax-engine`. Annotations from other libraries, such as `@Filter` and `@Sort`, are left
out. See the [README](README.md#terminology) for the terminology used here.

- [Domain](#domain)
    - [`@DomainEntity`](#domainentity)
    - [`@CreatesDomainEntity` and `@UpdatesDomainEntity`](#createsdomainentity-and-updatesdomainentity)
    - [`@DomainEntityProjection`](#domainentityprojection)
- [Command matching](#command-matching)
    - [`@DomainCreateProperty` and `@DomainUpdateProperty`](#domaincreateproperty-and-domainupdateproperty)
- [Persistence](#persistence)
    - [`@PersistenceEntity` and `@PersistenceColumn`](#persistenceentity-and-persistencecolumn)
    - [`@PersistenceId`, `@PersistenceVersion` and `@PersistenceSoftDelete`](#persistenceid-persistenceversion-and-persistencesoftdelete)
- [Generated values](#generated-values)
    - [`@DomainCreateFunction`](#domaincreatefunction)
    - [`@DomainUpdateFunction`](#domainupdatefunction)
    - [`@PersistenceGenerated`](#persistencegenerated)
- [Relations](#relations)
    - [`@PersistenceJoin` with `@PersistenceManageLinks`](#persistencejoin-with-persistencemanagelinks)
    - [`@PersistenceJoin` with `@PersistenceManageChildren`](#persistencejoin-with-persistencemanagechildren)
    - [`@PersistenceJoinMany` with `@PersistenceManageChildren`](#persistencejoinmany-with-persistencemanagechildren)
    - [`@PersistenceJoinMany` with `@PersistenceManageLinks`](#persistencejoinmany-with-persistencemanagelinks)
    - [Read-only relations](#read-only-relations)
- [API](#api)
    - [`@ApiEntity`](#apientity)
    - [`@ApiRead`](#apiread)
    - [`@ApiType`](#apitype)

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

A class that is also used as the group type of a `@PersistenceJoin`/`@PersistenceJoinMany`, or that is a
`@DomainEntityProjection`, is never a root.

### `@CreatesDomainEntity` and `@UpdatesDomainEntity`

Mark a data class as the create or update command of an entity. Use `PatchField` for the properties of an
update command, so that a property that was not supplied (`Unset`) is distinguished from one explicitly set to
`null`.

```kotlin
@CreatesDomainEntity(Playset::class)
data class CreatePlayset(
    val gameId: GameId,
    val displayName: String?,
    val variants: List<String>,
    val scenario: String?,
    val expansions: List<ExpansionId>,
)

@UpdatesDomainEntity(Playset::class)
data class UpdatePlayset(
    val displayName: PatchField<String> = PatchField.Unset,
    val scenario: PatchField<String> = PatchField.Unset,
    val variants: PatchField<List<String>> = PatchField.Unset,
    val expansions: PatchField<List<ExpansionId>> = PatchField.Unset,
)
```

How command properties are matched to entity properties is described in [Command matching](#command-matching).

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

## Command matching

### `@DomainCreateProperty` and `@DomainUpdateProperty`

Each entity property that is set by a command is matched to a property of the create/update command. In order
of priority:

1. An explicit `@DomainCreateProperty(property)` / `@DomainUpdateProperty(property)`.
2. An exact name match: `Playset.displayName` ↔ `CreatePlayset.displayName`.
3. The command property whose name is the entity property name plus a suffix `Id` or `Ref`, matched on a
   camelCase boundary: `Playset.game` ↔ `CreatePlayset.gameId`, `MatchParticipant.ghostPlayer` ↔
   `CreateMatchParticipant.ghostPlayerId`.
4. For a relation (group): the single command property of the target type, for example
   `User.canonicalPlayer` ↔ `CreateUser.canonicalPlayer: CreateCanonicalPlayer`.

When more than one candidate matches, the build fails with an ambiguity error. When an exact match exists but
a near miss does too (`provider` vs `providerUserId`), the exact match wins and a warning is logged.

The annotations are therefore only needed when the names cannot be matched. Without a `property` argument
they have no effect.

```kotlin
@DomainEntity
@PersistenceEntity("MATCH")
data class Match(
    // Matched by name: no annotation needed.
    @PersistenceColumn("PLAYED_AT")
    val playedAt: OffsetDateTime,

    // Command property has a different name: map it explicitly.
    @DomainUpdateProperty("finalizedAt")
    @PersistenceColumn("LOCKED_AT")
    val lockedAt: OffsetDateTime?,
)

@UpdatesDomainEntity(Match::class)
data class UpdateMatch(
    val playedAt: PatchField<OffsetDateTime> = PatchField.Unset,
    val finalizedAt: PatchField<OffsetDateTime> = PatchField.Unset,
)
```

`@DomainCreateProperty` works the same way for the create command.

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

## Generated values

### `@DomainCreateFunction`

The value is generated by the application on create, by the `TypeGenerator` with the matching `key`. When the
key is blank, it is derived from the property type as `generate${Type}`, so all properties of one type share one
generator.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceId
    @DomainCreateFunction                      // key "generatePlaysetId"
    @PersistenceColumn("PUBLIC_ID")
    val id: PlaysetId,

    @DomainCreateFunction                      // key "generateOffsetDateTime"
    @PersistenceColumn("CREATED_AT")
    val createdAt: OffsetDateTime,
)

@ApplicationScoped
class PlaysetIdGenerator(private val uuidGenerator: StandardUuidGenerator) :
    TypeGenerator<PlaysetId>() {
    override fun generate(): PlaysetId = PlaysetId(uuidGenerator.generate())
}
```

Set a key to use a dedicated generator, for example to fill a relation with the current user:

```kotlin
@DomainCreateFunction("currentPrincipal")
@PersistenceJoin("OWNER_USER_ID")
val owner: UserRef,
```

```kotlin
internal class CurrentPrincipalGenerator : TypeGenerator<UserRef>("currentPrincipal") {
    override fun generate(): UserRef = ...
}
```

### `@DomainUpdateFunction`

Works like `@DomainCreateFunction`, but the value is regenerated on every update.

```kotlin
@DomainUpdateFunction
@PersistenceColumn("UPDATED_AT")
val updatedAt: OffsetDateTime? = null,
```

When a property has both, they must use the same key:

```kotlin
@DomainCreateFunction("currentPrincipal")
@DomainUpdateFunction("currentPrincipal")
@PersistenceJoin("LAST_EDITED_BY_USER_ID")
val lastEditedBy: UserRef?,
```

### `@PersistenceGenerated`

The value is generated by the database on insert (e.g. an identity column). Mutually exclusive with
`@DomainCreateFunction`.

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

`@PersistenceJoin` describes a 1:1 relation and `@PersistenceJoinMany` a 1:N relation. How the relation is
written is decided by `@PersistenceManageLinks` (only the link) or `@PersistenceManageChildren` (the link and
the target rows). The two are mutually exclusive.

### `@PersistenceJoin` with `@PersistenceManageLinks`

`from` is the foreign-key column of this entity. Only the foreign-key value is written; the target row is not
touched. The command supplies the id of the target (see [Command matching](#command-matching)).

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceManageLinks
    @PersistenceJoin("GAME_ID")
    val game: GameRef,
)

@CreatesDomainEntity(Playset::class)
data class CreatePlayset(val gameId: GameId)
```

### `@PersistenceJoin` with `@PersistenceManageChildren`

The target row is created, updated and deleted together with this entity. The create/update command contains
a property for the relation, typed as the target's create/update command.

```kotlin
@DomainEntity
@PersistenceEntity("APP_USER")
data class User(
    @PersistenceManageChildren
    @PersistenceJoin("CANONICAL_PLAYER_ID")
    val canonicalPlayer: CanonicalPlayer,
)

@CreatesDomainEntity(User::class)
data class CreateUser(
    val displayName: String,
    val canonicalPlayer: CreateCanonicalPlayer,
)
```

A managed child may be optional:

```kotlin
@PersistenceManageChildren
@PersistenceJoin("LOADOUT_ID")
val loadout: MatchLoadout?,
```

Only manage a target as a child when it is not shared with other parents; otherwise rows that are still
referenced elsewhere may be deleted.

### `@PersistenceJoinMany` with `@PersistenceManageChildren`

A direct foreign-key relation has a path of 2 elements: the key of this entity and the foreign key in the
target table.

```kotlin
@DomainEntity
@PersistenceEntity("MATCH")
data class Match(
    @PersistenceManageChildren(OneToManyUpdateStrategy.REPLACE_ALL)
    @PersistenceJoinMany("MATCH.ID", "MATCH_PARTICIPANT.MATCH_ID")
    val participants: List<MatchParticipant>,
)

@CreatesDomainEntity(Match::class)
data class CreateMatch(val participants: List<CreateMatchParticipant>)

@UpdatesDomainEntity(Match::class)
data class UpdateMatch(
    val participants: PatchField<List<CreateMatchParticipant>?> = PatchField.Unset,
)
```

`updateStrategy` decides how children are reconciled on update when the collection is supplied (not `Unset`). It
applies only to direct foreign-key relations:

| Strategy           | Behaviour                                                                                         |
|--------------------|---------------------------------------------------------------------------------------------------|
| `UPSERT` (default) | Matches children by identity: updates matches, inserts new elements, deletes missing rows.        |
| `REPLACE_ALL`      | Deletes all existing children and inserts every element as a new row with a generated identity. |

### `@PersistenceJoinMany` with `@PersistenceManageLinks`

A junction-table relation has a path of 3 elements: the key of this entity, the junction column referencing
this entity, and the junction column referencing the target. With `@PersistenceManageLinks` only the junction
rows are written; the target rows are not touched. The command supplies the target ids.

```kotlin
@DomainEntity
@PersistenceEntity("PLAYSET")
data class Playset(
    @PersistenceManageLinks
    @PersistenceJoinMany(
        "PLAYSET.ID",
        "PLAYSET_EXPANSION.PLAYSET_ID",
        "PLAYSET_EXPANSION.EXPANSION_ID",
    )
    val expansions: Set<ExpansionRef>,
)

@CreatesDomainEntity(Playset::class)
data class CreatePlayset(val expansions: List<ExpansionId>)

@UpdatesDomainEntity(Playset::class)
data class UpdatePlayset(
    val expansions: PatchField<List<ExpansionId>> = PatchField.Unset,
)
```

Junction-table relations always replace their link rows completely. With `@PersistenceManageChildren` on a
junction relation, the target rows are deleted as well.

### Read-only relations

A `@PersistenceJoin`/`@PersistenceJoinMany` without `@PersistenceManageLinks` or `@PersistenceManageChildren` is
read-only: it is not written by a command.

```kotlin
@DomainEntity
@PersistenceEntity("GAME")
data class Game(
    @PersistenceJoinMany("GAME.ID", "EXPANSION.GAME_ID")
    val expansions: List<ExpansionRef>,
)
```

The link of a read-only `@PersistenceJoin` can still be set on create by a generator, see
[`@DomainCreateFunction`](#domaincreatefunction):

```kotlin
@DomainCreateFunction("currentPrincipal")
@PersistenceJoin("OWNER_USER_ID")
val owner: UserRef,
```

## API

The API annotations link domain types to REST DTOs. The referenced DTO classes are not used as types: only
their names are matched against the OpenAPI spec loaded for a codegen run.

### `@ApiEntity`

Declares the DTO for a single entity (`read`) and for a list of entities (`readAll`).

```kotlin
@DomainEntity
@ApiEntity(PlaysetV1::class, PlaysetListV1::class)
data class Playset(...)
```

Only `read`:

```kotlin
@DomainEntity
@ApiEntity(PlaysetRefV1::class)
@PersistenceEntity("PLAYSET")
data class PlaysetRef(...)
```

### `@ApiRead`

Marks a property readable into the DTO. `property` names the DTO property when it differs from the domain name.
Use the `@param:` use-site target on a constructor parameter:

```kotlin
@DomainEntity
@ApiEntity(GameV1::class, GameListV1::class)
data class Game(
    @param:ApiRead("displayName")
    @PersistenceColumn("NAME")
    val name: String,
)
```

On a computed property in the class body:

```kotlin
@DomainEntity
@ApiEntity(MatchV1::class, MatchListV1::class)
data class Match(
    @PersistenceColumn("LOCKED_AT")
    val lockedAt: OffsetDateTime?,
) {
    @ApiRead
    val status: MatchStatus =
        when {
            lockedAt == null -> MatchStatus.Draft
            else -> MatchStatus.Finalized
        }
}
```

### `@ApiType`

Declares the DTO type to convert a property to; implies `@ApiRead`. Like `@ApiEntity`, only the name of the type
is used.

```kotlin
@DomainEntity
@ApiEntity(MatchBatchResultItemV1::class)
data class MatchBatchResultItem(
    @param:ApiType(MatchBatchResultItemV1.StatusEnum::class)
    val status: MatchBatchResultStatus,
    val match: Match? = null,
)
```
