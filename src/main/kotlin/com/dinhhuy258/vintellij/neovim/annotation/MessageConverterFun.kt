package com.dinhhuy258.vintellij.neovim.annotation

import com.dinhhuy258.vintellij.neovim.rpc.Message
import com.dinhhuy258.vintellij.neovim.rpc.Notification
import com.dinhhuy258.vintellij.neovim.rpc.Request

/**
 * When register message handlers, instead of taking [Notification]/[Request] as the input parameter, a specific class
 * can be used as the input parameter. Just add a companion method in the class and annotate it as [MessageConverterFun].
 * The neovim client will automatically convert the [Message] to the class instance when needed.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class MessageConverterFun
