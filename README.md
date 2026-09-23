<div align="center">

# 🎬 LabysWatchParty

**Cinema screens for your Minecraft server.**<br>
Put a YouTube, Twitch or TikTok video on any wall and let your players watch it together, perfectly in sync.

[![Build](https://github.com/LabyMod/watchparty-paper-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/LabyMod/watchparty-paper-plugin/actions/workflows/build.yml)
![Paper](https://img.shields.io/badge/Paper%20%7C%20Spigot-1.20.1%2B-blue)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![LabyMod Server API](https://img.shields.io/badge/LabyMod%20Server%20API-1.0.14-8a2be2)

</div>

---

## ✨ Features

- **Two commands to a running cinema.** Create a screen by looking at a wall, paste a link, done.
- **Everyone in sync.** The server owns the timeline. Late joiners jump right into the current scene, pause and seek apply to everyone at once.
- **Sound stays in the room.** Only players near a screen see and hear it, walk out and it's gone.
- **Invisible for everyone else.** The screen is drawn by the LabyMod client. Players without it just see your wall (and get a friendly hint once).
- **Zero dependencies.** The LabyMod Server API is bundled and relocated, it can't clash with other plugins.
- **Proxy friendly.** Works on Paper/Spigot backends behind Velocity or BungeeCord.
- **Fully translatable.** Every message lives in `config.yml`.

## 📦 Installation

1. Grab `LabysWatchParty-<version>.jar` from [Releases](https://github.com/LabyMod/watchparty-paper-plugin/releases) (or the artifact of the latest [build](https://github.com/LabyMod/watchparty-paper-plugin/actions/workflows/build.yml)).
2. Drop it into `plugins/` and restart the server.

| | |
|---|---|
| Server | Paper or Spigot **1.20.1+** (Folia is not supported) |
| Java | 17+ |
| Players | [LabyMod 4](https://www.labymod.net) with *Watch Party* enabled (on by default) |

## 🚀 Quick start

**1. Build a screen.** Stand in front of a wall, look at the block where the **top center** of the screen should be and run:

```
/wp screen create cinema 16x9
```

**2. Press play.**

```
/wp play https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

That's it. 🍿

### Where does the screen go?

The plugin places a waxed sign **one block behind the wall and one block up** from the block you looked at. LabyMod draws the video right on the wall surface, hanging down from the top of that block.

```
      behind      wall      in front
     ┌──────┐
     │ sign │  ◄── hidden, placed for you
     └──────┘┌──────┐
             │ aim  │▌ ◄── top edge of the video (the block you looked at)
             │      │▌
             │      │▌     the video, drawn on the wall surface
             │      │▌     and hanging down 9 blocks for 16x9
             │      │▌
```

Tips:
- Keep the space behind the wall closed, so players without LabyMod never see the sign.
- Even widths (like `16`) center on the middle of the block you aim at, so the screen extends half a block further on one side. Use odd widths (like `15` or `17`) if you want the edges flush with the block grid.
- `/wp screen remove <id>` puts back whatever block the sign replaced.

## 🎮 Commands

`/wp` is short for `/watchparty`. The screen id is **optional** when there is only one screen or you're standing next to one.

| Command | What it does |
|---|---|
| `/wp play [screen] <url>` | Starts a video from the beginning |
| `/wp pause [screen]` | Pauses for everyone at the same position |
| `/wp resume [screen]` | Continues |
| `/wp seek [screen] <time>` | Jumps to `1:30`, `90`, `1:02:03`, or relative: `+10`, `-30`, `+1:00` |
| `/wp stop [screen]` | Stops and clears the screen |
| `/wp list` | All screens, what they're playing and how many watch |
| `/wp screen create <id> <size> [radius]` | Creates a screen. Size in blocks: `16x9`, or just `16` to keep 16:9 |
| `/wp screen remove <id>` | Removes a screen and restores the wall |
| `/wp reload` | Reloads `config.yml` and `screens.yml` |

**Supported links:** `youtube.com`, `youtu.be`, `twitch.tv`, `tiktok.com`. Direct video files (`.mp4` etc.) are not supported by the client.

## 🔐 Permissions

| Permission | Grants | Default |
|---|---|---|
| `watchparty.admin` | Everything below | op |
| `watchparty.control` | `play`, `pause`, `resume`, `seek`, `stop`, `list` | op |
| `watchparty.screen` | `screen create`, `screen remove`, `reload` | op |

Want your players to run their own movie night? Give them `watchparty.control`.

## ⚙️ Configuration

`plugins/LabysWatchParty/config.yml`

```yaml
# Radius in blocks around a screen in which LabyMod players see and hear it.
# Audio does not fade with distance, so outside this radius the screen is switched off.
# Can be overridden per screen: /wp screen create <id> <size> <radius>
default-radius: 48

# How often player distances to screens are checked, in ticks (20 ticks = 1 second).
range-check-ticks: 10

# How often everyone watching gets the current position again, in seconds.
# Clients only correct themselves when they drift more than 3 seconds, so this is cheap.
sync-interval-seconds: 20

# The LabyMod client only plays links from these hosts (subdomains included).
# Links from other hosts are rejected by the command right away.
allowed-hosts:
  - youtube.com
  - youtu.be
  - twitch.tv
  - tiktok.com

# Tell players without LabyMod once per session that they are missing a screen.
hint-without-labymod: true

# Color codes use &. Placeholders are written as {name}.
messages:
  prefix: "&8[&dWatchParty&8] &7"
  playing: "Now playing on &f{id}&7: &f{url}"
  hint: "&7There is a screen here. Play with &dLabyMod&7 to watch it!"
  # ... every other message is in the generated file
```

| Option | Default | Notes |
|---|---|---|
| `default-radius` | `48` | The single most important setting. Big enough to cover your cinema, small enough that the lobby next door stays quiet. |
| `range-check-ticks` | `10` | Lower = screens pop in faster when walking in. `10` (half a second) is plenty. |
| `sync-interval-seconds` | `20` | Safety net against drift. The actual play, pause and seek commands are sent instantly. |
| `allowed-hosts` | see above | Mirrors what the LabyMod client accepts. Adding hosts here won't make the client play them. |
| `hint-without-labymod` | `true` | One chat message per session for players without LabyMod standing near a screen. |
| `messages.*` | English | Translate freely. `&` color codes and `{placeholders}` are supported. |

Screens are stored in `plugins/LabysWatchParty/screens.yml`. The `radius` can be changed by hand (then `/wp reload`). For a new size or position, remove the screen and create it again, the sign text is only written on creation.

```yaml
screens:
  cinema:
    world: world
    x: 120        # position of the hidden sign
    y: 71
    z: -34
    facing: SOUTH # direction the video faces
    width: 16.0
    height: 9.0
    radius: 48
    replaced-block: minecraft:stone  # restored on removal
```

## 🛠️ How it works

LabyMod turns wall signs in this format into a video canvas:

```
laby:media
16x9         <- width x height in blocks
id:cinema    <- canvas id
```

The plugin keeps the playback state (link, position, playing or paused) on the server and talks to the clients with the [LabyMod Server API](https://dev.labymod.net/pages/server/) Watch Party packets:

| Moment | Packet |
|---|---|
| Player walks into a screen's radius | `register`, then `sync` if something is playing |
| Player walks out | `unregister` |
| `/wp play` | `play` |
| `/wp pause` / `/wp resume` | `sync` with the exact position, so nobody pauses a few seconds apart |
| `/wp seek` | `seek` |
| `/wp stop` | `stop` |
| Every `sync-interval-seconds` | `sync` to all viewers |

## ❓ Troubleshooting

**Nobody sees anything.** Check that the player uses LabyMod 4 and has *Watch Party* enabled in the LabyMod settings. Players who were already online when the plugin got enabled (e.g. after `/reload`) need to rejoin once.

**The link doesn't play.** Only YouTube, Twitch and TikTok links work, age-restricted or region-locked videos may fail on the client.

**I hear the movie everywhere.** Lower the screen's `radius` in `screens.yml` and run `/wp reload`.

**Late joiners see "ended".** The server doesn't know how long a video is. After it ended, the timeline keeps running until the next `/wp play` or `/wp stop`.

## 🧑‍💻 Building

```bash
./gradlew build       # jar in build/libs, runs the tests
./gradlew runServer   # local Paper test server with the plugin
```

Every push builds the jar via GitHub Actions (grab it from the run's artifacts). Pushing a tag like `v1.0.0` publishes a GitHub release with the jar attached.
