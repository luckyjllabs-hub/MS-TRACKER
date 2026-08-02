package com.example.mstrackerapp.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create merchants table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `merchants` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `defaultCategoryId` TEXT NOT NULL,
                    `icon` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            // Create merchant_mappings table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `merchant_mappings` (
                    `id` TEXT NOT NULL,
                    `rawSmsPattern` TEXT NOT NULL,
                    `merchantId` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            // Create budgets table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budgets` (
                    `id` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `limitMinor` INTEGER NOT NULL,
                    `monthYear` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            // Create settings table
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `settings` (
                    `key` TEXT NOT NULL,
                    `value` TEXT NOT NULL,
                    PRIMARY KEY(`key`)
                )
                """.trimIndent()
            )
        }
    }
}
