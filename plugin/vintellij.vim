command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()


if !exists('vintellij_loaded')
  let vintellij_loaded = 1
  let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
  let s:init_path = s:path . '/vintellij_init.py'

  exe 'py3file' s:init_path

  call vintellij#events#Init()
endif

function! s:VintellijEnable() abort
  call coc#config('languageserver.vintellij.enable', v:true)
  execute 'silent! edit'

  echomsg "Vintellij is enabled"
endfunction

command! VintellijEnable call <SID>VintellijEnable()

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>cgd :VintellijGoToDefinition<CR>
  nnoremap <Leader>cgh :VintellijFindHierarchy<CR>
  nnoremap <Leader>cgu :VintellijFindUsage<CR>
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

