# Block UN-renderer

A lightweight, **Sodium-compatible** Fabric client mod for **Minecraft 26.1.2** that hides
chosen blocks and block entities instantly with a keybind.

## Features

- **Hide any block or block entity** — list them by ID (`minecraft:stone`, `minecraft:chest`, …)
- **Hold or Toggle** trigger modes
- **Instant** — no loading, no world reload
- **Optimized** — zero per-frame cost when off; hidden blocks are simply never added to the chunk mesh
- **Sodium compatible** — hooks the render path both vanilla and Sodium respect
- In-game config via **ModMenu**

## Default keybind

`X` — rebindable in Options → Controls → Gameplay.

## Requirements

| | |
|---|---|
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.2+ |
| Fabric API | 0.150.0+26.1.2 |
| Cloth Config | 26.1.x |
| ModMenu | 18.x |

## Usage

1. Open **ModMenu → Block UN-renderer → Settings**
2. Pick **Trigger Mode** (Hold or Toggle)
3. Add block IDs to **Hidden Block IDs** (e.g. `minecraft:stone`)
4. Press **X** in-game to hide/show them

## How it works

Regular blocks are hidden by returning `RenderShape.INVISIBLE` during chunk meshing, so they're
never built into the geometry (works identically under Sodium). Block entities like chests are
hidden by cancelling their per-frame render-state extraction. Toggling triggers a single chunk
re-mesh; there is no continuous overhead.

## Credits

Built by [Limucc-dev](https://github.com/dev-limucc).
