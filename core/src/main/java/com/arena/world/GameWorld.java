package com.arena.world;

import com.arena.audio.GameAudio;
import com.arena.audio.Sfx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Pool;

/**
 * Toda a logica do jogo, independente de renderizacao: Box2D, pools, hordas, biomas, chefes,
 * habilidades, XP e upgrades. Pode ser simulada sem janela (veja GameWorldTest).
 */
public class GameWorld implements Disposable {
    public enum State { PLAYING, LEVEL_UP, GAME_OVER }

    private static final float ORBIT_RADIUS = 2.2f;
    private static final float ORBIT_SPEED = 3.2f;
    private static final int MAX_GEMS = 600;

    public final GameAudio audio;
    public final WorldEvents events;
    public final RandomXS128 random;
    public final World box2d;
    public final Player player = new Player();
    public final WaveManager waves = new WaveManager(this);

    public final Array<Enemy> enemies = new Array<>(false, 512);
    public final Array<Bullet> bullets = new Array<>(false, 1024);
    public final Array<Bullet> orbitBullets = new Array<>(false, 8);
    public final Array<Gem> gems = new Array<>(false, 512);
    public final Array<SpawnMarker> markers = new Array<>(false, 64);
    public final Array<Meteor> meteors = new Array<>(false, 32);
    public final Array<Chest> chests = new Array<>(false, 4);
    public final Array<Obstacle> obstacles = new Array<>(false, 32);
    public final Array<Decor> decor = new Array<>(false, 64);
    public Biome biome;
    public Enemy boss;

    public final Pool<Bullet> bulletPool;
    public final EnemyPool[] enemyPools = new EnemyPool[EnemyType.values().length];
    public final Pool<Gem> gemPool = new Pool<Gem>(256) {
        @Override protected Gem newObject() { return new Gem(); }
    };
    public final Pool<SpawnMarker> markerPool = new Pool<SpawnMarker>(32) {
        @Override protected SpawnMarker newObject() { return new SpawnMarker(); }
    };
    public final Pool<Meteor> meteorPool = new Pool<Meteor>(16) {
        @Override protected Meteor newObject() { return new Meteor(); }
    };

    public State state = State.PLAYING;
    public final Upgrade[] choices = new Upgrade[3];
    public int pendingLevelUps;
    public float time;
    public int kills;
    public float biomeFade;      // > 0 durante a transicao de bioma

    private final Array<Bullet> hitBullets = new Array<>(false, 128);
    private final Array<Enemy> hitEnemies = new Array<>(false, 128);
    private final Array<Bullet> enemyBulletHits = new Array<>(false, 16);
    private final Array<Enemy> touchingPlayer = new Array<>(false, 32);
    private final Array<Enemy> chainHit = new Array<>(false, 16);
    private final FloatArray chainPoints = new FloatArray(32);

    private final Vector2 tmp = new Vector2();
    private final float[] normal = new float[2];
    private final Array<Upgrade> upgradeBag = new Array<>(Upgrade.values());
    private float ghostTimer;

