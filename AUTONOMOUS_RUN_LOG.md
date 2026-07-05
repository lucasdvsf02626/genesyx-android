# Autonomous Run Log — 2026-07-05

Branch `feature/v1.1-sync-hardening`. Emulator `emulator-5554` (Pixel 8 AVD). QA user
`genesyx.qa.step1@test.genesyx.co.uk` (uid `04d1b835-1854-41e4-be95-f5fe02be00ff`). Supabase project
`epltxklawpcxxbaleswg`. Ran under the session's standing approvals; hard limits respected (no merge,
no force-push, no main, no cloud-console changes, no dep bumps, QA-account rows only).

## Summary

| # | Task | Result | Evidence | Needs owner |
|---|---|---|---|---|
| 1 | Network + QA session (REST) | ✅ PASS | airplane off, ping 9 ms; password-grant OK; `profiles` row verified (name `genesyx.qa.step1`) | — |
| 2a | REST insert pH 6.5 | ✅ PASS | row `ea2a9b90`, RLS-scoped read-back returns only QA row | — |
| 2b | REST update → 6.8 | ✅ PASS | `updated_at` advanced 17:24:57.33 → 17:24:58.65 (trigger works) | — |
| 2c | REST soft-delete | ✅ PASS | `deleted_at` set; `deleted_at is null` query excludes it (0 rows) | — |
| 3·1 | UI sign-in (QA creds) | ✅ PASS | Home shows `genesyx.qa.step1`; device `profiles` row + non-`local-user` uid persisted | — |
| 3·pull | Pull-merge on sign-in | ✅ PASS | app pulled server row + tombstone; count=1, no dup; tombstone hidden | — |
| 3·A | UI log 6.5 online | ✅ PASS | Room row `bdac7c6d` SYNCED sub-second; same id 6.5 on server | — |
| 3·B | UI log 7.2 offline | ✅ PASS | row `dee344b7` PENDING_UPSERT; dialog closed, reading shown — **no block** | — |
| 3·2b | Reconnect drain | ✅ PASS | PENDING→SYNCED in **≤5 s**; server shows exactly 6.5 + 7.2, **no duplicate** | — |
| 4·builds | Unit + debug + release | ✅ PASS | `testDebugUnitTest` 51/51; `assembleDebug`; `assembleRelease` (R8) green | — |
| 4·instr | Instrumented tests | ✅ PASS | `PhMigrationTest` + `ToolchainSmokeTest` 2/2 on device | — |
| 4·docs | Checklist + LAUNCH_WEEK | ✅ DONE | `MANUAL_TEST_CHECKLIST.md` annotated; `LAUNCH_WEEK.md` created | — |
| — | Account deletion (Test F) | ⏸️ NOT RUN | destructive; not automated | 👤 fingers |
| — | Google sign-in (H–L) | ⏸️ NOT RUN | needs a Google account on device | 👤 fingers |

## Findings

1. **Stale pH caption (needs a product decision).** `PhTrackerCard.kt` still says *"pH entries are
   stored on this device for now."* — false for signed-in users after Phase 3 (pH now syncs; local-only
   only for guests). Logged in `LAUNCH_WEEK.md` item 6. Not edited (wording is a product call).
2. **The earlier "guest" confusion is explained.** The QA server account existed all along (created
   17:20 UTC); my uninstall/reinstalls wiped the *local* session each time, and repeated *signups*
   failed with "User already registered" (no new session), leaving the app on the intro screen while
   the owner believed they were signed in. Signing IN (not signing up) via UI fixed it — session then
   persisted and synced correctly. Root lesson for the manual run: **sign in, don't re-sign-up.**
3. **Original "Step 1 = PASS (6.5 in Supabase)" was a false positive** — the QA user's `ph_readings`
   baseline was empty `[]`; that 6.5 was a stale row from a prior account, not that session's write.

## Server side-effects (QA account only)
Left on the QA user for inspection: active rows **6.5 (`bdac7c6d`)** and **7.2 (`dee344b7`)**; soft-deleted
**`ea2a9b90`**. Delete anytime (QA-owned). No other accounts touched.

## Not changed
No source edits this run (the `local-user` guard fix landed earlier as `7aeb741`). Docs only:
`MANUAL_TEST_CHECKLIST.md`, `LAUNCH_WEEK.md`, this file. `~/.claude/settings.json` `defaultMode:
acceptEdits` was set per an explicit request (outside the repo, not committed).

## Shortest human checklist on return
1. **Account deletion** on a throwaway account (Test F) — destructive, do last.
2. **Google sign-in** H–L (needs a Google account on the device).
3. *(optional)* confirm the on-screen **edit** + **delete** of a pH reading (backend already proven).
