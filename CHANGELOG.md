# Changelog

[中文](CHANGELOG_zh-CN.md)


## 0.3.23 “Starward” — 2026-11-14

- The chunk-image send throttle from 0.3.22 is per player view instead of shared state. It was static, so on a server one player's expensive rotated send would pause every other player's chunk streaming for two ticks; each view now keeps its own send cost and quiet ticks.

## 0.3.22 “Starward” — 2026-11-14

Third pass on the spark profile; the old profile showed 17.6% of the server thread inside a single chunk-image send (6.7% of it light sampling out of StarLight).

- **Light sampling is 16× cheaper for sections with no stored light layer.** Those sections have one value for all 4096 blocks, so one probe per column replaces sixteen `getLightValue` calls through the Starlight engine — the most expensive item in the profile — while mixed sections still sample per block exactly as before.
- **Expensive sends no longer land on consecutive ticks.** A rebuilt (rotated) image chunk costs far more than the tick budget, and the loop used to force at least one send per tick, so a 15–30 ms send turned into a 15–30 ms tick. After an expensive send the next two ticks send nothing; streaming totals the same, spread out.
- Block-change publishing per tick drops from 256 to 96 positions.

## 0.3.21 “Starward” — 2026-11-14

Second pass on the spark profile (Kolustar 24.3% → 21.4% of server-thread samples in 0.3.20; this attacks the next three items).

- Storage addresses and chart cells are memoised per thread. `PlanetAccess.address` alone was 3.6% of the server thread, rebuilt on every collision, seam and image lookup although a column's owner and rotation never change; each call also allocated three objects.
- The planet clock is memoised per level. `GroundFlightServer.mask` was 1.7% for what should be a field read, because every call went through the level's saved-data lookup.
- Chunk image streaming drops to a 2.5 ms tick budget with a 1 ms relight share (was 3 ms / 1.5 ms).

## 0.3.20 “Starward” — 2026-11-14

Measured against a spark profile of a real world (Kolustar was about a quarter of server-thread samples); these are the two largest items in it.

- **Planets no longer plan at world load.** Every planet of the starting system used to build its structure plan immediately, one after another on Minecraft's shared worldgen thread pool: 3–8 seconds each, twenty times, while chunk generation queued behind them. Plans now start only when a planet is actually loaded or entered, and they run on their own low-priority `kolustar-plan` thread instead of a worldgen worker. The home planet is still prepared before players join.
- **Chunk image streaming costs half a tick budget.** `ImageStream.tick` was the single biggest Kolustar item on the server thread (10% of samples: relight queue, packet forwarding, sends). Its per-tick budget drops from 6 ms to 3 ms, its relight share to 1.5 ms, and chunks per tick from 24 to 12. Chunks still arrive nearest-first; the ring simply fills over a few more ticks.

## 0.3.19 “Starward” — 2026-11-14

- `farTerrain.enabled` now defaults to **false**: Kolustar's own far-terrain LOD meshes, the planet-wide coarse surface build they need, and the LOD thread pool stay off unless you turn them on in the config screen. With Distant Horizons installed you keep far terrain through DH; without it, drawing stops at loaded chunks. Raise `farTerrain.enabled` (or set `farTerrain.deferToDistantHorizons` for DH-only) to bring Kolustar's LODs back.

## 0.3.18 “Starward” — 2026-11-14

Server-tick and chunk-generation cost work, all in the hot path that runs while you explore.

- Generation heightmaps are now written once per column instead of once per placed block. A heightmap only ever rises, so the topmost matching block of a column is the whole update; on a deep planet this removes roughly 25 000 heightmap writes per generated chunk.
- Blocks go into their section through a hoisted section array, so no per-block section lookup and division remain.
- Column generation no longer allocates: the column buffer is reused per worldgen thread and the position probe is a single mutable instance, saving a ~400-entry array and 256 objects per chunk.
- A planet whose plan has no sites (the normal case at `generation.planBudgetMs = 50`) now skips structure start scanning and biome decoration entirely, instead of walking 256 columns and the whole structure registry for every chunk.
- The structure registry is grouped by generation step once and cached, rather than rebuilt for every chunk.
- New server watchdog: when a tick misses 200 ms, the log reports the worst tick and how many chunks were generated in the same window, e.g. `[Kolustar] worst tick 3844 ms since last report; 214 chunks generated in 3120 ms`. This separates chunk generation from other server work without a profiler.

## 0.3.17 “Starward” — 2026-11-14

