---
name: using-superpowers
description: "Use when the user explicitly names using-superpowers, uses /using-superpowers, links to using-superpowers/SKILL.md, or clearly asks to use this skill"
disable-model-invocation: true
---

<SUBAGENT-STOP>
If you were dispatched as a subagent to execute a specific task, ignore this skill.
</SUBAGENT-STOP>

<INVOCATION-GATE>
Invoke this skill only when the current user message explicitly names `using-superpowers`, uses `/using-superpowers`, links to this skill, or clearly says to use using-superpowers.

Do not auto-invoke this skill because it might be useful, seems related, or the conversation is starting.
</INVOCATION-GATE>

## The Rule

**Invoke only skills that the current user message explicitly specifies**, and invoke them before responding or acting. Explicit specification includes:

- Writing the skill name directly, such as `using-superpowers`
- Using slash form, such as `/using-superpowers`
- Providing a Markdown link or file path that points to a skill
- Clearly asking to use, invoke, or load a skill

If the user did not explicitly specify this skill, do not invoke it. Do not treat "the task seems related" as explicit specification.

After invocation, announce "Using [skill] to [purpose]" and follow the invoked skill exactly. If it has a checklist, create a task for each checklist item.

## Skill Priority

When the same user message explicitly specifies multiple skills, run process skills first, then implementation skills.

- "Use using-superpowers and writing-skills to edit a skill" -> use `using-superpowers` to confirm explicit skill invocation, then use `writing-skills` to edit the skill.
- "/brainstorming design X" -> invoke `brainstorming`.
- "Help me build X" without explicitly naming `brainstorming` -> do not auto-invoke `brainstorming` because the task is creative.

## Red Flags

These thoughts mean you are mistaking implicit relevance for explicit specification:

| Thought | Reality |
|---------|---------|
| "This skill might be useful" | Might be useful is not explicit specification. |
| "This is a new conversation, so invoke it" | A new conversation is not a trigger. |
| "The user wants creative or implementation work, so auto-invoke brainstorming" | Do not invoke `brainstorming` unless it was explicitly named. |
| "Invoking it just in case cannot hurt" | Unrequested skills can change the user's intended workflow. |
| "The description used to say this was mandatory" | The current invocation gate wins: only explicit names, slash usage, links, or clear requests trigger it. |

## Platform Adaptation

If your harness appears here, read its reference file for special instructions:

- Codex: `references/codex-tools.md`
- Pi: `references/pi-tools.md`
- Antigravity: `references/antigravity-tools.md`

## User Instructions

User instructions (CLAUDE.md, AGENTS.md, GEMINI.md, etc, direct requests) take precedence over skills, which in turn override default behavior. Only skip skill workflows or instructions when your human partner has explicitly told you to.
