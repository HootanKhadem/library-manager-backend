package com.dw.plugins

import com.dw.db.mapping.UserTable
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configureDatabases() {
    val dbUrl = environment.config.property("ktor.psql-database.url").getString()
    val dbUser = environment.config.property("ktor.psql-database.username").getString()
    val dbPassword = environment.config.property("ktor.psql-database.password").getString()

    Database.connect(
        url = dbUrl,
        driver = "org.postgresql.Driver",
        user = dbUser,
        password = dbPassword
    )

    // Ensure table is created
    transaction {
        SchemaUtils.create(UserTable)
    }
}
