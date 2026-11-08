from __future__ import annotations

import pytest

from app.adapters.lrclib import LrclibProvider


@pytest.mark.integration
def test_real_lrclib_returns_lyrics_for_a_famous_song():
    provider = LrclibProvider(user_agent="MiMusica/0.1 (personal use; integration test)")
    lyrics = provider.get("Bohemian Rhapsody", "Queen", 354)
    assert lyrics is not None
    assert lyrics.synced_lrc or lyrics.plain
