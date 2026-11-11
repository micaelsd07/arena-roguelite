package com.arena.world;

import com.arena.audio.GameAudio;
import com.arena.audio.Sfx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Pool;

/**
 * Toda a logica do jogo, independente de renderizacao: Box2D, pools, hordas, XP e upgrades.
 * Pode ser simulada sem janela (veja GameWorldTest).
 */
public class GameWorld implements Disposable {
    public enum State { PLAYING, LEVEL_UP, GAME_OVER }

    private static final float ORBIT_RADIUS = 2.2f;
    private static final float ORBIT_SPEED = 3.2f;   // rad/s
    private static final float MAX_GEMS = 600;

    public final GameAudio audio;
    public final RandomXS128 random;
    public final World box2d;
    public final Player player = new Player();
    public final WaveManager waves = new WaveManager(this);

    public final Array<Enemy> enemies = new Array<>(false, 512);
    public final Array<Bullet> bullets = new Array<>(false, 1024);
    public final Array<Bullet> orbitBullets = new Array<>(false, 8);
    public final Array<Gem> gems = new Array<>(false, 512);

    public final Pool<Bullet> bulletPool;
    public final EnemyPool[] enemyPools = new EnemyPool[EnemyType.values().length];
    public final Pool<Gem> gemPool = new Pool<Gem>(256) {
        @Override protected Gem newObject() { return new Gem(); }
    };

    public State state = State.PLAYING;
    public final Upgrade[] choices = new Upgrade[3];
    public int pendingLevelUps;
    public float time;
    public int kills;

    // Colisoes coletadas durante o step e processadas depois (nao se pode alterar corpos dentro do step)
    private final Array<Bullet> hitBullets = new Array<>(false, 128);
    private final Array<Enemy> hitEnemies = new Array<>(false, 128);
    private final Array<Bullet> enemyBulletHits = new Array<>(false, 16);
    private final Array<Enemy> touchingPlayer = new Array<>(false, 32);

    private final Vector2 tmp = new Vector2();
    private final Vector2 tmp2 = new Vector2();
    private final Array<Upgrade> upgradeBag = new Array<>(Upgrade.values());

    public GameWorld(GameAudio audio, long seed) {
        this.audio = audio;
        this.random = new RandomXS128(seed);
        box2d = new World(Vector2.Zero, true);
        box2d.setContactListener(new Contacts());

        bulletPool = new Pool<Bullet>(512) {
            @Override protected Bullet newObject() {
                Bullet b = new Bullet();
                b.body = createBulletBody(b);
                return b;
            }
        };
        for (EnemyType t : EnemyType.values()) enemyPools[t.ordinal()] = new EnemyPool(t);

        createWalls();
        createPlayer();
        waves.startNextWave();
    }

    // ------------------------------------------------------------------ criacao de corpos

    private void createWalls() {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.StaticBody;
        Body walls = box2d.createBody(def);
        ChainShape chain = new ChainShape();
        chain.createLoop(new float[]{0, 0, Phys.ARENA_W, 0, Phys.ARENA_W, Phys.ARENA_H, 0, Phys.ARENA_H});
        FixtureDef fd = new FixtureDef();
        fd.shape = chain;
        fd.filter.categoryBits = Phys.WALL;
        fd.filter.maskBits = Phys.MASK_WALL;
        walls.createFixture(fd);
        chain.dispose();
    }

    private void createPlayer() {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.fixedRotation = true;
        def.position.set(Phys.ARENA_W / 2f, Phys.ARENA_H / 2f);
        Body body = box2d.createBody(def);
        CircleShape c = new CircleShape();
        c.setRadius(Player.RADIUS);
        FixtureDef fd = new FixtureDef();
        fd.shape = c;
        fd.density = 20f; // pesado: inimigos nao conseguem empurrar o jogador facilmente
        fd.filter.categoryBits = Phys.PLAYER;
        fd.filter.maskBits = Phys.MASK_PLAYER;
        body.createFixture(fd);
        body.setUserData(player);
        c.dispose();
        player.body = body;
    }

