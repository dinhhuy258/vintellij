function! coc#source#vintellij#init() abort
  return {
        \ 'priority': 99,
        \ 'shortcut': 'IDEA',
        \ 'filtypes': ['java', 'kt'],
        \ 'triggerCharacters': ['.'],
        \}
endfunction

let s:autocomplete_candidates = v:null
let s:last_buffer_id = v:null
let s:last_changedtick = v:null

function! coc#source#vintellij#complete(opt, cb) abort
  if s:autocomplete_candidates is v:null
    let l:request = {
          \ "buf_id": a:opt["bufnr"],
          \ "buf_name": a:opt["filepath"],
          \ "buf_changedtick": a:opt["changedtick"],
          \ "row": a:opt["linenr"] - 1,
          \ "col": a:opt["colnr"] - 1,
          \ }

    let s:last_buffer_id = a:opt["bufnr"]
    let s:last_changedtick = a:opt["changedtick"]
    call vintellij#RequestCompletion(a:opt["bufnr"], l:request)
    call a:cb([])
    return
  endif
  call a:cb(s:autocomplete_candidates)
  let s:autocomplete_candidates = v:null
endfunction

function! coc#source#vintellij#OnAutocompleteCallback(result) abort
  if !empty(a:result.candidates)
    let s:autocomplete_candidates = a:result.candidates
    call coc#start()
  elseif bufnr("%") == s:last_buffer_id && s:last_changedtick != b:changedtick
    call coc#start()
  endif
endfunction
