local utils = require("vintellij.utils")
local M = {}

local function setup_events(lib_dirs)
	vim.api.nvim_command("autocmd BufEnter * lua require('vintellij.lsp').on_buf_enter()")

	if lib_dirs ~= nil then
		for _, lib_dir in pairs(lib_dirs) do
			vim.api.nvim_command(
				string.format(
					"autocmd BufEnter zipfile://%s* lua require('vintellij.lsp').on_lib_file_loaded()",
					lib_dir
				)
			)
		end
	end
end

local function on_attach_vintellij(bufnr)
	-- Currently, nvim lsp does not support `will save` that why we need to add the autocmd here
	-- TODO: Remove this command when nvim support `will save`
	vim.api.nvim_command(
		string.format(
			"autocmd BufWriteCmd <buffer=%d> lua require('vintellij.lsp').text_document_will_save_handler(0)",
			bufnr
		)
	)
end

function M.text_document_will_save_handler(bufnr)
	bufnr = utils.resolve_bufnr(bufnr)
	local uri = vim.uri_from_bufnr(bufnr)

	vim.lsp.for_each_buffer_client(bufnr, function(client, _)
		if client.name == "vintellij" then
			client.notify("textDocument/willSave", {
				textDocument = {
					uri = uri,
				},
				reason = 1,
			})

			vim.api.nvim_buf_set_option(bufnr, "modified", false)
		end
	end)
end

function M.on_buf_enter()
	local bufnr = utils.resolve_bufnr(0)
	local uri = vim.uri_from_bufnr(bufnr)

	vim.lsp.for_each_buffer_client(bufnr, function(client, _)
		if client.name == "vintellij" then
			client.notify("textDocument/didOpen", {
				textDocument = {
					uri = uri,
				},
			})
		end
	end)
end

function M.on_lib_file_loaded()
	local current_clients = vim.lsp.buf_get_clients(0)
	if current_clients ~= nil and #current_clients ~= 0 then
		return
	end

	local clients = vim.lsp.get_active_clients()
	for _, client in pairs(clients) do
		if client.name == "vintellij" then
			vim.lsp.buf_attach_client(0, client.id)
		end
	end
end

function M.setup()
	local cfg = require("vintellij.config").config

	local configs_ok, configs = pcall(require, "lspconfig.configs")

	if not configs_ok then
		utils.err("Plugin lspconfig not found")
		return
	end

	cfg.common_capabilities.textDocument.synchronization.willSave = true

	if not configs.vintellij then
		configs.vintellij = {
			default_config = {
				cmd = { "nc", "localhost", "6969" },
				root_dir = function(fname)
					local util = require("lspconfig.util")

					local root_files = {
						"settings.gradle", -- Gradle (multi-project)
						"settings.gradle.kts", -- Gradle (multi-project)
						"build.xml", -- Ant
						"pom.xml", -- Maven
					}

					local fallback_root_files = {
						"build.gradle", -- Gradle
						"build.gradle.kts", -- Gradle
					}
					return util.root_pattern(unpack(root_files))(fname)
						or util.root_pattern(unpack(fallback_root_files))(fname)
				end,
				filetypes = { "kotlin", "java" },
				autostart = false,
				on_attach = function(client_id, bufnr)
					cfg.common_on_attach(client_id, bufnr)
					on_attach_vintellij(bufnr)
				end,
				on_init = cfg.common_on_init,
				capabilities = cfg.common_capabilities,
			},
		}
	end

	configs.vintellij.setup({})

	setup_events(cfg.lib_dirs)
end

return M
