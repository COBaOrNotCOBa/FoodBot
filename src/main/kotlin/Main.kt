import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

val coroutineContext: CoroutineContext = Executors.newFixedThreadPool(4).asCoroutineDispatcher()
val coroutineScope = CoroutineScope(coroutineContext)

fun main(args: Array<String>) {
//исходные данные
    val tokenBotAt = args[1]
    val baseIdAt = args[2]
    val tableIdAt = args[3]
    val clientSecretIdGpt = args[4]
    val clientSecretGpt = args[5]

    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
//создаем класс Tg для телеграмм
    val tg = Tg(args[0], json)
//Создаем экземпляр для GPT бота
    val gptBot = GptBot(json, clientSecretIdGpt, clientSecretGpt)
//создаем класс таблицы для базы данных АТ
    val airtable = Airtable(tokenBotAt, baseIdAt, tableIdAt, json)
//создаем список пользователей которые вводят данные последовательно
    val waitingForInput = mutableMapOf<Long, UserInput>()
//будем хранить тут список блюд пользователя
    val savedUserMenuData = mutableMapOf<Long, String>()
//меню команд в телеграмме
    tg.botCommand(listOf(BotCommand("start", "Глвное меню")))

    coroutineScope.launch {
//обновления каждые 2.5 секунд, проверка были ли запросы из телеграмма
        while (true) {
            try {
                delay(2500)
                val getUpdatesDeferred = async(Dispatchers.IO) {
                    runCatching { tg.getUpdates(lastUpdateId) }
                }
                val resultTg = getUpdatesDeferred.await()
                val responseStringTg = resultTg.getOrNull() ?: continue
                if (responseStringTg != "{\"ok\":true,\"result\":[]}") println(responseStringTg)
//если ошибка ждём дополнительно 5 секунд, на последний запрос нет ответа TODO
                if (responseStringTg.contains("error_code")) {
                    delay(5000)
                    continue
                }
//получаем список апдейтов и проверяем не пустые ли они. После по очереди обрабатываем
                val responseTg: ResponseTg = json.decodeFromString(responseStringTg)
                if (responseTg.result.isEmpty()) continue
//проверяем токен gptbota
                gptBot.tokenBotGpt = gptBot.getTokenWhenNeeded()
//сортируем входящие запросы
                val sortedUpdates = responseTg.result.sortedBy { it.updateId }
                lastUpdateId = sortedUpdates.last().updateId + 1
                launch {
                    try {
                        sortedUpdates.forEach {
                            handleUpdate(
                                it,
                                tg,
                                airtable,
                                gptBot,
                                waitingForInput,
                                savedUserMenuData,
                            )
                        }
                    } catch (e: Exception) {
                        println("Ошибка $e")
                    }
                }
            } catch (e: Exception) {
                println("Ошибка $e")
            }
        }
    }
}

