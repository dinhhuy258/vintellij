local M = {}

function M.gen_doc()
	local current_clients = vim.lsp.buf_get_clients(0)
	if current_clients == nil or #current_clients == 0 then
		return
	end

	for _, client in pairs(current_clients) do
		if client.name == "vintellij" then
			local row, col = unpack(vim.api.nvim_win_get_cursor(0))

			vim.lsp.buf.execute_command({
				command = "gen_doc",
				arguments = {
					vim.uri_from_bufnr(0),
					row,
					col,
				},
			})
			break
		end
	end
end

return M
