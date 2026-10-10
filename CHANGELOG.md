# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-10-10

### Added

First release of Stratastax Entity: annotations to describe an entity once and let the Stratastax tooling
generate commands, persistence, REST mappers, search and CSV import from it.

- Annotations for entities, commands, generated values, relations, REST, search, type conversion and CSV import.
- `PatchField` for partial updates.
- `EntityField` and `Sort` for search.
- `FieldMapping`, `TypeConverter` and `TypeGenerator` SPIs.

See [ANNOTATIONS.md](https://github.com/buijs-dev/stratastax-entity/blob/main/ANNOTATIONS.md) for examples.