//разбиваем апдейт на куски
fun handleUpdate(
    updateTg: Update,
    tg: Tg,
    airtable: Airtable,
    gptBot: GptBot,
    waitingForInput: MutableMap<Long, UserInput>,
    savedUserMenuData: MutableMap<Long, String>,
) {
    val message = updateTg.message?.text ?: ""
    val chatId = updateTg.message?.chat?.id ?: updateTg.callbackQuery?.message?.chat?.id ?: return
    val data = updateTg.callbackQuery?.data ?: ""
    println("2 $chatId")
//проверяем есть ли юзер в базе
    getUserRecordIdFromAt(chatId, airtable)
//обрабатываем команду или сообщение от пользователя
    when {
//при первом обращении сразу предлагает ввести основные данные
        waitingForInput[chatId]?.step == 1 -> {
            userInputData(chatId, message, tg, waitingForInput[chatId], airtable)
        }
//тест
        (message.startsWith("тест")) -> {
            val sendMessageFromUser = message.substringAfter("тест")
            val textForUser =
                sendMessageFromUser.let { it -> sendMessageFromUser.substring(it.indexOfFirst { it.isLetter() }) }
            gptBot.gigaChatRequest.messages[0].content = textForUser
            val listOfFood = gptBot.getGigaChatResponse().choices[0].message.content
            tg.sendMessage(chatId, listOfFood)
        }
//тест
        message.lowercase() == "т" -> {

        }
//Стартовое меню
        message.lowercase() == MenuItem.ITEM_0.menuItem || data == MenuItem.ITEM_0.menuItem -> {
            tg.sendMenu(chatId)
        }

        data == "foodPreferencesSave" -> {
            waitingForInput[chatId]?.data?.let { date ->
                airtable.patchAirtable("foodPreferences", date)
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            tg.sendMessage(chatId, "Ваши данные записаны!")
            tg.sendDataMenu(chatId)
        }

        data == "foodExcludeSave" -> {
            waitingForInput[chatId]?.data?.let { date ->
                airtable.patchAirtable("foodExclude", date)
            }
            waitingForInput[chatId]?.step = 0
            waitingForInput.remove(chatId)
            tg.sendMessage(chatId, "Ваши данные записаны!")
            tg.sendDataMenu(chatId)
        }

        data == "stopUserInput" -> {
            waitingForInput.remove(chatId)
            tg.sendMessage(chatId, "Отмена записи, успешно.")
            tg.sendDataMenu(chatId)
        }

//выслать меню на неделю пользователю
        data == MenuItem.ITEM_1.menuItem -> {
            tg.sendMessage(chatId, "Немного подождите, подбираем меню")

            val userDataFullRecord = airtable.getUserRecord()

            val foodPreferences = if (userDataFullRecord.fields.foodPreferences != "") {
                "Продукты которые я предпочитаю: " +
                        userDataFullRecord.fields.foodPreferences.replace("|", ", ")
            } else {
                "Любымых блюд нет."
            }

            val foodExclude = if (userDataFullRecord.fields.foodExclude != "") {
                "Не должно быть блюд с этими продуктами: " +
                        userDataFullRecord.fields.foodExclude.replace("|", ", ")
            } else {
                "Исключений нет"
            }

            val humanDataFull = userDataFullRecord.fields.humanData
            if (humanDataFull != "") {
                val humanData = humanDataFull.split("|")
                gptBot.gigaChatRequest.messages[0].content =
                    "Предложи мне список блюд на неделю. Учитывай мои данные и исключения: " +
                            "пол ${humanData[0]}, " +
                            "год рождения ${humanData[1]}, " +
                            "рост ${humanData[2]}, " +
                            "вес ${humanData[3]}. " +
                            "$foodPreferences. " +
                            "$foodExclude."
                val gptBotResponse = gptBot.getGigaChatResponse().choices[0].message.content
//                gptBotResponse = gptBotResponse.substringAfter("Понедельник")
//                gptBotResponse = "Понедельник$gptBotResponse"
//                gptBotResponse = gptBotResponse.replace("\n\n", "\n")

                airtable.patchAirtable("listOfDish", gptBotResponse)
                tg.sendGenerationMenu(chatId, gptBotResponse)
                savedUserMenuData[chatId] = gptBotResponse
            } else {
                gptBot.gigaChatRequest.messages[0].content =
                    "Предложи мне список блюд на неделю. Учитывай мои данные и исключения: " +
                            "$foodPreferences. " +
                            "$foodExclude."
                var gptBotResponse = gptBot.getGigaChatResponse().choices[0].message.content
                gptBotResponse = gptBotResponse.substringAfter("Понедельник")
                gptBotResponse = "Понедельник$gptBotResponse"

                airtable.patchAirtable("listOfDish", gptBotResponse)
                tg.sendGenerationMenu(chatId, gptBotResponse)
                savedUserMenuData[chatId] = gptBotResponse
            }

        }
//выслать новое меню на неделю пользователю
        data == MenuItem.ITEM_17.menuItem -> {
            tg.sendMessage(chatId, "Немного подождите, подбираем меню")
            gptBot.gigaChatRequest.messages[0].content =
                "Вот список блюд на неделю: ${savedUserMenuData[chatId]}. " +
                        "Выполни в точности все уточнения и пришли новый список"

            val gptBotResponse = gptBot.getGigaChatResponse().choices[0].message.content

            airtable.patchAirtable("listOfDish", gptBotResponse)
            tg.sendGenerationMenu(chatId, gptBotResponse)
            savedUserMenuData[chatId] = gptBotResponse
        }
//Список продуктов для покупки сгенерированный ботом
        data == MenuItem.ITEM_9.menuItem -> {
            tg.sendMessage(chatId, "Немного подождите, составляем список...")

            val listOfDish = airtable.getUserRecord().fields.listOfDish
            val content = listOfDish.replace("\n", " ").trim()
            gptBot.gigaChatRequest.messages[0].content = "Вот мой список блюд: $content. " +
                    "Составь общий список продуктов для разовой покупки в магазине для моих блюд. " +
                    "Обязательно с весом всех ингредиентов. Не дублируй ингредиенты"

            val listOfFood = gptBot.getGigaChatResponse().choices[0].message.content
            airtable.patchAirtable("listOfIngredients", listOfFood)

            tg.sendMessage(chatId, listOfFood)
        }
//Меню для изменения блюд
        data == MenuItem.ITEM_10.menuItem -> {
            tg.sendChangingMenu(chatId)
        }
//Больше мяса
        data == MenuItem.ITEM_11.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать больше мясных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было больше мясных блюд."
        }
//Меньше мяса
        data == MenuItem.ITEM_12.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать меньше мясных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было меньше мясных блюд."
        }
//Больше рыбы
        data == MenuItem.ITEM_13.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать больше рыбных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было больше рыбных блюд."
        }
