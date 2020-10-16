package com.dinhhuy258.vintellij.neovim.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class RequestHandler(val name: String)