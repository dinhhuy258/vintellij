package com.dinhhuy258.vintellij

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class VintellijApplicationStarter : ApplicationStarter {
    private lateinit var projectPath: String

    override fun getCommandName(): String = "vintellij-inspect" /* This is a trick to force intellij run this application in headless mode (add inspect suffix to the command name) */

    override fun premain(args: MutableList<String>) {
        if (args.size == 1 /* First argument is the command name */) {
            println("Please provide the project path")
            exitProcess(1)
        }

        projectPath = args[1] // Ignore the first argument since it's command name
        val path = try {
            Paths.get(projectPath)
        } catch (e: Throwable) {
            println("Please provide valid project path")
            exitProcess(1)
        }

        if (!Files.isDirectory(path)) {
            println("Please provide valid project path")
            exitProcess(1)
        }
    }

    override fun main(args: MutableList<String>) {
        openProject(projectPath)
    }

    private fun openProject(projectPath: String) {
        println("Opening project...")
        println("Project path: $projectPath")

        ApplicationManagerEx.getApplicationEx().isSaveAllowed = false
        val project: Project? = ProjectUtil.openOrImport(projectPath, null, false)
        if (project == null) {
            println("Couldn't load project from path: $projectPath")
            exitProcess(1)
        }

        println("Project is loaded!!!")
    }
}
