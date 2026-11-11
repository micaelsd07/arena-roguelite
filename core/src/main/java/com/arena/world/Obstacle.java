package com.arena.world;

import com.badlogic.gdx.physics.box2d.Body;

/** Obstaculo solido do bioma (pilar, arvore, sarcofago...). Circulo (hw = raio, hh = 0) ou caixa. */
public class Obstacle {
    public final float x, y, hw, hh;
    public final String sprite;
    public final int frames;
    public Body body;

    public Obstacle(float x, float y, float hw, float hh, String sprite, int frames) {
        this.x = x; this.y = y; this.hw = hw; this.hh = hh; this.sprite = sprite; this.frames = frames;
    }

    public boolean isCircle() { return hh <= 0f; }

    /** Distancia com sinal (negativa = dentro) e normal de saida, para colisao manual de projeteis. */
    public float penetration(float px, float py, float r, float[] normalOut) {
        if (isCircle()) {
            float dx = px - x, dy = py - y, d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < 1e-4f) { dx = 1; d = 1; }
            normalOut[0] = dx / d; normalOut[1] = dy / d;
            return d - (hw + r);
        }
        float cx = Math.max(x - hw, Math.min(px, x + hw)), cy = Math.max(y - hh, Math.min(py, y + hh));
        float dx = px - cx, dy = py - cy, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 1e-4f) { normalOut[0] = dx / d; normalOut[1] = dy / d; return d - r; }
        // centro dentro da caixa: sai pelo lado mais proximo
        float l = px - (x - hw), rr = (x + hw) - px, b = py - (y - hh), t = (y + hh) - py;
        float m = Math.min(Math.min(l, rr), Math.min(b, t));
        normalOut[0] = m == l ? -1 : m == rr ? 1 : 0;
        normalOut[1] = m == b ? -1 : m == t ? 1 : 0;
        return -m - r;
    }
}
