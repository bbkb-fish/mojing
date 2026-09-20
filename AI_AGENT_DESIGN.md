# 墨境小说创作 Agent 设计说明

## 1. 项目定位

墨境目前是一套“AI 辅助小说编辑器 + 可控创作 Agent”。

运行可观测性采用两层记录：一次用户动作对应一个 `agent_run`，一次真实模型 HTTP 请求对应一条 `ai_call_log`。它能够表达“章节导演先规划、确认后再生成正文”这类包含多次模型调用的任务，并将快速续写纳入相同的监控口径。运行中心聚合展示模型、Token、耗时、重试和错误，后续可直接用于成本预算、模型/Prompt 对比和离线评测。

评测中心采用“确定性规则 + LLM Judge”的混合评测。规则负责长度、人物、必需词、禁用词和原文重复等可验证约束；Judge 负责连贯性、文风、剧情推进与指令遵循。`eval_case` 保存可复用样本，`eval_run` 保存每次输出和评分，并固定记录生成 Prompt 与 Judge Prompt 版本，使同一用例能用于模型切换和 Prompt 修改后的回归比较。

普通 AI 续写通常只有一次模型调用：

```text
正文结尾 + 作者要求 → 大模型 → 续写正文
```

这种方式实现简单，但容易出现人物串名、遗忘世界观、偏离大纲和擅自决定关键剧情等问题。

当前项目将创作拆成一个带状态、可追踪、需要作者确认的工作流：

```text
作者提出创作要求
        ↓
后端读取小说上下文
  ├─ 小说简介
  ├─ 故事大纲
  ├─ 人物档案
  ├─ 世界观设定
  └─ 当前章节及之前的最近章节
        ↓
AI 生成结构化章节计划
        ↓
作者修改并确认计划（Human-in-the-loop）
        ↓
AI 根据确认后的计划生成正文草稿
        ↓
作者检查并插入编辑器
        ↓
复用章节自动保存机制完成持久化
```

当前实现属于“确定性工作流 Agent”：Java 后端负责任务编排，大模型负责规划和写作，关键剧情方向由作者确认。

## 2. 设计目标

当前阶段主要解决以下问题：

1. 让 AI 知道它正在写哪一本小说，避免复用其他故事的人名和设定。
2. 将人物、世界观和故事大纲作为长期创作资料保存。
3. 在生成正文前先生成章节计划，提高结果的可控性。
4. 由作者确认关键计划，避免 AI 擅自改变故事方向。
5. 保存 Agent 的运行状态和每一步记录，便于调试、展示和后续恢复。
6. 保留原有快速续写，满足补写一小段内容的轻量场景。

## 3. 技术架构

### 3.1 前端

- Vue 3
- TypeScript
- Vite
- Element Plus

主要职责：

- 小说和章节管理
- 正文编辑与自动保存
- 人物、世界观资料维护
- 展示并编辑章节计划
- 发起计划确认和正文生成
- 展示 Agent 执行记录
- 将草稿插入正文编辑器

主要实现文件：

```text
frontend/src/App.vue
```

### 3.2 后端

- Java 21
- Spring Boot 3.4
- Spring MVC
- Spring Data JPA
- MySQL
- Java HttpClient
- Jackson

主要职责：

- 小说、章节和故事资料持久化
- 自动组装创作上下文
- 编排 Agent 工作流
- 调用 OpenAI 兼容模型接口
- 解析结构化章节计划
- 保存 Agent 运行状态和步骤
- 提供演示模式，未配置 API Key 时也能体验流程

## 4. Agent 工作流

### 4.1 创建章节计划

前端调用：

```http
POST /api/agent/novels/{novelId}/chapters/{chapterId}/plan
Content-Type: application/json

{
  "guidance": "让主角发现密室，但暂时不要揭晓幕后人物"
}
```

后端执行：

1. 校验小说和章节是否存在，以及章节是否属于当前小说。
2. 创建 `agent_run`，状态设为 `RUNNING`。
3. 从数据库加载小说简介、大纲、人物、世界观和最近章节。
4. 写入 `LOAD_CONTEXT` 步骤。
5. 调用大模型生成结构化 `ChapterPlan`。
6. 保存计划 JSON。
7. 将任务状态改为 `WAITING_APPROVAL`。
8. 写入 `PLAN_CHAPTER` 步骤。
9. 将计划返回前端供作者编辑。

### 4.2 人工确认计划

前端将计划拆分成可编辑字段：

- 标题建议
- 本章目标
- 开场方式
- 情节推进
- 高潮
- 结尾钩子
- 出场人物
- 连续性约束

