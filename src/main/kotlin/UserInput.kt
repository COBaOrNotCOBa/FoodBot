class UserInput(var step: Int, var data: String)

fun userInputData(
    chatId: Long,
    message: String,
    tg: Tg,
    userInput: UserInput?,
    airtable: Airtable,
): Int {
    userInput ?: return 0
    when (userInput.step) {
        1 -> {
            userInput.step = 2
            tg.sendMessage(
                chatId,
                "Для более точных рекомендаций блюд, ответьте пожалуйста на несколько вопросов.\n" +
                        "Введите ваш пол (Мужской/Женский)"
            )
        }

        2 -> {
            userInput.data = message
            userInput.step = 3
            tg.sendMessage(chatId, "Введите ваш год рождения:")
        }

        3 -> {
            userInput.data = userInput.data + "|" + message
            userInput.step = 4
            tg.sendMessage(chatId, "Введите ваш рост:")
        }

        4 -> {
            userInput.data = userInput.data + "|" + message
            userInput.step = 5
            tg.sendMessage(chatId, "Введите ваш вес:")
        }

        5 -> {
            userInput.data = userInput.data + "|" + message
            userInput.step = 0
            airtable.patchAirtable("humanData", userInput.data)
            tg.sendMessage(chatId, "Ваши данные записаны!")
            tg.sendMenu(chatId)
        }

        6 -> {
            if (userInput.data == "") {
                userInput.data = message
            } else {
                userInput.data = userInput.data + "|" + message
            }
            tg.sendFoodPreferencesMenu(chatId)
        }

        7 -> {
            if (userInput.data == "") {
                userInput.data = message
            } else {
                userInput.data = userInput.data + "|" + message
            }
            tg.sendFoodExcludeMenu(chatId)
        }

    }
    return userInput.step
}