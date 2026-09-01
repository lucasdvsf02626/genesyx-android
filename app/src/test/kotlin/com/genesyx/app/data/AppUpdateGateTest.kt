package com.genesyx.app.data

import com.genesyx.app.core.result.DataResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The minimum-version gate's decision table. The gate only ever blocks on a confirmed, well-formed
 * `min_supported_build` above the running build; every failure mode must fail OPEN.
 */
class AppUpdateGateTest {

    // ── Ordered comparisons ────────────────────────────────────────────────────────────────

    @Test
    fun `a build below the minimum is told to update`() {
        assertTrue(isUpdateRequired(21, DataResult.Success("22")))
    }

    @Test
    fun `a build equal to the minimum may continue`() {
        assertFalse(isUpdateRequired(21, DataResult.Success("21")))
    }

    @Test
    fun `a build above the minimum may continue`() {
        assertFalse(isUpdateRequired(22, DataResult.Success("21")))
    }

    // ── Fail-open paths ────────────────────────────────────────────────────────────────────

    @Test
    fun `a missing row allows the launch`() {
        assertFalse(isUpdateRequired(21, DataResult.Success(null)))
    }

    @Test
    fun `a malformed value allows the launch`() {
        assertFalse(isUpdateRequired(21, DataResult.Success("not-a-number")))
    }

    @Test
    fun `a blank value allows the launch`() {
        assertFalse(isUpdateRequired(21, DataResult.Success("")))
    }

    @Test
    fun `a transport error allows the launch`() {
        assertFalse(isUpdateRequired(21, DataResult.Error(IOException("offline"))))
    }

    @Test
    fun `a missing table allows the launch`() {
        // PostgREST 404 surfaces as an Error result like any other failure.
        assertFalse(isUpdateRequired(21, DataResult.Error(RuntimeException("relation not found"))))
    }

    @Test
    fun `loading allows the launch`() {
        assertFalse(isUpdateRequired(21, DataResult.Loading))
    }

    // ── Lenient parsing ────────────────────────────────────────────────────────────────────

    @Test
    fun `a whitespace-padded value still parses`() {
        assertTrue(isUpdateRequired(21, DataResult.Success(" 22 ")))
    }
}
