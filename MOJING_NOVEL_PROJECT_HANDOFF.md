# 墨境小说项目交接说明

更新时间：2026-09-07

## 1. 项目位置

正式项目目录：

```text
F:\proj-big\mojing-novel
├─ frontend   Vue 前端
└─ backend    Spring Boot 后端
```

本次旧任务实际绑定的工作区仍是：

```text
F:\proj-big\yun-picture
```

因此旧任务不能稳定写入 `F:\proj-big\mojing-novel`。请在新任务中确认 `cwd` 或可写根目录包含：

```text
F:\proj-big\mojing-novel
```

## 2. 项目目标

这是一个面向网络小说作者的 AI Agent 写作平台。核心目标不是简单调用大模型续写，而是维护长篇小说中的剧情记忆、人物状态、组织、世界观、伏笔和文风，使多轮创作保持连续性。

目前主要支持：

- 小说与章节的新建、编辑、删除和持久化。
- 正文自动保存、撤销、重做、字号设置、目录跳转和左右面板调整。
- AI 快速续写。
- 段落选择、润色、重写、扩写、缩写、语气调整和自定义修改。
- 章节导演独立页面及多轮对话。
- 根据确认后的章节主线生成正文草稿。
- 章节记忆压缩、确认、重新生成和向量化。
- RAG 检索历史剧情，并展示实际引入的剧情片段。
- 人物、组织和世界观故事资料库。
- 人物别名、所属组织、人物历史记录和身份揭秘后的角色合并设计。
- 系统预设、用户自定义、参考作品分析和根据当前小说分析生成的文风档案。
- Agent 运行记录、Token/耗时统计和评测中心。

## 3. 技术栈

### 前端

- Vue 3
- TypeScript
- Vite
- Element Plus
- 当前主要页面和大量交互集中在：
  `F:\proj-big\mojing-novel\frontend\src\App.vue`
- 默认开发端口由 `package.json` 配置为 `5174`，也可能被开发者临时改用其他端口。

### 后端

- Java 21
- Spring Boot 3.4.4
- Spring MVC
- Spring Data JPA
- MySQL；测试使用 H2
- Java `HttpClient` 调用 OpenAI 兼容的大模型接口
- SSE 流式响应
- 默认端口 `8081`

### AI 与检索

- DeepSeek/OpenAI 兼容聊天接口，具体模型与地址由本地配置决定。
- Embedding 服务使用 OpenAI 兼容接口。
- Qdrant 部署在虚拟机，通过虚拟机内网 IP 访问。
- Qdrant 使用 API Key；不要把真实 Key 写入本文档或提交到 Git。

## 4. 本地配置

后端启用了 `local` profile。本地敏感配置应放在：

```text
F:\proj-big\mojing-novel\backend\src\main\resources\application-local.yml
```

该文件应保持在 `.gitignore` 中。主要配置包括：

- 数据库连接。
- 聊天模型 URL、模型名和 API Key。
- Embedding URL、模型名、维度和 API Key。
- Qdrant 虚拟机地址、集合名和 API Key。

不要在交接文档、日志或前端代码里写入真实密钥。

## 5. 核心领域设计

### 5.1 小说与章节

小说包含标题、简介、大纲和章节。章节正文保存到数据库，并通过版本字段处理并发更新。前端编辑正文时采用延迟自动保存。

### 5.2 章节记忆

章节压缩不是保存完整正文，而是生成结构化记忆，主要包括：

- 本章故事线摘要。
- 按场景整理的事件。
- 关键事件和结果。
- 伏笔、未解线索、重要物品、世界规则、地点状态和关系变化。
- 人物在本章的动作、状态变化、新获知信息及人物档案变更建议。

场景边界区分硬切换与软切换，避免仅因为“上午/下午”变化就把连续事件机械拆成多个场景。

章节记忆确认后会拆分并写入 Qdrant。重新生成同一章节记忆时，应以章节和版本为依据更新或替换旧向量，避免多个版本同时参与检索。

### 5.3 人物档案

人物档案保存最新状态，例如：

