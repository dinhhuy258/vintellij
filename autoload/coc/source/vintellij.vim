function! coc#source#vintellij#init() abort
  return {
        \ 'priority': 99,
        \ 'shortcut': 'IDEA',
        \ 'filtypes': ['java', 'kt'],
        \ 'triggerCharacters': ['.'],
        \}
endfunction

function! coc#source#vintellij#complete(opt, cb) abort
  let l:request = {
          \ "buf_id": a:opt["bufnr"],
          \ "buf_name": a:opt["filepath"],
          \ "buf_changedtick": a:opt["changedtick"],
          \ "row": a:opt["linenr"] - 1,
          \ "col": a:opt["colnr"],
          \ }

  call vintellij#RequestCompletion(a:opt["bufnr"], l:request)
  let s:autocomplete_callback = a:cb
endfunction

function! coc#source#vintellij#OnAutocompleteCallback(result) abort
  call s:autocomplete_callback(a:result.candidates)
endfunction
