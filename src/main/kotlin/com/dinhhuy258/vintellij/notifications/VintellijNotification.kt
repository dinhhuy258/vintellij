package com.dinhhuy258.vintellij.notifications

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler

enum class VintellijEventType(val value: Int) {
    CLOSE_CONNECTION(1),
    REQUEST_COMPLETION(2),
}

class VintellijNotification(
    @SerializedName("eventType")
    @Expose
    private val type: VintellijEventType,
) {
    override fun toString(): String {
        return MessageJsonHandler.toString(this)
    }
}