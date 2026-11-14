package com.voicerewriter

import android.os.Debug
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Runs real whisper.cpp / sherpa-onnx inference on the device, never cloud APIs.
 *
 * Import a private fixture directory into targetContext.filesDir/benchmark/:
 *   fixtures.tsv  -> id<TAB>language<TAB>verified reference transcript
 *   <id>.wav      -> mono 16-kHz PCM16, fixed 44-byte WAV header (WavIo format)
 *
 * A single test emits results.jsonl and a summary.json in the same directory.
 * Use instrumentation arguments "models" (comma separated; default v2/v3 and
 * Whisper base) and "repeats" (>=1; default 2). No recordings or transcripts
 * are logged or uploaded.
 */
@RunWith(AndroidJUnit4::class)
class OnDeviceBenchmarkTest {
    @Test fun benchmark() {
        val registry = InstrumentationRegistry.getInstrumentation()
        val context = registry.targetContext
        val args = InstrumentationRegistry.getArguments()
        val requestedModels = args.getString("models", "parakeet,parakeet-v3,base")!!
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val repeats = (args.getString("repeats", "2")!!.toInt()).also {
            require(it in 1..10) { "repeats must be 1..10" }
        }
        val root = File(context.filesDir, "benchmark")
        val manifest = File(root, "fixtures.tsv")
        assertTrue("Missing benchmark fixtures at ${manifest.absolutePath}", manifest.isFile)
        val fixtures = manifest.readLines().map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val fields = line.split("\t", limit = 3)
                require(fields.size == 3) { "Each fixture needs id<TAB>language<TAB>reference" }
                val (id, language, reference) = fields
                require(id.matches(Regex("[A-Za-z0-9_-]+"))) { "Unsafe fixture ID: $id" }
                require(reference.isNotBlank()) { "Empty reference in fixture $id" }
                require(language in listOf("pt", "es", "en", DictationLanguage.AUTO)) {
                    "Unsupported evaluation language: $language"
                }
                val audio = File(root, "$id.wav")
                require(audio.isFile) { "Missing audio for $id" }
                Fixture(id, language, reference, audio)
            }
        require(fixtures.isNotEmpty()) { "No evaluation fixtures provided" }
        val approved = setOf(ParakeetModelManager.MODEL_ID,
            ParakeetV3ModelManager.MODEL_ID) +
            WhisperModelManager.MODELS.map { it.id }
        require(requestedModels.isNotEmpty() && requestedModels.all { it in approved }) {
            "Unknown model in benchmark arguments"
        }
        val output = File(root, "results.jsonl")
        val records = mutableListOf<Result>()
        output.bufferedWriter().use { writer ->
            for (model in requestedModels) {
                for (fixture in fixtures) {
                    val resolved = OnDeviceStt.resolveModel(context, model, fixture.language)
                    // Parakeet v2 is English only; skip unsupported languages rather
                    // than secretly benchmarking Whisper under a Parakeet label.
                    if (resolved != model) continue
                    check(OnDeviceStt.isReady(context, model, fixture.language)) {
                        "Model '$model' unavailable. Download it in Settings before running benchmark"
                    }
                    val samples = requireNotNull(WavIo.read(fixture.audio)) {
                        "Invalid WAV for ${fixture.id}; needs 16-kHz mono PCM16"
                    }
                    val durationSec = samples.size / 16000.0
                    require(durationSec > 0.0) { "Empty WAV ${fixture.id}" }
                    val floats = WavIo.toFloats(samples)
                    for (run in 1..repeats) {
                        val nativeBefore = Debug.getNativeHeapAllocatedSize()
                        val memoryBefore = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss
                        val startNs = SystemClock.elapsedRealtimeNanos()
                        var outputText = ""
                        var failure: String? = null
                        try {
                            outputText = runBlocking {
                                OnDeviceStt.transcribe(context,
                                    Settings(sttProvider = "local", sttModel = model,
                                        sttLanguage = fixture.language,
                                        parakeetHotwordsExperimental = false),
                                    floats)
                            }
                        } catch (e: Exception) {
                            failure = e.javaClass.simpleName + ": " + (e.message ?: "")
                        }
                        val latencyMs = (SystemClock.elapsedRealtimeNanos() - startNs) / 1_000_000.0
                        val nativeAfter = Debug.getNativeHeapAllocatedSize()
                        val memoryAfter = Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss
                        val rec = Result(
                            model, fixture, run, latencyMs, durationSec,
                            if (failure == null) BenchmarkMetrics.wer(fixture.reference, outputText) else null,
                            if (failure == null) BenchmarkMetrics.cer(fixture.reference, outputText) else null,
                            outputText.isBlank(), nativeBefore, nativeAfter,
                            memoryBefore, memoryAfter, failure
                        )
                        records.add(rec)
                        writer.write(rec.asJson().toString())
                        writer.newLine()
                        writer.flush()
                    }
                }
            }
        }
        val summary = JSONObject()
        summary.put("schemaVersion", 1)
        summary.put("deviceModel", android.os.Build.MODEL)
        summary.put("sdk", android.os.Build.VERSION.SDK_INT)
        summary.put("note", "PSS/native heap are before/after snapshots, not peak memory.")
        for (model in requestedModels) {
            val modelRecords = records.filter { it.model == model }
            val done = modelRecords.filter { it.error == null }
            val item = JSONObject()
            item.put("runs", modelRecords.size)
            item.put("failures", modelRecords.count { it.error != null })
            item.put("emptyOutputs", modelRecords.count { it.empty })
            item.put("p50LatencyMs", done.takeIf { it.isNotEmpty() }?.let { BenchmarkMetrics.percentile(it.map { r -> r.latencyMs }, 50) } ?: JSONObject.NULL)
            item.put("p95LatencyMs", done.takeIf { it.isNotEmpty() }?.let { BenchmarkMetrics.percentile(it.map { r -> r.latencyMs }, 95) } ?: JSONObject.NULL)
            item.put("meanWer", done.mapNotNull { it.wer }.takeIf { it.isNotEmpty() }?.average() ?: JSONObject.NULL)
            item.put("meanCer", done.mapNotNull { it.cer }.takeIf { it.isNotEmpty() }?.average() ?: JSONObject.NULL)
            summary.put(model, item)
        }
        File(root, "summary.json").writeText(summary.toString(2))
        assertTrue("Benchmark recorded inference failures; inspect results.jsonl", records.none { it.error != null })
    }

    private data class Fixture(val id: String, val language: String, val reference: String, val audio: File)

    private data class Result(
        val model: String,
        val fixture: Fixture,
        val run: Int,
        val latencyMs: Double,
        val audioSeconds: Double,
        val wer: Double?,
        val cer: Double?,
        val empty: Boolean,
        val nativeHeapBefore: Long,
        val nativeHeapAfter: Long,
        val pssBeforeKb: Int,
        val pssAfterKb: Int,
        val error: String?,
    ) {
        fun asJson(): JSONObject = JSONObject().apply {
            put("model", model)
            put("fixtureId", fixture.id)
            put("language", fixture.language)
            put("run", run)
            put("latencyMs", latencyMs)
            put("audioSeconds", audioSeconds)
            put("realTimeFactor", latencyMs / (audioSeconds * 1000))
            put("wer", wer ?: JSONObject.NULL)
            put("cer", cer ?: JSONObject.NULL)
            put("empty", empty)
            put("nativeHeapBeforeBytes", nativeHeapBefore)
            put("nativeHeapAfterBytes", nativeHeapAfter)
            put("pssBeforeKb", pssBeforeKb)
            put("pssAfterKb", pssAfterKb)
            put("error", error ?: JSONObject.NULL)
        }
    }
}
