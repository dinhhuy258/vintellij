if get(g:, 'vintellij_refresh_on_save', 0)
  augroup vintellij_on_kt_java_file_save
    autocmd!
    autocmd BufWritePost,FileReadPost *.kt,*.java call vintellij#RefreshFile()
  augroup END
endif

if get(g:, 'vintellij_health_check_on_load', 0)
  augroup vintellij_on_kt_java_file_load
    autocmd!
    autocmd BufReadPost,FileReadPost *.kt,*.java call vintellij#HealthCheck()
  augroup END
endif