    public GameWorld(GameAudio audio, WorldEvents events, long seed) {
        this.audio = audio;
        this.events = events;
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

    // ================================================================== corpos

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
        fd.density = 20f;
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

    /** Um pool por tipo, para reaproveitar o corpo com o raio certo. */
    public final class EnemyPool extends Pool<Enemy> {
        final EnemyType type;

        EnemyPool(EnemyType type) { super(type.boss ? 1 : 64); this.type = type; }

        @Override protected Enemy newObject() {
            Enemy e = new Enemy(type);
            BodyDef def = new BodyDef();
            def.type = BodyDef.BodyType.DynamicBody;
            def.fixedRotation = true;
            def.active = false;
            def.linearDamping = 0f;
            e.body = box2d.createBody(def);
            CircleShape c = new CircleShape();
            c.setRadius(type.radius);
            FixtureDef fd = new FixtureDef();
            fd.shape = c;
            fd.density = type.boss ? 30f : 1f;
            fd.filter.categoryBits = Phys.ENEMY;
            // Espectros atravessam outros inimigos
            fd.filter.maskBits = type == EnemyType.WRAITH ? (short) (Phys.MASK_ENEMY & ~Phys.ENEMY) : Phys.MASK_ENEMY;
            e.body.createFixture(fd);
            e.body.setUserData(e);
            c.dispose();
            return e;
        }
    }

    // ================================================================== biomas

    void changeBiome(Biome b) {
        boolean first = biome == null;
        biome = b;
        for (Obstacle o : obstacles) box2d.destroyBody(o.body);
        obstacles.clear();
        decor.clear();
        b.build(obstacles, decor, random);
        for (Obstacle o : obstacles) {
            BodyDef def = new BodyDef();
            def.type = BodyDef.BodyType.StaticBody;
            def.position.set(o.x, o.y);
            o.body = box2d.createBody(def);
            Shape shape;
            if (o.isCircle()) { CircleShape c = new CircleShape(); c.setRadius(o.hw); shape = c; }
            else { PolygonShape p = new PolygonShape(); p.setAsBox(o.hw, o.hh); shape = p; }
            FixtureDef fd = new FixtureDef();
            fd.shape = shape;
            fd.filter.categoryBits = Phys.WALL;
            fd.filter.maskBits = Phys.MASK_WALL;
            o.body.createFixture(fd);
            shape.dispose();
        }
        if (!first) {
            // Limpa a arena: gemas viram XP, sobras somem, jogador volta ao centro
            for (int i = gems.size - 1; i >= 0; i--) { gainXp(gems.get(i).value); gemPool.free(gems.get(i)); }
            gems.clear();
            for (int i = enemies.size - 1; i >= 0; i--) removeEnemy(i);
            for (int i = bullets.size - 1; i >= 0; i--) freeBullet(i);
            for (SpawnMarker m : markers) markerPool.free(m);
            markers.clear();
            for (Meteor m : meteors) meteorPool.free(m);
            meteors.clear();
            chests.clear();
            player.body.setTransform(Phys.ARENA_W / 2f, Phys.ARENA_H / 2f, 0);
            biomeFade = 1.2f;
            events.biomeChanged(b);
        }
    }

    public boolean hasClosedChest() {
        for (Chest c : chests) if (!c.isOpen()) return true;
        return false;
    }

    public boolean insideObstacle(float x, float y, float margin) {
        for (Obstacle o : obstacles) if (o.penetration(x, y, margin, normal) < 0) return true;
        return false;
    }

    // ================================================================== spawn

    void spawnAtEdge(EnemyType type, float hpScale) {
        float px = player.x(), py = player.y();
        float x = 0, y = 0, m = type.radius + 0.6f;
        for (int tries = 0; tries < 16; tries++) {
            float per = random.nextFloat() * 2f * (Phys.ARENA_W + Phys.ARENA_H);
            if (per < Phys.ARENA_W) { x = per; y = m; }
            else if ((per -= Phys.ARENA_W) < Phys.ARENA_H) { x = Phys.ARENA_W - m; y = per; }
            else if ((per -= Phys.ARENA_H) < Phys.ARENA_W) { x = per; y = Phys.ARENA_H - m; }
            else { x = m; y = per - Phys.ARENA_W; }
            x = MathUtils.clamp(x, m, Phys.ARENA_W - m);
            y = MathUtils.clamp(y, m, Phys.ARENA_H - m);
            if (Vector2.dst2(x, y, px, py) > 64f && !insideObstacle(x, y, type.radius + 0.2f)) break;
        }
        spawnMarker(type, x, y, hpScale);
    }

    void spawnMarker(EnemyType type, float x, float y, float hpScale) {
        SpawnMarker mk = markerPool.obtain();
        mk.type = type; mk.x = x; mk.y = y; mk.hpScale = hpScale;
        mk.timer = SpawnMarker.DURATION * (type.boss ? 2f : 1f);
        markers.add(mk);
    }

    private void spawnSummon(EnemyType type, float cx, float cy, float dist) {
        for (int t = 0; t < 8; t++) {
            float a = random.nextFloat() * MathUtils.PI2;
            float x = MathUtils.clamp(cx + MathUtils.cos(a) * dist, 1.5f, Phys.ARENA_W - 1.5f);
            float y = MathUtils.clamp(cy + MathUtils.sin(a) * dist, 1.5f, Phys.ARENA_H - 1.5f);
            if (!insideObstacle(x, y, type.radius + 0.2f)) { spawnMarker(type, x, y, 1f + 0.12f * (waves.wave - 1)); return; }
        }
    }

    private void spawnEnemy(SpawnMarker mk) {
        Enemy e = enemyPools[mk.type.ordinal()].obtain();
        e.hp = e.maxHp = mk.type.hp * mk.hpScale;
        e.attackTimer = 1f + random.nextFloat() * 1.5f;
        e.stateTimer = 1f + random.nextFloat();
        e.body.setTransform(mk.x, mk.y, 0);
        e.body.setLinearVelocity(0, 0);
        e.body.setActive(true);
        enemies.add(e);
        if (mk.type.boss) boss = e;
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

    private Bullet playerArrow(float x, float y, float angle, float damageMul) {
        PlayerStats s = player.stats;
        float dx = MathUtils.cos(angle), dy = MathUtils.sin(angle);
        Bullet b = spawnBullet(x + dx * 0.5f, y + dy * 0.5f, dx, dy, false);
        setBulletRadius(b, 0.16f);
        b.damage = s.damage * damageMul;
        b.speed = s.bulletSpeed;
        b.life = 2.2f;
        b.pierce = s.pierce;
        b.ricochet = s.ricochet;
        b.sineAmp = s.sineLevel * 0.35f;
        b.homing = s.homingLevel * 2.5f;
        b.knockback = 2.5f;
        return b;
    }

    void enemyBullet(float x, float y, float angle, float speed, float damage) {
        Bullet b = spawnBullet(x, y, MathUtils.cos(angle), MathUtils.sin(angle), true);
        setBulletRadius(b, 0.22f);
        b.damage = damage;
        b.speed = speed;
        b.life = 5f;
    }

    private void setBulletRadius(Bullet b, float r) {
        b.radius = r;
        b.body.getFixtureList().first().getShape().setRadius(r);
    }

    // ================================================================== update

    public void update(InputState in, float dt) {
        if (state != State.PLAYING) return;
        time += dt;
        biomeFade = Math.max(0f, biomeFade - dt);

        updatePlayer(in, dt);
        updateMarkers(dt);
        updateEnemies(dt);
        updateBullets(dt);
        updateMeteors(dt);
        syncOrbitBullets();

        hitBullets.clear();
        hitEnemies.clear();
        enemyBulletHits.clear();
        box2d.step(dt, 6, 2);

        resolveHits();
        updateGems(dt);
        updateChests(dt);
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
        Player p = player;
        PlayerStats s = p.stats;
        p.invulnerable -= dt;
        p.fireCooldown -= dt;
        p.dashCooldown -= dt;
        p.specialCooldown -= dt;
        p.shootAnim -= dt;
        p.animTime += dt;
        if (p.shieldTime > 0f) { p.shieldTime -= dt; if (p.shieldTime <= 0f) p.shieldCharges = 0; }

        p.aim.set(in.aimX - p.x(), in.aimY - p.y());
        if (p.aim.isZero(0.001f)) p.aim.set(1, 0);
        p.aim.nor();

        tmp.set(in.moveX, in.moveY);
        if (tmp.len2() > 1f) tmp.nor();
        p.moving = !tmp.isZero(0.01f);
        if (p.moving) p.moveDir.set(tmp).nor();

        // Dash: atravessa inimigos e projeteis
        if (in.dash && p.dashCooldown <= 0f && !p.isDashing()) {
            p.dashTime = Player.DASH_TIME;
            p.dashCooldown = Player.DASH_COOLDOWN;
            p.dashDir.set(p.moving ? p.moveDir : p.aim);
            p.invulnerable = Math.max(p.invulnerable, Player.DASH_INVULNERABLE);
            setPlayerMask((short) Phys.WALL);
            audio.play(Sfx.DASH);
        }
        if (p.isDashing()) {
            p.dashTime -= dt;
            p.body.setLinearVelocity(p.dashDir.x * Player.DASH_DISTANCE / Player.DASH_TIME,
                    p.dashDir.y * Player.DASH_DISTANCE / Player.DASH_TIME);
            ghostTimer -= dt;
            if (ghostTimer <= 0f) { ghostTimer = 0.03f; events.dashGhost(p.x(), p.y()); }
            if (p.dashTime <= 0f) setPlayerMask(Phys.MASK_PLAYER);
        } else {
            p.body.setLinearVelocity(tmp.scl(s.moveSpeed));
        }

        if (in.firing && p.fireCooldown <= 0f && !p.isDashing()) {
            p.fireCooldown = 1f / s.fireRate;
            p.shootAnim = 0.15f;
            float base = p.aim.angleRad();
            float spread = 0.16f;
            for (int i = 0; i < s.multishot; i++) playerArrow(p.x(), p.y(), base + (i - (s.multishot - 1) / 2f) * spread, 1f);
            audio.play(Sfx.SHOOT);
        }

        if (in.special && p.special != null && p.specialCooldown <= 0f) castSpecial(in);

        if (touchingPlayer.size > 0 && p.invulnerable <= 0f) {
            float dmg = 0;
            for (Enemy e : touchingPlayer) dmg = Math.max(dmg, e.type.contactDamage);
            hurtPlayer(dmg);
        }
        // Escudo de gelo congela quem encosta
        if (p.shieldCharges > 0) for (Enemy e : touchingPlayer) e.slowTimer = 2f;
    }

    private void setPlayerMask(short mask) {
        Fixture f = player.body.getFixtureList().first();
        Filter filter = f.getFilterData();
        filter.maskBits = mask;
        f.setFilterData(filter);
    }

    private void hurtPlayer(float dmg) {
        if (player.invulnerable > 0f) return;
        if (player.shieldCharges > 0) {
            player.shieldCharges--;
            player.invulnerable = 0.3f;
            audio.play(Sfx.SHIELD);
            events.shieldBreak(player.x(), player.y());
            return;
        }
        player.hp -= dmg;
        player.invulnerable = Player.INVULNERABLE_TIME;
        audio.play(Sfx.PLAYER_HURT);
        events.playerHurt(player.x(), player.y());
        events.shake(0.45f);
    }

    // ================================================================== especiais

    private void castSpecial(InputState in) {
        Player p = player;
        int lvl = p.specialLevel;
        p.specialCooldown = p.special.cooldown(lvl);
        switch (p.special) {
            case ARROW_NOVA: {
                int n = 16 + (lvl - 1) * 4;
                for (int i = 0; i < n; i++) {
                    Bullet b = playerArrow(p.x(), p.y(), i * MathUtils.PI2 / n, 1.3f);
                    b.pierce += 2;
                    b.homing = 0;
                    b.sineAmp = 0;
                }
                events.nova(p.x(), p.y());
                audio.play(Sfx.NOVA);
                break;
            }
            case ICE_SHIELD:
                p.shieldCharges = 3 + (lvl - 1);
                p.shieldTime = 12f;
                audio.play(Sfx.SHIELD);
                break;
            case CHAIN_LIGHTNING: {
                chainHit.clear();
                chainPoints.clear();
                chainPoints.add(p.x(), p.y() + 0.4f);
                float dmg = p.stats.damage * 3f * (1f + 0.25f * (lvl - 1));
                Enemy cur = nearestEnemy(p.x(), p.y(), 9f, null, chainHit);
                int jumps = 5 + (lvl - 1);
                while (cur != null && jumps-- > 0) {
                    chainHit.add(cur);
                    chainPoints.add(cur.x(), cur.y());
                    damageEnemy(cur, dmg, cur.x() - p.x(), cur.y() - p.y(), 1.5f);
                    cur = nearestEnemy(cur.x(), cur.y(), 5f, null, chainHit);
                }
                if (chainPoints.size <= 2) chainPoints.add(in.aimX, in.aimY);
                events.lightning(chainPoints);
                events.shake(0.25f);
                audio.play(Sfx.LIGHTNING);
                removeDeadEnemies();
                break;
            }
            case METEORS: {
                int n = 8 + (lvl - 1) * 2;
                for (int i = 0; i < n; i++) {
                    Meteor m = meteorPool.obtain();
                    float a = random.nextFloat() * MathUtils.PI2, d = (float) Math.sqrt(random.nextFloat()) * 3.5f;
                    m.x = MathUtils.clamp(in.aimX + MathUtils.cos(a) * d, 0.5f, Phys.ARENA_W - 0.5f);
                    m.y = MathUtils.clamp(in.aimY + MathUtils.sin(a) * d, 0.5f, Phys.ARENA_H - 0.5f);
                    m.total = m.timer = 0.6f + i * 0.09f;
                    m.damage = p.stats.damage * 4f * (1f + 0.2f * (lvl - 1));
                    m.radius = 1.6f;
                    meteors.add(m);
                }
                break;
            }
        }
    }

    private void updateMeteors(float dt) {
        for (int i = meteors.size - 1; i >= 0; i--) {
            Meteor m = meteors.get(i);
            m.timer -= dt;
            if (m.timer > 0f) continue;
            for (int j = 0; j < enemies.size; j++) {
                Enemy e = enemies.get(j);
                float dx = e.x() - m.x, dy = e.y() - m.y;
                if (dx * dx + dy * dy < (m.radius + e.type.radius) * (m.radius + e.type.radius))
                    damageEnemy(e, m.damage, dx, dy, 6f);
            }
            events.explosion(m.x, m.y, m.radius);
            events.shake(0.3f);
            audio.play(Sfx.EXPLOSION);
            meteors.removeIndex(i);
            meteorPool.free(m);
        }
        removeDeadEnemies();
    }

    // ================================================================== inimigos

    private void updateMarkers(float dt) {
        for (int i = markers.size - 1; i >= 0; i--) {
            SpawnMarker m = markers.get(i);
            m.timer -= dt;
            if (m.timer <= 0f) {
                spawnEnemy(m);
                markers.removeIndex(i);
                markerPool.free(m);
            }
        }
    }

    private void updateEnemies(float dt) {
        float px = player.x(), py = player.y();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            e.flash -= dt;
            e.orbitHitTimer -= dt;
            e.castTimer -= dt;
            e.age += dt;
            e.slowTimer -= dt;
            float slow = e.slowTimer > 0f ? 0.3f : 1f;
            e.animTime += dt * slow;

            float dx = px - e.x(), dy = py - e.y();
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float nx = dist > 0.001f ? dx / dist : 0, ny = dist > 0.001f ? dy / dist : 0;
            float sp = e.type.speed;
            tmp.setZero();

            switch (e.type.behavior) {
                case CHASER:
                case TANK:
                    tmp.set(nx, ny).scl(sp);
                    break;
                case SHOOTER:
                    if (dist > 8f) tmp.set(nx, ny).scl(sp);
                    else if (dist < 5f) tmp.set(-nx, -ny).scl(sp);
                    else tmp.set(-ny, nx).scl(sp * 0.6f);
                    e.attackTimer -= dt * slow;
                    if (e.attackTimer <= 0f && dist < 15f) {
                        e.attackTimer = 2.2f;
                        e.castTimer = 0.4f;
                        enemyBullet(e.x() + nx * 0.6f, e.y() + ny * 0.6f, MathUtils.atan2(ny, nx), 7f, 8f);
                        audio.play(Sfx.ENEMY_SHOOT);
                    }
                    break;
                case DASHER:
                    dasher(e, nx, ny, dist, dt * slow, 13f, 0.6f, 0.4f, 7f);
                    tmp.set(lastVel);
                    break;
                case BOSS_KING:
                    bossKing(e, nx, ny, dist, dt * slow);
                    tmp.set(lastVel);
                    break;
                case BOSS_ALPHA:
                    bossAlpha(e, nx, ny, dist, dt * slow);
                    tmp.set(lastVel);
                    break;
                case BOSS_SUPREME:
                    bossSupreme(e, nx, ny, dist, dt * slow);
                    tmp.set(lastVel);
                    break;
            }
            if (Math.abs(tmp.x) > 0.05f) e.facingLeft = tmp.x < 0;
            else if (Math.abs(dx) > 0.05f) e.facingLeft = dx < 0;
            tmp.scl(slow).add(e.knock);
            e.knock.scl((float) Math.exp(-9f * dt));
            e.body.setLinearVelocity(tmp);

            if (e.orbitContacts > 0 && e.orbitHitTimer <= 0f) {
                e.orbitHitTimer = 0.35f;
                damageEnemy(e, player.stats.damage * 0.8f, e.x() - px, e.y() - py, 3f);
            }
        }
        removeDeadEnemies();
    }

    private final Vector2 lastVel = new Vector2();

    /** Estado: 0 aproximar, 1 preparar (rosnado), 2 investida, 3 recuperar. */
    private void dasher(Enemy e, float nx, float ny, float dist, float dt, float dashSpeed, float windup, float dashTime, float trigger) {
        e.stateTimer -= dt;
        switch (e.state) {
            case 0:
                lastVel.set(nx, ny).scl(e.type.speed);
                if (dist < trigger && e.stateTimer <= 0f) { e.state = 1; e.stateTimer = windup; e.lockedDir.set(nx, ny); }
                break;
            case 1:
                lastVel.setZero();
                e.lockedDir.set(nx, ny);
                if (e.stateTimer <= 0f) { e.state = 2; e.stateTimer = dashTime; audio.play(Sfx.DASH); }
                break;
            case 2:
                lastVel.set(e.lockedDir).scl(dashSpeed);
                if (e.stateTimer <= 0f) { e.state = 3; e.stateTimer = 0.8f; }
                break;
            default:
                lastVel.set(nx, ny).scl(e.type.speed * 0.4f);
                if (e.stateTimer <= 0f) { e.state = 0; e.stateTimer = 0.5f; }
        }
    }

    private void ring(Enemy e, int n, float speed, float offset) {
        for (int i = 0; i < n; i++) enemyBullet(e.x(), e.y(), offset + i * MathUtils.PI2 / n, speed, 12f);
        audio.play(Sfx.ENEMY_SHOOT);
    }

    /** Cavaleiro-Rei: anda, prepara, investe e solta um anel de projeteis ao parar. */
    private void bossKing(Enemy e, float nx, float ny, float dist, float dt) {
        int before = e.state;
        dasher(e, nx, ny, dist, dt, 11f, 0.8f, 0.8f, 12f);
        if (before == 2 && e.state == 3) {
            ring(e, 16, 6f, e.patternAngle += 0.2f);
            events.bossSlam(e.x(), e.y());
            events.shake(0.6f);
            audio.play(Sfx.EXPLOSION);
        }
        e.attackTimer -= dt;
        if (e.state == 0 && e.attackTimer <= 0f) { e.attackTimer = 3f; ring(e, 10, 5f, random.nextFloat()); }
    }

    /** Lobisomem Alfa: tres investidas seguidas e uivo que convoca lobos. */
    private void bossAlpha(Enemy e, float nx, float ny, float dist, float dt) {
        int before = e.state;
        dasher(e, nx, ny, dist, dt, 15f, 0.45f, 0.5f, 20f);
        if (before == 2 && e.state == 3) {
            e.counter++;
            if (e.counter % 3 == 0) {
                e.stateTimer = 1.6f;
                audio.play(Sfx.BOSS_ROAR);
                events.shake(0.5f);
                for (int k = 0; k < 3; k++) spawnSummon(EnemyType.WEREWOLF, e.x(), e.y(), 3f);
            } else e.stateTimer = 0.25f;
        }
    }

    /** Necromante Supremo: espiral de projeteis, leque mirado e convocacao de espectros. */
    private void bossSupreme(Enemy e, float nx, float ny, float dist, float dt) {
        if (dist > 9f) lastVel.set(nx, ny).scl(e.type.speed);
        else if (dist < 6f) lastVel.set(-nx, -ny).scl(e.type.speed);
        else lastVel.set(-ny, nx).scl(e.type.speed * 0.5f);
        e.stateTimer -= dt;
        e.attackTimer -= dt;
        if (e.state == 0) { // espiral
            if (e.attackTimer <= 0f) {
                e.attackTimer = 0.12f;
                for (int k = 0; k < 3; k++) enemyBullet(e.x(), e.y(), e.patternAngle + k * MathUtils.PI2 / 3f, 5.5f, 10f);
                e.patternAngle += 0.33f;
                e.castTimer = 0.2f;
            }
            if (e.stateTimer <= 0f) { e.state = 1; e.stateTimer = 2.5f; e.counter = 0; }
        } else { // leques mirados
            if (e.attackTimer <= 0f && e.counter < 3) {
                e.attackTimer = 0.7f;
                e.counter++;
                float base = MathUtils.atan2(ny, nx);
                for (int k = -3; k <= 3; k++) enemyBullet(e.x(), e.y(), base + k * 0.16f, 8f, 12f);
                e.castTimer = 0.4f;
                audio.play(Sfx.ENEMY_SHOOT);
            }
            if (e.stateTimer <= 0f) {
                e.state = 0; e.stateTimer = 3.2f;
                for (int k = 0; k < 4; k++) spawnSummon(EnemyType.WRAITH, e.x(), e.y(), 2.5f);
            }
        }
    }

    // ================================================================== projeteis

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

            // Kinematic x estatico nao gera contato no Box2D: paredes e obstaculos sao checados aqui
            float x = b.x(), y = b.y(), r = b.radius;
            boolean hitX = x < r || x > Phys.ARENA_W - r;
            boolean hitY = y < r || y > Phys.ARENA_H - r;
            boolean hitObstacle = false;
            if (!hitX && !hitY) {
                for (Obstacle o : obstacles) {
                    if (o.penetration(x, y, r, normal) < 0f) { hitObstacle = true; break; }
                }
            }
            if (!hitX && !hitY && !hitObstacle) continue;

            if (!b.enemyOwned && b.ricochet > 0) {
                b.ricochet--;
                if (hitObstacle) {
                    float dot = b.dir.x * normal[0] + b.dir.y * normal[1];
                    if (dot < 0) b.dir.sub(2 * dot * normal[0], 2 * dot * normal[1]);
                    b.body.setTransform(x + normal[0] * 0.15f, y + normal[1] * 0.15f, 0);
                } else {
                    if (hitX) b.dir.x = -b.dir.x;
                    if (hitY) b.dir.y = -b.dir.y;
                    b.body.setTransform(MathUtils.clamp(x, r, Phys.ARENA_W - r), MathUtils.clamp(y, r, Phys.ARENA_H - r), 0);
                }
                b.alreadyHit.clear();
                b.target = null;
                b.life = Math.max(b.life, 1.2f);
                audio.play(Sfx.RICOCHET);
                events.wallSpark(x, y);
            } else {
                events.wallSpark(x, y);
                freeBullet(i);
            }
        }
    }

    private void steerHoming(Bullet b, float dt) {
        b.retargetTimer -= dt;
        if (b.target == null || b.target.dead || b.retargetTimer <= 0f) {
            b.retargetTimer = 0.15f;
            b.target = nearestEnemy(b.x(), b.y(), 12f, b, null);
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

    private Enemy nearestEnemy(float x, float y, float maxDist, Bullet ignoreHitBy, Array<Enemy> exclude) {
        Enemy best = null;
        float bestD = maxDist * maxDist;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.dead || (ignoreHitBy != null && ignoreHitBy.alreadyHit.contains(e, true))) continue;
            if (exclude != null && exclude.contains(e, true)) continue;
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
            b.dir.set(-MathUtils.sin(a), MathUtils.cos(a));
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
            damageEnemy(e, b.damage, b.dir.x, b.dir.y, b.knockback);
            events.hit(b.x(), b.y(), b.dir.x, b.dir.y, true);
            if (b.pierce-- <= 0) b.dead = true;
        }
        for (Bullet b : enemyBulletHits) {
            if (b.dead || player.isDashing()) continue;
            b.dead = true;
            events.hit(b.x(), b.y(), b.dir.x, b.dir.y, false);
            hurtPlayer(b.damage);
        }
        removeDeadEnemies();
    }

    private void damageEnemy(Enemy e, float dmg, float dirX, float dirY, float knockback) {
        if (e.dead) return;
        e.hp -= dmg;
        e.flash = 0.09f;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len > 0.001f) e.knock.add(dirX / len * knockback * e.type.knockback, dirY / len * knockback * e.type.knockback);
        events.damageNumber(e.x(), e.y() + e.type.drawHeight * 0.6f, dmg, dmg >= player.stats.damage * 2.5f);
        if (e.hp <= 0f) {
            e.dead = true;
            kills++;
            audio.play(e.type.deathSfx);
            events.enemyDied(e);
            if (e.type.boss) {
                chests.add(new Chest(MathUtils.clamp(e.x(), 1.5f, Phys.ARENA_W - 1.5f), MathUtils.clamp(e.y(), 1.5f, Phys.ARENA_H - 1.5f)));
                events.shake(0.8f);
                for (int k = 0; k < 10; k++) dropGem(e.x() + random.nextFloat() * 2 - 1, e.y() + random.nextFloat() * 2 - 1, e.type.xp / 10);
            } else {
                dropGem(e.x(), e.y(), e.type.xp);
            }
        } else {
            audio.play(Sfx.HIT);
        }
    }

    private void removeDeadEnemies() {
        for (int i = enemies.size - 1; i >= 0; i--) if (enemies.get(i).dead) removeEnemy(i);
    }

    private void removeEnemy(int i) {
        Enemy e = enemies.removeIndex(i);
        touchingPlayer.removeValue(e, true);
        if (e == boss) boss = null;
        enemyPools[e.type.ordinal()].free(e);
    }

    private void freeBullet(int index) {
        Bullet b = bullets.removeIndex(index);
        bulletPool.free(b);
    }

    // ================================================================== gemas, baus, XP

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

    private void updateChests(float dt) {
        for (int i = chests.size - 1; i >= 0; i--) {
            Chest c = chests.get(i);
            if (c.isOpen()) {
                c.openTimer += dt;
                if (c.openTimer > 2.5f) chests.removeIndex(i);
            } else if ((c.age += dt) > Chest.AUTO_OPEN || Vector2.dst2(c.x, c.y, player.x(), player.y()) < 1.1f) {
                c.openTimer = 0f;
                pendingLevelUps++;
                audio.play(Sfx.CHEST);
                events.chestOpened(c.x, c.y);
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

    // ================================================================== level up

    private void openLevelUp() {
        state = State.LEVEL_UP;
        for (int i = upgradeBag.size - 1; i > 0; i--) upgradeBag.swap(i, random.nextInt(i + 1));
        int n = 0;
        boolean specialOffered = false;
        for (Upgrade u : upgradeBag) {
            if (u == Upgrade.ORBIT && player.stats.orbitCount >= 6) continue;
            if (u.special != null) {
                // no maximo uma especial por rodada, e nivel maximo 5
                if (specialOffered || (player.special == u.special && player.specialLevel >= 5)) continue;
                specialOffered = true;
            }
            choices[n++] = u;
            if (n == choices.length) break;
        }
        player.body.setLinearVelocity(0, 0);
        audio.play(Sfx.LEVEL_UP);
    }

    public void chooseUpgrade(int index) {
        if (state != State.LEVEL_UP || index < 0 || index >= choices.length) return;
        choices[index].apply(player);
        player.taken[choices[index].ordinal()]++;
        audio.play(Sfx.SELECT);
        pendingLevelUps--;
        if (pendingLevelUps > 0) openLevelUp();
        else state = State.PLAYING;
    }

    // ================================================================== contatos

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
