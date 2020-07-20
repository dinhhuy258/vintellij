package com.dinhhuy258.vintellij.utils

class UrlUtils private constructor() {
    companion object {
        private const val VIM_PATH_PREFIX = "zipfile:"
        private const val INTELLIJ_PATH_PREFIX = "jar://"
        private const val INTELLIJ_JAR_SEPARATOR = ".jar!/"
        private const val VIM_JAR_SEPARATOR = ".jar::"
        private const val INTELLIJ_ZIP_SEPARATOR = ".zip!/"
        private const val VIM_ZIP_SEPARATOR = ".zip::"

        fun toVimJarFilePath(filePath: String): String {
            val path = if (filePath.contains(INTELLIJ_JAR_SEPARATOR)) {
                filePath.replaceFirst(INTELLIJ_JAR_SEPARATOR, VIM_JAR_SEPARATOR)
            }
            else {
                filePath.replaceFirst(INTELLIJ_ZIP_SEPARATOR, VIM_ZIP_SEPARATOR)
            }

            return "$VIM_PATH_PREFIX$path"
        }

        fun isVimJarFilePath(filePath: String): Boolean = filePath.startsWith(VIM_PATH_PREFIX)

        fun toIntellijJarFilePath(filePath: String): String {
            val path = filePath.replaceFirst(VIM_PATH_PREFIX, INTELLIJ_PATH_PREFIX)

            return if (path.contains(VIM_JAR_SEPARATOR)) {
                path.replaceFirst(VIM_JAR_SEPARATOR, INTELLIJ_JAR_SEPARATOR)
            }
            else {
                path.replaceFirst(VIM_ZIP_SEPARATOR, INTELLIJ_ZIP_SEPARATOR)
            }
        }

        fun isIntellijJarFile(filePath: String) = filePath.contains(INTELLIJ_JAR_SEPARATOR) || filePath.contains(INTELLIJ_ZIP_SEPARATOR)
    }
}
