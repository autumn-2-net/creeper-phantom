# 苦力幻翼 · Creeper Phantom

把苦力怕的自爆和幻翼的空袭合在一起：绿色头身、蓝色双翼，盘旋在失眠玩家头顶。找不到玩家时，它还有机会转而轰炸露天容器。

适用于 **Minecraft 1.20.1 / Forge 47.3.7+（47.x）**，无需其他内容模组。提供普通与高压两种形态、中英文名称和独立刷怪蛋。

## 安装

将构建生成的 `creeper-phantom-forge-1.20.1-<版本>.jar` 放入游戏的 `mods` 目录。多人游戏需要客户端与服务端同时安装；不要将 `-sources.jar` 当作模组安装。

## 自然生成与攻击

- **25% 的自然幻翼会被替换**为苦力幻翼，沿用原版的失眠、夜晚、难度和天空等生成条件，不额外增加生成轮次。
- 不替换存档里已有的幻翼，也不替换命令或刷怪蛋生成的原版幻翼。
- 保留幻翼的盘旋、俯冲、怕猫和日光燃烧行为。
- 普通形态接触攻击目标后点燃 **20 tick（约 1 秒）**的引信，随后以普通爬行者的强度 **3** 自爆。引信点燃后不会因为目标逃走而取消。

## 容器轰炸

无法找到可攻击且可见的玩家时，每隔 **200～239 tick** 进行一次搜索判定，有 **10% 概率**尝试攻击容器。这是每次搜索的概率，不是每 tick 的概率；能重新索敌玩家时优先攻击玩家。

目标必须露天且视线可达。默认搜索水平半径 **32 格**、垂直距离不超过 **48 格**，只检查已加载区块，每轮最多检查 4096 个方块实体。

支持有物品槽位的 `Container` 容器，以及通过 Forge `ITEM_HANDLER` 能力提供物品槽位的模组方块，包括只在特定面开放的物品接口。没有接入这些接口的特殊容器不会被识别。

容器追击最多持续 200 tick；目标失去视线、撞墙、容器消失或检测到玩家/猫时会放弃。

## 高压形态

普通苦力幻翼被闪电击中后进入高压形态，周身出现流动电弧。自然闪电、引雷和命令产生的实际雷击都能触发转换，强化状态会保存到存档。

| 形态 | 默认爆炸强度 | 攻击方式 |
| --- | ---: | --- |
| 普通 | 3 | 接触后等待引信结束 |
| 高压 | 6 | 接触时立即自爆 |

已经点燃引信的普通形态被雷击时，也会立即引爆。雷击伤害和着火保留原版处理，因此濒死个体仍可能被雷击杀死。爆炸强度不是固定的破坏半径，实际破坏范围受方块抗爆性等原版规则影响。

两种形态的爆炸均遵循 **`mobGriefing`**：关闭时不主动轰炸容器，攻击玩家仍可爆炸并造成实体伤害，但不破坏方块。爆炸通过 Forge 原生事件处理。

## 刷怪蛋与命令

创造模式“刷怪蛋”分类中搜索 **苦力幻翼**，即可找到普通和高压两种刷怪蛋。

```mcfunction
/give @s creeperphantom:creeper_phantom_spawn_egg
/give @s creeperphantom:charged_creeper_phantom_spawn_egg
/summon creeperphantom:creeper_phantom ~ ~3 ~
/summon creeperphantom:charged_creeper_phantom ~ ~3 ~
```

## 配置

进入世界后，在该存档的 `serverconfig/creeperphantom-server.toml` 中调整配置。

| 配置项 | 默认值 | 含义 |
| --- | ---: | --- |
| `naturalReplacementChance` | `0.25` | 自然幻翼替换比例 |
| `containerBombingChance` | `0.10` | 每轮容器搜索的触发概率 |
| `containerSearchInterval` | `200` | 搜索间隔 tick 数，另加 0～39 tick 随机间隔 |
| `containerSearchRadius` | `32` | 水平搜索半径，单位为格 |
| `attackModdedContainers` | `true` | 在 `Container` 之外识别 Forge 物品能力接口 |
| `fuseTicks` | `20` | 普通形态引信时间 |
| `explosionPower` | `3.0` | 普通形态爆炸强度 |
| `chargedExplosionPower` | `6.0` | 高压形态爆炸强度 |

## 从源码构建

需要 **JDK 17 或以上**，通过 `JAVA_HOME` 或 `PATH` 指定。游戏运行建议使用 Java 17。仓库包含 Gradle Wrapper，首次构建需要联网下载依赖。

Windows PowerShell：

```powershell
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle-user-home'
.\gradlew.bat build
```

Linux / macOS：

```sh
export GRADLE_USER_HOME="$PWD/.gradle-user-home"
./gradlew build
```

产物位于 `build/libs/`。上述命令把 Gradle 依赖缓存留在项目目录中；缓存、运行目录和构建产物不纳入版本控制。

## 验证

已通过 Forge GameTest 集成检查，并收到游戏内安装测试可用的反馈。测试覆盖两种刷怪蛋、雷击与存档恢复、引信与瞬爆、自然替换、容器遮挡和 `mobGriefing`。完整范围见 [VERIFICATION.md](VERIFICATION.md)。

```sh
./gradlew -Pverification runGameTest
./gradlew clean build
```

Windows 将 `./gradlew` 替换为 `.\gradlew.bat`。测试源码和结构位于 `src/verification/`，只有启用 `-Pverification` 才会编译；测试后执行普通 `clean build`，生成不含验证代码的模组包。测试世界写入项目下的 `run-gametest/`。

## 美术与授权

生物使用自定义立方体模型和扇翼动画。实体图集由图像生成工具制作，保存在 [creeper_phantom.png](src/main/resources/assets/creeperphantom/textures/entity/creeper_phantom.png)，对应提示词见 [art/texture-prompt.txt](art/texture-prompt.txt)。高压电弧引用游戏自带的高压爬行者材质。

项目当前采用 **All Rights Reserved**；Gradle Wrapper 保留其原有许可证声明。
