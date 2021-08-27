package com.dinhhuy258.vintellij.utils

import com.intellij.openapi.application.ApplicationManager

class IdeaUtils {
    companion object {
        fun invokeOnMainAndWait(exceptionHandler: ((Throwable) -> Unit)? = null, runnable: () -> Unit) {
            var throwable: Throwable? = null
            ApplicationManager.getApplication().invokeAndWait {
                try {
                    runnable.invoke()
                } catch (t: Throwable) {
                    throwable = t
                }
            }
            val toThrow = throwable ?: return
            if (exceptionHandler == null) {
                throw toThrow
            } else {
                exceptionHandler.invoke(toThrow)
            }
        }
    }
}
