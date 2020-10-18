package com.dinhhuy258.vintellij.neovim.rpc

import com.dinhhuy258.vintellij.neovim.NeovimConnection
import com.intellij.openapi.diagnostic.Logger

class Sender(private val connection: NeovimConnection) {

    private val log = Logger.getInstance(Sender::class.java)

    fun send(msg: Message) {
        MsgPackMapper.writeValue(connection.outputStream, msg)
        log.debug("Connection['$connection'] Sent message: $msg")
    }
}