- `generation.planBudgetMs` now defaults to 50 ms (allowed range 10–600000), so a planet plan can no longer hold world load for a noticeable time. A 50 ms budget surveys and places only what fits in that window, so expect very few or no planned structures on newly generated planets; raise the value when you want them back. Cached plans are unaffected.

## 0.3.16 “Starward” — 2026-11-14

- Cut the remaining planet-plan stalls. The plan budget is now one hard deadline shared by site survey and structure placement (previously each phase got its own, so a 3 s setting could block for about 5 s), and the default drops to 1500 ms.
- Loading a cached plan no longer replays every structure recipe: structural checks still run for every site, but the deep template replay is capped at 250 ms per plan, and the log reports how many sites were verified in depth.
- Chunk generation no longer waits for a planet plan that is still being built when it collects structure entity sites.
- New timing lines for diagnosing stalls: `Planet plan <seed>: <n> sites, <ms> ms (survey … / place … / record …, budget hit)` and `Home planet plan held world load for <ms> ms` when that wait is 250 ms or more.

## 0.3.15 “Starward” — 2026-11-14

- `generation.gpuChunkGeneration` now defaults to on. The GL compute path only runs on the render thread, because world-generation workers have no current GL context; off-thread generation falls back to the parallel CPU fill and logs that once. The compute stage still produces column hints the CPU composition does not consume, so this does not reduce CPU work yet — it also no longer performs the per-chunk synchronous readback that stalled chunk streaming.

## 0.3.14 “Starward” — 2026-11-14

- Replace every `-Dkolustar.*` tuning flag with a real config file and an in-game config screen (Mods → Kolustar → Config): bloom (plus threshold/knee/strength), far terrain on/off, defer-to-Distant-Horizons, LOD and surface build thread counts, coarse far-terrain step, planet structures, the planet-plan time budget, experimental GPU chunk generation and the HBM compatibility mixins. Files: `config/kolustar-client.toml` and `config/kolustar-common.toml`.
- Cut the world-load/join stall: the planet structure-plan time budget now defaults to 3 seconds instead of 12, and `generation.planetStructures=false` removes the pass entirely. Plans are cached, so existing worlds are unaffected until a planet is regenerated; raise `generation.planBudgetMs` if you want more structures placed.
- The HBM compatibility switch is read from the common config at startup, so it survives without launch arguments.

## 0.3.13 “Starward” — 2026-11-14

