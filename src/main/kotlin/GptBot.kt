import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
import java.util.concurrent.TimeUnit

@Serializable
data class ResponseGptToken(
    @SerialName("access_token")
    val accessToken: String,
)

@Serializable
data class GigaChatRequest(
    val model: String,
    val messages: List<MessagePromptGpt>,
    val temperature: Int,
    val top: Double,
    val n: Int,
    val stream: Boolean,
    val max: Int,
    val repetitionPenalty: Double,
    val updateInterval: Int
)

@Serializable
data class MessagePromptGpt(
    val role: String,
    var content: String,
)

@Serializable
data class GigaChatResponse(
    @SerialName("choices")
    val choices: List<Choices>,
    @SerialName("created")
    val created: Long,
    @SerialName("model")
    val model: String,
    @SerialName("object")
    val objectModel: String,
    @SerialName("usage")
    val usage: Usage,
)

@Serializable
data class Choices(
    @SerialName("message")
    val message: MessageGptResponse,
    @SerialName("index")
    val index: Int,
    @SerialName("finish_reason")
    val finishReason: String,
)

@Serializable
data class MessageGptResponse(
    @SerialName("content")
    val content: String,
    @SerialName("role")
    val role: String,
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int,
    @SerialName("system_tokens")
    val systemTokens: Int? = null,
)

object GptBot {
    var clientSecretIdGpt: String = ""
    var clientSecretGpt: String = ""
    var tokenBotGpt: String = ""
    private val json = Json { ignoreUnknownKeys = true }
    private var lastTokenGenerationTime: Long = 0
    val gigaChatRequest: GigaChatRequest = GigaChatRequest(
        model = "GigaChat",
        messages = listOf(MessagePromptGpt(role = "user", content = "Привет")),
        temperature = 1,
        top = 0.1,
        n = 1,
        stream = false,
        max = 512,
        repetitionPenalty = 1.0,
        updateInterval = 0
    )

    fun getGigaChatModel(): String {
        val client = MyOkHttpClientFactory.createClient()
        val request = Request.Builder()
            .url("https://gigachat.devices.sberbank.ru/api/v1/models")
            .method("GET", null)
            .addHeader("Accept", "application/json")
            .addHeader("Authorization", "Bearer $tokenBotGpt")
            .build()
        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    fun getGigaChatResponse(): GigaChatResponse {
        val resultGpt = runCatching { sendGigaChatRequest() }.getOrNull() ?: ""
        println(resultGpt)
        return json.decodeFromString(resultGpt)
    }

    private fun sendGigaChatRequest(): String {
        try {
            val client = MyOkHttpClientFactory.createClient()
            val mediaType = "application/json".toMediaType()
            val gigaChatRequestString = json.encodeToString(gigaChatRequest)
            val body = gigaChatRequestString.toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://gigachat.devices.sberbank.ru/api/v1/chat/completions")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer $tokenBotGpt")
                .build()
            val response = client.newCall(request).execute()
            return response.body?.string() ?: ""
        } catch (e: Exception) {
            println(e)
            return ""
        }
    }

    //отправляем запрос на токен если нужен
    fun getTokenWhenNeeded(): String {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTokenGenerationTime > 28 * 60 * 1000) { // Проверяем прошло ли 28 минут
            tokenBotGpt = getTokenBotGpt().accessToken  // Генерируем новый токен
            lastTokenGenerationTime = currentTime  // Обновляем время последней генерации токена
        }
        lastTokenGenerationTime = currentTime // Обновляем время последней генерации токена
        return tokenBotGpt
    }

    //вынимаем сам токен из ответа
    private fun getTokenBotGpt(): ResponseGptToken {
        val resultGpt = runCatching { requestTokenBotGpt() }.getOrNull() ?: ""
        return json.decodeFromString(resultGpt)
    }

    //запрос на токен
    private fun requestTokenBotGpt(): String {
        val gptUrl = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        val client = MyOkHttpClientFactory.createClient()
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

    object MyOkHttpClientFactory {
        private val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        private val sslContext by lazy {
            SSLContext.getInstance("SSL").apply {
                init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            }
        }

        fun createClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(20, TimeUnit.SECONDS) // Установка таймаута соединения (примерно 20 секунд)
                .writeTimeout(20, TimeUnit.SECONDS) // Установка таймаута записи (примерно 20 секунд)
                .readTimeout(120, TimeUnit.SECONDS) // Установка таймаута чтения (примерно 120 секунд)
                .build()
        }
    }
}