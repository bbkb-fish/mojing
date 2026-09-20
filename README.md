# 墨境 AI 小说创作

项目目录：

- `frontend`：Vue 3 + TypeScript + Vite
- `backend`：Java 21 + Spring Boot 3

## 本地启动

首次启用创作 Agent 时，先在 MySQL 的 `bbkb_novel` 数据库执行：

```text
backend/mysql_table/table.sql
```

该脚本包含小说、章节、人物档案、世界观、Agent 运行记录、修改历史、一致性检查和章节记忆的完整表结构。项目使用 `ddl-auto: validate`，缺少表时后端会拒绝启动。

如果数据库中已经存在此前的 Agent 表，只需执行本次增量脚本：

```text
backend/mysql_table/alter_add_content_version.sql
backend/mysql_table/alter_add_agent_observability.sql
backend/mysql_table/alter_add_eval_center.sql
backend/mysql_table/alter_add_character_affiliations.sql
backend/mysql_table/alter_add_story_bible_chat.sql
backend/mysql_table/alter_add_novel_project_profile.sql
backend/mysql_table/alter_add_novel_project_chat.sql
```

如果已经创建过资料库会话表，执行 `backend/mysql_table/alter_add_story_bible_chat_retrieval.sql`，为现有会话增加资料关联和旧对话召回索引。

分卷表按数据库当前状态选择一个脚本执行：尚未创建 `story_volume` 时执行 `backend/mysql_table/alter_add_story_volumes.sql`；已经在上一版本创建过该表时，只执行 `backend/mysql_table/alter_add_story_volume_chapter_facts.sql`。

## 立项工作台

新建小说只需要填写名称和一句灵感，创建后会进入独立的“立项工作台”。这里可以维护题材、频道、目标读者、发布平台、核心卖点、主角金手指、升级路线、预计篇幅、卷数、单章字数、读者期待和开篇三章设计。

AI 立项顾问使用持久化多轮对话，可以继续追问和调整方向，也可分别执行“完善设定”“生成简介”“生成总纲”或“整理全案”。AI 提出的修改会作为方案卡保存，只有作者点击“应用到正式立项”后才会更新作品资料。作者手工修改表单时，停止输入约一秒后自动保存。

已写作品打开立项工作台时，正文默认不会发送给 AI。作者可在某一轮主动开启“导入已写正文”：3 万字内使用全文；超过 3 万字时使用前 3 章、最近 2 章原文，中间章只使用与正文版本一致的已确认压缩。方案生成后若正式立项资料或正文发生变化，旧方案会拒绝应用，避免覆盖新内容。

已有数据库需要先执行 `backend/mysql_table/alter_add_novel_project_profile.sql` 和 `backend/mysql_table/alter_add_novel_project_chat.sql`，否则 `ddl-auto: validate` 会阻止后端启动。
如果立项对话表是在长期记忆功能之前已创建的，只需再执行 `backend/mysql_table/alter_add_novel_project_chat_memory.sql`，为现有会话增加结构化记忆账本。

## 分卷主线

故事资料库中的“分卷主线”支持手工维护，也可以让 AI 根据指定卷范围内已经写出的章节生成。系统会把已有正文视为不可推翻的事实，分别整理“已写剧情事实”和“剩余剧情安排”；作者确认后，当前卷主线才会进入章节导演、草稿生成和一致性检查的上下文。

分卷记录会保存本次分析覆盖到的章节及其正文版本。相关章节再次修改后，界面会提示主线需要更新，过期的卷主线不会继续注入创作上下文。

章节压缩完成后，作者点击“确认并用于后续创作”，系统会把该章的摘要、主线推进和未解决问题自动归纳到所属分卷的“章节压缩自动归纳”中。同一章节重新压缩并确认时会替换旧记录，不会覆盖作者手工维护的卷级剧情事实。

故事资料库右侧的“资料库顾问”保留整本小说的多轮对话。AI 会读取当前关注的分卷、人物、组织或世界设定，并可提出新增或修改人物、组织、世界观设定和分卷的建议卡；建议只有在作者点击应用后才会写入正式资料库。修改分卷时会保留已经归纳的章节事实、已写剧情、锁定节拍和分析进度。

资料库消息会按人物、组织、分卷和世界设定建立本地关联。超过近期窗口后，系统根据本轮共同对象、当前关注范围和主题重合度召回少量早期原始问答，并保存引用的来源消息 ID；不会因为主角出现频繁而全量加载其历史。

先启动后端：

```powershell
cd backend
mvn spring-boot:run
```

再启动前端：

```powershell
cd frontend
pnpm install
pnpm run dev
```

- 前端：http://127.0.0.1:5174
- 后端：http://127.0.0.1:8081
- 健康检查：http://127.0.0.1:8081/actuator/health

未设置 `AI_API_KEY` 时，后端返回本地演示计划和草稿，便于直接体验。设置 OpenAI 兼容模型的环境变量后会调用真实 AI：

```powershell
$env:AI_API_KEY = "你的密钥"
$env:AI_BASE_URL = "https://api.openai.com/v1"
$env:AI_MODEL = "gpt-4.1-mini"
```

AI HTTP 客户端默认强制直连，不继承 IDE、JVM 或 Windows 系统代理。如果模型地址必须通过系统代理访问，再设置 `$env:AI_USE_SYSTEM_PROXY = "true"`；恢复直连时删除该环境变量或设为 `false`，然后重启后端。

