"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

" Called by JetBrains to register the buffer with the calling channel.
" If lines is not empty, the content of buffer will be set. This means
" whenever JetBrains try to register the buffer, the nvim buffer's content
" will be synced with JetBrains' corresponding file content.
function! vintellij#buffer#Register(buf, channel, lines) abort
    call vintellij#key#Register(a:buf)
    call vintellij#bvar#clear(a:buf)
    call vintellij#bvar#set(a:buf, 'channel', a:channel)
    call setbufvar(a:buf, '&buftype', 'acwrite')
    augroup ComradeBufEvents
        execute('autocmd! BufWriteCmd <buffer=' . a:buf . '>')

        execute('autocmd BufWriteCmd <buffer=' . a:buf .
                    \ '> call s:WriteBuffer(' . a:buf. ')')
    augroup END

    " Without this, vim might put all the old signs at the first line.
    call vintellij#sign#Clear(a:buf)

    if !empty(a:lines)
        call nvim_buf_set_lines(a:buf, 0, -1, v:true, a:lines)
        " treat the buffer as an unmodified one since its content is the same
        " with JetBrains' which has auto-save feature always enabled.
        call setbufvar(a:buf, '&modified', 0)
    endif
endfunction

" Unregister the given buffer
function! vintellij#buffer#Unregister(buf) abort
    call vintellij#key#Unregister(a:buf)
    let l:has_channel = vintellij#bvar#has(a:buf, 'channel')
    call vintellij#bvar#clear(a:buf)

    if l:has_channel
        call setbufvar(a:buf, '&buftype', '')
        execute('autocmd! ComradeBufEvents * <buffer=' . a:buf . '>')

        call vintellij#sign#SetSigns(a:buf)
        call vintellij#highlight#SetHighlights(a:buf)
    endif
endfunction

" Unregister the current buffer
function! vintellij#buffer#UnregisterCurrent()
    call vintellij#buffer#Unregister(bufnr('%'))
endfunction

" To notify the JetBrains that nvim switches to a different buffer.
function! vintellij#buffer#Notify() abort
    let l:bufId = bufnr('%')
    let l:bufPath = expand('%:p')

    if (filereadable(l:bufPath))
        let l:channels = vintellij#jetbrain#Channels()

        for channel in l:channels
            try
                call call('rpcnotify', [channel, 'comrade_buf_enter', {'id' : l:bufId, 'path' : l:bufPath}])
            catch /./
                call vintellij#util#TruncatedEcho('Failed to send new buffer notification to JetBrains instance ' . channel . '.\n' . v:exception)
            endtry
        endfor
    endif
endfunction

function! s:WriteBuffer(buffer)
    if !vintellij#bvar#has(a:buffer, 'channel')
        call vintellij#buffer#Unregister(a:buffer)
        return
    endif

    let l:channel = vintellij#bvar#get(a:buffer, 'channel')
    try
        call call('rpcrequest', [l:channel, 'comrade_buf_write', {'id' : a:buffer}])
        call setbufvar(a:buffer, '&modified', 0)
    catch /./
        if !vintellij#jetbrain#IsChannelExisting(l:channel)
            call vintellij#buffer#Unregister(a:buffer)
            echoe 'The JetBrain has been disconnected thus please write the buffer again if you want to save it.'
        else
            " The disconnecting takes a few seconds, user may have to write
            " again to trigger the unregister.
            echoe 'JetBrain failed to save file.' . v:exception
        endif
    endtry

endfunction
