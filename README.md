# LabysWatchParty

Synchronized video screens for Minecraft servers. Players using LabyMod see a YouTube, Twitch or TikTok video on a wall and watch it together, in sync. Everyone else just sees the wall.

Built on the [LabyMod Server API](https://dev.labymod.net/pages/server/) (Watch Party packets, `server-bukkit` 1.0.14), bundled and relocated. There is nothing else to install.

## Requirements

- Spigot or Paper **1.20.1 or newer**, Java 17+ (only Bukkit API is used)
- Players need LabyMod 4 with *Watch Party* enabled (it is on by default)
- Works behind Velocity/BungeeCord, install it on the backend server that has the screen
- Folia is not supported

## Quick start

1. Drop `LabysWatchParty-<version>.jar` into `plugins/` and restart.
2. Stand in front of the wall, look at the **top center block** of where the screen should be and run
   ```
   /wp screen create kino 16x9
   ```
   The plugin puts a hidden sign one block behind the wall. The video appears on the wall surface, hanging down from the top of the block you looked at.
3. Play something:
   ```
   /wp play https://www.youtube.com/watch?v=dQw4w9WgXcQ
   ```

## Commands

The screen id can be left out when there is only one screen or you stand next to one.

| Command | Permission | |
|---|---|---|
| `/wp play [screen] <url>` | `watchparty.control` | Start a video from the beginning |
| `/wp pause\|resume\|stop [screen]` | `watchparty.control` | |
| `/wp seek [screen] <1:30\|90\|+10\|-10>` | `watchparty.control` | Absolute or relative (seconds or mm:ss) |
| `/wp list` | `watchparty.control` | Screens, what they play, how many watch |
| `/wp screen create <id> <16x9\|16> [radius]` | `watchparty.screen` | Size in blocks. A single number keeps 16:9 |
| `/wp screen remove <id>` | `watchparty.screen` | Restores the block the sign replaced |
| `/wp reload` | `watchparty.screen` | Reloads `config.yml` and `screens.yml` |

`watchparty.admin` grants both. All permissions default to op. `/watchparty` is the long form of `/wp`.

## Configuration

`config.yml` holds the defaults (radius, sync interval, allowed hosts) and every message (`&` color codes, `{placeholders}`). Screens live in `screens.yml` and are managed with the commands above.

**Radius matters.** The client plays the audio at the player's media volume regardless of distance. A LabyMod player only gets a screen while inside its radius (default 48 blocks), outside of it the screen and its sound are switched off.

## How it works

The LabyMod client turns wall signs with this text into a video canvas:

```
laby:media
16x9          width x height in blocks
id:kino       canvas id
```

The canvas is rendered 1.5 blocks in front of the sign, centered on it and hanging down from the bottom of the sign block. The plugin keeps the playback state on the server (URL, position, playing or paused) and sends `MediaPlayerPacket`s:

- entering a screen's radius: `register` + `sync` (late joiners land at the current position)
- leaving it: `unregister`
- `play`, `pause`/`resume` (as `sync`, with the exact position), `seek`, `stop` to everyone in range
- a `sync` to all viewers every `sync-interval-seconds`; clients only correct drift above 3 seconds

Only `http(s)` links from youtube.com, youtu.be, twitch.tv and tiktok.com are played by the client. The plugin checks this up front so a wrong link fails at the command.

## Known limitations

- Players who were online before the plugin was enabled (e.g. `/reload`) are only recognized as LabyMod players after rejoining.
- The server doesn't know the video length. After the end, the position keeps counting and late joiners see the video as finished until the next `/wp play`.

## Development

```bash
./gradlew build       # shaded jar in build/libs, runs the tests
./gradlew runServer   # local Paper 26.2 test server with the plugin
```
