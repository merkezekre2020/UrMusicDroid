package com.example.lyricsplayer.data.model

data class Lyrics(
    val plainLyrics: String?,
    val syncedLyrics: List<LyricsLine>?
) {
    companion object {
        /**
         * Parse LRC format synced lyrics string into list of LyricsLine.
         * Format: [mm:ss.xx] lyric text
         */
        fun parseSyncedLyrics(syncedLyricsText: String?): Lyrics? {
            if (syncedLyricsText.isNullOrBlank()) return null
            
            val lines = syncedLyricsText.lines()
                .filter { it.isNotBlank() }
            
            val syncedLines = mutableListOf<LyricsLine>()
            val plainLines = mutableListOf<String>()
            
            val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})]""")
            
            for (line in lines) {
                val match = timeRegex.find(line)
                if (match != null) {
                    val minutes = match.groupValues[1].toLong()
                    val seconds = match.groupValues[2].toLong()
                    val millisStr = match.groupValues[3]
                    val millis = if (millisStr.length == 2) {
                        millisStr.toLong() * 10
                    } else {
                        millisStr.toLong()
                    }
                    
                    val timeMs = minutes * 60000 + seconds * 1000 + millis
                    val text = line.substring(match.range.last + 1).trim()
                    
                    if (text.isNotEmpty()) {
                        syncedLines.add(LyricsLine(timeMs, text))
                        plainLines.add(text)
                    }
                }
            }
            
            val plainLyrics = if (plainLines.isNotEmpty()) plainLines.joinToString("\n") else null
            val synced = if (syncedLines.isNotEmpty()) syncedLines else null
            
            return Lyrics(plainLyrics, synced)
        }
    }
}
