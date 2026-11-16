package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import com.example.BuildConfig

// --- Mistral REST request/response structures for Moshi ---

@JsonClass(generateAdapter = true)
data class MistralRequest(
    val model: String = "pixtral-12b-2409",
    val messages: List<MistralMessage>,
    val response_format: MistralResponseFormat? = null,
    val temperature: Double = 0.1
)

@JsonClass(generateAdapter = true)
data class MistralMessage(
    val role: String,
    val content: List<MistralContentPart>
)

@JsonClass(generateAdapter = true)
data class MistralContentPart(
    val type: String,
    val text: String? = null,
    val image_url: MistralImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class MistralImageUrl(
    val url: String
)

@JsonClass(generateAdapter = true)
data class MistralResponseFormat(
    val type: String = "json_object"
)

@JsonClass(generateAdapter = true)
data class MistralResponse(
    val choices: List<MistralChoice>?
)

@JsonClass(generateAdapter = true)
data class MistralChoice(
    val message: MistralResponseMessage?
)

@JsonClass(generateAdapter = true)
data class MistralResponseMessage(
    val content: String?
)

@JsonClass(generateAdapter = true)
data class ExtractedInvoiceJson(
    val supplierName: String = "",
    val ico: String? = null,
    val dic: String? = null,
    val issueDate: String = "",
    val totalAmount: Double = 0.0,
    val vatAmount: Double? = null,
    val vatRate: Double? = null,
    val category: String = "Iné",
    val description: String? = null
)

interface MistralApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: MistralRequest
    ): MistralResponse
}

object MistralService {
    private const val BASE_URL = "https://api.mistral.ai/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val api: MistralApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MistralApiService::class.java)
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Compress bitmap to make the upload faster and fit under limits
        compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun extractInvoiceData(bitmap: Bitmap): ExtractedInvoiceJson? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.MISTRAL_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_MISTRAL_API_KEY") {
            // Placeholder key or missing key
            return@withContext null
        }

        val promptText = """
            Jste expert na slovenské účetnictví a analýzu faktur/účtenek (dokladů) pro slovenské živnostníky (SZČO).
            Analyzuj poskytnutý obrázek faktury nebo pokladničního bloku a extrahuj následující pole v češtině/slovenštině do formátu JSON:
            
            Štruktúra JSON:
            {
              "supplierName": "Meno/názov dodávateľa (např. Tesco, Orange, Lidl, atd.)",
              "ico": "Identifikačné číslo organizácie (IČO) dodávateľa - pouze číslice",
              "dic": "Daňové identifikačné číslo (DIČ) alebo IČ DPH dodávateľa",
              "issueDate": "Dátum vystavenia/zdaniteľného plnenia vo formáte YYYY-MM-DD",
              "totalAmount": Celková suma k úhradě včetně DPH jako desetinné číslo (Double)",
              "vatAmount": "Suma DPH v EUR jako desetinné číslo (Double) - pokud existuje",
              "vatRate": "Sadzba DPH v % jako desetinné číslo (Double), například 20.0, 10.0 atd.",
              "category": "Kategória výdavku. Zvoľ jednu z: 'Kancelária', 'Cestovné', 'Služby', 'Technika', 'Strava', 'Iné'",
              "description": "Veľmi stručný popis (napr. 'Nákup monitora', 'Mýtny poplatok', 'Obed s klientom')"
            }
            
            Ujisti se, že výstup bude výhradně validní JSON bez jakéhokoli doprovodného textu.
        """.trimIndent()

        val base64Image = bitmap.toBase64()
        val dataUrl = "data:image/jpeg;base64,$base64Image"

        val request = MistralRequest(
            model = "pixtral-12b-2409",
            messages = listOf(
                MistralMessage(
                    role = "user",
                    content = listOf(
                        MistralContentPart(type = "text", text = promptText),
                        MistralContentPart(type = "image_url", image_url = MistralImageUrl(url = dataUrl))
                    )
                )
            ),
            response_format = MistralResponseFormat(type = "json_object"),
            temperature = 0.1
        )

        try {
            val response = api.getChatCompletion("Bearer $apiKey", request)
            val jsonText = response.choices?.firstOrNull()?.message?.content
                ?: return@withContext null
            
            // Clean JSON string in case the model added markdown blocks
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(ExtractedInvoiceJson::class.java)
            adapter.fromJson(cleanedJson)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