    private Body createBulletBody(Bullet b) {
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.KinematicBody;
        def.bullet = true;
        def.active = false;
        Body body = box2d.createBody(def);
        CircleShape c = new CircleShape();
        c.setRadius(0.15f);
        FixtureDef fd = new FixtureDef();
        fd.shape = c;
        fd.isSensor = true;
        body.createFixture(fd);
        body.setUserData(b);
        c.dispose();
        return body;
    }

    /** Um pool por tipo, para que o formato do corpo (raio) seja reaproveitado sem recriar fixtures. */
    public final class EnemyPool extends Pool<Enemy> {
        final EnemyType type;

        EnemyPool(EnemyType type) { super(64); this.type = type; }

        @Override protected Enemy newObject() {
            Enemy e = new Enemy(type);
            BodyDef def = new BodyDef();
            def.type = BodyDef.BodyType.DynamicBody;
            def.fixedRotation = true;
            def.active = false;
            e.body = box2d.createBody(def);
            CircleShape c = new CircleShape();
            c.setRadius(type.radius);
            FixtureDef fd = new FixtureDef();
            fd.shape = c;
            fd.density = 1f;
            fd.filter.categoryBits = Phys.ENEMY;
            fd.filter.maskBits = Phys.MASK_ENEMY;
            e.body.createFixture(fd);
            e.body.setUserData(e);
            c.dispose();
            return e;
        }
    }

    // ------------------------------------------------------------------ spawn

    void spawnEnemy(EnemyType type, float hpScale) {
        float px = player.x(), py = player.y();
        float x = 0, y = 0;
        float m = type.radius + 0.3f;
        for (int tries = 0; tries < 12; tries++) {
            float perimeterPos = random.nextFloat() * 2f * (Phys.ARENA_W + Phys.ARENA_H);
            if (perimeterPos < Phys.ARENA_W) { x = perimeterPos; y = m; }
            else if ((perimeterPos -= Phys.ARENA_W) < Phys.ARENA_H) { x = Phys.ARENA_W - m; y = perimeterPos; }
            else if ((perimeterPos -= Phys.ARENA_H) < Phys.ARENA_W) { x = perimeterPos; y = Phys.ARENA_H - m; }
            else { x = m; y = perimeterPos - Phys.ARENA_W; }
            x = MathUtils.clamp(x, m, Phys.ARENA_W - m);
            y = MathUtils.clamp(y, m, Phys.ARENA_H - m);
            if (Vector2.dst2(x, y, px, py) > 64f) break;
        }
        Enemy e = enemyPools[type.ordinal()].obtain();
        e.hp = e.maxHp = type.hp * hpScale;
        e.shootTimer = 1f + random.nextFloat() * 1.5f;
        e.body.setTransform(x, y, 0);
        e.body.setLinearVelocity(0, 0);
        e.body.setActive(true);
        enemies.add(e);
    }

    private Bullet spawnBullet(float x, float y, float dirX, float dirY, boolean enemyOwned) {
        Bullet b = bulletPool.obtain();
        b.enemyOwned = enemyOwned;
        b.dir.set(dirX, dirY).nor();
        Fixture f = b.body.getFixtureList().first();
        Filter filter = f.getFilterData();
        filter.categoryBits = enemyOwned ? Phys.ENEMY_BULLET : Phys.PLAYER_BULLET;
        filter.maskBits = enemyOwned ? Phys.MASK_ENEMY_BULLET : Phys.MASK_PLAYER_BULLET;
        f.setFilterData(filter);
        b.body.setTransform(x, y, 0);
        b.body.setActive(true);
        bullets.add(b);
        return b;
    }

    private void setBulletRadius(Bullet b, float r) {
        b.radius = r;
        b.body.getFixtureList().first().getShape().setRadius(r);
    }

    // ------------------------------------------------------------------ update

    public void update(InputState in, float dt) {
        if (state != State.PLAYING) return;
        time += dt;

        updatePlayer(in, dt);
        updateEnemies(dt);
        updateBullets(dt);
        syncOrbitBullets();

        hitBullets.clear();
        hitEnemies.clear();
        enemyBulletHits.clear();
        box2d.step(dt, 6, 2);

        resolveHits();
        updateGems(dt);
        waves.update(dt);

        if (player.hp <= 0f) {
            player.hp = 0f;
            state = State.GAME_OVER;
            audio.play(Sfx.GAME_OVER);
        } else if (pendingLevelUps > 0) {
            openLevelUp();
        }
    }

