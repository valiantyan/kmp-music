# Domain Docs

How the engineering skills should consume this repo's domain documentation.

## Before exploring, read these

- `CONTEXT.md` at the repo root
- `docs/adr/` for ADRs that touch the area being changed

If these files do not exist, proceed silently. The domain-modeling skills create them lazily when terms or decisions get resolved.

## File structure

Single-context repo:

```text
/
|-- CONTEXT.md
|-- docs/adr/
`-- composeApp/
```

## Use the glossary's vocabulary

When output names a domain concept, use the term as defined in `CONTEXT.md`. If the concept is missing, note it for domain modeling instead of inventing competing language.

## Flag ADR conflicts

If output contradicts an existing ADR, surface that conflict explicitly rather than silently overriding it.
