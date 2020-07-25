command! -bang VintellijEnableAutoRefreshFile call vintellij#EnableAutoRefreshFile(<bang>0)
command! -bang VintellijEnableHealthCheckOnLoad call vintellij#EnableHealthCheckOnLoad(<bang>0)

if get(g:, 'vintellij_refresh_on_save', 1) == 1
  VintellijEnableAutoRefreshFile
endif

if get(g:, 'vintellij_health_check_on_load', 1) == 1
  VintellijEnableHealthCheckOnLoad
endif

command! VintellijGoToDefinition call vintellij#GoToDefinition()
command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! VintellijFindHierarchy call vintellij#FindHierarchy()
command! VintellijFindUsage call vintellij#FindUsage()

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>cgd :VintellijGoToDefinition<CR>
  nnoremap <Leader>cgh :VintellijFindHierarchy<CR>
  nnoremap <Leader>cgu :VintellijFindUsage<CR>
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

