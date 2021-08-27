package com.dinhhuy258.vintellij

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler

enum class VintellijEventType(val value: Int) {
    CLOSE_CONNECTION(1),
    BUFFER_SAVED(2)
}

class VintellijEventNotification(
    @SerializedName("eventType")
    @Expose
    private val type: VintellijEventType,
) {
    override fun toString(): String {
        return MessageJsonHandler.toString(this)
    }
}