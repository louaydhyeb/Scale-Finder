package com.lddev.scalefinder.audio.engine

abstract class Dsp(
    protected val sampleRate: Int = 44100
) {
    abstract fun compute(): Float
}

abstract class DspSource : Dsp() {
    open fun noteOn(freq: Double) {}
    open fun noteOff() {}
}

abstract class DspEffect(private val input: Dsp) : Dsp() {
    protected fun next() = input.compute()
}
