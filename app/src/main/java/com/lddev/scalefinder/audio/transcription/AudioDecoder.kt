package com.lddev.scalefinder.audio.transcription

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.coroutineContext

class AudioDecoder(private val context: Context) {

    companion object {
        const val TARGET_SAMPLE_RATE = 22050
        private const val TIMEOUT_US = 10_000L
    }

    data class DecodedAudio(
        val samples: FloatArray,
        val sampleRate: Int,
        val durationMs: Long
    )

    suspend fun decode(uri: Uri): DecodedAudio = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            val audioTrackIndex = findAudioTrack(extractor)
            extractor.selectTrack(audioTrackIndex)

            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: throw IllegalArgumentException("No MIME type for audio track")
            val inputSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val inputChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            val codec = MediaCodec.createDecoderByType(mime)
            try {
                codec.configure(format, null, null, 0)
                codec.start()

                val pcmData = decodeLoop(codec, extractor, inputChannelCount)

                var samples = pcmData.toFloatArray()
                if (inputSampleRate != TARGET_SAMPLE_RATE) {
                    samples = resample(samples, inputSampleRate, TARGET_SAMPLE_RATE)
                }

                val durationMs = (samples.size * 1000L) / TARGET_SAMPLE_RATE
                DecodedAudio(samples, TARGET_SAMPLE_RATE, durationMs)
            } finally {
                codec.stop()
                codec.release()
            }
        } finally {
            extractor.release()
        }
    }

    private suspend fun decodeLoop(
        codec: MediaCodec,
        extractor: MediaExtractor,
        channelCount: Int
    ): MutableList<Float> {
        val pcmData = mutableListOf<Float>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var outputChannelCount = channelCount
        var outputEncoding = AudioFormat.ENCODING_PCM_16BIT

        while (!outputDone && coroutineContext.isActive) {
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)!!
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(
                            inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(
                            inputIndex, 0, sampleSize, extractor.sampleTime, 0
                        )
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            when {
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val outFormat = codec.outputFormat
                    outputChannelCount =
                        outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    outputEncoding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        outFormat.getInteger(
                            MediaFormat.KEY_PCM_ENCODING,
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                    } else {
                        AudioFormat.ENCODING_PCM_16BIT
                    }
                }

                outputIndex >= 0 -> {
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                    if (bufferInfo.size > 0) {
                        val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        extractSamples(outputBuffer, outputEncoding, outputChannelCount, pcmData)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }
        }
        return pcmData
    }

    private fun extractSamples(
        buffer: ByteBuffer,
        encoding: Int,
        channelCount: Int,
        output: MutableList<Float>
    ) {
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                val floatBuf = buffer.asFloatBuffer()
                while (floatBuf.hasRemaining()) {
                    var mono = 0f
                    for (ch in 0 until channelCount) {
                        mono += if (floatBuf.hasRemaining()) floatBuf.get() else 0f
                    }
                    output.add(mono / channelCount)
                }
            }
            else -> {
                val shortBuf = buffer.asShortBuffer()
                while (shortBuf.hasRemaining()) {
                    var mono = 0f
                    for (ch in 0 until channelCount) {
                        mono += if (shortBuf.hasRemaining()) shortBuf.get().toFloat() else 0f
                    }
                    output.add(mono / channelCount / 32768f)
                }
            }
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) return i
        }
        throw IllegalArgumentException("No audio track found in file")
    }

    private fun resample(input: FloatArray, fromRate: Int, toRate: Int): FloatArray {
        if (fromRate == toRate) return input
        val ratio = fromRate.toDouble() / toRate
        val outputSize = (input.size / ratio).toInt()
        val output = FloatArray(outputSize)
        for (i in output.indices) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt()
            val frac = (srcPos - srcIndex).toFloat()
            output[i] = if (srcIndex + 1 < input.size) {
                input[srcIndex] * (1f - frac) + input[srcIndex + 1] * frac
            } else {
                input[srcIndex.coerceAtMost(input.lastIndex)]
            }
        }
        return output
    }
}
