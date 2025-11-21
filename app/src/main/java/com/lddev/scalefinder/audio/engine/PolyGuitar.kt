package com.lddev.scalefinder.audio.engine

class PolyGuitar(
    private val voiceCount: Int = 8,
    sampleRate: Int = 44100
) {
    private val voices = List(voiceCount) { GuitarKarplusStrong() }

    fun getFreeVoice(): GuitarKarplusStrong? {
        return voices.firstOrNull { !it.isActive() }
    }

    fun noteOn(freq: Double) {
        val voice = getFreeVoice() ?: voices.random() // steal oldest
        voice.noteOn(freq)
    }

    fun addToEngine(engine: AudioEngine) {
        voices.forEach { engine.addDsp(it) }
    }
}
