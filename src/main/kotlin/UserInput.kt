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
            userInput.date = message
            userInput.step = 2
            sendMessage(json, botTokenTg, chatId, "Введите ваш год рождения:")
        }
        2 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 3
            sendMessage(json, botTokenTg, chatId, "Введите ваш рост:")
        }
        3 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 4
            sendMessage(json, botTokenTg, chatId, "Введите ваш вес:")
        }
        4 -> {
            userInput.date = userInput.date + "|" + message
            userInput.step = 0
            airtable.patchAirtable(userIdAt, mapOf("humanData" to userInput.date))
            sendMessage(json, botTokenTg, chatId, "Ваши данные записаны")
        }
    }
    return userInput.step
}