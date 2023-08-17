package com.hoho.chatbot.api

import com.hoho.chatbot.constants.textCompletionsTurboEndpoint
import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface OpenAIApi {
    @POST(textCompletionsTurboEndpoint)
    @Streaming
    fun textCompletionsTurboWithStream(
        @Header("Content-Type") contentType:String,
        @Header("Authorization") authorization:String,
        @Body body: JsonObject
    ): Call<ResponseBody>
}