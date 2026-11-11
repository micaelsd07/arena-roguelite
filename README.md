# Arena Roguelite

Roguelite de ação top-down (arena shooter) em **LibGDX 1.13 + Box2D**. Sobreviva a ondas de
inimigos numa arena fechada; ao subir de nível, escolha melhorias passivas para a arma.

## Rodar

Requer JDK 17+.

```bash
./gradlew :lwjgl3:run          # joga
./gradlew :core:test           # simula partidas inteiras sem janela (bot)
./gradlew :lwjgl3:dist         # jar único em lwjgl3/build/libs/arena-roguelite.jar
./gradlew :lwjgl3:generateAudio  # regenera os .wav em assets/sfx
```

## Controles

| Tecla | Ação |
|---|---|
| WASD | mover |
| Mouse | mirar |
| Botão esquerdo (segurar) | atirar |
| 1 / 2 / 3 ou clique | escolher melhoria |
| R | reiniciar após game over |
| M | mudo · `-` / `+` volume |

## Estrutura

```
core/src/main/java/com/arena
├── ArenaGame.java          Game: SpriteBatch, texturas, áudio
├── world/                  lógica pura (sem renderização, testável headless)
│   ├── GameWorld.java      Box2D, pools, contatos, XP, level up
│   ├── WaveManager.java    hordas por orçamento (10 + onda*5), limite de 300 vivos
│   ├── Bullet/Enemy/Gem    objetos Poolable
│   ├── Upgrade.java        12 melhorias acumuláveis
│   └── Phys.java           bits de colisão e tamanho da arena
├── render/                 WorldRenderer (metros), Hud (pixels), Assets (texturas via Pixmap)
├── screens/                MenuScreen, GameScreen (passo fixo 60 Hz, câmera seguindo)
├── audio/                  Sfx, AudioManager (throttle 50 ms, pitch ±10%, volume, música)
└── tools/SfxGenerator.java sintetizador chiptune que gera os .wav
```

### Decisões técnicas

- **Box2D em tudo**: paredes (estático, `ChainShape`), jogador e inimigos (dinâmicos),
  projéteis (**kinematic + sensor**). Filtros de categoria separam tiros do jogador e dos inimigos.
- **Trajetórias**: a velocidade de cada projétil é recalculada por frame — reta, senoidal
  (deslocamento perpendicular), teleguiada (curva limitada por rad/s) e orbital (posicionada via
  `setTransform`). Kinematic × estático não gera contato no Box2D, então ricochete e colisão com a
  parede usam os limites da arena.
- **Object pooling**: `Pool<Bullet>`, um `Pool<Enemy>` por tipo (o raio da fixture é fixo) e
  `Pool<Gem>`. Os corpos Box2D são criados uma vez e reaproveitados com `setActive(false/true)`.
  O HUD mostra os objetos livres em cada pool e o total de corpos — num teste com 500+ tiros,
  o mundo nunca passou de ~40 corpos.
- **Colisões** são coletadas no `ContactListener` e processadas depois do `step`
  (não se pode alterar corpos durante o step).
- **Renderização**: texturas brancas geradas em `Pixmap` e tingidas, num único `SpriteBatch`.
- **Áudio**: 13 efeitos e uma música em loop, sintetizados em código (onda quadrada, triangular,
  dente de serra e ruído com envelope). Para trocar por sons próprios, basta substituir os
  arquivos em `assets/sfx/` mantendo os nomes.

## Inimigos

| Tipo | Custo | A partir da onda | Comportamento |
|---|---|---|---|
| Chaser | 1 | 1 | persegue, dano por contato |
| Shooter | 3 | 2 | mantém 5–8 m de distância e atira |
| Tank | 5 | 3 | lento, muita vida, grande |

A vida dos inimigos cresce 12% por onda. A próxima onda começa quando a atual é limpa ou após 30 s.
