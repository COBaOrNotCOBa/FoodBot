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