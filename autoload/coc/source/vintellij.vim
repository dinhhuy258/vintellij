function! coc#source#vintellij#init() abort
  return {
        \ 'priority': 99,
        \ 'shortcut': 'IDEA',
        \ 'filetypes': ['java', 'kotlin'],
        \ 'triggerCharacters': ['.'],
        \}
endfunction

let s:autocomplete_candidates = v:null

function! coc#source#vintellij#complete(opt, cb) abort
  if s:autocomplete_candidates is v:null
    let l:request = {
          \ "buf_id": a:opt["bufnr"],
          \ "row": a:opt["linenr"] - 1,
          \ "col": a:opt["colnr"] - 1,
          \ "acceptable_num_candidates": get(g:, 'vintellij_acceptable_num_candidates', -1),
          \ }

    call vintellij#RequestAsyncCompletion(a:opt["bufnr"], l:request)
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
  endif
endfunction
