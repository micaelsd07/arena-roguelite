package com.arena.render;

import com.arena.world.*;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;

/** Desenha o mundo em metros: chao, paredes, props e personagens ordenados por Y, efeitos e luzes. */
public class WorldRenderer {
    private static final float PPM = Assets.PPM;

    private final Assets assets;
    private final Fx fx;
    public final Lighting lighting = new Lighting();
    private final Array<Object> sorted = new Array<>(false, 512);
    private final FloatArray sortKeys = new FloatArray(512);
    private float time;

    public WorldRenderer(Assets assets, Fx fx) {
        this.assets = assets;
        this.fx = fx;
    }

    public void render(SpriteBatch batch, GameWorld w, OrthographicCamera cam, float dt, int vx, int vy, int vw, int vh) {
        time += dt;
        Biome biome = w.biome;
        lighting.clear();
        lighting.ambR = biome.ambR; lighting.ambG = biome.ambG; lighting.ambB = biome.ambB;

        float left = cam.position.x - cam.viewportWidth / 2f - 1, right = cam.position.x + cam.viewportWidth / 2f + 1;
        float bottom = cam.position.y - cam.viewportHeight / 2f - 1, top = cam.position.y + cam.viewportHeight / 2f + 1;

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        drawFloor(batch, biome, left, right, bottom, top);
        for (Decor d : w.decor) if (d.layer == Decor.Layer.FLOOR && !d.sprite.isEmpty()) drawDecor(batch, d);
        drawWalls(batch, biome, left, right, bottom, top);
        for (Decor d : w.decor) {
            if (d.layer == Decor.Layer.WALL) drawDecor(batch, d);
            if (d.lightRadius > 0f) {
                float f = d.flicker ? 0.88f + 0.08f * MathUtils.sin(time * 9f + d.x) + 0.05f * MathUtils.sin(time * 23f + d.y) : 1f;
                lighting.add(d.x, d.y, d.lightRadius * f, d.lr, d.lg, d.lb);
            }
        }

        drawGroundLayer(batch, w);
        drawSorted(batch, w);
        drawProjectiles(batch, w);
        fx.particles.draw(batch, false);
        batch.end();

        addDynamicLights(w);
        for (Particles.P p : fx.particles.active) if (p.light > 0f) lighting.add(p.x, p.y, p.light * p.life / p.max, p.r, p.g, p.b);
        lighting.apply(batch, cam, assets.light, vx, vy, vw, vh);

        // Depois da luz: brilhos aditivos, raios, rastros e o flash branco de dano
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        drawGlows(batch, w);
        fx.particles.draw(batch, true);
        drawBolts(batch);
        drawGhosts(batch, w);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        drawShield(batch, w);
        batch.end();

        batch.setShader(assets.flashShader);
        batch.begin();
        for (Enemy e : w.enemies)
            if (e.flash > 0f) drawEnemy(batch, w, e, Math.min(1f, e.flash / 0.09f) * (e.type.boss ? 0.3f : 0.6f), true);
        batch.end();
        batch.setShader(null);
        batch.setColor(1, 1, 1, 1);
    }

    // ================================================================== cenario

    private static int hash(int x, int y) {
        int h = x * 73856093 ^ y * 19349663;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return (h ^ (h >>> 15)) & 0x7fffffff;
    }