//Меньше рыбы
        data == MenuItem.ITEM_14.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать меньше рыбных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было меньше рыбных блюд."
        }
//Больше овощей
        data == MenuItem.ITEM_15.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать больше овощных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было больше овощьных блюд."
        }
//Меньше овощей
        data == MenuItem.ITEM_16.menuItem -> {
            tg.sendMessage(chatId, "Теперь будем предлагать меньше овощных блюд")
            tg.sendChangingMenu(chatId)
            savedUserMenuData[chatId] =
                savedUserMenuData[chatId] + " Измени этот список чтобы было меньше овощьных блюд."
        }
//Меню с данными пользователя и их редактированием
        data == MenuItem.ITEM_2.menuItem -> {
            tg.sendDataMenu(chatId)
        }
//просмотр данных пользователя
        data == MenuItem.ITEM_3.menuItem -> {
            val humanDataFull = airtable.getUserRecord().fields.humanData
            val humanData = humanDataFull.split("|")
            tg.sendMessage(
                chatId,
                "Текущие данные:\nПол: ${humanData[0]}\n" +
                        "Год рождения: ${humanData[1]}\n" +
                        "Рост: ${humanData[2]}\n" +
                        "Вес: ${humanData[3]}"
            )
        }
//изменить данные пользователя
        data == MenuItem.ITEM_4.menuItem -> {
            waitingForInput[chatId] = UserInput(2, "")
            tg.sendMessage(chatId, "Введите ваш пол (Мужской/Женский)")
        }
//просмотр предпочтений в еде
        data == MenuItem.ITEM_5.menuItem -> {
            val foodPreferencesFull = airtable.getUserRecord().fields.foodPreferences
            val foodPreferences = foodPreferencesFull.split("|")
            tg.sendMessage(chatId, "Предпочитаемые продукты:\n$foodPreferences")
        }
//изменить предпочтения в еде
        data == MenuItem.ITEM_6.menuItem -> {
            waitingForInput[chatId] = UserInput(6, "")
            tg.sendMessage(chatId, "Отправляйте по одному продукту, которые вы хотели бы чаще есть")
        }
//просмотр исключений в еде
        data == MenuItem.ITEM_7.menuItem -> {
            val foodExcludeFull = airtable.getUserRecord().fields.foodExclude
            val foodExclude = foodExcludeFull.split("|")
            tg.sendMessage(chatId, "Исключенные продукты:\n$foodExclude")
        }
//изменить исключения в еде
        data == MenuItem.ITEM_8.menuItem -> {
            waitingForInput[chatId] = UserInput(7, "")
            tg.sendMessage(chatId, "Отправляйте по одному продукты на исключение из вашего меню")
        }
//Выводим последний сохраненый спислк на неделю из базы
        data == MenuItem.ITEM_18.menuItem -> {
            tg.sendMessage(chatId, airtable.getUserRecord().fields.listOfDish)
        }
//выводим последний список продуктов для покупки из базы
        data == MenuItem.ITEM_19.menuItem -> {
            tg.sendMessage(chatId, airtable.getUserRecord().fields.listOfIngredients)
        }
//ожидание ввода даных от пользователя
        waitingForInput.containsKey(chatId) -> {
            val step = userInputData(chatId, message, tg, waitingForInput[chatId], airtable)
            if (step == 0) waitingForInput.remove(chatId)
        }

        else -> {
            println("ололо")
        }
    }
}