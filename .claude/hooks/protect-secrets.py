"""PreToolUse hook: block reads and edits of signing secrets and machine-local config."""
import json
import re
import sys

NAME = r"(keystore\.properties|local\.properties|[\w.-]+\.(?:jks|keystore))"
PROTECTED = re.compile(rf"(?:^|[\\/\s\"'=]){NAME}$", re.IGNORECASE)
# A shell line that reads, writes, copies or stages a protected file.
SHELL_ACCESS = re.compile(
    rf"(?:\b(?:cat|head|tail|less|more|type|gc|Get-Content|Set-Content|Out-File|cp|mv|rm|del|copy|move|sed|awk|tee|"
    rf"nano|vim?|notepad|code|keytool|git\s+add|git\s+show)\b|>>?)[^\n]*?{NAME}",
    re.IGNORECASE,
)

event = json.load(sys.stdin)
tool = event.get("tool_name", "")
args = event.get("tool_input", {}) or {}

if tool in ("Read", "Edit", "Write", "MultiEdit", "NotebookEdit"):
    match = PROTECTED.search((args.get("file_path") or args.get("notebook_path") or "").strip())
elif tool in ("Bash", "PowerShell"):
    match = SHELL_ACCESS.search(args.get("command", ""))
else:
    sys.exit(0)

if match:
    print(
        f"Blocked: '{match.group(1)}' holds signing secrets or machine-local config. "
        "Ask the user to change it themselves; never read, print, create or commit these files.",
        file=sys.stderr,
    )
    sys.exit(2)
