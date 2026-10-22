# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Language

All text produced for this project (code, comments, commit messages, documentation, etc.) must be in plain technical English, regardless of the language the conversation with the user is conducted in.

## Git

- The index (staging area) belongs to the user. The user stages files after reviewing Claude's changes. Never add files to the index or remove them from it: no `git add`, `git stage`, `git rm`, `git mv`, `git reset`, `git restore --staged`, `git update-index`, `git stash`, and so on. Rename and delete files with plain `mv` / `rm`.
- `git commit` and `git push` only on the user's explicit command, every time. Approval for one commit does not extend to the next.
- Branches only on the user's explicit command, every time: no creating, switching, renaming or deleting branches (`git branch <name>`, `git switch`, `git checkout`, and so on). Reading the list of branches is fine.
- Commit exactly what the user has staged: no `git commit -a`, no paths passed to `git commit`.
- These rules are partly enforced in [.claude/settings.json](.claude/settings.json), but they apply in full even where the settings do not catch a command.

## The machine and the phone

- Anything that needs `sudo`, changes the system, or installs software outside the project runs only after the user approves it, every time.
- Do not edit the shell profile (`~/.bashrc`, `~/.profile`) or the Claude Code settings without the user's approval.
- The app on the phone holds the user's progress data. Never do anything that loses it without the user's explicit command: no `adb uninstall`, no `pm clear`, no change of the signing key (which forces an uninstall). Install updates over the installed app with `adb install -r`.
- On the phone, touch only this app. Do not change the phone's settings, other apps or files, and do not simulate input (`adb shell input`) without the user's approval.
- These rules are partly enforced in [.claude/settings.json](.claude/settings.json), but they apply in full even where the settings do not catch a command.
