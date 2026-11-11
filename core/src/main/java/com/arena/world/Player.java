package com.arena.world;

import com.badlogic.gdx.physics.box2d.Body;

public class Player {
    public static final float RADIUS = 0.5f;
    public static final float INVULNERABLE_TIME = 0.5f;

    public final PlayerStats stats = new PlayerStats();
    public Body body;
    public float hp = stats.maxHp;
    public float invulnerable;
    public float fireCooldown;
    public int level = 1;
    public int xp;

    public int xpToNext() { return 5 + level * 5; }

    public float x() { return body.getPosition().x; }

    public float y() { return body.getPosition().y; }
}
