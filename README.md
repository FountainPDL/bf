# Block Front (FountainPDL Ministries)

Phase 1 — pipeline scaffold. Splash screen, fullscreen landscape lock,
and a working OpenGL ES voxel render loop with a touch-joystick flying
camera over a block grid. This is the foundation everything else
(shooting, AI, vehicles, world generation, survival) gets layered onto
in later phases — built and tested one working system at a time, the
same way Fountain Home and FountainPDL Bible were built.

## Loop (Termux)

1. Edit/add files under `bf/` (use the `0X_*.sh` setup scripts or
   edit directly with `cat > file << 'EOF' ... EOF`).
2. `git add -A && git commit -m "message" && git push`
3. GitHub Actions builds the debug APK automatically (see
   `.github/workflows/build-bf.yml`).
4. Download the `block-front-debug-apk` artifact from the Actions run,
   install it, test on-device.

## Status

- [x] Project skeleton + Gradle config
- [x] Splash screen ("FountainPDL Ministries")
- [x] Fullscreen immersive + locked landscape
- [x] OpenGL ES 2.0 render pipeline (voxel grid + flying camera)
- [x] Virtual joystick (movement)
- [ ] Chunked world / collision
- [ ] First-person look (drag-to-look) + weapon viewmodel
- [ ] Shooting system
- [ ] Block placement/destruction
- [ ] Enemy AI
- [ ] Vehicles
- [ ] Real icon art + branding pass

## Notes

- Icon is a placeholder gold square — swap `ic_launcher_foreground.xml`
  for real art (or a PNG-based adaptive icon) whenever ready.
- minSdk is 26 (Android 8.0), matching the adaptive-icon requirement
  and the original spec's "Android 8+" target.
