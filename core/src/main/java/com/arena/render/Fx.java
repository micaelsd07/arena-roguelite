package com.arena.render;

import com.arena.world.Biome;
import com.arena.world.Enemy;
import com.arena.world.WorldEvents;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;

/** Recebe os eventos da logica e os transforma em "juice": particulas, numeros, tremor, raios, rastros. */
public class Fx implements WorldEvents {
    public static class Number implements Pool.Poolable {
        public float x, y, vy, life, value;
        public boolean big;
        @Override public void reset() { }
    }

    public static class Ghost implements Pool.Poolable {
        public float x, y, life;
        @Override public void reset() { }
    }

    public static class Bolt {
        public final FloatArray points = new FloatArray();
        public float life;
    }

    private final Assets assets;
    public final Particles particles = new Particles();
    public final Array<Number> numbers = new Array<>(false, 128);
    public final Array<Ghost> ghosts = new Array<>(false, 16);
    public final Array<Bolt> bolts = new Array<>(false, 4);
    private final Pool<Number> numberPool = new Pool<Number>(64) { @Override protected Number newObject() { return new Number(); } };
    private final Pool<Ghost> ghostPool = new Pool<Ghost>(16) { @Override protected Ghost newObject() { return new Ghost(); } };
    public float trauma;
    public float hurtFlash;
    public float biomeBanner;
    public Biome bannerBiome;

    public Fx(Assets assets) { this.assets = assets; }

    public void update(float dt) {
        particles.update(dt);
        trauma = Math.max(0f, trauma - dt * 1.6f);
        hurtFlash = Math.max(0f, hurtFlash - dt * 2.5f);
        biomeBanner = Math.max(0f, biomeBanner - dt);
        for (int i = numbers.size - 1; i >= 0; i--) {
            Number n = numbers.get(i);
            n.life -= dt;
            n.y += n.vy * dt;
            n.vy *= (float) Math.exp(-4f * dt);
            if (n.life <= 0f) { numbers.removeIndex(i); numberPool.free(n); }
        }
        for (int i = ghosts.size - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.life -= dt;
            if (g.life <= 0f) { ghosts.removeIndex(i); ghostPool.free(g); }
        }
        for (int i = bolts.size - 1; i >= 0; i--) if ((bolts.get(i).life -= dt) <= 0f) bolts.removeIndex(i);
    }

    public void reset() {
        particles.clear();
        numberPool.freeAll(numbers); numbers.clear();
        ghostPool.freeAll(ghosts); ghosts.clear();
        bolts.clear();
        trauma = hurtFlash = biomeBanner = 0;
    }

    // ---------------------------------------------------------------- eventos

    @Override public void hit(float x, float y, float dx, float dy, boolean enemyHit) {
        for (int i = 0; i < 5; i++) {
            float a = MathUtils.atan2(-dy, -dx) + MathUtils.random(-0.9f, 0.9f), s = MathUtils.random(3f, 7f);
            Particles.P p = particles.spawn(assets.pixel, x, y, MathUtils.cos(a) * s, MathUtils.sin(a) * s, 0.25f, 0.12f, 0.02f,
                    enemyHit ? 1f : 1f, enemyHit ? 0.85f : 0.4f, enemyHit ? 0.5f : 0.3f, 1f, true);
            if (p != null) p.drag = 6f;
        }
    }

    @Override public void damageNumber(float x, float y, float amount, boolean big) {
        if (numbers.size > 120) return;
        Number n = numberPool.obtain();
        n.x = x + MathUtils.random(-0.25f, 0.25f);
        n.y = y;
        n.vy = 3f;
        n.life = 0.7f;
        n.value = amount;
        n.big = big;
        numbers.add(n);
    }

    @Override public void enemyDied(Enemy e) {
        float x = e.x(), y = e.y() + e.type.drawHeight * 0.35f;
        float scale = e.type.boss ? 3f : 1f;
        switch (e.type.sprite) {
            case "wraith":
                particles.burst(assets.soft, x, y, 18, 4f, 0.8f, 0.6f, 0.45f, 0.25f, 0.55f, false);
                particles.burst(assets.soft, x, y, 6, 2f, 0.5f, 0.4f, 1f, 0.2f, 0.2f, true);
                break;
            case "werewolf":
                particles.burst(assets.pixel, x, y, (int) (16 * scale), 7f * scale, 0.6f, 0.16f, 0.55f, 0.08f, 0.1f, false);
                particles.burst(assets.soft, x, y, 8, 3f, 0.6f, 0.5f * scale, 0.35f, 0.35f, 0.4f, false);
                break;
            case "necro":
                particles.burst(assets.pixel, x, y, (int) (14 * scale), 6f * scale, 0.7f, 0.15f, 0.85f, 0.8f, 0.65f, false);
                particles.burst(assets.soft, x, y, 10, 3f, 0.9f, 0.5f * scale, 0.5f, 1f, 0.5f, true);
                break;
            default:
                particles.burst(assets.pixel, x, y, (int) (18 * scale), 8f * scale, 0.5f, 0.14f, 0.75f, 0.75f, 0.8f, true);
                particles.burst(assets.soft, x, y, 8, 3f, 0.7f, 0.6f * scale, 0.55f, 0.25f, 0.8f, true);
        }
        if (e.type.boss) explosion(x, y, 3f);
    }