- 姓名和别名。
- 角色定位。
- 所属组织。
- 身份经历。
- 性格。
- 目标。
- 当前状态。
- 当前关系。

人物历史按章节追加，例如“第 1 章初次登场”“第 10 章加入某组织”。人物最新状态可以更新，但历史记录不能覆盖丢失。

人物名称解析支持别名，避免把“卡尔·伯格”和“卡尔”创建成两个人。身份揭秘时，例如“老爷爷其实是已有角色假扮”，应合并身份而不是简单删除历史记录。

### 5.4 按需上下文

人物和组织不会每次把完整档案全部塞进提示词。流程是：

1. 先读取名称、别名、所属组织等轻量元数据。
2. 根据当前正文、作者要求和 RAG 结果筛选相关对象。
3. 只装载相关人物和组织的完整档案。
4. 人物档案中出现其他人物或所属组织时，可继续按需补充相关资料。

章节导演目前还会组合小说简介、大纲、世界观、最近章节和 RAG 历史剧情。相关上下文有明确字符预算，修改时要注意不要因为简单拼接导致后部重要设定被截断。

### 5.5 文风档案

支持：

- 系统预设文风。
- 用户自定义文风。
- 根据参考作品分析文风。
- 根据当前小说正文生成文风档案。

未来增加用户系统后，除系统预设外，用户创建或调教的文风都应按用户隔离，不能互相可见。

## 6. 主要 AI 功能

### 6.1 快速续写

现有非流式接口：

```text
POST /api/ai/completion
```

主要代码：

```text
backend/src/main/java/com/mojing/novel/completion/
```

续写会组合当前正文尾部、RAG、人物、组织和文风。

### 6.2 章节导演

章节导演多轮对话已经改为 SSE 流式返回。普通讨论只回复作者，不会每轮自动生成结构化主线。只有用户点击“生成本章主线”或者使用对应快捷键时，才生成主线计划。

交互约定：

- Enter：发送讨论消息。
- Ctrl+Enter：生成本章主线。
- 对话消息可以删除。
- 用户消息应先立即显示在对话区，再等待模型返回。

### 6.3 段落打磨

只传：

- 选中的原文。
- 紧邻前后文。
- 必要的小说约束。
- 按需人物、组织和文风。

不要把整章和全部资料无条件传入。AI 返回修改版后，用户接受之前不能覆盖正文。调整侧栏宽度也不能清空尚未接受的结果。

### 6.4 章节记忆压缩

压缩规则以稳定的系统规则/Skill 形式维护。压缩指令本身不属于小说上下文。压缩允许选择是否开启深度思考；快速模式更适合普通章节，深度思考用于结构复杂章节。

## 7. 自动补全功能现状

### 已同步到正式项目的版本

正式项目中已经存在自动补全基础版本：

- 光标停止输入 1 秒后触发。
- `Alt+/` 或 `Ctrl+Space` 手动触发。
- `Tab` 接受。
- `Esc` 取消。
- 顶部按钮可以开启/关闭，偏好保存在浏览器 `localStorage`。
- 使用快速模式，不启用深度思考。
- SSE 流式输出。
- 前后端都限制最多 100 字。
- 以淡紫色“幽灵文本”显示。

流式接口：

```text
POST /api/ai/completion/stream
Content-Type: text/event-stream
```

相关文件：

```text
backend/src/main/java/com/mojing/novel/completion/CompletionRequest.java
backend/src/main/java/com/mojing/novel/completion/NovelCompletionController.java
backend/src/main/java/com/mojing/novel/completion/NovelCompletionService.java
backend/src/main/java/com/mojing/novel/completion/CompletionStreamingService.java
frontend/src/App.vue
```

### 当前发现的问题

当光标位于正文中间时，结构如下：

```text
上方正文
<光标，需要补全的位置>
下方正文
```

旧提示词把下方正文放在提示词最后，模型容易误以为应该在“下方正文之后”继续写，而不是填充光标位置。

旧前端幽灵文本也只是把候选覆盖到原正文上，没有把光标后的正文按插入结果向后推，因此中间补全预览不准确。

