class UserInput(var step: Int, var data: String)

fun userInputData(
    tg: Tg,
    userInput: UserInput?,
    airtable: Airtable,
): Int {
    userInput ?: return 0
    when (userInput.step) {
        1 -> {
            userInput.step = 2
            tg.sendMessage(
                "Для более точных рекомендаций блюд, ответьте пожалуйста на несколько вопросов.\n" +
                        "Введите ваш пол (Мужской/Женский)"
            )
        }

        2 -> {
            userInput.data = tg.message
            userInput.step = 3
            tg.sendMessage("Введите ваш год рождения:")
        }

        3 -> {
            userInput.data = userInput.data + "|" + tg.message
            userInput.step = 4
            tg.sendMessage("Введите ваш рост:")
        }

        4 -> {
            userInput.data = userInput.data + "|" + tg.message
            userInput.step = 5
            tg.sendMessage("Введите ваш вес:")
        }

        5 -> {
            userInput.data = userInput.data + "|" + tg.message
            userInput.step = 0
            airtable.patchAirtable("humanData", userInput.data)
            tg.sendMessage("Ваши данные записаны!")
            tg.sendMenu()
        }

        6 -> {
            if (userInput.data == "") {
                userInput.data = tg.message
            } else {
                userInput.data = userInput.data + "|" + tg.message
            }
            tg.sendFoodPreferencesMenu()
        }

        7 -> {
            if (userInput.data == "") {
                userInput.data = tg.message
            } else {
                userInput.data = userInput.data + "|" + tg.message
            }
            tg.sendFoodExcludeMenu()
        }

    }
    return userInput.step
}