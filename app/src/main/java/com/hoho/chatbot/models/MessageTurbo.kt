package com.hoho.chatbot.models

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class MessageTurbo(
    @SerializedName("content")
    var content: String,
    @SerializedName("role")
    var role: String,
)

fun MessageTurbo.toJson() : JsonObject {
    val json = JsonObject()
    json.addProperty("content", content)
    json.addProperty("role", role)

    return json
}