## 8. 尚未同步的修复（下一任务优先处理）

因为旧任务没有 `F:\proj-big\mojing-novel` 的稳定写权限，以下修复已经写在暂存目录，但还没有同步到正式项目：

```text
F:\proj-big\yun-picture\tmp\inline-completion\backend\src\main\java\com\mojing\novel\completion\NovelCompletionService.java
F:\proj-big\yun-picture\tmp\inline-completion\frontend\src\App.vue
```

这两个暂存文件基于正式项目当时的最新版本修改，包含：

1. 真正的 FIM（Fill-in-the-Middle）提示结构。
2. 使用 `<AI_INSERT_HERE>` 明确标记唯一插入位置。
3. 将光标后文声明为不可重复、不可越过的固定边界。
4. 在提示词最后再次要求只输出占位符位置的正文。
5. RAG、人物和组织筛选同时参考光标前文与后文。
6. 前端预览重新渲染“前文 + 紫色候选 + 后文”，使后文按真实插入结果向后移动。

下一任务应先比较暂存文件与正式项目文件，确认期间没有其他新改动，然后只合并相关差异，不要盲目覆盖用户后续修改。

## 9. 推荐的下一步操作

在绑定 `F:\proj-big\mojing-novel` 的新 Codex 任务中：

1. 完整阅读本文档。
2. 检查当前工作目录和 Git/文件状态。
3. 比较以下文件：

   ```text
   F:\proj-big\yun-picture\tmp\inline-completion\backend\src\main\java\com\mojing\novel\completion\NovelCompletionService.java
   F:\proj-big\mojing-novel\backend\src\main\java\com\mojing\novel\completion\NovelCompletionService.java

   F:\proj-big\yun-picture\tmp\inline-completion\frontend\src\App.vue
   F:\proj-big\mojing-novel\frontend\src\App.vue
   ```

4. 合并 FIM 和中间预览修复。
5. 运行构建：

   ```powershell
   cd F:\proj-big\mojing-novel\backend
   mvn -DskipTests package

   cd F:\proj-big\mojing-novel\frontend
   npm run build
   ```

6. 启动前后端，至少人工验证以下场景：

   - 光标在正文末尾，停顿 1 秒后流式补全。
   - 光标在正文中间，生成内容位于上下文之间。
   - 中间补全必须衔接下方正文，不能续写到下方正文之后。
   - 输出不超过 100 字。
   - 流式生成过程中按 Esc 能停止显示。
   - 流式生成过程中继续输入，旧请求不会污染新正文。
   - Tab 接受后只产生一次可撤销编辑。
   - 调整左右侧栏不会丢失候选。

## 10. 构建与测试说明

此前已经验证：

- Java 21 后端 `mvn -DskipTests package` 构建成功。
- 前端 `npm run build` 构建成功。
- Vite/Rolldown 会显示来自第三方依赖的 `PURE` 注释和大 chunk 警告，不影响构建成功。

完整 `mvn test` 在旧任务环境中可能因为沙箱禁止建立本机回环连接而失败，常见错误是：

```text
Unable to establish loopback connection
```

这会影响使用本地 HTTP Server 的 Embedding、Qdrant 和 Spring 上下文测试，不代表业务代码编译失败。在正常本机终端或允许 loopback 的环境中应重新运行完整测试。

## 11. 开发注意事项

- 项目中存在用户已经完成的大量功能，修改 `App.vue` 时不要覆盖无关区域。
- 写文件前先检查当前文件与暂存文件的差异。
- 不要提交 `application-local.yml`、API Key 或其他密钥。
- 自动补全属于高频调用，默认必须使用快速模式和短输出，避免深度思考造成高延迟和高费用。
- 所有异步 AI 结果都要校验请求 ID、章节 ID、正文快照和光标位置，避免过期响应写入新章节。
- SSE 要处理浏览器中断、心跳、超时和服务端异常。
- 人物、组织和历史剧情应按需加载，不要为了“更完整”而无条件把全部资料放入每次提示词。
