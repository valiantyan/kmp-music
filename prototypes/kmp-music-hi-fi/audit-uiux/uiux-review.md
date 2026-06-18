# KMP Music UI/UX Review

Date: 2026-06-18
Scope: current high-fidelity local prototype at `http://127.0.0.1:4173/`.
Mode: combined UX and accessibility review.

## Evidence

- Step 1 screenshot: `01-home.png`
- Step 2 observed in Chrome: search results page.
- Step 3 observed in Chrome: album detail page for `The Best of Me`.
- Step 4 observed in Chrome: playback queue bottom sheet.

## Flow Steps

1. Home
   Health: strong visual direction, but bottom chrome can obscure content in shorter desktop preview heights.

2. Search
   Health: usable and scannable, but long mixed results need clearer structure and safer bottom spacing.

3. Album Detail
   Health: visually calm and understandable, but content density and metadata completeness feel thin.

4. Playback Queue
   Health: useful quick action, but modal accessibility and destructive remove affordances need tightening.

## Strengths

1. The selected home visual direction is coherent: mint local-library card, restrained shadows, realistic covers, and compact row rhythm fit a local music app.
2. The information hierarchy on home is easy to scan: library status, recent playback, local albums, mini player, and main tabs are visually separated.
3. Current-song state is now consistently visible in lists and queue, which supports orientation between page content and the mini player.
4. Top-level vs secondary navigation is conceptually clearer after hiding main tabs on secondary pages.
5. Most icon-only controls have accessible names in the accessibility tree, including `搜索`, `播放`, `更多`, `播放队列`, and `返回`.

## Findings

### P1: Fixed bottom chrome hides or crowds content in shorter viewports

Evidence: Step 1 home screenshot shows the mini player and bottom nav covering the lower part of the home content; the album area is partially visible behind/under the fixed controls. Step 2 search has a long list plus album/artist results, with the mini player sitting over the lower content.

Impact: Users may think the screen ends earlier than it does, or miss the last rows/cards. This is especially risky on shorter Android devices, split-screen, desktop preview, and browser UI with visible toolbars.

Recommendation: Treat the phone content as a scroll container with bottom padding equal to `mini-player + nav + safe-area + spacing` for top-level pages, and `mini-player + safe-area + spacing` for secondary pages. Validate at 360x740, 390x844, 430x932, and current desktop preview height.

### P1: Queue drawer does not isolate background interaction for assistive tech

Evidence: Step 4 accessibility tree exposes the album page controls and queue drawer content at the same time. The drawer has a visible close control and queue rows, but background controls remain in the tree.

Impact: Screen reader and keyboard users can wander behind the sheet. This makes the modal feel unreliable and can cause accidental page actions while the queue is open.

Recommendation: Add `aria-modal="true"`, move focus into the drawer on open, trap focus while open, close on Escape, restore focus to the queue button on close, and mark the underlying page inert while the drawer is active.

### P2: Search page mixes too many result types without enough grouping controls

Evidence: Step 2 shows songs first, then albums and artists in a continuous long page. Quick chips exist, but there is no result count, active filter state, or empty-state framing.

Impact: Search works as a demo, but the user has to infer whether they are browsing all music, filtered music, or recommendations. The lower album/artist results are easy to miss under the mini player.

Recommendation: Add a visible active filter state, result counts, and a clear empty state. Consider tabs or segmented controls for `歌曲 / 专辑 / 歌手` once a query is active.

### P2: Album detail promises more content than it shows

Evidence: Step 3 `The Best of Me` says `15 首`, but the visible track list only shows two rows.

Impact: This can reduce trust in the prototype because album metadata and actual content disagree.

Recommendation: Either populate enough realistic tracks to match the count, show a "仅显示最近导入" explanation, or change the count to match the visible demo data.

### P2: Queue removal uses the same visual language as close

Evidence: Step 4 uses a close `x` for the sheet and small `x` controls for removing tracks.

Impact: Users may confuse dismissing the sheet with removing a queue item, especially because both are near the right edge and visually quiet.

Recommendation: Use a more distinct remove affordance, such as a trash/minus icon, row swipe affordance, or undo toast after removal.

### P2: Motion state is not communicated to reduced-motion users

Evidence: Secondary-page bottom chrome relies on a 780ms downward motion to communicate hierarchy. No reduced-motion handling is visible from CSS.

Impact: Users with motion sensitivity may experience unnecessary movement; users who miss the animation may not understand why the primary nav disappears.

Recommendation: Add `@media (prefers-reduced-motion: reduce)` to shorten or remove the transform animation while preserving final layout states.

### P3: "More" and "All" actions are visually clear but semantically generic

Evidence: Step 1 home has `全部` for recent playback and `更多` for local albums. In the accessibility tree these are generic button names.

Impact: Screen reader users hear weak actions without context; sighted users can infer from placement, but labels still rely on surrounding headings.

Recommendation: Keep visible labels if desired, but set stronger accessible labels such as `查看全部最近播放` and `查看更多本地专辑`.

### P3: Current playing red is clear, but may imply error/destructive state

Evidence: Current playing song text is red across lists and queue.

Impact: Red can be read as error or danger in some interfaces. Since this was explicitly requested, it is acceptable, but it should be supported by a secondary cue.

Recommendation: Pair red with a tiny equalizer/playing glyph, pulse, or "正在播放" label in dense lists so the meaning is not color-only.

## Evidence Limits

- Screenshot evidence saved locally only for the home step because macOS screen capture from shell was blocked and Browser screenshot capture could not directly write to the workspace without a temp copy.
- Chrome visual and accessibility-tree observations were used for search, album detail, and queue states.
- This is not a full WCAG compliance review. Keyboard traversal order, focus trap behavior, reduced motion, and screen reader announcement behavior still need targeted testing.

## Recommended Fix Order

1. Fix bottom spacing and scroll containment so fixed chrome never covers content.
2. Add modal focus isolation and `aria-modal` behavior for queue and action sheets.
3. Tighten search structure with active filters, counts, and empty states.
4. Align album detail metadata with visible track count.
5. Add reduced-motion CSS for bottom chrome and sheet transitions.
