# Classic Tiles (Piano Tiles-style game for Android)

Tap the black tiles; each tap plays the next note of a public-domain classical piece.
16 pieces (Bach, Mozart, Beethoven, Haydn, Brahms, Pachelbel, Grieg, ...). Notes are synthesised in-app.

## Build without Android Studio
1. Create an empty GitHub repo and push this whole folder to it (make sure the hidden `.github/` folder is included).
2. Open the repo's **Actions** tab -> "Build APK" runs automatically on every push (or press "Run workflow").
3. When it's green, download `ClassicTiles.apk` from the repo's **Releases** page ("Latest build"),
   or from the run's **Artifacts** (zipped).
4. Open the APK on your phone and allow "Install unknown apps" for your browser/file manager when asked.

Add songs: edit `app/src/main/java/com/example/pianotiles/Songs.kt`.