作者可以修改任何字段。只有点击“确认计划并生成草稿”后，Agent 才会继续执行。

这一阶段对应 Human-in-the-loop，用于将重要剧情决策权保留给作者。

### 4.3 生成正文草稿

前端调用：

```http
POST /api/agent/runs/{runId}/draft
Content-Type: application/json

{
  "plan": {
    "title": "雾中的来客",
    "objective": "让主角得到密室线索",
    "opening": "从敲门声切入",
    "developments": [
      "陌生人交出一封旧信",
      "主角发现信纸上的特殊标记"
    ],
    "climax": "陌生人突然遭到袭击",
    "endingHook": "旧信背面浮现密室地图",
    "characters": ["沈舟", "陌生来客"],
    "continuityNotes": ["不要提前揭晓幕后人物"]
  },
  "targetLength": 1200
}
```

后端重新读取最新上下文，并使用作者修改后的计划生成正文。成功后：

```text
agent_run.status       = COMPLETED
agent_run.current_step = DRAFT_READY
agent_run.draft        = 生成的正文
```

同时新增一条 `WRITE_DRAFT` 步骤。

## 5. 创作上下文设计

当前上下文由后端统一组装，前端不直接拼接提示词。

示例：

```text
【作品】雾港来信
【简介】在海雾与旧信之间寻找失踪的真相。
【总纲】……

【人物档案】
- 沈舟（主角）；调查员；谨慎；目标是查明真相
- 林晚（盟友）；……

【世界观设定】
- [地点] 雾港：常年被海雾覆盖……
- [组织] 守夜人：……

【较早章节记忆】
第一章：主要事件、人物状态变化、伏笔与未解决问题……

【最近正文】
第二章 ……
第三章 ……
第四章 ……
```

当前上下文限制约为 20000 个字符。稳定的小说资料最多占 6000 字符，已确认的较早章节记忆最多占 6000 字符，其余空间优先给当前章节及前面两章的原文。这样既保留近期叙事细节，又能让模型记住长篇作品早期发生过的重要事件。

章节记忆采用 Human-in-the-loop 流程：AI 先生成 `GENERATED` 草稿，作者编辑后确认成 `CONFIRMED`。记忆保存提取时的章节版本；正文再次保存后，该记忆会变为 stale，不再进入后续创作上下文，直到重新提取并确认。旧的已确认版本会标记为 `SUPERSEDED`。

由后端组装上下文的原因：

- 避免前端漏传重要资料。
- 避免客户端任意修改系统级上下文。
- 便于未来增加章节摘要、伏笔检索和向量检索。
- 可以将数据读取操作进一步封装成 Agent Tool。

## 6. 结构化章节计划

大模型在规划阶段必须返回一个合法 JSON 对象：

```json
{
  "title": "string",
  "objective": "string",
  "opening": "string",
  "developments": ["string"],
  "climax": "string",
  "endingHook": "string",
  "characters": ["string"],
  "continuityNotes": ["string"]
}
```

后端使用 Jackson 将模型结果转换为 Java `ChapterPlan` record。

结构化输出的价值：

- 后端可以验证计划格式。
- 前端可以分字段展示和编辑。
- 正文生成阶段可以稳定引用。
- 后续可以对计划进行自动评测。
- 可以检查出场人物是否来自人物资料库。

如果模型返回 Markdown 代码块，后端会提取第一个 `{` 到最后一个 `}` 再解析。如果仍然无法解析，则终止当前请求，不继续生成正文。

## 7. 数据库设计

### 7.1 `story_character`

保存人物长期档案：

| 字段 | 含义 |
| --- | --- |
| `novel_id` | 所属小说 |
| `name` | 人物姓名 |
| `role` | 主角、反派、盟友等角色定位 |
| `description` | 身份、经历和背景 |
| `personality` | 性格特征 |
| `goal` | 目标和动机 |
| `version` | 乐观锁版本 |

### 7.2 `world_setting`

保存世界观规则：

| 字段 | 含义 |
| --- | --- |
| `novel_id` | 所属小说 |
| `category` | 地点、组织、力量体系、历史等分类 |
| `title` | 设定标题 |
| `content` | 详细内容和规则 |
| `version` | 乐观锁版本 |

### 7.3 `agent_run`

保存一次完整 Agent 任务：

| 字段 | 含义 |
| --- | --- |
| `novel_id` | 所属小说 |
| `chapter_id` | 基于哪个章节执行 |
| `status` | `RUNNING`、`WAITING_APPROVAL`、`COMPLETED` |
| `current_step` | 当前执行步骤 |
| `guidance` | 作者原始要求 |
| `plan_json` | AI 生成并经作者确认的计划 |
| `draft` | 最终正文草稿 |
| `provider_mode` | `ai` 或 `demo` |

