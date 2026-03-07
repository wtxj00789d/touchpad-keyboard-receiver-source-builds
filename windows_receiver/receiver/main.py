from __future__ import annotations

import os
import sys
import tkinter as tk

# Support both:
# 1) python -m receiver.main
# 2) python receiver/main.py
# 3) frozen entry execution that behaves like script mode
if __package__ is None or __package__ == "":
    sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))
    from receiver.config import RuntimeConfig
    from receiver.controller import ReceiverController
    from receiver.ui import ReceiverUI
else:
    from .config import RuntimeConfig
    from .controller import ReceiverController
    from .ui import ReceiverUI


def main() -> None:
    config = RuntimeConfig()
    controller = ReceiverController(config)

    root = tk.Tk()
    ui: ReceiverUI | None = None

    def _on_close() -> None:
        if ui is not None:
            ui.dispose()
        controller.shutdown()
        root.destroy()

    ui = ReceiverUI(root, controller, on_exit=_on_close)
    root.protocol("WM_DELETE_WINDOW", _on_close)
    root.mainloop()


if __name__ == "__main__":
    main()
