package com.arena.world;

import com.arena.audio.Sfx;

public enum EnemyType {
    //            sprite      alt.(m) raio  vida  veloc  dano custo onda xp  comportamento        chefe  massa  som
    WRAITH  ("wraith",   1.5f, 0.42f,  18f, 3.4f, 10f, 1, 1, 1, Behavior.CHASER,  false, 1.0f, Sfx.DEATH_CHASER),
    NECRO   ("necro",    1.6f, 0.45f,  26f, 2.6f,  8f, 3, 2, 3, Behavior.SHOOTER, false, 1.0f, Sfx.DEATH_SHOOTER),
    WEREWOLF("werewolf", 1.5f, 0.52f,  32f, 3.0f, 14f, 2, 3, 2, Behavior.DASHER,  false, 0.8f, Sfx.DEATH_WOLF),
    KNIGHT  ("knight",   1.9f, 0.70f,  95f, 1.6f, 22f, 5, 4, 5, Behavior.TANK,    false, 0.3f, Sfx.DEATH_TANK),

    KING    ("knight",   4.2f, 1.35f, 900f, 1.9f, 30f, 0, 0, 40, Behavior.BOSS_KING,    true, 0.05f, Sfx.DEATH_TANK),
    ALPHA   ("werewolf", 3.6f, 1.15f,1300f, 2.8f, 28f, 0, 0, 50, Behavior.BOSS_ALPHA,   true, 0.05f, Sfx.DEATH_WOLF),
    SUPREME ("necro",    4.0f, 1.10f,1800f, 2.2f, 24f, 0, 0, 60, Behavior.BOSS_SUPREME, true, 0.05f, Sfx.DEATH_SHOOTER);

    public enum Behavior { CHASER, SHOOTER, DASHER, TANK, BOSS_KING, BOSS_ALPHA, BOSS_SUPREME }

    public final String sprite;
    public final float drawHeight, radius, hp, speed, contactDamage, knockback;
    public final int cost, minWave, xp;
    public final Behavior behavior;
    public final boolean boss;
    public final Sfx deathSfx;

    EnemyType(String sprite, float drawHeight, float radius, float hp, float speed, float contactDamage, int cost,
              int minWave, int xp, Behavior behavior, boolean boss, float knockback, Sfx deathSfx) {
        this.sprite = sprite;
        this.drawHeight = drawHeight;
        this.radius = radius;
        this.hp = hp;
        this.speed = speed;
        this.contactDamage = contactDamage;
        this.cost = cost;
        this.minWave = minWave;
        this.xp = xp;
        this.behavior = behavior;
        this.boss = boss;
        this.knockback = knockback;
        this.deathSfx = deathSfx;
    }

    public String displayName() {
        switch (this) {
            case KING: return "Cavaleiro-Rei";
            case ALPHA: return "Lobisomem Alfa";
            case SUPREME: return "Necromante Supremo";
            default: return name();
        }
    }
}
