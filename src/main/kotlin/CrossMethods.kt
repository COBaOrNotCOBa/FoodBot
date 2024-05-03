//проверяем есть ли юзер в базе
//fun getUserRecordIdFromAt(tg: Tg, airtable: Airtable, waitingForInput: MutableMap<Long, UserInput>): String {
fun getUserRecordIdFromAt(tg: Tg, airtable: Airtable): String {
    val listOfUsers = airtable.loadListOfUsersId()
    airtable.userId = airtable.checkUserInBase(listOfUsers, tg.chatId.toString()).takeIf { it?.isNotEmpty() ?: false }
        ?: run {
            val userIdFromAt = airtable.getIdForNewUser(mapOf("userID" to tg.chatId.toString()))
            airtable.saveListOfUsersId(listOfUsers + (tg.chatId.toString() to userIdFromAt.id))
//            waitingForInput[tg.chatId] = UserInput(1, "")
            userIdFromAt.id
        }
    return airtable.userId
}