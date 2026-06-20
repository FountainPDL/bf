# Block Front (FountainPDL Ministries)

Android voxel FPS built entirely from Termux via GitHub Actions CI — no
Android Studio, no local SDK. Flow: Splash -> Menu -> Sandbox/Demolition.

## Loop (Termux)

1. Edit/add files under `bf/` using numbered `0X_*.sh` / `1X_*.sh` patch
   scripts, or edit directly with `cat > file << 'EOF' ... EOF`.
2. `git add -A && git commit -m "message" && git push`
3. GitHub Actions builds the debug APK automatically (see
   `.github/workflows/build-bf.yml`).
4. Download the `block-front-debug-apk` artifact, install, test on-device.

## Feature audit vs. the original master prompt

Legend: [x] done · [~] partial/MVP · [ ] not started

### Core / Technical
- [x] Java, offline, no accounts/ads/cloud
- [x] GitHub Actions CI build pipeline (debug, signed, persistent key)
- [x] Landscape lock, fullscreen immersive
- [ ] OpenGL ES 3.0 (currently ES 2.0 — fine for current scope)
- [ ] SQLite/Room, MVVM, modular package structure

### Graphics
- [x] Voxel rendering pipeline
- [ ] Smooth lighting, shadows, day/night cycle
- [ ] Particle effects, explosions
- [ ] Water, lava, fog, rain, storms, snow
- [ ] Graphics quality settings (Low/Med/High/Ultra)

### World System
- [~] Single static 24x24 flat block grid
- [ ] Infinite/chunked terrain generation
- [ ] Biomes (mountains, forests, rivers, oceans, deserts, caves, etc.)
- [ ] World seed, create/delete/rename/backup/restore

### Building System
- [~] Block destruction (shoot to break)
- [ ] Block placement, multiple block types, rotation
- [ ] Blueprints, copy/paste, structure saving

### Combat System
- [~] One mechanic: hitscan "shoot" with a 30-round mag + reload
- [ ] Multiple weapon types (pistols/SMGs/ARs/shotguns/snipers/LMGs/launchers/melee)
- [ ] Visible weapon viewmodel, recoil, muzzle flash, ADS, bullet spread, sway
- [ ] Weapon switching, reload animation, sound

### Enemy AI
- [ ] Not started (patrol/chase/cover/squads/difficulty tiers)

### Vehicles
- [ ] Not started (cars/tanks/helicopters/boats etc.)

### Survival System
- [ ] Health/hunger/thirst/stamina/temperature/fall damage — not started

### Inventory / Crafting
- [ ] Not started

### Campaign Mode
- [ ] Not started (was speced at 50 missions — realistically its own
      multi-session project once core systems above exist)

### Sandbox Mode
- [x] Free-roam, unlimited shooting/ammo-refill, no objectives
- [ ] Spawn menu, weather/time controls, vehicle/enemy spawning

### Demolition Mode (new — not in original spec)
- [x] Real gameplay loop: 15 marked targets, 60s countdown, win/lose,
      retry/menu — added as the first "actual gameplay mode" beyond
      free-roam, built on the existing shoot mechanic

### NPCs / Progression
- [ ] Not started

### Maps
- [x] One test map (flat grid)
- [ ] Forest/Desert/Snow/Island/City/Military Base

### Audio
- [ ] Not started at all — no footsteps, gunshots, music, ambience

### Save System
- [x] Settings persistence (look sensitivity) via SharedPreferences
- [ ] World state / progress / mission saves

### Controls
- [x] Virtual joystick, drag-to-look, fire button, view toggle
- [ ] Jump, sprint, crouch, dedicated reload button, vehicle controls

### Settings
- [x] Look sensitivity slider
- [ ] Graphics/FPS/audio/language/accessibility settings

### UI
- [x] Main menu (Sandbox / Demolition / Settings / Quit)
- [x] HUD: crosshair (hit-flash), ammo counter, Demolition targets/timer
- [ ] Health/hunger/stamina/compass/minimap (no systems behind them yet)

### Icon / Branding
- [x] Adaptive icon (green bg, blocky B, shadow)
- [x] Cinematic animated splash (fountain burst -> loop, glowing title)

## Notes

- minSdk 26 (Android 8.0), matching the adaptive-icon requirement and
  the original spec's "Android 8+" target.
- Everything under "not started" is a real, separately-scoped feature —
  none of it is stubbed with fake UI. Settings only exposes controls
  that actually do something.
