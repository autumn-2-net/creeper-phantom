# 苦力幻翼与末影苦力怕 · Creeper Phantom

把苦力怕的自爆分别与幻翼空袭、末影人瞬移结合：苦力幻翼盘旋俯冲，末影苦力怕瞬移到目标身边。两者都能自主袭击露天容器，并可选联动 GT 多方块机器。

适用于 **Minecraft 1.20.1 / Forge 47.3.7+（47.x）**，无需其他内容模组。两种生物各有普通、高压形态，中英文名称及刷怪蛋，共四种刷怪蛋。

## 安装

将构建生成的 `creeper-phantom-forge-1.20.1-<版本>.jar` 放入游戏的 `mods` 目录。多人游戏需要客户端与服务端同时安装；不要将 `-sources.jar` 当作模组安装。

## 自然生成与攻击

- **25% 的自然幻翼会被替换**为苦力幻翼，沿用原版的失眠、夜晚、难度和天空等生成条件，不额外增加生成轮次。
- 不替换存档里已有的幻翼，也不替换命令或刷怪蛋生成的原版幻翼。
- 保留幻翼的盘旋、俯冲、怕猫和日光燃烧行为。
- 普通形态接触攻击目标后点燃 **20 tick（约 1 秒）**的引信，随后以普通爬行者的强度 **3** 自爆。引信点燃后不会因为目标逃走而取消。

## 容器轰炸

苦力幻翼无法找到可攻击且可见的玩家时，每隔 **200～239 tick** 进行一次搜索判定，有 **10% 概率**尝试攻击容器。这是每次搜索的概率，不是每 tick 的概率；能重新索敌玩家时优先攻击玩家。末影苦力怕的主动容器搜索规则见下一节。

目标必须露天且视线可达。默认搜索水平半径 **32 格**、垂直距离不超过 **48 格**，只检查已加载区块，每轮最多检查 4096 个方块实体。

支持有物品槽位的 `Container` 容器，以及通过 Forge `ITEM_HANDLER` 能力提供物品槽位的模组方块，包括只在特定面开放的物品接口。没有接入这些接口的特殊容器不会被识别。

苦力幻翼的容器追击最多持续 200 tick；目标失去视线、撞墙、容器消失或检测到玩家/猫时会放弃。

## 末影苦力怕

绿色的末影人身形、紫色发光眼睛和苦力怕腹纹。默认替换 **25% 的自然末影人**，沿用末影人的自然生成环境，包括原版允许其生成的维度。

- 对玩家保留末影人的中立规则：**被注视或攻击后才敌对**，雕刻南瓜防注视、怕水、瞬移闪避和搬方块等行为沿用原版。
- 敌对后尝试瞬移到玩家身边，普通形态点燃约 **1 秒引信**；高压形态瞬移成功后立即自爆。
- **容器是主动搜索的目标，不需要先激怒生物。** 每轮搜索有 10% 概率尝试袭击露天可见容器，附近有尚未激怒它的玩家也不会阻止搜索；已有敌对目标时优先处理战斗。
- 同样支持下述 25% GT 多方块袭击；机器袭击可以优先于敌对玩家。
- 瞬移落点需要已加载、有地面、足够容纳身高的空间且没有液体；找不到位置或 Forge 事件取消瞬移时，不会点燃引信。每轮最多为 16 个候选方块探测落点，避免无处落脚时反复大量搜索。
- 引信点燃后停止自主移动和随机瞬移。闪电强化、爆炸强度和 `mobGriefing` 行为与苦力幻翼一致。

苦力幻翼的怕猫行为不套用到末影苦力怕；末影形态沿用末影人的习性。

## GT / GTL 多方块联动

安装 GTCEu 时，两种生物每轮机器搜索有 **25% 概率**尝试轰炸附近**已成型的多方块控制器**，包括使用同一控制器接口的 GTLCore、GTL Additions 等附属机器。没有安装 GTCEu 时，联动自动关闭，模组仍可独立运行。

机器袭击可以优先于当前玩家目标；苦力幻翼一旦开始俯冲，不会因为仍能看见玩家就立刻折返。它会寻找控制器旁边露天、可见且有足够飞行空间的外露面，而不只检查控制器正上方。末影苦力怕则需要控制器附近有合适的地面落点。控制器未成型、变为未成型或被移除时，不会继续选择该目标。

机器判定与容器的 10% 判定分开，控制器不会因为有物品接口就再走一次容器判定。未命中机器袭击或没有可接近的控制器时，继续原有行为。搜索沿用上述距离、间隔和区块检查上限，并遵循 `mobGriefing`；苦力幻翼仍然怕猫。

靠近控制器后，普通形态使用引信，高压形态立即自爆。联动不会直接删除控制器或整台机器，能否炸毁及具体破坏范围仍由正常爆炸和方块抗爆性决定。

## 高压形态

普通苦力幻翼和末影苦力怕被闪电击中后进入高压形态，周身出现流动电弧。自然闪电、引雷和命令产生的实际雷击都能触发转换，强化状态会保存到存档。

| 形态 | 默认爆炸强度 | 攻击方式 |
| --- | ---: | --- |
| 普通 | 3 | 接触后等待引信结束 |
| 高压 | 6 | 接触时立即自爆 |