AI HTTP 客户端默认强制直连，不继承 IDE、JVM 或 Windows 系统代理。如果模型地址必须通过系统代理访问，再设置 `$env:AI_USE_SYSTEM_PROXY = "true"`；恢复直连时删除该环境变量或设为 `false`，然后重启后端。

## 创作 Agent

编辑器左侧的“Agent 运行记录”是统一运行中心，覆盖章节导演、快速续写、段落打磨、一致性检查和章节记忆。后端用 `agent_run` 保存一次业务任务，用 `ai_call_log` 保存任务内的每次模型调用，并聚合模型名、输入/输出 Token、耗时、重试次数与错误信息。

列表接口为 `GET /api/agent/novels/{novelId}/runs?limit=50`，详情接口为 `GET /api/agent/runs/{runId}`。

## Agent 评测中心

左侧“Agent 评测中心”可以把固定的正文上下文、续写要求、预期人物、必需词、禁用词和长度范围保存为回归用例。每次运行都会重新生成续写，先执行确定性规则，再由 LLM Judge 评价连贯性、人物一致性、文风、情节推进和指令遵循。

评测结果会保存生成文本、规则/Judge/综合评分、违规项、模型、Prompt 版本、Token 与耗时。未配置 API Key 时使用演示续写和模拟 Judge，仍可体验完整流程。

## Embedding 连通性测试

配置 `EMBEDDING_API_KEY` 后，可通过 `POST /api/embeddings/test` 输入一段文字并查看完整浮点向量、维度和 Token 用量。默认使用百炼 OpenAI 兼容地址、`text-embedding-v4` 和 1024 维；如果使用业务空间专属地址，可通过 `EMBEDDING_BASE_URL` 覆盖。

## Qdrant 向量记忆测试

通过 `QDRANT_BASE_URL`、`QDRANT_API_KEY` 配置远程 Qdrant。章节事实继续写入 `QDRANT_COLLECTION`（默认 `novel_memory`）；故事资料库多轮对话写入完全独立的 `QDRANT_STORY_BIBLE_COLLECTION`（默认 `novel_story_bible_chat`）。后端会在第一次写入或检索时自动创建对应集合，并检查它是否为 1024 维 Cosine 集合。资料库对话的向量写入或语义召回失败时，会自动退回数据库中的人物、组织、分卷和设定关联召回，不影响消息本身保存。

- `POST /api/vector-memories/initialize`：检查并创建集合。
- `POST /api/vector-memories`：将文本生成 Embedding 后写入 Qdrant。
- `POST /api/vector-memories/search`：根据输入文本检索相似记忆，并强制按 `novelId` 隔离。

当前接口用于验证 Embedding 与 Qdrant 的完整链路；后续再把已确认的章节结构化记忆自动同步到该集合，并在续写前检索相关上下文。

1. 打开一本小说，在左侧“故事资料库”维护人物和世界观。
2. 在右侧“章节导演”填写本次创作要求并生成章节计划。
3. 修改并确认计划，再生成正文草稿。
4. 检查草稿后插入当前章节，编辑器会自动保存。

局部修改已有正文：

1. 在正文编辑器中拖动选择需要修改的文字。
2. 打开右侧“段落打磨”，选择润色、重写、扩写、缩写、语气或自定义。
3. 对照原文和修改版，确认后替换并自动保存。
4. 已接受的修改可以从当前章节的修改历史中安全回滚。

检查并修复当前章节：

1. 打开右侧“质量检查”，可以填写本次检查重点或直接全面检查。
2. 查看评分以及人物、设定、时间线、大纲、视角和逻辑问题。
3. 可以定位原文、忽略问题，或让 AI 生成修复版本。
4. 修复版本仍需在“段落打磨”中人工确认；接受、放弃或回滚会同步更新问题状态。

压缩并确认章节记忆：

1. 选择章节，打开左侧“当前章节记忆”。
2. 让 AI 从正文提取章节摘要、人物变化、关键事件、地点、伏笔和未解决问题。
3. 作者可以修改提取结果，再点击“确认并用于后续创作”。
4. 章节正文再次修改后，旧记忆会标记为已过期，需要重新提取和确认。

只有作者确认且未过期的记忆会进入 AI 上下文。较早章节使用结构化记忆，靠近当前创作位置的最近三章仍使用原文，在长期连贯性与 Token 成本之间取得平衡。

可选的章节整理：

1. 打开右侧“章节整理”，可以单独执行一致性检查或章节记忆提取。
2. 也可以点击“检查并提取章节记忆”，一次生成检查报告和可编辑的记忆草稿。
3. 检查问题不要求必须处理，记忆草稿也可以直接关闭。
4. 只有作者主动确认的记忆才会作为高可信资料进入后续 Agent 上下文。

章节整理完全可选，作者可以只使用快速续写、章节导演或段落打磨。标题或正文变化时 `content_version` 才会增加，并让旧检查报告和章节记忆自动标记为过期。

后端会自动读取作品简介、大纲、人物、世界观和当前章节之前的最近三章，并记录 `LOAD_CONTEXT`、`PLAN_CHAPTER`、`WRITE_DRAFT`、`REWRITE_SELECTION`、`CHECK_CONSISTENCY` 等执行步骤。
