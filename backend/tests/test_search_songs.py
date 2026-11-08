from __future__ import annotations

from datetime import UTC, datetime

from app.application.search import SearchSongs
from app.domain.models import Song, TrackHint
from tests.fakes import FakeLyricsSearch, result


def make(music, repo, lyrics_search):
    return SearchSongs(music, lyrics_search, repo)


def test_title_mode_tags_title_and_skips_lyrics_search(music, repo, lyrics_search):
    music.catalog["bohemian"] = [result("a"), result("b")]
    out = make(music, repo, lyrics_search).execute("bohemian", "title", 5)
    assert [r.source_id for r in out] == ["a", "b"]
    assert {r.match for r in out} == {"title"}
    assert lyrics_search.calls == []


def test_auto_merges_dedupes_and_puts_title_matches_first(music, repo):
    music.catalog["some words"] = [result("t1"), result("dup")]
    music.catalog["Song X Artist X"] = [result("dup")]
    music.catalog["Song Z Artist Z"] = [result("l1")]
    ls = FakeLyricsSearch([TrackHint("Song X", "Artist X"), TrackHint("Song Z", "Artist Z")])
    out = make(music, repo, ls).execute("some words", "auto", 5)
    assert [r.source_id for r in out] == ["t1", "dup", "l1"]
    assert [r.match for r in out] == ["title", "title", "lyrics"]


def test_auto_caps_at_limit(music, repo):
    music.catalog["q"] = [result("a"), result("b"), result("c")]
    music.catalog["S A"] = [result("d")]
    ls = FakeLyricsSearch([TrackHint("S", "A")])
    out = make(music, repo, ls).execute("q", "auto", 3)
    assert [r.source_id for r in out] == ["a", "b", "c"]


def test_auto_without_lyrics_search_is_just_title_search(music, repo):
    music.catalog["q"] = [result("a")]
    out = make(music, repo, None).execute("q", "auto", 5)
    assert [(r.source_id, r.match) for r in out] == [("a", "title")]


def test_auto_survives_lyrics_search_error(music, repo):
    music.catalog["q"] = [result("a")]
    ls = FakeLyricsSearch()
    ls.error = RuntimeError("genius down")
    out = make(music, repo, ls).execute("q", "auto", 5)
    assert [r.source_id for r in out] == ["a"]


def test_lyrics_mode_uses_hints_and_tags_lyrics(music, repo):
    music.catalog["Song X Artist X"] = [result("x1"), result("x2")]
    music.catalog["Song Y Artist Y"] = [result("y1")]
    ls = FakeLyricsSearch([TrackHint("Song X", "Artist X"), TrackHint("Song Y", "Artist Y")])
    out = make(music, repo, ls).execute("fragment of lyrics", "lyrics", 5)
    assert [r.source_id for r in out] == ["x1", "y1"]
    assert {r.match for r in out} == {"lyrics"}
    assert ("Song X Artist X", 1) in music.search_calls


def test_lyrics_mode_falls_back_when_no_token(music, repo):
    music.catalog["fragment"] = [result("f1")]
    out = make(music, repo, None).execute("fragment", "lyrics", 5)
    assert [(r.source_id, r.match) for r in out] == [("f1", "lyrics")]


def test_lyrics_mode_falls_back_when_genius_errors(music, repo):
    music.catalog["fragment"] = [result("f1")]
    ls = FakeLyricsSearch()
    ls.error = RuntimeError("401")
    out = make(music, repo, ls).execute("fragment", "lyrics", 5)
    assert [(r.source_id, r.match) for r in out] == [("f1", "lyrics")]


def test_lyrics_mode_falls_back_when_no_hints(music, repo):
    music.catalog["fragment"] = [result("f1")]
    out = make(music, repo, FakeLyricsSearch([])).execute("fragment", "lyrics", 5)
    assert [r.source_id for r in out] == ["f1"]


def test_in_library_flag(music, repo):
    repo.add_song(
        Song(
            id="s1",
            source_id="a",
            title="t",
            artist="a",
            duration_seconds=10,
            thumbnail_url=None,
            audio_filename="s1.m4a",
            created_at=datetime.now(UTC),
        )
    )
    music.catalog["q"] = [result("a"), result("b")]
    out = make(music, repo, None).execute("q", "title", 5)
    assert {r.source_id: r.in_library for r in out} == {"a": True, "b": False}
