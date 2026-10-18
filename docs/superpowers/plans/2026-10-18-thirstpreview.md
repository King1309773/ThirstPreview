# Thirst Preview Implementation Plan

> **For agentic workers:** Use subagent-driven-development for independent implementation and code review. This project is isolated in work/thirstpreview because the pack is not a Git repository.

**Goal:** Show SimpleDifficulty food/drink hydration in tooltips and preview restoration over the thirst HUD.

**Architecture:** Read the installed SimpleDifficulty API and runtime consumable registry without mutating stacks. Use ordinary Forge client events for tooltip and HUD integration; keep clipping math pure for tests.

**Tech Stack:** Java 8, Forge 1.12.2-14.23.5.2860, ForgeGradle 3.0.197, Gradle 4.10.3, JUnit 4.12.

## Task 1: Build and core math (root)

- [x] Create build.gradle, wrapper, metadata and language files in this dedicated project. Use the local SimpleDifficulty JAR as a deobfuscated compile dependency; exclude it from distribution.
- [x] Write PreviewMathTest with an odd current level (11), overflow (18 + 6), full thirst (20), and per-slot visible fill. Direct JUnit red run failed five assertions for placeholder behavior.
- [x] Implement immutable ThirstValues and PreviewMath; verify tests pass.

## Task 2: Real consumable values (integration implementer)

- [x] Inspect installed consumer bytecode and write failing tests for metadata, NBT, configured override and native drink/canteen behavior.
- [x] Implement src/main/java/dev/rlcraftceaddons/thirstpreview/ThirstValueResolver.java using the actual JSON matcher and consumer precedence. Return null for no hydration effect, including empty canteens.
- [x] Run resolver tests; verify stack NBT remains unchanged.

## Task 3: Tooltip and HUD (client implementer)

- [x] Implement src/main/java/dev/rlcraftceaddons/thirstpreview/client/ClientEvents.java and client configuration. Use ThirstValueResolver.resolve(ItemStack) returning nullable ThirstValues with public final amount, saturation and dirtyChance fields.
- [x] Add tooltips through ItemTooltipEvent to avoid colliding with AppleSkin's external panel. Localize primary hydration + droplet units; show saturation and dirtyChance with Shift.
- [x] Capture AIR Pre layout at LOWEST after NORMAL SimpleDifficulty increments right_height; overlay AIR Post. Use actual native texture variants, preserve GL state, respect cancellations/visibility and thirst upper bound. Never alter right_height.

## Task 4: Review, build, delivery (root + independent reviewer)

- [x] Run test and build with Java 8; fix root causes of failures. Verify JAR reobfuscation and dependencies.
- [x] Review spec compliance followed by code quality; fix material issues and rerun affected checks.
- [x] Write Chinese README with source/build/install paths, feature switches and manual in-game acceptance steps. Install only the new JAR into this pack's mods, preserving all existing files.
- [x] Record checks actually run and limitations; provide the deliverable paths.
