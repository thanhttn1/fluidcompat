# Credits and attribution

## Flowing Fluids (required)

**FluidCompact is an unofficial, independent intermediary mod for Flowing Fluids.**
It is not made, endorsed or supported by Flowing Fluids' author.

* Mod: **Flowing Fluids**, by **Traben**
* License: **GNU Lesser General Public License 3.0 (LGPL-3.0)**
* Source: https://github.com/Traben-0/flowing_fluids

What FluidCompact does with it, stated plainly:

* Flowing Fluids is a **required dependency**. FluidCompact does not work without it, the game stops with a
  "missing dependency" message, and the Flowing Fluids jar is **never bundled** inside the FluidCompact jar.
* FluidCompact **does not contain any code, assets or data from Flowing Fluids**. It only calls the public
  Flowing Fluids API (`FlowingFluidsAPI`) at runtime, the same way any addon would. The Flowing Fluids jar is
  used purely as a compile-time reference while building.
* The goal is the opposite of copying: FluidCompact teaches Flowing Fluids' world about other mods' water pumps
  (Modern Industrialization, Mekanism, ...) so those machines drain the world instead of creating fluid.
* All the fluid physics people enjoy are Traben's work. Support the original mod.
* report Fluid Compact issues to me (via Github or Discord), please do not flood the author page.

## Alex's Caves (Unofficial Port)

* Alex's Caves (Unofficial Port, GPL-3.0) by Alexthe668, Noonyeyz, Raguto and contributors, ports included.
* FluidCompact contains no Alex's Caves code or assets. It references the Drain block by class name at runtime and only
  replaces what the Drain does when a finite fluid is involved; everything else (Copper Valve, redstone, sounds) stays original.

## Immersive Engineering and Immersive Petroleum

* Immersive Engineering by BluSunrize and Damien A.W. Hazard (Blu's License of Common Sense). FluidCompact contains no
  Immersive Engineering code or assets. It references the Fluid Pump by class name at runtime and only changes the
  answer to one question the pump asks (whether a fluid may become an endless source).
* Immersive Petroleum (All rights reserved). FluidCompact reads nothing from it and adds no hook; the module only
  shows up in the compat list.

## Mekanism Diesel Generators

* Mekanism Diesel Generators by Noe (Apache-2.0; its port and construction textures derive from Mekanism, MIT).
  FluidCompact contains no code or assets of it. It references the Create-powered Shaft Pump and Inline Pump class by name
  at runtime and only makes the world pay for the water those pumps put into their tank.

## Reporting bugs

**Report every FluidCompact bug to me (thanhttn1), not to the author of Flowing Fluids or of any other mod.**
Problems that show up only when FluidCompact is installed are very likely mine.

Please attach `logs/fluidcompact.log` (turn on `logging.debugMode` first if you can).

## Other mods FluidCompact works with

None of these are bundled or modified. FluidCompact references them by name or through their public API.
Licenses below are as declared in the mods' own metadata (versions we tested against).

| Mod | Author(s) | License |
| --- | --- | --- |
| Modern Industrialization 2.5.x | Azerococo, Technici4n | MIT |
| Mekanism 10.7.x | Aidancbrady and contributors | MIT |
| Mekanism Elements 3.0.x | FixedDolphin927, NadienDev | MIT |
| Mekanism Diesel Generators 1.5.x | Noe | Apache-2.0 |
| Fluidlogged 2.0.x | Leximon | MIT |
| Jade 15.x | Snownee | CC-BY-NC-SA-4.0 |
| Just Enough Items 19.x | mezz | MIT |
| YetAnotherConfigLib 3.8.x | isXander | LGPL-3.0-or-later |
| PneumaticCraft: Repressurized 8.2.x | desht, MineMaarten | GPL-3.0 |
| Alex's Caves | Alexthe668, Noonyeyz, Raguto and contributors | GPL-3.0 |

PneumaticCraft addons checked (no fluid mechanics to hook): PNC Ocean Addons (desht, LGPL-3.0), PNC Electrostatic Fix (JuiceyBeans, LGPL-3.0).
FluidCompact copies no PneumaticCraft code and does not link against PneumaticCraft classes at compile time. At runtime it
reads one setting of PneumaticCraft and adds a step after the Liquid Hopper's own import, both reached by name, and adds
a button to the Liquid Hopper screen through NeoForge's screen event. PneumaticCraft is GPL-3.0 (copyright MineMaarten and
desht); this project is MIT and ships none of its code or assets. If a PneumaticCraft author objects, please tell me and I will remove the hook.

Also checked for compatibility (no changes needed): Modern Dynamics (Technici4n, LGPLv3), XNet (McJty, MIT),
Refined Storage (Refined Mods, MIT), Tesseract API (Swedz, MIT), Mekanism Generators (MIT), mekmm (MIT).

## FluidCompact

Copyright (c) 2026 thanhtnt2, released under the MIT License (see `LICENSE.txt`).
Build scripts derive from the NeoForged MDK template (MIT, Copyright (c) 2023 NeoForged project).
