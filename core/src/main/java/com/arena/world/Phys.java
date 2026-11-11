package com.arena.world;

/** Categorias de colisao do Box2D (bits de filtro) e dimensoes da arena, em metros. */
public final class Phys {
    public static final short WALL = 0x0001;
    public static final short PLAYER = 0x0002;
    public static final short ENEMY = 0x0004;
    public static final short PLAYER_BULLET = 0x0008;
    public static final short ENEMY_BULLET = 0x0010;

    public static final short MASK_WALL = PLAYER | ENEMY;
    public static final short MASK_PLAYER = WALL | ENEMY | ENEMY_BULLET;
    public static final short MASK_ENEMY = WALL | PLAYER | ENEMY | PLAYER_BULLET;
    public static final short MASK_PLAYER_BULLET = ENEMY;
    public static final short MASK_ENEMY_BULLET = PLAYER;

    public static final float ARENA_W = 40f;
    public static final float ARENA_H = 25f;
    public static final float STEP = 1f / 60f;

    private Phys() {}
}
