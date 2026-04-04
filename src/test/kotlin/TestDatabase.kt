import com.dw.db.mapping.UserTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * TestDatabase is a singleton object that manages the H2 in-memory database
 * for all tests. By using a singleton with an 'init' block, we ensure that:
 * 1. The database connection is established only once.
 * 2. The schema is created only once.
 * 
 * This follows the user's request for "executed once" and is a performance-oriented
 * approach for database tests.
 */
object TestDatabase {
    init {
        // Initialize the connection only once for the entire JVM lifetime
        Database.connect("jdbc:h2:mem:test_user_dao;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    }

    fun init() {
        transaction {
            SchemaUtils.create(UserTable)
        }
    }

    fun tearDown() {
        transaction {
            SchemaUtils.drop(UserTable)
        }
    }
}
