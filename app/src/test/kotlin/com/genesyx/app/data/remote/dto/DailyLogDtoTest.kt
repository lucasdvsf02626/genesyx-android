package com.genesyx.app.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format guard for `daily_logs`, in two halves that pull against each other.
 *
 * **Half one — what must stay OFF the wire.** `sexual_activity` is `boolean NOT NULL DEFAULT false`,
 * so an unset value has to be omitted rather than sent as an explicit null, which would fail the
 * whole upsert. It is the only field left in this half; `food_groups` was here until 26 Aug 2026,
 * on a mistaken reading of `docs/schema.sql` — the column is live and it now belongs in half two.
 *
 * **Half two — fields that must stay ON the wire even when empty.** PostgREST builds `columns=` from
 * the JSON body, so a field dropped for equalling its default is a column that never gets written.
 * For anything the user can *clear*, that silently discards the clear and lets the stale server
 * value come back on the next pull. Observed on 1.4.2 (19): un-ticking the last supplement produced
 * `columns=user_id,date`, with `supplements` absent entirely. `@EncodeDefault` on the clearable
 * fields is what keeps them present, and these tests are the tripwire if it is ever removed.
 */
class DailyLogDtoTest {

    // Mirrors supabase-kt's serializer config: encodeDefaults off, unknown keys ignored. The
    // clearable fields must survive THIS config — that is the whole point of @EncodeDefault.
    private val json = Json { ignoreUnknownKeys = true }

    private fun dto(
        sexualActivity: Boolean? = null,
        foodGroups: List<String> = emptyList(),
    ) = DailyLogDto(
        userId = "user-a",
        date = "2026-08-10",
        waterMl = 500,
        sexualActivity = sexualActivity,
        foodGroups = foodGroups,
    )

    /** The exact key set PostgREST turns into `columns=` for this row. */
    private fun wireKeys(dto: DailyLogDto): Set<String> =
        json.encodeToJsonElement(DailyLogDto.serializer(), dto).jsonObject.keys

    @Test
    fun `an unrecorded intimacy field stays off the wire`() {
        assertFalse(json.encodeToString(dto()).contains("sexual_activity"))
    }

    @Test
    fun `a recorded intimacy field is sent`() {
        assertTrue(json.encodeToString(dto(sexualActivity = true)).contains("\"sexual_activity\":true"))
    }

