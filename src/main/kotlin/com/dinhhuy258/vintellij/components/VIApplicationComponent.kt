package com.dinhhuy258.vintellij.components

import com.dinhhuy258.vintellij.connections.VIServer
import com.intellij.openapi.components.ApplicationComponent

class VIApplicationComponent : ApplicationComponent {
    companion object {
        private const val DEFAULT_PORT = 6969
    }

    private val server = VIServer(DEFAULT_PORT)

    override fun initComponent() {
        server.start()
    }
}
