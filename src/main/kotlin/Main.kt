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
//создаем список пользователей которые вводят данные
    val waitingForInput = mutableMapOf<Long, UserInput>()
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
                waitingForInput,
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
    tokenGpt: String,
    waitingForInput: MutableMap<Long, UserInput>,
) {
    val message = updateTg.message?.text ?: ""
    val chatId = updateTg.message?.chat?.id ?: updateTg.callbackQuery?.message?.chat?.id ?: return
    val data = updateTg.callbackQuery?.data ?: ""
//проверяем есть ли юзер в базе
    val listOfUsers = mutableMapOf<String, String>()
    listOfUsers.putAll(airtable.loadListOfUsersId())
    var userIdAt = airtable.checkUserInBase(listOfUsers, chatId.toString())
    if (userIdAt == null) {
        val userIdFromAt = airtable.getIdForNewUser(mapOf("userID" to chatId.toString()))
        listOfUsers[chatId.toString()] = userIdFromAt.id
        airtable.saveListOfUsersId(listOfUsers)
        userIdAt = userIdFromAt.id
        waitingForInput[chatId] = UserInput(1, "")
    }
//обрабатываем команду или сообщение от пользователя
    when {
//при первом обращении сразу предлагает ввести основные данные
        waitingForInput[chatId]?.step == 1 -> {
            userInputData(json, botTokenTg, chatId, waitingForInput[chatId], message, airtable, userIdAt)
        }
//Стартовое меню
        message.lowercase() == MAIN_MENU || data == MAIN_MENU -> {
            sendMenu(json, botTokenTg, chatId)
        }

        data == "foodPreferencesSave" -> {
            waitingForInput[chatId]?.date?.let { date ->
                airtable.patchAirtable(userIdAt, mapOf("foodPreferences" to date))
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            sendMessage(json, botTokenTg, chatId, "Ваши данные записаны!")
            sendDataMenu(json, botTokenTg, chatId)
        }

        data == "foodExcludeSave" -> {
            waitingForInput[chatId]?.date?.let { date ->
                airtable.patchAirtable(userIdAt, mapOf("foodExclude" to date))
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            sendMessage(json, botTokenTg, chatId, "Ваши данные записаны!")
            sendDataMenu(json, botTokenTg, chatId)
        }

        data == "stopUserInput" -> {
            waitingForInput.remove(chatId)
            sendMessage(json, botTokenTg, chatId, "Отмена записи, успешно.")
            sendDataMenu(json, botTokenTg, chatId)
        }

//выслать меню на неделю пользователю
        data == MenuItem.ITEM_1.menuItem -> {
            sendMessage(json, botTokenTg, chatId, "Немного подождите")
            val folderId = "ajejmadk7ai886qpha8e"
            val promptFilePath = "src/main/kotlin/prompt.json"
            val gptBot = GptBot(tokenGpt, folderId, promptFilePath, json)
            val gptRequest = gptBot.getUpdateGpt().result.alternatives[0].message.text
//            val filePath = "src/main/kotlin/PdfFilesToUsers/1.pdf"
//            val filePdfToSendUser = FilePdf().createPdf(filePath,gptReq)
            sendMessage(json, botTokenTg, chatId, gptRequest)
//            sendDocument(json, botTokenTg, chatId, filePdfToSendUser, gptReq)
        }
//меню с данными пользователя и их редактированием
        data == MenuItem.ITEM_2.menuItem -> {
            sendDataMenu(json, botTokenTg, chatId)
        }
//просмотр данных пользователя
        data == MenuItem.ITEM_3.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val humanDataFull = userDataFullRecord.fields.humanData
            val humanData = humanDataFull.split("|")
            sendMessage(
                json, botTokenTg, chatId, "Текущие данные:\nПол: ${humanData[0]}\n" +
                        "Год рождения: ${humanData[1]}\n" +
                        "Рост: ${humanData[2]}\n" +
                        "Вес: ${humanData[3]}"
            )
        }
//изменить данные пользователя
        data == MenuItem.ITEM_4.menuItem -> {
            waitingForInput[chatId] = UserInput(2, "")
            sendMessage(json, botTokenTg, chatId, "Введите ваш пол (Мужской/Женский)")
        }
//просмотр предпочтений в еде
        data == MenuItem.ITEM_5.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val foodPreferencesFull = userDataFullRecord.fields.foodPreferences
            val foodPreferences = foodPreferencesFull.split("|")
            sendMessage(json, botTokenTg, chatId, "Предпочетаемые продукты:\n$foodPreferences")
        }
//изменить предпочтения в еде
        data == MenuItem.ITEM_6.menuItem -> {
            waitingForInput[chatId] = UserInput(6, "")
            sendMessage(
                json,
                botTokenTg,
                chatId,
                "Отправляйте по одному продукты, которые вы хотели бы чаще кушать"
            )
        }
//просмотр исключений в еде
        data == MenuItem.ITEM_7.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val foodExcludeFull = userDataFullRecord.fields.foodExclude
            val foodExclude = foodExcludeFull.split("|")
            sendMessage(json, botTokenTg, chatId, "Исключенные продукты:\n$foodExclude")
        }
//изменить исключения в еде
        data == MenuItem.ITEM_8.menuItem -> {
            waitingForInput[chatId] = UserInput(7, "")
            sendMessage(
                json,
                botTokenTg,
                chatId,
                "Отправляйте по одному продукты на исключение из вашего меню"
            )
        }
//ожидание ввода даных от пользователя
        waitingForInput.containsKey(chatId) -> {
            val step = userInputData(json, botTokenTg, chatId, waitingForInput[chatId], message, airtable, userIdAt)
            println(step)
            if (step == 0) waitingForInput.remove(chatId)
        }

        else -> {
            println("ололо")
        }
    }

//    if (waitingForInput.containsKey(chatId)) {
//        val step = userInputData(json, botTokenTg, chatId, waitingForInput[chatId], message, airtable, userIdAt)
//        println(step)
//        if (step == 0) waitingForInput.remove(chatId)}

}