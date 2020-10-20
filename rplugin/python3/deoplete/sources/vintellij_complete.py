# ============================================================================
# AUTHOR: beeender <chenmulong at gmail.com>
# License: GPLv3
# ============================================================================

from .base import Base


class Source(Base):

    def __init__(self, vim):
        Base.__init__(self, vim)

        self.name = 'Vintellij'
        self.mark = '[IntelliJ]'
        self.filetypes = ['kotlin', 'java']
        self.rank = 999
        self.max_pattern_length = 100
        self.min_pattern_length = 1
        self.max_abbr_width = 300
        self.max_menu_width = 300
        # Just put all possible patterns here. Category them when we have a
        # performance issue.
        self.input_pattern = (r'(\.)\w*|' r'(:)\w*|' r'(::)\w*|' r'(->)\w*')
        self.is_debug_enabled = True
        self.dup = True

    def gather_candidates(self, context):
        buf_id = context["bufnr"]
        win = self.vim.current.window

        row = win.cursor[0] - 1
        col = win.cursor[1] - 1
        request = {"buf_id": buf_id, "row": row, "col": col, "new_request": not context["is_async"]}

        completion_result = self.vim.call("vintellij#RequestCompletion", buf_id, request)

        if completion_result:
            context["is_async"] = not completion_result["is_finished"]
            return completion_result["candidates"]

        context["is_async"] = False
        return []
