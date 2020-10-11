"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

" Manage comrade ralated buffer vars

let s:bvar_key='comrade_info'

function! vintellij#bvar#set(buffer, name, value)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    if empty(l:map)
        let l:map = {}
        call setbufvar(a:buffer, s:bvar_key, l:map)
    endif
    let map[a:name] = a:value
endfunction

function! vintellij#bvar#get(buffer, name)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    if empty(l:map) || !has_key(l:map, a:name)
        return v:false
    endif
    return map[a:name]
endfunction

function! vintellij#bvar#has(buffer, name)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    if empty(l:map) || !has_key(l:map, a:name)
        return 0
    endif
    return 1
endfunction

function! vintellij#bvar#remove(buffer, name)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    if empty(l:map) || !has_key(l:map, a:name)
        return
    endif
    call remove(l:map, a:name)
endfunction

" Clear all buffer vars
function! vintellij#bvar#clear(buffer)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    if empty(l:map)
        return
    endif

    let l:to_remove = keys(l:map)
    for l:key in l:to_remove
        call remove(l:map, l:key)
    endfor
endfunction

function! vintellij#bvar#all(buffer)
    let l:map = getbufvar(a:buffer, s:bvar_key)
    return l:map
endfunction
