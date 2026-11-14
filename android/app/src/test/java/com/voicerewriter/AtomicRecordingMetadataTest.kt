package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException

class AtomicRecordingMetadataTest {
    private fun directory(): File =
        createTempDir(prefix = "pending-audio-atomic-").also { it.deleteOnExit() }

    @Test fun createsValidMetadataWithNoTemporaryFiles() {
        val folder = directory()
        val target = File(folder, "recording.json")
        assertTrue(AtomicRecordingMetadata.writeText(target, """{"id":"recording"}"""))
        assertEquals("""{"id":"recording"}""", target.readText())
        assertEquals(listOf("recording.json"), folder.listFiles()!!.map { it.name })
        folder.deleteRecursively()
    }

    @Test fun replacesOldMetadataWithoutTruncatingIt() {
        val folder = directory()
        val target = File(folder, "recording.json")
        target.writeText("""{"id":"recording","old":true}""")
        assertTrue(AtomicRecordingMetadata.writeText(target, """{"result":"Olá, mundo!"}"""))
        assertEquals("""{"result":"Olá, mundo!"}""", target.readText())
        folder.deleteRecursively()
    }

    @Test fun interruptedWriterKeepsOriginalMetadataUntouched() {
        val folder = directory()
        val target = File(folder, "recording.json")
        val original = """{"id":"recording","result":null}"""
        target.writeText(original)
        assertFalse(AtomicRecordingMetadata.replace(target) { output ->
            output.write("""{"result":"partial""".toByteArray())
            throw IOException("out of storage")
        })
        assertEquals(original, target.readText())
        assertEquals(listOf("recording.json"), folder.listFiles()!!.map { it.name })
        folder.deleteRecursively()
    }

    @Test fun metadataCommitFailureDoesNotDeleteOriginal() {
        val folder = directory()
        val target = File(folder, "recording.json")
        assertTrue(target.mkdir()) // rename cannot replace a nonempty directory
        File(target, "keep.txt").writeText("data")
        assertFalse(AtomicRecordingMetadata.writeText(target, """{"id":"recording"}"""))
        assertTrue(File(target, "keep.txt").isFile)
        assertEquals(listOf("recording.json"), folder.listFiles()!!.map { it.name })
        folder.deleteRecursively()
    }

    @Test fun abandonedTempFilesAreNotMistakenForCommittedRecordings() {
        val folder = directory()
        val target = File(folder, "recording.json")
        File(folder, "recording.json.abandoned.tmp").writeText("{")
        assertTrue(AtomicRecordingMetadata.writeText(target, """{"id":"recording"}"""))
        assertEquals(listOf("recording.json"),
            folder.listFiles()!!.filter { it.name.endsWith(".json") }.map { it.name })
        folder.deleteRecursively()
    }

    @Test fun inaccessibleParentCannotProduceSuccess() {
        val folder = directory()
        val notDirectory = File(folder, "not-a-directory")
        notDirectory.writeText("occupied")
        assertFalse(AtomicRecordingMetadata.writeText(
            File(notDirectory, "recording.json"), """{"id":"x"}"""))
        assertEquals("occupied", notDirectory.readText())
        folder.deleteRecursively()
    }
}
