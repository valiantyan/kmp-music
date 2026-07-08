---
name: yangming-requirement-clarification
description: "Use this before product design, feature implementation, architecture changes, or ambiguous task execution. Clarifies product requirements using Yangming-style objective calibration, concrete investigation, and self-correction."
---

# Yangming Requirement Clarification

## Purpose

Clarify product requirements before design or implementation. The goal is to avoid building the user's surface wording when the real objective, success criteria, constraints, or risks are still unclear.

This SKILL translates Wang Yangming's practical philosophy into product clarification discipline:

- **致良知**: align with the real objective, not only the latest wording.
- **知行合一**: prove understanding through a concrete requirement artifact.
- **事上磨炼**: ground every judgment in concrete users, scenarios, constraints, and evidence.
- **省察克治**: detect and correct assumptions, overconfidence, and goal drift.

Do not quote philosophy unless the user asks. Apply the method silently.

## When to Use

Use this SKILL when any of these are true:

- The user asks to build, design, change, optimize, refactor, or integrate something.
- The requirement contains vague words: simple, fast, better, smart, automatic, good UX, production-ready, robust, secure, scalable.
- The user describes a solution before explaining the problem.
- The work could cause wasted engineering effort if the goal is misunderstood.
- There are multiple possible interpretations, users, workflows, constraints, or success metrics.

Do not use this SKILL for trivial copy edits, direct factual answers, or tasks where all acceptance criteria are already explicit.

## Hard Gates

Before producing a design or implementation plan, you MUST identify:

1. Real objective
2. Target user or actor
3. Current pain or trigger
4. Desired outcome
5. Success criteria
6. Key constraints
7. Open uncertainties
8. Minimal next action

If any of these are unknown, either ask the smallest decisive clarification question or state an explicit assumption and mark it as provisional.

## Operating Loop

### 1. 明目标 — Identify the Real Objective

Separate the user's wording from the underlying goal.

Output internally or explicitly when useful:

```text
User said: ...
Likely real objective: ...
What must be true for this to be useful: ...
```

### 2. 察偏差 — Detect Assumptions and Goal Drift

Check whether you are assuming any of the following without evidence:

- User type
- Business goal
- Technical platform
- Priority order
- Performance target
- Security requirement
- Data source
- Scope boundary
- Existing system behavior
- Definition of done

If an assumption is decisive, ask one focused question. Do not ask a long questionnaire unless the task truly requires it.

### 3. 格事实 — Ground in Concrete Scenarios

Convert abstract requests into concrete usage scenes:

```text
Actor:
Trigger:
Current workflow:
Pain:
Desired workflow:
Output/result:
Failure case:
```

If the user has provided no scenario, produce one provisional scenario and label it as an assumption.

### 4. 践行动 — Produce a Requirement Artifact

Do not end with only questions. Produce one of the following:

- Clarified requirement brief
- PRD skeleton
- User story set
- Acceptance criteria
- Decision table
- Risk/assumption list
- Next-step clarification question

### 5. 观结果 — Validate Usefulness

Check whether the artifact can guide implementation or decision-making. If not, refine it.

A clarified requirement must answer:

```text
What will be built?
For whom?
Why is it needed?
What is in scope?
What is out of scope?
How will success be judged?
What can go wrong?
What remains unknown?
```

### 6. 正其心 — Correct Before Continuing

Before finalizing, inspect for:

- Asking too many questions
- Pretending uncertain facts are known
- Solving the wrong problem
- Accepting the user's proposed solution too quickly
- Ignoring constraints
- Missing acceptance criteria
- Missing negative cases

## Output Format

Use this default format unless the user requests another format:

```markdown
## Requirement Understanding

**User wording:** ...
**Likely true objective:** ...
**Primary actor/user:** ...
**Current pain:** ...
**Desired outcome:** ...

## Clarified Scope

### In Scope
- ...

### Out of Scope
- ...

## Acceptance Criteria

1. Given ..., when ..., then ...
2. ...

## Key Risks / Unknowns

| Item | Why it matters | Current handling |
|---|---|---|
| ... | ... | Ask / Assume / Defer |

## Smallest Decisive Next Step

...
```

## Clarification Question Policy

Ask at most one question first when one answer would significantly change the solution.

Good question:

```text
Before designing this, which outcome matters most: reducing manual work, improving accuracy, or speeding up delivery?
```

Bad question:

```text
Please answer these 15 questions before I continue.
```

If the user explicitly asks you not to ask questions, make the best provisional assumptions and mark them clearly.

## Quality Bar

A good requirement clarification is:

- Concrete enough to implement
- Honest about uncertainty
- Focused on the real objective
- Minimal but sufficient
- Testable through acceptance criteria
- Clear about scope boundaries
- Useful even if the user does not answer immediately

## Privacy and Reasoning Boundary

Do not expose hidden chain-of-thought. Provide concise reasoning summaries, assumptions, evidence, and decisions instead. The user should see the requirement artifact and the decisive reasoning, not internal scratchpad deliberation.

## Stop Condition

Stop clarifying and move to a usable artifact when:

- The real objective is sufficiently clear to propose a bounded next step.
- Remaining unknowns can be handled as explicit assumptions.
- Additional questions would delay progress more than they reduce risk.

If the task is high-risk, clearly state why more clarification is required.
