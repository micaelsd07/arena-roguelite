package com.arena.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

public class Player {
    public static final float RADIUS = 0.4f;
    public static final float DRAW_HEIGHT = 1.45f;
    public static final float INVULNERABLE_TIME = 0.5f;
    public static final float DASH_TIME = 0.15f, DASH_DISTANCE = 4f, DASH_COOLDOWN = 1.5f, DASH_INVULNERABLE = 0.25f;

    public final PlayerStats stats = new PlayerStats();
    public Body body;
    public float hp = stats.maxHp;
    public float invulnerable;
    public float fireCooldown;
    public int level = 1;
    public int xp;

    public final Vector2 aim = new Vector2(1, 0);
    public final Vector2 moveDir = new Vector2();
    public boolean moving;
    public float animTime;
    public float shootAnim;

    public float dashTime, dashCooldown;
    public final Vector2 dashDir = new Vector2();

    public Special special;
    public int specialLevel;
    public float specialCooldown;
    public int shieldCharges;
    public float shieldTime;

    /** Quantas vezes cada melhoria foi escolhida (para o HUD). */
    public final int[] taken = new int[Upgrade.values().length];

    public int xpToNext() { return 5 + level * 5; }

    public boolean isDashing() { return dashTime > 0f; }

    public float x() { return body.getPosition().x; }

    public float y() { return body.getPosition().y; }
}
