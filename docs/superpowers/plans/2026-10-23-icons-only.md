# Icons-only tooltip implementation plan

**Goal:** Restore waterdrop-only item tooltips while retaining RS footer/list-only compatibility and held HUD preview.

**Architecture:** Reconnect the existing snapshot-based icon renderer. Keep native container drawing and eliminate hydration text, fallback numbers and counters. Cap defensive custom values at the 20-point bar capacity, using full/half slots for current 1–20 rules.

**Tech Stack:** Java 8, Minecraft 1.12.2, Forge 2860, SimpleDifficulty 0.3.9, existing JUnit and isolated runtime renderer harness.

1. Runtime harness in ignored `build/runtime-smoke-icons-only`: assert actual 1.1.3 tooltip has no snapshot and fails desired icon-only contract. Preserve previous evidence.
2. Update `TooltipHydrationLayoutTest` to require empty counters, capped full/half icon geometry and safe integer extremes. Run to observe failure before changing layout.
3. Reconnect `ClientEvents` renderer without a numeric hydration line. Update `TooltipHydrationLayout` and `TooltipHydrationRenderer` for icons-only rendering; no Pre replacement, no font-drawn numbers or label. Update config comments and version to 1.1.4.
4. Run Java 8 `test build`, supplying actual v涛.3 rules and GraveStone JAR; require successful reobfuscation and all tests passing. Then run real RS/Forge rendering with the exact final candidate and inspect screenshots.
5. Update README/release/verification with observed counts and limits. Back up installed 1.1.3 before installing identical 1.1.4. Commit, archive source, create checksums and release; verify remote asset digests and tag commit.
