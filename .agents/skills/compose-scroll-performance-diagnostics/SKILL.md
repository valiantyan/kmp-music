---
name: compose-scroll-performance-diagnostics
description: Manual-only Compose list performance diagnostic workflow. Use this skill only when the user explicitly invokes `$compose-scroll-performance-diagnostics` or asks to run the exact skill by name; do not auto-invoke it for generic Compose, list jank, dropped frames, or recomposition work.
---

# Compose Scroll Performance Diagnostics

Use this skill as a measurement-first workflow for Compose list jank, dropped frames, slow list entry, or excessive recomposition. Do not apply a fixed recipe such as "add keys" or "remove shadows" until the bottleneck is classified.

## Manual Invocation

- Run this skill only when the user explicitly asks for `$compose-scroll-performance-diagnostics` or names `compose-scroll-performance-diagnostics`.
- If a list performance issue appears without explicit invocation, use normal debugging instead and do not cite this skill as active.
- Keep fixes root-cause oriented. If the root fix requires a broad architecture change, explain the tradeoff and ask before expanding scope.

## Diagnostic Loop

1. Define the symptom narrowly:
   - Slow entry into a list page.
   - Continuous scroll jank.
   - State changes causing list-wide recomposition.
   - First image appearance or image-heavy rows stuttering.
   - Section switching, filtering, sorting, or paging stuttering.
2. Isolate the action:
   - Reset metrics immediately before the action.
   - Perform one repeatable tap, swipe, section switch, or state change.
   - Collect metrics immediately after the action.
3. Classify the bottleneck before editing:
   - Eager composition or unbounded layout.
   - Visible item composition, measure, or draw cost.
   - Bitmap upload or image decode cost.
   - State-read/recomposition scope.
   - Lazy item identity or content reuse mismatch.
   - Data transformation during composition.
4. Make the smallest root-cause fix at the right layer.
5. Re-run the same isolated action and compare metrics.

## Android Evidence

Prefer real-device evidence when the issue is Android-visible. Useful commands:

```bash
adb shell dumpsys gfxinfo <package> reset
adb shell input swipe 540 1800 540 520 700
adb shell dumpsys gfxinfo <package>
adb logcat -d -v time Choreographer:I OpenGLRenderer:I '*:S'
```

Read the metrics as signals:

- High skipped frames or Davey frames during page entry points toward one-time composition/layout work.
- High `Slow UI thread` points toward composition, measure, layout, or main-thread work.
- High `Slow issue draw commands` points toward drawing cost in visible content.
- High `Slow bitmap uploads` points toward image/resource upload.
- P95/P99 spikes matter more than average frame time for user-visible jank.

Use debug builds for triage only. For final performance claims, prefer release-mode, R8-enabled, real-device verification when practical.

## Decision Table

| Evidence | Likely Cause | Preferred Fix Shape |
| --- | --- | --- |
| Slow entry, all rows appear in hierarchy, full data rendered at once | Eager full-list composition | Make the page own a finite `LazyColumn`; put header, tabs, list rows, and footers in one lazy DSL |
| Lazy list inside same-direction `verticalScroll` or unbounded parent | Invalid scroll ownership | Move scroll ownership to one container; keep lazy list under finite constraints |
| Mixed headers, grids, rows, and empty states reuse poorly | Lazy composition reuse mismatch | Add stable keys and `contentType` for structurally similar item groups |
| Scroll jank with high draw-command cost | Visible row drawing too expensive | Inspect shared row components for shadows, clips, gradients, surfaces, glyphs, and decorative drawing |
| State changes mark many rows dirty | State read scope too broad | Read fast-changing state lower in the tree, use stable IDs, and avoid list-wide derived work in composition |
| First image display stutters or bitmap uploads are high | Image decode/upload bottleneck | Reduce resource size, avoid synchronous large local image loads, and limit image-heavy first composition |
| Filtering/sorting/section switch stutters | Data transformation in composition | Move transformation to state/use case or memoize with the correct keys |
| Stability report shows unstable hot parameters | Skipping blocked by unstable inputs | Fix model stability or collection boundaries only where reports show it matters |

## Compose Checks

- Prefer one lazy container per vertical scroll surface.
- Do not put many real UI elements inside one lazy item; item-level laziness is the unit of composition and reuse.
- Use stable keys for identity, not as a cure for expensive rows.
- Use `contentType` when a lazy list mixes item structures.
- Treat `remember` as a scoped cache, not a performance bandage.
- Read scroll, animation, progress, or frequently changing state as late as possible when it only affects a small visual area.
- Check shared components before cloning a lightweight local-only row. Optimize the shared dense/list mode when the shared component is the measured bottleneck.

## Anti-Patterns

- Do not truncate real data to hide list cost unless the product explicitly calls for pagination or preview limits.
- Do not add `key`, `contentType`, `remember`, or `@Stable` without matching evidence.
- Do not write a fixed rule such as "remove shadows from lists." Instead, measure whether draw cost is the bottleneck and then reduce dense item drawing.
- Do not move platform scanning, playback, or repository work into UI just to avoid a UI performance issue.
- Do not claim success without rerunning the same isolated action.

## References

- Read `references/kmp-music-case-study.md` when the task benefits from the concrete Local Music two-stage jank investigation and verification data.
