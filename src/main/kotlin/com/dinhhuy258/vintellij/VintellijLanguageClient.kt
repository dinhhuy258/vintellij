package com.dinhhuy258.vintellij

import com.dinhhuy258.vintellij.notifications.VintellijNotification
import com.dinhhuy258.vintellij.notifications.VintellijSyncBuffer
import com.intellij.openapi.util.Key
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification
import org.eclipse.lsp4j.services.LanguageClient

val VINTELLIJ_CLIENT: Key<VintellijLanguageClient> = Key.create("vintellij_client");

interface VintellijLanguageClient : LanguageClient {
    /**
     * The event notification is sent from a server to a client to notify the
     * client certain events happened on the server side
     */
    @JsonNotification("vintellij/notification")
    fun sendNotification(notification: VintellijNotification)

    /**
     * The event is sent from a server to a client to notify the client about the change was made on the server side
     */
    @JsonNotification("vintellij/syncBuffer")
    fun syncBuffer(syncBuffer: VintellijSyncBuffer)
}