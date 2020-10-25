package com.dinhhuy258.vintellij.comrade.core

import com.dinhhuy258.vintellij.comrade.ComradeNeovimService
import com.dinhhuy258.vintellij.comrade.ComradeScope
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

object NvimInstanceManager : Disposable {

    private val instanceMap = ConcurrentHashMap<NvimInfo, NvimInstance>()
    private val log = Logger.getInstance(NvimInfoCollector::class.java)

    /**
     * Stop monitoring nvim instances and close all the nvim connections.
     */
    private fun stop() {
        NvimInfoCollector.stop()
        instanceMap.clear()
    }

    /**
     * Try to load all nvim instances' current buffers if it is contained by the opened JetBrains' projects.
     */
    fun refresh() {
        val instances = instanceMap.values
        ComradeScope.launch {
            instances.forEach {
                if (it.connected) it.bufManager.loadCurrentBuffer()
            }
        }
    }

    fun cleanUp(project: Project) {
        instanceMap.forEach {
            it.value.bufManager.cleanUp(project)
        }
    }

    /**
     * Lists all the running nvim instances and their connection status.
     *
     * @return List of running [NvimInfo] and its connection status.
     */
    fun list(): List<Pair<NvimInfo, Boolean>> {
        val instances = instanceMap.toMap()
        return NvimInfoCollector.all.map { Pair(it, instances.containsKey(it)) }
    }

    /**
     * Connect to the given nvim.
     */
    fun connect(nvimInfo: NvimInfo): NvimInstance? {
        if (instanceMap.containsKey(nvimInfo)) return null
        val address = nvimInfo.address
        try {
            val instance = NvimInstance(address) {
                onStop(nvimInfo)
            }
            Disposer.register(this, instance)
            instanceMap[nvimInfo] = instance
            val exceptionHandler = CoroutineExceptionHandler { _, exception ->
                ComradeNeovimService.showBalloon("Failed to connect to nvim '$address': $exception",
                        NotificationType.ERROR)
                val toDispose = instanceMap.remove(nvimInfo) ?: return@CoroutineExceptionHandler
                Disposer.dispose(toDispose)
            }
            ComradeScope.launch(exceptionHandler) {
                instance.connect()
                instance.bufManager.loadCurrentBuffer()
                ComradeNeovimService.instance.showBalloon("Connected to Neovim instance $address",
                        NotificationType.INFORMATION)
            }
            log.info("Try to connect to Neovim instance '$nvimInfo'.")

            return instance
        } catch (t: Throwable) {
            log.warn("Failed to create Neovim instance for $nvimInfo", t)
            val toDispose = instanceMap.remove(nvimInfo) ?: return null
            Disposer.dispose(toDispose)
        }
        return null
    }

    /**
     * Disconnect from the given nvim.
     */
    fun disconnect(nvimInfo: NvimInfo) {
        log.debug("disconnect: Nvim '${nvimInfo.address}'")
        val toDispose = instanceMap.remove(nvimInfo) ?: return
        Disposer.dispose(toDispose)
    }

    private fun onStop(nvimInfo: NvimInfo) {
        log.info("onStop: Nvim '${nvimInfo.address}' has been disconnected.")
        val toDispose = instanceMap.remove(nvimInfo) ?: return
        Disposer.dispose(toDispose)
    }

    override fun dispose() {
        stop()
    }
}