- Fix HBM Continuum machines losing model parts as the camera turns on planets. Golem Core's GemRender learns each machine's model bounds and culls whole machines and individual parts against them (`dev.golem.core.render.Frustums.isVisible`); those bounds assume the flat vanilla chart, so parts dropped out and reappeared while looking around. Kolustar now skips that frustum test on planet levels only; the renderer still decides what to draw. (`-Dkolustar.hbmCompat=false` disables the compat mixins; Golem's own `-Dgolem.gem.cull=off` also disables its culling everywhere.)
- Stop drawing the coarse far-terrain patch that contains the camera. That patch is covered by real chunks, and its downsampled heightfield could sit above the real surface and hide nearby blocks and block entities (machines, chests) depending on the view angle.

## 0.3.12 “Starward” — 2026-11-14

- HBM Continuum machines on planets: the compatibility mixin no longer forces every `hbm:` block entity to render unconditionally. It bypasses only the flat-chart frustum bounds test and still honours the machine renderer's own visibility decision (incomplete/unformed machines, render distance), so partially built machines stop drawing model parts that belong to a state they are not in.

## 0.3.11 “Starward” — 2026-11-14

- Fix severe stutter while chunks generate: the GL 4.3 chunk-generation path dispatched compute work and synchronously read it back once per chunk from worldgen worker threads (where Minecraft's GL context is not current), serialising all chunk generation behind a global lock while the CPU fill still did the real work. Normal generation now uses the parallel CPU fill; the GPU readback path is opt-in with `-Dkolustar.chunkGenGpu=true`.
- Keep Kolustar's own far terrain enabled when Distant Horizons is installed, because DH LODs do not follow the curved planet chart and showed up as detached sheets/holes over real chunks. Use `-Dkolustar.deferToDistantHorizons=true` to defer to DH again, or `-Dkolustar.lod=false` to disable Kolustar far terrain entirely. A log line now reports which path is active.
- Add `-Dkolustar.hbmCompat=false` to disable the HBM Continuum culling compatibility mixin when diagnosing machine-model rendering.

## 0.3.10 “Starward” — 2026-11-14

- Show a “Preparing universe…” progress screen while planet plans are being generated/loaded, instead of stalling without feedback. Plan generation now reports progress stages (surveying, placing structures, saving) used by the loading UI.

## 0.3.9 “Starward” — 2026-11-14

- Bound planet structure planning to reduce long world-load/join stalls: add a planning time budget (default 12s, `-Dkolustar.planBudgetMs` to adjust), cap selection work/samples per candidate, and allow skipping planet structures entirely with `-Dkolustar.planetStructures=false`. Plans are cached after generation, so existing cached plans are unaffected until regenerated.

## 0.3.8 “Starward” — 2026-11-14

- Replace voxel-box far terrain/LOD meshing in `GroundFarMesh` with a downsampled polygon heightfield: far tiles sample terrain surfaces at LOD step resolution (seed/worldgen-derived cached summaries) and emit continuous triangulated/quads with edge skirts instead of stacked box columns. This targets smoother, lighter far terrain; visual correctness and compatibility with Kolustar's curved planet charts/shaders still need in-game validation.

## 0.3.7 “Starward” — 2026-11-14

- Fix a launch crash on NeoForge (`Decoration with key 'fabric-modId' already exists on config kolustar.hbm.mixins.json`) caused by `kolustar.hbm.mixins.json` being registered twice in `neoforge.mods.toml`.
- Disable the new bloom post pass by default (`-Dkolustar.bloom=true` to opt in) after reports of severe render corruption/overlays in modded packs; it needs in-game validation across Sodium/Iris/Veil pipelines before being on by default.
- Add a hard kill switch for Kolustar's custom far terrain/LOD/cover rendering (`-Dkolustar.lod=false` or `-Dkolustar.farTerrain=false`) and skip building/drawing those meshes when Distant Horizons is present (also detects `distant_horizons`/`lod` mod ids) or when explicitly disabled, to avoid heavy CPU/GPU load and render issues in packs that use DH or shaders.

## 0.3.6 “Starward” — 2026-11-14

- Add bloom: an additive half-resolution post pass at `AFTER_LEVEL` (bright extraction with threshold/knee, one separable 9-tap Gaussian and a composite), so suns, glowing cores and engines bleed light into their surroundings. Targets are recreated on resize and released on world unload; GL state and the bound program are restored around the passes. Tunable with `-Dkolustar.bloom=false|threshold|knee|strength`. It only runs in Kolustar dimensions, so other worlds keep vanilla's look.
- Defer distant terrain to Distant Horizons when it is installed: Kolustar's own CPU LOD job/mesh pipeline is skipped and DH owns far terrain, instead of both systems meshing the same distance. `distanthorizons` is declared as an optional client dependency and is not bundled. Whether DH lines up with the curved planet charts still needs an in-game check.
- Cut chunk-generation CPU and allocation cost: one `BlockState` column buffer is reused per chunk instead of allocating one per column (`SurfaceFunction.blocksInto`), the canonical cell/ground/structure lookups are computed once per column, the planet terrain column cache evicts earlier, and worker threads remember that no GL context is available instead of re-probing it every chunk.
- Dedicated servers are forced to compatibility chunk generation; performance mode stays forced on for non-macOS GL 4.3 clients and force-disabled on macOS.

## 0.3.5 “Starward” — 2026-11-14

- Force chunk-generation performance mode on non-macOS GL 4.3-capable devices (macOS remains force-disabled/compatibility). `SystemChunkGenerator.fillFromNoise` now attempts GL 4.3 compute-assisted generation via `GpuChunkGenerator`, which compiles/dispatches `assets/kolustar/shaders/compute/chunk_gen.comp` with SSBO upload/readback when a live GL 4.3 context is available, and falls back to the deterministic CPU pipeline when compute/context/dispatch is unavailable. Final block/material composition remains CPU-authored through `SurfaceFunction` for terrain/plan correctness and deterministic output; chunk generation typically runs on worker threads without a GL context, so render-thread/client-side dispatch scheduling is still needed for consistent GPU execution.
- Add targeted HBM Continuum (1.0.5) planet-chart compatibility: `HbmRadarPlanetMixin` guards `TileEntityMachineRadarNT` map sampling/prefetch slots that leave the local planet chart/storage and filters detected radar entries to the same planet, reducing invalid height/chunk reads and cross-planet radar hits on curved/wrapped charts. Existing dispatcher-level forced rendering for `hbm` block entities on planet levels remains in place to mitigate culling/flicker; HBM height/radar behavior and GemRender flicker still need in-game verification.

## 0.3.4 “Starward” — 2026-11-14

- Add chunk generation modes: `-Dkolustar.chunkGenMode=compatibility|performance`. Compatibility uses the deterministic CPU pipeline. Performance is intended for GL 4.3 compute-assisted generation on non-macOS runtimes but is force-disabled on macOS and currently falls back to the CPU pipeline because chunk generation runs without a guaranteed GL context; a GL 4.3 compute shader scaffold (`assets/kolustar/shaders/compute/chunk_gen.comp`) is included for future wiring.

## 0.3.3 “Starward” — 2026-11-14

- Add optional HBM Continuum compatibility scaffolding for NeoForge 1.21.1: local development/build jars in `libs/hbm/` (`hbm-neoforge-1.0.5-1.21.1.jar`, `gemrender-0.1.20-neoforge-1.21.1.jar`, `golemcore-neoforge-1.0.5+1.21.1.jar`) are on the compile/test/runtime classpath, and `kolustar.hbm.mixins.json` loads only when HBM is installed.
- On Kolustar planet levels, HBM-namespaced block-entity renderers are forced through the block-entity render dispatcher to reduce model flicker/disappearing from frustum/culling mismatches. This HBM integration is partial and not verified in-game here; height detection and radar behavior on curved planet charts may still need targeted fixes.
- Reduce chunk-generation CPU work when generating many chunks in parallel: the planet terrain column cache is now concurrent with bounded eviction, chunk fill skips the leading empty air range per column, and surface block generation avoids redundant full-column writes. Minecraft chunk/block world generation remains CPU-bound in this pipeline (GL 3.3 provides no compute shaders for worldgen), so this release does not move chunk generation itself to the GPU.
- Cap background LOD mesh workers and the in-flight tile queue so distant terrain rebuilds no longer saturate the CPU while moving (`kolustar.lodThreads` overrides the worker count). Tiles fill in more slowly under load, but the game thread and chunk work keep more headroom.
- Keep the planet minimap always visible on a planet surface; the world map remains on `K`.
- Unbind the LOD vertex array after draws so custom render passes do not leave GL state behind for other mods.


## 0.3.1 “Starward” — 2026-11-14

- Move the ground sky scattering integral from the CPU to the vertex shader (`atmosphere_color.vsh`), where it
  runs per vertex on the GPU instead of 1225 CPU ray marches every frame. Fog, star extinction and remote limbs keep
  the CPU integrator, and both copies share the same constants. Measured on the same inputs: 1.16 ms to 0.01 ms of
  frame CPU, with the sky unchanged (float parity within 8-bit colour precision).
- Add an always-visible planet minimap and a dedicated world map (default `K`). Both draw the same cube-face
  ground chart the atlas already uses, so the map continues across face edges and cube corners instead of
  stopping at a flat world border the way a world-texture minimap does. The minimap is a compact HUD panel
  (terrain, north-up, waypoint bearing); the map screen opens straight on the surface layer with zoom, pan,
  coordinates and the north-up toggle. Map tiles are uploaded as GPU textures and drawn as quads, so the
  chart itself costs no per-frame CPU beyond what is already on screen.
- Unbind the LOD vertex array after draws so custom render passes do not leave GL state behind for other mods.
- Cap background LOD mesh workers and the in-flight tile queue so distant terrain rebuilds no longer
  saturate the CPU while moving (`kolustar.lodThreads` overrides the worker count). Tiles fill in more slowly
  under load, but the game thread and chunk work keep more headroom.
- Upload the sky dome once and rotate it per vertex in `atmosphere_color.vsh` from a three-vector observer
  basis, so a frame no longer rebuilds 1225 directions or re-uploads the dome. Measured on the same inputs:
  6.9 µs of frame CPU and the whole 19.6 KB per-frame upload removed (0.4 ms of CPU per second at 60 fps).
- Cut the CPU cost of uploading a LOD tile: the tile extent is now one min/max pass instead of two
  `AABB` allocations per vertex, and the material palette interns by field probing instead of allocating a
  key object and hashing it for every vertex. Measured on a 4000-vertex tile: 0.38 ms to 0.17 ms of render-thread
  CPU per upload (extent 0.124 to 0.059 ms, interning 0.252 to 0.109 ms).
- Project planet terrain through Sodium 0.8: its chunk shader receives the same warp, projection uniforms
  and native-chunk coverage as the vanilla core shaders. Iris is supported while no shader pack is enabled;
  an active Iris pack, Embeddium or Canvas still disables planet terrain projection and far views.
- Add Particle Rain and Weather2 compatibility as optional, mod-gated hooks: Particle Rain precipitation asks
  each planet's own weather instead of the level rain flag (so airless worlds and space stay dry). The Kolustar planet
  level is registered in Weather2's weather, cloud, storm and wind dimension lists, so those systems now run on planets
  rather than being confined to the overworld: precipitation, cloud and funnel particles wrap with the planet through
  Kolustar's projected particle rendering, and the rain state Weather2 publishes there drives Particle Rain too. Only
  Weather2's flat-world fog override stays suppressed inside planet levels, where the planet atmosphere owns fog;
  airless worlds and above-atmosphere regions stay dry and silent. Normal dimensions are untouched.
- Fix the Veil depth hook targeting two framebuffer wrapper classes that Veil 4.3-4.5 no longer contains, which made
  the whole patch fail to apply; only classes present across those versions are targeted now.
- Give clearer feedback from `/kolustar` and flight commands when a planet is not ready, the player is not on a planet,
  or there is no active flight session, instead of returning no output.
- Make the default (Normal) world type a Kolustar native universe so new worlds spawn players on an earth-like home planet by default.

## 0.3.0-alpha.1 “Starward” — 2026-11-09

**Create a new world. Saves from 0.2.0 are not supported, and no migration is provided.**

- Split experience worlds, starter content, maps, materials and Biotech into optional modules; keep development probes and profiling separate from player jars.
- Add Atlas planet surveys, ground/globe views, 3D system and interstellar maps, navigation targets and persistent ship tracking.
- Extend reachable star systems and interstellar engine progression. Fuel and component mass now affect flight and remaining delta-v.
- Add gas-giant pressure, skimming, aluminum hull plating and osmium pressure hulls.
- Add optional Flesh hive terrain, blood and the Host encounter through Biotech (requires Worlds and GeckoLib 4.8.2).
- Rework deterministic terrain planning, caves, roads, structures and ecology; improve far-view caching, streaming and rendering.
- Improve return flights and map-based recovery of saved ship dependency groups.

This prerelease contains changes since 0.2.0. Single-player remains the validation target. Full survival progression, fuel balance, multiplayer and shader compatibility are not accepted. The optional seasonal display module is omitted while its third-party provenance review remains pending; core seasonal APIs remain available.

## 0.2.0 “Moonrise”

**Worlds from 0.1.x cannot be opened with 0.2.0.** Create a new world. An experience-pack or generated-universe
world from 0.1.x stops with a crash report when it is opened; Kolustar does not overwrite its data, but open such
a world with 0.1.1 only. A plain overworld that merely had Kolustar installed still opens, without its old Kolustar
dimension entries. Back up before trying.

### Added

- Moons: a planet in a star system datapack can list `moons`, each a full planet with its own surface, orbit and
  spin. See [docs/DATAPACKS.md](docs/DATAPACKS.md).
- A star system may hold a single planet, and the home planet no longer needs a companion.
- Other planets, moons and suns appear in the sky seen from a planet's surface.
- A ship left without a pilot in space coasts to a stop and then holds its place relative to the nearest body, also
  across saving and reloading.

### Changed

- Landing: the height above ground is measured from the hull's real underside against the terrain right below it.
  Ships touch down on slopes instead of cutting lift a block or two above them, and the airstair deploys once the
  ship rests on sloped ground.
- All bodies of a world now share one level.
- The far view reuses its patches while nothing in them changes.
- The code is sorted into packages; a roadmap is in [docs/ROADMAP.md](docs/ROADMAP.md).

### Fixed

- Light placed or removed next to a face edge now updates on the other side of the edge too.
- Worlds with more than 256 planets no longer mix up planets over the network.

### Known issues

- On stepped or sharply sloped ground a landed hull can rest a fraction of a block into a terrain edge.
- After saving and reloading in space, the locked destination is cleared; lock it again.
- On long flights the server can pause for 2–4 seconds around arrival.
- Tested with NeoForge 21.1.247, Create 6.0.10, Create Aeronautics 1.3.2 and Sable 2.0.5. With Sable 2.0.6 a
  landing, a walk on the ground and a take-off were checked, not a whole trip.
