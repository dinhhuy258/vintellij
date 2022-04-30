local M = {}

function M.setup(cfg)
	require("vintellij.config").setup(cfg)
	cfg = require("vintellij.config").config

	require("vintellij.lsp").setup()
	require("vintellij.handlers").setup()

	vim.api.nvim_command([[command! VintellijGenDoc lua require("vintellij.doc").gen_doc()]])
	vim.api.nvim_set_keymap("n", cfg.keymaps.gen_doc, "<CMD>lua require('vintellij.commands.doc').gen_doc()<CR>", {
		noremap = true,
		silent = true,
		expr = false,
	})
end

return M
