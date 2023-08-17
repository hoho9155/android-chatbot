package com.hoho.chatbot.remote

import android.util.Log
import com.hoho.chatbot.constants.baseUrlOpenAI
import com.hoho.chatbot.constants.openAIApiKey
import com.hoho.chatbot.api.OpenAIApi
import com.hoho.chatbot.models.TextCompletionsParam
import com.hoho.chatbot.models.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okio.IOException
import org.json.JSONException
import org.json.JSONObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder


@Suppress("UNREACHABLE_CODE")
class OpenAIRepositoryImpl:OpenAIRepository {
    private var openAIApi: OpenAIApi = Retrofit.Builder()
        .baseUrl(baseUrlOpenAI)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIApi::class.java)

    override fun textCompletionsWithStream(params: TextCompletionsParam): Flow<String> =
        callbackFlow {
            withContext(Dispatchers.IO) {
                val response = (openAIApi.textCompletionsTurboWithStream(
                    "application/json",
                    "Bearer ${openAIApiKey}",
                    params.toJson()
                )).execute()

                if (response.isSuccessful) {
                    val input = response.body()?.byteStream()?.bufferedReader() ?: throw Exception()
                    try {
                        while (true) {
                            val line = withContext(Dispatchers.IO) {
                                input.readLine()
                            } ?: continue
                            if (line == "data: [DONE]") {
                                close()
                            } else if (line.startsWith("data:")) {
                                try {
                                    // Handle & convert data -> emit to client
                                    val value = lookupDataFromResponseTurbo(line)

                                    if (value.isNotEmpty()) {
                                        trySend(value)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Log.e("ChatGPT Lite BUG", e.toString())
                                }
                            }
                        }
                    } catch (e: IOException) {
                        Log.e("ChatGPT Lite BUG", e.toString())
                        throw Exception(e)
                    } finally {
                        withContext(Dispatchers.IO) {
                            input.close()
                        }

                        close()
                    }
                } else {
                    if (!response.isSuccessful) {
                        var jsonObject: JSONObject? = null
                        try {
                            jsonObject = JSONObject(response.errorBody()!!.string())
                            println(jsonObject)
                            trySend("Failure! Try again. $jsonObject")
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }
                    trySend("Failure! Try again")
                    close()
                }
            }

            close()
        }

    private fun lookupDataFromResponseTurbo(jsonString: String): String {
        val regex = """"content"\s*:\s*"([^"]+)"""".toRegex()
        val matchResult = regex.find(jsonString)

        if (matchResult != null && matchResult.groupValues.size > 1) {
            return matchResult.groupValues[1]
        }

        return " "
    }
}