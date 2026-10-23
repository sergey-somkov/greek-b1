package com.somkov.greek_b1.data

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query

/**
 * A table that only proves the setup of the database: Room needs at least one entity, and the
 * tests need a table and a DAO to check the generated code, the exported schema and that the
 * data survives. The first real tables (story 4) replace it, and their migration drops it.
 */
@Entity(tableName = "placeholder")
data class Placeholder(@PrimaryKey val id: Long, val text: String)

@Dao
interface PlaceholderDao {
    @Insert
    suspend fun insert(placeholder: Placeholder)

    @Query("SELECT * FROM placeholder WHERE id = :id")
    suspend fun get(id: Long): Placeholder?
}
