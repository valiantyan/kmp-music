# Local audio discovery depends on explicit access models

The user already understands Android media permissions and `MediaStore` well enough to use it as the comparison point. Future lessons can start from the corrected model that iOS does not provide Android-style whole-device audio scanning: iOS MVP should use user-selected/imported audio files, with MediaPlayer library scanning as an optional later path, while Desktop should use user-selected folders and recursive JVM file scanning.
