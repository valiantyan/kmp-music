# Mobile Favorites Figma Redesign

## Goal

Redesign the production mobile favorites root page to match the provided Figma frame one-to-one while preserving the existing global mini player, bottom tab, playback queue, favorite toggle, more menu, and contextual search behavior.

## Source Of Truth

- Figma: https://www.figma.com/design/N3Uno2XyvEz5uBj9uE8CJf/Untitled?node-id=899-1147&m=dev
- Required fidelity: one-to-one visual replication for the default favorite songs view.
- Captured through `figmaDesktop` MCP node `899:1147`, including design context, metadata, screenshot, and asset references.

## Captured Figma Facts

- Frame: `397 x 812`.
- Page background: `#F8FAFB`.
- Fixed top app bar: `x=0`, `y=0`, `397 x 64`, translucent `#F8FAFB`, title `收藏`, search icon at the right.
- Main content starts at `y=80` with horizontal padding `20`.
- Action header: `357 x 44`, primary button `156.59 x 44`, radius `16`, color `#26A69A`, text `播放全部 (5)`.
- Song list starts at `y=148`.
- Song item: `357 x 80`, white background, radius `24`, padding `12`, row gap `16`.
- Cover art: `56 x 56`, radius `16`.
- Text block starts at `x=84`; title uses `14 / 16`, artist uses `14 / 20`.
- Row actions occupy `88 x 40`: favorite heart `40 x 40`, more button `40 x 40`, gap `8`.
- Figma variables: none returned for this node.

## Hard Constraints

- Change the production KMP App, not `prototypes/kmp-music-hi-fi`.
- Do not duplicate or modify the global mini player.
- Do not duplicate or modify the bottom tab.
- Preserve current playing song red text and playback auxiliary marker where global rules require it.
- Preserve contextual favorite search by opening `SearchContext.Favorites` from the top search icon.

## Decisions

- The default `FavoriteSection.Songs` state is the one-to-one Figma target.
- The old visible `FilterChip` row is removed because it does not exist in the Figma frame.
- The Figma right-side filter/sort icon is mapped to the existing favorite section switcher so albums and artists remain reachable without adding non-Figma controls.
- Empty states are not present in Figma, so they use the same card geometry and background rhythm instead of leaving a blank page.

## Verification

- Android Kotlin compile must pass after the visual change.
- Desktop tests should still pass because shared routing and favorite projection stay intact.
- Visual verification should compare the default favorite songs view against node `899:1147`: top bar, action row, first five song rows, 20dp horizontal padding, 16dp row gap, and global chrome spacing.
