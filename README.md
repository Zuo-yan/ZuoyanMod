# ZuoyanMod

包含核心机制模组 `ZuoyanMod` 以及独立的 AI 助手模组 `AI-Assisted`。

| 项 | 值 |
|---|---|
| 加载器 | NeoForge `26.3.0.3-beta` |
| Minecraft | `1.26.3` |
| 模组版本 | 见 `ZuoyanMod/gradle.properties` 的 `mod_version` |
| 许可 | All Rights Reserved |
| 可选依赖 | Curios（装了则饰品必须佩戴生效，没装则放背包生效） |

## 安装

1. 装好对应版本的 NeoForge
2. 把 `zuoyanmod-<版本>.jar` 丢进 `mods/`
3. 启动游戏（首次启动会在 `config/` 下生成 `zuoyanmod-common.toml`）

开发环境：见 `ZuoyanMod/` 下的 Gradle 工程，`./gradlew runClient` 起客户端、`./gradlew build` 构建。

## 独立 AI 助手模组（AI-Assisted）

接**你自己的**大模型密钥，在游戏里中文问答，并让它读游戏状态。

1. 按 `O` → 主界面第一行 **API Key** 粘贴密钥 → 「应用」
2. 选 `provider`（`openai-compatible` 覆盖 OpenAI / DeepSeek / 通义 / 智谱 / Claude 兼容层 / LM Studio…；本地 Ollama 单独一档）→ 填 `baseUrl` 与模型名
3. **把 `ai.maxTokens` 调到 2048~4096** —— 默认 1024 够聊天，但装不下"建造蓝图"这类长输出，会表现为「模型返回了空回复」
4. 聊天里说 `ai:<问题>`（需先开 `ai.chatPrefixEnabled`），或直接用 `/ai chat <问题>`

它能做的事、会外发什么、两档"动手"能力（提议执行指令 / 按蓝图建造）各自的门禁与确认流程、排错表，全部写在 **[AI-Assisted/docs/ai.md](AI-Assisted/docs/ai.md)**。

要点先说：密钥加密存在 `config/zuoyanmod/ai-secret.dat`，**永不写进 TOML、永不进日志、永不下发客户端**；任何"改世界"的动作都**默认关闭**且**必须你在游戏内确认**；`/ai undo` 可撤销最近一次 AI 建造。

## 文档

游戏内可用 `/ai help` 查看命令。仓库里的功能文档：

| 文档 | 内容 |
|---|---|
| [AI-Assisted/docs/ai.md](AI-Assisted/docs/ai.md) | **AI 助手**：配置、命令、只读工具、危险级能力、隐私边界、排错 |
| [docs/absolute_zero.md](docs/absolute_zero.md) | 绝对零度 |
| [docs/vacuum_decay.md](docs/vacuum_decay.md) | 真空衰变 |
| [docs/void_resonance_pump.md](docs/void_resonance_pump.md) | 虚空共振泵 |
| [docs/primordial_black_hole.md](docs/primordial_black_hole.md) | 原始黑洞 |
| [docs/klein_bottle.md](docs/klein_bottle.md) | 克莱因瓶 |
| [docs/universal_tools.md](docs/universal_tools.md) | 通用工具 |
| [docs/upgrade_system.md](docs/upgrade_system.md) | 升级系统 |
| [docs/rick_trade.md](docs/rick_trade.md) | 瑞克交易 |
| [docs/item_acquisition.md](docs/item_acquisition.md) | 物品获取途径总表 |
| [docs/achievements.md](docs/achievements.md) | 成就 |
| [docs/version_migration.md](docs/version_migration.md) | 跨 Minecraft 版本迁移指南（开发向） |

## 许可

All Rights Reserved。转载、整合、二次分发前请先联系作者。
