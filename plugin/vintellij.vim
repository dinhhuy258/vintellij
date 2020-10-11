command! -bang VintellijEnableHealthCheckOnLoad call vintellij#EnableHealthCheckOnLoad(<bang>0)

if get(g:, 'vintellij_health_check_on_load', 0) == 1
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

autocmd FileType kotlin set omnifunc=vintellij#Autocomplete
autocmd FileType java set omnifunc=vintellij#Autocomplete

"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

let s:comrade_major_version = 0
let s:comrade_minor_version = 1
let s:comrade_patch_version = 1
let g:comrade_version = s:comrade_major_version .
      \ '.' . s:comrade_minor_version .
      \ '.' . s:comrade_patch_version
let g:comrade_enabled = get(g:, 'comrade_enabled', v:false)

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'
let s:coc_completion_path = s:path . '/../coc-vintellij'

function! s:VintellijComradeEnable() abort
  if exists('s:comrade_loaded')
    return
  endif
  let s:comrade_loaded = 1

  exe 'py3file' s:init_path
  call vintellij#events#Init()
  let &runtimepath .= ',' . s:coc_completion_path

  let s:comrade_loaded = v:true
  echomsg "Vintellij comrade: ON"
endfunction

command! -nargs=0 VintellijQuickFix call vintellij#fixer#FixAtCursor()
command! VintellijComradeEnable call <SID>VintellijComradeEnable()
