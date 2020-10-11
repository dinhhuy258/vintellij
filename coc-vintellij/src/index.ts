// https://github.com/joshua7v/coc-comrade/blob/master/src/index.ts

import {
  commands,
  CompleteResult,
  ExtensionContext,
  sources,
  workspace,
} from "coc.nvim";

const delay = (delay: number) => new Promise((res) => setTimeout(res, delay));

export async function activate(context: ExtensionContext): Promise<void> {
  workspace.showMessage(`coc-vintellij connected!`);

  context.subscriptions.push(
    sources.createSource({
      name: "coc-vintellij completion source", // unique id
      shortcut: "IDEA", // [CS] is custom source
      priority: 99,
      triggerCharacters: ["."],
      doComplete: async function () {
        const buffer = await workspace.nvim.buffer;
        const bufferId = buffer.id;
        const bufferName = await buffer.name;
        const window = await workspace.nvim.window;
        const [row, col] = await window.cursor;
        const changedTick = await workspace.nvim.eval(
          `nvim_buf_get_changedtick(${bufferId})`
        );

        const ret = {
          buf_id: buffer.id,
          buf_name: bufferName,
          buf_changedtick: changedTick,
          row: row - 1,
          col: col,
          new_request: true,
        };

        let results = await workspace.nvim.call("vintellij#RequestCompletion", [
          buffer.id,
          ret,
        ]);

        while (
          typeof results.is_finished == "boolean" &&
          !results.is_finished
        ) {
          await delay(100);
          ret.new_request = false;
          results = await workspace.nvim.call("vintellij#RequestCompletion", [
            buffer.id,
            ret,
          ]);
        }

        return {
          items: results.candidates.map((x) => ({
            ...x,
            menu: (this as any).menu,
          })),
        } as CompleteResult;
      },
    })
  );
}
