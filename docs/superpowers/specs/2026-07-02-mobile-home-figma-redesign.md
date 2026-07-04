# Mobile Home Figma Redesign

## Goal

Redesign the production mobile home screen to match the provided Figma frame one-to-one while preserving the existing global mini player and bottom tab behavior.

## Source Of Truth

- Figma: https://www.figma.com/design/N3Uno2XyvEz5uBj9uE8CJf/Untitled?node-id=871-477&m=dev
- Required fidelity: one-to-one visual replication.
- Current access status: `figmaDesktop` MCP can read node `871:477`, including design context, metadata, screenshot, and localhost asset URLs. Command-line URL access still returns CloudFront 403, so use the MCP output as the implementation source.

## Captured Figma Facts

- Frame: `390 x 884`.
- Header: top app bar at `0,0`, size `390 x 64`.
- Main content: starts at `y=80`, uses horizontal page padding `20`.
- Filter chips: container `350 x 48`, chip height `32`, horizontal gap `8`.
- Dynamic label row: `本地音乐` at `x=20`, stats text at the trailing edge.
- Song list: starts at `y=200`; each song item is `350 x 82`, with `12px` vertical list gap.
- Cover art: `56 x 56`, radius `12`, positioned `13px` from the song item top/left.
- Active row: light mint background with teal border and playing badge over the cover.
- Figma variables: none returned for this node; implementation should map raw colors and sizes into local Compose tokens/components.

## Hard Constraints

- Change the production KMP App, not `prototypes/kmp-music-hi-fi`.
- Do not modify, remove, or duplicate the global mini player.
- Do not modify, remove, or duplicate the bottom tab.
- Keep the home screen as a top-level mobile page under the existing app chrome.

## Open Design Decisions

- Resolved: node `871:477` replaces the production mobile home body.
- Resolved: the first implementation only completes the `歌曲` state.
- Resolved: the `歌曲` list uses the same full local song data source as the existing local music page Songs tab. If full songs are not loaded yet, the UI may briefly fall back to the home preview until the controller loads the local library.
- Pending: `专辑`, `歌手`, and `文件夹` chips are visual placeholders for this slice and should receive their own scoped implementation later.
