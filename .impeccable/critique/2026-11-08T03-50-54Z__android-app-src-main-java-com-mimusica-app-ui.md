---
target: Android app UI (Mi Música)
total_score: 28
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 1
target_identity: "file:C:\\Users\\Hilbert\\source\\repos\\AppMusicPlayer\\android\\app\\src\\main\\java\\com\\mimusica\\app\\ui"
timestamp: 2026-11-08T03-50-54Z
slug: android-app-src-main-java-com-mimusica-app-ui
---
# Critique #3: Mi Música Android UI (2026-11-07, after round-2 fixes + redesign commit dde1c4c)
Method: dual-agent (A design review, B mechanical evidence). Detector returned [] (cannot scan Kotlin); no browser overlay (native). Evidence: Pixel 7a screenshots in .impeccable/critique/screens-v3/.

## Heuristics: 28/40 (prev 27)
1 Status 3 (karaoke no semantics, volume no level) | 2 Real world 3 (PIN/número secreto drift; "Para quien ayuda") | 3 Control 3 (no seek/restart/cancel download) | 4 Consistency 3 (tiles shift with mini-player) | 5 Error prevention 3 (delete dialog no reassurance, "Sí, borrar" dominant) | 6 Recognition 3 (mini-player generic note) | 7 Efficiency 2 (no play-all/continue; ~735px song cards) | 8 Minimalist 3 (tall cards; header ~19%) | 9 Recovery 3 (Cantar dead end) | 10 Help 2

## Specificity
Product logic authored (per-door colour, green=success only, labelled controls, size previews, lyric search). Visual/emotional layer generic: stock M3 + Roboto, cold grey Paper F1F3F5 with 2dp slate outlines, kiosk-like tiles; "WarmColors" not warm.

## Mechanical (B)
All text >=7:1. Progress track 3.29/3.66, fill 3.44 (fixed). Targets >=64dp (volume exactly 64). No gesture-only. Live regions on banners/downloads; none on play/pause, song change, searching, empty results, karaoke, delete done. Identical "Portada de la canción" everywhere; song row clickable without Role duplicating Play; mini-player no click label (mini_player_open unused); progress spoken twice; "Aa" label-in-name; 7 maxLines without Ellipsis; unused strings selected_check, playlist_mic_desc; app text scale x OS fontScale uncapped (1.82x worst) — Home weighted tiles may clip.

## Priority issues
- P1 Cantar has no on-ramp, fails silently (SingScreen.kt:29, NowPlayingScreen.kt:190, KaraokeScreen.kt). clarify. Also check why lyrics missing (GENIUS_TOKEN absent / LRCLIB).
- P2 Home unstable + stateless: tiles 434->350px with mini-player, no cover, no counts (HomeScreen.kt:99-105,198-205). layout
- P2 TalkBack debt: unnamed repeated Borrar/Descargar/Reproducir, mini-player label, cover noise, double progress, Aa, ellipsis. harden
- P2 Lists don't scale, delete dialog thin, ¡Lista! pill looks tappable (SongsScreen.kt:94-127, Components.kt:220-239,409-423). distill
- P2 Cold generic identity + uncapped text scale (Theme.kt:20-79). colorize, typeset

## Personas
Rosa: Cantar dead end, can't preview 3 versions, taps green pill, progress looks like slider, tiles jump. Caregiver: hidden "Para quien ayuda", PIN wording, no examples/show toggle/test connection. TalkBack: 5 identical Borrar, mini-player mute, karaoke silent, Volver top-left.

## Minor
2-line empty search field; "No encontré nada" first person; redundant "Reproduciendo"; unused strings; hardcoded "Aa"; no volume level.

## Questions
Fetch lyrics at download and announce? Home as living-room radio with "Seguir escuchando"? 15-second previews for choosing versions?
