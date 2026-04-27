---
name: sast-android-cli
description: Android CLI commands specific to this SAST project
---

# SAST Android CLI Skill

This skill provides project-specific commands using the `android` CLI tool.

## Quick commands

```bash
# Project structure analysis
android describe

# Device interaction
android screen capture -o screenshot.png
android layout -p -o layout.json
```

## Project-specific notes

- This is a Koin DI project (not Hilt)
- Main activity: `dev.shinyparadise.sast.ui.MainActivity`
- Module: `:app`

## Reference

For full android-cli commands, see global skill at `~/.config/opencode/skills/android-cli/SKILL.md`