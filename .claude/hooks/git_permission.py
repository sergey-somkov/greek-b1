"""Claude Code PreToolUse hook: lets read-only git commands run without a prompt.

Permission rules match commands by prefix, so they cannot tell `git branch -vv`
from `git branch new-name`, and an `ask` rule always wins over an `allow` rule.
This hook reads the whole Bash command and decides:

- allow: a single read-only git command, such as `git log`, `git branch -vv`,
  `git tag -l`, `git config --get user.name`, `git stash list`;
- ask: a command that may change the repository through a subcommand that has
  both read-only and changing forms (branch, tag, config, worktree, stash,
  clean, remote, reflog), also inside a compound command;
- no decision: anything else, so that the permission rules and the permission
  mode decide as usual (for example the `ask` rules for git commit and git push).

Deny rules in the settings still win over the allow of this hook.

Install: copy this file to `.claude/hooks/` of a project and add to its
`.claude/settings.json`:

    "hooks": {
      "PreToolUse": [{
        "matcher": "Bash",
        "hooks": [{
          "type": "command",
          "command": "python3 -I \"$CLAUDE_PROJECT_DIR/.claude/hooks/git_permission.py\"",
          "onFailure": "block"
        }]
      }]
    }

Remove the `ask` rules for the subcommands above (such as `Bash(git branch *)`),
since an `ask` rule would still prompt for their read-only forms.
"""

import json
import re
import shlex
import sys

# Global options of git that do not change what the subcommand does.
GLOBAL_OPTIONS = {"--no-pager", "-P", "--no-optional-locks", "--literal-pathspecs"}

# Subcommands that only read the repository.
READ_ONLY = {
    "status", "log", "show", "diff", "blame", "annotate", "shortlog", "describe",
    "grep", "rev-parse", "rev-list", "merge-base", "name-rev", "for-each-ref",
    "show-ref", "show-branch", "ls-files", "ls-tree", "ls-remote", "cat-file",
    "check-attr", "check-ignore", "check-ref-format", "count-objects", "var",
    "diff-tree", "diff-files", "diff-index", "range-diff", "cherry",
    "whatchanged", "verify-commit", "verify-tag", "version", "help",
}

# Options of read-only subcommands that write a file or start a program.
UNSAFE_OPTIONS = ("--output", "-O", "--open-files-in-pager")

# Subcommands that have both read-only and changing forms.
MIXED = re.compile(
    r"\bgit\b.*\b(branch|tag|config|worktree|stash|clean|remote|reflog)\b", re.S
)


def take_value(arg, args):
    """Skips the value of an option given as `--option value`; False if it is missing."""
    return "=" in arg or next(args, None) is not None


def branch_is_read_only(args):
    value_options = {"--contains", "--no-contains", "--merged", "--no-merged",
                     "--points-at", "--sort", "--format"}
    flags = {"-a", "--all", "-r", "--remotes", "-v", "-vv", "--verbose", "-l",
             "--list", "--show-current", "-i", "--ignore-case", "--omit-empty",
             "--color", "--no-color", "--column", "--no-column", "--abbrev",
             "--no-abbrev"}
    list_mode = positional = False
    args = iter(args)
    for arg in args:
        name = arg.split("=", 1)[0]
        if name in value_options:
            if not take_value(arg, args):
                return False
        elif name in flags or re.fullmatch(r"-[arvli]+", arg):
            list_mode = list_mode or arg in ("-l", "--list")
        elif arg.startswith("-"):
            return False
        else:
            positional = True
    # Without list mode a name creates a branch; with it, a name is a pattern.
    return list_mode or not positional


def tag_is_read_only(args):
    value_options = {"--contains", "--no-contains", "--merged", "--no-merged",
                     "--points-at", "--sort", "--format"}
    flags = {"-l", "--list", "-v", "--verify", "-i", "--ignore-case",
             "--color", "--no-color", "--column", "--no-column", "--omit-empty"}
    list_mode = positional = False
    args = iter(args)
    for arg in args:
        name = arg.split("=", 1)[0]
        if name in value_options:
            if not take_value(arg, args):
                return False
        elif name in flags or re.fullmatch(r"-n\d*", arg):
            list_mode = list_mode or arg not in ("-i", "--ignore-case")
        elif arg.startswith("-"):
            return False
        else:
            positional = True
    # Without list mode or --verify a name creates a tag.
    return list_mode or not positional


