package com.dw.plugins

import com.dw.db.mapping.UserTable
import io.ktor.server.application.*
import io.ktor.server.config.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabases() {
    val config = environment.config
    configureDatabases(config)
}

fun configureDatabases(config: ApplicationConfig) {
    val dbUrl = config.property("ktor.psql-database.url").getString()
    val dbUser = config.property("ktor.psql-database.username").getString()
    val dbPassword = config.property("ktor.psql-database.password").getString()
    val dbDriver = config.propertyOrNull("ktor.psql-database.driver")?.getString() ?: "org.postgresql.Driver"

    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )

    // Ensure table is created
    transaction {
        SchemaUtils.create(UserTable)
    }
}
