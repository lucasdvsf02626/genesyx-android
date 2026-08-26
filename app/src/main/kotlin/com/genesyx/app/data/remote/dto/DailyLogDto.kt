package com.genesyx.app.data.remote.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire model for the Supabase `daily_logs` row (snake_case; date ISO yyyy-MM-dd; arrays as text[]).
 *
 * ## Why some fields carry `@EncodeDefault` and others must not
 *
 * The shared serializer runs with `encodeDefaults` off, so a property equal to its default is
 * dropped from the JSON body — and PostgREST derives `columns=` from the body, so a dropped
 * property means **the column is never written**. For a field the user can *clear*, that turns an
 * explicit "remove this" into a silent no-op: the row is still marked SYNCED, and the next pull
 * brings the stale server value straight back. Un-ticking her last supplement undid itself this way
 * (observed on 1.4.2 (19): `columns=user_id,date` with `supplements` absent).
 *
 * So the rule is: **a field with a real clear-to-default path must be `@EncodeDefault`.** A field
 * without one must not be, because forcing it costs compatibility for nothing.
 *
 * | field | forced? | why |
 * |---|---|---|
 * | `symptoms`, `water_ml`, `supplements`, `notes` | yes | all four can be cleared back to their default by the UI, and all four are columns that exist on the server (see `docs/schema.sql`), so naming them can never fail |
 * | `mood`, `energy`, `sleep_minutes` | no | the form only ever *sets* these — there is no deselect, so they cannot reach their `null` default after having a value |
 * | `food_groups` | no — see below | |
 * | `sexual_activity` | **never** — see below | |
 */
@Serializable
data class DailyLogDto(
    @SerialName("user_id") val userId: String,
    val date: String,
    val mood: String? = null,
    val energy: String? = null,
    /** Clearable: deselecting the last symptom must blank the column, not skip it. */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val symptoms: List<String> = emptyList(),
    @SerialName("sleep_minutes") val sleepMinutes: Int? = null,
    /** Clearable: removing the last glass returns to 0, which must reach the server. */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) @SerialName("water_ml") val waterMl: Int = 0,
    /**
     * Clearable, and the field that exposed this whole class of bug. Un-logging the last supplement
     * writes an empty list locally; without the annotation that empty list never reached the server
     * and the removed supplements reappeared on the next refresh.
     */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val supplements: List<String> = emptyList(),
    /**
     * Omitted from the wire while empty (encodeDefaults is off), so the server's `'{}'` default
     * applies and a log with no meals recorded still syncs — including against a server that
     * predates the column.
     *
     * **This field has the same clear-never-syncs defect as [supplements]** — the Nutrition chips
     * can un-tick the last food group — but it is deliberately left unforced for now. Unlike the
     * four forced fields above, `food_groups` is *not* in `docs/schema.sql`'s `CREATE TABLE`; the
     * evidence that it is live is a worklog note (`docs/worklog/2026-08-13.md`), not the schema.
     * Naming a column that does not exist makes PostgREST reject the upsert, which would break
     * **every** daily-log push — a far worse failure than the clear bug it would fix. Force it only
     * once the column is confirmed live against production, and flip the test below with it.
     */
    @SerialName("food_groups") val foodGroups: List<String> = emptyList(),
    /** Clearable: blanking the notes field yields null, which must blank the column server-side. */
    @EncodeDefault(EncodeDefault.Mode.ALWAYS) val notes: String? = null,
    /**
     * Omitted from the wire while null (encodeDefaults is off in the shared serializer), so a log
     * without an intimacy record still syncs against a server that predates the column — only a
     * row that actually carries one needs the migration applied, and until then it queues and
     * retries like any failed push.
     *
     * **Must never carry `@EncodeDefault`.** The column is `boolean NOT NULL DEFAULT false`, so
     * forcing the `null` default would send `"sexual_activity":null` and the server would reject
     * the whole row. It also does not need forcing: the clear here is `false`, not `null`, and
     * `false` already differs from the default so it encodes on its own.
     */
    @SerialName("sexual_activity") val sexualActivity: Boolean? = null,
)