def config_is_read_only(args):
    value_options = {"-f", "--file", "--blob", "--type", "--default"}
    read_actions = {"--get", "--get-all", "--get-regexp", "--get-urlmatch",
                    "--get-color", "--get-colorbool", "-l", "--list"}
    flags = {"--global", "--system", "--local", "--worktree", "--show-origin",
             "--show-scope", "--name-only", "-z", "--null", "--bool", "--int",
             "--bool-or-int", "--path", "--expiry-date", "--includes",
             "--no-includes", "--fixed-value", "--all", "--regexp", "--url",
             "--value", "--show-names"}
    read_action = False
    positional = []
    args = iter(args)
    for arg in args:
        name = arg.split("=", 1)[0]
        if name in value_options:
            if not take_value(arg, args):
                return False
        elif name in read_actions:
            read_action = True
        elif name in flags:
            pass
        elif arg.startswith("-"):
            return False
        else:
            positional.append(arg)
    if positional[:1] in (["get"], ["list"]):
        return True
    if positional[:1] in (["set"], ["unset"], ["rename-section"],
                          ["remove-section"], ["edit"]):
        return False
    # `git config <name>` reads a value; `git config <name> <value>` sets it.
    return read_action or len(positional) <= 1


def worktree_is_read_only(args):
    return args[:1] == ["list"] and all(
        arg in ("--porcelain", "-v", "--verbose", "-z") for arg in args[1:])


def stash_is_read_only(args):
    return args[:1] in (["list"], ["show"])


def clean_is_read_only(args):
    dry_run = False
    args = iter(args)
    for arg in args:
        if arg in ("-e", "--exclude"):
            next(args, None)
        elif arg in ("-i", "--interactive"):
            return False
        elif arg == "--dry-run" or re.fullmatch(r"-[a-zA-Z]*n[a-zA-Z]*", arg):
            if "i" in arg.lstrip("-") and not arg.startswith("--"):
                return False
            dry_run = True
    return dry_run


def remote_is_read_only(args):
    if all(arg in ("-v", "--verbose") for arg in args):
        return True
    return args[:1] in (["get-url"], ["show"])


def reflog_is_read_only(args):
    return args[:1] not in (["expire"], ["delete"], ["drop"], ["write"])


MIXED_CHECKS = {
    "branch": branch_is_read_only,
    "tag": tag_is_read_only,
    "config": config_is_read_only,
    "worktree": worktree_is_read_only,
    "stash": stash_is_read_only,
    "clean": clean_is_read_only,
    "remote": remote_is_read_only,
    "reflog": reflog_is_read_only,
}


def split_simple_command(command):
    """Returns the words of a simple command, or None if it uses shell syntax."""
    if re.search(r"[$`\n\\]", command):
        return None
    lexer = shlex.shlex(command, posix=True, punctuation_chars=True)
    lexer.whitespace_split = True
    lexer.commenters = ""
    try:
        words = list(lexer)
    except ValueError:
        return None
    if any(word and set(word) <= set("();<>|&") for word in words):
        return None
    return words


def classify(words):
    """Returns "allow", "ask" or None for the words of a simple git command."""
    if not words or words[0] != "git":
        return None
    i = 1
    while i < len(words) and words[i].startswith("-"):
        if words[i] not in GLOBAL_OPTIONS:
            return None
        i += 1
    if i == len(words):
        return None
    subcommand, args = words[i], words[i + 1:]
    if any(arg.startswith(UNSAFE_OPTIONS) for arg in args):
        return "ask" if subcommand in READ_ONLY or subcommand in MIXED_CHECKS else None
    if subcommand in READ_ONLY:
        return "allow"
    check = MIXED_CHECKS.get(subcommand)
    if check:
        return "allow" if check(args) else "ask"
    return None


def decide(decision, reason):
    print(json.dumps({
        "hookSpecificOutput": {
            "hookEventName": "PreToolUse",
            "permissionDecision": decision,
            "permissionDecisionReason": reason,
        }
    }))


def main():
    command = json.load(sys.stdin).get("tool_input", {}).get("command", "").strip()
    if not re.search(r"\bgit\b", command):
        return
    words = split_simple_command(command)
    decision = classify(words) if words else None
    if decision == "allow":
        decide("allow", "Read-only git command.")
    elif decision == "ask" or MIXED.search(command):
        decide("ask", "The git command may change the repository.")


if __name__ == "__main__":
    main()