    private void updatePlayer(InputState in, float dt) {
        PlayerStats s = player.stats;
        tmp.set(in.moveX, in.moveY);
        if (tmp.len2() > 1f) tmp.nor();
        player.body.setLinearVelocity(tmp.scl(s.moveSpeed));
        player.invulnerable -= dt;
        player.fireCooldown -= dt;

        if (in.firing && player.fireCooldown <= 0f) {
            player.fireCooldown = 1f / s.fireRate;
            float base = MathUtils.atan2(in.aimY - player.y(), in.aimX - player.x());
            float spread = 0.16f;
            for (int i = 0; i < s.multishot; i++) {
                float a = base + (i - (s.multishot - 1) / 2f) * spread;
                float dx = MathUtils.cos(a), dy = MathUtils.sin(a);
                Bullet b = spawnBullet(player.x() + dx * 0.6f, player.y() + dy * 0.6f, dx, dy, false);
                setBulletRadius(b, 0.15f);
                b.damage = s.damage;
                b.speed = s.bulletSpeed;
                b.life = 2.2f;
                b.pierce = s.pierce;
                b.ricochet = s.ricochet;
                b.sineAmp = s.sineLevel * 0.35f;
                b.homing = s.homingLevel * 2.5f;
            }
            audio.play(Sfx.SHOOT);
        }

        // Dano por contato com inimigos
        if (touchingPlayer.size > 0 && player.invulnerable <= 0f) {
            float dmg = 0;
            for (Enemy e : touchingPlayer) dmg = Math.max(dmg, e.type.contactDamage);
            hurtPlayer(dmg);
        }
    }

    private void hurtPlayer(float dmg) {
        if (player.invulnerable > 0f) return;
        player.hp -= dmg;
        player.invulnerable = Player.INVULNERABLE_TIME;
        audio.play(Sfx.PLAYER_HURT);
    }

