# Fluid Compat

> **An unofficial intermediary compatibility mod that helps Flowing Fluids understand other mods better.**
> Built for [Flowing Fluids](https://github.com/Traben-0/flowing_fluids) by Traben.
> Not affiliated with or endorsed by its author. It contains no Flowing Fluids code: it only calls its public API.
> **Flowing Fluids is a required dependency** and is not bundled. See [CREDITS.md](CREDITS.md).

Flowing Fluids makes fluids finite. Fluid Compat helps it **understand the water pumps of other mods**, which
normally create water from nothing, so those machines drain the world instead.

NeoForge 1.21.1 - license: MIT (`LICENSE.txt`).

## What it does

Fluid Compat exists for one purpose: **to keep water finite when other mods move it.** Flowing Fluids makes the
world's water a limited resource, but pumps and drains from other mods still create water out of nothing or fail
to understand Flowing Fluids' partial fluid levels. Fluid Compat sits between the two and makes those machines take
exactly what they receive out of the world, so ponds shrink, rivers can be drained, and nothing is duplicated.
Fluid removal goes through the Flowing Fluids API.

## Supported mods

| Mod | What Fluid Compat covers |
| --- | --- |
| **Modern Industrialization** | Bronze, Steel and Electric Water Pumps drain the world, including thin water layers |
| **Mekanism** | Electric Pump removes the source blocks it pumps |
| **Mekanism Elements** | Seawater Pump drains the water source below it |
| **Mekanism Diesel Generators** (with Create) | Shaft Pump and Inline Pump drain the world instead of making water |
| **PneumaticCraft: Repressurized** | Liquid Hopper (also thin leftover water) and Gas Lift stop duplicating water |
| **Immersive Engineering** | Fluid Pump takes real source blocks, empties ponds fully, leaves no cobblestone |
| **Alex's Caves** | Drain moves finite fluid level by level (Copper Valve needs nothing) |
| **Create** | Handled by Flowing Fluids itself; Fluid Compat only checks the combination |
| **Immersive Petroleum** | Nothing needed: its pumpjacks draw from underground reservoirs |
| **Flowing Fluids** (patches) | Water wheels get their flow updates; negative removal amounts no longer create fluid; blocks without fluid are not erased; fluid interaction checks are skipped for pure level changes (large server tick saving). Each can be switched off |
| **Fluidlogged** | Optional, off by default (it can duplicate water): lets fluid enter pipes |
| **Jade, JEI, YACL** | Pump tooltip, pump info page, and the "Mod Compat List" config tab |

Pipes and cables (Modern Industrialization, Modern Dynamics, XNet, Refined Storage, Mekanism transmitters) never
take fluid from the world, so they become finite automatically once their pump is. Lava, which Flowing Fluids does
not flow by default, falls back to a small built-in vanilla drain. Fluid Compat writes its own log to
`logs/fluidcompat.log`.

## Requirements

* NeoForge 21.1.x, Minecraft 1.21.1
* **Flowing Fluids 1.0.6 or newer (required)**
* Optional: any of the mods in the table above

## Addon API (`me.thanhtnt2.fluidcompat.api`)

* `FluidCompatApi.service().requestIntake(...)` for your own pumps
* `DrainBackendRegistry.register(...)` to plug in another fluid-physics system
* `FluidIntakeEvent.Pre / Post` on `NeoForge.EVENT_BUS` to cancel or adjust intakes
* `CompatModuleRegistry.register(...)` to add an isolated compat module
* Tags `fluidcompat:managed_fluids`, `fluidcompat:finite_pumps`, `fluidcompat:fluid_passable_blocks`

## Reporting bugs

Report Fluid Compat bugs to me (thanhttn1) through GitHub issues and Discord
(thanhnhnh), **never to the authors of Flowing Fluids or the other mods**.
Please attach `logs/fluidcompat.log`.

## Credits and license

Fluid Compat is MIT licensed. Flowing Fluids (Traben, GNU LGPL 3.0) and every other mod it works with are credited
in [CREDITS.md](CREDITS.md).
