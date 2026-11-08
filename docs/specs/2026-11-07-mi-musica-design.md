# Plan: "Mi Música" — app de música y karaoke para un adulto mayor (73 años)

## Contexto
Un familiar de 73 años necesita escuchar su música, armar listas y cantar (karaoke) sin pelear con apps complejas. Requisitos: descargar canciones (fuente: **YouTube**), listas de reproducción, letra sincronizada para cantar, y **buscar una canción por un fragmento de letra** que recuerde. Decisiones confirmadas con el usuario:

- Teléfono **Android**; distribución como **APK personal** (sideload). Descargar de YouTube viola sus Términos → nunca Play Store, uso estrictamente personal/familiar.
- **Servidor propio + app**: el backend hace la extracción con yt-dlp; cuando YouTube rompa algo se actualiza el servidor sin tocar el teléfono del familiar.
- Stack: **Android nativo Kotlin + Jetpack Compose + Media3** / **Python FastAPI + yt-dlp**.
- Uso **mixto**: el familiar busca solo (voz o texto) y el cuidador (usuario) puede agregar canciones/listas remotamente desde un panel web.

Carpeta `C:\Users\Hilbert\source\repos\AppMusicPlayer` vacía y sin git → proyecto nuevo.

## Hechos verificados
- **LRCLIB** (lrclib.net): gratis, sin API key, `/api/get` devuelve `syncedLyrics` (LRC con tiempos). Su `/api/search?q=` busca solo título/artista/álbum, **NO texto de letra**. Rate limit ~45–90 req/min por cliente identificado (enviar User-Agent propio).
- **Genius API** `/search?q=`: busca metadatos y letras a la vez (requiere token gratuito). Se usa para "buscar por fragmento".
- yt-dlp sigue siendo la herramienta más mantenida; NewPipe tiene roturas recientes (SABR) → refuerza la elección de servidor.

## Arquitectura

```
[Teléfono familiar: App Android] ──HTTPS + token──► [Servidor FastAPI (Docker)]
[Cuidador: panel web /admin]     ──HTTPS + login──►   ├─ yt-dlp / ytmusicapi (búsqueda + audio)
                                                      ├─ LRCLIB (letra sincronizada)
                                                      ├─ Genius (búsqueda por fragmento)
                                                      └─ SQLite + carpeta de audio/LRC
```

El servidor es la **fuente de verdad** de biblioteca y listas (así el cuidador edita remotamente). El teléfono descarga audio + `.lrc` a almacenamiento local (Room + archivos) para **escuchar sin internet**.

### Monorepo
```
AppMusicPlayer/
  backend/   FastAPI hexagonal
  android/   app Kotlin
  docs/      specs, ADRs
```

### Backend (hexagonal, Python 3.12, FastAPI)
- `domain/`: `Song`, `Playlist`, `Lyrics`, `DownloadJob` (estados: pending/downloading/ready/failed).
- `ports/`: `MusicSource` (search, fetch_audio), `LyricsProvider` (get_synced), `LyricsSearch` (find_by_fragment), `LibraryRepository`, `AudioStorage`.
- `adapters/`: `YtMusicSource` (ytmusicapi para resultados limpios tipo canción + yt-dlp para audio m4a/opus), `LrclibProvider`, `GeniusLyricsSearch`, `SqliteRepository` (SQLAlchemy), `FileSystemStorage`.
- `application/`: casos de uso `SearchSongs`, `SearchByLyrics` (Genius → título/artista → MusicSource; fallback: buscar el fragmento directo en YouTube), `RequestDownload` (job en background: audio → LRCLIB por título/artista/duración → guarda), `ManagePlaylists`.
- `api/`: REST `/search`, `/search/lyrics`, `/songs`, `/songs/{id}/audio`, `/songs/{id}/lyrics`, `/downloads`, `/playlists`. **OpenAPI** autogenerado por FastAPI, enriquecido con descripciones, ejemplos y esquemas de respuesta (regla global; usar skill `openapi-spec-generation`).
- `admin/`: panel web mínimo para el cuidador (Jinja2 + HTMX): buscar, descargar, armar listas, ajustar desfase de letra.
- Seguridad: token por dispositivo para la app, login para el panel; nunca exponer sin HTTPS (Caddy). Despliegue en Docker Compose; el lugar (PC de casa con Tailscale/Cloudflare Tunnel o VPS) se decide en la fase de despliegue.
- Jobs: `BackgroundTasks`/cola simple en proceso (no hace falta Celery para un usuario).

