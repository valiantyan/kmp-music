# Home UI Prototype Design

## Goal

Explore a calmer mobile home screen for KMP Music. The app is a local music player whose audio source comes from scanning on-device files, but the current home screen gives too much space to library statistics and repeats song lists across sections.

## Question

What should the home screen look like if the primary task is "continue listening" while scanned local library data remains visible and trustworthy?

## Prototype Scope

- Build three throwaway visual variants in the existing high-fidelity prototype.
- Keep the real mobile constraints visible: mini player, bottom tab bar, local songs, albums, and scan entry.
- Do not change production KMP code.
- Do not treat the prototype implementation as production-ready code.

## Variants

### A1 Continue Listening First

Lead with the current song and recent listening context. Show scanned library data as a compact status strip with song, album, and artist counts plus scan/folder actions.

### A2 Compressed Library Dashboard

Keep the scanned local library as the top signal, but reduce it from a large hero card into a compact dashboard with immediate entry points for songs, albums, artists, folders, and rescan.

### A3 Balanced Home

Keep the first screen split between continue listening and local library status. Show only three recent songs and move deeper lists to secondary pages.

## Success Criteria

- The first screen feels less crowded than the current screenshot.
- Scanned local library data remains visible on the home screen.
- Recent playback and local library no longer compete as duplicate full lists.
- The chosen direction can later be translated into Compose by editing shared home components rather than patching individual rows.
