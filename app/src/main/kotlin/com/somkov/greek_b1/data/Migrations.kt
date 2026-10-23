package com.somkov.greek_b1.data

import androidx.room3.migration.Migration

/**
 * The migrations of [AppDatabase]. The database holds the user's progress, so it is migrated,
 * never dropped and recreated: the builder has no destructive fallback, and a missing migration
 * makes the app fail to open the database rather than lose the data.
 *
 * The rules for a change of the schema:
 * - A schema version that has been installed on the phone never changes. Any change raises
 *   [AppDatabase.VERSION] and adds a migration from the previous version: here, or as an
 *   `AutoMigration` in the `@Database` annotation where Room can work it out. Room enforces
 *   this on the phone: it refuses to open a database whose schema differs from the code at the
 *   same version (the identity hash), so the app fails on its first query and the data is kept.
 * - The build exports the schema of the new version to `app/schemas/`; the file is committed
 *   with the change.
 * - `MigrationTest` (on the phone) migrates every exported version to the current one; a
 *   migration that moves data gets a test of its own that checks the data.
 */
val MIGRATIONS: List<Migration> = emptyList()