### App Android (Clean Architecture, Kotlin)
- `domain/`: modelos + `LrcParser` (puro, testeable) + casos de uso.
- `data/`: Retrofit (API), Room (biblioteca/listas offline), WorkManager (descargas a disco con reintento), sync con servidor.
- `player/`: Media3 `MediaSessionService` → reproducción en segundo plano y controles en pantalla bloqueada.
- `ui/`: Compose, pantallas container + componentes presentacionales.

### Diseño para adulto mayor (no negociable)
- Inicio con **4 botones gigantes**: Mis canciones · Mis listas · Buscar · Cantar.
- Texto ≥ 22sp, objetivos táctiles ≥ 64dp, alto contraste, sin gestos (nada de deslizar ni mantener presionado), botón "Volver" visible siempre.
- **Búsqueda por voz** con un micrófono grande (Android `SpeechRecognizer`), además de teclado. Un solo campo: la app intenta título/artista y fragmento de letra, y muestra máximo 5 resultados grandes con botón "Descargar".
- Confirmación clara antes de borrar; configuración oculta tras PIN del cuidador.
- Reproductor: play/pausa enorme, anterior/siguiente, volumen; botón "Cantar" visible si hay letra.
- **Karaoke**: línea actual grande y resaltada, siguiente línea debajo, sincronizada con la posición de Media3 (~100 ms). Sin letra sincronizada → letra estática; sin letra → aviso amable.

## Fases (cada una entregable y probable)
1. **Base**: `git init`, Commitizen/Conventional Commits (skill `conventional-commit`), estructura monorepo, Docker Compose.
2. **Backend núcleo**: búsqueda, descarga con yt-dlp, letra LRCLIB, biblioteca y listas + OpenAPI.
3. **App núcleo**: inicio, biblioteca offline, reproductor en segundo plano, descargas, listas.
4. **Karaoke**: `LrcParser` + pantalla sincronizada.
5. **Búsqueda por fragmento + voz**: `GeniusLyricsSearch` + fallback, micrófono en la app.
6. **Panel del cuidador** + sincronización remota de listas.
7. **Despliegue**: servidor accesible por HTTPS, APK firmada instalada en el teléfono.

## Testing (Strict TDD activo)
- Backend: pytest test-first por caso de uso con adapters falsos (fake `MusicSource`, `LyricsProvider`); tests de contrato de la API con `httpx`; un test de integración opcional (marcado) contra yt-dlp/LRCLIB reales.
- Android: JUnit para `LrcParser`, casos de uso y ViewModels; tests de UI Compose que verifican tamaños táctiles y textos; prueba manual en el teléfono real.

## Verificación end-to-end
1. `docker compose up` → abrir `/docs` (Swagger) y probar `/search?q=...` y `/search/lyrics?q=<fragmento>`.
2. Pedir descarga → job pasa a `ready`, `/songs/{id}/audio` y `/songs/{id}/lyrics` responden.
3. Instalar APK en emulador/teléfono → buscar por voz, descargar, activar modo avión y reproducir; abrir "Cantar" y comprobar que la línea resaltada sigue la música.
4. Desde el panel web agregar una canción a una lista → aparece en el teléfono tras sincronizar.
5. Prueba de usabilidad real con el familiar: ¿puede buscar, reproducir y cantar sin ayuda?

## Riesgos aceptados
- YouTube puede romper yt-dlp temporalmente → mitigado actualizando el contenedor (`pip install -U yt-dlp`).
- Uso contra los Términos de YouTube: solo uso personal, sin distribución pública.
- Cobertura de letras sincronizadas en español no es total → fallback a letra estática.
