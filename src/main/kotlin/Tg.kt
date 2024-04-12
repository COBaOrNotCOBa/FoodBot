import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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

fun getUpdates(tokenBot: String, updateId: Long): String {
    val urlGetUpdates = "https://api.telegram.org/bot$tokenBot/getUpdates?offset=$updateId"
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(urlGetUpdates)
        .build()
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
}

fun sendMessage(json: Json, tokenBot: String, chatId: Long, message: String): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
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

fun sendMenu(json: Json, tokenBot: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Доброго времени суток!",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard(MenuItem.ITEM_1.menuItem, MenuItem.ITEM_1.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_2.menuItem, MenuItem.ITEM_2.menuText),
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

fun sendGenerationMenu(json: Json, tokenBot: String, chatId: Long, text: String): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = text,
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard(MenuItem.ITEM_9.menuItem, MenuItem.ITEM_9.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_10.menuItem, MenuItem.ITEM_10.menuText),
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

fun sendChangingMenu(json: Json, tokenBot: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Нажмите на нужную кнопку",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard(MenuItem.ITEM_11.menuItem, MenuItem.ITEM_11.menuText),
                    InlineKeyboard(MenuItem.ITEM_12.menuItem, MenuItem.ITEM_12.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_13.menuItem, MenuItem.ITEM_13.menuText),
                    InlineKeyboard(MenuItem.ITEM_14.menuItem, MenuItem.ITEM_14.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_15.menuItem, MenuItem.ITEM_15.menuText),
                    InlineKeyboard(MenuItem.ITEM_16.menuItem, MenuItem.ITEM_16.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_17.menuItem, MenuItem.ITEM_17.menuText),
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

fun sendDataMenu(json: Json, tokenBot: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Просмотр/изменение",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard(MenuItem.ITEM_1.menuItem, MenuItem.ITEM_1.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_3.menuItem, MenuItem.ITEM_3.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_4.menuItem, MenuItem.ITEM_4.menuText),
                ),
//                listOf(
//                    InlineKeyboard(MenuItem.ITEM_5.menuItem, MenuItem.ITEM_5.menuText),
//                ),
//                listOf(
//                    InlineKeyboard(MenuItem.ITEM_6.menuItem, MenuItem.ITEM_6.menuText),
//                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_7.menuItem, MenuItem.ITEM_7.menuText),
                ),
                listOf(
                    InlineKeyboard(MenuItem.ITEM_8.menuItem, MenuItem.ITEM_8.menuText),
                ),
                listOf(
                    InlineKeyboard(MAIN_MENU, "В главное меню"),
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

fun sendFoodPreferencesMenu(json: Json, tokenBot: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Пришлите продукты который желаете добавить в предпочетаемые или нажмите на кнопку",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard("foodPreferencesSave", "Занести в базу ваши предпочтения"),
                ),
                listOf(
                    InlineKeyboard("stopUserInput", "Отмена записи"),
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

fun sendFoodExcludeMenu(json: Json, tokenBot: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$tokenBot/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Пришлите продукты для исключения из вашего меню или нажмите на кнопку",
        replyMarkup = ReplyMarkup(
            listOf(
                listOf(
                    InlineKeyboard("foodExcludeSave", "Занести в базу ваши исключения"),
                ),
                listOf(
                    InlineKeyboard("stopUserInput", "Отмена записи"),
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

fun botCommand(json: Json, tokenBot: String, command: List<BotCommand>) {
    val setMyCommandsRequest = SetMyCommandsRequest(command)
    val requestBody = json.encodeToString(setMyCommandsRequest)
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://api.telegram.org/bot$tokenBot/setMyCommands")
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

enum class MenuItem(val menuItem: String, val menuText: String) {
    ITEM_1("1", "Сгенерировать меню на неделю"),
    ITEM_2("2", "Просмотр/изменение своих данных"),
    ITEM_3("3", "Мои данные, просмотр"),
    ITEM_4("4", "Мои данные, изменение"),
    ITEM_5("5", "Предпочтения в еде, просмотр"),
    ITEM_6("6", "Предпочтения в еде, изменить"),
    ITEM_7("7", "Исключённые продукты, просмотр"),
    ITEM_8("8", "Исключённые продукты, изменить"),
    ITEM_9("9", "Список продуктов для покупки"),
    ITEM_10("10", "Изменить блюда"),
    ITEM_11("11", "Больше мяса"),
    ITEM_12("12", "Меньше мяса"),
    ITEM_13("13", "Больше рыбы"),
    ITEM_14("14", "Меньше рыбы"),
    ITEM_15("15", "Больше овощей"),
    ITEM_16("16", "Меньше овощей"),
    ITEM_17("17", "Сгенерировать новое меню на неделю"),
}