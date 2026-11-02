# Changelog

Versioning: `2.X.0` for a big update (new mod support), `2.0.X` for a small update or fix.

## 2.2.0

* **New: Mekanism Diesel Generators.** Its Create-powered **Shaft Pump** and **Inline Pump** filled their tank from any
  water source block without taking anything out of the world, so with Flowing Fluids they made endless water. Now the
  world pays for exactly the water the pump receives, every tick it pumps, using the same intake rules as every other
  pump (`pumps.drainPercent` applies). When the water runs short the pump slows down instead of inventing water. The hook
  only applies while Create is installed (the pumps do not exist without it) and is checked against the pump's code
  when the game loads: if a Mekanism Diesel Generators update changes how the pump works, the hook stays off and the log
  says why. New option `mekanism_diesel_generators.enabled` and a matching group in "Mod Compact List".
  Mekanism Diesel Generators' other machines need no hook: the Oil Pumpjack uses underground deposits, and the cooling
  tower, digester and pipes only handle fluid inside their own tanks.
* Create 6.0.10 was checked for conflicts with the new hook: none of its mixins touch fluid tanks, block entities or
  the pump class, and FluidCompact's Create exclusion only matches Create's own blocks.
* **Auto Balanced Compact moved into the core** (`core.balance`): it is the shared optimisation system of the whole mod,
  not a Flowing Fluids feature. Behaviour and config are unchanged.
* **Lighter intake path.** Finding the drain backend no longer copies a list and builds a stream on every intake
  (336 bytes per intake down to 16), which matters for pumps that work every tick. Same result, same API.
* Housekeeping: removed five stray duplicate source files (copies without the `.java` extension) from the repository.
* README: "What it does" is now the purpose of the mod, with a new **Supported mods** section.

## 2.1.1

* **Immersive Engineering: no more cobblestone from the Fluid Pump for finite fluids, including on blocks that had
  already dried.** The pump builds its work list first and takes the blocks later. With Flowing Fluids a thin layer can
  flow away in between; the pump still "takes" that empty block, spends energy and puts cobblestone there. The decision
  now also looks at the fluid the pump is working on. The debug log says `block already empty, pump works on a finite
  fluid` when this case is caught.
* **Immersive Engineering: the Fluid Pump now empties a pond completely.** It also takes the thin layers Flowing Fluids
  leaves behind (`immersive_engineering.drainThinFluids`).
* **New guard for the pump.** The Immersive Engineering hooks are now checked as one contract (`PumpContract`, a separate
  class that only needs ASM): the pump's fields, the exact number and order of the calls the hooks rely on, and the two
  helper methods in `Utils`. If an Immersive Engineering update changes any of it, **all** Immersive Engineering hooks
  stay off, the log says why, and the pump works like in plain Immersive Engineering. A hook that keeps failing while
  the game runs also switches itself off for the session. The pump guard also checks that the area search still stores the fluid it searches for.

## 2.1.0

* **New: Immersive Engineering.** The Fluid Pump no longer makes endless water (or lava, if lava source conversion is on)
  out of a pool with two source blocks. Flowing Fluids does not change that rule, so the pump filled its tank from
  nothing. With FluidCompact the pump takes real source blocks out of the world, 1000 mB per block, the way it already
  works for oil. The Fluid Placer needs no hook: it puts one source block down per 1000 mB taken from its tank.
* **Immersive Petroleum** needs no hook (it shows up in the compat list): its pumpjacks and derricks draw from
  underground reservoirs, and its fluids can never turn into source blocks by themselves.
* The pump no longer leaves cobblestone where it emptied a block of a finite fluid. Immersive Engineering replaces
  every emptied block with cobblestone (its own `pump_placeCobble` option and a per-pump toggle); for a finite fluid
  that left stone where the pond should shrink. FluidCompact skips the replacement for finite fluids only, so
  Flowing Fluids can refill or drain the area. New option `immersive_engineering.noCobbleForFiniteFluids`.
* Each Immersive Engineering hook is checked on its own against the pump's code when the game loads; a hook whose
  target changed stays off with a log line, and a hook that keeps failing switches itself off for the session.
* New options `immersive_engineering.finitePump` and `noCobbleForFiniteFluids`, with a matching group in "Mod Compact List".
* Known limit: the Fluid Pump only takes whole source blocks, so thin water layers stay behind, as with any pump that
  uses sources only.

## 2.0.0 - Official release

First stable release. It contains everything from the 1.3.x betas (Alex's Caves Drain, thin-water pumping for Modern
Industrialization pumps, the PneumaticCraft Liquid Hopper fixes, the fail-open hooks and the pipe-water cleanup) and
these final changes:

* **Public API is frozen.** `me.thanhtnt2.fluidcompact.api`, `api.event` and `api.compat` only gain members during 2.x;
  nothing is removed or changed in meaning. Everything else is internal.
* Safer hook loading: the PneumaticCraft pickup hook is now tied to the one flag it was written for
  (`dontUpdateInfiniteWaterSources`), and the Modern Industrialization thin-water hook checks the method it redirects
  and says so in the log when it has to stay off.
* `fluidcompact.log` rolls over at 16 MB (the first part of the run goes to `fluidcompact-rolled.log`), so debug mode
  can no longer fill a disk. The orphan-pipe cleanup also logs a repeating error only once a minute.
