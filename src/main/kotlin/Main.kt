import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
//исходные данные
    val tokenBotTg = args[0]
    val tokenBotAt = args[1]
    val baseIdAt = args[2]
    val tableIdAt = args[3]
    val clientSecretIdGpt = args[4]
    val clientSecretGpt = args[5]

    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
//Создаем экземпляр Gpt
    val gptBot = GptBot(json, clientSecretIdGpt, clientSecretGpt)
//создаем класс таблицы из АТ
    val airtable = Airtable(tokenBotAt, baseIdAt, tableIdAt, json)
//создаем список пользователей которые вводят данные
    val waitingForInput = mutableMapOf<Long, UserInput>()
//будем хранить тут список блюд пользователя
    val savedUserMenuData = mutableMapOf<Long, String>()
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
        gptBot.tokenBotGpt = gptBot.getTokenWhenNeeded()
        val sortedUpdates = responseTg.result.sortedBy { it.updateId }
        lastUpdateId = sortedUpdates.last().updateId + 1
        sortedUpdates.forEach {
            handleUpdate(
                it,
                json,
                tokenBotTg,
                airtable,
                gptBot,
                waitingForInput,
                savedUserMenuData,
            )
        }
    }
}

//разбиваем апдейт на куски
fun handleUpdate(
    updateTg: Update,
    json: Json,
    tokenBotTg: String,
    airtable: Airtable,
    gptBot: GptBot,
    waitingForInput: MutableMap<Long, UserInput>,
    savedUserMenuData: MutableMap<Long, String>,
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
            userInputData(json, tokenBotTg, chatId, waitingForInput[chatId], message, airtable, userIdAt)
        }
//тест
        (message.startsWith("тест")) -> {
            val sendMessageFromUser = message.substringAfter("тест")
            val textForUser =
                sendMessageFromUser.let { it -> sendMessageFromUser.substring(it.indexOfFirst { it.isLetter() }) }
            gptBot.gigaChatRequest.messages[0].content = textForUser
            val listOfFood = gptBot.getGigaChatResponse().choices[0].message.content
            sendMessage(json, tokenBotTg, chatId, listOfFood)
        }

//Стартовое меню
        message.lowercase() == MAIN_MENU || data == MAIN_MENU -> {
            sendMenu(json, tokenBotTg, chatId)
        }
//тест
        message.lowercase() == "т" -> {
            println(gptBot.getGigaChatModel())
        }

        data == "foodPreferencesSave" -> {
            waitingForInput[chatId]?.date?.let { date ->
                airtable.patchAirtable(userIdAt, mapOf("foodPreferences" to date))
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            sendMessage(json, tokenBotTg, chatId, "Ваши данные записаны!")
            sendDataMenu(json, tokenBotTg, chatId)
        }

        data == "foodExcludeSave" -> {
            waitingForInput[chatId]?.date?.let { date ->
                airtable.patchAirtable(userIdAt, mapOf("foodExclude" to date))
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            sendMessage(json, tokenBotTg, chatId, "Ваши данные записаны!")
            sendDataMenu(json, tokenBotTg, chatId)
        }

        data == "stopUserInput" -> {
            waitingForInput.remove(chatId)
            sendMessage(json, tokenBotTg, chatId, "Отмена записи, успешно.")
            sendDataMenu(json, tokenBotTg, chatId)
        }

//выслать меню на неделю пользователю
        data == MenuItem.ITEM_1.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Немного подождите, подбираем меню")
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val humanDataFull = userDataFullRecord.fields.humanData
            val humanData = humanDataFull.split("|")

            val foodPreferences = if (userDataFullRecord.fields.foodPreferences != "") {
                "Продукты которые я предпочитаю: ${userDataFullRecord.fields.foodPreferences.replace("|", ",")}"
            } else {
                ""
            }
            val foodExclude = if (userDataFullRecord.fields.foodExclude != "") {
                "Не должно быть блюд с этими продуктами: ${userDataFullRecord.fields.foodExclude.replace("|", ",")}"
            } else {
                "Исключений нет"
            }
            gptBot.gigaChatRequest.messages[0].content =
                "Предложи мне список блюд на неделю. Учитывай мои данные и исключения: " +
                        "пол ${humanData[0]}, " +
                        "год рождения ${humanData[1]}, " +
                        "рост ${humanData[2]}, " +
                        "вес ${humanData[3]}. " +
                        "$foodPreferences. " +
                        "$foodExclude."
            savedUserMenuData[chatId] = gptBot.getGigaChatResponse().choices[0].message.content
            savedUserMenuData[chatId]?.let { sendGenerationMenu(json, tokenBotTg, chatId, it) }
        }
//выслать новое меню на неделю пользователю
        data == MenuItem.ITEM_17.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Немного подождите, подбираем меню")
            gptBot.gigaChatRequest.messages[0].content =
                "Вот список блюд на неделю: ${savedUserMenuData[chatId]}. " +
                        "Выполни в точности все уточнения и пришли новый список"
            savedUserMenuData[chatId] = gptBot.getGigaChatResponse().choices[0].message.content
            savedUserMenuData[chatId]?.let { sendGenerationMenu(json, tokenBotTg, chatId, it) }
        }
