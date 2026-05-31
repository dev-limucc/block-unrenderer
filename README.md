# Block UN-renderer

A lightweight, **Sodium-compatible** Fabric client mod for **Minecraft 26.1.2** that hides
chosen blocks and block entities instantly with a keybind.

## Features

- **Two filter modes** — *Hide listed* (blacklist) to make some blocks invisible, or *Show only listed*
  (whitelist) to make **only** the listed blocks visible. One list, switch the mode.
- **Hide any block or block entity** — list them by ID (`minecraft:stone`, `minecraft:chest`, …)
- **Hold or Toggle** trigger modes
- **Instant** — no loading, no world reload
- **Optimized** — zero per-frame cost when off; hidden blocks are simply never added to the chunk mesh
- **Sodium compatible** — hooks the render path both vanilla and Sodium respect
- **Map-mod friendly** — does not distort **Xaero's Minimap / World Map**; the map keeps showing the
  real world while blocks are hidden from your view
- In-game config via **ModMenu** (Cloth Config) — plus a searchable block picker with icons

## Default keybinds

**Unbound by default** — bind them under **Options → Controls → Block UN-renderer**:

- *Toggle Block Hiding* — turn the filter on/off (or hold, in Hold mode)
- *Open Block Manager* — open the block picker screen any time

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | 0.150.0+26.1.2 |
| Cloth Config | 26.1.x |
| ModMenu | 18.x |

## Usage

1. Open **ModMenu → Block UN-renderer** (or bind & press *Open Block Manager*)
2. Pick a **Filter Mode** — *Hide listed* or *Show only listed*
3. Pick a **Trigger Mode** (Hold or Toggle)
4. Add the blocks the filter acts on (e.g. `minecraft:stone`)
5. Bind a key under **Options → Controls → Block UN-renderer** and press it to hide/show

## How it works

Regular blocks are hidden by returning `RenderShape.INVISIBLE` during chunk meshing, so they're
never built into the geometry (works identically under Sodium). Block entities like chests are
hidden by cancelling their per-frame render-state extraction. Toggling triggers a single chunk
re-mesh; there is no continuous overhead.

**Map mods:** block states are never altered — only the *render* path is. While Xaero's Minimap /
World Map sample the world to build their maps, the mod reports the real (unhidden) world (via a
re-entrant guard plus a map-thread fallback), so hidden blocks stay on the map even though they're
gone from your view.

## Credits

Built by [Limucc-dev](https://github.com/dev-limucc).
