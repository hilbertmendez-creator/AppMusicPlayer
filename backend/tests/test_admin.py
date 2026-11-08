from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from tests.conftest import basic
from tests.fakes import result

PAGES = ["/admin", "/admin/search", "/admin/library", "/admin/playlists"]


@pytest.fixture
def admin(client):
    c = TestClient(client.app)
    c.headers.update(basic())
    return c


@pytest.fixture
def container(client):
    return client.app.state.container


def add_song(admin, container, music, source_id="vid1") -> str:
    music.catalog["q"] = [result(source_id, "Hello", "Adele")]
    r = admin.post(
        "/admin/downloads", data={"source_id": source_id, "title": "Hello", "artist": "Adele"}
    )
    assert r.status_code == 200
    return container.repo.get_song_by_source_id(source_id).id


@pytest.mark.parametrize("path", PAGES)
def test_admin_requires_basic_auth(anon, path):
    r = anon.get(path, follow_redirects=False)
    assert r.status_code == 401
    assert r.headers["www-authenticate"].startswith("Basic")


@pytest.mark.parametrize(("user", "pw"), [("admin", "wrong"), ("root", "s3cret-pass"), ("", "")])
def test_admin_rejects_bad_credentials(anon, user, pw):
    assert anon.get("/admin/library", headers=basic(user, pw)).status_code == 401


def test_admin_rejects_bearer_token(anon):
    assert (
        anon.get("/admin/library", headers={"Authorization": "Bearer test-app-token"}).status_code
        == 401
    )


@pytest.mark.parametrize("path", PAGES[1:])
def test_admin_pages_render(admin, path):
    r = admin.get(path)
    assert r.status_code == 200
    assert "htmx.org@2.0.4" in r.text


def test_admin_root_redirects_to_search(admin):
    r = admin.get("/admin", follow_redirects=False)
    assert r.status_code in (302, 303, 307) and r.headers["location"].endswith("/admin/search")


def test_search_results_partial_has_download_button(admin, music):
    music.catalog["hello"] = [result("vid1", "Hello", "Adele")]
    r = admin.get("/admin/search/results", params={"q": "hello", "mode": "title"})
    assert r.status_code == 200
    assert "Hello" in r.text and "Adele" in r.text and "Descargar" in r.text


def test_search_results_empty_query_is_graceful(admin):
    assert admin.get("/admin/search/results", params={"q": ""}).status_code == 200


def test_download_partial_polls_until_ready(admin, container, music):
    r = admin.post(
        "/admin/downloads", data={"source_id": "vid1", "title": "Hello", "artist": "Adele"}
    )
    assert r.status_code == 200
    # background task already ran in the test client -> partial for a ready job is final
    song = container.repo.get_song_by_source_id("vid1")
    assert song is not None
    job = container.repo.list_jobs()[0]
    again = admin.get(f"/admin/downloads/{job.id}")
    assert again.status_code == 200
    assert "hx-trigger" not in again.text  # no more polling once ready


def test_pending_job_partial_keeps_polling(admin, container):
    job = container.request_download.submit("vid9")
    r = admin.get(f"/admin/downloads/{job.id}")
    assert r.status_code == 200 and "hx-trigger" in r.text


def test_failed_job_shows_error(admin, container, music):
    music.fail_download = RuntimeError("x")
    admin.post("/admin/downloads", data={"source_id": "vidx"})
    # a failed job shows its readable error and stops polling
    job = container.repo.list_jobs()[0]
    r = admin.get(f"/admin/downloads/{job.id}")
    assert job.error in r.text and "hx-trigger" not in r.text


def test_library_lists_song_and_edits_offset(admin, container, music):
    sid = add_song(admin, container, music)
    page = admin.get("/admin/library")
    assert "Hello" in page.text and "Adele" in page.text
    r = admin.post(f"/admin/library/{sid}/offset", data={"offset_ms": "-350"})
    assert r.status_code == 200
    assert container.repo.get_song(sid).lyrics_offset_ms == -350


def test_library_offset_validation(admin, container, music):
    sid = add_song(admin, container, music)
    r = admin.post(f"/admin/library/{sid}/offset", data={"offset_ms": "abc"})
    assert r.status_code == 422
    assert container.repo.get_song(sid).lyrics_offset_ms == 0


def test_library_refresh_lyrics(admin, container, music, lyrics_provider):
    from app.domain.models import Lyrics

    sid = add_song(admin, container, music)
    lyrics_provider.lyrics = Lyrics(synced_lrc="[00:03.00]new", plain="new")
    assert admin.post(f"/admin/library/{sid}/refresh-lyrics").status_code == 200
    assert container.repo.get_song(sid).synced_lrc == "[00:03.00]new"


def test_library_delete_asks_confirmation_and_deletes(admin, container, music):
    sid = add_song(admin, container, music)
    assert "confirm(" in admin.get("/admin/library").text
    assert admin.post(f"/admin/library/{sid}/delete").status_code == 200
    assert container.repo.get_song(sid) is None


def test_playlists_full_flow(admin, container, music):
    s1 = add_song(admin, container, music, "vid1")
    s2 = add_song(admin, container, music, "vid2")
    assert admin.post("/admin/playlists", data={"name": "Mornings"}).status_code == 200
    pl = container.repo.list_playlists()[0]
    assert pl.name == "Mornings"
    admin.post(f"/admin/playlists/{pl.id}/songs", data={"song_id": s1})
    admin.post(f"/admin/playlists/{pl.id}/songs", data={"song_id": s2})
    assert container.repo.get_playlist(pl.id).song_ids == [s1, s2]
    admin.post(f"/admin/playlists/{pl.id}/songs/{s2}/move", data={"direction": "up"})
    assert container.repo.get_playlist(pl.id).song_ids == [s2, s1]
    admin.post(f"/admin/playlists/{pl.id}/rename", data={"name": "Evenings"})
    assert container.repo.get_playlist(pl.id).name == "Evenings"
    assert "Evenings" in admin.get("/admin/playlists").text
    admin.post(f"/admin/playlists/{pl.id}/songs/{s1}/remove")
    assert container.repo.get_playlist(pl.id).song_ids == [s2]
    admin.post(f"/admin/playlists/{pl.id}/delete")
    assert container.repo.list_playlists() == []


def test_playlist_blank_name_rejected(admin):
    assert admin.post("/admin/playlists", data={"name": "  "}).status_code == 422


def test_admin_post_requires_auth(anon):
    assert anon.post("/admin/playlists", data={"name": "x"}).status_code == 401


def test_cross_origin_post_blocked(admin):
    r = admin.post(
        "/admin/playlists", data={"name": "x"}, headers={"Origin": "https://evil.example"}
    )
    assert r.status_code == 403
