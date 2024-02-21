import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@Serializable
data class ResponseTg(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long?,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyboard>>,
)

@Serializable
data class InlineKeyboard(
    @SerialName("callback_data")
    val callbackData: String,
    @SerialName("text")
    val text: String,
)

@Serializable
data class SetMyCommandsRequest(
    @SerialName("commands")
    val commands: List<BotCommand>
)

@Serializable
data class BotCommand(
    @SerialName("command")
    val command: String,
    @SerialName("description")
    val description: String
)

fun getUpdates(botToken: String, updateId: Long): String {
    val urlGetUpdates = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateId"
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(urlGetUpdates)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendMessage(json: Json, botToken: String, chatId: Long, message: String): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = message,
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client = OkHttpClient()
    val requestBodyJson = requestBodyString.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(sendMessage)
        .header("Content-type", "application/json")
        .post(requestBodyJson)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Доброго времени суток!",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard( MenuItem.ITEM_1.menuItem,  MenuItem.ITEM_1.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_2.menuItem,  MenuItem.ITEM_2.menuText),
                ),
            )
        )
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client = OkHttpClient()
    val requestBodyJson = requestBodyString.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(sendMessage)
        .header("Content-type", "application/json")
        .post(requestBodyJson)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendDataMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Нажмите на нужную кнопку",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard( MenuItem.ITEM_3.menuItem,  MenuItem.ITEM_3.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_4.menuItem,  MenuItem.ITEM_4.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_5.menuItem,  MenuItem.ITEM_5.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_6.menuItem,  MenuItem.ITEM_6.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_7.menuItem,  MenuItem.ITEM_7.menuText),
                ),
                listOf(
                    InlineKeyboard( MenuItem.ITEM_8.menuItem,  MenuItem.ITEM_8.menuText),
                ),
                listOf(
                    InlineKeyboard( MAIN_MENU,  "В главное меню"),
                ),
            )
        )
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client = OkHttpClient()
    val requestBodyJson = requestBodyString.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(sendMessage)
        .header("Content-type", "application/json")
        .post(requestBodyJson)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendDocument(json: Json, botToken: String, chatId: Long, pdfFile: File, caption: String? = null): String {
    val sendDocumentUrl = "https://api.telegram.org/bot$botToken/sendDocument"

    val requestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("chat_id", chatId.toString())
        .addFormDataPart("document", pdfFile.name, pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull()))
    caption?.let {
        requestBody.addFormDataPart("caption", it)
    }
    val request = Request.Builder()
        .url(sendDocumentUrl)
        .post(requestBody.build())
        .build()

    val client = OkHttpClient()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendFoodPreferencesMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Пришлите продукт который желаете добавить в предпочетаемые",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard( "foodPreferencesSave",  "Занести в базу ваши предпочтения"),
                ),
                listOf(
                    InlineKeyboard( "stopUserInput",  "Отмена записи"),
                ),
            )
        )
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client = OkHttpClient()
    val requestBodyJson = requestBodyString.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(sendMessage)
        .header("Content-type", "application/json")
        .post(requestBodyJson)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendFoodExcludeMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Пришлите продукт для исключения из вашего меню",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard( "foodExcludeSave",  "Занести в базу ваши исключения"),
                ),
                listOf(
                    InlineKeyboard( "stopUserInput",  "Отмена записи"),
                ),
            )
        )
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client = OkHttpClient()
    val requestBodyJson = requestBodyString.toRequestBody("application/json".toMediaType())
    val request = Request.Builder()
        .url(sendMessage)
        .header("Content-type", "application/json")
        .post(requestBodyJson)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun botCommand(json: Json, botTokenTg: String, command: List<BotCommand>) {
    val setMyCommandsRequest = SetMyCommandsRequest(command)
    val requestBody = json.encodeToString(setMyCommandsRequest)
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.telegram.org/bot$botTokenTg/setMyCommands")
        .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
        .build()
    val response = client.newCall(request).execute()
    val responseBody = response.body?.string()
    println(responseBody)
    responseBody?.let {
        ""
    }
    response.close()
}

const val MAIN_MENU = "/start"
enum class MenuItem(val menuItem : String, val menuText : String) {
    ITEM_1 ("1","Рекомендации блюд для меня!"),
    ITEM_2("2","Просмотр/изменение своих данных"),
    ITEM_3("3","Просмотр моих данных (пол, возраст, рост, вес)"),
    ITEM_4("4","Изменение моих данных (пол, возраст, рост, вес)"),
    ITEM_5("5","Просмотр предпочтений в еде"),
    ITEM_6("6","Изменить предпочетаемые продукты"),
    ITEM_7("7","Просмотр исключённых продуктов"),
    ITEM_8("8","Изменить исключаемые продукты"),
}