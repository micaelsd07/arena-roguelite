package com.arena.world;

/** Decoracao sem colisao: decalques no chao, props, tochas na parede e fontes de luz. */
public class Decor {
    public enum Layer { FLOOR, PROP, WALL }

    public final Layer layer;
    public final String sprite;
    public final int index;      // variante (-1 = animado pelo numero de frames)
    public final int frames;
    public final float x, y;
    public final float lightRadius, lr, lg, lb;
    public final boolean flicker;

    public Decor(Layer layer, String sprite, int index, int frames, float x, float y,
                 float lightRadius, float lr, float lg, float lb, boolean flicker) {
        this.layer = layer; this.sprite = sprite; this.index = index; this.frames = frames;
        this.x = x; this.y = y; this.lightRadius = lightRadius; this.lr = lr; this.lg = lg; this.lb = lb;
        this.flicker = flicker;
    }

    public static Decor of(Layer layer, String sprite, int index, float x, float y) {
        return new Decor(layer, sprite, index, 1, x, y, 0, 0, 0, 0, false);
    }
}