### 7.4 `agent_step`

保存任务内部执行步骤：

| 字段 | 含义 |
| --- | --- |
| `run_id` | 所属 Agent 任务 |
| `step_no` | 步骤序号 |
| `type` | `LOAD_CONTEXT`、`PLAN_CHAPTER`、`WRITE_DRAFT` |
| `status` | 步骤状态 |
| `summary` | 执行摘要 |
| `duration_ms` | 执行耗时 |

关系：

```text
novel 1 ── N story_character
novel 1 ── N world_setting
novel 1 ── N agent_run
chapter 1 ── N agent_run
agent_run 1 ── N agent_step
```

手工建表脚本：

```text
backend/mysql_table/agent_tables.sql
```

## 8. 状态设计

当前任务状态流转：

```text
RUNNING / LOAD_CONTEXT
        ↓
RUNNING / PLAN_CHAPTER
        ↓
WAITING_APPROVAL / WAITING_PLAN_APPROVAL
        ↓ 作者确认计划
RUNNING / WRITE_DRAFT
        ↓
COMPLETED / DRAFT_READY
```

保存状态的意义：

- 前端刷新后可以重新查询任务。
- 可以知道任务停在哪一步。
- 便于统计各阶段耗时和失败率。
- 以后可以增加失败重试和断点恢复。
- 面试演示时可以展示 Agent 的真实执行轨迹。

## 9. 提示词设计

### 9.1 规划提示词

模型角色是“网络小说创作导演 Agent”，主要约束：

- 必须尊重人物、能力、关系、世界规则和总纲。
- 只返回固定结构 JSON。
- 不从其他故事借用人名。
- 资料没有姓名时使用身份称谓，不擅自命名。
- 规划明确的目标、推进过程、高潮和结尾钩子。

### 9.2 正文提示词

模型角色是“网络小说正文写作 Agent”，主要约束：

- 只输出可以插入编辑器的正文。
- 不输出标题、解释和 Markdown。
- 延续原文人物、视角、语气和世界规则。
- 严格执行作者确认后的章节计划。
- 不重复原文，不复用其他故事人名。
- 按目标字数生成，并以自然钩子收束。

## 10. 前端状态设计

右侧面板包含两个模式：

1. `章节导演`：完整 Agent 工作流。
2. `快速续写`：只根据当前正文结尾补写一小段。

章节导演的主要前端状态：

```ts
agentRunId   // 当前任务 ID
agentPlan    // 可编辑章节计划
agentDraft   // 正文草稿
agentSteps   // Agent 执行记录
planning     // 是否正在生成计划
drafting     // 是否正在生成正文
```

切换章节时会清空当前 Agent 状态，防止上一章计划被错误应用到下一章。

插入草稿时只修改正文编辑器内容，之后复用原有章节自动保存逻辑，不重复实现持久化。

## 11. 当前版本的 Agent 边界

目前由 Java 后端固定编排执行顺序，而不是由大模型自主选择工具。因此它属于确定性工作流 Agent，而不是完全自主 Agent。

当前没有让模型决定是否调用：

```text
getNovelOutline
listCharacters
listWorldSettings
getRecentChapters
searchChapterContent
```

使用固定编排是当前阶段的主动选择：

- 执行路径稳定。
- 容易测试和定位错误。
- 不会进入无限工具调用循环。
- Token 和接口成本更容易控制。
- Human-in-the-loop 更容易实现。

后续可以在稳定的数据层和运行记录基础上，将查询操作逐步包装为真正的 Tool Calling。

## 12. 当前不足

- AI 请求目前为同步调用，生成时间较长时 HTTP 请求会持续等待。
- 暂无 SSE 实时步骤推送。
- 暂无章节摘要，长篇上下文主要依赖最近三章。
- 暂无人物关系、事件时间线和伏笔管理。
- 一致性检查已支持当前章节，跨全书的事件时间线和伏笔检查仍待完善。
- 草稿暂无独立版本、差异对比和回滚。
- Agent 失败后暂不能从指定步骤恢复。
- 暂未实现真正的模型 Tool Calling。
- 暂无专门的 Agent 质量评测集。
- 暂无登录、用户隔离和完整权限控制。

## 13. 后续演进路线

### 阶段一：当前已完成

