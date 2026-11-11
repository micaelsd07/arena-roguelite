package com.arena.audio;

/** Todos os efeitos sonoros do jogo. Cada um vira assets/sfx/&lt;arquivo&gt;.wav. */
public enum Sfx {
    SHOOT("shoot", 0.35f, 3),
    ENEMY_SHOOT("enemy_shoot", 0.45f, 2),
    HIT("hit", 0.40f, 3),
    DEATH_CHASER("death_chaser", 0.55f, 3),
    DEATH_TANK("death_tank", 0.80f, 2),
    DEATH_SHOOTER("death_shooter", 0.60f, 2),
    PLAYER_HURT("player_hurt", 0.90f, 1),
    PICKUP("pickup", 0.30f, 4),
    LEVEL_UP("level_up", 0.80f, 1),
    SELECT("select", 0.70f, 1),
    RICOCHET("ricochet", 0.35f, 2),
    WAVE_START("wave_start", 0.75f, 1),
    GAME_OVER("game_over", 0.90f, 1);

    public final String file;
    /** Volume relativo do efeito (multiplicado pelo volume geral). */
    public final float volume;
    /** Maximo de reproducoes dentro da janela de 50 ms (throttle). */
    public final int maxPerWindow;

    Sfx(String file, float volume, int maxPerWindow) {
        this.file = file;
        this.volume = volume;
        this.maxPerWindow = maxPerWindow;
    }
}
