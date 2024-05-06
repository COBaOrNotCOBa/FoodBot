//проверяем есть ли юзер в базе
//fun getUserRecordIdFromAt(tg: Tg, airtable: Airtable, waitingForInput: MutableMap<Long, UserInput>): String {
fun getUserRecordIdFromAt(chatId: Long, airtable: Airtable): String {
    val listOfUsers = airtable.loadListOfUsersId()
    airtable.userId = airtable.checkUserInBase(listOfUsers, chatId.toString()).takeIf { it?.isNotEmpty() ?: false }
        ?: run {
            val userIdFromAt = airtable.getIdForNewUser(mapOf("userID" to chatId.toString()))
            airtable.saveListOfUsersId(listOfUsers + (chatId.toString() to userIdFromAt.id))
//            waitingForInput[tg.chatId] = UserInput(1, "")
            userIdFromAt.id
        }
    return airtable.userId
}