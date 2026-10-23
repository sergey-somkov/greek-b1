"""Claude Code PreToolUse hook: lets read-only git and gh commands run without a prompt.

Permission rules match commands by prefix, so they cannot tell `git branch -vv`
from `git branch new-name`, or `gh pr view` from `gh pr merge`, and an `ask`
rule always wins over an `allow` rule. This hook reads the whole Bash command
and decides:

- allow: read-only git and gh commands, such as `git log`, `git branch -vv`,
  `git tag -l`, `git config --get user.name`, `git stash list`, `gh pr view`,
  `gh run list`, `gh api repos/{owner}/{repo}`, alone or joined by &&, ||, ;
  or |, optionally with `cd <dir>` among them and with redirections that write
  no file (`2>/dev/null`, `2>&1`);
- ask: a command that may change the repository through a git subcommand that
  has both read-only and changing forms (branch, tag, config, worktree, stash,
  clean, remote, reflog), and any gh command that is not known to be
  read-only, also inside a compound command;
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

Remove the `ask` rules for the git subcommands above (such as
`Bash(git branch *)`) and for gh (`Bash(gh *)`), since an `ask` rule would
still prompt for their read-only forms. This hook asks for the other forms.
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


# Read-only gh commands: a command and its subcommands, or a command alone (None).
GH_READ_ONLY = {
    "repo": {"view", "list"},
    "pr": {"view", "list", "diff", "checks", "status"},
    "issue": {"view", "list", "status"},
    "run": {"view", "list", "watch"},
    "workflow": {"view", "list"},
    "release": {"view", "list"},
    "label": {"list"},
    "gist": {"view", "list"},
    "search": {"repos", "issues", "prs", "code", "commits"},
    "cache": {"list"},
    "ruleset": {"view", "list", "check"},
    "variable": {"list"},
    "secret": {"list"},
    "auth": {"status"},
    "config": {"get", "list"},
    "extension": {"list"},
    "alias": {"list"},
    "status": None,
    "version": None,
    "help": None,
    "--version": None,
    "--help": None,
}

# Options of read-only gh commands that open a browser or print a secret.
GH_UNSAFE_OPTIONS = {"-w", "--web"}
GH_AUTH_UNSAFE_OPTIONS = {"-t", "--show-token"}

# The word gh in a command that cannot be split into words.
GH = re.compile(r"(?<![\w.-])gh(?![\w.-])")


def gh_api_is_read_only(args):
    """`gh api` sends GET, or POST when it has fields; GraphQL may mutate."""
    value_options = {"-X", "--method", "-F", "--field", "-f", "--raw-field",
                     "--input", "-H", "--header", "-q", "--jq", "-t",
                     "--template", "--cache", "--hostname", "-p", "--preview"}
    flags = {"--paginate", "-i", "--include", "--silent", "--verbose"}
    method, fields, endpoints = None, False, []
    args = iter(args)
    for arg in args:
        if arg.startswith("--"):
            name, eq, value = arg.partition("=")
            if name in value_options and not eq:
                value = next(args, None)
        elif arg[:2] in value_options:
            name, value = arg[:2], arg[2:] or next(args, None)
        else:
            name, value = arg, None
        if name in value_options:
            if value is None:
                return False
            if name in ("-X", "--method"):
                method = value.upper()
            elif name in ("-F", "--field", "-f", "--raw-field", "--input"):
                fields = True
            elif name in ("-H", "--header") and not value.lower().startswith("accept:"):
                return False
        elif name in flags:
            pass
        elif arg.startswith("-"):
            return False
        else:
            endpoints.append(arg)
    if len(endpoints) != 1 or endpoints[0] == "graphql":
        return False
    return (method or ("POST" if fields else "GET")) == "GET"


def classify_gh(args):
    """Returns "allow" or "ask" for the arguments of a gh command."""
    if not args:
        return "ask"
    command, rest = args[0], args[1:]
    if command == "api":
        return "allow" if gh_api_is_read_only(rest) else "ask"
    names = {arg.split("=", 1)[0] for arg in rest}
    unsafe = GH_UNSAFE_OPTIONS | (GH_AUTH_UNSAFE_OPTIONS if command == "auth" else set())
    if command not in GH_READ_ONLY or unsafe & names:
        return "ask"
    subcommands = GH_READ_ONLY[command]
    return "allow" if subcommands is None or rest[:1] and rest[0] in subcommands else "ask"


def is_gh(word):
    return word == "gh" or word.endswith("/gh")


SEPARATORS = {"&&", "||", ";", "|"}

# A quoted string, which is kept as it is, or a redirection that writes no file:
# output to /dev/null (`2>/dev/null`, `&>/dev/null`) or from one standard stream
# to another (`2>&1`, `>&2`). A redirection must stand alone, not inside a word.
HARMLESS_REDIRECTION = re.compile(r"""
    '[^']*' | "[^"]*"
    | (?<![^\s;&|]) (?: [012]?>>? | &>>? ) [ \t]* /dev/null (?![^\s;&|])
    | (?<![^\s;&|]) [012]?>&[012] (?![^\s;&|])
""", re.X)


def drop_harmless_redirections(command):
    return HARMLESS_REDIRECTION.sub(
        lambda match: match[0] if match[0][0] in "'\"" else " ", command)


def split_commands(command):
    """Returns the words of each simple command joined by &&, ||, ; or |,
    or None if the command uses other shell syntax."""
    if re.search(r"[$`\n\\]", command):
        return None
    command = drop_harmless_redirections(command)
    lexer = shlex.shlex(command, posix=True, punctuation_chars=True)
    lexer.whitespace_split = True
    lexer.commenters = ""
    try:
        words = list(lexer)
    except ValueError:
        return None
    commands = [[]]
    for word in words:
        if word in SEPARATORS:
            commands.append([])
        elif word and set(word) <= set("();<>|&"):
            return None
        else:
            commands[-1].append(word)
    return [words for words in commands if words]


def is_cd(words):
    """`cd <dir>` changes no files, so it does not affect the decision."""
    return len(words) == 2 and words[0] == "cd"


def classify(words):
    """Returns "allow", "ask" or None for the words of a simple command."""
    if not words:
        return None
    if is_gh(words[0]):
        return classify_gh(words[1:])
    if words[0] != "git":
        # gh run by another command, such as `env gh` or `xargs gh`.
        return "ask" if any(is_gh(word) for word in words) else None
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
    if not re.search(r"\bgit\b", command) and not GH.search(command):
        return
    commands = split_commands(command)
    if not commands:
        if MIXED.search(command) or GH.search(command):
            decide("ask", "The command may change the repository or GitHub.")
        return
    decisions = [None if is_cd(words) else classify(words) for words in commands]
    if any(decision == "ask" or (decision is None and MIXED.search(" ".join(words)))
           for words, decision in zip(commands, decisions)):
        decide("ask", "The command may change the repository or GitHub.")
    elif "allow" in decisions and all(
            decision == "allow" or is_cd(words)
            for words, decision in zip(commands, decisions)):
        decide("allow", "Read-only git or gh command.")


if __name__ == "__main__":
    main()
