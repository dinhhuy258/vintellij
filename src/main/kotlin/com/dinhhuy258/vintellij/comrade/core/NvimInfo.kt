package com.dinhhuy258.vintellij.comrade.core

import com.dinhhuy258.vintellij.comrade.isIPV4String
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.io.exists
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CopyOnWriteArrayList

private const val CONFIG_DIR_NAME = ".vintellij"
private var HOME = System.getProperty("user.home")
private val CONFIG_DIR = File(HOME, CONFIG_DIR_NAME)

/**
 * Information about the running neovim instances on the system.
 * @param pid nvim instance pid.
 * @param address nvim listen address.
 * @param projectName the current project name.
 * @param startDir the starting director where nvim starts at.
 */
class NvimInfo(
    val pid: Int,
    val address: String,
    val projectName: String,
    val startDir: String
) {
    override fun toString(): String {
        return "Nvim Listen Address: $address, Start Directory: $startDir, Project Name: $projectName"
    }
}

/**
 * Monitor the system to collect all the running Neovim instance information.
 */
internal object NvimInfoCollector {
    private val watchPath = Paths.get(CONFIG_DIR.canonicalPath)
    private val log = Logger.getInstance(NvimInfoCollector::class.java)
    private val backingAll = CopyOnWriteArrayList<NvimInfo>()

    /**
     * All running nvim instances's information.
     */
    val all: List<NvimInfo>
        get() {
            cleanNonExisting()
            return backingAll
        }

    fun getNvimInfo(startDir: String): NvimInfo? {
        watchPath.toFile().walk().forEach { file ->
            val nvimInfo = parseInfoFile(file)
            if (nvimInfo?.startDir.equals(startDir)) {
                return nvimInfo
            }
        }

        return null
    }

    private fun parseInfoFile(file: File): NvimInfo? {
        if (!file.isFile) return null

        val lines = file.readLines()
        if (lines.isEmpty() || lines.size < 3) return null

        val pid = try {
            file.nameWithoutExtension.toInt()
        } catch (e: NumberFormatException) {
            return null
        }
        val address = lines.first()
        if (!checkAddress(address)) return null

        val projectName = lines[1]
        val startDir = lines[2]

        val existingNvimInfo = all.firstOrNull { it.pid == pid }
        if (existingNvimInfo != null) {
            log.warn("NvimInfo with pid '$pid' has been discovered before.")
            return existingNvimInfo
        }

        val info = NvimInfo(pid, address, projectName, startDir)
        backingAll.add(info)
        return info
    }

    private fun cleanNonExisting() {
        backingAll.removeIf {
            !checkAddress(it.address)
        }
    }

    private fun checkAddress(address: String): Boolean {
        if (!isIPV4String(address)) {
            val file = File(address)
            return file.exists()
        }
        return true
    }

    fun stop() {
    }
}