//Список продуктов для покупки сгенерированный ботом
        data == MenuItem.ITEM_9.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Немного подождите, составляем список...")
            val content = savedUserMenuData[chatId].toString().replace("\n", " ").trim()
            gptBot.gigaChatRequest.messages[0].content =
                "Вот список блюд: $content. " +
                        "Пришли мне общий список для покупки ингредиентов с указанием их веса. " +
                        "Выбери сам продукты для этих блюд."
            val listOfFood = gptBot.getGigaChatResponse().choices[0].message.content
            sendMessage(json, tokenBotTg, chatId, listOfFood)
        }
//Меню для изменения блюд
        data == MenuItem.ITEM_10.menuItem -> {
            sendChangingMenu(json, tokenBotTg, chatId)
        }
//Больше мяса
        data == MenuItem.ITEM_11.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать больше мясных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] = savedUserMenuData[chatId] + " Измени этот список чтобы было больше мясных блюд."
        }
//Меньше мяса
        data == MenuItem.ITEM_12.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать меньше мясных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] = savedUserMenuData[chatId] + " Измени этот список чтобы было меньше мясных блюд."
        }
//Больше рыбы
        data == MenuItem.ITEM_13.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать больше рыбных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] = savedUserMenuData[chatId] + " Измени этот список чтобы было больше рыбных блюд."
        }
//Меньше рыбы
        data == MenuItem.ITEM_14.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать меньше рыбных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] = savedUserMenuData[chatId] + " Измени этот список чтобы было меньше рыбных блюд."
        }
//Больше овощей
        data == MenuItem.ITEM_15.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать больше овощных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было больше овощьных блюд."
        }
//Меньше овощей
        data == MenuItem.ITEM_16.menuItem -> {
            sendMessage(json, tokenBotTg, chatId, "Теперь будем предлагать меньше овощных блюд")
            sendChangingMenu(json, tokenBotTg, chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было меньше овощьных блюд."
        }
//Меню с данными пользователя и их редактированием
        data == MenuItem.ITEM_2.menuItem -> {
            sendDataMenu(json, tokenBotTg, chatId)
        }
//просмотр данных пользователя
        data == MenuItem.ITEM_3.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val humanDataFull = userDataFullRecord.fields.humanData
            val humanData = humanDataFull.split("|")
            sendMessage(
                json, tokenBotTg, chatId, "Текущие данные:\nПол: ${humanData[0]}\n" +
                        "Год рождения: ${humanData[1]}\n" +
                        "Рост: ${humanData[2]}\n" +
                        "Вес: ${humanData[3]}"
            )
        }
//изменить данные пользователя
        data == MenuItem.ITEM_4.menuItem -> {
            waitingForInput[chatId] = UserInput(2, "")
            sendMessage(json, tokenBotTg, chatId, "Введите ваш пол (Мужской/Женский)")
        }
//просмотр предпочтений в еде
        data == MenuItem.ITEM_5.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val foodPreferencesFull = userDataFullRecord.fields.foodPreferences
            val foodPreferences = foodPreferencesFull.split("|")
            sendMessage(json, tokenBotTg, chatId, "Предпочетаемые продукты:\n$foodPreferences")
        }
//изменить предпочтения в еде
        data == MenuItem.ITEM_6.menuItem -> {
            waitingForInput[chatId] = UserInput(6, "")
            sendMessage(
                json,
                tokenBotTg,
                chatId,
                "Отправляйте по одному продукты, которые вы хотели бы чаще кушать"
            )
        }
//просмотр исключений в еде
        data == MenuItem.ITEM_7.menuItem -> {
            val userDataFullRecord = airtable.getUpdateRecord(userIdAt)
            val foodExcludeFull = userDataFullRecord.fields.foodExclude
            val foodExclude = foodExcludeFull.split("|")
            sendMessage(json, tokenBotTg, chatId, "Исключенные продукты:\n$foodExclude")
        }
//изменить исключения в еде
        data == MenuItem.ITEM_8.menuItem -> {
            waitingForInput[chatId] = UserInput(7, "")
            sendMessage(
                json,
                tokenBotTg,
                chatId,
                "Отправляйте по одному продукты на исключение из вашего меню"
            )
        }
//ожидание ввода даных от пользователя
        waitingForInput.containsKey(chatId) -> {
            val step = userInputData(json, tokenBotTg, chatId, waitingForInput[chatId], message, airtable, userIdAt)
            println(step)
            if (step == 0) waitingForInput.remove(chatId)
        }

        else -> {
            println("ололо")
        }
    }
}