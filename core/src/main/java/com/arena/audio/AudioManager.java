package com.arena.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Carrega os .wav gerados e toca os efeitos com:
 * - throttle: no maximo {@link Sfx#maxPerWindow} reproducoes a cada 50 ms por efeito;
 * - pitch aleatorio de +-10% para nao soar repetitivo;
 * - volume geral, mudo, e musica em loop (streaming).
 */
public class AudioManager implements GameAudio, Disposable {
    private static final long WINDOW_NANOS = 50_000_000L;
    private static final float MUSIC_FACTOR = 0.35f;

    private final Sound[] sounds = new Sound[Sfx.values().length];
    private final long[] windowStart = new long[sounds.length];
    private final int[] windowCount = new int[sounds.length];
    private Music music;

    private float masterVolume = 0.7f;
    private boolean muted;

    public AudioManager() {
        for (Sfx s : Sfx.values()) {
            sounds[s.ordinal()] = Gdx.audio.newSound(Gdx.files.internal("sfx/" + s.file + ".wav"));
        }
        music = Gdx.audio.newMusic(Gdx.files.internal("sfx/music.wav"));
        music.setLooping(true);
        applyMusicVolume();
    }

    @Override
    public void play(Sfx sfx) {
        if (muted || masterVolume <= 0f) return;
        int i = sfx.ordinal();
        long now = TimeUtils.nanoTime();
        if (now - windowStart[i] > WINDOW_NANOS) {
            windowStart[i] = now;
            windowCount[i] = 0;
        }
        if (windowCount[i] >= sfx.maxPerWindow) return;
        windowCount[i]++;
        sounds[i].play(sfx.volume * masterVolume, MathUtils.random(0.9f, 1.1f), 0f);
    }

    public void startMusic() { if (!music.isPlaying()) music.play(); }

    public void toggleMute() { muted = !muted; applyMusicVolume(); }

    public void changeVolume(float delta) {
        masterVolume = MathUtils.clamp(Math.round((masterVolume + delta) * 10f) / 10f, 0f, 1f);
        applyMusicVolume();
    }

    public float getMasterVolume() { return masterVolume; }

    public boolean isMuted() { return muted; }

    private void applyMusicVolume() {
        music.setVolume(muted ? 0f : masterVolume * MUSIC_FACTOR);
    }

    @Override
    public void dispose() {
        for (Sound s : sounds) s.dispose();
        music.dispose();
    }
}
