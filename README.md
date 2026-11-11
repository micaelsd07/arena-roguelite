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
| WASD / setas | mover |
| Mouse | mirar |
| Botão esquerdo (segurar) | atirar flechas |
| Espaço / Shift | dash (atravessa inimigos, 0,25 s invencível) |
| Q / botão direito | habilidade especial |
| 1 / 2 / 3 ou clique | escolher melhoria |
| R | reiniciar após a derrota |
| M · `-` / `+` | mudo · volume |
| F3 | mostrar/ocultar estatísticas de pooling |

## Arte

A arte parte das imagens de referência em `art/` e é processada por `tools/ArtBuilder`:

```bash
./gradlew :tools:run    # gera assets/atlas/game.atlas + game.png
```

- **Personagens e ícones** são recortados das planilhas (`hero.png`, `enemies.jpg`, `icons.jpg`):
  fundo removido por cor com tolerância adaptativa, números/rótulos e sombras descartados,
  redução por média de área e alfa binário para bordas nítidas.
- **Cenários** são pintados proceduralmente (tiles de chão/parede, pilares, sarcófagos, árvores,
  cogumelos, ruínas, fogueiras...) com a paleta extraída por k-means de cada painel de `biomes.jpg`.
- Para trocar a arte, substitua os arquivos em `art/` (mantendo a grade 8x4) e rode `:tools:run`.
  O mapeamento de células fica em `ArtBuilder.java`.

## Conteúdo

| Bioma | Ondas | Obstáculos | Chefe (onda) |
|---|---|---|---|
| Cripta Esquecida | 1–5 | pilares, sarcófagos | Cavaleiro-Rei (5) |
| Floresta Corrompida | 6–10 | árvores mortas, rochas | Lobisomem Alfa (10) |
| Ruínas Sombrias | 11+ | muros quebrados, colunas, fogueiras | Necromante Supremo (15), depois aleatório |

| Inimigo | Comportamento |
|---|---|
| Espectro | persegue flutuando e atravessa outros inimigos |
| Necromante | mantém distância e lança bolas de fogo |
| Lobisomem | para, rosna e investe em linha reta |
| Cavaleiro | lento, resistente, quase não sofre knockback |

O chefe deixa um baú com uma melhoria garantida, e a próxima onda espera o baú ser aberto.
**Habilidades especiais** (oferecidas no level up, uma equipada por vez, até o nível 5):
Chuva de Flechas, Escudo de Gelo, Raio em Cadeia e Chuva de Meteoros.

## Estrutura

```
core/src/main/java/com/arena
├── world/        lógica pura (testável headless): GameWorld, WaveManager, Biome, Enemy, Bullet,
│                 Obstacle, Special, Upgrade, WorldEvents...
├── render/       WorldRenderer (Y-sort, sombras, projéteis), Lighting (FrameBuffer de luz),
│                 Particles (pool), Fx (juice: partículas, tremor, números, raios), Hud, Assets
├── screens/      MenuScreen, GameScreen (passo fixo 60 Hz, câmera suave com tremor)
├── audio/        Sfx, AudioManager (throttle 50 ms, pitch ±10%, volume, música)
└── tools/        SfxGenerator (sintetizador chiptune dos .wav)
tools/            ArtBuilder, Painter, Packer (pipeline de arte, java.awt)
art/              imagens de referência (fonte da arte)
```

### Decisões técnicas

- **Box2D**: paredes e obstáculos estáticos, jogador e inimigos dinâmicos, projéteis kinematic +
  sensor. Kinematic × estático não gera contato, então projéteis testam paredes e obstáculos
  manualmente (com reflexão na normal para o ricochete). Durante o dash, a máscara de colisão do
  jogador vira só `WALL`.
- **Object pooling** em projéteis, inimigos (um pool por tipo), gemas, portais, meteoros,
  partículas e números de dano. Corpos Box2D são reaproveitados com `setActive`.
- **Iluminação**: luz ambiente por bioma; jogador, tochas, fogueiras, cogumelos, flechas, bolas
  de fogo, gemas, portais e explosões emitem luz num FrameBuffer multiplicado sobre a cena.
- **Renderização**: tudo vem de um único atlas, então o SpriteBatch faz poucas trocas de textura.
  Personagens são ordenados por Y; o flash de dano usa um shader de silhueta branca.