* The debug snapshot now shows `leftoverNeedsOutput`, the option that is actually used.
* Worlds saved by 1.2.x and 1.3.x load unchanged. The old `requireOutputContainer` key stays in the config file, ignored;
  it will be removed in 3.0.

Measured in a stress test (60 MI pumps, 21 Liquid Hoppers, 13,800 water cells): FluidCompact used about 0.1% of the
tick time and about 3 KB of memory; the cost of a big water simulation comes from Flowing Fluids itself.

## 1.3.2

* Fix: Modern Industrialization water pumps now also take the thin, non-source water layers Flowing Fluids leaves
  behind, so a pond can be drained to the last level. Before, such a layer made the pump run without producing water.
  The output per pump tier is still Modern Industrialization's own (bronze 125 mB, steel 250 mB, electric 2000 mB per
  water cell and cycle) and is always paid by the world. New option `modern_industrialization_pumps.drainThinWater`.
* Fix: the PneumaticCraft Liquid Hopper no longer needs a container on its output side to drain leftover water; with no
  output the hopper's own tank simply fills. The old `requireOutputContainer` key is ignored; the new
  `liquidHopperLeftoverNeedsOutput` (default off) replaces it. Skipped hoppers now log why when verbose/debug is on.
* Alex's Caves: a Copper Valve on any of the four sides of the Drain now also lets it take fluid from around the valve.
* Alex's Caves: kept the community-requested valve-above handling and the new skip-reason log lines.
* Hardening: every hook now also catches `LinkageError` (a changed Flowing Fluids API) and writes a repeating failure at
  most once a minute (`FluidCompactLog.errorThrottled`) instead of a stack trace on every tick.
* Alex's Caves: an error after the world was already charged no longer hands the Drain back to the original logic, so
  the same fluid cannot be moved twice.
* Optimisation: the pipe cleanup takes fewer chunks per tick while the server lags, reads the debug flag once per chunk,
  and the Drain sound/bubble lookup no longer takes a lock on every cycle.
* README now lists exactly which versions of each mod were checked.

## 1.3.1

* Fix: the Modern Industrialization pump hook now fails open like every other hook. An error inside it (for example a
  listener of another mod) is logged once a minute and the pump keeps running, instead of crashing the server with
  "Ticking block entity".
* Optimisation: the waterlogged-pipe cleanup no longer scans every chunk around the players in a single tick. The
  chunk list is worked off a few chunks per tick (same interval, same result), and almost every tick now exits at once.
* Optimisation: Liquid Hoppers that nobody configured no longer get a saved FluidCompact data attachment just by
  running the leftover-water check.
* Optimisation: the intake log and the Alex's Caves Drain build no log text while verbose/debug logging is off, and the
  verbose-log throttle table forgets machines whose window has passed.
* Alex's Caves: a Drain above an already full tank no longer re-checks the whole tank every cycle.
* Hardening: the PneumaticCraft pickup hook is switched off with a warning (instead of answering wrongly) if a future
  PneumaticCraft reads more than one config flag in that method.

## 1.3.0

* **New: Alex's Caves.** The Drain now works with Flowing Fluids. When a Copper Valve (or any redstone signal) opens it, the Drain
  takes fluid from above and puts exactly the same amount below, level by level, so no water is created or destroyed.
  The Copper Valve itself needs no hook: it only sends the redstone signal.
* New config section `alexscaves_drain` (speed, interval, sideways spread, effects) and a matching group in "Mod Compact List".
* If the Drain has nowhere to put the fluid, it waits instead of deleting it.

## 1.2.0

* New: Create is checked at startup. Flowing Fluids already supports Create by itself, so FluidCompact adds no Create hooks;
  it keeps its pipe features away from Create's blocks and warns when Flowing Fluids' Create settings contradict finite water.
* New: Auto Balanced Compact (enabled by default). While the server lags, FluidCompact stretches the intervals of its own
  optional housekeeping. It follows Flowing Fluids' own tick-time target and never touches the finite-water logic.
* Optimisation: idle Liquid Hoppers allocate nothing per call and no longer touch the saved data of their chunk.
* Cleanup: one pipe matcher instead of two identical copies.

## 1.1.1

* Liquid Hopper can now also drain the thin leftover water of a pond (partial levels that PneumaticCraft cannot pick up).
* New on/off button in the Liquid Hopper screen, per hopper.
* New options: drain leftover water without a Dispenser Upgrade (off by default), require a connected container, retry delay.
* Works with the tanks of any mod through the standard fluid capability; redstone control is PneumaticCraft's own.

## 1.1.0

* **New: PneumaticCraft: Repressurized.** Liquid Hopper and Gas Lift no longer duplicate water: PneumaticCraft's
  "leave the source block when it has two water neighbours" rule is switched off while water is finite.
  Drones (Liquid Import) already remove every source block, so they need no hook.
* New config section `pneumaticcraft_pickup`, new group in the "Mod Compact List" tab, JEI info page for the pumps.
* Optimisation: the Modern Industrialization hook allocates no block positions while scanning around a pump.
* Optimisation: the pipe cleanup uses a primitive set instead of boxed numbers.
* Checked and documented: the pump statistics are four flat numbers (no block coordinates), and the
  Modern Industrialization code is only reachable behind a mod-presence guard.
