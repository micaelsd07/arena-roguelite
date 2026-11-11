package com.arena.world;

import com.arena.audio.Sfx;
import com.badlogic.gdx.graphics.Color;

public enum EnemyType {
    //       raio  vida  veloc dano custo ondaMin xp  cor                              som de morte
    CHASER (0.45f, 20f, 3.6f, 10f, 1, 1, 1, new Color(0.95f, 0.30f, 0.30f, 1f), Sfx.DEATH_CHASER),
    SHOOTER(0.50f, 26f, 2.6f,  8f, 3, 2, 3, new Color(1.00f, 0.60f, 0.15f, 1f), Sfx.DEATH_SHOOTER),
    TANK   (0.95f, 90f, 1.6f, 22f, 5, 3, 5, new Color(0.62f, 0.35f, 0.95f, 1f), Sfx.DEATH_TANK);

    public final float radius, hp, speed, contactDamage;
    public final int cost, minWave, xp;
    public final Color color;
    public final Sfx deathSfx;

    EnemyType(float radius, float hp, float speed, float contactDamage, int cost, int minWave, int xp,
              Color color, Sfx deathSfx) {
        this.radius = radius;
        this.hp = hp;
        this.speed = speed;
        this.contactDamage = contactDamage;
        this.cost = cost;
        this.minWave = minWave;
        this.xp = xp;
        this.color = color;
        this.deathSfx = deathSfx;
    }
}
