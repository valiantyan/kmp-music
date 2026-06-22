# Mobile playback scope is background and media surface

The current mobile playback scope is intentionally narrow but applies to both mobile platforms: Android should support background playback plus a media notification surface, and iOS should support background playback as a P0 requirement. iOS has no Android-style notification bar, so its optional system-facing media surface is `MPNowPlayingInfoCenter`; Bluetooth headset controls, lock-screen-specific polish, car integrations, and Desktop system integration are deferred.
