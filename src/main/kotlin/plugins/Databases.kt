package com.dw.plugins

import io.ktor.server.application.*
import io.ktor.server.config.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.Logger

fun Application.configureDatabases() {
    val config = environment.config
    configureDatabases(config, log)
}

fun configureDatabases(config: ApplicationConfig, logger: Logger? = null) {
    var dbUrl = config.property("ktor.psql-database.url").getString()
    val dbUser = config.property("ktor.psql-database.username").getString()
    val dbPassword = config.property("ktor.psql-database.password").getString()
    val dbDriver = config.propertyOrNull("ktor.psql-database.driver")?.getString() ?: "org.postgresql.Driver"

    // Automatic H2 compatibility for tests
    if (dbUrl.startsWith("jdbc:h2:") && !dbUrl.contains("MODE=")) {
        dbUrl += ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
    }

    logger?.info("Configuring Flyway migrations for $dbUrl")

    try {
        val flywayConfig = Flyway.configure()
            .dataSource(dbUrl, dbUser, dbPassword)
            .driver(dbDriver)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
        
        // Allow cleaning in-memory databases for tests
        if (dbUrl.startsWith("jdbc:h2:mem:")) {
            flywayConfig.cleanDisabled(false)
        }

        val flyway = flywayConfig.load()
        
        if (dbUrl.startsWith("jdbc:h2:mem:")) {
            flyway.clean()
        }

        val result = flyway.migrate()
        if (result.success) {
            logger?.info("Flyway migration successful. Applied ${result.migrationsExecuted} migrations.")
        } else {
            logger?.warn("Flyway migration completed but success flag is false.")
        }
    } catch (e: Exception) {
        logger?.error("Flyway migration failed: ${e.message}", e)
        throw e
    }

    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        user = dbUser,
        password = dbPassword
    )
}