    @Override public void explosion(float x, float y, float radius) {
        particles.burst(assets.soft, x, y, 26, radius * 6f, 0.55f, radius * 0.7f, 1f, 0.55f, 0.15f, true);
        particles.burst(assets.soft, x, y, 14, radius * 2.5f, 1.0f, radius * 0.9f, 0.25f, 0.2f, 0.22f, false);
        Particles.P flash = particles.spawn(assets.soft, x, y, 0, 0, 0.25f, radius * 3f, radius * 4f, 1f, 0.9f, 0.6f, 1f, true);
        if (flash != null) flash.light = radius * 4f;
    }

    @Override public void shake(float t) { trauma = Math.min(1f, trauma + t); }

    @Override public void lightning(FloatArray pts) {
        Bolt b = bolts.size < 4 ? new Bolt() : bolts.removeIndex(0);
        b.points.clear();
        // Segmentos com zigue-zague
        for (int i = 0; i + 3 < pts.size; i += 2) {
            float x0 = pts.get(i), y0 = pts.get(i + 1), x1 = pts.get(i + 2), y1 = pts.get(i + 3);
            int segs = 6;
            for (int s = 0; s < segs; s++) {
                float t = s / (float) segs;
                float jx = s == 0 ? 0 : MathUtils.random(-0.35f, 0.35f), jy = s == 0 ? 0 : MathUtils.random(-0.35f, 0.35f);
                b.points.add(x0 + (x1 - x0) * t + jx, y0 + (y1 - y0) * t + jy);
            }
            particles.burst(assets.soft, x1, y1, 6, 4f, 0.3f, 0.4f, 1f, 0.95f, 0.5f, true);
        }
        b.points.add(pts.get(pts.size - 2), pts.get(pts.size - 1));
        b.life = 0.25f;
        bolts.add(b);
    }

    @Override public void dashGhost(float x, float y) {
        Ghost g = ghostPool.obtain();
        g.x = x; g.y = y; g.life = 0.25f;
        ghosts.add(g);
        Particles.P p = particles.spawn(assets.soft, x, y - 0.3f, 0, 0, 0.35f, 0.5f, 0.1f, 0.6f, 0.6f, 0.65f, 0.6f, false);
        if (p != null) p.drag = 0;
    }

    @Override public void wallSpark(float x, float y) {
        particles.burst(assets.pixel, x, y, 4, 4f, 0.2f, 0.1f, 1f, 0.8f, 0.4f, true);
    }

    @Override public void playerHurt(float x, float y) {
        hurtFlash = 1f;
        particles.burst(assets.pixel, x, y + 0.5f, 10, 5f, 0.4f, 0.14f, 0.85f, 0.1f, 0.1f, false);
    }

    @Override public void chestOpened(float x, float y) {
        particles.burst(assets.soft, x, y + 0.4f, 30, 6f, 1.0f, 0.35f, 1f, 0.85f, 0.3f, true);
        Particles.P f = particles.spawn(assets.soft, x, y, 0, 0, 0.6f, 2f, 5f, 1f, 0.9f, 0.5f, 1f, true);
        if (f != null) f.light = 8f;
    }

    @Override public void biomeChanged(Biome biome) {
        particles.clear();
        bannerBiome = biome;
        biomeBanner = 3.5f;
    }

    @Override public void nova(float x, float y) {
        Particles.P ring = particles.spawn(assets.ring, x, y, 0, 0, 0.4f, 0.5f, 8f, 1f, 0.85f, 0.5f, 1f, true);
        if (ring != null) ring.light = 5f;
    }

    @Override public void shieldBreak(float x, float y) {
        particles.burst(assets.pixel, x, y + 0.5f, 16, 6f, 0.5f, 0.15f, 0.6f, 0.9f, 1f, true);
    }

    @Override public void bossSlam(float x, float y) {
        particles.spawn(assets.ring, x, y, 0, 0, 0.5f, 1f, 9f, 0.8f, 0.4f, 1f, 1f, true);
        particles.burst(assets.soft, x, y, 20, 8f, 0.6f, 0.8f, 0.35f, 0.3f, 0.35f, false);
    }
}
