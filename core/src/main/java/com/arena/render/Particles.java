package com.arena.render;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/** Sistema de particulas simples, tambem com object pooling. */
public class Particles {
    public static class P implements Pool.Poolable {
        public float x, y, vx, vy, life, max, size, endSize, r, g, b, a, drag, gravity, rot, rotSpeed;
        public boolean additive;
        public float light; // raio de luz emitida (0 = nenhuma)
        public TextureRegion region;

        @Override public void reset() { region = null; light = 0; additive = false; rot = rotSpeed = gravity = drag = 0; }
    }

    private final Pool<P> pool = new Pool<P>(512, 4000) {
        @Override protected P newObject() { return new P(); }
    };
    public final Array<P> active = new Array<>(false, 1024);

    public int freeCount() { return pool.getFree(); }

    public P spawn(TextureRegion region, float x, float y, float vx, float vy, float life, float size, float endSize,
                   float r, float g, float b, float a, boolean additive) {
        if (active.size > 3000) return null;
        P p = pool.obtain();
        p.region = region;
        p.x = x; p.y = y; p.vx = vx; p.vy = vy;
        p.life = p.max = life;
        p.size = size; p.endSize = endSize;
        p.r = r; p.g = g; p.b = b; p.a = a;
        p.additive = additive;
        p.drag = 3f;
        active.add(p);
        return p;
    }

    /** Explosao radial de particulas. */
    public void burst(TextureRegion region, float x, float y, int n, float speed, float life, float size,
                      float r, float g, float b, boolean additive) {
        for (int i = 0; i < n; i++) {
            float a = MathUtils.random(MathUtils.PI2), s = speed * MathUtils.random(0.3f, 1f);
            spawn(region, x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, life * MathUtils.random(0.6f, 1f),
                    size * MathUtils.random(0.6f, 1.2f), 0f, r, g, b, 1f, additive);
        }
    }

    public void update(float dt) {
        for (int i = active.size - 1; i >= 0; i--) {
            P p = active.get(i);
            p.life -= dt;
            if (p.life <= 0f) { active.removeIndex(i); pool.free(p); continue; }
            float d = (float) Math.exp(-p.drag * dt);
            p.vx *= d; p.vy = p.vy * d - p.gravity * dt;
            p.x += p.vx * dt; p.y += p.vy * dt;
            p.rot += p.rotSpeed * dt;
        }
    }

    public void draw(SpriteBatch batch, boolean additive) {
        for (P p : active) {
            if (p.additive != additive) continue;
            float t = p.life / p.max, s = p.endSize + (p.size - p.endSize) * t;
            batch.setColor(p.r, p.g, p.b, p.a * Math.min(1f, t * 2f));
            float w = s * p.region.getRegionWidth() / Math.max(p.region.getRegionWidth(), p.region.getRegionHeight());
            float h = s * p.region.getRegionHeight() / Math.max(p.region.getRegionWidth(), p.region.getRegionHeight());
            batch.draw(p.region, p.x - w / 2, p.y - h / 2, w / 2, h / 2, w, h, 1, 1, p.rot);
        }
    }

    public void clear() {
        pool.freeAll(active);
        active.clear();
    }
}
