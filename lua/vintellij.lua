local M = {}

function M.on_attach(client_id, buffnr)
	local buf_will_save_autocommand = [[
    augroup vintellij_lsp_will_save
      au!
      au BufWriteCmd <buffer=%d> lua require("vintellij").text_document_will_save_handler(0)
    augroup END
  ]]

	vim.api.nvim_exec(string.format(buf_will_save_autocommand, buffnr), false)
end

function M.text_document_will_save_handler(bufnr)
  vim.notify("text_document_will_save_handler")
	-- bufnr = resolve_bufnr(bufnr)
	-- local uri = vim.uri_from_bufnr(bufnr)
	-- local text = once(buf_get_full_text)
	-- for_each_buffer_client(bufnr, function(client, _client_id)
	--   if client.resolved_capabilities.text_document_save then
	--     local included_text
	--     if client.resolved_capabilities.text_document_save_include_text then
	--       included_text = text()
	--     end
	--     client.notify('textDocument/didSave', {
	--       textDocument = {
	--         uri = uri;
	--       };
	--       text = included_text;
	--     })
	--   end
	-- end)
end

return M
