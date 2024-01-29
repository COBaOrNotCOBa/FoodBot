import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
//исходные данные
    val tokenBotTg = args[0]
    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
//меню команд в телеграмме
    botCommand(
        json, tokenBotTg, listOf(BotCommand("start", "Глвное меню"))
    )
//обновления каждые 2.5 секунд, проверка были ли запросы из телеграмма
    while (true) {
        Thread.sleep(2500)
        val resultTg = runCatching { getUpdates(tokenBotTg, lastUpdateId) }
        val responseStringTg = resultTg.getOrNull() ?: continue
        if (responseStringTg!="{\"ok\":true,\"result\":[]}") println(responseStringTg)

        if (responseStringTg.contains("error_code")) {
            Thread.sleep(5000)
            continue
        }

        val responseTg: ResponseTg = json.decodeFromString(responseStringTg)
        if (responseTg.result.isEmpty()) continue
        val sortedUpdates = responseTg.result.sortedBy { it.updateId }
        lastUpdateId = sortedUpdates.last().updateId + 1
        sortedUpdates.forEach {
            handleUpdate(
                it,
                json,
                tokenBotTg,
            )
        }
    }
}

fun handleUpdate(
    updateTg: Update,
    json: Json,
    botTokenTg: String,
) {
    val message = updateTg.message?.text ?: ""
    val chatId = updateTg.message?.chat?.id ?: updateTg.callbackQuery?.message?.chat?.id ?: return
    val data = updateTg.callbackQuery?.data ?: ""

    when {
//Стартовое меню
        message.lowercase() == MAIN_MENU || data == MAIN_MENU -> {
            sendMenu(json, botTokenTg, chatId)
        }

        data == "1" -> {
            sendMessage(json, botTokenTg, chatId, "1")
        }
        data == "2" -> {
            sendMessage(json, botTokenTg, chatId, "2")
        }
        data == "3" -> {
            sendMessage(json, botTokenTg, chatId, "3")
        }
    }
}