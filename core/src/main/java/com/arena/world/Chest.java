package com.arena.world;

/** Bau deixado pelo chefe: ao tocar, concede uma melhoria garantida. */
public class Chest {
    public static final float AUTO_OPEN = 20f;
    public float x, y, age, openTimer = -1f;

    public Chest(float x, float y) { this.x = x; this.y = y; }

    public boolean isOpen() { return openTimer >= 0f; }
}
