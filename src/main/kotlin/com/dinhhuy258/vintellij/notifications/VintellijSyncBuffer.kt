package com.dinhhuy258.vintellij.notifications

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler

class VintellijSyncBuffer(
    @SerializedName("path")
    @Expose
    private val path: String,
    @SerializedName("startLine")
    @Expose
    private val startLine: Int,
    @SerializedName("endLine")
    @Expose
    private val endLine: Int,
    @SerializedName("lines")
    @Expose
    private val lines: List<String>,
) {
    override fun toString(): String {
        return MessageJsonHandler.toString(this)
    }
}