    @Test
    fun `a server row without the column decodes as not recorded`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","water_ml":500}""",
        )
        assertTrue(decoded.sexualActivity == null)
    }

    /**
     * Was the opposite assertion until 26 Aug 2026, on the belief that the column might not exist.
     * It does — applied to production 13 Aug 2026, `text[] NOT NULL DEFAULT '{}'`. Un-ticking the
     * last food group is a clear like any other and has to reach the column.
     */
    @Test
    fun `un-ticking the last food group sends an explicit empty list`() {
        assertTrue(json.encodeToString(dto()).contains("\"food_groups\":[]"))
    }

    @Test
    fun `food groups carried from the server are sent back`() {
        val encoded = json.encodeToString(dto(foodGroups = listOf("vegetables", "protein")))
        assertTrue(encoded.contains("\"food_groups\":[\"vegetables\",\"protein\"]"))
    }

    @Test
    fun `a server row without food groups decodes as no meals recorded`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","water_ml":500}""",
        )
        assertTrue(decoded.foodGroups.isEmpty())
    }

    /** Tokens, not an enum: a group iOS adds later must decode here rather than throw. */
    @Test
    fun `an unfamiliar food group decodes intact`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","food_groups":["fermented"]}""",
        )
        assertTrue(decoded.foodGroups == listOf("fermented"))
    }

    // ── Half two: the clear must reach the server ────────────────────────────────────────────────

    @Test
    fun `a logged supplement is sent`() {
        val encoded = json.encodeToString(dto().copy(supplements = listOf("zinc", "folate")))
        assertTrue(encoded.contains("\"supplements\":[\"zinc\",\"folate\"]"))
    }

    /**
     * The 1.4.2 (19) defect, pinned. An empty list is NOT the same as "leave the column alone" —
     * it is "she removed them", and the payload has to say so out loud.
     */
    @Test
    fun `un-logging the last supplement sends an explicit empty list, not nothing`() {
        val encoded = json.encodeToString(dto().copy(supplements = emptyList()))
        assertTrue(
            "an empty supplement list must appear on the wire, or the clear never reaches the column",
            encoded.contains("\"supplements\":[]"),
        )
    }

    /** The column list PostgREST derives is what actually decides whether the clear lands. */
    @Test
    fun `the cleared row still names supplements in its column set`() {
        assertTrue("supplements" in wireKeys(dto().copy(supplements = emptyList())))
    }

    @Test
    fun `deselecting the last symptom sends an explicit empty list`() {
        val encoded = json.encodeToString(dto().copy(symptoms = emptyList()))
        assertTrue(encoded.contains("\"symptoms\":[]"))
    }

    @Test
    fun `removing the last glass sends an explicit zero`() {
        val encoded = json.encodeToString(dto().copy(waterMl = 0))
        assertTrue(encoded.contains("\"water_ml\":0"))
    }

    /** Blanking the notes field yields null; a deleted note must not survive on the server. */
    @Test
    fun `clearing the notes field sends an explicit null`() {
        val encoded = json.encodeToString(dto().copy(notes = null))
        assertTrue(encoded.contains("\"notes\":null"))
    }

    /** Identity must ride along or the upsert has nothing to conflict on. */
    @Test
    fun `identity survives a fully cleared row`() {
        val encoded = json.encodeToString(
            dto().copy(supplements = emptyList(), symptoms = emptyList(), waterMl = 0, notes = null),
        )
        assertTrue(encoded.contains("\"user_id\":\"user-a\""))
        assertTrue(encoded.contains("\"date\":\"2026-08-10\""))
    }

    /**
     * The whole contract in one assertion. A row cleared of everything names exactly the five
     * clearable columns plus its identity — and still does NOT name `sexual_activity`, the one
     * field that must never be forced. If this set changes, someone has either reintroduced the
     * silent-clear bug or started sending an explicit null into a `NOT NULL` column.
     */
    @Test
    fun `a fully cleared row names exactly the clearable columns and no risky ones`() {
        val keys = wireKeys(
            dto().copy(supplements = emptyList(), symptoms = emptyList(), waterMl = 0, notes = null),
        )
        assertEquals(
            setOf("user_id", "date", "symptoms", "water_ml", "supplements", "food_groups", "notes"),
            keys,
        )
    }

    /**
     * The payload itself, verbatim — not a `contains` check that could pass on a substring.
     *
     * This is the exact body the client PUTs when she un-ticks her last supplement, and it is what
     * PostgREST reads `columns=` off. `"supplements":[]` is present and empty: an explicit clear,
     * not an omission. On 1.4.2 (19) this same row serialized to `{"user_id":"user-a",
     * "date":"2026-08-10"}` and the column was never written.
     */
    @Test
    fun `the exact body a fully cleared row sends`() {
        val encoded = json.encodeToString(
            dto().copy(supplements = emptyList(), symptoms = emptyList(), waterMl = 0, notes = null),
        )
        assertEquals(
            """{"user_id":"user-a","date":"2026-08-10","symptoms":[],"water_ml":0,""" +
                """"supplements":[],"food_groups":[],"notes":null}""",
            encoded,
        )
    }

    /**
     * `sexual_activity` must never gain `@EncodeDefault`: the column is `NOT NULL DEFAULT false`,
     * so an explicit null would fail the whole upsert. Its clear is `false`, which encodes anyway.
     */
    @Test
    fun `withdrawing intimacy sends false rather than dropping the column`() {
        val encoded = json.encodeToString(dto(sexualActivity = false))
        assertTrue(encoded.contains("\"sexual_activity\":false"))
    }
}
