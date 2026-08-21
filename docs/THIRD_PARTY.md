# Third-party runtime notice

The Windows development package currently includes an FFmpeg 8.1.1 full build from gyan.dev. That binary reports GPLv3 configuration and includes libmp3lame. Before distributing Resonance outside local development, ship the corresponding FFmpeg license and complete corresponding source information alongside the binary, or replace it with a deliberately selected compliant build.

Resonance invokes FFmpeg as a separate process for audio conversion; source audio is never overwritten or deleted.

# LRCLIB

Resonance uses the public read-only [LRCLIB API](https://lrclib.net/docs) to automatically match plain and synchronized lyrics. Requests identify the client as required by LRCLIB and include only track title, artist, album, and duration. Audio files, artwork, playlists, and local paths are not uploaded. Results are cached locally to reduce repeated requests and service load.

The LRCLIB server implementation is available under the MIT license. Lyrics content can remain subject to its respective copyright holders and service policies; Resonance does not publish or edit lyrics through the API.
# FFmpegKit Maintained / FFmpeg

The Android application uses the LGPL `ffmpeg-kit-audio` package to transcode locally decrypted FLAC/Ogg audio to 320 kbps MP3. FFmpegKit and FFmpeg retain their respective upstream licenses; the GPL package variants are not used.
