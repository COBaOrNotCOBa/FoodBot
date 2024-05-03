import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileWriter
import java.io.IOException

@Serializable
data class ResponseAt(
    @SerialName("records")
    val records: List<Records>
)

@Serializable
data class Records(
    @SerialName("id")
    val id: String,
    @SerialName("createdTime")
    val createdTime: String,
    @SerialName("fields")
    val fields: Fields
)

@Serializable
data class Fields(
    @SerialName("userID")
    val userID: String,
    @SerialName("humanData")
    val humanData: String = "",
    @SerialName("foodPreferences")
    val foodPreferences: String = "",
    @SerialName("foodExclude")
    val foodExclude: String = "",
    @SerialName("listOfDish")
    val listOfDish: String = "",
    @SerialName("listOfIngredients")
    val listOfIngredients: String = "",
)

class Airtable(
    private val tokenBotAt: String,
    private val airBaseId: String,
    private val tableId: String,
    private val json: Json,
    var userId: String = "",
) {

    fun getUpdateAt(): ResponseAt {
        val resultAt = runCatching { getAirtable() }.getOrNull() ?: ""
        println(resultAt)
        return json.decodeFromString(resultAt)
    }

    private fun getAirtable(): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.airtable.com/v0/$airBaseId/$tableId")
            .get()
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error getting records from Airtable: ${e.message}")
            ""
        }
    }

    fun getUserRecord(): Records {
        val resultRecord = runCatching { getUserData() }.getOrNull() ?: ""
        return json.decodeFromString(resultRecord)
    }


    private fun getUserData(): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.airtable.com/v0/$airBaseId/$tableId/$userId")
            .get()
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error deleting record from Airtable: ${e.message}")
            ""
        }
    }

    fun getIdForNewUser(fields: Map<String, String>): Records {
        val resultRecord = runCatching { postAirtable(fields) }.getOrNull() ?: ""
        println(resultRecord)
        return json.decodeFromString(resultRecord)
    }

    private fun postAirtable(fields: Map<String, String>): String {
        val client = OkHttpClient()
        val fieldsJson = fields.entries.joinToString(separator = ",") {
            "\"${it.key}\":\"${it.value}\""
        }
        val postData = "{\"fields\": {$fieldsJson}}"
        val requestBody = postData.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://api.airtable.com/v0/$airBaseId/$tableId")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error creating new record in Airtable: ${e.message}")
            ""
        }
    }

    fun putAirtable(fields: Map<String, String>): String {
        val client = OkHttpClient()
        val fieldsJson = fields.entries.joinToString(separator = ",") {
            "\"${it.key}\":\"${it.value}\""
        }
        val postData = "{\"fields\": {$fieldsJson}}"
        val requestBody = postData.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("https://api.airtable.com/v0/$airBaseId/$tableId/$userId")
            .put(requestBody)
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error updating record in Airtable: ${e.message}")
            ""
        }
    }


    fun patchAirtable(field: String, text: String): String {
        val client = OkHttpClient()
        val url = "https://api.airtable.com/v0/$airBaseId/$tableId/$userId"
        val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val textWithEscapedNewlines = text.replace("\n", "\\n")
        println(textWithEscapedNewlines)
        val requestBody = "{\"fields\": {\"$field\":\"$textWithEscapedNewlines\"}}".toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error updating record in Airtable: ${e.message}")
            ""
        }
    }

    fun deleteAirtable(): String {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.airtable.com/v0/$airBaseId/$tableId/$userId")
            .delete()
            .addHeader("Authorization", "Bearer $tokenBotAt")
            .build()
        return try {
            val response = client.newCall(request).execute()
            response.body?.string() ?: ""
        } catch (e: IOException) {
            println("Error deleting record from Airtable: ${e.message}")
            ""
        }
    }

    //проверяем есть ли текущий пользователь в списке
    fun checkUserInBase(loadListOfUsersId: Map<String, String>, userIdTg: String): String? {
        return loadListOfUsersId[userIdTg]
    }

    //загружаем существующий список пользователей
    fun loadListOfUsersId(): Map<String, String> {
        try {
            val wordsFile: File = File("src/main/kotlin/Users.txt")
            val listOfUsers: MutableMap<String, String> = mutableMapOf()
            wordsFile.readLines().forEach {
                val line = it.split("|")
                listOfUsers[line[0]] = line[1]
            }
            return listOfUsers
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректный файл")
        }
    }

    //заносим нового пользователя в список
    fun saveListOfUsersId(listOfUsers: Map<String, String>) {
        try {
            val wordsFile = File("src/main/kotlin/Users.txt")
            val writer = FileWriter(wordsFile)
            listOfUsers.forEach { (key, value) ->
                writer.write("$key|$value\n")
            }
            writer.close()
        } catch (e: IOException) {
            throw IllegalStateException("Не удалось записать в файл")
        }
    }
}