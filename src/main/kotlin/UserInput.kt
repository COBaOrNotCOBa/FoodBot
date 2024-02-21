import kotlinx.serialization.json.Json

class UserInput(var step: Int, var date: String)

fun userInputData(
    json: Json,
    botTokenTg: String,
    chatId: Long,
    userInput: UserInput?,
    message: String,
    airtable: Airtable,
    userIdAt: String,
): Int {
    userInput ?: return 0
    when (userInput.step) {
        1 -> {
            userInput.step = 2
            sendMessage(
                json,
                botTokenTg,
                chatId,
                "Для более точных рекомендаций блюд, ответьте пожалуйста на несколько вопросов.\n" +
                        "Введите ваш пол (Мужской/Женский)"
            )
        }

        2 -> {
            userInput.date = message
            userInput.step = 3
            sendMessage(json, botTokenTg, chatId, "Введите ваш год рождения:")
        }

        3 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 4
            sendMessage(json, botTokenTg, chatId, "Введите ваш рост:")
        }

        4 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 5
            sendMessage(json, botTokenTg, chatId, "Введите ваш вес:")
        }

        5 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 0
            airtable.patchAirtable(userIdAt, mapOf("humanData" to userInput.date))
            sendMessage(json, botTokenTg, chatId, "Ваши данные записаны!")
            sendDataMenu(json,botTokenTg,chatId)
        }

        6 -> {
            if (userInput.date == "") {
                userInput.date = message
            } else {
                userInput.date = userInput.date + "|" + message
            }
            sendFoodPreferencesMenu(json,botTokenTg,chatId)
        }

        7 -> {
            if (userInput.date == "") {
                userInput.date = message
            } else {
                userInput.date = userInput.date + "|" + message
            }
            sendFoodExcludeMenu(json,botTokenTg,chatId)
        }

    }
    return userInput.step
}