    private void updateEnemies(float dt) {
        float px = player.x(), py = player.y();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            e.flash -= dt;
            e.orbitHitTimer -= dt;
            tmp.set(px - e.x(), py - e.y());
            float dist = tmp.len();
            if (dist > 0.001f) tmp.scl(1f / dist);

            if (e.type == EnemyType.SHOOTER) {
                // Mantem distancia: aproxima, recua ou circula
                if (dist > 8f) tmp.scl(e.type.speed);
                else if (dist < 5f) tmp.scl(-e.type.speed);
                else tmp.set(-tmp.y, tmp.x).scl(e.type.speed * 0.6f);
                e.shootTimer -= dt;
                if (e.shootTimer <= 0f && dist < 15f) {
                    e.shootTimer = 2.2f;
                    float dx = (px - e.x()) / dist, dy = (py - e.y()) / dist;
                    Bullet b = spawnBullet(e.x() + dx * 0.7f, e.y() + dy * 0.7f, dx, dy, true);
                    setBulletRadius(b, 0.22f);
                    b.damage = 8f;
                    b.speed = 7f;
                    b.life = 4f;
                    audio.play(Sfx.ENEMY_SHOOT);
                }
            } else {
                tmp.scl(e.type.speed);
            }
            e.body.setLinearVelocity(tmp);

            // Projeteis orbitais causam dano continuo enquanto encostam
            if (e.orbitContacts > 0 && e.orbitHitTimer <= 0f) {
                e.orbitHitTimer = 0.35f;
                damageEnemy(e, player.stats.damage * 0.8f);
            }
        }
        removeDeadEnemies();
    }

    private void updateBullets(float dt) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.time += dt;
            b.life -= dt;
            if (b.life <= 0f || b.dead) { freeBullet(i); continue; }

            if (b.homing > 0f) steerHoming(b, dt);

            tmp.set(b.dir).scl(b.speed);
            if (b.sineAmp > 0f) {
                float freq = 11f;
                float lateral = b.sineAmp * freq * MathUtils.cos(b.time * freq);
                tmp.add(-b.dir.y * lateral, b.dir.x * lateral);
            }
            b.body.setLinearVelocity(tmp);

            // Paredes: kinematic x static nao gera contato no Box2D, entao checamos os limites da arena
            float x = b.x(), y = b.y(), r = b.radius;
            boolean hitX = x < r || x > Phys.ARENA_W - r;
            boolean hitY = y < r || y > Phys.ARENA_H - r;
            if (hitX || hitY) {
                if (b.ricochet > 0) {
                    b.ricochet--;
                    if (hitX) b.dir.x = -b.dir.x;
                    if (hitY) b.dir.y = -b.dir.y;
                    b.body.setTransform(MathUtils.clamp(x, r, Phys.ARENA_W - r), MathUtils.clamp(y, r, Phys.ARENA_H - r), 0);
                    b.alreadyHit.clear();
                    b.life = Math.max(b.life, 1.2f);
                    audio.play(Sfx.RICOCHET);
                } else {
                    freeBullet(i);
                }
            }
        }
    }

    private void steerHoming(Bullet b, float dt) {
        b.retargetTimer -= dt;
        if (b.target == null || b.target.dead || b.retargetTimer <= 0f) {
            b.retargetTimer = 0.15f;
            b.target = nearestEnemy(b.x(), b.y(), 12f, b);
        }
        if (b.target == null) return;
        float want = MathUtils.atan2(b.target.y() - b.y(), b.target.x() - b.x());
        float cur = b.dir.angleRad();
        float diff = want - cur;
        while (diff > MathUtils.PI) diff -= MathUtils.PI2;
        while (diff < -MathUtils.PI) diff += MathUtils.PI2;
        float maxTurn = b.homing * dt;
        b.dir.setAngleRad(cur + MathUtils.clamp(diff, -maxTurn, maxTurn));
    }

    private Enemy nearestEnemy(float x, float y, float maxDist, Bullet ignoreHitBy) {
        Enemy best = null;
        float bestD = maxDist * maxDist;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.dead || (ignoreHitBy != null && ignoreHitBy.alreadyHit.contains(e, true))) continue;
            float d = Vector2.dst2(x, y, e.x(), e.y());
            if (d < bestD) { bestD = d; best = e; }
        }
        return best;
    }

    private void syncOrbitBullets() {
        int want = player.stats.orbitCount;
        while (orbitBullets.size < want) {
            Bullet b = bulletPool.obtain();
            b.orbit = true;
            b.life = Float.MAX_VALUE;
            Fixture f = b.body.getFixtureList().first();
            Filter filter = f.getFilterData();
            filter.categoryBits = Phys.PLAYER_BULLET;
            filter.maskBits = Phys.MASK_PLAYER_BULLET;
            f.setFilterData(filter);
            setBulletRadius(b, 0.35f);
            b.body.setActive(true);
            orbitBullets.add(b);
        }
        for (int i = 0; i < orbitBullets.size; i++) {
            Bullet b = orbitBullets.get(i);
            float a = time * ORBIT_SPEED + i * MathUtils.PI2 / orbitBullets.size;
            b.body.setTransform(player.x() + MathUtils.cos(a) * ORBIT_RADIUS,
                    player.y() + MathUtils.sin(a) * ORBIT_RADIUS, 0);
        }
    }

    private void resolveHits() {
        for (int i = 0; i < hitBullets.size; i++) {
            Bullet b = hitBullets.get(i);
            Enemy e = hitEnemies.get(i);
            if (b.dead || e.dead || b.alreadyHit.contains(e, true)) continue;
            b.alreadyHit.add(e);
            damageEnemy(e, b.damage);
            if (b.pierce-- <= 0) b.dead = true;
        }
        for (Bullet b : enemyBulletHits) {
            if (b.dead) continue;
            b.dead = true;
            hurtPlayer(b.damage);
        }
        removeDeadEnemies();
    }

    private void damageEnemy(Enemy e, float dmg) {
        if (e.dead) return;
        e.hp -= dmg;
        e.flash = 0.08f;
        if (e.hp <= 0f) {
            e.dead = true;
            kills++;
            audio.play(e.type.deathSfx);
            dropGem(e.x(), e.y(), e.type.xp);
        } else {
            audio.play(Sfx.HIT);
        }
    }

    private void removeDeadEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.dead) continue;
            touchingPlayer.removeValue(e, true);
            enemies.removeIndex(i);
            enemyPools[e.type.ordinal()].free(e); // reset() desativa o corpo
        }
    }

    private void freeBullet(int index) {
        Bullet b = bullets.removeIndex(index);
        bulletPool.free(b);
    }

    private void dropGem(float x, float y, int value) {
        if (gems.size >= MAX_GEMS) { gainXp(value); return; }
        Gem g = gemPool.obtain();
        g.x = x;
        g.y = y;
        g.value = value;
        gems.add(g);
    }

    private void updateGems(float dt) {
        float px = player.x(), py = player.y();
        float magnet2 = player.stats.magnet * player.stats.magnet;
        for (int i = gems.size - 1; i >= 0; i--) {
            Gem g = gems.get(i);
            float dx = px - g.x, dy = py - g.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < 0.45f) {
                gems.removeIndex(i);
                gainXp(g.value);
                gemPool.free(g);
                audio.play(Sfx.PICKUP);
                continue;
            }
            if (g.attracted || d2 < magnet2) {
                g.attracted = true;
                float d = (float) Math.sqrt(d2);
                float speed = Math.max(8f, player.stats.moveSpeed * 1.8f);
                g.x += dx / d * speed * dt;
                g.y += dy / d * speed * dt;
            }
        }
    }

    private void gainXp(int value) {
        player.xp += value;
        while (player.xp >= player.xpToNext()) {
            player.xp -= player.xpToNext();
            player.level++;
            pendingLevelUps++;
        }
    }

    // ------------------------------------------------------------------ level up

    private void openLevelUp() {
        state = State.LEVEL_UP;
        // Fisher-Yates com o random da partida (deterministico por seed)
        for (int i = upgradeBag.size - 1; i > 0; i--) upgradeBag.swap(i, random.nextInt(i + 1));
        int n = 0;
        for (Upgrade u : upgradeBag) {
            if (u == Upgrade.ORBIT && player.stats.orbitCount >= 6) continue;
            choices[n++] = u;
            if (n == choices.length) break;
        }
        player.body.setLinearVelocity(0, 0);
        audio.play(Sfx.LEVEL_UP);
    }

    public void chooseUpgrade(int index) {
        if (state != State.LEVEL_UP || index < 0 || index >= choices.length) return;
        choices[index].apply(player);
        audio.play(Sfx.SELECT);
        pendingLevelUps--;
        if (pendingLevelUps > 0) openLevelUp();
        else state = State.PLAYING;
    }

    // ------------------------------------------------------------------ contatos

    private class Contacts implements ContactListener {
        @Override public void beginContact(Contact c) {
            Object a = c.getFixtureA().getBody().getUserData();
            Object b = c.getFixtureB().getBody().getUserData();
            if (!handleBegin(a, b)) handleBegin(b, a);
        }

        private boolean handleBegin(Object a, Object b) {
            if (a instanceof Bullet && b instanceof Enemy) {
                Bullet bullet = (Bullet) a;
                if (bullet.orbit) ((Enemy) b).orbitContacts++;
                else { hitBullets.add(bullet); hitEnemies.add((Enemy) b); }
                return true;
            }
            if (a instanceof Bullet && b instanceof Player) {
                enemyBulletHits.add((Bullet) a);
                return true;
            }
            if (a instanceof Enemy && b instanceof Player) {
                if (!touchingPlayer.contains((Enemy) a, true)) touchingPlayer.add((Enemy) a);
                return true;
            }
            return false;
        }

        @Override public void endContact(Contact c) {
            Object a = c.getFixtureA().getBody().getUserData();
            Object b = c.getFixtureB().getBody().getUserData();
            if (!handleEnd(a, b)) handleEnd(b, a);
        }

        private boolean handleEnd(Object a, Object b) {
            if (a instanceof Bullet && b instanceof Enemy) {
                Enemy e = (Enemy) b;
                if (((Bullet) a).orbit && e.orbitContacts > 0) e.orbitContacts--;
                return true;
            }
            if (a instanceof Enemy && b instanceof Player) {
                touchingPlayer.removeValue((Enemy) a, true);
                return true;
            }
            return false;
        }

        @Override public void preSolve(Contact c, Manifold m) { }

        @Override public void postSolve(Contact c, ContactImpulse i) { }
    }

    @Override
    public void dispose() { box2d.dispose(); }
}
