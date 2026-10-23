# Container Tooltip Compatibility Implementation Plan

> **For agentic workers:** Use subagent-driven-development for the independent snapshot helper and read-only review. Preserve the current feature branch and existing work.

**Goal:** Restore hydration in RS terminals and list-based Forge tooltip containers without adding GUI-specific dependencies.

**Architecture:** Capture the already-resolved signed hydration amount inside its invisible reservation row, then decode that snapshot during rendering. Accept extra footer height without moving the ordinary main tooltip rows. Keep the existing numeric fallback and boundaries.

**Tech Stack:** Java 8, Forge 2860, SimpleDifficulty 0.3.9, JUnit 4.12, Gradle 4.10.3.

## Task 1: Reproduce RS footer geometry (root)

- [x] Add a regression in `src/test/java/dev/rlcraftceaddons/thirstpreview/client/TooltipHydrationLayoutTest.java`: `iconTop(20, 60, 3, 4)` must equal 51, and `iconTop(20, 38, 3, 4)` must remain -1.
- [x] Run the test and verify the extra-footer assertion fails on 1.1.1.
- [x] In `TooltipHydrationLayout.iconTop`, reject `(long) height < 10L * count` instead of inequality. Retain every other check. Verify the native and footer cases pass.

## Task 2: Stateless tooltip snapshots (implementer)

- [x] Create `client/TooltipHydrationSnapshot.java` and `client/TooltipHydrationSnapshotTest.java`. API: `create(int amount, int width, int spaceWidth)`, `decode(List<String>)`, `decode(String)`, `removeRows(List<String>)`, and `upsert(List<String>, String)`.
- [x] Tests first: interleaved/copied/unmodifiable tooltip lists retain their own signed amount, full int range round trips, payload is visually empty, malformed/ambiguous data never recovers another item's amount, repeated insertion is idempotent and clearing removes only owned rows.
- [x] Run RED assertions before implementing. Use the existing Layout MARKER and nonbreaking-space reservation width; only valid legacy format codes may carry the versioned payload. No stack retention or global map.

## Task 3: Integrate event recovery (root)

- [x] In `TooltipHydrationRenderer`, create snapshot reservations, upsert the row, and read amount from the rendered row in PostText. Remove Pre cancellation/redraw so RS can keep drawing its footer. Support complete metadata fragments even when wrapping moves all padding to later lines.
- [x] In `ClientEvents.onItemTooltip`, clear owned old rows before resolving new values or returning for disabled/unsupported items.
- [x] Add runtime-independent integration checks for empty render-stack recovery and footer placement without depending on an OpenGL context. Retain existing static wiring checks as auxiliary evidence.
- [x] Fix both LWJGL float-buffer capacities after the actual isolated RS renderer reproduced the four-versus-sixteen failure.

## Task 4: Validate and deliver

- [x] Run full test/build with actual pack rules and verified GraveStone JAR; inspect the new JAR language keys and dependency isolation.
- [x] Request independent spec and code review, fix actual findings and rerun affected checks.
- [x] Bump 1.1.2 and update README/release notes/verification with exact coverage and limits. Back up the target v涛.3 installed 1.1.1 before replacement and prepare release source, checksums and screenshots for the established GitHub release workflow.
- [x] Independently run actual RS/Forge rendering in an isolated client: all eight screenshots pass, including numeric fallback retaining RS footer information. No world or save is loaded.
