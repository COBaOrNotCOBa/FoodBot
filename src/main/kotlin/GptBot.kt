import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

@Serializable
data class ResponseGptToken(
    @SerialName("access_token")
    val accessToken: String,
)

class GptBot(
    private val json: Json,
    private val clientSecretIdGpt: String,
    private val clientSecretGpt: String,
) {
    fun getTokenBotGpt(): ResponseGptToken {
        val resultGpt = runCatching { requestTokenBotGpt() }.getOrNull() ?: ""
//        println(resultGpt)
        return json.decodeFromString(resultGpt)
    }

    private fun requestTokenBotGpt(): String {
        val gptUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
// Создание TrustManager'а для доверенного сертификата
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

// Настройка OkHttpClient с TrustManager'ом
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true } // Для пропуска проверки hostname
            .build()

        val requestBody = "scope=GIGACHAT_API_PERS".toRequestBody("application/x-www-form-urlencoded".toMediaType())

        val request = Request.Builder()
            .url(gptUrl)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .header("RqUID", clientSecretIdGpt)
            .header("Authorization", "Basic $clientSecretGpt")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }
}

//@Serializable
//data class ResponseGpt(
//    @SerialName("result")
//    val result: Alternatives,
//)
//
//@Serializable
//data class Alternatives(
//    @SerialName("alternatives")
//    val alternatives: List<MessageGpt>,
//)
//
//@Serializable
//data class MessageGpt(
//    @SerialName("message")
//    val message: TextGpt,
//)
//
//@Serializable
//data class TextGpt(
//    @SerialName("text")
//    val text: String,
//)
//
//@Serializable
//data class RequestGpt(
//    @SerialName("modelUri")
//    val modelUri: String = "gpt://b1gidtrrq0kiv3kf31u2/yandexgpt-lite",
//    @SerialName("completionOptions")
//    val completionOptions: CompletionOptions,
//    @SerialName("messages")
//    val messages: List<Role>,
//)
//
//@Serializable
//data class CompletionOptions(
//    @SerialName("stream")
//    val stream: Boolean = false,
//    @SerialName("temperature")
//    val temperature: Double = 0.6,
//    @SerialName("maxTokens")
//    val maxTokens: String = "2000",
//)
//
//@Serializable
//data class Role(
//    @SerialName("role")
//    val role: String,
//    @SerialName("text")
//    val text: String,
//)
//
//class GptBot(
//    private val json: Json,
//    private val botToken: String,
//    private val requestGpt: String,
//) {
//
//    private val folderId: String = "ajejmadk7ai886qpha8e"
////    private val promptJson = json.encodeToString(requestGpt)
//
//    fun getUpdateGpt(): ResponseGpt {
//        val resultGpt = runCatching { sendGptRequest() }.getOrNull() ?: ""
//        println(resultGpt)
//        return json.decodeFromString(resultGpt)
//    }
//
//    private fun sendGptRequest(): String {
//        val gptUrl = "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"
//        val client = OkHttpClient()
//        val requestBody = requestGpt.toRequestBody("application/json".toMediaType())
//        val request = Request.Builder()
//            .url(gptUrl)
//            .header("Content-Type", "application/json")
//            .header("Authorization", "Api-Key $botToken")
//            .header("x-folder-id", folderId)
//            .post(requestBody)
//            .build()
//        val response = client.newCall(request).execute()
//        return response.body?.string() ?: ""
//    }
//}