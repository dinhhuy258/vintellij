local M = {}

local function resolve_bufnr(bufnr)
	vim.validate({ bufnr = { bufnr, "n", true } })
	if bufnr == nil or bufnr == 0 then
		return vim.api.nvim_get_current_buf()
	end

	return bufnr
end

function M.on_attach(_, bufnr)
	vim.api.nvim_command(
		string.format(
			"autocmd BufWriteCmd <buffer=%d> lua require('vintellij').text_document_will_save_handler(0)",
			bufnr
		)
	)
end

function M.text_document_will_save_handler(bufnr)
	bufnr = resolve_bufnr(bufnr)
	local uri = vim.uri_from_bufnr(bufnr)

	vim.lsp.for_each_buffer_client(bufnr, function(client, _)
    -- TODO: Check capabilities
		client.notify("textDocument/willSave", {
			textDocument = {
				uri = uri,
			},
			reason = 1,
		})
	end)
end

return M