已经点燃引信的普通形态被雷击时，也会立即引爆。雷击伤害和着火保留原版处理，因此濒死个体仍可能被雷击杀死。爆炸强度不是固定的破坏半径，实际破坏范围受方块抗爆性等原版规则影响。

两种形态的爆炸均遵循 **`mobGriefing`**：关闭时不主动轰炸容器，攻击玩家仍可爆炸并造成实体伤害，但不破坏方块。爆炸通过 Forge 原生事件处理。

## 刷怪蛋与命令

创造模式“刷怪蛋”分类中搜索 **苦力幻翼**，即可找到普通和高压两种刷怪蛋。

搜索 **末影苦力怕** 可找到新增的两种刷怪蛋。

```mcfunction
/give @s creeperphantom:creeper_phantom_spawn_egg
/give @s creeperphantom:charged_creeper_phantom_spawn_egg
/summon creeperphantom:creeper_phantom ~ ~3 ~
/summon creeperphantom:charged_creeper_phantom ~ ~3 ~
/give @s creeperphantom:ender_creeper_spawn_egg
/give @s creeperphantom:charged_ender_creeper_spawn_egg
/summon creeperphantom:ender_creeper ~ ~ ~
/summon creeperphantom:charged_ender_creeper ~ ~ ~
```

## 配置

进入世界后，在该存档的 `serverconfig/creeperphantom-server.toml` 中调整配置。

自然生成开关**只控制自然生成**：关闭后仍保留刷怪蛋、命令、已有生物及雷击转换。倍率支持 `0～64`，以一次有效原版生成事件为基准：`0` 不替换，`0.25` 约 25% 替换一只，`1` 每次替换一只，`2` 尝试生成两只，`2.5` 尝试生成两只并有 50% 概率再加一只。额外个体仍需要空间、已加载区块等条件；末影人的额外个体还会重新检查原版生成规则，因此实际数量可能少于请求数量。原版刷怪上限仍影响后续生成，不保证全世界数量严格按倍率增长。

`naturalReplacementChance` 保留旧配置键名，现在是苦力幻翼的生成倍率。示例见 [examples/creeperphantom-server.toml](examples/creeperphantom-server.toml)；希望新存档使用自定义默认配置时，可将文件放进游戏的 `defaultconfigs/` 目录。

| 配置项 | 默认值 | 含义 |
| --- | ---: | --- |
| `phantomNaturalSpawns` | `true` | 开关苦力幻翼自然生成 |
| `naturalReplacementChance` | `0.25` | 苦力幻翼生成倍率，可大于 1 |
| `enderNaturalSpawns` | `true` | 开关末影苦力怕自然生成 |
| `enderSpawnMultiplier` | `0.25` | 末影苦力怕生成倍率，可大于 1 |
| `containerBombingChance` | `0.10` | 每轮容器搜索的触发概率 |
| `containerSearchInterval` | `200` | 搜索间隔 tick 数，另加 0～39 tick 随机间隔 |
| `containerSearchRadius` | `32` | 水平搜索半径，单位为格 |
| `attackModdedContainers` | `true` | 在 `Container` 之外识别 Forge 物品能力接口 |
| `attackGtMultiblocks` | `true` | 启用可选 GT 多方块袭击 |
| `gtMultiblockBombingChance` | `0.25` | 每轮机器搜索触发概率，允许优先于玩家目标 |
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

0.3.0 已通过全部 4 项 Forge GameTest 集成检查，覆盖四种刷怪蛋、雷击与存档恢复、引信与瞬爆、自然替换与倍率开关、末影注视规则、主动容器袭击、瞬移取消和安全落点。GT 联动使用接口替身检查控制器选择与两种生物的目标优先级；这不等于已经在完整 GTL 整合包中实测。此前已收到苦力幻翼基础版本游戏内可用的反馈，新增末影形态的客户端外观仍需游戏内确认。完整范围见 [VERIFICATION.md](VERIFICATION.md)。

```sh
./gradlew -Pverification runGameTest
./gradlew clean build
```

Windows 将 `./gradlew` 替换为 `.\gradlew.bat`。测试源码和结构位于 `src/verification/`，只有启用 `-Pverification` 才会编译；测试后执行普通 `clean build`，生成不含验证代码的模组包。测试世界写入项目下的 `run-gametest/`。

## 美术与授权

生物使用自定义立方体模型和扇翼动画。实体图集由图像生成工具制作，保存在 [creeper_phantom.png](src/main/resources/assets/creeperphantom/textures/entity/creeper_phantom.png)，对应提示词见 [art/texture-prompt.txt](art/texture-prompt.txt)。高压电弧引用游戏自带的高压爬行者材质。

末影形态使用独立模型、行走和搬方块姿势，以及发光眼睛。图集同样通过内置图像生成工具生成：[ender_creeper.png](src/main/resources/assets/creeperphantom/textures/entity/ender_creeper.png)，提示词保存在 [art/ender-texture-prompt.txt](art/ender-texture-prompt.txt)。

项目当前采用 **All Rights Reserved**；Gradle Wrapper 保留其原有许可证声明。
