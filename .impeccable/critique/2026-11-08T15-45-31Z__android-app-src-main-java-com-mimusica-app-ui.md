---
target: Android app UI (Mi Música)
total_score: 28
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 1
target_identity: "file:C:\\Users\\Hilbert\\source\\repos\\AppMusicPlayer\\android\\app\\src\\main\\java\\com\\mimusica\\app\\ui"
timestamp: 2026-11-08T15-45-31Z
slug: android-app-src-main-java-com-mimusica-app-ui
---
# Critique #4: Mi Música Android UI (2026-11-08, after round-3 fixes, uncommitted)
Method: dual-agent (A design review, B mechanical evidence). Detector [] (cannot scan Kotlin); no browser overlay (native). Evidence: Pixel 7a screenshots in .impeccable/critique/screens-v4/.

## Heuristics: 28/40 (prev 28)
1 Status 3 (plain-lyrics karaoke silent; no volume level) | 2 Real world 3 (Sonando ahora/Reproduciendo/Reproducir ahora; PIN/número secreto) | 3 Control 3 (no seek/restart/cancel) | 4 Consistency 3 (icon-only karaoke pause; Reproducir 188 vs Borrar 166px) | 5 Error prevention 3 | 6 Recognition 3 ("Aa", karaoke pause) | 7 Efficiency 2 (~590px search results, no continue) | 8 Minimalist 3 (header ~21%) | 9 Recovery 3 | 10 Help 2

## Resolved since #3
Cantar on-ramp; stable Home tiles + counts + mini-player cover; delete dialog safe-first + reassurance; ¡Lista! as status; compact song rows (~490px); text scale cap 1.6; per-song TalkBack names, decorative covers, single progress stop, Aa label, mini-player label. Partial: silent states remain.

## Mechanical (B)
All text >=7:1 (min 7.07), targets >=64dp, insets OK everywhere, cap applied. Remaining: no liveRegion on play/pause, song change, searching, empty results, karaoke loading, delete done, text-size change, Home counts; Surface(onClick) without Role (Home tiles, transport, volume) unverified; playlist detail rows unmerged + unnamed Reproducir/Quitar; search field placeholder-only; spinner separate focus stop; 6dp transport gap; Home subtitle maxLines=2 clips at >=1.4x; dialog bodies don't scroll; text-size preview ignores cap; unused home_text_size; hardcoded karaoke "title · artist" and "♪".

## Priority issues
- P1 Plain-lyrics karaoke isn't karaoke, no notice; icon-only pause; no restart (KaraokeScreen.kt:132-180). clarify+harden
- P2 Vertical budget: Volver+H1 ~21%, tall search results, no Instrumental tag (Components.kt:167-200, SearchScreen.kt:144-230). layout
- P2 "Para quien ayuda" prominent button → PIN wall; PIN wording drift (SettingsScreens.kt:156). clarify
- P2 TalkBack remaining (silent states, Roles, playlist rows, search label, spinner). harden
- P3 Now Playing status: slider-looking progress, no volume level, 6dp gap, no restart (NowPlayingScreen.kt). polish

## Personas
Rosa: unsynced karaoke wall, PIN wall via Para quien ayuda, can't compare versions, "Aa". Caregiver: entry via text size, PIN drift, no test connection (unverified). TalkBack: karaoke/volume silent, play/pause & delete done unannounced.

## Minor
Home subtitle clip at >=1.4x; 500px idle void; mini-player artist truncation; dialogs not scrollable; preview ignores cap; unused/hardcoded strings; centred empty states.

## Questions
"Leer la letra" instead of Cantar for plain lyrics? Door colour on destination screens? "Seguir con…" on Home?