- 人物资料库
- 世界观资料库
- 章节规划
- 人工确认计划
- 正文草稿生成
- Agent 运行和步骤记录
- 正文选区的局部润色、重写、扩写、缩写和语气调整
- 原文与修改版人工确认
- 章节修改历史和安全回滚
- 当前章节一致性评分与结构化问题报告
- 问题定位、忽略、AI 修复和状态同步

### 局部修改工作流（当前已完成）

```text
作者选择正文片段
        ↓
REWRITE_SELECTION 读取前后文和故事资料
        ↓
AI 返回 rewrittenText、changeSummary 和 warnings
        ↓
作者对照原文与修改版
        ↓
接受并保存 / 放弃
        ↓
接受后的记录可以回滚
```

局部修改记录保存在 `chapter_revision`，状态依次为 `GENERATED`、`ACCEPTED`、`REJECTED` 或 `ROLLED_BACK`。AI 生成结果时不会直接覆盖章节；只有作者确认、前端完成替换并成功保存章节后，记录才会变为 `ACCEPTED`。

### 阶段二：故事长期记忆

增加：

- `chapter_summary`：章节摘要
- `story_event`：关键事件和时间线
- `character_relation`：人物关系
- `foreshadowing`：伏笔及回收状态
- 自动从已保存章节中提取和更新资料

### 阶段三：质量检查工作流（当前已完成第一版）

在正文生成后增加：

```text
WRITE_DRAFT
    ↓
CHECK_CONSISTENCY
    ↓
REVISE_DRAFT
    ↓
WAITING_DRAFT_APPROVAL
```

一致性检查输出结构化报告：

- 人物行为是否符合设定
- 人物名是否来自当前小说
- 时间线是否冲突
- 是否偏离总纲和章节计划
- 是否遗忘需要推进或回收的伏笔
- 视角和文风是否一致

质量检查和章节记忆通过可选的“章节整理”入口组合，但不会成为写作流程的门槛：

```text
正常写作 / 快速续写 / 段落打磨
              ↓ 作者按需触发
        ┌──── 章节整理 ────┐
        │                   │
  一致性检查          章节记忆提取
  可处理或忽略        可编辑、确认或关闭
```

章节的乐观锁 `version` 与正文版本 `content_version` 分离。只有标题或正文发生变化才增加 `content_version`，此前的检查报告和章节记忆会被判定为 stale。系统不要求章节进入特定状态，也不阻止作者继续写作。

### 阶段四：异步执行与可观测性

- 使用 SSE 推送步骤进度。
- 支持取消、失败重试和断点恢复。
- 增加 Token、延迟和费用统计。
- 接入 Micrometer、Prometheus、Grafana 或 OpenTelemetry。
- 增加 Agent 运行历史页面。

### 阶段五：Tool Calling 与检索

将数据能力包装为工具：

```text
getNovelInfo
getNovelOutline
listCharacters
getCharacter
listWorldSettings
getRecentChapters
searchChapterContent
listUnresolvedForeshadows
saveChapterRevision
```

章节数量较大后，再考虑章节摘要检索或向量检索，不在数据量很小时过早引入复杂 RAG。

### 阶段六：工程化与求职展示

- 登录、JWT 和多用户数据隔离
- Docker Compose 一键启动
- 数据库版本迁移
- OpenAPI 接口文档
- GitHub Actions 自动测试
- Agent 评测集和评测报告
- 系统架构图、时序图、演示视频
- 线上部署和运行监控

## 14. 面试时的项目表述

可以将项目概括为：

> 我实现了一套面向长篇小说创作的 Human-in-the-loop Agent。系统会从数据库加载小说大纲、人物、世界观和最近章节，先让模型生成结构化章节计划，作者确认后再生成正文。每次运行和内部步骤都会持久化，便于追踪、评测和后续恢复。相比一次性续写，这种设计提高了剧情可控性，也为后续一致性检查、长期记忆和 Tool Calling 提供了稳定基础。

需要诚实说明：当前版本主要是后端确定性编排，不是让大模型无限自主循环。这样做是为了优先保证稳定性、可测试性和成本可控。

## 15. 关键代码位置

```text
frontend/src/App.vue

backend/src/main/java/com/mojing/novel/writing/NovelAgentService.java
backend/src/main/java/com/mojing/novel/writing/NovelAgentController.java
backend/src/main/java/com/mojing/novel/writing/AgentDtos.java
backend/src/main/java/com/mojing/novel/writing/StoryBibleService.java
backend/src/main/java/com/mojing/novel/writing/StoryBibleController.java

backend/mysql_table/agent_tables.sql
backend/src/test/java/com/mojing/novel/writing/NovelAgentControllerTest.java
```
