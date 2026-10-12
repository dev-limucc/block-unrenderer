# Block UN-renderer

[![build](https://github.com/dev-limucc/block-unrenderer/actions/workflows/build.yml/badge.svg)](https://github.com/dev-limucc/block-unrenderer/actions/workflows/build.yml)

A lightweight, **Sodium-compatible** Fabric client mod for **Minecraft 26.3** that instantly
hides chosen blocks, block entities, liquids and entities — with a keybind, no world reload, and
near-zero performance cost.

<p align="center">
  <img src="https://cdn.modrinth.com/data/9fQJdnIj/images/0edc3075d77a455947826890b76ef9de600efcdf.png" alt="Before: normal world view" width="49%" />
  <img src="https://cdn.modrinth.com/data/9fQJdnIj/images/e58b4580ce4636040f680e3e7036b80eaf35bf16.png" alt="After: listed blocks hidden" width="49%" />
</p>

## Features

- **Two filter modes** — *Hide listed* (blacklist) to make some blocks invisible, or *Show only
  listed* (whitelist) to make **only** the listed blocks visible. One list, switch the mode.
- **Hide blocks & block entities** — list them by ID (`minecraft:stone`, `minecraft:chest`, …).
- **Hide liquids** — a one-click **Liquids** toggle hides all water/lava, or add specific fluids
  to the list (covers flowing and waterlogged blocks too).
- **Hide entities** — an **Entities** toggle hides all mobs / items / projectiles.
- **Fullbright** — light exposed areas while hiding (OFF or FULLBRIGHT).
- **Hold or Toggle** trigger modes.
- **Instant** — no loading, no world reload; toggling triggers a single chunk re-mesh.
- **Heavily optimized** — zero-allocation render hooks (MixinExtras `@ModifyReturnValue`); when
  off it costs a single boolean check, so it's safe on low-end PCs.
- **Sodium compatible** — hooks the render path both vanilla and Sodium respect (including Sodium's
  own fluid renderer).
- **Map-mod friendly** — does not distort **Xaero's Minimap / World Map**; the map keeps showing
  the real world while blocks are hidden from your view.
- In-game config via **ModMenu** — a searchable block/liquid picker with icons and toggle buttons.

## Default keybinds

**Unbound by default** — bind them under **Options → Controls → Block UN-renderer**:

- *Toggle Block Hiding* — turn the filter on/off (or hold, in Hold mode)
- *Open Block Manager* — open the picker screen any time

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.3+ |
| Fabric API | * |
| ModMenu | 20.x — optional (in-game config UI) |

## Usage

1. Open **ModMenu → Block UN-renderer** (or bind & press *Open Block Manager*).
2. Pick a **Filter** mode — *Hide listed* or *Show only listed*.
3. Add the blocks/liquids the filter acts on (e.g. `minecraft:stone`), or use the **Liquids** /
   **Entities** toggles for those whole categories.
4. Bind a key under **Options → Controls → Block UN-renderer** and press it to hide/show.

## Client-only and multiplayer use

This is a **client-only** mod: it does not install server code, send a handshake, or require the
server to have the mod installed. Its rendering filter can be used in singleplayer or while
connected to a server. It only changes what your client renders and does not change world or block
data on the server.

Hiding blocks can reveal information that is normally occluded, so **check and follow each server's
rules before using it**. Client-only compatibility does not mean every server permits the mod.

## How it works

Regular blocks are hidden by returning `RenderShape.INVISIBLE` during chunk meshing, so they're
never built into the geometry (works identically under Sodium). Block entities (chests, signs…)
are hidden by dropping their per-frame render state; liquids by skipping the fluid mesher (both
vanilla and Sodium); entities via the entity render dispatcher. All per-block hooks use
MixinExtras `@ModifyReturnValue`, so no objects are allocated on the hot path.

**Map mods:** block states are never altered — only the *render* path is. While Xaero's Minimap
(`MinimapWriter`) and World Map (`MapWriter`) sample the world to build their maps, the mod reports
the real (unhidden) world, so hidden blocks stay on the map even though they're gone from view.

## Credits

Built by [Limucc-dev](https://github.com/dev-limucc).

## Gallery

<p align="center">
  <img src="https://cdn.modrinth.com/data/9fQJdnIj/images/0f08a8b6c632a7a7d6a7e438c1fd27b4778ded58.png" alt="In-game GUI — searchable block & liquid picker" width="70%" />
</p>
