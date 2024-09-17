import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import comdatabases.UsersData
import comdatabases.UsersDataQueries

object UsersDatabase {

    private val driver: SqlDriver = JdbcSqliteDriver(
        "jdbc:sqlite:build/generated/sqldelight/code/Database/main/com/databases/UsersDatabase.db"
    )

    //    val db = Database.Schema.create(driver)
//    val database2 = Database(driver)
    private val database = UsersDataQueries(driver)

    fun listOfDb() = println(database.selectAll().executeAsList())

    fun addUser(userId: Long) = database.insertUserId(userId)

    fun searchUserTgId(chatId: Long): UsersData? =
        database.userTgId(searchQuery = chatId.toString()).executeAsOneOrNull()

    fun updateUserData(userData: String, chatId: Long) =
        database.update(userData, chatId)

    fun deleteUser(userTgId : String) =
        database.delete(userTgId.toLong())

}