    private void drawFloor(SpriteBatch batch, Biome b, float l, float r, float bo, float t) {
        Array<TextureRegion> tiles = assets.frames("floor_" + b.key);
        batch.setColor(1, 1, 1, 1);
        int x0 = Math.max(0, (int) l), x1 = Math.min((int) Phys.ARENA_W, (int) r + 1);
        int y0 = Math.max(0, (int) bo), y1 = Math.min((int) Phys.ARENA_H, (int) t + 1);
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                int h = hash(x, y);
                int v = h % 10 < 6 ? 0 : 1 + (h % 3);
                batch.draw(tiles.get(Math.min(v, tiles.size - 1)), x, y, 1, 1);
            }
        // Sombra da parede de cima projetada no chao
        batch.setColor(0, 0, 0, 0.45f);
        batch.draw(assets.pixel, 0, Phys.ARENA_H - 0.35f, Phys.ARENA_W, 0.35f);
        batch.setColor(0, 0, 0, 0.25f);
        batch.draw(assets.pixel, 0, Phys.ARENA_H - 0.8f, Phys.ARENA_W, 0.45f);
        batch.setColor(1, 1, 1, 1);
    }

    private void drawWalls(SpriteBatch batch, Biome b, float l, float r, float bo, float t) {
        Array<TextureRegion> wall = assets.frames("wall_" + b.key);
        TextureRegion cap = assets.region("wallcap_" + b.key);
        int x0 = Math.max(-1, (int) l - 1), x1 = Math.min((int) Phys.ARENA_W + 1, (int) r + 1);
        // Face da parede de cima (2 tiles) + topo
        for (int x = x0; x < x1; x++) {
            for (int row = 0; row < 2; row++) {
                batch.setColor(row == 0 ? 0.8f : 1f, row == 0 ? 0.8f : 1f, row == 0 ? 0.8f : 1f, 1f);
                batch.draw(wall.get(hash(x, row) % 7 == 0 ? 1 : 0), x, Phys.ARENA_H + row, 1, 1);
            }
            batch.setColor(1, 1, 1, 1);
            batch.draw(cap, x, Phys.ARENA_H + 2, 1, 0.6f);
            batch.draw(cap, x, -1f, 1, 1);
        }
        for (float y = Math.max(-1, (int) bo - 1); y < Math.min(Phys.ARENA_H + 2.6f, t + 1); y++) {
            batch.draw(cap, -1f, y, 1, 1);
            batch.draw(cap, Phys.ARENA_W, y, 1, 1);
        }
        // Borda clara nas laterais para ler o limite da arena
        batch.setColor(0, 0, 0, 0.35f);
        batch.draw(assets.pixel, 0, 0, 0.2f, Phys.ARENA_H);
        batch.draw(assets.pixel, Phys.ARENA_W - 0.2f, 0, 0.2f, Phys.ARENA_H);
        batch.draw(assets.pixel, 0, 0, Phys.ARENA_W, 0.2f);
        batch.setColor(1, 1, 1, 1);
    }

    private TextureRegion decorRegion(Decor d) {
        if (d.frames > 1) return assets.frame(d.sprite, time + d.x * 0.37f, 8f);
        if (d.index >= 0) return assets.frames(d.sprite).get(Math.min(d.index, assets.frames(d.sprite).size - 1));
        Array<TextureRegion> all = assets.frames(d.sprite);
        return all.get(hash((int) (d.x * 10), (int) (d.y * 10)) % all.size);
    }

    private void drawDecor(SpriteBatch batch, Decor d) {
        TextureRegion r = decorRegion(d);
        float w = r.getRegionWidth() / PPM, h = r.getRegionHeight() / PPM;
        batch.setColor(1, 1, 1, 1);
        if (d.layer == Decor.Layer.FLOOR) batch.draw(r, d.x - w / 2, d.y - h / 2, w, h);
        else batch.draw(r, d.x - w / 2, d.y - 0.2f, w, h);
    }

    // ================================================================== camada do chao

    private void drawGroundLayer(SpriteBatch batch, GameWorld w) {
        // Sombras
        batch.setColor(1, 1, 1, w.player.isDashing() ? 0.5f : 0.9f);
        shadow(batch, w.player.x(), w.player.y() - Player.RADIUS * 0.7f, Player.RADIUS * 2.4f);
        for (Enemy e : w.enemies) {
            float hover = e.type == EnemyType.WRAITH ? 0.6f : 1f;
            batch.setColor(1, 1, 1, Math.min(1f, e.age * 3f) * hover);
            shadow(batch, e.x(), e.y() - e.type.radius * 0.7f, e.type.radius * 2.6f);
        }
        batch.setColor(1, 1, 1, 1);

        // Gemas
        TextureRegion gem = assets.region("gem");
        for (Gem g : w.gems) {
            float s = 0.32f + 0.05f * Math.min(g.value, 5);
            float bob = 0.08f * MathUtils.sin(time * 4f + g.x * 3f);
            batch.draw(gem, g.x - s * 0.375f, g.y - s / 2 + bob, s * 0.75f, s);
        }

        // Portais de surgimento
        TextureRegion portal = assets.region("portal");
        for (SpawnMarker m : w.markers) {
            float dur = SpawnMarker.DURATION * (m.type.boss ? 2f : 1f);
            float t = 1f - m.timer / dur, s = (m.type.boss ? 4f : 1.6f) * Interp.backOut(Math.min(1f, t * 1.6f));
            batch.setColor(1, 1, 1, 0.9f);
            batch.draw(portal, m.x - s / 2, m.y - s / 2 * 0.6f, s / 2, s / 2 * 0.6f, s, s * 0.6f, 1, 1, time * 120f);
        }
        batch.setColor(1, 1, 1, 1);

        // Aviso no chao dos meteoros
        for (Meteor m : w.meteors) {
            float t = 1f - m.timer / m.total;
            batch.setColor(1f, 0.3f, 0.1f, 0.25f + 0.4f * t);
            float s = m.radius * 2f * (1.4f - 0.4f * t);
            batch.draw(assets.ring, m.x - s / 2, m.y - s / 2, s, s);
            batch.setColor(0, 0, 0, 0.5f * t);
            batch.draw(assets.shadow, m.x - s / 2 * t, m.y - s / 6 * t, s * t, s / 3 * t);
        }
        batch.setColor(1, 1, 1, 1);
    }

    private void shadow(SpriteBatch batch, float x, float y, float w) {
        batch.draw(assets.shadow, x - w / 2, y - w * 0.18f, w, w * 0.36f);
    }

    // ================================================================== Y-sort

    private void drawSorted(SpriteBatch batch, GameWorld w) {
        sorted.clear();
        sortKeys.clear();
        for (Obstacle o : w.obstacles) add(o, o.y);
        for (Decor d : w.decor) if (d.layer == Decor.Layer.PROP) add(d, d.y);
        for (Chest c : w.chests) add(c, c.y);
        for (Enemy e : w.enemies) add(e, e.y());
        add(w.player, w.player.y());
        // ordenacao por insercao (lista quase ordenada entre frames)
        for (int i = 1; i < sorted.size; i++) {
            float k = sortKeys.get(i);
            Object o = sorted.get(i);
            int j = i - 1;
            while (j >= 0 && sortKeys.get(j) < k) {
                sortKeys.set(j + 1, sortKeys.get(j));
                sorted.set(j + 1, sorted.get(j));
                j--;
            }
            sortKeys.set(j + 1, k);
            sorted.set(j + 1, o);
        }
        for (Object o : sorted) {
            if (o instanceof Enemy) drawEnemy(batch, w, (Enemy) o, 1f, false);
            else if (o instanceof Player) drawPlayer(batch, (Player) o);
            else if (o instanceof Obstacle) drawObstacle(batch, (Obstacle) o);
            else if (o instanceof Decor) drawDecor(batch, (Decor) o);
            else if (o instanceof Chest) drawChest(batch, (Chest) o);
        }
        batch.setColor(1, 1, 1, 1);
    }

    private void add(Object o, float y) { sorted.add(o); sortKeys.add(y); }

    private void drawObstacle(SpriteBatch batch, Obstacle o) {
        TextureRegion r = o.frames > 1 ? assets.frame(o.sprite.replaceAll("\\d$", ""), time + o.x, 8f) : spriteFor(o.sprite);
        float w = r.getRegionWidth() / PPM, h = r.getRegionHeight() / PPM;
        float base = o.y - (o.isCircle() ? o.hw * 0.9f : o.hh + 0.15f);
        batch.setColor(1, 1, 1, 1);
        batch.draw(r, o.x - w / 2, base, w, h);
    }

    /** "prop_rock0" = regiao prop_rock indice 0. */
    private TextureRegion spriteFor(String name) {
        char last = name.charAt(name.length() - 1);
        if (Character.isDigit(last)) {
            Array<TextureRegion> all = assets.frames(name.substring(0, name.length() - 1));
            return all.get(Math.min(last - '0', all.size - 1));
        }
        return assets.region(name);
    }

    private void drawChest(SpriteBatch batch, Chest c) {
        TextureRegion r = assets.frames("chest").get(c.isOpen() ? 1 : 0);
        float w = r.getRegionWidth() / PPM * 1.3f, h = r.getRegionHeight() / PPM * 1.3f;
        float bounce = c.isOpen() ? 0 : Math.abs(MathUtils.sin(time * 3f)) * 0.12f;
        batch.setColor(1, 1, 1, c.isOpen() ? Math.max(0f, 1f - (c.openTimer - 1.5f)) : 1f);
        batch.draw(r, c.x - w / 2, c.y - 0.4f + bounce, w, h);
        batch.setColor(1, 1, 1, 1);
    }

    // ================================================================== personagens

    private void character(SpriteBatch batch, TextureRegion r, float x, float footY, float height,
                           boolean flip, float sx, float sy, float rot) {
        float w = height * r.getRegionWidth() / r.getRegionHeight();
        batch.draw(r, x - w / 2, footY, w / 2, 0, w, height, flip ? -sx : sx, sy, rot);
    }

    private TextureRegion playerRegion(Player p) {
        float ax = p.aim.x, ay = p.aim.y;
        String dir = Math.abs(ay) > Math.abs(ax) * 1.1f ? (ay > 0 ? "hero_up" : "hero_down") : "hero_side";
        Array<TextureRegion> frames = assets.frames(dir);
        int f = p.moving ? ((int) (p.animTime * 8f)) % frames.size : 0;
        return frames.get(f);
    }

    private void drawPlayer(SpriteBatch batch, Player p) {
        if (p.invulnerable > 0f && !p.isDashing() && ((int) (p.invulnerable * 20f)) % 2 == 1) return;
        TextureRegion r = playerRegion(p);
        boolean side = Math.abs(p.aim.y) <= Math.abs(p.aim.x) * 1.1f;
        boolean flip = side && p.aim.x < 0;
        float bob = p.moving ? Math.abs(MathUtils.sin(p.animTime * 12f)) * 0.07f : 0f;
        float breathe = p.moving ? 1f : 1f + 0.02f * MathUtils.sin(p.animTime * 3f);
        float sx = 1f, sy = breathe;
        if (p.shootAnim > 0f) { sx = 1.05f; sy *= 0.96f; }
        float lean = p.isDashing() ? -p.dashDir.x * 12f : p.moving ? -p.moveDir.x * 4f : 0f;
        batch.setColor(1, 1, 1, 1);
        character(batch, r, p.x(), p.y() - Player.RADIUS * 0.7f + bob, Player.DRAW_HEIGHT, flip, sx, sy, lean);
    }

    private static final float[][] BOSS_TINT = {{1f, 0.82f, 0.5f}, {0.72f, 0.74f, 0.95f}, {0.95f, 0.6f, 1f}};

    void drawEnemy(SpriteBatch batch, GameWorld w, Enemy e, float alpha, boolean flashPass) {
        EnemyType t = e.type;
        String s = t.sprite;
        TextureRegion r;
        boolean flip = e.facingLeft;
        float sx = 1f, sy = 1f, rot = 0f, lift = 0f, jitter = 0f;
        float dist2 = com.badlogic.gdx.math.Vector2.dst2(e.x(), e.y(), w.player.x(), w.player.y());

        if (s.equals("wraith")) {
            boolean attacking = dist2 < 2.6f;
            r = attacking ? assets.region("wraith_attack") : assets.frame("wraith", e.animTime, 3f);
            flip = attacking && e.facingLeft;
            lift = 0.18f + 0.1f * MathUtils.sin(e.animTime * 3f + e.x());
        } else if (s.equals("werewolf")) {
            if (e.state == 1) { r = assets.region("werewolf_front"); flip = false; jitter = 0.05f; }
            else if (e.state == 2) { r = assets.frames("werewolf").get(1); sx = 1.15f; sy = 0.9f; }
            else r = assets.frame("werewolf", e.animTime, 10f);
        } else if (s.equals("necro")) {
            if (e.castTimer > 0f) r = assets.region("necro_cast");
            else { r = assets.region("necro"); flip = false; }
            lift = 0.04f * MathUtils.sin(e.animTime * 2.5f);
        } else { // cavaleiro
            r = assets.frame("knight", e.animTime, 6f);
            if (e.state == 1) jitter = 0.05f;
        }

        float spawn = Math.min(1f, e.age / 0.3f);
        float pop = Interp.backOut(spawn);
        float h = t.drawHeight;
        float x = e.x() + (jitter > 0 ? MathUtils.random(-jitter, jitter) : 0f);
        float foot = e.y() - t.radius * 0.7f + lift;

        if (flashPass) batch.setColor(1f, 1f, 1f, alpha);
        else {
            float cr = w.biome.tintR, cg = w.biome.tintG, cb = w.biome.tintB;
            if (t.boss) { float[] bt = BOSS_TINT[t.ordinal() - EnemyType.KING.ordinal()]; cr *= bt[0]; cg *= bt[1]; cb *= bt[2]; }
            if (e.slowTimer > 0f) { cr *= 0.55f; cg *= 0.85f; cb *= 1f; }
            batch.setColor(cr, cg, cb, spawn);
        }
        character(batch, r, x, foot, h, flip, sx * pop, sy * pop, rot);
    }

    // ================================================================== projeteis

    private void drawProjectiles(SpriteBatch batch, GameWorld w) {
        TextureRegion arrow = assets.region("arrow");
        float aw = 0.8f, ah = aw * arrow.getRegionHeight() / arrow.getRegionWidth();
        for (Bullet b : w.bullets) {
            if (b.enemyOwned) {
                TextureRegion fb = assets.frame("fireball", time + b.time, 12f);
                float s = b.radius * 3.2f;
                batch.setColor(1, 1, 1, 1);
                batch.draw(fb, b.x() - s / 2, b.y() - s / 2, s / 2, s / 2, s, s, 1, 1, time * 400f);
            } else {
                float ang = MathUtils.atan2(b.body.getLinearVelocity().y, b.body.getLinearVelocity().x) * MathUtils.radDeg;
                batch.setColor(1, 1, 1, 1);
                batch.draw(arrow, b.x() - aw / 2, b.y() - ah / 2, aw / 2, ah / 2, aw, ah, 1, 1, ang);
            }
        }
        batch.setColor(0.6f, 0.95f, 1f, 1f);
        for (Bullet b : w.orbitBullets) {
            float ang = b.dir.angleDeg();
            batch.draw(arrow, b.x() - aw / 2, b.y() - ah / 2, aw / 2, ah / 2, aw, ah, 1.3f, 1.3f, ang);
        }
        // Meteoros caindo
        TextureRegion met = assets.region("meteor");
        batch.setColor(1, 1, 1, 1);
        for (Meteor m : w.meteors) {
            float t = m.timer / m.total;
            float mx = m.x + t * 4f, my = m.y + t * 9f, s = 0.9f;
            batch.draw(met, mx - s / 2, my - s / 2, s / 2, s / 2, s, s, 1, 1, time * 300f);
            if (MathUtils.randomBoolean(0.6f)) {
                Particles.P p = fx.particles.spawn(assets.soft, mx, my, MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f),
                        0.4f, 0.5f, 0.1f, 1f, 0.5f, 0.15f, 1f, true);
                if (p != null) p.drag = 1f;
            }
        }
        batch.setColor(1, 1, 1, 1);
    }

    // ================================================================== luzes e brilhos

    private void addDynamicLights(GameWorld w) {
        Player p = w.player;
        lighting.add(p.x(), p.y() + 0.3f, 7.5f, 1f, 0.86f, 0.65f);
        for (Bullet b : w.bullets) {
            if (b.enemyOwned) lighting.add(b.x(), b.y(), 2.0f, 1f, 0.45f, 0.15f);
            else lighting.add(b.x(), b.y(), 1.1f, 1f, 0.8f, 0.5f);
        }
        for (Bullet b : w.orbitBullets) lighting.add(b.x(), b.y(), 1.6f, 0.4f, 0.85f, 1f);
        for (Gem g : w.gems) lighting.add(g.x, g.y, 1.0f, 0.3f, 0.8f, 1f);
        for (SpawnMarker m : w.markers) lighting.add(m.x, m.y, m.type.boss ? 6f : 2.6f, 0.65f, 0.3f, 1f);
        for (Chest c : w.chests) lighting.add(c.x, c.y, 3.5f, 1f, 0.8f, 0.35f);
        for (Meteor m : w.meteors) lighting.add(m.x + m.timer / m.total * 4f, m.y + m.timer / m.total * 9f, 2.5f, 1f, 0.5f, 0.15f);
        for (Enemy e : w.enemies) {
            if (e.type == EnemyType.NECRO && e.castTimer > 0f) lighting.add(e.x(), e.y() + 0.8f, 2.5f, 1f, 0.45f, 0.15f);
            if (e.type.boss) lighting.add(e.x(), e.y() + 1f, 6f, 0.6f, 0.3f, 0.8f);
        }
        if (p.shieldCharges > 0) lighting.add(p.x(), p.y() + 0.5f, 3.5f, 0.4f, 0.8f, 1f);
        for (Fx.Bolt b : fx.bolts)
            for (int i = 0; i < b.points.size; i += 4) lighting.add(b.points.get(i), b.points.get(i + 1), 4f, 0.9f, 0.9f, 0.6f);
    }

    private void drawGlows(SpriteBatch batch, GameWorld w) {
        TextureRegion soft = assets.soft;
        for (Bullet b : w.bullets) {
            if (!b.enemyOwned) continue;
            batch.setColor(1f, 0.4f, 0.1f, 0.6f);
            float s = b.radius * 5f;
            batch.draw(soft, b.x() - s / 2, b.y() - s / 2, s, s);
        }
        batch.setColor(0.4f, 0.85f, 1f, 0.5f);
        for (Bullet b : w.orbitBullets) batch.draw(soft, b.x() - 0.5f, b.y() - 0.5f, 1f, 1f);
        batch.setColor(0.3f, 0.8f, 1f, 0.35f);
        for (Gem g : w.gems) batch.draw(soft, g.x - 0.35f, g.y - 0.35f, 0.7f, 0.7f);
        for (Enemy e : w.enemies) {
            if (e.type == EnemyType.WRAITH) { // olhos vermelhos brilhando
                batch.setColor(1f, 0.15f, 0.1f, 0.35f);
                float ey = e.y() - e.type.radius * 0.7f + 0.18f + 0.1f * MathUtils.sin(e.animTime * 3f + e.x()) + e.type.drawHeight * 0.68f;
                batch.draw(soft, e.x() - 0.3f, ey - 0.3f, 0.6f, 0.6f);
            }
            if (e.type.boss) {
                batch.setColor(0.7f, 0.25f, 1f, 0.25f + 0.1f * MathUtils.sin(time * 4f));
                float s = e.type.radius * 4f;
                batch.draw(soft, e.x() - s / 2, e.y() - s / 2, s, s);
            }
        }
        batch.setColor(1, 1, 1, 1);
    }

    private void drawBolts(SpriteBatch batch) {
        for (Fx.Bolt b : fx.bolts) {
            float a = Math.min(1f, b.life * 6f);
            for (int i = 0; i + 3 < b.points.size; i += 2) {
                float x0 = b.points.get(i), y0 = b.points.get(i + 1), x1 = b.points.get(i + 2), y1 = b.points.get(i + 3);
                float len = (float) Math.hypot(x1 - x0, y1 - y0), ang = MathUtils.atan2(y1 - y0, x1 - x0) * MathUtils.radDeg;
                batch.setColor(1f, 0.85f, 0.3f, 0.7f * a);
                batch.draw(assets.pixel, x0, y0 - 0.12f, 0, 0.12f, len, 0.24f, 1, 1, ang);
                batch.setColor(1f, 1f, 0.9f, a);
                batch.draw(assets.pixel, x0, y0 - 0.04f, 0, 0.04f, len, 0.08f, 1, 1, ang);
            }
        }
        batch.setColor(1, 1, 1, 1);
    }

    private void drawGhosts(SpriteBatch batch, GameWorld w) {
        TextureRegion r = playerRegion(w.player);
        boolean flip = Math.abs(w.player.aim.y) <= Math.abs(w.player.aim.x) * 1.1f && w.player.aim.x < 0;
        for (Fx.Ghost g : fx.ghosts) {
            batch.setColor(0.4f, 0.7f, 1f, g.life * 2f);
            character(batch, r, g.x, g.y - Player.RADIUS * 0.7f, Player.DRAW_HEIGHT, flip, 1, 1, 0);
        }
        batch.setColor(1, 1, 1, 1);
    }

    private void drawShield(SpriteBatch batch, GameWorld w) {
        Player p = w.player;
        if (p.shieldCharges <= 0) return;
        float s = 2.1f + 0.05f * MathUtils.sin(time * 6f);
        batch.setColor(1, 1, 1, 0.55f);
        batch.draw(assets.region("shield"), p.x() - s / 2, p.y() + 0.35f - s / 2, s / 2, s / 2, s, s, 1, 1, time * 20f);
        batch.setColor(0.7f, 0.95f, 1f, 1f);
        for (int i = 0; i < p.shieldCharges; i++) {
            float a = time * 2f + i * MathUtils.PI2 / p.shieldCharges;
            batch.draw(assets.soft, p.x() + MathUtils.cos(a) * 1.1f - 0.12f, p.y() + 0.35f + MathUtils.sin(a) * 1.1f - 0.12f, 0.24f, 0.24f);
        }
        batch.setColor(1, 1, 1, 1);
    }

    /** Curvas de animacao. */
    static final class Interp {
        static float backOut(float t) {
            float s = 1.70158f;
            t -= 1f;
            return t * t * ((s + 1f) * t + s) + 1f;
        }
    }
}
