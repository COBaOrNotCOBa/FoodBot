import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
//исходные данные
    val tokenBotTg = args[0]
    val tokenBotAt = args[1]
    val baseIdAt = args[2]
    val tableIdAt = args[3]
    val tokenGPT = args[4]
    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
//создаем класс таблицы из АТ
    val airtable = Airtable(tokenBotAt, baseIdAt, tableIdAt, json)
//меню команд в телеграмме
    botCommand(
        json, tokenBotTg, listOf(BotCommand("start", "Глвное меню"))
    )
//обновления каждые 2.5 секунд, проверка были ли запросы из телеграмма
    while (true) {
        Thread.sleep(2500)
        val resultTg = runCatching { getUpdates(tokenBotTg, lastUpdateId) }
        val responseStringTg = resultTg.getOrNull() ?: continue
        if (responseStringTg != "{\"ok\":true,\"result\":[]}") println(responseStringTg)
//если ошибка ждём дополнительно 5 секунд, на последний запрос нет ответа
        if (responseStringTg.contains("error_code")) {
            Thread.sleep(5000)
            continue
        }
//получаем список апдейтов и проверяем не пустые ли они. После поочереди обрабатываем
        val responseTg: ResponseTg = json.decodeFromString(responseStringTg)
        if (responseTg.result.isEmpty()) continue
        val sortedUpdates = responseTg.result.sortedBy { it.updateId }
        lastUpdateId = sortedUpdates.last().updateId + 1
        sortedUpdates.forEach {
            handleUpdate(
                it,
                json,
                tokenBotTg,
                airtable,
                tokenGPT,
            )
        }
    }
}

//разбиваем апдейт на куски
fun handleUpdate(
    updateTg: Update,
    json: Json,
    botTokenTg: String,
    airtable: Airtable,
    tokenGPT: String,
) {
    val message = updateTg.message?.text ?: ""
    val chatId = updateTg.message?.chat?.id ?: updateTg.callbackQuery?.message?.chat?.id ?: return
    val data = updateTg.callbackQuery?.data ?: ""
//обрабатываем команду или сообщение от пользователя
    when {
//Стартовое меню
        message.lowercase() == MAIN_MENU || data == MAIN_MENU -> {
            sendMenu(json, botTokenTg, chatId)
        }

        data == "1" -> {
            sendMessage(json, botTokenTg, chatId, "1")
        }

        data == "2" -> {
            val userData = airtable.getUpdateAt().records
            val humanData = userData[0].fields.humanData
            val foodPreferance = userData[0].fields.foodPreferance
            val excludeFood = userData[0].fields.excludeFood
            sendMessage(json, botTokenTg, chatId, "$humanData\n$foodPreferance\n$excludeFood")
        }

        data == "3" -> {
            val folderId = "ajejmadk7ai886qpha8e"
            val promptFilePath = "src/main/kotlin/prompt.json"
            val gptBot = GptBot(tokenGPT, folderId, promptFilePath, json)
            val gptReq = gptBot.getUpdateGpt().result.alternatives[0].message.text
//            val filePath = "src/main/kotlin/PdfFilesToUsers/1.pdf"
//            val filePdfToSendUser = FilePdf().createPdf(filePath,gptReq)
            sendMessage(json, botTokenTg, chatId, gptReq)
//            sendDocument(json, botTokenTg, chatId, filePdfToSendUser, gptReq)
        }
    }
}