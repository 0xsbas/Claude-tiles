package com.example.pianotiles

/** One note: MIDI pitch + length in beats (only used to decide how long it rings). */
class Note(val midi: Int, val dur: Float)

/**
 * Notation: space-separated tokens like "C4", "F#5", "Bb3", optional ":beats" (default 1).
 * `unit` scales every duration in the song.
 */
class Song(val title: String, val composer: String, src: String, unit: Float = 1f) {
    val notes: List<Note> = parseNotes(src, unit)
}

private fun parseNotes(src: String, unit: Float): List<Note> {
    val semis = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)
    return src.trim().split(Regex("\\s+")).mapNotNull { tok ->
        try {
            val parts = tok.split(":")
            val name = parts[0]
            var semi = semis.getValue(name[0])
            var i = 1
            if (name[i] == '#') {
                semi++; i++
            } else if (name[i] == 'b') {
                semi--; i++
            }
            val octave = name.substring(i).toInt()
            val beats = if (parts.size > 1) parts[1].toFloat() else 1f
            Note(12 * (octave + 1) + semi, beats * unit)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Simplified arrangements of public-domain melodies (all composers died long ago;
 * the notes are synthesised in-app, so no recordings are involved).
 * To add a song: append a Song(...) to this list.
 */
val SONGS: List<Song> by lazy {
    val prelude = "C4 E4 G4 C5 E5 G4 C5 E5  C4 D4 A4 D5 F5 A4 D5 F5  B3 D4 G4 D5 F5 G4 D5 F5  " +
        "C4 E4 G4 C5 E5 G4 C5 E5  C4 E4 A4 E5 A5 A4 E5 A5  C4 D4 F#4 A4 D5 F#4 A4 D5  " +
        "B3 D4 G4 D5 G5 G4 D5 G5  B3 C4 E4 G4 C5 E4 G4 C5  C4 E4 G4 C5 E5:6"

    val toccata = "A5 G5 A5:6 G5 F5 E5 D5 C#5:2 D5:6  A4 G4 A4:6 G4 F4 E4 D4 C#4:2 D4:6  " +
        "A3 G3 A3:6 G3 F3 E3 D3 C#3:2 D3:8"

    val cello = listOf(
        "G2 D3 B3 A3 B3 D3 B3 D3", "G2 E3 C4 B3 C4 E3 C4 E3",
        "A2 F#3 C4 B3 C4 F#3 C4 F#3", "B2 G3 D4 C4 D4 G3 D4 G3"
    ).joinToString(" ") { "$it $it" } + " G3:6"

    val minuet = "D5 G4:0.5 A4:0.5 B4:0.5 C5:0.5 D5 G4 G4  E5 C5:0.5 D5:0.5 E5:0.5 F#5:0.5 G5 G4 G4  " +
        "C5 D5:0.5 C5:0.5 B4:0.5 A4:0.5 B4 C5:0.5 B4:0.5 A4:0.5 G4:0.5  " +
        "F#4 G4:0.5 A4:0.5 B4:0.5 G4:0.5 A4:3"

    val turkishA = "B4:0.5 A4:0.5 G#4:0.5 A4:0.5 C5:2  D5:0.5 C5:0.5 B4:0.5 C5:0.5 E5:2  " +
        "F5:0.5 E5:0.5 D#5:0.5 E5:0.5 B5:0.5 A5:0.5 G#5:0.5 A5:0.5 B5:0.5 A5:0.5 G#5:0.5 A5:0.5 C6:2 A5:3"

    val nachtmusik = "G4:0.5 D4:0.5 G4:0.5 D4:0.5 G4:0.5 D4:0.5 G4:0.5 B4:0.5 D5:2  " +
        "C5:0.5 A4:0.5 C5:0.5 A4:0.5 C5:0.5 A4:0.5 F#4:0.5 A4:0.5 C5:2  " +
        "B4:0.5 G4:0.5 B4:0.5 G4:0.5 B4:0.5 G4:0.5 D4:0.5 G4:0.5 B4:2  " +
        "G4:0.5 D4:0.5 G4:0.5 D4:0.5 G4:0.5 D4:0.5 G4:0.5 B4:0.5 D5:2 B4 G4:3"

    val twinkle = "C4 C4 G4 G4 A4 A4 G4:2  F4 F4 E4 E4 D4 D4 C4:2  G4 G4 F4 F4 E4 E4 D4:2  " +
        "G4 G4 F4 F4 E4 E4 D4:2  C4 C4 G4 G4 A4 A4 G4:2  F4 F4 E4 E4 D4 D4 C4:2"

    val ode = "E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 E4:1.5 D4:0.5 D4:2  " +
        "E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 D4:1.5 C4:0.5 C4:2  " +
        "D4 D4 E4 C4 D4 E4:0.5 F4:0.5 E4 C4 D4 E4:0.5 F4:0.5 E4 D4 C4 D4 G3:2  " +
        "E4 E4 F4 G4 G4 F4 E4 D4 C4 C4 D4 E4 D4:1.5 C4:0.5 C4:3"

    val elise = "E5:0.5 D#5:0.5 E5:0.5 D#5:0.5 E5:0.5 B4:0.5 D5:0.5 C5:0.5 A4:2  " +
        "C4:0.5 E4:0.5 A4:0.5 B4:2  E4:0.5 G#4:0.5 B4:0.5 C5:2  " +
        "E4:0.5 E5:0.5 D#5:0.5 E5:0.5 D#5:0.5 E5:0.5 B4:0.5 D5:0.5 C5:0.5 A4:2  " +
        "C4:0.5 E4:0.5 A4:0.5 B4:2  E4:0.5 C5:0.5 B4:0.5 A4:3"

    val canon = "F#5 E5 D5 C#5 B4 A4 B4 C#5 D5 C#5 B4 A4 G4 F#4 G4 E4"
    val canonFull = canon.split(" ").joinToString(" ") { "$it:2" } + "  " +
        canon.split(" ").joinToString(" ") { "$it $it" }

    val haydn = "C4 C4 E4 E4 G4 G4 E4:2  F4 F4 D4 D4 B3 B3 G3:2"

    val brahms = "E4:0.5 E4:0.5 G4:2  E4:0.5 E4:0.5 G4:2  E4:0.5 G4:0.5 C5:1 B4:0.5 A4:0.5 A4:0.5 G4:2  " +
        "D4:0.5 E4:0.5 F4:1.5 D4:0.5 D4:0.5 E4:0.5 F4:2  D4:0.5 F4:0.5 B4:1 A4:0.5 G4:0.5 B4:0.5 C5:2.5"

    val king = "B3 C#4 D4 E4 F#4 D4 F#4:2  F4 C#4 F4:2  B3 C#4 D4 E4 F#4 D4 F#4:2  B4 F#4 D4 F#4 B4:2  " +
        "B4 C#5 D5 E5 F#5 D5 F#5:2  F5 C#5 F5:2  B4 C#5 D5 E5 F#5 D5 F#5:2  B5:4"

    val silent = "G4:1.5 A4:0.5 G4 E4:3  G4:1.5 A4:0.5 G4 E4:3  D5:2 D5 B4:3  C5:2 C5 G4:3  " +
        "A4:2 A4 C5:1.5 B4:0.5 A4  G4:1.5 A4:0.5 G4 E4:3  A4:2 A4 C5:1.5 B4:0.5 A4  G4:1.5 A4:0.5 G4 E4:3  " +
        "D5:2 D5 F5:1.5 D5:0.5 B4  C5:3 E5:3  C5 G4 E4  G4:1.5 F4:0.5 D4  C4:3"

    val greensleeves = "A4  C5:2 D5  E5:1.5 F5:0.5 E5  D5:2 B4  G4:1.5 A4:0.5 B4  C5:2 A4  " +
        "A4:1.5 G#4:0.5 A4  B4:2 G#4  E4:2 A4  C5:2 D5  E5:1.5 F5:0.5 E5  D5:2 B4  " +
        "G4:1.5 A4:0.5 B4  C5:1.5 B4:0.5 A4  G#4:1.5 F#4:0.5 G#4  A4:3"

    val grace = "D4  G4:2 B4:0.5 G4:0.5  B4:2 A4  G4:2 E4  D4:2 D4  G4:2 B4:0.5 G4:0.5  B4:2 A4  D5:3"

    listOf(
        Song("Prelude in C major", "J.S. Bach", prelude, 0.5f),
        Song("Toccata in D minor", "J.S. Bach", toccata, 0.5f),
        Song("Cello Suite No. 1: Prelude", "J.S. Bach", cello, 0.5f),
        Song("Minuet in G", "Petzold (once credited to Bach)", minuet),
        Song("Rondo alla Turca", "W.A. Mozart", "$turkishA $turkishA", 1f),
        Song("Eine kleine Nachtmusik", "W.A. Mozart", nachtmusik),
        Song("Ah! vous dirai-je, Maman", "W.A. Mozart", twinkle),
        Song("Ode to Joy", "L. van Beethoven", ode),
        Song("Für Elise", "L. van Beethoven", elise),
        Song("Canon in D", "J. Pachelbel", canonFull),
        Song("Surprise Symphony", "J. Haydn", "$haydn $haydn C4:3"),
        Song("Lullaby (Wiegenlied)", "J. Brahms", brahms),
        Song("In the Hall of the Mountain King", "E. Grieg", king, 0.5f),
        Song("Silent Night", "F. Gruber", silent),
        Song("Greensleeves", "Traditional", greensleeves),
        Song("Amazing Grace", "Traditional", grace)
    )
}
