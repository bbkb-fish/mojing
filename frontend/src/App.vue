<template>
  <section v-if="!authReady" class="auth-loading"><span class="brand-mark"><span>墨</span></span><p>正在确认登录状态…</p></section>
  <section v-else-if="!authUser" class="login-shell">
    <main class="login-card">
      <div class="login-brand"><span class="brand-mark"><span>墨</span></span><div><strong>墨境</strong><small>AI 小说创作空间</small></div></div>
      <div class="login-heading"><span>{{ authMode === 'login' ? 'WELCOME BACK' : 'START YOUR STORY' }}</span><h1>{{ authMode === 'login' ? '继续你的故事' : '创建创作账号' }}</h1><p>{{ authMode === 'login' ? '登录后进入个人创作空间。' : '注册后就能建立自己的小说书架。' }}</p></div>
      <form v-if="authMode === 'login'" @submit.prevent="login">
        <label><span>用户名</span><input v-model="loginForm.username" autocomplete="username" maxlength="50" autofocus /></label>
        <label><span>密码</span><div class="password-input"><input v-model="loginForm.password" :type="showLoginPassword ? 'text' : 'password'" autocomplete="current-password" maxlength="100" @input="loginError = ''" /><button type="button" @click="showLoginPassword = !showLoginPassword">{{ showLoginPassword ? '隐藏' : '显示' }}</button></div></label>
        <div class="login-options"><label><input v-model="rememberLogin" type="checkbox" /><span>在这台设备上保持登录</span></label></div>
        <p v-if="loginError" class="login-error">{{ loginError }}</p>
        <button type="submit" :disabled="loggingIn">{{ loggingIn ? '正在登录…' : '登录' }}</button>
      </form>
      <form v-else @submit.prevent="register">
        <label><span>用户名</span><input v-model="registerForm.username" autocomplete="username" maxlength="50" placeholder="字母开头，至少 3 位" required /></label>
        <label><span>昵称</span><input v-model="registerForm.nickname" autocomplete="nickname" maxlength="100" placeholder="你希望如何被称呼" required /></label>
        <label><span>密码</span><input v-model="registerForm.password" type="password" autocomplete="new-password" minlength="8" maxlength="100" placeholder="至少 8 个字符" required /></label>
        <label><span>确认密码</span><input v-model="registerForm.confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="100" required /></label>
        <div class="login-options"><label><input v-model="rememberLogin" type="checkbox" /><span>在这台设备上保持登录</span></label></div>
        <p v-if="registerError" class="login-error">{{ registerError }}</p>
        <button type="submit" :disabled="registering">{{ registering ? '正在创建账号…' : '注册并进入' }}</button>
      </form>
      <div class="auth-switch">{{ authMode === 'login' ? '还没有账号？' : '已有账号？' }}<button type="button" @click="switchAuthMode">{{ authMode === 'login' ? '立即注册' : '返回登录' }}</button></div>
    </main>
  </section>
  <section v-else-if="currentView === 'home'" class="home-shell">
    <header class="home-header">
      <div class="brand home-brand">
        <span class="brand-mark"><span>墨</span></span>
        <div><div class="brand-name">墨境</div><div class="brand-tagline">AI 小说创作空间</div></div>
      </div>
      <el-dropdown trigger="click" placement="bottom-end" @command="handleAccountCommand">
        <button class="avatar-button" title="账号菜单">{{ (authUser.nickname || authUser.username).slice(0, 1) }}</button>
        <template #dropdown><el-dropdown-menu><el-dropdown-item disabled><span class="account-menu-user"><strong>{{ authUser.nickname }}</strong><small>@{{ authUser.username }}</small></span></el-dropdown-item><el-dropdown-item command="settings" divided>账号设置</el-dropdown-item><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template>
      </el-dropdown>
    </header>
    <main class="home-main">
      <div class="home-heading">
        <div><span class="home-eyebrow">WRITING DESK</span><h1>我的小说</h1><p>继续上一次创作，让故事从停下的地方重新开始。</p></div>
        <div class="home-heading-side">
          <div class="home-summary"><strong>{{ novels.length }}</strong><span>部作品</span><i></i><strong>{{ homeTotalWords.toLocaleString() }}</strong><span>总字数</span></div>
          <button class="create-novel-button" @click="openCreateNovel"><span>＋</span> 新建小说</button>
        </div>
      </div>
      <div v-if="loadingWorkspace" class="home-loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在整理书架…</span></div>
      <div v-else-if="!novels.length" class="home-empty"><span>✦</span><h2>你的故事，从这里开始</h2><p>书架还是空的。创建第一部小说，把灵感写下来吧。</p><button @click="openCreateNovel">＋ 新建第一部小说</button></div>
      <div v-else class="novel-grid">
        <button v-for="novel in novels" :key="novel.id" class="novel-home-card" @click="openNovel(novel)">
          <span class="home-cover"><b>{{ novel.title.slice(0, 1) }}</b><i>MOJING</i></span>
          <span class="home-card-copy">
            <small>{{ novel.status === 'DRAFT' ? '创作中' : novel.status }}</small>
            <strong>{{ novel.title }}</strong>
            <span>{{ novel.description || '这个故事正在等待继续书写。' }}</span>
            <em>{{ novel.totalWords.toLocaleString() }} 字 · 更新于 {{ formatUpdatedAt(novel.updatedAt) }}</em>
          </span>
          <span class="continue-writing">继续写作 <el-icon><ArrowRight /></el-icon></span>
        </button>
      </div>
    </main>
  </section>

  <section v-else-if="currentView === 'director'" class="director-page-shell">
    <header class="director-page-header">
      <div class="director-page-brand">
        <button class="back-home-button" title="返回正文" @click="closeDirectorPage"><el-icon><ArrowLeft /></el-icon></button>
        <span class="brand-mark"><span>墨</span></span>
        <div><div class="brand-name">章节导演</div><div class="brand-tagline">先讨论清楚，再开始落笔</div></div>
      </div>
      <div class="director-page-context">
        <span>{{ novelTitle }}</span>
        <select :value="activeChapter" aria-label="切换章节" @change="selectDirectorChapter">
          <option v-for="(chapter, index) in chapters" :key="chapter.id" :value="index">{{ chapter.title }}</option>
        </select>
      </div>
      <div class="director-page-actions">
        <button class="ghost-action" :disabled="loadingDirectorRag" @click="openDirectorRag"><el-icon v-if="loadingDirectorRag" class="is-loading"><Loading /></el-icon> ◈ 查看引入剧情</button>
        <button class="ghost-action" @click="openStoryBible">◎ 大纲</button>
        <button class="director-return-button" @click="closeDirectorPage">返回正文</button>
      </div>
    </header>

    <main class="director-page-main">
      <section class="director-conversation-panel">
        <div class="director-panel-heading">
          <div><small>DIRECTOR ROOM</small><h1>讨论本章主线</h1><p>告诉导演这一章要发生什么，也可以针对上一版继续调整。</p></div>
          <span :class="{ ready: agentPlan }">{{ agentPlan ? '已有主线' : '等待规划' }}</span>
        </div>

        <div ref="directorMessagesContainer" class="director-page-messages">
          <div v-if="loadingDirector" class="director-page-empty"><el-icon class="is-loading"><Loading /></el-icon><p>正在读取本章导演对话…</p></div>
          <div v-else-if="!directorMessages.length" class="director-page-empty"><span>◇</span><h2>这一章准备怎么写？</h2><p>可以只给一句想法，例如“主角第一次进入地下城，但不要立刻揭晓黑衣人的身份”。</p></div>
          <article v-for="message in directorMessages" v-else :key="message.id" :class="['director-message', message.role.toLowerCase(), { pending: message.pending, failed: message.failed }]">
            <div class="director-message-meta"><strong>{{ message.role === 'USER' ? '你' : '章节导演' }}</strong><div><span v-if="message.pending">正在发送</span><span v-else-if="message.failed">发送失败</span><button v-if="message.plan" @click="restoreDirectorPlan(message)">查看此版主线</button><button v-if="message.role === 'USER' && !message.pending && !message.failed" class="delete-turn-button" @click="deleteDirectorTurn(message)">删除本轮</button></div></div>
            <p>{{ message.content }}</p>
            <ul v-if="message.changeSummary.length"><li v-for="change in message.changeSummary" :key="change">{{ change }}</li></ul>
          </article>
          <div v-if="planning" class="director-thinking"><el-icon class="is-loading"><Loading /></el-icon><span>{{ directorGeneratingPlan ? '导演正在整理对话并生成本章主线，通常需要 20～60 秒…' : '导演正在思考并回复…' }}</span></div>
        </div>

        <div class="director-composer">
          <div v-if="directorError" class="director-page-error"><strong>本轮对话失败</strong><span>{{ directorError }}</span><button @click="directorError = ''">关闭</button></div>
          <div class="director-thinking-choice"><span>思考模式</span><button :class="{ active: !directorThinkingEnabled }" @click="directorThinkingEnabled = false">快速</button><button :class="{ active: directorThinkingEnabled }" @click="directorThinkingEnabled = true">深度思考</button><small>{{ directorThinkingEnabled ? '更充分地推演情节与设定，响应会更慢' : '适合普通调整，响应更快' }}</small></div>
          <textarea v-model="agentGuidance" maxlength="2000" placeholder="和导演讨论人物动机、冲突、节奏或结尾方向……" @keydown="handleDirectorComposerKeydown"></textarea>
          <div><span>Enter 发送 · Ctrl + Enter 生成主线 · Shift + Enter 换行 · {{ agentGuidance.length }}/2000</span><div class="director-composer-actions"><button class="director-chat-button" :disabled="planning || drafting || !agentGuidance.trim()" @click="sendDirectorMessage(false)">发送消息</button><button class="director-plan-button" :disabled="planning || drafting" @click="generatePlan"><el-icon v-if="planning && directorGeneratingPlan" class="is-loading"><Loading /></el-icon>{{ planning && directorGeneratingPlan ? '正在生成…' : '生成本章主线' }}</button></div></div>
        </div>
      </section>

      <section class="director-plan-panel">
        <div class="director-panel-heading plan-heading">
          <div><small>CHAPTER BLUEPRINT</small><h1>本章主线</h1><p>导演给出的内容都可以直接手工修改，确认后再生成正文草稿。</p></div>
          <span v-if="agentPlan" :class="{ ready: directorPlanSaveState === 'saved', failed: directorPlanSaveState === 'failed' }">{{ directorPlanSaveLabel }}</span>
        </div>
        <div v-if="!agentPlan" class="director-plan-empty"><span>✦</span><h2>主线方案会显示在这里</h2><p>先在左侧说出你的想法。你不需要写完整大纲，一句话也可以开始。</p></div>
        <div v-else class="director-plan-board">
          <label><span>标题建议</span><input v-model="agentPlan.title" @input="scheduleDirectorPlanSave" /></label>
          <label><span>本章目标</span><textarea v-model="agentPlan.objective" @input="scheduleDirectorPlanSave"></textarea></label>
          <label><span>开场</span><textarea v-model="agentPlan.opening" @input="scheduleDirectorPlanSave"></textarea></label>
          <label><span>情节推进（每行一项）</span><textarea v-model="agentPlan.developmentsText" rows="5" @input="scheduleDirectorPlanSave"></textarea></label>
          <label><span>结尾钩子</span><textarea v-model="agentPlan.endingHook" @input="scheduleDirectorPlanSave"></textarea></label>
          <div class="director-plan-grid">
            <label><span>出场人物（每行一位）</span><textarea v-model="agentPlan.charactersText" @input="scheduleDirectorPlanSave"></textarea></label>
            <label><span>连续性约束（每行一项）</span><textarea v-model="agentPlan.continuityNotesText" @input="scheduleDirectorPlanSave"></textarea></label>
          </div>
          <div class="director-style-row"><label><span>正文文风</span><select v-model="selectedWritingStyleId" @change="rememberWritingStyle"><option v-for="style in writingStyles" :key="style.id" :value="style.id">{{ style.system ? '系统 · ' : '我的 · ' }}{{ style.name }}</option></select></label><button @click="openStyleManager">管理文风</button></div>
          <div class="director-draft-settings"><strong>草稿长度</strong><div class="segmented"><button v-for="length in draftLengths" :key="length.value" :class="{ active: draftLength === length.value }" @click="draftLength = length.value">{{ length.label }}</button><button :class="{ active: draftLength === 'custom' }" @click="draftLength = 'custom'">自定义</button></div><label v-if="draftLength === 'custom'" class="custom-draft-length"><input v-model.number="customDraftLength" type="number" min="300" max="5000" step="100" @keydown.enter.prevent="generateDraft" /><span>字</span></label><button class="director-draft-button" :disabled="drafting" @click="generateDraft"><el-icon v-if="drafting" class="is-loading"><Loading /></el-icon>{{ drafting ? `正在生成约 ${draftTargetLength} 字…` : '确认主线并生成正文草稿' }}</button></div>
          <div v-if="agentDraft" ref="directorDraftRef" class="director-draft-result"><div><strong>正文草稿（尚未插入正文）</strong><button @click="agentDraft = ''"><el-icon><Close /></el-icon></button></div><p>{{ agentDraft }}</p><footer><button @click="generateDraft"><el-icon><Refresh /></el-icon> 重写</button><button class="insert-button" @click="insertAgentDraftAndReturn"><el-icon><Bottom /></el-icon> 插入正文并返回</button></footer></div>
        </div>
      </section>
    </main>
  </section>

  <div v-else-if="currentView === 'editor'" class="studio-shell" :class="{ 'focus-mode': focusMode }">
    <header class="topbar">
      <div class="brand">
        <button class="back-home-button" title="返回我的小说" @click="goHome"><el-icon><ArrowLeft /></el-icon></button>
        <button class="icon-button mobile-only" aria-label="打开章节目录" @click="showMobileNav = true">
          <el-icon><Menu /></el-icon>
        </button>
        <span class="brand-mark"><span>墨</span></span>
        <div>
          <div class="brand-name">墨境</div>
          <div class="brand-tagline">AI 小说创作空间</div>
        </div>
      </div>

      <div class="book-title">
        <span class="status-dot"></span>
        <span>{{ novelTitle }}</span>
        <span class="saved">{{ saveState }}</span>
      </div>

      <div class="top-actions">
        <button class="ghost-action" @click="focusMode = !focusMode">
          <el-icon><FullScreen /></el-icon><span>{{ focusMode ? '退出专注' : '专注模式' }}</span>
        </button>
        <button class="ghost-action" @click="showExport = true">
          <el-icon><Download /></el-icon><span>导出</span>
        </button>
        <button class="avatar-button">林</button>
      </div>
    </header>

    <main class="workspace" :style="{ gridTemplateColumns: workspaceColumns }">
      <aside v-if="isMobile || !leftCollapsed" class="navigator" :class="{ 'mobile-open': showMobileNav }">
        <div class="mobile-nav-head mobile-only">
          <strong>章节目录</strong>
          <button class="icon-button" @click="showMobileNav = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="novel-card">
          <div class="cover-mini">{{ novelTitle.slice(0, 1) }}</div>
          <div class="novel-meta">
            <strong>{{ novelTitle }}</strong>
            <span>{{ chapters.length }} 章 · {{ totalWords.toLocaleString() }} 字</span>
          </div>
          <button class="more-button" aria-label="小说设置" title="编辑简介与大纲" @click="openNovelSettings">•••</button>
        </div>
        <button class="story-bible-button" @click="openStoryBible"><span>◎</span><strong>大纲</strong><small>立项 · 总纲 · {{ storyVolumes.length }} 分卷 · {{ characters.length }} 人物 · {{ worldSettings.length }} 设定</small></button>
        <button class="story-bible-button chapter-memory-button" @click="openChapterMemory"><span>◈</span><strong>当前章节记忆</strong><small>{{ activeMemoryLabel }}</small></button>
        <details class="writing-more-tools">
          <summary>更多工具 <span>⌄</span></summary>
          <button @click="openRunCenter">运行记录 <small>调用与耗时</small></button>
          <button @click="openEvalCenter">评测中心 <small>质量与回归</small></button>
        </details>
        <div class="chapter-panel">
          <div class="section-label"><span>章节目录</span><button class="add-chapter-button" title="添加章节" :disabled="loadingWorkspace || creatingChapter" @click="addChapter"><span>＋</span> {{ creatingChapter ? '创建中' : '新建章节' }}</button></div>
          <div v-for="(chapter, index) in chapters" :key="chapter.id" class="chapter-item" :class="{ active: activeChapter === index }" role="button" tabindex="0" @click="selectChapter(index)" @keydown.enter="selectChapter(index)">
            <span class="chapter-index">{{ String(index + 1).padStart(2, '0') }}</span>
            <span class="chapter-copy"><strong>{{ chapter.title }}</strong><small>{{ chapter.words.toLocaleString() }} 字</small></span>
            <span v-if="activeChapter === index" class="chapter-active-dot"></span>
            <button class="delete-chapter-button" :disabled="deletingChapterId === chapter.id" :aria-label="`删除${chapter.title}`" title="删除章节" @click.stop="deleteChapter(chapter, index)"><el-icon><Delete /></el-icon></button>
          </div>
        </div>
        <div class="nav-footer">
          <div class="word-goal"><span>全书字数</span><strong>{{ totalWords.toLocaleString() }} <small>字</small></strong></div>
          <div class="writing-footer-note">一章一章，让故事生长。</div>
        </div>
      </aside>

      <div v-if="!isMobile && !focusMode && !leftCollapsed" class="pane-resizer pane-resizer-left" title="拖动调整目录宽度" @mousedown="startResize('left', $event)"></div>

      <section class="editor-section">
        <div class="editor-toolbar">
          <button class="panel-toggle" :class="{ active: !leftCollapsed }" title="显示或隐藏章节目录" @click="toggleLeftPanel">
            <el-icon><Menu /></el-icon><span>目录</span>
          </button>
          <div class="format-actions">
            <button title="撤销（Ctrl+Z）" aria-label="撤销" :disabled="!canUndo" @click="undoEditor"><el-icon><RefreshLeft /></el-icon></button><button title="重做（Ctrl+Y / Ctrl+Shift+Z）" aria-label="重做" :disabled="!canRedo" @click="redoEditor"><el-icon><RefreshRight /></el-icon></button>
          </div>
          <label class="font-size-control">
            <span>字号</span>
            <select v-model.number="fontSize" aria-label="选择正文字号">
              <option v-for="size in fontSizes" :key="size" :value="size">{{ size }} px</option>
            </select>
          </label>
          <button class="panel-toggle inline-completion-toggle" :class="{ active: inlineCompletionEnabled }" :title="inlineCompletionEnabled ? '自动补全已开启：停顿 1 秒后生成' : '自动补全已关闭，仍可按 Alt+/'" @click="toggleInlineCompletion">
            <span>✦</span> 补全 {{ inlineCompletionEnabled ? '开' : '关' }}
          </button>
          <button class="panel-toggle director-toolbar-button" title="在独立页面讨论本章主线" @click="openDirectorPage"><span>◇</span> 导演</button>
          <button class="panel-toggle panel-toggle-ai" :class="{ active: !rightCollapsed }" title="显示或隐藏 AI 续写" @click="toggleRightPanel">
            <span>AI</span><el-icon><MagicStick /></el-icon>
          </button>
          <button class="mobile-ai mobile-only" @click="showMobileAi = true"><span>✦</span> AI 续写</button>
        </div>
        <article ref="paperRef" class="paper">
          <div class="chapter-kicker">CHAPTER {{ String(activeChapter + 1).padStart(2, '0') }}</div>
          <input v-model="chapterTitle" class="chapter-title-input" aria-label="章节标题" @input="handleTitleInput" />
          <div class="chapter-divider"><span></span><i>◆</i><span></span></div>
          <div class="manuscript-shell" :class="{ 'has-inline-completion': inlineCompletion }">
            <textarea ref="editorRef" v-model="editorContent" class="manuscript" aria-label="章节正文" placeholder="故事，从这里继续……" :style="{ fontSize: `${fontSize}px` }" spellcheck="false" @beforeinput="handleEditorBeforeInput" @input="handleEditorInput" @keydown="handleEditorKeydown" @select="captureSelection" @mouseup="captureSelection" @keyup="captureSelection" @compositionstart="handleEditorCompositionStart" @compositionend="handleEditorCompositionEnd" @wheel.prevent="handleEditorWheel"></textarea>
            <div v-if="inlineCompletionLoading || inlineCompletion" class="inline-completion-ghost" :style="{ fontSize: `${fontSize}px` }" aria-live="polite" aria-hidden="true">
              <span class="inline-completion-prefix">{{ editorContent.slice(0, inlineCompletionOffset) }}</span><span v-if="inlineCompletion" class="inline-completion-copy">{{ inlineCompletionInsertText }}</span><span v-if="inlineCompletionLoading" class="inline-completion-loading">{{ inlineCompletion ? ' ✦' : '✦ 正在补全…' }}</span><span class="inline-completion-suffix">{{ editorContent.slice(inlineCompletionOffset) }}</span>
            </div>
          </div>
        </article>
        <footer class="editor-statusbar">
          <span class="editor-save-status" :class="{ 'is-error': saveState === '保存失败', 'is-pending': saveState !== '已保存' && saveState !== '保存失败' }" role="status"><i></i>{{ saveState }}</span>
          <div><span>{{ currentWords.toLocaleString() }} 字</span><span>约 {{ readingMinutes }} 分钟阅读</span></div>
        </footer>
      </section>

      <div v-if="!isMobile && !focusMode && !rightCollapsed" class="pane-resizer pane-resizer-right" title="拖动调整 AI 面板宽度" @mousedown="startResize('right', $event)"></div>

      <aside v-if="isMobile || !rightCollapsed" class="ai-panel" :class="{ 'mobile-open': showMobileAi }">
        <div class="mobile-nav-head mobile-only">
          <strong>创作 Agent</strong>
          <button class="icon-button" @click="showMobileAi = false"><el-icon><Close /></el-icon></button>
        </div>
        <div class="ai-header"><div><span class="sparkle">✦</span><strong>创作助手</strong></div><span class="assistant-caption">与你一起构思</span></div>
        <div class="agent-tabs"><button :class="{ active: aiMode === 'agent' }" @click="aiMode = 'agent'">章节导演</button><button :class="{ active: aiMode === 'rewrite' }" @click="openRewriteMode">段落打磨</button><button :class="{ active: aiMode === 'consistency' }" @click="openConsistencyMode">质量检查</button><button :class="{ active: aiMode === 'organize' }" @click="openOrganizeMode">章节整理</button><button :class="{ active: aiMode === 'quick' }" @click="aiMode = 'quick'">快速续写</button></div>
        <div class="ai-scroll">
          <template v-if="aiMode === 'agent'">
            <div class="director-launcher">
              <span class="director-launcher-icon">◇</span>
              <small>独立策划工作台</small>
              <h2>章节导演</h2>
              <p>用更大的空间和 AI 多轮讨论本章主线，确认方案后再生成正文。</p>
              <div><span>{{ chapterTitle }}</span><strong>{{ agentPlan ? '已有主线方案' : '尚未规划' }}</strong></div>
              <button @click="openDirectorPage">打开章节导演 <el-icon><ArrowRight /></el-icon></button>
            </div>
          </template>
          <template v-else-if="aiMode === 'rewrite'">
            <div class="mode-intro rewrite-intro"><div class="mode-icon">✎</div><div><strong>只修改你选中的正文</strong><p>AI 会参考前后文和故事资料，确认之前不会覆盖原文</p></div></div>
            <div v-if="selectedText" class="selection-card"><div><strong>已选择 {{ selectedText.length }} 字</strong><button @click="clearSelection">清除</button></div><p>{{ selectedText }}</p></div>
            <div v-else class="selection-empty"><span>①</span><p><strong>先在正文中拖动选择一段文字</strong><small>建议每次选择 50～1500 字，选择后再回到这里</small></p><button @click="focusEditorForSelection">去选择</button></div>
            <label class="field-label rewrite-label">修改方式</label>
            <div class="rewrite-modes"><button v-for="mode in rewriteModes" :key="mode.value" :class="{ active: rewriteMode === mode.value }" @click="rewriteMode = mode.value"><strong>{{ mode.label }}</strong><small>{{ mode.help }}</small></button></div>
            <div class="writing-style-picker"><label><span>正文文风</span><select v-model="selectedWritingStyleId" @change="rememberWritingStyle"><option v-for="style in writingStyles" :key="style.id" :value="style.id">{{ style.system ? '系统 · ' : '我的 · ' }}{{ style.name }}</option></select></label><button @click="openStyleManager">管理</button></div>
            <label class="field-label">推理模式</label>
            <div class="rewrite-thinking-options">
              <button :class="{ active: !rewriteThinkingEnabled }" @click="rewriteThinkingEnabled = false"><strong>快速模式</strong><small>关闭推理，更快且成本更低</small></button>
              <button :class="{ active: rewriteThinkingEnabled }" @click="rewriteThinkingEnabled = true"><strong>深度推理</strong><small>适合复杂改写，最长可能等待更久</small></button>
            </div>
            <label class="field-label">额外修改要求</label>
            <div class="prompt-box compact-prompt"><textarea v-model="rewriteInstruction" maxlength="2000" placeholder="例如：增强紧张感，保留所有剧情事实；或者改成更加冷峻的语气…"></textarea><div class="prompt-tools"><button @click="openStoryBible">◎ 查看故事资料</button><span>{{ rewriteInstruction.length }}/2000</span></div></div>
            <button class="generate-button" :disabled="rewriting || !selectedText" @click="generateRewrite"><span v-if="!rewriting">✎</span><el-icon v-else class="is-loading"><Loading /></el-icon>{{ rewriting ? '正在结合上下文打磨…' : rewriteResult ? '重新生成修改版' : '生成修改版' }}</button>
            <div v-if="rewriteResult" class="rewrite-result">
              <div class="result-head"><span><i></i> 修改对照</span><small>{{ rewriteStatusLabel(rewriteResult.status) }}</small></div>
              <div class="comparison-block original"><strong>原文</strong><p>{{ rewriteResult.originalText }}</p></div>
              <div class="comparison-arrow">↓</div>
              <div class="comparison-block revised"><strong>修改版</strong><p>{{ rewriteResult.revisedText }}</p></div>
              <div v-if="rewriteResult.changeSummary.length" class="change-summary"><strong>本次调整</strong><span v-for="item in rewriteResult.changeSummary" :key="item">✓ {{ item }}</span></div>
              <div v-if="rewriteResult.warnings.length" class="rewrite-warnings"><strong>请注意</strong><span v-for="item in rewriteResult.warnings" :key="item">{{ item }}</span></div>
              <div v-if="rewriteResult.status === 'GENERATED'" class="rewrite-actions"><button :disabled="decidingRevision" @click="rejectRewrite">放弃修改</button><button class="insert-button" :disabled="decidingRevision" @click="acceptRewrite">接受并替换</button></div>
            </div>
            <div class="revision-history"><div class="history-title"><strong>修改历史</strong><button @click="loadRevisionHistory(true)"><el-icon><Refresh /></el-icon></button></div><div v-if="revisionHistory.length" class="history-list"><div v-for="revision in revisionHistory" :key="revision.id"><span class="history-mode">{{ rewriteModeLabel(revision.revisionType) }}</span><p><strong>{{ revision.originalText.slice(0, 36) }}{{ revision.originalText.length > 36 ? '…' : '' }}</strong><small>{{ rewriteStatusLabel(revision.status) }} · {{ formatRevisionTime(revision.createdAt) }}</small></p><button v-if="revision.status === 'ACCEPTED'" @click="rollbackRevision(revision)">回滚</button></div></div><div v-else class="empty-history">当前章节还没有 AI 修改记录</div></div>
          </template>
          <template v-else-if="aiMode === 'consistency'">
            <div class="mode-intro consistency-intro"><div class="mode-icon">✓</div><div><strong>检查当前章节的一致性</strong><p>核对人物、世界观、时间线、大纲、视角和情节逻辑</p></div></div>
            <label class="field-label">想重点检查什么？</label>
            <div class="prompt-box consistency-focus"><textarea v-model="consistencyFocus" maxlength="1000" placeholder="留空则全面检查；也可以指定：重点检查人物能力和时间线…"></textarea><div class="prompt-tools"><button @click="openStoryBible">◎ 查看故事资料</button><span>{{ consistencyFocus.length }}/1000</span></div></div>
            <button class="generate-button check-button" :disabled="checkingConsistency || !editorContent.trim()" @click="checkConsistency"><span v-if="!checkingConsistency">✓</span><el-icon v-else class="is-loading"><Loading /></el-icon>{{ checkingConsistency ? '正在审校当前章节…' : consistencyReport ? '重新检查当前章节' : '开始一致性检查' }}</button>
            <div v-if="consistencyReport" class="consistency-report">
              <div class="score-card"><div :class="scoreClass(consistencyReport.score)"><strong>{{ consistencyReport.score }}</strong><small>综合评分</small></div><p><strong>{{ consistencyReport.issues.length ? `发现 ${consistencyReport.issues.length} 个问题` : '没有发现明显问题' }}</strong><span>{{ consistencyReport.summary }}</span></p></div>
              <div v-if="consistencyReport.issues.length" class="issue-list"><article v-for="issue in consistencyReport.issues" :key="issue.id" :class="[`severity-${issue.severity.toLowerCase()}`, { resolved: issue.status === 'FIXED' || issue.status === 'IGNORED' }]">
                <div class="issue-head"><span>{{ severityLabel(issue.severity) }}</span><strong>{{ issueTypeLabel(issue.type) }}</strong><small>{{ issueStatusLabel(issue.status) }}</small></div>
                <blockquote>“{{ issue.quote }}”</blockquote><p>{{ issue.message }}</p><div class="issue-suggestion"><b>建议</b>{{ issue.suggestion }}</div>
                <div class="issue-actions"><button @click="locateIssue(issue)">定位原文</button><button v-if="issue.status === 'OPEN'" :disabled="decidingIssueId === issue.id" @click="ignoreIssue(issue)">忽略</button><button v-if="issue.status === 'IGNORED'" :disabled="decidingIssueId === issue.id" @click="reopenIssue(issue)">重新处理</button><button v-if="issue.status === 'OPEN'" class="fix-button" :disabled="decidingIssueId === issue.id" @click="fixConsistencyIssue(issue)">{{ decidingIssueId === issue.id ? '生成中…' : 'AI 修复' }}</button><button v-if="issue.status === 'WAITING_APPROVAL' && issue.revision" class="fix-button" @click="openIssueRevision(issue)">查看修改版</button></div>
              </article></div>
            </div>
            <div class="report-history"><div class="history-title"><strong>检查历史</strong><button @click="loadConsistencyReports(true)"><el-icon><Refresh /></el-icon></button></div><div v-if="consistencyReports.length" class="report-history-list"><button v-for="report in consistencyReports" :key="report.id" :class="{ active: consistencyReport?.id === report.id }" @click="consistencyReport = report"><strong>{{ report.score }} 分</strong><span>{{ report.issues.length }} 个问题</span><small>{{ formatRevisionTime(report.createdAt) }}</small></button></div><div v-else class="empty-history">当前章节还没有检查记录</div></div>
          </template>
          <template v-else-if="aiMode === 'organize'">
            <div class="mode-intro organize-intro"><div class="mode-icon">◈</div><div><strong>需要时再整理本章</strong><p>不会改变章节状态，也不会影响正常写作和续写</p></div></div>
            <div class="organize-status-list">
              <article><span>✓</span><div><strong>一致性检查</strong><p>{{ latestConsistencyStatus }}</p></div><button @click="runOrganizeCheck">单独检查</button></article>
              <article><span>◈</span><div><strong>章节结构化记忆</strong><p>{{ activeMemoryLabel }}</p></div><button @click="openChapterMemory">单独整理</button></article>
            </div>
            <button class="generate-button organize-button" :disabled="organizingChapter || !editorContent.trim()" @click="organizeChapter"><el-icon v-if="organizingChapter" class="is-loading"><Loading /></el-icon>{{ organizingChapter ? '正在检查并提取记忆…' : '检查并提取章节记忆' }}</button>
            <div class="organize-note"><strong>完全可选</strong><p>你可以只使用续写或段落打磨。整理结果不会强制处理；章节记忆也只有在你确认后才会作为高可信资料使用。</p></div>
          </template>
          <template v-else>
            <div class="mode-intro"><div class="mode-icon">✦</div><div><strong>根据正文快速续写</strong><p>只读取当前章节结尾，适合补一小段内容</p></div></div>
            <label class="field-label">接下来想写什么？</label>
            <div class="prompt-box"><textarea v-model="prompt" placeholder="描述接下来的情节；留空则自由续写…"></textarea><div class="prompt-tools"><button @click="useSelection"><el-icon><Crop /></el-icon> 引用正文结尾</button><span>{{ prompt.length }}/2000</span></div></div>
            <div class="writing-style-picker"><label><span>正文文风</span><select v-model="selectedWritingStyleId" @change="rememberWritingStyle"><option v-for="style in writingStyles" :key="style.id" :value="style.id">{{ style.system ? '系统 · ' : '我的 · ' }}{{ style.name }}</option></select></label><button @click="openStyleManager">管理</button></div>
            <div class="settings-row"><label>生成长度</label><div class="segmented"><button v-for="length in lengths" :key="length" :class="{ active: generateLength === length }" @click="generateLength = length">{{ length }}</button></div></div>
            <button class="generate-button" :disabled="generating" @click="generate"><span v-if="!generating">✦</span><el-icon v-else class="is-loading"><Loading /></el-icon>{{ generating ? '正在续写中…' : '生成续写' }}</button>
            <div v-if="result" class="result-card"><div class="result-head"><span><i></i> 续写结果</span><button @click="clearQuickResult"><el-icon><Close /></el-icon></button></div><p>{{ result }}</p>
              <details v-if="ragDebug?.enabled" class="rag-debug"><summary><span>◈ 创作记忆</span><small v-if="ragDebug.status === 'COMPLETED'">采用 {{ ragDebug.usedCount }}/{{ ragDebug.retrievedCount }} 条 · {{ ragDebug.durationMs }}ms</small><small v-else>检索暂不可用，本次已自动降级</small></summary>
                <div v-if="ragDebug.memories.length" class="rag-memory-list"><article v-for="memory in ragDebug.memories" :key="memory.pointId" :class="{ unused: !memory.used }"><div><strong>第{{ memory.chapterOrder ?? '?' }}章 · {{ memoryTypeLabel(memory.memoryType) }}</strong><span>{{ Math.round(memory.score * 100) }}%</span><i>{{ memory.used ? '已采用' : '未采用' }}</i></div><p>{{ memory.text }}</p></article></div>
                <div v-else class="rag-empty">{{ ragDebug.status === 'COMPLETED' ? '没有检索到足够相关的历史记忆。' : 'Qdrant 或 Embedding 暂时不可用，不影响本次续写。' }}</div>
              </details>
              <div class="result-actions"><button @click="generate"><el-icon><Refresh /></el-icon> 换一版</button><button class="insert-button" @click="insertResult"><el-icon><Bottom /></el-icon> 插入正文</button></div></div>
            <div v-else class="inspiration-card"><div class="inspiration-title"><span>✧</span> 使用提示</div><p>需要严格遵循人物和大纲时，建议使用“章节导演”。</p></div>
          </template>
        </div>
      </aside>
    </main>

    <div v-if="showMobileNav || showMobileAi" class="backdrop mobile-only" @click="showMobileNav = false; showMobileAi = false"></div>

    <el-dialog v-model="showExport" title="导出作品" width="420px">
      <div class="export-options">
        <button><strong>TXT</strong><span>纯文本，适合投稿</span></button>
        <button><strong>DOCX</strong><span>保留章节格式</span></button>
        <button><strong>PDF</strong><span>适合阅读与分享</span></button>
      </div>
      <template #footer><el-button @click="showExport = false">取消</el-button><el-button type="primary" @click="exportBook">导出全部章节</el-button></template>
    </el-dialog>
  </div>

  <el-dialog v-model="showStyleManager" title="我的文风" width="min(900px, calc(100vw - 28px))" class="style-manager-dialog" :close-on-click-modal="false">
    <div class="style-manager-layout">
      <aside class="style-profile-list">
        <div><strong>私人文风</strong><button @click="newWritingStyle">＋ 新建</button></div>
        <button v-for="style in privateWritingStyles" :key="style.id" :class="{ active: styleForm.databaseId === style.databaseId }" @click="editWritingStyle(style)"><strong>{{ style.name }}</strong><span>{{ style.sourceType === 'REFERENCE' ? '参考作品提取' : style.sourceType === 'NOVEL' ? '当前小说提取' : '自定义' }}</span></button>
        <p v-if="!privateWritingStyles.length">还没有私人文风，可以用一句描述或参考片段创建。</p>
      </aside>
      <section class="style-profile-editor">
        <div class="style-source-tabs"><button :class="{ active: styleAssistMode === 'ORGANIZE' }" @click="styleAssistMode = 'ORGANIZE'">自定义要求</button><button :class="{ active: styleAssistMode === 'NOVEL' }" @click="styleAssistMode = 'NOVEL'">从当前小说提取</button><button :class="{ active: styleAssistMode === 'REFERENCE' }" @click="styleAssistMode = 'REFERENCE'">参考作品提取</button></div>
        <label><span>文风名称 *</span><input v-model="styleForm.name" maxlength="100" placeholder="例如：阴郁工业英伦" /></label>
        <label><span>{{ styleAssistMode === 'ORGANIZE' ? '用自然语言描述文风' : '希望保留或调整的风格倾向' }}</span><textarea v-model="styleForm.description" maxlength="5000" rows="3" placeholder="例如：保留当前小说克制的对话，但减少文言表达和网文套话"></textarea></label>
        <label v-if="styleAssistMode === 'REFERENCE'"><span>参考作品片段</span><textarea v-model="styleReferenceInput" maxlength="30000" rows="7" placeholder="粘贴你有权使用的代表性片段。多段材料可用单独一行 --- 分隔；AI只提取高层文风特征，不带入原作剧情和人物。"></textarea></label>
        <button class="style-ai-button" :disabled="assistingStyle" @click="assistWritingStyle"><el-icon v-if="assistingStyle" class="is-loading"><Loading /></el-icon>{{ assistingStyle ? '正在分析文风…' : styleAssistMode === 'ORGANIZE' ? 'AI 整理规则' : styleAssistMode === 'NOVEL' ? '分析当前小说文风' : 'AI 提取文风' }}</button>
        <label><span>生成时使用的文风规则 *</span><textarea v-model="styleForm.rulesText" maxlength="10000" rows="8" placeholder="这里的规则会作为正文生成的强制系统提示词"></textarea></label>
        <label><span>明确避免的表达</span><textarea v-model="styleForm.forbiddenWords" maxlength="4000" rows="3" placeholder="文言文、古风称谓、冷哼一声、心中暗道……"></textarea></label>
        <div class="style-form-actions"><button v-if="styleForm.databaseId" class="danger" @click="deleteWritingStyle">删除</button><span></span><button @click="showStyleManager = false">取消</button><button class="primary" :disabled="savingStyle" @click="saveWritingStyle">{{ savingStyle ? '保存中…' : '保存并使用' }}</button></div>
      </section>
    </div>
  </el-dialog>

  <el-dialog v-model="showDirectorRag" title="章节导演 · 引入剧情" width="min(760px, calc(100vw - 28px))" class="director-rag-dialog">
    <div v-if="loadingDirectorRag" class="director-rag-loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在根据导演对话检索较早章节记忆…</span></div>
    <template v-else-if="directorRagDebug">
      <div class="director-rag-summary"><div><strong>远期剧情按需召回</strong><p>根据最近导演对话和当前输入，检索当前章前两章以前的向量记忆；“已引入”的内容会放进章节导演上下文。</p></div><span v-if="directorRagDebug.status === 'COMPLETED'">引入 {{ directorRagDebug.usedCount }} / 找到 {{ directorRagDebug.retrievedCount }} 条 · {{ directorRagDebug.durationMs }}ms</span><span v-else>{{ directorRagDebug.status === 'SKIPPED' ? '当前没有更早章节' : '检索暂不可用' }}</span></div>
      <div v-if="directorRagDebug.memories.length" class="rag-memory-list director-rag-list"><article v-for="memory in directorRagDebug.memories" :key="memory.pointId" :class="{ unused: !memory.used }"><div><strong>第{{ memory.chapterOrder ?? '?' }}章 · {{ memoryTypeLabel(memory.memoryType) }}</strong><span>{{ Math.round(memory.score * 100) }}%</span><i>{{ memory.used ? '已引入' : '候选未采用' }}</i></div><p>{{ memory.text }}</p></article></div>
      <div v-else class="director-rag-empty">{{ directorRagDebug.status === 'UNAVAILABLE' ? 'Qdrant 或 Embedding 暂时不可用，本次导演对话会自动降级。' : '没有检索到足够相关的远期剧情；当前章和前两章正文仍会正常传入。' }}</div>
    </template>
  </el-dialog>

  <el-dialog v-model="showCreateNovel" title="新建小说" width="min(560px, calc(100vw - 28px))" :close-on-click-modal="false">
    <div class="novel-form">
      <label><span>小说名称 <b>*</b></span><input v-model="newNovelForm.title" maxlength="200" placeholder="给你的故事起一个名字" /></label>
      <label><span>一句灵感</span><textarea v-model="newNovelForm.inspiration" maxlength="5000" placeholder="不必先写简介和大纲，例如：一个能看见别人死亡倒计时的外卖员"></textarea><small>{{ newNovelForm.inspiration.length }}/5000</small></label>
    </div>
    <template #footer><el-button @click="showCreateNovel = false">取消</el-button><el-button type="primary" :loading="creatingNovel" @click="createNovel">创建并开始立项</el-button></template>
  </el-dialog>

  <el-dialog v-model="showNovelSettings" title="作品设置" width="min(600px, calc(100vw - 28px))" :close-on-click-modal="false">
    <div class="novel-form">
      <label><span>小说名称 <b>*</b></span><input v-model="novelSettingsForm.title" maxlength="200" /></label>
      <label><span>小说简介</span><textarea v-model="novelSettingsForm.description" maxlength="1000" placeholder="介绍故事背景、主角和主要冲突"></textarea><small>{{ novelSettingsForm.description.length }}/1000</small></label>
      <label><span>故事大纲</span><textarea v-model="novelSettingsForm.outline" class="outline-input" placeholder="记录主线剧情、关键节点与结局"></textarea></label>
    </div>
    <template #footer><el-button @click="showNovelSettings = false">取消</el-button><el-button type="primary" :loading="savingNovelSettings" @click="saveNovelSettings">保存设置</el-button></template>
  </el-dialog>

  <section v-if="currentView === 'projectPlanning'" class="project-page-shell">
    <header class="story-library-header project-page-header">
      <div class="story-library-brand"><button class="back-home-button" title="返回正文" @click="closeProjectPlanning"><el-icon><ArrowLeft /></el-icon></button><span class="brand-mark"><span>墨</span></span><div><div class="brand-name">立项工作台</div><div class="brand-tagline">从一句灵感到可连载的故事方案</div></div></div>
      <div class="story-library-book"><small>当前作品</small><strong>{{ novelTitle }}</strong></div>
      <button class="story-library-return" @click="closeProjectPlanning">返回正文</button>
    </header>
    <main class="project-page-main">
      <section class="project-hero">
        <div><span>NOVEL FOUNDATION</span><h1>先定一个能写的方向</h1><p>一句灵感就能开始，细节边写边补。手工修改自动保存，AI 草案由你确认后应用。</p><small :class="['project-save-state', projectSaveState]">{{ projectSaveLabel }}</small></div>
        <div class="project-status"><strong>{{ projectWrittenThrough ? `已写至第 ${projectWrittenThrough} 章` : '尚无正文' }}</strong><span>{{ projectWrittenThrough ? '可在立项顾问中开启“补建立项”' : '适合从一句灵感开始' }}</span></div>
      </section>
      <div v-if="loadingProject" class="story-library-loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在读取立项资料…</span></div>
      <template v-else>
        <div class="project-workspace">
        <div class="project-editor-column">
        <nav class="project-setting-tabs"><button v-for="tab in projectSettingTabs" :key="tab.value" :class="{ active: projectSettingsTab === tab.value }" @click="projectSettingsTab = tab.value">{{ tab.label }}</button></nav>
        <div class="project-grid" @input="scheduleProjectSave">
          <section v-show="projectSettingsTab === 'POSITIONING'" class="project-card project-foundation-card">
            <header><span>01</span><div><h2>作品定位</h2><p>告诉 AI 这本书为谁而写、在哪里发布。</p></div></header>
            <label class="wide"><span>一句灵感</span><textarea v-model="projectForm.inspiration" rows="3" maxlength="5000" placeholder="例如：一个能看见别人死亡倒计时的外卖员"></textarea></label>
            <div class="project-field-grid"><label><span>题材</span><input v-model="projectForm.genre" maxlength="100" placeholder="都市异能、玄幻、悬疑…" /></label><label><span>频道</span><input v-model="projectForm.channel" maxlength="100" placeholder="男频、女频、出版向…" /></label><label><span>目标读者</span><input v-model="projectForm.targetAudience" maxlength="500" /></label><label><span>发布平台</span><input v-model="projectForm.platform" maxlength="100" placeholder="番茄、起点、晋江…" /></label></div>
          </section>
          <section v-show="projectSettingsTab === 'ENGINE'" class="project-card">
            <header><span>02</span><div><h2>故事引擎</h2><p>确保核心机制能够持续制造冲突与回报。</p></div></header>
            <label><span>核心卖点</span><textarea v-model="projectForm.coreSellingPoint" rows="4" maxlength="5000" placeholder="这本书最独特、最让读者想继续看的内容"></textarea></label>
            <label><span>主角金手指 / 核心能力</span><textarea v-model="projectForm.protagonistHook" rows="4" maxlength="5000" placeholder="能力、限制、代价和成长空间"></textarea></label>
            <label><span>升级路线</span><textarea v-model="projectForm.growthRoute" rows="5" maxlength="10000" placeholder="每个阶段的目标、阻力和读者回报"></textarea></label>
            <label><span>主要爽点与读者期待</span><textarea v-model="projectForm.readerExpectations" rows="4" maxlength="5000"></textarea></label>
          </section>
          <section v-show="projectSettingsTab === 'OPENING'" class="project-card">
            <header><span>03</span><div><h2>篇幅与开篇</h2><p>把长期规划落实到卷数、章节和前三章。</p></div></header>
            <div class="project-number-grid"><label><span>预计总字数</span><input v-model.number="projectForm.expectedWords" type="number" min="10000" max="10000000" step="10000" /></label><label><span>预计卷数</span><input v-model.number="projectForm.expectedVolumes" type="number" min="1" max="100" /></label><label><span>单章目标字数</span><input v-model.number="projectForm.chapterWordTarget" type="number" min="300" max="10000" step="100" /></label></div>
            <label><span>开篇三章设计</span><textarea v-model="projectForm.openingThreeChapters" rows="10" maxlength="10000" placeholder="分别记录每章任务、冲突或爽点、信息释放和章末钩子"></textarea></label>
          </section>
          <section v-show="projectSettingsTab === 'MATERIALS'" class="project-card project-output-card">
            <header><span>04</span><div><h2>正式作品资料</h2><p>这里的草案保存后会直接进入“大纲”，并用于后续 AI 创作。</p></div></header>
            <label><span>小说简介 <em>{{ projectForm.description.length }}/1000</em></span><textarea v-model="projectForm.description" rows="8" maxlength="1000" placeholder="可手写，也可以点击上方“生成简介”"></textarea></label>
            <label><span>故事大纲</span><textarea v-model="projectForm.outline" rows="16" maxlength="100000" placeholder="可手写，也可以点击上方“生成故事大纲”"></textarea></label>
          </section>
          <section v-show="projectSettingsTab === 'STYLE'" class="project-card project-style-card">
            <header><span>05</span><div><h2>作品风格</h2><p>选择后会用于章节导演、草稿生成和段落打磨。</p></div></header>
            <label><span>当前正文文风</span><select v-model="selectedWritingStyleId" @change="rememberWritingStyle"><option v-for="style in writingStyles" :key="style.id" :value="style.id">{{ style.system ? '系统 · ' : '我的 · ' }}{{ style.name }}</option></select></label>
            <div v-if="writingStyles.find(style => style.id === selectedWritingStyleId)" class="project-style-preview"><strong>{{ writingStyles.find(style => style.id === selectedWritingStyleId)?.name }}</strong><p>{{ writingStyles.find(style => style.id === selectedWritingStyleId)?.description || '暂无风格说明' }}</p><pre>{{ writingStyles.find(style => style.id === selectedWritingStyleId)?.rulesText }}</pre></div>
            <button class="project-style-manage" @click="openStyleManager">管理我的文风</button>
          </section>
          <section v-show="projectSettingsTab === 'MEMORY'" class="project-card project-memory-card">
            <header><span>06</span><div><h2>讨论长期记忆</h2><p>AI 每轮都会读取这份账本，即使设定还没有应用到正式立项也不会遗忘。</p></div></header>
            <div v-if="!projectMemoryGroups.some(group => group.items.length)" class="project-memory-empty">继续一轮对话后，系统会自动回溯现有历史并建立记忆账本。</div>
            <div v-for="group in projectMemoryGroups.filter(group => group.items.length)" :key="group.label" class="project-memory-group"><strong>{{ group.label }}</strong><ul><li v-for="item in group.items" :key="item">{{ item }}</li></ul></div>
            <small v-if="projectMemory.sourceMessageIds.length">已整理 {{ projectMemory.sourceMessageIds.length }} 条原始消息来源</small>
          </section>
        </div>
        <footer class="project-save-bar"><div><strong>{{ projectAiDraftPending ? 'AI 草案等待确认' : '手工修改自动保存' }}</strong><span>{{ projectAiDraftPending ? '检查生成结果；继续手工修改也视为确认，并会自动保存。' : '停止输入约一秒后保存，离开页面前也会提交待保存内容。' }}</span></div><el-button type="primary" size="large" :loading="savingProject" @click="saveProject">{{ projectAiDraftPending ? '确认并保存 AI 草案' : '立即保存' }}</el-button></footer>
        </div>
        <aside class="project-chat">
          <header><div><span>✦</span><div><strong>立项顾问</strong><small>先给方向 · 细节慢慢补</small></div></div></header>
          <section v-if="projectWrittenThrough" class="project-chat-source">
            <div><strong>补建立项模式</strong><small>当前已写至第 {{ projectWrittenThrough }} 章</small></div>
            <label><input v-model="projectImportWrittenStory" type="checkbox" /><span>本轮导入已写正文</span></label>
            <p v-if="projectImportWrittenStory">3 万字内读取全文；超过后读取前 3 章、最近 2 章与中间章已确认的压缩事实。未确认的压缩不会作为事实。</p>
            <p v-else>AI 只使用立项资料和对话，不读取你已写的章节。</p>
          </section>
          <div ref="projectChatMessagesRef" class="project-chat-messages">
            <div v-if="loadingProjectChat" class="project-chat-empty"><el-icon class="is-loading"><Loading /></el-icon><span>正在读取对话…</span></div>
            <div v-else-if="!projectChatMessages.length" class="project-chat-empty"><span>✦</span><strong>一句灵感就能开始</strong><p>说说你想写的人物或故事，我先给一个可写的方向。没想好的设定可以以后再补。</p></div>
            <article v-for="message in projectChatMessages" :key="message.id" :class="['project-chat-message', { user: message.role === 'USER' }]">
              <strong>{{ message.role === 'USER' ? '你' : '立项顾问' }}</strong>
              <p>{{ message.content }}</p>
              <small v-if="message.analyzedThroughChapterNo">本轮已分析至第 {{ message.analyzedThroughChapterNo }} 章</small>
              <section v-if="message.suggestion" class="project-suggestion-card">
                <div><strong>可应用的立项方案</strong><span>{{ message.applied ? '已应用' : '待确认' }}</span></div>
                <dl><template v-for="item in projectSuggestionHighlights(message.suggestion)" :key="item.label"><dt>{{ item.label }}</dt><dd>{{ item.value }}</dd></template></dl>
                <button :disabled="message.applied || applyingProjectSuggestion === message.id" @click="applyProjectChatSuggestion(message)">{{ message.applied ? '已应用到正式立项' : applyingProjectSuggestion === message.id ? '正在应用…' : '应用到正式立项' }}</button>
              </section>
            </article>
            <article v-if="sendingProjectChat" class="project-chat-message"><strong>立项顾问</strong><p class="project-chat-thinking"><el-icon class="is-loading"><Loading /></el-icon> 正在理解你的想法…</p></article>
          </div>
          <footer>
            <textarea v-model="projectChatInput" maxlength="5000" placeholder="例如：我想写男频都市异能，节奏快，不要系统流，能力必须有代价…" @keydown.ctrl.enter.prevent="sendProjectChat('DISCUSS')"></textarea>
            <div class="project-chat-quick"><button @click="sendProjectChat('FOUNDATION')">整理方向</button><button @click="sendProjectChat('DESCRIPTION')">生成简介</button><button @click="sendProjectChat('OUTLINE')">生成总纲</button><button @click="sendProjectChat('ALL')">生成草案</button></div>
            <div class="project-chat-send"><span>Ctrl + Enter 发送</span><button :disabled="sendingProjectChat || !projectChatInput.trim()" @click="sendProjectChat('DISCUSS')">发送</button></div>
          </footer>
        </aside>
        </div>
      </template>
    </main>
  </section>

  <section v-if="currentView === 'storyBible'" class="story-library-shell">
    <header class="story-library-header">
      <div class="story-library-brand"><button class="back-home-button" title="返回" @click="closeStoryBiblePage"><el-icon><ArrowLeft /></el-icon></button><span class="brand-mark"><span>墨</span></span><div><div class="brand-name">大纲</div><div class="brand-tagline">从全书定位到章节事实，集中管理故事结构</div></div></div>
      <div class="story-library-book"><small>当前作品</small><strong>{{ novelTitle }}</strong></div>
      <button class="story-library-return" @click="closeStoryBiblePage">{{ storyBibleReturnView === 'director' ? '返回章节导演' : '返回正文' }}</button>
    </header>
    <main class="story-library-main">
      <section class="story-library-overview"><div><span>STORY OUTLINE</span><h1>这本书的统一大纲</h1><p>立项结论、全书总纲、分卷计划、人物和世界观都放在这里；已写章节的确认事实也会持续回流。</p></div><div class="story-library-stats"><article><strong>{{ projectForm.expectedVolumes || storyVolumes.length || '—' }}</strong><span>预计卷数</span></article><article><strong>{{ storyVolumes.length }}</strong><span>分卷主线</span></article><article><strong>{{ characters.length }}</strong><span>人物</span></article><article><strong>{{ worldSettings.length }}</strong><span>世界设定</span></article></div></section>
      <div v-if="showBibleLoading" class="story-library-loading"><el-icon class="is-loading"><Loading /></el-icon><span>正在整理故事资料…</span></div>
      <div v-else class="story-library-workspace">
      <section class="story-library-card">
    <p class="bible-intro">立项与全书总纲提供方向，已确认的分卷主线约束当前卷；人物、组织和世界观按创作内容按需读取。</p>
    <div v-if="bibleTab !== 'foundation' && bibleTab !== 'volumes'" class="bible-ai-scope-bar"><div><strong>AI 修改范围</strong><small>自动识别会从你的要求中寻找字段名；也可以在这里明确指定</small></div><select v-model="bibleAiTarget"><option value="AUTO">根据要求自动识别</option><option v-for="field in currentBibleFieldOptions" :key="field.value" :value="field.value">只修改：{{ field.label }}</option></select></div>
    <el-tabs v-model="bibleTab" tab-position="top" class="story-library-tabs" @tab-change="bibleAiTarget = 'AUTO'">
      <el-tab-pane label="立项与总纲" name="foundation">
        <div class="outline-foundation">
          <header><div><small>已确认的作品方向</small><h2>{{ novelTitle }}</h2><p>这里展示立项工作台保存后的正式结论，后续 AI 创作会以这些内容作为全书方向。</p></div><button @click="openProjectPlanning">编辑立项与总纲</button></header>
          <section class="outline-positioning"><article><span>题材 / 频道</span><strong>{{ [projectForm.genre, projectForm.channel].filter(Boolean).join(' · ') || '尚未确定' }}</strong></article><article><span>目标读者</span><strong>{{ projectForm.targetAudience || '尚未确定' }}</strong></article><article><span>发布平台</span><strong>{{ projectForm.platform || '尚未确定' }}</strong></article><article><span>预计规模</span><strong>{{ projectForm.expectedWords ? `${projectForm.expectedWords.toLocaleString()} 字` : '字数未定' }} · {{ projectForm.expectedVolumes || '—' }} 卷</strong></article></section>
          <div class="outline-foundation-grid"><article><span>核心卖点</span><p>{{ projectForm.coreSellingPoint || '尚未确定核心卖点。' }}</p></article><article><span>主角能力 / 核心机制</span><p>{{ projectForm.protagonistHook || '尚未确定核心机制。' }}</p></article><article><span>升级路线</span><p>{{ projectForm.growthRoute || '尚未规划升级路线。' }}</p></article><article><span>主要爽点与读者期待</span><p>{{ projectForm.readerExpectations || '尚未整理读者期待。' }}</p></article></div>
          <article class="outline-long-card"><span>小说简介</span><p>{{ projectForm.description || '尚未填写小说简介。' }}</p></article>
          <article class="outline-long-card"><span>全书总纲</span><p>{{ projectForm.outline || '尚未填写全书总纲。' }}</p></article>
          <article class="outline-long-card"><span>开篇三章设计</span><p>{{ projectForm.openingThreeChapters || '尚未规划开篇三章。' }}</p></article>
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`卷 / 部分 / 章 (${storyVolumes.length})`" name="volumes">
        <div class="outline-tree-layout">
          <aside class="outline-tree">
            <header><div><strong>故事结构</strong><span>卷 → 部分 → 章</span></div><button title="新建卷" @click="resetStoryVolumeForm">＋</button></header>
            <div v-if="!storyVolumes.length" class="outline-tree-empty">还没有卷，点击右上角 ＋ 开始。</div>
            <section v-for="volume in storyVolumes" :key="volume.id" class="outline-tree-volume">
              <button class="outline-tree-row volume-node" :class="{ active: outlineNodeType === 'volume' && storyVolumeForm.id === volume.id }" @click="editStoryVolume(volume)"><span class="tree-caret">{{ storyVolumeForm.id === volume.id ? '⌄' : '›' }}</span><b>第{{ volume.volumeNo }}卷</b><strong>{{ volume.title }}</strong><em>{{ volume.status === 'CONFIRMED' ? '已确认' : '草稿' }}</em></button>
              <div v-if="storyVolumeForm.id === volume.id" class="outline-tree-children">
                <template v-for="part in storyParts" :key="part.id">
                  <button class="outline-tree-row part-node" :class="{ active: outlineNodeType === 'part' && storyPartForm.id === part.id }" @click="editStoryPart(part)"><span class="tree-line"></span><b>第{{ part.partNo }}部分</b><strong>{{ part.title }}</strong></button>
                  <div v-if="storyPartForm.id === part.id" class="outline-tree-chapters">
                    <button v-for="chapter in chaptersForPart(part)" :key="chapter.id" class="outline-tree-row chapter-node" :class="{ active: outlineNodeType === 'chapter' && selectedOutlineChapter?.id === chapter.id }" @click="selectOutlineChapter(chapter)"><span class="tree-line"></span><b>第{{ chapter.chapterNo }}章</b><strong>{{ chapter.title || '未命名章节' }}</strong></button>
                  </div>
                </template>
                <button class="outline-tree-add" @click="resetStoryPartForm()">＋ 新建部分</button>
              </div>
            </section>
          </aside>
          <main class="outline-detail-page">
            <section v-if="outlineNodeType === 'volume'" class="outline-detail-card volume-detail-card">
              <header><span>卷</span><div><h2>{{ storyVolumeForm.id ? `第${storyVolumeForm.volumeNo}卷` : '新建卷' }}</h2><p>只说明这一卷主要写什么。阶段拆解和章节细节放到“部分”和“章纲”中。</p></div></header>
              <div class="outline-compact-fields"><label><span>卷序 *</span><input v-model.number="storyVolumeForm.volumeNo" type="number" min="1" /></label><label><span>卷标题 *</span><input v-model="storyVolumeForm.title" maxlength="200" placeholder="例如：雾港来信" /></label></div>
              <label><span>这一卷写什么 *</span><textarea v-model="storyVolumeForm.objective" rows="10" placeholder="用一段话说明本卷的主要事件、核心冲突和大致结果，不需要拆到章节。"></textarea></label>
              <div class="outline-detail-actions"><el-button v-if="storyVolumeForm.id" type="danger" plain @click="deleteStoryVolume">删除卷</el-button><span></span><el-button @click="saveStoryVolume('GENERATED')">保存草稿</el-button><el-button type="primary" :loading="savingBible" @click="saveStoryVolume('CONFIRMED')">确认本卷</el-button></div>
            </section>
            <section v-else-if="outlineNodeType === 'part'" class="outline-detail-card part-detail-card">
              <header><span>部分</span><div><h2>{{ storyPartForm.id ? `第${storyPartForm.partNo}部分` : '新建部分' }}</h2><p>部分代表卷内一个较大的阶段事件，章节范围用于把已有正文归到这部分。</p></div></header>
              <div class="outline-part-fields"><label><span>部分序号 *</span><input v-model.number="storyPartForm.partNo" type="number" min="1" /></label><label><span>部分标题 *</span><input v-model="storyPartForm.title" maxlength="200" placeholder="例如：潜入雾港" /></label><label><span>起始章 *</span><input v-model.number="storyPartForm.chapterStart" type="number" min="1" /></label><label><span>结束章</span><input v-model.number="storyPartForm.chapterEnd" type="number" :min="storyPartForm.chapterStart" placeholder="暂不确定" /></label></div>
              <label><span>核心事件 *</span><textarea v-model="storyPartForm.objective" rows="5" placeholder="这部分要完成的较大事件是什么？"></textarea></label>
              <label><span>详细安排</span><textarea v-model="storyPartForm.plan" rows="8" placeholder="事件如何开始、发展和结束；具体到单章的安排留给章纲。"></textarea></label>
              <div class="outline-detail-actions"><el-button v-if="storyPartForm.id" type="danger" plain @click="deleteStoryPart">删除部分</el-button><span></span><el-button @click="saveStoryPart('GENERATED')">保存草稿</el-button><el-button type="primary" :loading="savingStoryPart" @click="saveStoryPart('CONFIRMED')">确认部分</el-button></div>
            </section>
            <section v-else class="outline-detail-card chapter-detail-card">
              <header><span>章</span><div><h2>第{{ selectedOutlineChapter?.chapterNo }}章 · {{ selectedOutlineChapter?.title || '未命名章节' }}</h2><p>章纲在章节导演中维护，写作前可以继续讨论和修改，不会被这里的卷或部分锁死。</p></div></header>
              <div class="chapter-detail-summary"><article><span>正文状态</span><strong>{{ selectedOutlineChapter?.words ? `${selectedOutlineChapter.words.toLocaleString()} 字` : '尚未写作' }}</strong></article><article><span>所属部分</span><strong>第{{ storyPartForm.partNo }}部分 · {{ storyPartForm.title }}</strong></article></div>
              <button class="open-chapter-outline" @click="openSelectedChapterOutline">进入本章章纲与章节导演 →</button>
            </section>
          </main>
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`人物档案 (${characters.length})`" name="characters">
        <div class="bible-layout">
          <div class="bible-list"><button v-for="item in characters" :key="item.id" :class="{ active: characterForm.id === item.id }" @click="editCharacter(item)"><strong>{{ item.name }}</strong><span>{{ item.role || '未设置角色定位' }}<template v-if="item.identities?.length"> · 身份：{{ item.identities.map(identity => identity.identityName).join('、') }}</template></span></button><div v-if="!characters.length" class="empty-bible">还没有人物，先添加主角吧。</div></div>
          <div class="bible-form"><label><span>人物姓名 *</span><input v-model="characterForm.name" maxlength="100" /></label><label><span>别名/简称</span><input v-model="characterForm.aliases" maxlength="500" placeholder="多个名称用逗号分隔，例如：卡尔，伯格先生" /></label><label><span>角色定位</span><input v-model="characterForm.role" placeholder="主角、反派、盟友…" /></label><label><span>所属组织</span><input v-model="characterForm.affiliations" list="character-organization-options" maxlength="500" placeholder="输入组织名称，多个组织使用逗号分隔" /><datalist id="character-organization-options"><option v-for="organization in organizations" :key="organization.id" :value="organization.name">{{ organization.aliases || organization.type || '' }}</option></datalist></label><label><span>身份与经历</span><textarea v-model="characterForm.description"></textarea></label><label><span>性格特征（保持最新）</span><textarea v-model="characterForm.personality"></textarea></label><label><span>目标与动机</span><textarea v-model="characterForm.goal"></textarea></label><label><span>当前状态</span><textarea v-model="characterForm.currentState" placeholder="当前位置、身体状态、掌握的信息等"></textarea></label><label><span>当前关系</span><textarea v-model="characterForm.relationships" placeholder="例如：对哈基米由敌对转为友好"></textarea></label><div v-if="characterForm.id" class="character-history-panel"><div class="character-history-title"><strong>人物历史</strong><small>由已确认的章节记忆自动追加</small></div><div v-if="loadingCharacterHistory" class="memory-empty-row">正在读取人物历史…</div><div v-else-if="!characterHistory.length" class="memory-empty-row">还没有已确认的人物历史</div><article v-for="item in characterHistory" :key="item.id"><strong>第{{ item.chapterNo }}章</strong><p v-if="item.actions">{{ item.actions }}</p><p v-if="item.stateChange">状态：{{ item.stateChange }}</p><p v-if="item.newKnowledge">新认知：{{ item.newKnowledge }}</p><p v-for="clue in item.foreshadowings" :key="clue">伏笔：{{ clue }}</p></article></div><div class="bible-ai-box"><div><strong>✦ AI 辅助完善</strong><small>生成内容只回填表单；相关人物和组织会按名称命中后装入上下文</small></div><textarea v-model="bibleAiInstruction" maxlength="5000" placeholder="输入关键信息，例如：哈基米是落魄镖师，谨慎但重情，正在寻找失踪的父亲"></textarea><div class="bible-ai-actions"><button v-for="item in bibleAiModes" :key="item.value" :class="{ active: bibleAiMode === item.value }" @click="bibleAiMode = item.value">{{ item.label }}</button><el-button type="primary" :loading="assistingBible === 'CHARACTER'" @click="assistStoryBible('CHARACTER')">AI {{ bibleAiModeLabel }}</el-button></div></div><div class="bible-actions"><el-button v-if="characterForm.id" type="danger" plain @click="deleteCharacter">删除</el-button><span></span><el-button @click="resetCharacterForm">新建</el-button><el-button type="primary" :loading="savingBible" @click="saveCharacter">保存人物</el-button></div></div>
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`组织档案 (${organizations.length})`" name="organizations">
        <div class="bible-layout">
          <div class="bible-list"><button v-for="item in organizations" :key="item.id" :class="{ active: organizationForm.id === item.id }" @click="editOrganization(item)"><strong>{{ item.name }}</strong><span>{{ item.type || item.aliases || '未设置组织类型' }}</span></button><div v-if="!organizations.length" class="empty-bible">还没有组织，可以添加宗门、公司、家族或其他势力。</div></div>
          <div class="bible-form"><label><span>组织名称 *</span><input v-model="organizationForm.name" maxlength="100" placeholder="例如：玄天剑宗" /></label><label><span>组织类型</span><input v-model="organizationForm.type" maxlength="100" placeholder="宗门、公司、家族、帮派…" /></label><label><span>别名</span><input v-model="organizationForm.aliases" maxlength="500" placeholder="多个别名使用逗号分隔，例如：玄天宗，剑宗" /></label><label><span>组织概况</span><textarea v-model="organizationForm.description" placeholder="来历、活动范围、公开身份与核心特征"></textarea></label><label><span>目标与立场</span><textarea v-model="organizationForm.goal" placeholder="当前目标、利益诉求、阵营立场"></textarea></label><label><span>结构与重要成员</span><textarea v-model="organizationForm.structure" placeholder="首领、部门、等级、已知成员及职责"></textarea></label><label><span>与其他势力的关系</span><textarea v-model="organizationForm.relationships" placeholder="盟友、敌对、附属、竞争或秘密合作关系"></textarea></label><div class="bible-ai-box"><div><strong>✦ AI 辅助完善</strong><small>可根据组织名称和零散设定补全档案</small></div><textarea v-model="bibleAiInstruction" maxlength="5000" placeholder="输入关键信息，例如：表面经营航运，暗中收集古代遗物，与城主府互相利用"></textarea><div class="bible-ai-actions"><button v-for="item in bibleAiModes" :key="item.value" :class="{ active: bibleAiMode === item.value }" @click="bibleAiMode = item.value">{{ item.label }}</button><el-button type="primary" :loading="assistingBible === 'ORGANIZATION'" @click="assistStoryBible('ORGANIZATION')">AI {{ bibleAiModeLabel }}</el-button></div></div><div class="bible-actions"><el-button v-if="organizationForm.id" type="danger" plain @click="deleteOrganization">删除</el-button><span></span><el-button @click="resetOrganizationForm">新建</el-button><el-button type="primary" :loading="savingBible" @click="saveOrganization">保存组织</el-button></div></div>
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`世界观设定 (${worldSettings.length})`" name="world">
        <div class="bible-layout"><div class="bible-list"><button v-for="item in worldSettings" :key="item.id" :class="{ active: worldSettingForm.id === item.id }" @click="editWorldSetting(item)"><strong>{{ item.title }}</strong><span>{{ item.category }}</span></button><div v-if="!worldSettings.length" class="empty-bible">还没有设定，可以添加地点、规则或势力。</div></div><div class="bible-form"><label><span>分类 *</span><input v-model="worldSettingForm.category" placeholder="地点、力量体系、组织、历史…" /></label><label><span>设定标题 *</span><input v-model="worldSettingForm.title" /></label><label><span>详细内容 *</span><textarea v-model="worldSettingForm.content" class="large"></textarea></label><div class="bible-ai-box"><div><strong>✦ AI 辅助完善</strong><small>根据核心规则扩写成可复用的世界观资料</small></div><textarea v-model="bibleAiInstruction" maxlength="5000" placeholder="输入关键信息，例如：修炼分为五境，突破会遗忘一段记忆，越高境界代价越大"></textarea><div class="bible-ai-actions"><button v-for="item in bibleAiModes" :key="item.value" :class="{ active: bibleAiMode === item.value }" @click="bibleAiMode = item.value">{{ item.label }}</button><el-button type="primary" :loading="assistingBible === 'WORLD_SETTING'" @click="assistStoryBible('WORLD_SETTING')">AI {{ bibleAiModeLabel }}</el-button></div></div><div class="bible-actions"><el-button v-if="worldSettingForm.id" type="danger" plain @click="deleteWorldSetting">删除</el-button><span></span><el-button @click="resetWorldSettingForm">新建</el-button><el-button type="primary" :loading="savingBible" @click="saveWorldSetting">保存设定</el-button></div></div></div>
      </el-tab-pane>
    </el-tabs>
      </section>
      <aside class="story-bible-chat">
        <header>
          <div><span>✦</span><div><strong>大纲顾问</strong><small>多轮讨论 · 修改前由你确认</small></div></div>
          <em>{{ storyBibleChatFocus.label }}</em>
        </header>
        <div ref="storyBibleChatMessagesRef" class="story-bible-chat-messages">
          <div v-if="loadingStoryBibleChat" class="story-bible-chat-empty">
            <el-icon class="is-loading"><Loading /></el-icon><span>正在读取大纲讨论…</span>
          </div>
          <div v-else-if="!storyBibleChatMessages.length" class="story-bible-chat-empty">
            <span>◇</span><strong>和 AI 一起搭建故事关系</strong>
            <p>可以讨论人物、组织和分卷之间的关系，也可以让 AI 为当前卷设计反派或其他新角色。</p>
            <button @click="storyBibleChatInput = '结合当前分卷和已有角色，帮我设计一组有内在联系的反派人物。'">试试：为本卷设计反派</button>
          </div>
          <article v-for="message in storyBibleChatMessages" v-else :key="message.id" :class="['story-bible-chat-message', message.role.toLowerCase()]">
            <strong>{{ message.role === 'USER' ? '你' : '大纲顾问' }}</strong>
            <small v-if="message.role === 'ASSISTANT' && message.recalledMessageIds.length">本轮参考了 {{ message.recalledMessageIds.length }} 条早期原始对话</small>
            <p>{{ message.content }}</p>
            <section v-for="(suggestion,index) in message.suggestions" :key="index" class="character-suggestion-card">
              <div><span>{{ suggestion.action === 'CREATE' ? '新建' : '修改' }}{{ storyBibleSuggestionTypeLabel(suggestion.type) }}</span><em>{{ suggestion.role || suggestion.category || suggestion.objective || '资料方案' }}</em></div>
              <h4>{{ suggestion.name || suggestion.title || `第${suggestion.volumeNo || '?'}卷` }}</h4>
              <p v-if="suggestion.rationale">{{ suggestion.rationale }}</p>
              <dl>
                <template v-if="suggestion.affiliations"><dt>所属</dt><dd>{{ suggestion.affiliations }}</dd></template>
                <template v-if="suggestion.goal"><dt>目标</dt><dd>{{ suggestion.goal }}</dd></template>
                <template v-if="suggestion.personality"><dt>性格</dt><dd>{{ suggestion.personality }}</dd></template>
                <template v-if="suggestion.relationships"><dt>关系</dt><dd>{{ suggestion.relationships }}</dd></template>
                <template v-if="suggestion.content"><dt>设定</dt><dd>{{ suggestion.content }}</dd></template>
                <template v-if="suggestion.futurePlan"><dt>规划</dt><dd>{{ suggestion.futurePlan }}</dd></template>
              </dl>
              <button :disabled="message.appliedSuggestionIndexes.includes(index) || applyingStoryBibleSuggestion === `${message.id}-${index}`" @click="applyStoryBibleSuggestion(message,index)">
                {{ message.appliedSuggestionIndexes.includes(index) ? '已写入大纲' : applyingStoryBibleSuggestion === `${message.id}-${index}` ? '正在应用…' : suggestion.action === 'CREATE' ? '创建到大纲' : '应用资料修改' }}
              </button>
            </section>
          </article>
        </div>
        <footer>
          <textarea v-model="storyBibleChatInput" maxlength="4000" :disabled="sendingStoryBibleChat" placeholder="例如：结合第二卷，设计一名与主角导师有旧关系的反派……" @keydown="handleStoryBibleChatKeydown"></textarea>
          <div><span>当前关注：{{ storyBibleChatFocus.label }} · Enter 发送，Shift + Enter 换行</span><button :disabled="sendingStoryBibleChat || !storyBibleChatInput.trim()" @click="sendStoryBibleChat">{{ sendingStoryBibleChat ? '思考中…' : '发送' }}</button></div>
        </footer>
      </aside>
      </div>
    </main>
  </section>

  <el-dialog v-model="showChapterMemory" title="章节结构化记忆" width="min(920px, calc(100vw - 28px))" :close-on-click-modal="false" class="chapter-memory-dialog">
    <div class="memory-dialog-head"><div><strong>{{ chapterTitle }}</strong><p>较早章节会使用这份压缩记忆参与规划、改写和一致性检查，原始正文不会被删除。</p></div><span :class="memoryStatusClass">{{ memoryStatusText }}</span></div>
    <div v-if="loadingMemory" class="memory-loading"><el-icon class="is-loading"><Loading /></el-icon> 正在读取章节记忆…</div>
    <template v-else>
      <div class="memory-extract-bar"><input v-model="memoryInstruction" maxlength="1000" placeholder="可选：补充提取重点，例如特别关注黑衣人的首次出场和蛇形刺青" /><el-button type="primary" :loading="extractingMemory" @click="extractChapterMemory">{{ chapterMemory ? '重新提取' : 'AI 提取章节记忆' }}</el-button></div>
      <div class="director-thinking-choice memory-thinking-choice"><span>思考模式</span><button :class="{ active: !memoryThinkingEnabled }" @click="memoryThinkingEnabled = false">快速</button><button :class="{ active: memoryThinkingEnabled }" @click="memoryThinkingEnabled = true">深度思考</button><small>{{ memoryThinkingEnabled ? '适合情节复杂、伏笔较多的章节，耗时和 Token 会明显增加' : '推荐：结构化提取更快，也更不容易因推理过长而截断' }}</small></div>
      <div v-if="memoryForm" class="memory-form">
        <section><div class="memory-section-title"><h4>本章故事线</h4><small>{{ memoryForm.skillVersion }}</small></div><label class="memory-wide"><span>本章主要讲了什么 *</span><textarea v-model="memoryForm.summary" rows="4"></textarea></label><div class="memory-grid"><label><span>时间范围</span><input v-model="memoryForm.timeInfo" placeholder="例如：上午至下午、三天后" /></label><label><span>重要程度</span><select v-model="memoryForm.importance"><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select></label></div><label class="memory-wide"><span>涉及地点（使用逗号分隔）</span><input v-model="memoryLocationsText" placeholder="大山，河边，废弃水道" /></label><label class="memory-wide"><span>对主线的推进</span><textarea v-model="memoryForm.plotProgress" rows="2"></textarea></label></section>
        <section><div class="memory-section-title"><h4>连续叙事场景</h4><button @click="addMemoryScene">＋ 添加场景</button></div><div v-for="(scene,index) in memoryForm.scenes" :key="index" class="memory-scene-card"><div class="memory-card-head"><strong>场景 {{ index + 1 }}</strong><select v-model="scene.boundaryType"><option value="HARD">新叙事单元</option><option value="SOFT">与上一场景连续</option></select><button @click="memoryForm.scenes.splice(index,1)">×</button></div><div class="memory-grid"><label><span>场景标题</span><input v-model="scene.title" /></label><label><span>时间范围</span><input v-model="scene.timeSpan" placeholder="上午至下午" /></label></div><label class="memory-wide"><span>地点（逗号分隔）</span><input :value="scene.locations.join('，')" @input="scene.locations = splitMemoryList(($event.target as HTMLInputElement).value)" /></label><label class="memory-wide"><span>人物（逗号分隔）</span><input :value="scene.characters.join('，')" @input="scene.characters = splitMemoryList(($event.target as HTMLInputElement).value)" /></label><div class="memory-grid"><label><span>目标</span><textarea v-model="scene.goal" rows="2"></textarea></label><label><span>冲突</span><textarea v-model="scene.conflict" rows="2"></textarea></label></div><label class="memory-wide"><span>子事件（每行一条）</span><textarea :value="scene.subEvents.map(item => [item.time,item.location,item.event].filter(Boolean).join('｜')).join('\n')" @input="scene.subEvents = parseSceneEvents(($event.target as HTMLTextAreaElement).value)" rows="4" placeholder="上午｜大山｜两人一路交战进入山中&#10;下午｜河边｜战斗转移到河边继续"></textarea></label><label class="memory-wide"><span>场景结果</span><textarea v-model="scene.result" rows="2"></textarea></label></div><div v-if="!memoryForm.scenes.length" class="memory-empty-row">本章没有可保留的连续叙事场景</div></section>
        <section><div class="memory-section-title"><h4>人物历史与档案更新</h4><button @click="addMemoryCharacter">＋ 添加人物</button></div><p class="memory-section-note">历史在确认后自动追加；只有勾选的档案变更会更新人物当前性格、状态、关系或阵营。</p><div v-for="(item,index) in memoryForm.characters" :key="index" class="memory-character-card"><div class="memory-card-head"><strong>{{ item.name || `人物 ${index + 1}` }}</strong><label class="first-appearance"><input v-model="item.firstAppearance" type="checkbox" /> 首次登场（无档案时将自动创建）</label><button @click="memoryForm.characters.splice(index,1)">×</button></div><div class="memory-grid"><label><span>姓名/称谓</span><input v-model="item.name" /></label><label><span>本章角色</span><input v-model="item.role" /></label></div><label class="memory-wide"><span>本章重要行为</span><textarea v-model="item.actions" rows="2"></textarea></label><div class="memory-grid"><label><span>状态变化</span><textarea v-model="item.stateChange" rows="2"></textarea></label><label><span>新获得的信息</span><textarea v-model="item.newKnowledge" rows="2"></textarea></label></div><label class="memory-wide"><span>人物相关伏笔（每行一条）</span><textarea :value="item.foreshadowings.join('\n')" @input="item.foreshadowings = ($event.target as HTMLTextAreaElement).value.split('\n').map(value => value.trim()).filter(Boolean)" rows="2"></textarea></label><div v-if="item.profileChanges.length" class="profile-change-list"><strong>档案变更建议</strong><article v-for="(change,changeIndex) in item.profileChanges" :key="changeIndex"><label class="profile-change-toggle"><input v-model="change.applyToProfile" type="checkbox" /><span>应用到当前档案</span></label><div><b>{{ profileFieldLabel(change.field) }} · {{ profileOperationLabel(change.operation) }}</b><p>{{ change.oldValue ? `${change.oldValue} → ` : '' }}{{ change.newValue }}</p><small>{{ change.evidence || '未提供依据' }} · 置信度{{ confidenceLabel(change.confidence) }}</small></div></article></div></div><div v-if="!memoryForm.characters.length" class="memory-empty-row">本章没有需要长期记录的人物</div></section>
        <section><div class="memory-section-title"><h4>其他重要信息</h4><button @click="addMemoryFact">＋ 添加信息</button></div><div v-for="(item,index) in memoryForm.importantFacts" :key="index" class="memory-fact-row"><select v-model="item.type"><option value="FORESHADOWING">伏笔</option><option value="UNRESOLVED_THREAD">未解决问题</option><option value="IMPORTANT_ITEM">重要物品</option><option value="WORLD_RULE">世界规则</option><option value="LOCATION_STATE">地点状态</option><option value="RELATIONSHIP">关系变化</option><option value="TIME_MARKER">时间标记</option><option value="REVEAL">身份/真相揭露</option><option value="OTHER">其他</option></select><textarea v-model="item.content" rows="2"></textarea><select v-model="item.importance"><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select><button @click="memoryForm.importantFacts.splice(index,1)">×</button></div><div v-if="!memoryForm.importantFacts.length" class="memory-empty-row">本章没有其他需要长期保留的信息</div></section>
      </div>
      <div v-else class="memory-empty-state"><span>◈</span><strong>还没有章节记忆</strong><p>点击“AI 提取章节记忆”，系统会把长篇正文压缩成可编辑的结构化故事状态。</p></div>
    </template>
    <div v-if="memoryForm?.identityReveals.length" class="identity-reveal-review"><strong>身份揭露与人物合并</strong><article v-for="(reveal,index) in memoryForm.identityReveals" :key="index"><label><input v-model="reveal.applyMerge" type="checkbox" /><span>确认将“{{ reveal.identityName }}”合并为“{{ reveal.realCharacterName }}”</span></label><p>{{ reveal.evidence || '正文明确揭露了两个身份属于同一人物' }} · 置信度{{ confidenceLabel(reveal.confidence) }}</p></article><small>取消勾选会保留两个独立人物；确认合并后，旧章节仍保留当时使用的称谓。</small></div>
    <template #footer><el-button @click="showChapterMemory = false">关闭</el-button><el-button v-if="memoryForm" type="primary" :loading="confirmingMemory" :disabled="chapterMemory?.stale" @click="confirmChapterMemory">确认并用于后续创作</el-button></template>
  </el-dialog>

  <el-dialog v-model="showRunCenter" title="Agent 运行中心" width="min(1040px, calc(100vw - 28px))" class="run-center-dialog">
    <div class="run-center-summary">
      <div><span>最近运行</span><strong>{{ runSummaries.length }}</strong></div>
      <div><span>累计 Token</span><strong>{{ runTotalTokens.toLocaleString() }}</strong></div>
      <div><span>失败任务</span><strong>{{ failedRunCount }}</strong></div>
      <button :disabled="loadingRuns" @click="loadAgentRuns"><el-icon :class="{ 'is-loading': loadingRuns }"><Refresh /></el-icon> 刷新</button>
    </div>
    <div v-if="loadingRuns && !runSummaries.length" class="run-center-loading"><el-icon class="is-loading"><Loading /></el-icon> 正在读取运行记录…</div>
    <div v-else-if="!runSummaries.length" class="run-center-empty">还没有 AI 运行记录。使用一次续写、改写、检查或章节记忆后，这里会显示模型调用详情。</div>
    <div v-else class="run-center-layout">
      <div class="run-list">
        <button v-for="run in runSummaries" :key="run.id" :class="{ active: selectedRun?.id === run.id }" @click="selectAgentRun(run)">
          <span class="run-operation">{{ operationLabel(run.operation) }}</span><i :class="`status-${run.status.toLowerCase()}`">{{ runStatusLabel(run.status) }}</i>
          <strong>{{ run.chapterTitle }}</strong>
          <small>{{ run.model || run.providerMode }} · {{ run.totalTokens.toLocaleString() }} Token · {{ formatDuration(run.totalDurationMs) }}</small>
          <time>{{ formatRevisionTime(run.createdAt) }}</time>
        </button>
      </div>
      <div class="run-detail">
        <div v-if="loadingRunDetail" class="run-center-loading"><el-icon class="is-loading"><Loading /></el-icon> 正在读取调用详情…</div>
        <template v-else-if="selectedRun">
          <div class="run-detail-head"><div><small>#{{ selectedRun.id }}</small><h3>{{ operationLabel(selectedRun.operation) }}</h3><p>{{ selectedRun.model || selectedRun.providerMode }} · {{ runStatusLabel(selectedRun.status) }}</p></div><div><strong>{{ selectedRun.totalTokens.toLocaleString() }}</strong><span>总 Token</span></div></div>
          <div class="metric-grid"><div><span>输入 Token</span><strong>{{ selectedRun.promptTokens.toLocaleString() }}</strong></div><div><span>输出 Token</span><strong>{{ selectedRun.completionTokens.toLocaleString() }}</strong></div><div><span>总耗时</span><strong>{{ formatDuration(selectedRun.totalDurationMs) }}</strong></div><div><span>重试</span><strong>{{ selectedRun.retryCount }} 次</strong></div></div>
          <div v-if="selectedRun.errorMessage" class="run-error"><strong>失败原因</strong><p>{{ selectedRun.errorMessage }}</p></div>
          <section class="call-timeline"><h4>模型调用</h4><article v-for="call in selectedRun.calls" :key="call.id"><span :class="call.status === 'COMPLETED' ? 'call-ok' : 'call-failed'">{{ call.status === 'COMPLETED' ? '✓' : '!' }}</span><div><strong>{{ stepLabel(call.stepType) }}</strong><p>{{ call.model }} · {{ call.totalTokens.toLocaleString() }} Token · {{ formatDuration(call.durationMs) }}<template v-if="call.retryCount"> · 重试 {{ call.retryCount }} 次</template></p><small v-if="call.errorMessage">{{ call.errorMessage }}</small></div></article><p v-if="!selectedRun.calls.length" class="no-calls">该历史任务没有模型调用明细。</p></section>
        </template>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="showEvalCenter" title="Agent 评测中心" width="min(1120px, calc(100vw - 28px))" class="eval-center-dialog" :close-on-click-modal="false">
    <div class="eval-toolbar"><div><strong>用可重复的样本验证续写质量</strong><p>规则评分检查硬约束，LLM Judge 评估连贯性、文风与指令遵循。</p></div><div><button class="eval-batch-button" :disabled="batchRunningEval || !evalCases.length" @click="runAllEvalCases">{{ batchRunningEval ? '批量评测中…' : '运行全部' }}</button><button @click="newEvalCase">＋ 新建用例</button></div></div>
    <div v-if="loadingEvalCases" class="run-center-loading"><el-icon class="is-loading"><Loading /></el-icon> 正在读取评测用例…</div>
    <div v-else class="eval-layout">
      <div class="eval-case-list">
        <button v-for="item in evalCases" :key="item.id" :class="{ active: selectedEvalCase?.id === item.id }" @click="selectEvalCase(item)"><span>{{ item.enabled ? '启用' : '停用' }}</span><strong>{{ item.name }}</strong><small v-if="item.latestRun" :class="item.latestRun.passed ? 'eval-pass' : 'eval-fail'">{{ item.latestRun.overallScore }} 分 · {{ item.latestRun.passed ? '通过' : '未通过' }}</small><small v-else>尚未运行</small></button>
        <div v-if="!evalCases.length" class="run-center-empty">还没有评测用例，创建一个固定场景开始测试。</div>
      </div>
      <div class="eval-main">
        <div v-if="editingEvalCase" class="eval-form">
          <div class="eval-section-head"><h3>{{ evalForm.id ? '编辑评测用例' : '新建评测用例' }}</h3><button @click="editingEvalCase = false">取消</button></div>
          <label><span>用例名称 *</span><input v-model="evalForm.name" maxlength="200" placeholder="例如：主角姓名不能串名" /></label>
          <label><span>输入上下文 *</span><textarea v-model="evalForm.inputContext" rows="6" maxlength="10000"></textarea></label>
          <label><span>作者续写要求</span><textarea v-model="evalForm.instruction" rows="2" maxlength="2000"></textarea></label>
          <div class="eval-form-grid"><label><span>预期出现人物</span><input v-model="evalForm.expectedCharacters" placeholder="沈舟，林晚" /></label><label><span>必须包含</span><input v-model="evalForm.requiredTerms" placeholder="黑衣人，旧邮局" /></label><label><span>禁止出现</span><input v-model="evalForm.forbiddenTerms" placeholder="顾临，未完待续" /></label><label><span>长度范围</span><div class="length-inputs"><input v-model.number="evalForm.minLength" type="number" min="20" max="300" /><i>—</i><input v-model.number="evalForm.maxLength" type="number" min="50" max="300" /></div></label></div>
          <div class="eval-form-actions"><el-button v-if="evalForm.id" type="danger" plain @click="deleteEvalCase">删除</el-button><span></span><el-button type="primary" :loading="savingEvalCase" @click="saveEvalCase">保存用例</el-button></div>
        </div>
        <template v-else-if="selectedEvalCase">
          <div class="eval-case-head"><div><span>评测用例 #{{ selectedEvalCase.id }}</span><h3>{{ selectedEvalCase.name }}</h3><p>{{ selectedEvalCase.instruction || '自由续写' }}</p></div><div><button @click="editEvalCase(selectedEvalCase)">编辑</button><button class="eval-run-button" :disabled="runningEval" @click="runEvalCase"><el-icon v-if="runningEval" class="is-loading"><Loading /></el-icon>{{ runningEval ? '评测中…' : '运行评测' }}</button></div></div>
          <div v-if="selectedEvalRun" class="eval-report">
            <div class="eval-score" :class="selectedEvalRun.passed ? 'passed' : 'failed'"><strong>{{ selectedEvalRun.overallScore ?? '—' }}</strong><span>综合得分</span><i>{{ selectedEvalRun.passed ? '通过' : selectedEvalRun.status === 'FAILED' ? '运行失败' : '未通过' }}</i></div>
            <div class="eval-metrics"><div><span>规则评分</span><strong>{{ selectedEvalRun.ruleScore ?? '—' }}</strong></div><div><span>Judge 评分</span><strong>{{ selectedEvalRun.judgeScore ?? '—' }}</strong></div><div><span>Token</span><strong>{{ selectedEvalRun.totalTokens.toLocaleString() }}</strong></div><div><span>耗时</span><strong>{{ formatDuration(selectedEvalRun.durationMs) }}</strong></div></div>
            <div v-if="selectedEvalRun.errorMessage" class="run-error"><strong>运行失败</strong><p>{{ selectedEvalRun.errorMessage }}</p></div>
            <section v-if="selectedEvalRun.generatedText"><h4>本次生成结果</h4><p class="eval-generated">{{ selectedEvalRun.generatedText }}</p></section>
            <section><h4>规则检查</h4><div v-if="selectedEvalRun.violations.length" class="eval-violations"><span v-for="item in selectedEvalRun.violations" :key="item">! {{ item }}</span></div><p v-else class="eval-clean">✓ 所有确定性规则均已通过</p></section>
            <section><h4>LLM Judge</h4><p class="eval-feedback">{{ selectedEvalRun.judgeFeedback || '没有 Judge 反馈' }}</p><small>生成 Prompt {{ selectedEvalRun.generatorPromptVersion }} · Judge Prompt {{ selectedEvalRun.judgePromptVersion }} · {{ selectedEvalRun.generatorModel }}</small></section>
          </div>
          <div v-else class="eval-empty-report">运行一次评测后，这里会显示生成内容、规则违规项和 Judge 反馈。</div>
          <div v-if="evalRuns.length" class="eval-history"><h4>历史运行</h4><button v-for="run in evalRuns" :key="run.id" :class="{ active: selectedEvalRun?.id === run.id }" @click="selectedEvalRun = run"><strong>{{ run.overallScore ?? '—' }} 分</strong><span>{{ run.generatorModel }}</span><small>{{ formatRevisionTime(run.createdAt) }}</small></button></div>
        </template>
        <div v-else class="eval-empty-report">选择左侧用例，或新建一个评测场景。</div>
      </div>
    </div>
  </el-dialog>

  <el-dialog v-model="showAccountSettings" title="账号设置" width="min(520px, calc(100vw - 28px))" class="account-settings-dialog" :close-on-click-modal="false">
    <div class="account-settings">
      <section><div><strong>个人信息</strong><p>用户名用于登录，昵称会显示在创作空间中。</p></div><label><span>用户名</span><input :value="authUser?.username" disabled /></label><label><span>昵称</span><input v-model="profileForm.nickname" maxlength="100" /></label><button :disabled="savingProfile" @click="saveProfile">{{ savingProfile ? '保存中…' : '保存昵称' }}</button></section>
      <section><div><strong>修改密码</strong><p>新密码至少 8 个字符。修改成功后需要重新登录。</p></div><label><span>当前密码</span><input v-model="passwordForm.currentPassword" type="password" autocomplete="current-password" maxlength="100" /></label><label><span>新密码</span><input v-model="passwordForm.newPassword" type="password" autocomplete="new-password" maxlength="100" /></label><label><span>确认新密码</span><input v-model="passwordForm.confirmPassword" type="password" autocomplete="new-password" maxlength="100" /></label><button class="password-change-button" :disabled="changingPassword" @click="changePassword">{{ changingPassword ? '修改中…' : '修改密码' }}</button></section>
      <section class="account-security-section"><div><strong>登录安全</strong><p>如果怀疑账号在其他设备登录，可以让所有已签发的登录凭证立即失效。</p></div><button class="revoke-sessions-button" :disabled="revokingSessions" @click="revokeAllSessions">{{ revokingSessions ? '正在退出…' : '退出所有设备' }}</button></section>
    </div>
  </el-dialog>

</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { readJsonResponse } from './api-response'

interface NovelDto {
  id: number
  title: string
  description: string | null
  outline: string | null
  totalWords: number
  version: number
  status: string
  updatedAt: string
}

interface ChapterDto {
  id: number
  novelId: number
  chapterNo: number
  title: string
  content: string
  wordCount: number
  contentVersion: number
  version: number
}

interface ChapterView {
  id: number
  chapterNo: number
  title: string
  content: string
  words: number
  contentVersion: number
  version: number
}

interface AuthUser { id: number; username: string; nickname: string; avatarUrl: string | null; lastLoginAt: string | null; createdAt: string }
interface LoginResponse { accessToken: string; expiresIn: number; user: AuthUser }

interface EditorSnapshot {
  content: string
  selectionStart: number
  selectionEnd: number
}

interface CharacterIdentityDto { id: number; identityName: string; identityType: string; firstAppearanceChapterNo: number | null; revealChapterNo: number; description: string | null }
interface CharacterDto { id: number; novelId: number; name: string; aliases: string | null; role: string | null; affiliations: string | null; description: string | null; personality: string | null; goal: string | null; currentState: string | null; relationships: string | null; identities: CharacterIdentityDto[]; version: number }
interface OrganizationDto { id: number; novelId: number; name: string; aliases: string | null; type: string | null; description: string | null; goal: string | null; structure: string | null; relationships: string | null; version: number }
interface WorldSettingDto { id: number; novelId: number; category: string; title: string; content: string; version: number }
interface StoryVolumeDto { id: number; novelId: number; volumeNo: number; title: string; chapterStart: number; chapterEnd: number | null; objective: string | null; retrospective: string | null; chapterFacts: string | null; futurePlan: string | null; keyTurningPoints: string | null; climax: string | null; endingHook: string | null; foreshadows: string | null; lockedBeats: string | null; analyzedThroughChapterNo: number | null; sourceContentVersionSum: number | null; status: 'GENERATED' | 'CONFIRMED'; stale: boolean; version: number; createdAt: string; updatedAt: string }
interface StoryVolumeContentDto { title: string; objective: string; retrospective: string; futurePlan: string; keyTurningPoints: string; climax: string; endingHook: string; foreshadows: string; lockedBeats: string }
interface StoryVolumeGenerateDto { content: StoryVolumeContentDto; analyzedThroughChapterNo: number | null; changeSummary: string[]; providerMode: string }
interface StoryVolumeForm extends StoryVolumeContentDto { id?: number; volumeNo: number; chapterStart: number; chapterEnd?: number; chapterFacts: string; analyzedThroughChapterNo?: number; status: 'GENERATED' | 'CONFIRMED'; stale?: boolean; version?: number }
interface StoryPartDto { id: number; novelId: number; volumeId: number; partNo: number; title: string; chapterStart: number; chapterEnd: number | null; objective: string | null; plan: string | null; retrospective: string | null; status: 'GENERATED' | 'CONFIRMED'; version: number; createdAt: string; updatedAt: string }
interface StoryPartForm { id?: number; partNo: number; title: string; chapterStart: number; chapterEnd?: number; objective: string; plan: string; retrospective: string; status: 'GENERATED' | 'CONFIRMED'; version?: number }
type StoryBibleType = 'CHARACTER' | 'ORGANIZATION' | 'WORLD_SETTING'
type StoryBibleAiMode = 'GENERATE' | 'EXPAND' | 'POLISH'
interface StoryBibleAssistDto { type: StoryBibleType; mode: StoryBibleAiMode; fields: Record<string, string>; changeSummary: string[]; providerMode: string }
interface StoryBibleCharacterSuggestionDto { action: 'CREATE' | 'UPDATE'; targetId: number | null; name: string; aliases: string | null; role: string | null; affiliations: string | null; description: string | null; personality: string | null; goal: string | null; currentState: string | null; relationships: string | null; rationale: string | null }
interface StoryBibleSuggestionDto extends StoryBibleCharacterSuggestionDto { type: 'CHARACTER' | 'ORGANIZATION' | 'WORLD_SETTING' | 'VOLUME'; structure: string | null; category: string | null; title: string | null; content: string | null; volumeNo: number | null; chapterStart: number | null; chapterEnd: number | null; objective: string | null; futurePlan: string | null; keyTurningPoints: string | null; climax: string | null; endingHook: string | null; foreshadows: string | null }
interface StoryBibleChatMessageDto { id: number; role: 'USER' | 'ASSISTANT'; content: string; suggestions: StoryBibleSuggestionDto[]; appliedSuggestionIndexes: number[]; recalledMessageIds: number[]; createdAt: string }
interface StoryBibleChatConversationDto { novelId: number; messages: StoryBibleChatMessageDto[] }
interface StoryBibleApplySuggestionDto { suggestionIndex: number; type: StoryBibleSuggestionDto['type']; result: Record<string, unknown>; conversation: StoryBibleChatConversationDto }
interface NovelProjectContentDto { inspiration: string; genre: string; channel: string; targetAudience: string; platform: string; coreSellingPoint: string; protagonistHook: string; growthRoute: string; readerExpectations: string; openingThreeChapters: string; expectedWords?: number; expectedVolumes?: number; chapterWordTarget?: number; description: string; outline: string }
interface NovelProjectDto { novelId: number; content: NovelProjectContentDto; novelVersion: number; profileVersion: number | null; writtenThroughChapterNo: number | null; updatedAt: string }
interface NovelProjectGenerateDto { content: NovelProjectContentDto; writtenThroughChapterNo: number | null; changeSummary: string[]; providerMode: string }
type NovelProjectChatTarget = 'DISCUSS' | 'FOUNDATION' | 'DESCRIPTION' | 'OUTLINE' | 'ALL'
interface NovelProjectChatMessageDto { id: number; role: 'USER' | 'ASSISTANT'; content: string; suggestion: NovelProjectContentDto | null; applied: boolean; analyzedThroughChapterNo: number | null; createdAt: string }
interface NovelProjectMemoryDto { confirmedFacts: string[]; characterPremises: string[]; lockedConstraints: string[]; rejectedIdeas: string[]; answeredQuestions: string[]; openQuestions: string[]; authorPreferences: string[]; sourceMessageIds: number[] }
interface NovelProjectChatConversationDto { novelId: number; messages: NovelProjectChatMessageDto[]; memory: NovelProjectMemoryDto }
interface NovelProjectChatApplyDto { project: NovelProjectDto; conversation: NovelProjectChatConversationDto }
interface ChapterPlanDto { title: string; objective: string; opening: string; developments: string[]; endingHook: string; characters: string[]; continuityNotes: string[] }
interface ChapterPlanView { title: string; objective: string; opening: string; developmentsText: string; endingHook: string; charactersText: string; continuityNotesText: string }
interface AgentStepDto { id: number; stepNo: number; type: string; status: string; summary: string; durationMs: number }
interface AiCallDto { id: number; stepType: string; model: string; status: string; promptTokens: number; completionTokens: number; totalTokens: number; durationMs: number; retryCount: number; errorMessage: string | null; createdAt: string }
interface AgentRunDto { id: number; novelId: number; chapterId: number; status: string; currentStep: string; operation: string; providerMode: string; model: string; promptTokens: number; completionTokens: number; totalTokens: number; totalDurationMs: number; retryCount: number; errorMessage: string | null; plan: ChapterPlanDto | null; draft: string | null; steps: AgentStepDto[]; calls: AiCallDto[]; createdAt: string; updatedAt: string }
interface AgentRunSummaryDto { id: number; novelId: number; chapterId: number; chapterTitle: string; operation: string; status: string; currentStep: string; providerMode: string; model: string; promptTokens: number; completionTokens: number; totalTokens: number; totalDurationMs: number; retryCount: number; errorMessage: string | null; createdAt: string }
interface DirectorMessageDto { id: number; role: 'USER' | 'ASSISTANT'; content: string; runId: number | null; plan: ChapterPlanDto | null; changeSummary: string[]; createdAt: string; pending?: boolean; failed?: boolean }
interface DirectorConversationDto { chapterId: number; messages: DirectorMessageDto[]; currentPlan: ChapterPlanDto | null; currentRunId: number | null }
interface EvalRunDto { id: number; caseId: number; status: string; generatorModel: string; generatorPromptVersion: string; judgePromptVersion: string; generatedText: string | null; ruleScore: number | null; judgeScore: number | null; overallScore: number | null; passed: boolean; violations: string[]; judgeFeedback: string | null; promptTokens: number; completionTokens: number; totalTokens: number; durationMs: number; errorMessage: string | null; createdAt: string }
interface EvalCaseDto { id: number; novelId: number; chapterId: number | null; name: string; inputContext: string; instruction: string; expectedCharacters: string; requiredTerms: string; forbiddenTerms: string; minLength: number; maxLength: number; enabled: boolean; version: number; createdAt: string; updatedAt: string; latestRun: EvalRunDto | null }
interface EvalCaseForm { id?: number; chapterId?: number; name: string; inputContext: string; instruction: string; expectedCharacters: string; requiredTerms: string; forbiddenTerms: string; minLength: number; maxLength: number; enabled: boolean; version?: number }
interface ChapterRevisionDto { id: number; novelId: number; chapterId: number; agentRunId: number; revisionType: string; instruction: string | null; originalText: string; revisedText: string; startOffset: number; endOffset: number; status: 'GENERATED' | 'ACCEPTED' | 'REJECTED' | 'ROLLED_BACK'; changeSummary: string[]; warnings: string[]; version: number; createdAt: string; updatedAt: string }
interface ConsistencyIssueDto { id: number; reportId: number; type: string; severity: 'HIGH' | 'MEDIUM' | 'LOW'; quote: string; message: string; suggestion: string; status: 'OPEN' | 'IGNORED' | 'WAITING_APPROVAL' | 'FIXED'; revisionId: number | null; version: number; createdAt: string; updatedAt: string; revision: ChapterRevisionDto | null }
interface ConsistencyReportDto { id: number; novelId: number; chapterId: number; agentRunId: number; sourceContentVersion: number; stale: boolean; score: number; summary: string; status: string; providerMode: string; issues: ConsistencyIssueDto[]; createdAt: string }
type CharacterProfileField = 'DESCRIPTION' | 'PERSONALITY' | 'GOAL' | 'AFFILIATIONS' | 'CURRENT_STATE' | 'RELATIONSHIPS'
interface CharacterProfileChangeDto { field: CharacterProfileField; operation: 'ADD' | 'REPLACE' | 'REMOVE'; oldValue: string; newValue: string; evidence: string; confidence: 'HIGH' | 'MEDIUM' | 'LOW'; applyToProfile: boolean }
interface MemoryCharacterDto { name: string; role: string; actions: string; stateChange: string; newKnowledge: string; firstAppearance: boolean; foreshadowings: string[]; profileChanges: CharacterProfileChangeDto[] }
interface CharacterHistoryDto { id: number; novelId: number; characterId: number; characterName: string; chapterId: number; chapterNo: number; sourceChapterVersion: number; actions: string | null; stateChange: string | null; newKnowledge: string | null; foreshadowings: string[]; profileChanges: CharacterProfileChangeDto[]; status: string; createdAt: string }
interface MemoryEventDto { event: string; cause: string; result: string; location: string }
interface MemoryForeshadowingDto { content: string; status: 'PLANTED' | 'RESOLVED'; importance: 'HIGH' | 'MEDIUM' | 'LOW' }
interface MemorySceneEventDto { time: string; location: string; event: string }
interface MemorySceneDto { sceneIndex: number; title: string; boundaryType: 'HARD' | 'SOFT'; continuityKey: string; timeSpan: string; locations: string[]; characters: string[]; goal: string; conflict: string; subEvents: MemorySceneEventDto[]; result: string }
interface MemoryImportantFactDto { type: 'FORESHADOWING' | 'UNRESOLVED_THREAD' | 'IMPORTANT_ITEM' | 'WORLD_RULE' | 'LOCATION_STATE' | 'RELATIONSHIP' | 'TIME_MARKER' | 'REVEAL' | 'OTHER'; content: string; importance: 'HIGH' | 'MEDIUM' | 'LOW' }
interface IdentityRevealDto { identityName: string; realCharacterName: string; evidence: string; confidence: 'HIGH' | 'MEDIUM' | 'LOW'; applyMerge: boolean }
interface ChapterMemoryContentDto { skillVersion: string; summary: string; timeInfo: string; locations: string[]; characters: MemoryCharacterDto[]; scenes: MemorySceneDto[]; importantFacts: MemoryImportantFactDto[]; keyEvents: MemoryEventDto[]; foreshadowings: MemoryForeshadowingDto[]; unresolvedQuestions: string[]; plotProgress: string; importance: 'HIGH' | 'MEDIUM' | 'LOW'; identityReveals: IdentityRevealDto[] }
interface ChapterMemoryDto { id: number; novelId: number; chapterId: number; chapterNo: number; sourceChapterVersion: number; status: 'GENERATED' | 'CONFIRMED' | 'SUPERSEDED'; content: ChapterMemoryContentDto; providerMode: string; stale: boolean; version: number; createdAt: string; updatedAt: string }
interface RagMemoryHitDto { pointId: string; score: number; chapterId: number | null; chapterOrder: number | null; memoryType: string; text: string; used: boolean }
interface RagDebugInfoDto { enabled: boolean; status: 'COMPLETED' | 'UNAVAILABLE' | 'SKIPPED'; retrievedCount: number; usedCount: number; durationMs: number; memories: RagMemoryHitDto[] }
interface CompletionDto { completion?: string; message?: string; rag?: RagDebugInfoDto }
interface WritingStyleDto { id: string; databaseId: number | null; novelId: number | null; name: string; sourceType: 'PRESET' | 'CUSTOM' | 'REFERENCE' | 'NOVEL'; description: string | null; rulesText: string; forbiddenWords: string | null; referenceExcerpt: string | null; system: boolean; editable: boolean; version: number; updatedAt: string | null }
interface WritingStyleAssistDto { name: string; description: string; rulesText: string; forbiddenWords: string; changeSummary: string[]; providerMode: string }
interface WritingStyleForm { databaseId?: number; name: string; description: string; rulesText: string; forbiddenWords: string; version?: number }

const currentView = ref<'home' | 'editor' | 'director' | 'storyBible' | 'projectPlanning'>('home')
const accessTokenKey = 'mojing-access-token'
const authToken = ref(window.localStorage.getItem(accessTokenKey) ?? window.sessionStorage.getItem(accessTokenKey) ?? '')
const authUser = ref<AuthUser>()
const authReady = ref(false)
const loggingIn = ref(false)
const registering = ref(false)
const authMode = ref<'login' | 'register'>('login')
const rememberLogin = ref(Boolean(window.localStorage.getItem(accessTokenKey)))
const showLoginPassword = ref(false)
const loginError = ref('')
const loginForm = ref({ username: window.localStorage.getItem('mojing-last-username') ?? '', password: '' })
const registerForm = ref({ username: '', nickname: '', password: '', confirmPassword: '' })
const registerError = ref('')
const showAccountSettings = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)
const revokingSessions = ref(false)
const profileForm = ref({ nickname: '' })
const passwordForm = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const storyBibleReturnView = ref<'editor' | 'director'>('editor')
const novels = ref<NovelDto[]>([])
const focusMode = ref(false)
const showMobileNav = ref(false)
const showMobileAi = ref(false)
const showExport = ref(false)
const showCreateNovel = ref(false)
const showNovelSettings = ref(false)
const showChapterMemory = ref(false)
const showRunCenter = ref(false)
const loadingRuns = ref(false)
const loadingRunDetail = ref(false)
const runSummaries = ref<AgentRunSummaryDto[]>([])
const selectedRun = ref<AgentRunDto>()
const showEvalCenter = ref(false)
const loadingEvalCases = ref(false)
const savingEvalCase = ref(false)
const runningEval = ref(false)
const batchRunningEval = ref(false)
const editingEvalCase = ref(false)
const evalCases = ref<EvalCaseDto[]>([])
const selectedEvalCase = ref<EvalCaseDto>()
const evalRuns = ref<EvalRunDto[]>([])
const selectedEvalRun = ref<EvalRunDto>()
const evalForm = ref<EvalCaseForm>({ name: '', inputContext: '', instruction: '', expectedCharacters: '', requiredTerms: '', forbiddenTerms: '顾临，未完待续', minLength: 50, maxLength: 160, enabled: true })
const creatingNovel = ref(false)
const savingNovelSettings = ref(false)
const loadingProject = ref(false)
const savingProject = ref(false)
const generatingProject = ref<'' | 'FOUNDATION' | 'DESCRIPTION' | 'OUTLINE' | 'ALL'>('')
const projectInstruction = ref('')
const projectWrittenThrough = ref<number>()
const projectNovelVersion = ref(0)
const projectProfileVersion = ref<number>()
const projectSaveState = ref<'idle' | 'pending' | 'saving' | 'saved' | 'failed' | 'draft'>('idle')
const projectAiDraftPending = ref(false)
const projectForm = ref<NovelProjectContentDto>({ inspiration: '', genre: '', channel: '', targetAudience: '', platform: '', coreSellingPoint: '', protagonistHook: '', growthRoute: '', readerExpectations: '', openingThreeChapters: '', description: '', outline: '' })
const projectChatMessages = ref<NovelProjectChatMessageDto[]>([])
const projectMemory = ref<NovelProjectMemoryDto>({ confirmedFacts: [], characterPremises: [], lockedConstraints: [], rejectedIdeas: [], answeredQuestions: [], openQuestions: [], authorPreferences: [], sourceMessageIds: [] })
const projectChatInput = ref('')
const loadingProjectChat = ref(false)
const sendingProjectChat = ref(false)
const applyingProjectSuggestion = ref<number>()
const projectImportWrittenStory = ref(false)
const projectChatMessagesRef = ref<HTMLElement>()
type ProjectSettingsTab = 'POSITIONING' | 'ENGINE' | 'OPENING' | 'MATERIALS' | 'STYLE' | 'MEMORY'
const projectSettingsTab = ref<ProjectSettingsTab>('POSITIONING')
const projectSettingTabs: Array<{ value: ProjectSettingsTab; label: string }> = [
  { value: 'POSITIONING', label: '作品定位' }, { value: 'ENGINE', label: '故事引擎' },
  { value: 'OPENING', label: '篇幅开篇' }, { value: 'MATERIALS', label: '简介总纲' }, { value: 'STYLE', label: '作品风格' }, { value: 'MEMORY', label: '讨论记忆' },
]
const newNovelForm = ref({ title: '', inspiration: '' })
const novelSettingsForm = ref({ title: '', description: '', outline: '', version: 0 })
const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const leftWidth = ref(232)
const rightWidth = ref(320)
const viewportWidth = ref(window.innerWidth)
const activeChapter = ref(0)
const saveState = ref('正在加载…')
const loadingWorkspace = ref(true)
const creatingChapter = ref(false)
const deletingChapterId = ref<number>()
const novelId = ref<number>()
const novelTitle = ref('雾港来信')
const generateLength = ref('标准')
const fontSize = ref(16)
const prompt = ref('')
const result = ref('')
const ragDebug = ref<RagDebugInfoDto>()
const generating = ref(false)
const aiMode = ref<'agent' | 'rewrite' | 'consistency' | 'organize' | 'quick'>('agent')
const agentGuidance = ref('')
const directorThinkingEnabled = ref(false)
const directorGeneratingPlan = ref(false)
const planning = ref(false)
const drafting = ref(false)
const agentRunId = ref<number>()
const agentPlan = ref<ChapterPlanView>()
const directorPlanSaveState = ref<'idle' | 'pending' | 'saving' | 'saved' | 'failed'>('idle')
const agentDraft = ref('')
const agentSteps = ref<AgentStepDto[]>([])
const directorMessages = ref<DirectorMessageDto[]>([])
const directorMessagesContainer = ref<HTMLElement>()
const loadingDirector = ref(false)
const directorError = ref('')
const showDirectorRag = ref(false)
const loadingDirectorRag = ref(false)
const directorRagDebug = ref<RagDebugInfoDto>()
const showStyleManager = ref(false)
const writingStyles = ref<WritingStyleDto[]>([])
const selectedWritingStyleId = ref('preset:NATURAL_WEB')
const styleAssistMode = ref<'ORGANIZE' | 'REFERENCE' | 'NOVEL'>('ORGANIZE')
const styleReferenceInput = ref('')
const assistingStyle = ref(false)
const savingStyle = ref(false)
const styleForm = ref<WritingStyleForm>({ name: '', description: '', rulesText: '', forbiddenWords: '' })
const draftLength = ref<number | 'custom'>(1000)
const customDraftLength = ref(1500)
const selectionStart = ref(0)
const selectionEnd = ref(0)
const selectedText = ref('')
const rewriteMode = ref('POLISH')
const rewriteInstruction = ref('')
const rewriteThinkingEnabled = ref(false)
const rewriting = ref(false)
const decidingRevision = ref(false)
const rewriteResult = ref<ChapterRevisionDto>()
const revisionHistory = ref<ChapterRevisionDto[]>([])
const consistencyFocus = ref('')
const checkingConsistency = ref(false)
const consistencyReport = ref<ConsistencyReportDto>()
const consistencyReports = ref<ConsistencyReportDto[]>([])
const decidingIssueId = ref<number>()
const chapterMemories = ref<Record<number, ChapterMemoryDto>>({})
const chapterMemory = ref<ChapterMemoryDto>()
const memoryForm = ref<ChapterMemoryContentDto>()
const memoryInstruction = ref('')
const memoryThinkingEnabled = ref(false)
const loadingMemory = ref(false)
const extractingMemory = ref(false)
const confirmingMemory = ref(false)
const organizingChapter = ref(false)
const memoryLocationsText = ref('')
const memoryQuestionsText = ref('')
const showBibleLoading = ref(false)
const savingBible = ref(false)
const generatingVolume = ref(false)
const volumeAiInstruction = ref('')
const assistingBible = ref<StoryBibleType>()
const bibleAiMode = ref<StoryBibleAiMode>('GENERATE')
const bibleAiInstruction = ref('')
const bibleAiTarget = ref('AUTO')
const bibleTab = ref('foundation')
const characters = ref<CharacterDto[]>([])
const characterHistory = ref<CharacterHistoryDto[]>([])
const loadingCharacterHistory = ref(false)
const organizations = ref<OrganizationDto[]>([])
const worldSettings = ref<WorldSettingDto[]>([])
const storyVolumes = ref<StoryVolumeDto[]>([])
const storyParts = ref<StoryPartDto[]>([])
const outlineNodeType = ref<'volume' | 'part' | 'chapter'>('volume')
const selectedOutlineChapter = ref<ChapterView>()
const storyBibleChatMessages = ref<StoryBibleChatMessageDto[]>([])
const storyBibleChatInput = ref('')
const loadingStoryBibleChat = ref(false)
const sendingStoryBibleChat = ref(false)
const applyingStoryBibleSuggestion = ref('')
const storyBibleChatMessagesRef = ref<HTMLElement>()
const characterForm = ref<{ id?: number; name: string; aliases: string; role: string; affiliations: string; description: string; personality: string; goal: string; currentState: string; relationships: string; version?: number }>({ name: '', aliases: '', role: '', affiliations: '', description: '', personality: '', goal: '', currentState: '', relationships: '' })
const organizationForm = ref<{ id?: number; name: string; aliases: string; type: string; description: string; goal: string; structure: string; relationships: string; version?: number }>({ name: '', aliases: '', type: '', description: '', goal: '', structure: '', relationships: '' })
const worldSettingForm = ref<{ id?: number; category: string; title: string; content: string; version?: number }>({ category: '', title: '', content: '' })
const storyVolumeForm = ref<StoryVolumeForm>({ volumeNo: 1, title: '', chapterStart: 1, objective: '', retrospective: '', chapterFacts: '', futurePlan: '', keyTurningPoints: '', climax: '', endingHook: '', foreshadows: '', lockedBeats: '', status: 'GENERATED' })
const storyPartForm = ref<StoryPartForm>({ partNo: 1, title: '', chapterStart: 1, objective: '', plan: '', retrospective: '', status: 'GENERATED' })
const savingStoryPart = ref(false)
const editorRef = ref<HTMLTextAreaElement>()
const paperRef = ref<HTMLElement>()
const directorDraftRef = ref<HTMLElement>()
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const rewriteContextChars = 1500

const chapters = ref<ChapterView[]>([])
const selectedPartChapters = computed(() => {
  const start = storyPartForm.value.chapterStart
  const end = storyPartForm.value.chapterEnd ?? Number.MAX_SAFE_INTEGER
  return chapters.value.filter(chapter => chapter.chapterNo >= start && chapter.chapterNo <= end)
})
const chapterTitle = ref('')
const editorContent = ref('')
const inlineCompletionEnabled = ref(window.localStorage.getItem('mojing-inline-completion-enabled') !== 'false')
const inlineCompletion = ref('')
const inlineCompletionLoading = ref(false)
const inlineCompletionOffset = ref(0)
const undoStack = ref<EditorSnapshot[]>([])
const redoStack = ref<EditorSnapshot[]>([])
const pendingHistoryStart = ref<EditorSnapshot>()
const lastEditorSnapshot = ref<EditorSnapshot>({ content: '', selectionStart: 0, selectionEnd: 0 })

const lengths = ['简短', '标准', '详细']
const draftLengths = [{ label: '600 字', value: 600 }, { label: '1000 字', value: 1000 }, { label: '2000 字', value: 2000 }, { label: '5000 字', value: 5000 }]
const bibleAiModes: { value: StoryBibleAiMode; label: string }[] = [{ value: 'GENERATE', label: '智能补全' }, { value: 'EXPAND', label: '扩写' }, { value: 'POLISH', label: '润色' }]
const storyBibleFieldOptions: Record<StoryBibleType, { value: string; label: string }[]> = {
  CHARACTER: [{ value: 'name', label: '人物姓名' }, { value: 'aliases', label: '别名/简称' }, { value: 'role', label: '角色定位' }, { value: 'affiliations', label: '所属组织' }, { value: 'description', label: '身份与经历' }, { value: 'personality', label: '性格特征' }, { value: 'goal', label: '目标与动机' }, { value: 'currentState', label: '当前状态' }, { value: 'relationships', label: '当前关系' }],
  ORGANIZATION: [{ value: 'name', label: '组织名称' }, { value: 'type', label: '组织类型' }, { value: 'aliases', label: '别名' }, { value: 'description', label: '组织概况' }, { value: 'goal', label: '目标与立场' }, { value: 'structure', label: '结构与重要成员' }, { value: 'relationships', label: '与其他势力的关系' }],
  WORLD_SETTING: [{ value: 'category', label: '分类' }, { value: 'title', label: '设定标题' }, { value: 'content', label: '详细内容' }],
}
const rewriteModes = [
  { value: 'POLISH', label: '润色', help: '改善表达，不改剧情' },
  { value: 'REWRITE', label: '重写', help: '保留事实，重新表达' },
  { value: 'EXPAND', label: '扩写', help: '增加动作与细节' },
  { value: 'SHORTEN', label: '缩写', help: '压缩冗余内容' },
  { value: 'TONE', label: '语气', help: '调整氛围与节奏' },
  { value: 'CUSTOM', label: '自定义', help: '完全按要求修改' },
]
const fontSizes = [14, 16, 18, 20, 22]
const currentWords = computed(() => editorContent.value.replace(/\s/g, '').length)
const inlineCompletionInsertText = computed(() => {
  if (!inlineCompletion.value) return ''
  const before = editorContent.value.slice(0, inlineCompletionOffset.value)
  const separator = before && /[。！？.!?]\s*$/.test(before) && !/\n\s*$/.test(before) ? '\n\n' : ''
  return `${separator}${inlineCompletion.value}`
})
const canUndo = computed(() => undoStack.value.length > 0 || Boolean(pendingHistoryStart.value))
const canRedo = computed(() => redoStack.value.length > 0)
const directorPlanSaveLabel = computed(() => ({
  idle: '未修改', pending: '待保存', saving: '保存中…', saved: '已保存', failed: '保存失败',
})[directorPlanSaveState.value])
const projectSaveLabel = computed(() => ({
  idle: '尚未修改', pending: '修改待保存', saving: '正在自动保存…', saved: '所有修改已保存',
  failed: '自动保存失败，请点击下方按钮重试', draft: 'AI 草案尚未保存',
})[projectSaveState.value])
const projectMemoryGroups = computed(() => [
  { label: '已确认设定', items: projectMemory.value.confirmedFacts },
  { label: '人物前提', items: projectMemory.value.characterPremises },
  { label: '作者锁定', items: projectMemory.value.lockedConstraints },
  { label: '明确否决', items: projectMemory.value.rejectedIdeas },
  { label: '已回答问题', items: projectMemory.value.answeredQuestions },
  { label: '待确认问题', items: projectMemory.value.openQuestions },
  { label: '作者偏好', items: projectMemory.value.authorPreferences },
])
const draftTargetLength = computed(() => draftLength.value === 'custom'
  ? Math.min(5000, Math.max(300, Math.round(Number(customDraftLength.value) || 1000)))
  : draftLength.value)
const privateWritingStyles = computed(() => writingStyles.value.filter(style => !style.system))
const bibleAiModeLabel = computed(() => bibleAiModes.find(item => item.value === bibleAiMode.value)?.label ?? '完善')
const currentBibleFieldOptions = computed(() => storyBibleFieldOptions[
  bibleTab.value === 'characters' ? 'CHARACTER' : bibleTab.value === 'organizations' ? 'ORGANIZATION' : 'WORLD_SETTING'
])
const storyBibleChatFocus = computed(() => {
  if (bibleTab.value === 'volumes' && storyVolumeForm.value.id)
    return { type: 'VOLUME', id: storyVolumeForm.value.id, label: `第${storyVolumeForm.value.volumeNo}卷《${storyVolumeForm.value.title}》` }
  if (bibleTab.value === 'characters' && characterForm.value.id)
    return { type: 'CHARACTER', id: characterForm.value.id, label: characterForm.value.name }
  if (bibleTab.value === 'organizations' && organizationForm.value.id)
    return { type: 'ORGANIZATION', id: organizationForm.value.id, label: organizationForm.value.name }
  if (bibleTab.value === 'world' && worldSettingForm.value.id)
    return { type: 'WORLD_SETTING', id: worldSettingForm.value.id, label: worldSettingForm.value.title }
  return { type: undefined, id: undefined, label: '整本作品' }
})
const totalWords = computed(() => chapters.value.reduce((sum, chapter) => sum + chapter.words, 0))
const homeTotalWords = computed(() => novels.value.reduce((sum, novel) => sum + novel.totalWords, 0))
const readingMinutes = computed(() => Math.max(1, Math.ceil(currentWords.value / 500)))
const isMobile = computed(() => viewportWidth.value <= 900)
const activeMemory = computed(() => {
  const chapter = chapters.value[activeChapter.value]
  return chapter ? chapterMemories.value[chapter.id] : undefined
})
const latestConsistencyStatus = computed(() => {
  const report = consistencyReports.value[0]
  if (!report) return '尚未检查'
  if (report.stale) return '正文已变化，之前的报告已过期'
  const open = report.issues.filter(issue => issue.status === 'OPEN' || issue.status === 'WAITING_APPROVAL').length
  return open ? `当前报告有 ${open} 个待处理问题` : `当前报告 ${report.score} 分，没有待处理问题`
})
const activeMemoryLabel = computed(() => !activeMemory.value ? '尚未提取' : activeMemory.value.stale ? '正文已变化，待更新' : '已确认，可用于长篇上下文')
const memoryStatusText = computed(() => !chapterMemory.value ? '尚未提取' : chapterMemory.value.stale ? '记忆已过期' : chapterMemory.value.status === 'CONFIRMED' ? '已确认' : '等待确认')
const memoryStatusClass = computed(() => !chapterMemory.value ? 'memory-none' : chapterMemory.value.stale ? 'memory-stale' : chapterMemory.value.status === 'CONFIRMED' ? 'memory-confirmed' : 'memory-generated')
const runTotalTokens = computed(() => runSummaries.value.reduce((sum, run) => sum + (run.totalTokens || 0), 0))
const failedRunCount = computed(() => runSummaries.value.filter(run => run.status === 'FAILED').length)
const workspaceColumns = computed(() => {
  if (focusMode.value || isMobile.value) return 'minmax(0, 1fr)'
  const compact = viewportWidth.value <= 1100
  const sideBudget = Math.max(400, viewportWidth.value - 492)
  const desiredLeft = compact ? Math.min(leftWidth.value, 210) : leftWidth.value
  const desiredRight = compact ? Math.min(rightWidth.value, 280) : rightWidth.value
  const desiredTotal = (leftCollapsed.value ? 0 : desiredLeft) + (rightCollapsed.value ? 0 : desiredRight)
  const scale = Math.min(1, sideBudget / Math.max(1, desiredTotal))
  const visibleLeftWidth = Math.floor(desiredLeft * scale)
  const visibleRightWidth = Math.floor(desiredRight * scale)
  const columns: string[] = []
  if (!leftCollapsed.value) columns.push(`${visibleLeftWidth}px`, '6px')
  columns.push('minmax(300px, 1fr)')
  if (!rightCollapsed.value) columns.push('6px', `${visibleRightWidth}px`)
  return columns.join(' ')
})

let saveTimer: number | undefined
let historyTimer: number | undefined
let directorPlanSaveTimer: number | undefined
let projectSaveTimer: number | undefined
let paneResizeUnlockTimer: number | undefined
let inlineCompletionTimer: number | undefined
let inlineCompletionController: AbortController | undefined
let inlineCompletionRequestId = 0
let editorComposing = false
let suppressNextInlineSchedule = false
let pendingDirectorMessageId = -1
let paneResizing = false
let dirty = false
let savingPromise: Promise<boolean> | undefined
let directorPlanDirty = false
let directorPlanSavingPromise: Promise<boolean> | undefined
let projectDirty = false
let projectEditRevision = 0
let projectSavingPromise: Promise<boolean> | undefined

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers)
  headers.set('Content-Type', 'application/json')
  if (authToken.value) headers.set('Authorization', `Bearer ${authToken.value}`)
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  })
  if (response.status === 401 && path !== '/api/auth/login') clearAuth()
  return readJsonResponse<T>(response)
}

async function requestEventStream(path: string, init: RequestInit,
                                  onEvent: (event: string, data: unknown) => void): Promise<void> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('Accept', 'text/event-stream')
  if (authToken.value) headers.set('Authorization', `Bearer ${authToken.value}`)
  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  })
  if (!response.ok) {
    if (response.status === 401) clearAuth()
    await readJsonResponse<unknown>(response)
  }
  if (!response.body) throw new Error('浏览器没有收到流式响应体')
  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  const consumeFrame = (frame: string) => {
    let event = 'message'
    const dataLines: string[] = []
    for (const line of frame.split('\n')) {
      if (line.startsWith('event:')) event = line.slice(6).trim()
      else if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart())
    }
    if (!dataLines.length) return
    const raw = dataLines.join('\n')
    let data: unknown = raw
    try { data = JSON.parse(raw) } catch { /* 允许纯文本事件 */ }
    onEvent(event, data)
  }
  try {
    while (true) {
      const { value, done } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      buffer = buffer.replaceAll('\r\n', '\n')
      let boundary = buffer.indexOf('\n\n')
      while (boundary >= 0) {
        consumeFrame(buffer.slice(0, boundary))
        buffer = buffer.slice(boundary + 2)
        boundary = buffer.indexOf('\n\n')
      }
      if (done) break
    }
    if (buffer.trim()) consumeFrame(buffer)
  } catch (error) {
    await reader.cancel().catch(() => undefined)
    throw error
  } finally {
    reader.releaseLock()
  }
}

function clearAuth() {
  authToken.value = ''
  authUser.value = undefined
  window.localStorage.removeItem(accessTokenKey)
  window.sessionStorage.removeItem(accessTokenKey)
}

async function restoreAuth() {
  if (!authToken.value) { authReady.value = true; return }
  try { authUser.value = await requestJson<AuthUser>('/api/auth/me') }
  catch { clearAuth() }
  finally { authReady.value = true }
}

async function login() {
  if (loggingIn.value) return
  const username = loginForm.value.username.trim()
  if (!username || !loginForm.value.password) { ElMessage.warning('请输入用户名和密码'); return }
  loginError.value = ''
  loggingIn.value = true
  try {
    const response = await requestJson<LoginResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify({ username, password: loginForm.value.password }) })
    acceptAuth(response)
    loginForm.value.password = ''
    await initializeWorkspace()
    ElMessage.success(`欢迎回来，${response.user.nickname}`)
  } catch (error) { loginError.value = error instanceof Error ? error.message : '登录失败' }
  finally { loggingIn.value = false }
}

function acceptAuth(response: LoginResponse) {
  authToken.value = response.accessToken
  authUser.value = response.user
  window.localStorage.setItem('mojing-last-username', response.user.username)
  window.localStorage.removeItem(accessTokenKey)
  window.sessionStorage.removeItem(accessTokenKey)
  const tokenStorage = rememberLogin.value ? window.localStorage : window.sessionStorage
  tokenStorage.setItem(accessTokenKey, response.accessToken)
}

function switchAuthMode() {
  authMode.value = authMode.value === 'login' ? 'register' : 'login'
  loginError.value = ''
  registerError.value = ''
  registerForm.value.password = ''
  registerForm.value.confirmPassword = ''
}

async function register() {
  if (registering.value) return
  const username = registerForm.value.username.trim()
  const nickname = registerForm.value.nickname.trim()
  const { password, confirmPassword } = registerForm.value
  if (!/^[A-Za-z][A-Za-z0-9_]{2,49}$/.test(username)) { registerError.value = '用户名需为3到50位，以字母开头，仅含字母、数字和下划线'; return }
  if (!nickname) { registerError.value = '请输入昵称'; return }
  if (password.length < 8) { registerError.value = '密码至少需要 8 个字符'; return }
  if (password !== confirmPassword) { registerError.value = '两次输入的密码不一致'; return }
  registerError.value = ''
  registering.value = true
  try {
    const response = await requestJson<LoginResponse>('/api/auth/register', { method: 'POST', body: JSON.stringify({ username, nickname, password }) })
    acceptAuth(response)
    registerForm.value = { username: '', nickname: '', password: '', confirmPassword: '' }
    await initializeWorkspace()
    ElMessage.success(`欢迎加入墨境，${response.user.nickname}`)
  } catch (error) { registerError.value = error instanceof Error ? error.message : '注册失败' }
  finally { registering.value = false }
}

function logout() {
  clearAuth()
  currentView.value = 'home'
  novels.value = []
  chapters.value = []
  showAccountSettings.value = false
}

function handleAccountCommand(command: string) {
  if (command === 'logout') { logout(); return }
  if (command === 'settings') {
    profileForm.value.nickname = authUser.value?.nickname ?? ''
    passwordForm.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    showAccountSettings.value = true
  }
}

async function saveProfile() {
  const nickname = profileForm.value.nickname.trim()
  if (!nickname) { ElMessage.warning('请输入昵称'); return }
  savingProfile.value = true
  try {
    authUser.value = await requestJson<AuthUser>('/api/auth/me', { method: 'PATCH', body: JSON.stringify({ nickname }) })
    ElMessage.success('昵称已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '保存失败') }
  finally { savingProfile.value = false }
}

async function changePassword() {
  const { currentPassword, newPassword, confirmPassword } = passwordForm.value
  if (!currentPassword || !newPassword || !confirmPassword) { ElMessage.warning('请完整填写密码信息'); return }
  if (newPassword.length < 8) { ElMessage.warning('新密码至少需要 8 个字符'); return }
  if (newPassword !== confirmPassword) { ElMessage.warning('两次输入的新密码不一致'); return }
  changingPassword.value = true
  try {
    await requestJson<void>('/api/auth/password', { method: 'PUT', body: JSON.stringify({ currentPassword, newPassword }) })
    logout()
    loginForm.value.password = ''
    ElMessage.success('密码修改成功，请重新登录')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '密码修改失败') }
  finally { changingPassword.value = false }
}

async function revokeAllSessions() {
  try { await ElMessageBox.confirm('这会让当前设备和其他设备的登录全部失效，是否继续？', '退出所有设备', { confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning' }) }
  catch { return }
  revokingSessions.value = true
  try {
    await requestJson<void>('/api/auth/sessions/revoke', { method: 'POST' })
    logout()
    ElMessage.success('所有设备已退出，请重新登录')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '操作失败') }
  finally { revokingSessions.value = false }
}

function toChapterView(chapter: ChapterDto): ChapterView {
  return {
    id: chapter.id,
    chapterNo: chapter.chapterNo,
    title: chapter.title,
    content: chapter.content,
    words: chapter.wordCount,
    contentVersion: chapter.contentVersion,
    version: chapter.version,
  }
}

async function initializeWorkspace() {
  loadingWorkspace.value = true
  saveState.value = '正在加载…'
  try {
    novels.value = await requestJson<NovelDto[]>('/api/novels')
    saveState.value = '已保存'
  } catch (error) {
    saveState.value = '加载失败'
    ElMessage.error(error instanceof Error ? error.message : '小说加载失败')
  } finally {
    loadingWorkspace.value = false
  }
}

function openCreateNovel() {
  newNovelForm.value = { title: '', inspiration: '' }
  showCreateNovel.value = true
}

async function createNovel() {
  const title = newNovelForm.value.title.trim()
  if (!title) {
    ElMessage.warning('请填写小说名称')
    return
  }
  creatingNovel.value = true
  try {
    const inspiration = newNovelForm.value.inspiration.trim()
    const created = await requestJson<NovelDto>('/api/novels', {
      method: 'POST',
      body: JSON.stringify({ title, description: '', outline: '' }),
    })
    novels.value.unshift(created)
    showCreateNovel.value = false
    await openNovel(created)
    await openProjectPlanning()
    projectForm.value.inspiration = inspiration
    if (inspiration) scheduleProjectSave()
    ElMessage.success('小说创建成功，从一句灵感开始完成立项吧')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '小说创建失败')
  } finally {
    creatingNovel.value = false
  }
}

function openNovelSettings() {
  const novel = novels.value.find(item => item.id === novelId.value)
  if (!novel) return
  novelSettingsForm.value = {
    title: novel.title,
    description: novel.description ?? '',
    outline: novel.outline ?? '',
    version: novel.version,
  }
  showNovelSettings.value = true
}

async function saveNovelSettings() {
  if (!novelId.value || !await flushSave()) return
  const title = novelSettingsForm.value.title.trim()
  if (!title) {
    ElMessage.warning('请填写小说名称')
    return
  }
  savingNovelSettings.value = true
  try {
    const saved = await requestJson<NovelDto>(`/api/novels/${novelId.value}`, {
      method: 'PATCH',
      body: JSON.stringify({
        title,
        description: novelSettingsForm.value.description.trim(),
        outline: novelSettingsForm.value.outline.trim(),
        version: novelSettingsForm.value.version,
      }),
    })
    const index = novels.value.findIndex(item => item.id === saved.id)
    if (index >= 0) novels.value[index] = saved
    novelTitle.value = saved.title
    novelSettingsForm.value.version = saved.version
    showNovelSettings.value = false
    ElMessage.success('作品设置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '作品设置保存失败')
  } finally {
    savingNovelSettings.value = false
  }
}

function normalizeProjectContent(content?: Partial<NovelProjectContentDto>): NovelProjectContentDto {
  return {
    inspiration: content?.inspiration ?? '', genre: content?.genre ?? '', channel: content?.channel ?? '',
    targetAudience: content?.targetAudience ?? '', platform: content?.platform ?? '',
    coreSellingPoint: content?.coreSellingPoint ?? '', protagonistHook: content?.protagonistHook ?? '',
    growthRoute: content?.growthRoute ?? '', readerExpectations: content?.readerExpectations ?? '',
    openingThreeChapters: content?.openingThreeChapters ?? '', expectedWords: content?.expectedWords ?? undefined,
    expectedVolumes: content?.expectedVolumes ?? undefined, chapterWordTarget: content?.chapterWordTarget ?? undefined,
    description: content?.description ?? '', outline: content?.outline ?? '',
  }
}

async function openProjectPlanning() {
  if (!novelId.value || !await flushSave()) return
  dismissInlineCompletion()
  currentView.value = 'projectPlanning'
  loadingProject.value = true
  window.clearTimeout(projectSaveTimer)
  projectDirty = false
  projectAiDraftPending.value = false
  projectSaveState.value = 'idle'
  projectInstruction.value = ''
  projectImportWrittenStory.value = false
  projectChatInput.value = ''
  projectSettingsTab.value = 'POSITIONING'
  try {
    const response = await requestJson<NovelProjectDto>(`/api/novels/${novelId.value}/project`)
    applyProjectResponse(response)
    projectSaveState.value = 'saved'
    await loadProjectChat()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '立项资料读取失败')
  } finally { loadingProject.value = false }
}

function applyProjectResponse(response: NovelProjectDto) {
  projectForm.value = normalizeProjectContent(response.content)
  projectNovelVersion.value = response.novelVersion
  projectProfileVersion.value = response.profileVersion ?? undefined
  projectWrittenThrough.value = response.writtenThroughChapterNo ?? undefined
  novels.value = novels.value.map(item => item.id === novelId.value
    ? { ...item, description: response.content.description || null,
        outline: response.content.outline || null, version: response.novelVersion }
    : item)
  novelSettingsForm.value.description = response.content.description
  novelSettingsForm.value.outline = response.content.outline
  novelSettingsForm.value.version = response.novelVersion
}

async function loadProjectChat(showError = true) {
  if (!novelId.value) return
  loadingProjectChat.value = true
  try {
    const response = await requestJson<NovelProjectChatConversationDto>(`/api/novels/${novelId.value}/project/chat`)
    projectChatMessages.value = response.messages
    projectMemory.value = response.memory
    scrollProjectChatToBottom()
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '立项对话读取失败')
  } finally { loadingProjectChat.value = false }
}

function scrollProjectChatToBottom() {
  void nextTick(() => {
    const element = projectChatMessagesRef.value
    if (element) element.scrollTop = element.scrollHeight
  })
}

const projectChatDefaults: Record<Exclude<NovelProjectChatTarget, 'DISCUSS'>, string> = {
  FOUNDATION: '请基于现有讨论，直接整理题材基调、主角特点、核心卖点和主线方向，给出可应用的简明立项草案。未定细节留到大纲和章节阶段再补。',
  DESCRIPTION: '请基于我们的讨论生成或修改小说简介，并给出可应用的立项方案。',
  OUTLINE: '请基于现有讨论，直接生成或修改故事总纲，只交代起点、主线目标和大致推进方向，具体事件以后再细化，并给出可应用的立项草案。',
  ALL: '请综合现有讨论，直接整理一份能开始写作的简明立项草案，不需要提前定完全部设定，未定细节保持弹性。',
}

async function sendProjectChat(target: NovelProjectChatTarget = 'DISCUSS') {
  if (!novelId.value || sendingProjectChat.value) return
  const message = projectChatInput.value.trim() || (target === 'DISCUSS' ? '' : projectChatDefaults[target])
  if (!message) { ElMessage.warning('请先告诉立项顾问你想讨论什么'); return }
  if (!await flushProjectSave()) return
  sendingProjectChat.value = true
  try {
    const response = await requestJson<NovelProjectChatConversationDto>(`/api/novels/${novelId.value}/project/chat/messages`, {
      method: 'POST', body: JSON.stringify({ message, target,
        importWrittenStory: Boolean(projectWrittenThrough.value && projectImportWrittenStory.value), current: projectForm.value }),
    })
    projectChatMessages.value = response.messages
    projectMemory.value = response.memory
    projectChatInput.value = ''
    scrollProjectChatToBottom()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '立项顾问暂时无法回复') }
  finally { sendingProjectChat.value = false }
}

async function applyProjectChatSuggestion(message: NovelProjectChatMessageDto) {
  if (!novelId.value || !message.suggestion || applyingProjectSuggestion.value) return
  if (!await flushProjectSave()) return
  applyingProjectSuggestion.value = message.id
  try {
    const response = await requestJson<NovelProjectChatApplyDto>(`/api/novels/${novelId.value}/project/chat/messages/${message.id}/apply`, { method: 'POST' })
    applyProjectResponse(response.project)
    projectChatMessages.value = response.conversation.messages
    projectMemory.value = response.conversation.memory
    projectAiDraftPending.value = false
    projectSaveState.value = 'saved'
    ElMessage.success('已将 AI 方案应用到正式立项资料')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '立项方案应用失败') }
  finally { applyingProjectSuggestion.value = undefined }
}

function projectSuggestionHighlights(suggestion: NovelProjectContentDto) {
  const fields: Array<[string, string | number | undefined]> = [
    ['题材 / 频道', [suggestion.genre, suggestion.channel].filter(Boolean).join(' · ')],
    ['核心卖点', suggestion.coreSellingPoint], ['主角机制', suggestion.protagonistHook],
    ['升级路线', suggestion.growthRoute], ['开篇三章', suggestion.openingThreeChapters],
    ['小说简介', suggestion.description], ['故事总纲', suggestion.outline],
  ]
  return fields.filter(([, value]) => value !== undefined && String(value).trim()).map(([label, value]) => ({ label, value: String(value) }))
}

async function closeProjectPlanning() {
  if (!await flushProjectSave()) return
  if (projectAiDraftPending.value) {
    try {
      await ElMessageBox.confirm('AI 生成的立项草案还没有保存，是否确认并保存后返回正文？', 'AI 草案尚未确认', {
        type: 'warning', confirmButtonText: '保存并返回', cancelButtonText: '放弃草案', distinguishCancelAndClose: true,
      })
      if (!await persistProject(true)) return
    } catch (action) {
      if (action === 'close') return
    }
  }
  currentView.value = 'editor'
  await nextTick()
  resizeEditor()
}

async function generateProject(target: 'FOUNDATION' | 'DESCRIPTION' | 'OUTLINE' | 'ALL') {
  if (!novelId.value || generatingProject.value) return
  const hasSeed = projectInstruction.value.trim() || projectForm.value.inspiration.trim()
    || projectForm.value.coreSellingPoint.trim() || projectForm.value.description.trim()
    || projectForm.value.outline.trim() || projectWrittenThrough.value
  if (!hasSeed) { ElMessage.warning('请先写下一句灵感'); return }
  if (!await flushProjectSave()) return
  generatingProject.value = target
  try {
    const response = await requestJson<NovelProjectGenerateDto>(`/api/novels/${novelId.value}/project/generate`, {
      method: 'POST', body: JSON.stringify({ target, instruction: projectInstruction.value.trim(), current: projectForm.value }),
    })
    projectForm.value = normalizeProjectContent(response.content)
    projectWrittenThrough.value = response.writtenThroughChapterNo ?? undefined
    projectAiDraftPending.value = true
    projectSaveState.value = 'draft'
    const summary = response.changeSummary.filter(Boolean).slice(0, 3).join('；') || 'AI 已生成立项草案，请检查后保存'
    ElMessage.success(summary)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : 'AI 生成立项方案失败') }
  finally { generatingProject.value = '' }
}

async function saveProject() {
  await persistProject(true)
}

function scheduleProjectSave() {
  if (loadingProject.value) return
  projectDirty = true
  projectEditRevision++
  projectSaveState.value = 'pending'
  window.clearTimeout(projectSaveTimer)
  projectSaveTimer = window.setTimeout(() => { void flushProjectSave() }, 1000)
}

async function flushProjectSave(): Promise<boolean> {
  window.clearTimeout(projectSaveTimer)
  if (projectSavingPromise) {
    const saved = await projectSavingPromise
    return saved && (!projectDirty || await flushProjectSave())
  }
  if (!projectDirty) return true
  return persistProject(false)
}

async function persistProject(force: boolean): Promise<boolean> {
  if (!novelId.value) return false
  if (projectSavingPromise) {
    const saved = await projectSavingPromise
    return saved && (!projectDirty || await persistProject(false))
  }
  if (!force && !projectDirty) return true
  window.clearTimeout(projectSaveTimer)
  const revision = projectEditRevision
  const content = JSON.parse(JSON.stringify(projectForm.value)) as NovelProjectContentDto
  projectDirty = false
  savingProject.value = true
  projectSaveState.value = 'saving'
  let saveFailed = false
  projectSavingPromise = (async () => {
  try {
    const response = await requestJson<NovelProjectDto>(`/api/novels/${novelId.value}/project`, {
      method: 'PUT', body: JSON.stringify({ content,
        novelVersion: projectNovelVersion.value, profileVersion: projectProfileVersion.value ?? null }),
    })
    projectNovelVersion.value = response.novelVersion
    projectProfileVersion.value = response.profileVersion ?? undefined
    projectWrittenThrough.value = response.writtenThroughChapterNo ?? undefined
    if (revision === projectEditRevision) projectForm.value = normalizeProjectContent(response.content)
    novels.value = novels.value.map(item => item.id === novelId.value
      ? { ...item, description: response.content.description || null,
          outline: response.content.outline || null, version: response.novelVersion }
      : item)
    novelSettingsForm.value.description = response.content.description
    novelSettingsForm.value.outline = response.content.outline
    novelSettingsForm.value.version = response.novelVersion
    projectAiDraftPending.value = false
    projectSaveState.value = projectDirty ? 'pending' : 'saved'
    if (force) ElMessage.success('立项资料、小说简介和故事大纲已保存')
    return true
  } catch (error) {
    saveFailed = true
    projectDirty = true
    projectSaveState.value = 'failed'
    ElMessage.error(error instanceof Error ? error.message : '立项资料保存失败')
    return false
  } finally {
    savingProject.value = false
  }
  })()
  try { return await projectSavingPromise }
  finally {
    projectSavingPromise = undefined
    if (projectDirty && !saveFailed) {
      projectSaveState.value = 'pending'
      window.clearTimeout(projectSaveTimer)
      projectSaveTimer = window.setTimeout(() => { void flushProjectSave() }, 600)
    }
  }
}

function writingStyleStorageKey() { return `mojing-writing-style:${novelId.value ?? 'global'}` }

function rememberWritingStyle() {
  localStorage.setItem(writingStyleStorageKey(), selectedWritingStyleId.value)
}

async function loadWritingStyles() {
  if (!novelId.value) return
  try {
    writingStyles.value = await requestJson<WritingStyleDto[]>(`/api/writing-styles?novelId=${novelId.value}`)
    const remembered = localStorage.getItem(writingStyleStorageKey())
    const candidate = remembered || selectedWritingStyleId.value
    selectedWritingStyleId.value = writingStyles.value.some(style => style.id === candidate)
      ? candidate
      : 'preset:NATURAL_WEB'
    rememberWritingStyle()
  } catch (error) {
    writingStyles.value = []
    ElMessage.error(error instanceof Error ? error.message : '文风选项加载失败，请先检查文风数据表')
  }
}

function newWritingStyle() {
  styleForm.value = { name: '', description: '', rulesText: '', forbiddenWords: '' }
  styleReferenceInput.value = ''
  styleAssistMode.value = 'ORGANIZE'
}

function editWritingStyle(style: WritingStyleDto) {
  if (style.system || !style.databaseId) return
  styleForm.value = { databaseId: style.databaseId, name: style.name, description: style.description ?? '',
    rulesText: style.rulesText, forbiddenWords: style.forbiddenWords ?? '', version: style.version }
  styleReferenceInput.value = style.referenceExcerpt ?? ''
  styleAssistMode.value = style.sourceType === 'REFERENCE' ? 'REFERENCE' : style.sourceType === 'NOVEL' ? 'NOVEL' : 'ORGANIZE'
}

function openStyleManager() {
  const selected = writingStyles.value.find(style => style.id === selectedWritingStyleId.value && !style.system)
  if (selected) editWritingStyle(selected); else newWritingStyle()
  showStyleManager.value = true
}

function referenceStyleTexts() {
  return styleReferenceInput.value.split(/\n\s*---\s*\n/).map(text => text.trim()).filter(Boolean).slice(0, 5)
}

async function assistWritingStyle() {
  const references = referenceStyleTexts()
  if (styleAssistMode.value === 'REFERENCE' && !references.length) { ElMessage.warning('请粘贴至少一段参考作品'); return }
  if (styleAssistMode.value === 'ORGANIZE' && !styleForm.value.description.trim()) { ElMessage.warning('请先描述想要的文风'); return }
  assistingStyle.value = true
  try {
    const result = await requestJson<WritingStyleAssistDto>('/api/writing-styles/assist', {
      method: 'POST', body: JSON.stringify({ mode: styleAssistMode.value, novelId: novelId.value,
        instruction: styleForm.value.description.trim(), referenceTexts: references }),
    })
    if (!styleForm.value.name.trim()) styleForm.value.name = result.name
    styleForm.value.description = result.description
    styleForm.value.rulesText = result.rulesText
    styleForm.value.forbiddenWords = result.forbiddenWords
    ElMessage.success(result.changeSummary.filter(Boolean).slice(0, 2).join('；') || '文风规则已整理，请检查后保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : 'AI文风分析失败') }
  finally { assistingStyle.value = false }
}

async function saveWritingStyle() {
  if (!novelId.value || !styleForm.value.name.trim() || !styleForm.value.rulesText.trim()) {
    ElMessage.warning('请填写文风名称和规则'); return
  }
  savingStyle.value = true
  try {
    const form = styleForm.value
    const path = form.databaseId ? `/api/writing-styles/${form.databaseId}` : '/api/writing-styles'
    const saved = await requestJson<WritingStyleDto>(path, { method: form.databaseId ? 'PUT' : 'POST',
      body: JSON.stringify({ novelId: novelId.value, name: form.name.trim(),
        sourceType: styleAssistMode.value === 'REFERENCE' ? 'REFERENCE' : styleAssistMode.value === 'NOVEL' ? 'NOVEL' : 'CUSTOM',
        description: form.description.trim(), rulesText: form.rulesText.trim(),
        forbiddenWords: form.forbiddenWords.trim(), referenceExcerpt: styleReferenceInput.value.trim(),
        version: form.version ?? null }) })
    await loadWritingStyles()
    selectedWritingStyleId.value = saved.id
    rememberWritingStyle()
    editWritingStyle(saved)
    showStyleManager.value = false
    ElMessage.success('私人文风已保存并用于后续正文生成')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '文风保存失败') }
  finally { savingStyle.value = false }
}

async function deleteWritingStyle() {
  if (!styleForm.value.databaseId) return
  try { await ElMessageBox.confirm(`确定删除私人文风“${styleForm.value.name}”吗？`, '删除文风', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/writing-styles/${styleForm.value.databaseId}`, { method: 'DELETE' })
    selectedWritingStyleId.value = 'preset:NATURAL_WEB'
    rememberWritingStyle()
    await loadWritingStyles()
    newWritingStyle()
    ElMessage.success('私人文风已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '文风删除失败') }
}

async function openNovel(novel: NovelDto) {
  loadingWorkspace.value = true
  saveState.value = '正在加载…'
  prompt.value = ''
  result.value = ''
  resetAgentWorkspace()
  chapterMemories.value = {}
  try {
    novelId.value = novel.id
    novelTitle.value = novel.title
    let chapterDtos = await requestJson<ChapterDto[]>(`/api/novels/${novel.id}/chapters`)
    if (chapterDtos.length === 0) {
      const firstChapter = await requestJson<ChapterDto>(`/api/novels/${novel.id}/chapters`, {
        method: 'POST',
        body: JSON.stringify({ title: '第一章', content: '' }),
      })
      chapterDtos = [firstChapter]
    }

    chapters.value = chapterDtos.map(toChapterView)
    activeChapter.value = 0
    loadChapterIntoEditor(0)
    dirty = false
    currentView.value = 'editor'
    saveState.value = '已保存'
    void loadWritingStyles()
    void loadStoryBible(false)
    void loadChapterMemoryIndex(false)
  } catch (error) {
    saveState.value = '加载失败'
    ElMessage.error(error instanceof Error ? error.message : '章节加载失败')
  } finally {
    loadingWorkspace.value = false
  }
}

async function goHome() {
  if (!await flushSave()) return
  dismissInlineCompletion()
  focusMode.value = false
  showMobileNav.value = false
  showMobileAi.value = false
  currentView.value = 'home'
  await initializeWorkspace()
}

async function openDirectorPage() {
  const chapter = chapters.value[activeChapter.value]
  if (!chapter || !await flushSave()) return
  dismissInlineCompletion()
  focusMode.value = false
  showMobileAi.value = false
  currentView.value = 'director'
  void loadDirectorConversation(chapter.id)
}

async function closeDirectorPage() {
  if (!await flushDirectorPlanSave()) return
  if (agentDraft.value.trim()) {
    try {
      await ElMessageBox.confirm(
        '正文草稿已经生成，但还没有插入当前章节。是否先插入正文再返回？',
        '草稿尚未插入',
        {
          type: 'warning',
          confirmButtonText: '插入并返回',
          cancelButtonText: '仅返回正文',
          distinguishCancelAndClose: true,
        },
      )
      await insertAgentDraftAndReturn()
      return
    } catch (action) {
      if (action === 'close') return
    }
  }
  await returnToEditor()
}

async function returnToEditor() {
  currentView.value = 'editor'
  await nextTick()
  resizeEditor()
}

async function selectDirectorChapter(event: Event) {
  const index = Number((event.target as HTMLSelectElement).value)
  if (!Number.isInteger(index) || index < 0 || index >= chapters.value.length || index === activeChapter.value) return
  if (!await flushDirectorPlanSave() || !await flushSave()) return
  loadChapterIntoEditor(index)
}

function formatUpdatedAt(value: string) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '最近'
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

function scheduleSave() {
  dirty = true
  saveState.value = '待保存'
  window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => { void flushSave() }, 1000)
}

async function flushSave(): Promise<boolean> {
  window.clearTimeout(saveTimer)
  if (savingPromise) {
    const activeSave = savingPromise
    const saved = await activeSave
    if (savingPromise === activeSave) savingPromise = undefined
    return saved && (!dirty || await flushSave())
  }
  if (!dirty) return true

  const chapter = chapters.value[activeChapter.value]
  if (!chapter) return true
  dirty = false
  saveState.value = '保存中…'

  const currentSave = (async () => {
    try {
      const saved = await requestJson<ChapterDto>(`/api/chapters/${chapter.id}`, {
        method: 'PUT',
        body: JSON.stringify({ title: chapter.title, content: chapter.content, version: chapter.version }),
      })
      chapter.version = saved.version
      chapter.words = saved.wordCount
      chapter.contentVersion = saved.contentVersion
      const memory = chapterMemories.value[chapter.id]
      if (memory) memory.stale = memory.sourceChapterVersion !== saved.contentVersion
      if (!dirty) saveState.value = '已保存'
      return true
    } catch (error) {
      dirty = true
      saveState.value = '保存失败'
      ElMessage.error(error instanceof Error ? error.message : '章节保存失败')
      return false
    }
  })()
  savingPromise = currentSave

  const saved = await currentSave
  if (savingPromise === currentSave) savingPromise = undefined
  if (saved && dirty) scheduleSave()
  return saved
}

function handleEditorInput() {
  recordEditorHistory()
  const chapter = chapters.value[activeChapter.value]
  if (chapter) {
    chapter.content = editorContent.value
    chapter.words = currentWords.value
  }
  scheduleSave()
  resizeEditor()
  if (suppressNextInlineSchedule) suppressNextInlineSchedule = false
  else scheduleInlineCompletion()
}

function currentEditorSnapshot(): EditorSnapshot {
  const editor = editorRef.value
  return {
    content: editorContent.value,
    selectionStart: editor?.selectionStart ?? editorContent.value.length,
    selectionEnd: editor?.selectionEnd ?? editorContent.value.length,
  }
}

function handleEditorBeforeInput() {
  dismissInlineCompletion()
  if (!pendingHistoryStart.value) pendingHistoryStart.value = currentEditorSnapshot()
  redoStack.value = []
}

function dismissInlineCompletion(cancelRequest = true) {
  window.clearTimeout(inlineCompletionTimer)
  inlineCompletionTimer = undefined
  if (cancelRequest) {
    inlineCompletionRequestId += 1
    inlineCompletionController?.abort()
    inlineCompletionController = undefined
  }
  inlineCompletionLoading.value = false
  inlineCompletion.value = ''
}

function inlineCompletionPosition() {
  const editor = editorRef.value
  if (!editor || editor.selectionStart !== editor.selectionEnd) return undefined
  return editor.selectionStart
}

function scheduleInlineCompletion(force = false) {
  window.clearTimeout(inlineCompletionTimer)
  inlineCompletionTimer = undefined
  if ((!inlineCompletionEnabled.value && !force) || editorComposing || currentView.value !== 'editor') return
  if (!force && document.activeElement !== editorRef.value) return
  if (inlineCompletionLoading.value || inlineCompletion.value) return
  const offset = inlineCompletionPosition()
  if (offset === undefined || !editorContent.value.slice(0, offset).trim()) return
  inlineCompletionTimer = window.setTimeout(() => void requestInlineCompletion(force), force ? 0 : 1000)
}

async function requestInlineCompletion(force = false) {
  window.clearTimeout(inlineCompletionTimer)
  inlineCompletionTimer = undefined
  if ((!inlineCompletionEnabled.value && !force) || editorComposing || currentView.value !== 'editor') return
  const editor = editorRef.value
  const chapter = chapters.value[activeChapter.value]
  const offset = inlineCompletionPosition()
  if (!editor || !chapter || offset === undefined) return
  const contentSnapshot = editorContent.value
  const beforeCursor = contentSnapshot.slice(0, offset)
  if (!beforeCursor.trim()) return

  inlineCompletionController?.abort()
  const controller = new AbortController()
  inlineCompletionController = controller
  const requestId = ++inlineCompletionRequestId
  inlineCompletionLoading.value = true
  inlineCompletion.value = ''
  inlineCompletionOffset.value = offset
  try {
    await requestEventStream('/api/ai/completion/stream', {
      method: 'POST',
      signal: controller.signal,
      body: JSON.stringify({
        novelId: novelId.value,
        chapterId: chapter.id,
        cursorContext: beforeCursor.slice(-6000),
        afterCursor: contentSnapshot.slice(offset, offset + 2000),
        maxLength: 100,
        instruction: '',
        styleId: selectedWritingStyleId.value,
        inlineCompletion: true,
      }),
    }, (event, payload) => {
      if (requestId !== inlineCompletionRequestId) return
      const currentOffset = inlineCompletionPosition()
      if (editorContent.value !== contentSnapshot || currentOffset !== offset || chapters.value[activeChapter.value]?.id !== chapter.id) {
        controller.abort()
        return
      }
      if (event === 'delta') {
        const delta = (payload as { text?: string })?.text ?? ''
        if (delta) inlineCompletion.value = `${inlineCompletion.value}${delta}`.slice(0, 100)
      } else if (event === 'complete' && !inlineCompletion.value) {
        inlineCompletion.value = ((payload as CompletionDto)?.completion ?? '').slice(0, 100)
      } else if (event === 'error') {
        throw new Error((payload as { message?: string })?.message || '自动补全流式响应失败')
      }
    })
    if (requestId === inlineCompletionRequestId && !inlineCompletion.value) {
      throw new Error('AI 没有返回补全内容')
    }
  } catch (error) {
    if (controller.signal.aborted || requestId !== inlineCompletionRequestId) return
    if (force) ElMessage.error(error instanceof Error ? error.message : '自动补全失败')
  } finally {
    if (requestId === inlineCompletionRequestId) {
      inlineCompletionLoading.value = false
      inlineCompletionController = undefined
    }
  }
}

async function acceptInlineCompletion() {
  const completion = inlineCompletion.value
  const offset = inlineCompletionOffset.value
  if (!completion || offset < 0 || offset > editorContent.value.length) return
  if (!pendingHistoryStart.value) pendingHistoryStart.value = currentEditorSnapshot()
  const before = editorContent.value.slice(0, offset)
  const after = editorContent.value.slice(offset)
  const insertion = inlineCompletionInsertText.value
  editorContent.value = `${before}${insertion}${after}`
  const nextOffset = offset + insertion.length
  dismissInlineCompletion(false)
  suppressNextInlineSchedule = true
  handleEditorInput()
  await nextTick()
  editorRef.value?.focus()
  editorRef.value?.setSelectionRange(nextOffset, nextOffset)
  lastEditorSnapshot.value = currentEditorSnapshot()
}

function toggleInlineCompletion() {
  inlineCompletionEnabled.value = !inlineCompletionEnabled.value
  window.localStorage.setItem('mojing-inline-completion-enabled', String(inlineCompletionEnabled.value))
  if (inlineCompletionEnabled.value) scheduleInlineCompletion()
  else dismissInlineCompletion()
}

function handleEditorCompositionStart() {
  editorComposing = true
  dismissInlineCompletion()
}

function handleEditorCompositionEnd() {
  editorComposing = false
  scheduleInlineCompletion()
}

function recordEditorHistory() {
  if (!pendingHistoryStart.value) pendingHistoryStart.value = lastEditorSnapshot.value
  lastEditorSnapshot.value = currentEditorSnapshot()
  redoStack.value = []
  window.clearTimeout(historyTimer)
  historyTimer = window.setTimeout(commitEditorHistory, 700)
}

function pushHistory(stack: typeof undoStack, snapshot: EditorSnapshot) {
  const last = stack.value[stack.value.length - 1]
  if (last?.content === snapshot.content) return
  stack.value.push(snapshot)
  if (stack.value.length > 100) stack.value.shift()
}

function commitEditorHistory() {
  window.clearTimeout(historyTimer)
  const start = pendingHistoryStart.value
  pendingHistoryStart.value = undefined
  const current = currentEditorSnapshot()
  if (start && start.content !== current.content) pushHistory(undoStack, start)
  lastEditorSnapshot.value = current
}

function resetEditorHistory(content: string) {
  window.clearTimeout(historyTimer)
  undoStack.value = []
  redoStack.value = []
  pendingHistoryStart.value = undefined
  lastEditorSnapshot.value = { content, selectionStart: 0, selectionEnd: 0 }
}

async function restoreEditorSnapshot(snapshot: EditorSnapshot) {
  dismissInlineCompletion()
  pendingHistoryStart.value = undefined
  editorContent.value = snapshot.content
  lastEditorSnapshot.value = snapshot
  selectionStart.value = snapshot.selectionStart
  selectionEnd.value = snapshot.selectionEnd
  selectedText.value = ''
  rewriteResult.value = undefined
  const chapter = chapters.value[activeChapter.value]
  if (chapter) {
    chapter.content = snapshot.content
    chapter.words = snapshot.content.replace(/\s/g, '').length
  }
  scheduleSave()
  resizeEditor()
  await nextTick()
  editorRef.value?.focus()
  editorRef.value?.setSelectionRange(snapshot.selectionStart, snapshot.selectionEnd)
}

async function undoEditor() {
  commitEditorHistory()
  const snapshot = undoStack.value.pop()
  if (!snapshot) return
  pushHistory(redoStack, currentEditorSnapshot())
  await restoreEditorSnapshot(snapshot)
}

async function redoEditor() {
  commitEditorHistory()
  const snapshot = redoStack.value.pop()
  if (!snapshot) return
  pushHistory(undoStack, currentEditorSnapshot())
  await restoreEditorSnapshot(snapshot)
}

function handleEditorKeydown(event: KeyboardEvent) {
  if (event.isComposing) return
  if (event.key === 'Tab' && inlineCompletion.value) {
    event.preventDefault()
    void acceptInlineCompletion()
    return
  }
  if (event.key === 'Escape' && (inlineCompletionLoading.value || inlineCompletion.value)) {
    event.preventDefault()
    dismissInlineCompletion()
    return
  }
  const inlineShortcut = (event.altKey && event.key === '/')
    || ((event.ctrlKey || event.metaKey) && event.code === 'Space')
  if (inlineShortcut) {
    event.preventDefault()
    dismissInlineCompletion()
    void requestInlineCompletion(true)
    return
  }
  if (!(event.ctrlKey || event.metaKey)) return
  const key = event.key.toLowerCase()
  if (key === 'z') {
    event.preventDefault()
    if (event.shiftKey) void redoEditor()
    else void undoEditor()
  } else if (key === 'y') {
    event.preventDefault()
    void redoEditor()
  }
}

function handleTitleInput() {
  const chapter = chapters.value[activeChapter.value]
  if (chapter) chapter.title = chapterTitle.value
  scheduleSave()
}

function handleEditorWheel(event: WheelEvent) {
  if (!paperRef.value) return
  paperRef.value.scrollTop += event.deltaY
}

function toggleLeftPanel() {
  if (isMobile.value) showMobileNav.value = true
  else leftCollapsed.value = !leftCollapsed.value
}

function toggleRightPanel() {
  if (isMobile.value) showMobileAi.value = true
  else rightCollapsed.value = !rightCollapsed.value
}

function startResize(side: 'left' | 'right', event: MouseEvent) {
  event.preventDefault()
  window.clearTimeout(paneResizeUnlockTimer)
  paneResizing = true
  const startX = event.clientX
  const startWidth = side === 'left' ? leftWidth.value : rightWidth.value

  const handleMove = (moveEvent: MouseEvent) => {
    const delta = moveEvent.clientX - startX
    if (side === 'left') leftWidth.value = Math.min(420, Math.max(210, startWidth + delta))
    else rightWidth.value = Math.min(560, Math.max(320, startWidth - delta))
  }
  const handleUp = () => {
    window.removeEventListener('mousemove', handleMove)
    window.removeEventListener('mouseup', handleUp)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
    paneResizeUnlockTimer = window.setTimeout(() => { paneResizing = false }, 0)
  }

  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  window.addEventListener('mousemove', handleMove)
  window.addEventListener('mouseup', handleUp)
}

function updateViewportWidth() {
  viewportWidth.value = window.innerWidth
}

function resizeEditor() {
  nextTick(() => {
    const editor = editorRef.value
    if (!editor) return
    const paper = paperRef.value
    const previousScrollTop = paper?.scrollTop ?? 0
    const distanceFromBottom = paper
      ? paper.scrollHeight - paper.clientHeight - paper.scrollTop
      : 0
    const keepAtBottom = Boolean(paper && distanceFromBottom <= 80)
    editor.style.height = 'auto'
    editor.style.height = `${Math.max(520, editor.scrollHeight)}px`
    if (paper) {
      const targetScrollTop = keepAtBottom
        ? Math.max(0, paper.scrollHeight - paper.clientHeight - distanceFromBottom)
        : previousScrollTop
      paper.scrollTop = targetScrollTop
      window.requestAnimationFrame(() => {
        if (paperRef.value === paper) paper.scrollTop = targetScrollTop
      })
    }
  })
}

function loadChapterIntoEditor(index: number) {
  dismissInlineCompletion()
  activeChapter.value = index
  const chapter = chapters.value[index]
  chapterTitle.value = chapter?.title ?? '未命名章节'
  editorContent.value = chapter?.content ?? ''
  resetEditorHistory(editorContent.value)
  result.value = ''
  resetAgentWorkspace()
  if (chapter) void loadDirectorConversation(chapter.id)
  void loadRevisionHistory(false)
  void loadConsistencyReports(false)
  nextTick(() => {
    resizeEditor()
    if (paperRef.value) paperRef.value.scrollTop = 0
    editorRef.value?.focus()
  })
}

function resetAgentWorkspace() {
  window.clearTimeout(directorPlanSaveTimer)
  directorPlanDirty = false
  directorPlanSaveState.value = 'idle'
  agentRunId.value = undefined
  agentPlan.value = undefined
  agentDraft.value = ''
  agentSteps.value = []
  directorMessages.value = []
  loadingDirector.value = false
  directorError.value = ''
  selectionStart.value = 0
  selectionEnd.value = 0
  selectedText.value = ''
  rewriteInstruction.value = ''
  rewriteResult.value = undefined
  revisionHistory.value = []
  consistencyReport.value = undefined
  consistencyReports.value = []
}

function planToView(plan: ChapterPlanDto): ChapterPlanView {
  return { title: plan.title, objective: plan.objective, opening: plan.opening,
    developmentsText: plan.developments.join('\n'), endingHook: plan.endingHook,
    charactersText: plan.characters.join('\n'), continuityNotesText: plan.continuityNotes.join('\n') }
}

function viewToPlan(plan: ChapterPlanView): ChapterPlanDto {
  const lines = (value: string) => value.split('\n').map(item => item.trim()).filter(Boolean)
  return { title: plan.title.trim(), objective: plan.objective.trim(), opening: plan.opening.trim(),
    developments: lines(plan.developmentsText), endingHook: plan.endingHook.trim(),
    characters: lines(plan.charactersText), continuityNotes: lines(plan.continuityNotesText) }
}

function scheduleDirectorPlanSave() {
  if (!agentPlan.value) return
  directorPlanDirty = true
  directorPlanSaveState.value = 'pending'
  window.clearTimeout(directorPlanSaveTimer)
  directorPlanSaveTimer = window.setTimeout(() => { void flushDirectorPlanSave() }, 800)
}

async function flushDirectorPlanSave(): Promise<boolean> {
  window.clearTimeout(directorPlanSaveTimer)
  if (directorPlanSavingPromise) {
    const activeSave = directorPlanSavingPromise
    const saved = await activeSave
    if (directorPlanSavingPromise === activeSave) directorPlanSavingPromise = undefined
    return saved && (!directorPlanDirty || await flushDirectorPlanSave())
  }
  if (!directorPlanDirty || !agentPlan.value) return true
  const chapter = chapters.value[activeChapter.value]
  if (!chapter) return true
  const chapterId = chapter.id
  const plan = viewToPlan(agentPlan.value)
  if (!plan.title || !plan.objective || !plan.opening || !plan.developments.length || !plan.endingHook) {
    directorPlanSaveState.value = 'pending'
    return true
  }
  directorPlanDirty = false
  directorPlanSaveState.value = 'saving'
  const currentSave = (async () => {
    try {
      await requestJson<ChapterPlanDto>(`/api/agent/chapters/${chapterId}/director/plan`, {
        method: 'PUT', body: JSON.stringify({ plan }),
      })
      if (chapters.value[activeChapter.value]?.id === chapterId && !directorPlanDirty) {
        directorPlanSaveState.value = 'saved'
      }
      return true
    } catch (error) {
      directorPlanDirty = true
      directorPlanSaveState.value = 'failed'
      ElMessage.error(error instanceof Error ? error.message : '本章主线保存失败')
      return false
    }
  })()
  directorPlanSavingPromise = currentSave
  const saved = await currentSave
  if (directorPlanSavingPromise === currentSave) directorPlanSavingPromise = undefined
  if (saved && directorPlanDirty) scheduleDirectorPlanSave()
  return saved
}

function handleDirectorComposerKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.isComposing || event.keyCode === 229) return
  if (event.shiftKey && !event.ctrlKey) return
  event.preventDefault()
  void sendDirectorMessage(event.ctrlKey)
}

function scrollDirectorMessagesToBottom() {
  nextTick(() => {
    const container = directorMessagesContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}

async function sendDirectorMessage(generateMainPlan: boolean) {
  const chapter = chapters.value[activeChapter.value]
  if (!novelId.value || !chapter || planning.value || !await flushSave()) return
  if (agentPlan.value && !await flushDirectorPlanSave()) return
  const message = agentGuidance.value.trim()
    || (generateMainPlan ? '请根据当前正文、作品资料和此前对话，生成这一章的主线。' : '')
  if (!message) return
  const temporaryMessageId = pendingDirectorMessageId--
  const temporaryAssistantId = pendingDirectorMessageId--
  agentGuidance.value = ''
  directorMessages.value.push({
    id: temporaryMessageId,
    role: 'USER',
    content: message,
    runId: null,
    plan: null,
    changeSummary: [],
    createdAt: new Date().toISOString(),
    pending: true,
  })
  if (!generateMainPlan) {
    directorMessages.value.push({
      id: temporaryAssistantId,
      role: 'ASSISTANT',
      content: '',
      runId: null,
      plan: null,
      changeSummary: [],
      createdAt: new Date().toISOString(),
      pending: true,
    })
  }
  scrollDirectorMessagesToBottom()
  planning.value = true
  directorGeneratingPlan.value = generateMainPlan
  directorError.value = ''
  if (generateMainPlan) agentDraft.value = ''
  try {
    let completedConversation: DirectorConversationDto | undefined
    await requestEventStream(`/api/agent/novels/${novelId.value}/chapters/${chapter.id}/director/messages/stream`, {
      method: 'POST', body: JSON.stringify({
        message,
        currentPlan: agentPlan.value ? viewToPlan(agentPlan.value) : null,
        thinkingEnabled: directorThinkingEnabled.value,
        generatePlan: generateMainPlan,
      }),
    }, (event, data) => {
      if (event === 'connected') {
        directorMessages.value = directorMessages.value.map(item => item.id === temporaryMessageId
          ? { ...item, pending: false }
          : item)
      } else if (event === 'delta' && !generateMainPlan) {
        const text = typeof data === 'object' && data && 'text' in data ? String(data.text ?? '') : ''
        if (!text) return
        directorMessages.value = directorMessages.value.map(item => item.id === temporaryAssistantId
          ? { ...item, content: item.content + text }
          : item)
        scrollDirectorMessagesToBottom()
      } else if (event === 'complete') {
        completedConversation = data as DirectorConversationDto
      } else if (event === 'error') {
        const message = typeof data === 'object' && data && 'message' in data
          ? String(data.message) : '章节导演流式响应失败'
        throw new Error(message)
      }
    })
    if (!completedConversation) throw new Error('章节导演流式响应提前结束')
    applyDirectorConversation(completedConversation)
    if (generateMainPlan) {
      if (!agentPlan.value) throw new Error('章节导演没有返回主线计划')
      ElMessage.success('已根据当前对话生成本章主线')
    }
  } catch (error) {
    directorMessages.value = directorMessages.value.map(item => item.id === temporaryMessageId || item.id === temporaryAssistantId
      ? { ...item, pending: false, failed: true }
      : item)
    directorError.value = error instanceof Error ? error.message : '章节导演对话失败'
    ElMessage.error(directorError.value)
  } finally {
    planning.value = false
    directorGeneratingPlan.value = false
  }
}

async function generatePlan() {
  await sendDirectorMessage(true)
}

async function loadDirectorConversation(chapterId: number) {
  loadingDirector.value = true
  try {
    const conversation = await requestJson<DirectorConversationDto>(`/api/agent/chapters/${chapterId}/director`)
    if (chapters.value[activeChapter.value]?.id !== chapterId) return
    applyDirectorConversation(conversation)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '读取章节导演对话失败')
  } finally {
    if (chapters.value[activeChapter.value]?.id === chapterId) loadingDirector.value = false
  }
}

function applyDirectorConversation(conversation: DirectorConversationDto) {
  window.clearTimeout(directorPlanSaveTimer)
  directorPlanDirty = false
  directorMessages.value = conversation.messages
  agentRunId.value = conversation.currentRunId ?? undefined
  agentPlan.value = conversation.currentPlan ? planToView(conversation.currentPlan) : undefined
  directorPlanSaveState.value = conversation.currentPlan ? 'saved' : 'idle'
  agentSteps.value = []
  scrollDirectorMessagesToBottom()
}

function restoreDirectorPlan(message: DirectorMessageDto) {
  if (!message.plan) return
  agentPlan.value = planToView(message.plan)
  agentRunId.value = message.runId ?? undefined
  agentDraft.value = ''
  scheduleDirectorPlanSave()
  ElMessage.success('已切换到这轮对话生成的主线版本')
}

async function deleteDirectorTurn(message: DirectorMessageDto) {
  if (planning.value || drafting.value) return
  try {
    await ElMessageBox.confirm(
      '将删除这条消息和导演针对它的回复。若本轮生成了当前主线，主线会回退到上一版。',
      '删除本轮对话',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
  } catch { return }
  try {
    const conversation = await requestJson<DirectorConversationDto>(`/api/agent/director/messages/${message.id}`, { method: 'DELETE' })
    applyDirectorConversation(conversation)
    agentDraft.value = ''
    ElMessage.success('本轮对话已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除导演对话失败')
  }
}

async function openDirectorRag() {
  const chapter = chapters.value[activeChapter.value]
  if (!novelId.value || !chapter || loadingDirectorRag.value) return
  showDirectorRag.value = true
  loadingDirectorRag.value = true
  try {
    directorRagDebug.value = await requestJson<RagDebugInfoDto>(
      `/api/agent/novels/${novelId.value}/chapters/${chapter.id}/director/rag-preview`,
      { method: 'POST', body: JSON.stringify({ message: agentGuidance.value.trim() }) },
    )
  } catch (error) {
    directorRagDebug.value = undefined
    ElMessage.error(error instanceof Error ? error.message : '读取章节导演引入剧情失败')
  } finally {
    loadingDirectorRag.value = false
  }
}

async function generateDraft() {
  if (!agentRunId.value || !agentPlan.value || drafting.value) return
  const plan = viewToPlan(agentPlan.value)
  if (!plan.title || !plan.objective || !plan.opening || !plan.developments.length || !plan.endingHook) {
    ElMessage.warning('请补充完整章节计划后再生成正文')
    return
  }
  if (!await flushDirectorPlanSave()) return
  drafting.value = true
  try {
    const run = await requestJson<AgentRunDto>(`/api/agent/runs/${agentRunId.value}/draft`, {
      method: 'POST', body: JSON.stringify({ plan, targetLength: draftTargetLength.value, styleId: selectedWritingStyleId.value }),
    })
    agentDraft.value = run.draft?.trim() ?? ''
    agentSteps.value = run.steps
    if (!agentDraft.value) throw new Error('Agent 没有返回正文草稿')
    await nextTick()
    directorDraftRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    ElMessage.success('正文草稿已生成，请确认后插入正文')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '正文草稿生成失败')
  } finally { drafting.value = false }
}

async function insertAgentDraft() {
  if (!agentDraft.value) return
  const draft = agentDraft.value
  editorContent.value = `${editorContent.value}${editorContent.value.trim() ? '\n\n' : ''}${draft}`
  agentDraft.value = ''
  handleEditorInput()
  resizeEditor()
  await nextTick()
  editorRef.value?.focus()
  editorRef.value?.setSelectionRange(editorContent.value.length, editorContent.value.length)
  ElMessage.success('Agent 草稿已插入当前章节，正在自动保存')
}

async function insertAgentDraftAndReturn() {
  await insertAgentDraft()
  await returnToEditor()
}

function captureSelection() {
  if (paneResizing) return
  const editor = editorRef.value
  if (!editor) return
  const start = editor.selectionStart
  const end = editor.selectionEnd
  if (end <= start) {
    selectionStart.value = start
    selectionEnd.value = start
    selectedText.value = ''
    if ((inlineCompletionLoading.value || inlineCompletion.value) && inlineCompletionOffset.value !== start) {
      dismissInlineCompletion()
    }
    scheduleInlineCompletion()
    return
  }
  dismissInlineCompletion()
  const nextSelectedText = editorContent.value.slice(start, end)
  if (start === selectionStart.value && end === selectionEnd.value && nextSelectedText === selectedText.value) return
  selectionStart.value = start
  selectionEnd.value = end
  selectedText.value = nextSelectedText
  rewriteResult.value = undefined
}

function openRewriteMode() {
  captureSelection()
  aiMode.value = 'rewrite'
  void loadRevisionHistory(false)
}

function clearSelection() {
  selectionStart.value = 0
  selectionEnd.value = 0
  selectedText.value = ''
  rewriteResult.value = undefined
  editorRef.value?.setSelectionRange(0, 0)
}

async function focusEditorForSelection() {
  await nextTick()
  editorRef.value?.focus()
  ElMessage.info('请在正文中按住鼠标拖动，选择需要修改的文字')
}

async function generateRewrite() {
  const chapter = chapters.value[activeChapter.value]
  if (!novelId.value || !chapter || !selectedText.value || rewriting.value) return
  const currentSelection = editorContent.value.slice(selectionStart.value, selectionEnd.value)
  if (currentSelection !== selectedText.value) {
    ElMessage.warning('正文在选择后发生了变化，请重新选择需要修改的段落')
    clearSelection()
    return
  }
  if (!await flushSave()) return
  rewriting.value = true
  try {
    const beforeContext = editorContent.value.slice(Math.max(0, selectionStart.value - rewriteContextChars), selectionStart.value)
    const afterContext = editorContent.value.slice(selectionEnd.value, selectionEnd.value + rewriteContextChars)
    const revision = await requestJson<ChapterRevisionDto>(`/api/agent/novels/${novelId.value}/chapters/${chapter.id}/rewrite`, {
      method: 'POST',
      body: JSON.stringify({ selectedText: selectedText.value, beforeContext, afterContext,
        mode: rewriteMode.value, instruction: rewriteInstruction.value.trim(),
        startOffset: selectionStart.value, endOffset: selectionEnd.value,
        thinkingEnabled: rewriteThinkingEnabled.value, styleId: selectedWritingStyleId.value }),
    })
    rewriteResult.value = revision
    revisionHistory.value = [revision, ...revisionHistory.value.filter(item => item.id !== revision.id)]
    ElMessage.success('修改版已生成，请对照确认')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '段落修改失败') }
  finally { rewriting.value = false }
}

function locateRevisionText(revision: ChapterRevisionDto, target: string, expectedStart: number) {
  if (editorContent.value.slice(expectedStart, expectedStart + target.length) === target)
    return { start: expectedStart, end: expectedStart + target.length }
  const first = editorContent.value.indexOf(target)
  if (first >= 0 && editorContent.value.indexOf(target, first + 1) < 0)
    return { start: first, end: first + target.length }
  return undefined
}

async function updateRevisionStatus(revision: ChapterRevisionDto, status: 'ACCEPTED' | 'REJECTED' | 'ROLLED_BACK') {
  const updated = await requestJson<ChapterRevisionDto>(`/api/agent/revisions/${revision.id}`, {
    method: 'PATCH', body: JSON.stringify({ status, version: revision.version }),
  })
  const index = revisionHistory.value.findIndex(item => item.id === updated.id)
  if (index >= 0) revisionHistory.value[index] = updated
  if (rewriteResult.value?.id === updated.id) rewriteResult.value = updated
  return updated
}

async function acceptRewrite() {
  const revision = rewriteResult.value
  if (!revision || revision.status !== 'GENERATED' || decidingRevision.value) return
  const range = locateRevisionText(revision, revision.originalText, revision.startOffset)
  if (!range) {
    ElMessage.error('无法安全定位原文，正文可能已经变化，请放弃本次修改后重新选择')
    return
  }
  decidingRevision.value = true
  const previousContent = editorContent.value
  try {
    editorContent.value = previousContent.slice(0, range.start) + revision.revisedText + previousContent.slice(range.end)
    handleEditorInput()
    if (!await flushSave()) throw new Error('替换后的章节保存失败')
    await updateRevisionStatus(revision, 'ACCEPTED')
    await loadConsistencyReports(false)
    selectionStart.value = range.start
    selectionEnd.value = range.start + revision.revisedText.length
    selectedText.value = revision.revisedText
    await nextTick()
    resizeEditor()
    editorRef.value?.focus()
    editorRef.value?.setSelectionRange(selectionStart.value, selectionEnd.value)
    ElMessage.success('修改版已替换并保存，可以从修改历史中回滚')
  } catch (error) {
    editorContent.value = previousContent
    handleEditorInput()
    ElMessage.error(error instanceof Error ? error.message : '接受修改失败')
  } finally { decidingRevision.value = false }
}

async function rejectRewrite() {
  const revision = rewriteResult.value
  if (!revision || revision.status !== 'GENERATED' || decidingRevision.value) return
  decidingRevision.value = true
  try {
    await updateRevisionStatus(revision, 'REJECTED')
    await loadConsistencyReports(false)
    ElMessage.success('已放弃本次修改，原文没有变化')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '修改状态保存失败') }
  finally { decidingRevision.value = false }
}

async function loadRevisionHistory(showError = true) {
  const chapter = chapters.value[activeChapter.value]
  if (!chapter) return
  try { revisionHistory.value = await requestJson<ChapterRevisionDto[]>(`/api/agent/chapters/${chapter.id}/revisions`) }
  catch (error) { if (showError) ElMessage.error(error instanceof Error ? error.message : '修改历史加载失败') }
}

async function rollbackRevision(revision: ChapterRevisionDto) {
  if (revision.status !== 'ACCEPTED' || decidingRevision.value) return
  const range = locateRevisionText(revision, revision.revisedText, revision.startOffset)
  if (!range) {
    ElMessage.error('当前正文中无法唯一找到修改后的文字，为避免覆盖其他内容，不能自动回滚')
    return
  }
  try {
    await ElMessageBox.confirm('将把这次修改恢复为原文，确定继续吗？', '回滚段落修改', { type: 'warning', confirmButtonText: '确认回滚', cancelButtonText: '取消' })
  } catch { return }
  decidingRevision.value = true
  const previousContent = editorContent.value
  try {
    editorContent.value = previousContent.slice(0, range.start) + revision.originalText + previousContent.slice(range.end)
    handleEditorInput()
    if (!await flushSave()) throw new Error('回滚后的章节保存失败')
    await updateRevisionStatus(revision, 'ROLLED_BACK')
    await loadConsistencyReports(false)
    resizeEditor()
    ElMessage.success('已恢复为修改前的原文')
  } catch (error) {
    editorContent.value = previousContent
    handleEditorInput()
    ElMessage.error(error instanceof Error ? error.message : '回滚失败')
  } finally { decidingRevision.value = false }
}

function rewriteModeLabel(mode: string) {
  return rewriteModes.find(item => item.value === mode)?.label ?? mode
}

function rewriteStatusLabel(status: string) {
  return ({ GENERATED: '等待确认', ACCEPTED: '已接受', REJECTED: '已放弃', ROLLED_BACK: '已回滚' } as Record<string, string>)[status] ?? status
}

function formatRevisionTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return `${date.getMonth() + 1}月${date.getDate()}日 ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

function openConsistencyMode() {
  aiMode.value = 'consistency'
  void loadConsistencyReports(false)
}

function openOrganizeMode() {
  aiMode.value = 'organize'
  void loadConsistencyReports(false)
  void loadChapterMemoryIndex(false)
}

async function runOrganizeCheck() {
  aiMode.value = 'consistency'
  await checkConsistency()
}

async function organizeChapter() {
  const chapter = chapters.value[activeChapter.value]
  if (!novelId.value || !chapter || organizingChapter.value || !editorContent.value.trim() || !await flushSave()) return
  organizingChapter.value = true
  try {
    const report = await requestJson<ConsistencyReportDto>(`/api/agent/novels/${novelId.value}/chapters/${chapter.id}/consistency-check`, {
      method: 'POST', body: JSON.stringify({ focus: '整理章节时进行全面检查，重点关注会影响后续剧情的冲突' }),
    })
    consistencyReport.value = report
    consistencyReports.value = [report, ...consistencyReports.value.filter(item => item.id !== report.id)]
    const memory = await requestJson<ChapterMemoryDto>(`/api/agent/chapters/${chapter.id}/memory/extract`, {
      method: 'POST', body: JSON.stringify({ instruction: '重点保留会影响后续章节的人物变化、关键事件、地点、伏笔和未解决问题' }),
    })
    setMemoryForm(memory)
    showChapterMemory.value = true
    ElMessage.success('章节检查和记忆草稿已生成；可以修改确认，也可以直接关闭')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '章节整理失败') }
  finally { organizingChapter.value = false }
}

async function checkConsistency() {
  const chapter = chapters.value[activeChapter.value]
  if (!novelId.value || !chapter || !editorContent.value.trim() || checkingConsistency.value || !await flushSave()) return
  checkingConsistency.value = true
  try {
    const report = await requestJson<ConsistencyReportDto>(`/api/agent/novels/${novelId.value}/chapters/${chapter.id}/consistency-check`, {
      method: 'POST', body: JSON.stringify({ focus: consistencyFocus.value.trim() }),
    })
    consistencyReport.value = report
    consistencyReports.value = [report, ...consistencyReports.value.filter(item => item.id !== report.id)]
    ElMessage.success(report.issues.length ? `检查完成，发现 ${report.issues.length} 个问题` : '检查完成，没有发现明显问题')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '一致性检查失败') }
  finally { checkingConsistency.value = false }
}

async function loadConsistencyReports(showError = true) {
  const chapter = chapters.value[activeChapter.value]
  if (!chapter) return
  const selectedId = consistencyReport.value?.id
  try {
    consistencyReports.value = await requestJson<ConsistencyReportDto[]>(`/api/agent/chapters/${chapter.id}/consistency-reports`)
    consistencyReport.value = consistencyReports.value.find(item => item.id === selectedId) ?? consistencyReports.value[0]
  } catch (error) { if (showError) ElMessage.error(error instanceof Error ? error.message : '检查历史加载失败') }
}

async function updateIssueStatus(issue: ConsistencyIssueDto, status: 'IGNORED' | 'OPEN') {
  decidingIssueId.value = issue.id
  try {
    await requestJson<ConsistencyIssueDto>(`/api/agent/consistency-issues/${issue.id}`, {
      method: 'PATCH', body: JSON.stringify({ status, version: issue.version }),
    })
    await loadConsistencyReports(false)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '问题状态更新失败') }
  finally { decidingIssueId.value = undefined }
}

async function ignoreIssue(issue: ConsistencyIssueDto) { await updateIssueStatus(issue, 'IGNORED') }
async function reopenIssue(issue: ConsistencyIssueDto) { await updateIssueStatus(issue, 'OPEN') }

async function locateIssue(issue: ConsistencyIssueDto) {
  const start = editorContent.value.indexOf(issue.quote)
  if (start < 0) { ElMessage.error('当前正文中已找不到这段原文，建议重新执行检查'); return }
  selectionStart.value = start
  selectionEnd.value = start + issue.quote.length
  selectedText.value = issue.quote
  await nextTick()
  editorRef.value?.focus()
  editorRef.value?.setSelectionRange(selectionStart.value, selectionEnd.value)
  ElMessage.success('已在正文中选中问题片段')
}

async function fixConsistencyIssue(issue: ConsistencyIssueDto) {
  if (decidingIssueId.value || !await flushSave()) return
  decidingIssueId.value = issue.id
  try {
    const updated = await requestJson<ConsistencyIssueDto>(`/api/agent/consistency-issues/${issue.id}/fix`, {
      method: 'POST', body: JSON.stringify({ instruction: '' }),
    })
    await loadConsistencyReports(false)
    if (!updated.revision) throw new Error('AI 没有返回可确认的修复版本')
    openIssueRevision(updated)
    ElMessage.success('修复版本已生成，请对照确认')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : 'AI 修复失败') }
  finally { decidingIssueId.value = undefined }
}

function openIssueRevision(issue: ConsistencyIssueDto) {
  const revision = issue.revision
  if (!revision) return
  rewriteResult.value = revision
  rewriteMode.value = revision.revisionType
  rewriteInstruction.value = revision.instruction ?? ''
  selectionStart.value = revision.startOffset
  selectionEnd.value = revision.endOffset
  selectedText.value = revision.originalText
  revisionHistory.value = [revision, ...revisionHistory.value.filter(item => item.id !== revision.id)]
  aiMode.value = 'rewrite'
}

function scoreClass(score: number) { return score >= 85 ? 'score-good' : score >= 70 ? 'score-warn' : 'score-bad' }
function severityLabel(severity: string) { return ({ HIGH: '严重', MEDIUM: '注意', LOW: '建议' } as Record<string, string>)[severity] ?? severity }
function issueStatusLabel(status: string) { return ({ OPEN: '待处理', IGNORED: '已忽略', WAITING_APPROVAL: '等待确认', FIXED: '已修复' } as Record<string, string>)[status] ?? status }
function issueTypeLabel(type: string) {
  return ({ CHARACTER_CONFLICT: '人物冲突', WORLD_SETTING_CONFLICT: '设定冲突', TIMELINE_CONFLICT: '时间线冲突', OUTLINE_DEVIATION: '偏离大纲', POV_INCONSISTENCY: '视角变化', LOGIC_GAP: '逻辑问题', REPETITION: '重复表达', STYLE: '表达建议' } as Record<string, string>)[type] ?? type
}

function stepLabel(type: string) {
  return ({ LOAD_CONTEXT: '读取创作上下文', PLAN_CHAPTER: '规划章节', WRITE_DRAFT: '生成正文草稿', REWRITE_SELECTION: '打磨选中段落', CHECK_CONSISTENCY: '检查一致性', EXTRACT_CHAPTER_MEMORY: '提取章节记忆', QUICK_COMPLETION: '快速续写' } as Record<string, string>)[type] ?? type
}

function operationLabel(operation: string) {
  return ({ CHAPTER_WRITING: '章节导演', PARAGRAPH_REWRITE: '段落打磨', CONSISTENCY_CHECK: '一致性检查', CHAPTER_MEMORY: '章节记忆', QUICK_COMPLETION: '快速续写' } as Record<string, string>)[operation] ?? operation
}

function runStatusLabel(status: string) {
  return ({ RUNNING: '运行中', WAITING_APPROVAL: '等待确认', COMPLETED: '已完成', FAILED: '失败' } as Record<string, string>)[status] ?? status
}

function formatDuration(duration?: number) {
  if (duration == null) return '—'
  if (duration < 1000) return `${duration} ms`
  return `${(duration / 1000).toFixed(duration < 10_000 ? 1 : 0)} 秒`
}

async function openRunCenter() {
  showRunCenter.value = true
  await loadAgentRuns()
}

async function loadAgentRuns() {
  if (!novelId.value || loadingRuns.value) return
  loadingRuns.value = true
  try {
    runSummaries.value = await requestJson<AgentRunSummaryDto[]>(`/api/agent/novels/${novelId.value}/runs?limit=50`)
    const current = runSummaries.value.find(run => run.id === selectedRun.value?.id) ?? runSummaries.value[0]
    if (current) await selectAgentRun(current)
    else selectedRun.value = undefined
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '运行记录加载失败') }
  finally { loadingRuns.value = false }
}

async function selectAgentRun(run: AgentRunSummaryDto) {
  loadingRunDetail.value = true
  try { selectedRun.value = await requestJson<AgentRunDto>(`/api/agent/runs/${run.id}`) }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '调用详情加载失败') }
  finally { loadingRunDetail.value = false }
}

async function openEvalCenter() {
  showEvalCenter.value = true
  await loadEvalCases()
}

async function loadEvalCases() {
  if (!novelId.value) return
  loadingEvalCases.value = true
  try {
    evalCases.value = await requestJson<EvalCaseDto[]>(`/api/evals/novels/${novelId.value}/cases`)
    const selected = evalCases.value.find(item => item.id === selectedEvalCase.value?.id) ?? evalCases.value[0]
    if (selected) await selectEvalCase(selected)
    else { selectedEvalCase.value = undefined; selectedEvalRun.value = undefined; evalRuns.value = [] }
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评测用例加载失败') }
  finally { loadingEvalCases.value = false }
}

function newEvalCase() {
  const chapter = chapters.value[activeChapter.value]
  evalForm.value = { chapterId: chapter?.id, name: `${chapter?.title || '当前章节'}续写回归`, inputContext: editorContent.value.trim().slice(-3000), instruction: prompt.value.trim(), expectedCharacters: '', requiredTerms: '', forbiddenTerms: '顾临，未完待续', minLength: 50, maxLength: 160, enabled: true }
  editingEvalCase.value = true
}

function editEvalCase(item: EvalCaseDto) {
  evalForm.value = { id: item.id, chapterId: item.chapterId ?? undefined, name: item.name, inputContext: item.inputContext, instruction: item.instruction, expectedCharacters: item.expectedCharacters, requiredTerms: item.requiredTerms, forbiddenTerms: item.forbiddenTerms, minLength: item.minLength, maxLength: item.maxLength, enabled: item.enabled, version: item.version }
  editingEvalCase.value = true
}

async function saveEvalCase() {
  if (!novelId.value || savingEvalCase.value) return
  if (!evalForm.value.name.trim() || !evalForm.value.inputContext.trim()) { ElMessage.warning('请填写用例名称和输入上下文'); return }
  savingEvalCase.value = true
  try {
    const path = evalForm.value.id ? `/api/evals/cases/${evalForm.value.id}` : `/api/evals/novels/${novelId.value}/cases`
    const saved = await requestJson<EvalCaseDto>(path, { method: evalForm.value.id ? 'PUT' : 'POST', body: JSON.stringify(evalForm.value) })
    editingEvalCase.value = false
    selectedEvalCase.value = saved
    await loadEvalCases()
    ElMessage.success('评测用例已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评测用例保存失败') }
  finally { savingEvalCase.value = false }
}

async function deleteEvalCase() {
  if (!evalForm.value.id) return
  try { await ElMessageBox.confirm('删除后，该用例的历史评测结果也会一起删除。', '删除评测用例', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) } catch { return }
  try {
    await requestJson<void>(`/api/evals/cases/${evalForm.value.id}`, { method: 'DELETE' })
    editingEvalCase.value = false; selectedEvalCase.value = undefined
    await loadEvalCases(); ElMessage.success('评测用例已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除失败') }
}

async function selectEvalCase(item: EvalCaseDto) {
  selectedEvalCase.value = item; editingEvalCase.value = false
  try {
    evalRuns.value = await requestJson<EvalRunDto[]>(`/api/evals/cases/${item.id}/runs`)
    selectedEvalRun.value = evalRuns.value[0] ?? item.latestRun ?? undefined
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评测历史加载失败') }
}

async function runEvalCase() {
  if (!selectedEvalCase.value || runningEval.value) return
  runningEval.value = true
  try {
    const run = await requestJson<EvalRunDto>(`/api/evals/cases/${selectedEvalCase.value.id}/runs`, { method: 'POST', body: JSON.stringify({ useLlmJudge: true }) })
    selectedEvalRun.value = run; evalRuns.value = [run, ...evalRuns.value]
    await loadEvalCases()
    if (run.status === 'FAILED') ElMessage.error(run.errorMessage || '评测运行失败')
    else ElMessage.success(run.passed ? `评测通过：${run.overallScore} 分` : `评测未通过：${run.overallScore} 分`)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '评测运行失败') }
  finally { runningEval.value = false }
}

async function runAllEvalCases() {
  if (!novelId.value || batchRunningEval.value || !evalCases.value.length) return
  batchRunningEval.value = true
  try {
    const runs = await requestJson<EvalRunDto[]>(`/api/evals/novels/${novelId.value}/runs`, { method: 'POST', body: JSON.stringify({ useLlmJudge: true }) })
    await loadEvalCases()
    const passed = runs.filter(run => run.passed).length
    ElMessage.success(`批量评测完成：${passed}/${runs.length} 个用例通过`)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '批量评测失败') }
  finally { batchRunningEval.value = false }
}

async function loadStoryBible(showError = true) {
  if (!novelId.value) return
  showBibleLoading.value = true
  try {
    const [loadedCharacters, loadedOrganizations, loadedSettings, loadedVolumes] = await Promise.all([
      requestJson<CharacterDto[]>(`/api/novels/${novelId.value}/characters`),
      requestJson<OrganizationDto[]>(`/api/novels/${novelId.value}/organizations`),
      requestJson<WorldSettingDto[]>(`/api/novels/${novelId.value}/world-settings`),
      requestJson<StoryVolumeDto[]>(`/api/novels/${novelId.value}/story-volumes`),
    ])
    characters.value = loadedCharacters
    organizations.value = loadedOrganizations
    worldSettings.value = loadedSettings
    storyVolumes.value = loadedVolumes
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '大纲资料加载失败')
  } finally { showBibleLoading.value = false }
}

async function loadProjectOutline(showError = true) {
  if (!novelId.value) return
  try {
    const response = await requestJson<NovelProjectDto>(`/api/novels/${novelId.value}/project`)
    applyProjectResponse(response)
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '立项与总纲加载失败')
  }
}

async function loadStoryBibleChat(showError = true) {
  if (!novelId.value) return
  loadingStoryBibleChat.value = true
  try {
    const conversation = await requestJson<StoryBibleChatConversationDto>(`/api/novels/${novelId.value}/story-bible/chat`)
    storyBibleChatMessages.value = conversation.messages
    scrollStoryBibleChatToBottom()
  } catch (error) {
    if (showError) ElMessage.error(error instanceof Error ? error.message : '大纲讨论读取失败')
  } finally { loadingStoryBibleChat.value = false }
}

function scrollStoryBibleChatToBottom() {
  void nextTick(() => {
    const element = storyBibleChatMessagesRef.value
    if (element) element.scrollTop = element.scrollHeight
  })
}

function handleStoryBibleChatKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  void sendStoryBibleChat()
}

async function sendStoryBibleChat() {
  if (!novelId.value || sendingStoryBibleChat.value) return
  const message = storyBibleChatInput.value.trim()
  if (!message) return
  sendingStoryBibleChat.value = true
  try {
    const focus = storyBibleChatFocus.value
    const conversation = await requestJson<StoryBibleChatConversationDto>(`/api/novels/${novelId.value}/story-bible/chat/messages`, {
      method: 'POST', body: JSON.stringify({ message, focusType: focus.type ?? null, focusId: focus.id ?? null }),
    })
    storyBibleChatMessages.value = conversation.messages
    storyBibleChatInput.value = ''
    scrollStoryBibleChatToBottom()
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '大纲顾问回复失败') }
  finally { sendingStoryBibleChat.value = false }
}

async function applyStoryBibleSuggestion(message: StoryBibleChatMessageDto, suggestionIndex: number) {
  const suggestion = message.suggestions[suggestionIndex]
  if (!suggestion) return
  const key = `${message.id}-${suggestionIndex}`
  if (applyingStoryBibleSuggestion.value) return
  applyingStoryBibleSuggestion.value = key
  try {
    const response = await requestJson<StoryBibleApplySuggestionDto>(`/api/story-bible/chat/messages/${message.id}/suggestions/${suggestionIndex}/apply`, { method: 'POST' })
    storyBibleChatMessages.value = response.conversation.messages
    await loadStoryBible(false)
    openAppliedStoryBibleResult(response.type, Number(response.result.id ?? suggestion.targetId))
    ElMessage.success(`${storyBibleSuggestionTypeLabel(suggestion.type)}资料已${suggestion.action === 'CREATE' ? '创建' : '更新'}`)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '资料建议应用失败') }
  finally { applyingStoryBibleSuggestion.value = '' }
}

function storyBibleSuggestionTypeLabel(type: StoryBibleSuggestionDto['type']) {
  return ({ CHARACTER: '人物', ORGANIZATION: '组织', WORLD_SETTING: '世界观', VOLUME: '分卷' })[type]
}

function openAppliedStoryBibleResult(type: StoryBibleSuggestionDto['type'], resultId: number) {
  if (!Number.isFinite(resultId)) return
  if (type === 'CHARACTER') {
    bibleTab.value = 'characters'
    const item = characters.value.find(candidate => candidate.id === resultId)
    if (item) editCharacter(item)
  } else if (type === 'ORGANIZATION') {
    bibleTab.value = 'organizations'
    const item = organizations.value.find(candidate => candidate.id === resultId)
    if (item) editOrganization(item)
  } else if (type === 'WORLD_SETTING') {
    bibleTab.value = 'world'
    const item = worldSettings.value.find(candidate => candidate.id === resultId)
    if (item) editWorldSetting(item)
  } else {
    bibleTab.value = 'volumes'
    const item = storyVolumes.value.find(candidate => candidate.id === resultId)
    if (item) editStoryVolume(item)
  }
}

async function openStoryBible() {
  const returnView = currentView.value === 'director' ? 'director' : 'editor'
  if (returnView === 'director' && !await flushDirectorPlanSave()) return
  if (returnView === 'editor' && !await flushSave()) return
  dismissInlineCompletion()
  storyBibleReturnView.value = returnView
  currentView.value = 'storyBible'
  bibleTab.value = 'foundation'
  resetCharacterForm()
  resetOrganizationForm()
  resetWorldSettingForm()
  await Promise.all([loadProjectOutline(), loadStoryBible(), loadStoryBibleChat()])
  resetStoryVolumeForm()
}

async function closeStoryBiblePage() {
  currentView.value = storyBibleReturnView.value
  if (storyBibleReturnView.value === 'editor') {
    await nextTick()
    resizeEditor()
  }
}

function setMemoryForm(memory?: ChapterMemoryDto) {
  chapterMemory.value = memory
  if (!memory) { memoryForm.value = undefined; memoryLocationsText.value = ''; memoryQuestionsText.value = ''; return }
  const form = JSON.parse(JSON.stringify(memory.content)) as ChapterMemoryContentDto
  form.skillVersion ||= 'legacy'
  form.locations ||= []
  form.keyEvents ||= []
  form.foreshadowings ||= []
  form.unresolvedQuestions ||= []
  form.identityReveals ||= []
  form.characters = (form.characters ?? []).map(item => ({ ...item, actions: item.actions ?? '', stateChange: item.stateChange ?? '', newKnowledge: item.newKnowledge ?? '', foreshadowings: item.foreshadowings ?? [], profileChanges: (item.profileChanges ?? []).map(change => ({ ...change, oldValue: change.oldValue ?? '', evidence: change.evidence ?? '', applyToProfile: Boolean(change.applyToProfile) })) }))
  form.scenes = form.scenes ?? form.keyEvents.map((event, index) => ({ sceneIndex: index + 1, title: event.event, boundaryType: 'HARD', continuityKey: event.event, timeSpan: '', locations: event.location ? [event.location] : [], characters: [], goal: event.cause ?? '', conflict: '', subEvents: [{ time: '', location: event.location ?? '', event: event.event }], result: event.result ?? '' }))
  form.importantFacts = form.importantFacts ?? [
    ...form.foreshadowings.map(item => ({ type: 'FORESHADOWING' as const, content: item.content, importance: item.importance })),
    ...form.unresolvedQuestions.map(content => ({ type: 'UNRESOLVED_THREAD' as const, content, importance: 'MEDIUM' as const })),
  ]
  memoryForm.value = form
  memoryLocationsText.value = memory?.content.locations.join('，') ?? ''
  memoryQuestionsText.value = memory?.content.unresolvedQuestions.join('\n') ?? ''
}

async function loadChapterMemoryIndex(showError = true) {
  if (!novelId.value) return
  try {
    const memories = await requestJson<ChapterMemoryDto[]>(`/api/agent/novels/${novelId.value}/chapter-memories`)
    chapterMemories.value = Object.fromEntries(memories.map(memory => [memory.chapterId, memory]))
  } catch (error) { if (showError) ElMessage.error(error instanceof Error ? error.message : '章节记忆状态加载失败') }
}

async function openChapterMemory() {
  const chapter = chapters.value[activeChapter.value]
  if (!chapter) return
  showChapterMemory.value = true
  loadingMemory.value = true
  memoryInstruction.value = ''
  try {
    const memory = await requestJson<ChapterMemoryDto | undefined>(`/api/agent/chapters/${chapter.id}/memory`)
    setMemoryForm(memory)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '章节记忆加载失败') }
  finally { loadingMemory.value = false }
}

async function extractChapterMemory() {
  const chapter = chapters.value[activeChapter.value]
  if (!chapter || extractingMemory.value || !editorContent.value.trim() || !await flushSave()) return
  extractingMemory.value = true
  try {
    const memory = await requestJson<ChapterMemoryDto>(`/api/agent/chapters/${chapter.id}/memory/extract`, {
      method: 'POST', body: JSON.stringify({ instruction: memoryInstruction.value.trim(), thinkingEnabled: memoryThinkingEnabled.value }),
    })
    setMemoryForm(memory)
    ElMessage.success(memory.providerMode === 'demo' ? '已生成演示记忆，请编辑确认' : '章节记忆已提取，请检查后确认')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '章节记忆提取失败') }
  finally { extractingMemory.value = false }
}

function normalizedMemoryContent(): ChapterMemoryContentDto | undefined {
  if (!memoryForm.value) return undefined
  const form = JSON.parse(JSON.stringify(memoryForm.value)) as ChapterMemoryContentDto
  form.summary = form.summary.trim()
  form.timeInfo = form.timeInfo?.trim() ?? ''
  form.plotProgress = form.plotProgress?.trim() ?? ''
  form.locations = memoryLocationsText.value.split(/[，,\n]/).map(item => item.trim()).filter(Boolean)
  form.unresolvedQuestions = memoryQuestionsText.value.split('\n').map(item => item.trim()).filter(Boolean)
  form.characters = form.characters.filter(item => item.name.trim()).map(item => ({ ...item, name: item.name.trim(), role: item.role?.trim() ?? '', actions: item.actions?.trim() ?? '', stateChange: item.stateChange?.trim() ?? '', newKnowledge: item.newKnowledge?.trim() ?? '', firstAppearance: Boolean(item.firstAppearance), foreshadowings: item.foreshadowings.map(value => value.trim()).filter(Boolean), profileChanges: item.profileChanges.filter(change => change.newValue.trim()).map(change => ({ ...change, oldValue: change.oldValue?.trim() ?? '', newValue: change.newValue.trim(), evidence: change.evidence?.trim() ?? '', applyToProfile: Boolean(change.applyToProfile) })) }))
  form.scenes = form.scenes.filter(scene => scene.title.trim() || scene.subEvents.some(item => item.event.trim())).map((scene, index) => ({ ...scene, sceneIndex: index + 1, title: scene.title.trim() || `场景 ${index + 1}`, boundaryType: index === 0 ? 'HARD' : scene.boundaryType, continuityKey: scene.continuityKey?.trim() || scene.title.trim() || `scene-${index + 1}`, timeSpan: scene.timeSpan?.trim() ?? '', locations: scene.locations.map(item => item.trim()).filter(Boolean), characters: scene.characters.map(item => item.trim()).filter(Boolean), goal: scene.goal?.trim() ?? '', conflict: scene.conflict?.trim() ?? '', subEvents: scene.subEvents.filter(item => item.event.trim()).map(item => ({ time: item.time?.trim() ?? '', location: item.location?.trim() ?? '', event: item.event.trim() })), result: scene.result?.trim() ?? '' }))
  form.importantFacts = form.importantFacts.filter(item => item.content.trim()).map(item => ({ ...item, content: item.content.trim() }))
  form.keyEvents = form.keyEvents.filter(item => item.event.trim()).map(item => ({ ...item, event: item.event.trim(), cause: item.cause?.trim() ?? '', result: item.result?.trim() ?? '', location: item.location?.trim() ?? '' }))
  form.foreshadowings = form.foreshadowings.filter(item => item.content.trim()).map(item => ({ ...item, content: item.content.trim() }))
  return form
}

async function confirmChapterMemory() {
  if (!chapterMemory.value || confirmingMemory.value) return
  const content = normalizedMemoryContent()
  if (!content?.summary) { ElMessage.warning('请填写本章核心概括'); return }
  confirmingMemory.value = true
  try {
    const memory = await requestJson<ChapterMemoryDto>(`/api/agent/chapter-memories/${chapterMemory.value.id}/confirm`, {
      method: 'PUT', body: JSON.stringify({ content, version: chapterMemory.value.version }),
    })
    setMemoryForm(memory)
    chapterMemories.value[memory.chapterId] = memory
    await loadStoryBible(false)
    ElMessage.success('章节记忆已确认；如该章已划入分卷，也已自动归纳到该卷剧情事实')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '章节记忆确认失败') }
  finally { confirmingMemory.value = false }
}

function addMemoryCharacter() { memoryForm.value?.characters.push({ name: '', role: '', actions: '', stateChange: '', newKnowledge: '', firstAppearance: false, foreshadowings: [], profileChanges: [] }) }
function addMemoryScene() { const scenes = memoryForm.value?.scenes; if (!scenes) return; scenes.push({ sceneIndex: scenes.length + 1, title: '', boundaryType: scenes.length ? 'HARD' : 'HARD', continuityKey: '', timeSpan: '', locations: [], characters: [], goal: '', conflict: '', subEvents: [], result: '' }) }
function addMemoryFact() { memoryForm.value?.importantFacts.push({ type: 'OTHER', content: '', importance: 'MEDIUM' }) }
function profileFieldLabel(field: CharacterProfileField) { return ({ DESCRIPTION: '身份与经历', PERSONALITY: '性格', GOAL: '目标', AFFILIATIONS: '所属组织/阵营', CURRENT_STATE: '当前状态', RELATIONSHIPS: '当前关系' } as Record<CharacterProfileField, string>)[field] }
function profileOperationLabel(operation: CharacterProfileChangeDto['operation']) { return ({ ADD: '追加', REPLACE: '替换', REMOVE: '移除' } as const)[operation] }
function confidenceLabel(confidence: CharacterProfileChangeDto['confidence']) { return ({ HIGH: '高', MEDIUM: '中', LOW: '低' } as const)[confidence] }
function splitMemoryList(value: string) { return value.split(/[，,\n]/).map(item => item.trim()).filter(Boolean) }
function parseSceneEvents(value: string): MemorySceneEventDto[] { return value.split('\n').map(line => { const [time = '', location = '', ...event] = line.split('｜'); return { time: time.trim(), location: location.trim(), event: event.join('｜').trim() } }).filter(item => item.event) }
function addMemoryEvent() { memoryForm.value?.keyEvents.push({ event: '', cause: '', result: '', location: '' }) }
function addMemoryForeshadowing() { memoryForm.value?.foreshadowings.push({ content: '', status: 'PLANTED', importance: 'MEDIUM' }) }

function resetBibleAiInput() { bibleAiInstruction.value = ''; bibleAiMode.value = 'GENERATE'; bibleAiTarget.value = 'AUTO' }
function resetCharacterForm() { characterForm.value = { name: '', aliases: '', role: '', affiliations: '', description: '', personality: '', goal: '', currentState: '', relationships: '' }; characterHistory.value = []; resetBibleAiInput() }
function editCharacter(item: CharacterDto) { characterForm.value = { id: item.id, name: item.name, aliases: item.aliases ?? '', role: item.role ?? '', affiliations: item.affiliations ?? '', description: item.description ?? '', personality: item.personality ?? '', goal: item.goal ?? '', currentState: item.currentState ?? '', relationships: item.relationships ?? '', version: item.version }; resetBibleAiInput(); bibleAiMode.value = 'EXPAND'; void loadCharacterHistory(item.id) }

async function loadCharacterHistory(characterId: number) {
  loadingCharacterHistory.value = true
  try { characterHistory.value = await requestJson<CharacterHistoryDto[]>(`/api/characters/${characterId}/history`) }
  catch (error) { characterHistory.value = []; ElMessage.error(error instanceof Error ? error.message : '人物历史加载失败') }
  finally { loadingCharacterHistory.value = false }
}

async function assistStoryBible(type: StoryBibleType) {
  if (!novelId.value || assistingBible.value) return
  const currentFields: Record<string, string> = type === 'CHARACTER'
    ? { name: characterForm.value.name, aliases: characterForm.value.aliases, role: characterForm.value.role, affiliations: characterForm.value.affiliations, description: characterForm.value.description, personality: characterForm.value.personality, goal: characterForm.value.goal, currentState: characterForm.value.currentState, relationships: characterForm.value.relationships }
    : type === 'ORGANIZATION'
      ? { name: organizationForm.value.name, aliases: organizationForm.value.aliases, type: organizationForm.value.type, description: organizationForm.value.description, goal: organizationForm.value.goal, structure: organizationForm.value.structure, relationships: organizationForm.value.relationships }
      : { category: worldSettingForm.value.category, title: worldSettingForm.value.title, content: worldSettingForm.value.content }
  if (!bibleAiInstruction.value.trim() && !Object.values(currentFields).some(value => value.trim())) {
    ElMessage.warning('请先填写一些关键信息')
    return
  }
  assistingBible.value = type
  try {
    const response = await requestJson<StoryBibleAssistDto>(`/api/novels/${novelId.value}/story-bible/assist`, {
      method: 'POST',
      body: JSON.stringify({ type, mode: bibleAiMode.value, instruction: bibleAiInstruction.value.trim(), currentFields,
        targetFields: bibleAiTarget.value === 'AUTO' ? [] : [bibleAiTarget.value] }),
    })
    const fields = response.fields
    if (type === 'CHARACTER') Object.assign(characterForm.value, {
      name: fields.name ?? characterForm.value.name, aliases: fields.aliases ?? characterForm.value.aliases, role: fields.role ?? characterForm.value.role,
      affiliations: fields.affiliations ?? characterForm.value.affiliations,
      description: fields.description ?? characterForm.value.description, personality: fields.personality ?? characterForm.value.personality,
      goal: fields.goal ?? characterForm.value.goal, currentState: fields.currentState ?? characterForm.value.currentState,
      relationships: fields.relationships ?? characterForm.value.relationships,
    })
    else if (type === 'ORGANIZATION') Object.assign(organizationForm.value, {
      name: fields.name ?? organizationForm.value.name, aliases: fields.aliases ?? organizationForm.value.aliases,
      type: fields.type ?? organizationForm.value.type, description: fields.description ?? organizationForm.value.description,
      goal: fields.goal ?? organizationForm.value.goal, structure: fields.structure ?? organizationForm.value.structure,
      relationships: fields.relationships ?? organizationForm.value.relationships,
    })
    else Object.assign(worldSettingForm.value, {
      category: fields.category ?? worldSettingForm.value.category, title: fields.title ?? worldSettingForm.value.title,
      content: fields.content ?? worldSettingForm.value.content,
    })
    const message = response.changeSummary.filter(Boolean).slice(0, 3).join('；') || 'AI 已完善资料，请检查后保存'
    ElMessage.success(response.providerMode === 'demo' ? `${message}` : message)
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : 'AI 完善故事资料失败') }
  finally { assistingBible.value = undefined }
}

async function saveCharacter() {
  if (!novelId.value || !characterForm.value.name.trim()) { ElMessage.warning('请填写人物姓名'); return }
  savingBible.value = true
  try {
    const form = characterForm.value
    const path = form.id ? `/api/characters/${form.id}` : `/api/novels/${novelId.value}/characters`
    const saved = await requestJson<CharacterDto>(path, { method: form.id ? 'PUT' : 'POST', body: JSON.stringify(form) })
    const index = characters.value.findIndex(item => item.id === saved.id)
    if (index >= 0) characters.value[index] = saved; else characters.value.push(saved)
    editCharacter(saved)
    ElMessage.success('人物档案已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '人物档案保存失败') }
  finally { savingBible.value = false }
}

async function deleteCharacter() {
  if (!characterForm.value.id) return
  try { await ElMessageBox.confirm(`确定删除人物“${characterForm.value.name}”吗？`, '删除人物', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/characters/${characterForm.value.id}`, { method: 'DELETE' })
    characters.value = characters.value.filter(item => item.id !== characterForm.value.id)
    resetCharacterForm(); ElMessage.success('人物已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '人物删除失败') }
}

function resetOrganizationForm() { organizationForm.value = { name: '', aliases: '', type: '', description: '', goal: '', structure: '', relationships: '' }; resetBibleAiInput() }
function editOrganization(item: OrganizationDto) { organizationForm.value = { id: item.id, name: item.name, aliases: item.aliases ?? '', type: item.type ?? '', description: item.description ?? '', goal: item.goal ?? '', structure: item.structure ?? '', relationships: item.relationships ?? '', version: item.version }; resetBibleAiInput(); bibleAiMode.value = 'EXPAND' }

async function saveOrganization() {
  if (!novelId.value || !organizationForm.value.name.trim()) { ElMessage.warning('请填写组织名称'); return }
  savingBible.value = true
  try {
    const form = organizationForm.value
    const path = form.id ? `/api/organizations/${form.id}` : `/api/novels/${novelId.value}/organizations`
    const saved = await requestJson<OrganizationDto>(path, { method: form.id ? 'PUT' : 'POST', body: JSON.stringify(form) })
    const index = organizations.value.findIndex(item => item.id === saved.id)
    if (index >= 0) organizations.value[index] = saved; else organizations.value.push(saved)
    editOrganization(saved)
    ElMessage.success('组织档案已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '组织档案保存失败') }
  finally { savingBible.value = false }
}

async function deleteOrganization() {
  if (!organizationForm.value.id) return
  try { await ElMessageBox.confirm(`确定删除组织“${organizationForm.value.name}”吗？`, '删除组织', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/organizations/${organizationForm.value.id}`, { method: 'DELETE' })
    organizations.value = organizations.value.filter(item => item.id !== organizationForm.value.id)
    resetOrganizationForm(); ElMessage.success('组织已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '组织删除失败') }
}

function resetWorldSettingForm() { worldSettingForm.value = { category: '', title: '', content: '' }; resetBibleAiInput() }
function editWorldSetting(item: WorldSettingDto) { worldSettingForm.value = { id: item.id, category: item.category, title: item.title, content: item.content, version: item.version }; resetBibleAiInput(); bibleAiMode.value = 'EXPAND' }

async function saveWorldSetting() {
  if (!novelId.value || !worldSettingForm.value.category.trim() || !worldSettingForm.value.title.trim() || !worldSettingForm.value.content.trim()) { ElMessage.warning('请填写完整的世界观设定'); return }
  savingBible.value = true
  try {
    const form = worldSettingForm.value
    const path = form.id ? `/api/world-settings/${form.id}` : `/api/novels/${novelId.value}/world-settings`
    const saved = await requestJson<WorldSettingDto>(path, { method: form.id ? 'PUT' : 'POST', body: JSON.stringify(form) })
    const index = worldSettings.value.findIndex(item => item.id === saved.id)
    if (index >= 0) worldSettings.value[index] = saved; else worldSettings.value.push(saved)
    editWorldSetting(saved); ElMessage.success('世界观设定已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '世界观设定保存失败') }
  finally { savingBible.value = false }
}

async function deleteWorldSetting() {
  if (!worldSettingForm.value.id) return
  try { await ElMessageBox.confirm(`确定删除设定“${worldSettingForm.value.title}”吗？`, '删除设定', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/world-settings/${worldSettingForm.value.id}`, { method: 'DELETE' })
    worldSettings.value = worldSettings.value.filter(item => item.id !== worldSettingForm.value.id)
    resetWorldSettingForm(); ElMessage.success('设定已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '设定删除失败') }
}

function resetStoryVolumeForm() {
  const last = storyVolumes.value[storyVolumes.value.length - 1]
  storyVolumeForm.value = {
    volumeNo: last ? last.volumeNo + 1 : 1,
    title: '', chapterStart: last?.chapterEnd ? last.chapterEnd + 1 : 1,
    chapterEnd: undefined, objective: '', retrospective: '', chapterFacts: '', futurePlan: '', keyTurningPoints: '',
    climax: '', endingHook: '', foreshadows: '', lockedBeats: '', status: 'GENERATED',
  }
  volumeAiInstruction.value = ''
  storyParts.value = []
  outlineNodeType.value = 'volume'
  selectedOutlineChapter.value = undefined
  resetStoryPartForm(false)
}

function editStoryVolume(item: StoryVolumeDto) {
  storyVolumeForm.value = {
    id: item.id, volumeNo: item.volumeNo, title: item.title, chapterStart: item.chapterStart,
    chapterEnd: item.chapterEnd ?? undefined, objective: item.objective ?? '', retrospective: item.retrospective ?? '',
    chapterFacts: item.chapterFacts ?? '',
    futurePlan: item.futurePlan ?? '', keyTurningPoints: item.keyTurningPoints ?? '', climax: item.climax ?? '',
    endingHook: item.endingHook ?? '', foreshadows: item.foreshadows ?? '', lockedBeats: item.lockedBeats ?? '',
    analyzedThroughChapterNo: item.analyzedThroughChapterNo ?? undefined, status: item.status,
    stale: item.stale, version: item.version,
  }
  volumeAiInstruction.value = ''
  outlineNodeType.value = 'volume'
  selectedOutlineChapter.value = undefined
  void loadStoryParts(item.id)
}

function resetStoryPartForm(select = true) {
  const last = storyParts.value[storyParts.value.length - 1]
  storyPartForm.value = {
    partNo: last ? last.partNo + 1 : 1,
    title: '',
    chapterStart: last?.chapterEnd ? last.chapterEnd + 1 : (chapters.value[0]?.chapterNo ?? 1),
    chapterEnd: undefined,
    objective: '', plan: '', retrospective: '', status: 'GENERATED',
  }
  if (select) outlineNodeType.value = 'part'
  selectedOutlineChapter.value = undefined
}

function editStoryPart(item: StoryPartDto) {
  storyPartForm.value = {
    id: item.id, partNo: item.partNo, title: item.title, chapterStart: item.chapterStart,
    chapterEnd: item.chapterEnd ?? undefined, objective: item.objective ?? '', plan: item.plan ?? '',
    retrospective: item.retrospective ?? '', status: item.status, version: item.version,
  }
  outlineNodeType.value = 'part'
  selectedOutlineChapter.value = undefined
}

function chaptersForPart(part: StoryPartDto) {
  const end = part.chapterEnd ?? Number.MAX_SAFE_INTEGER
  return chapters.value.filter(chapter => chapter.chapterNo >= part.chapterStart && chapter.chapterNo <= end)
}

function selectOutlineChapter(chapter: ChapterView) {
  selectedOutlineChapter.value = chapter
  outlineNodeType.value = 'chapter'
}

async function openSelectedChapterOutline() {
  const chapter = selectedOutlineChapter.value
  if (!chapter) return
  const index = chapters.value.findIndex(item => item.id === chapter.id)
  if (index < 0) return
  loadChapterIntoEditor(index)
  currentView.value = 'director'
}

async function loadStoryParts(volumeId: number) {
  try {
    const loaded = await requestJson<StoryPartDto[]>(`/api/story-volumes/${volumeId}/parts`)
    if (storyVolumeForm.value.id !== volumeId) return
    storyParts.value = loaded
    if (storyPartForm.value.id && loaded.some(item => item.id === storyPartForm.value.id)) {
      const selected = loaded.find(item => item.id === storyPartForm.value.id)
      if (selected) storyPartForm.value = { id: selected.id, partNo: selected.partNo, title: selected.title,
        chapterStart: selected.chapterStart, chapterEnd: selected.chapterEnd ?? undefined,
        objective: selected.objective ?? '', plan: selected.plan ?? '', retrospective: selected.retrospective ?? '',
        status: selected.status, version: selected.version }
    } else resetStoryPartForm(false)
  } catch (error) {
    storyParts.value = []
    resetStoryPartForm(false)
    ElMessage.error(error instanceof Error ? error.message : '卷内部分加载失败')
  }
}

async function saveStoryPart(status: 'GENERATED' | 'CONFIRMED') {
  const volumeId = storyVolumeForm.value.id
  const form = storyPartForm.value
  if (!volumeId || savingStoryPart.value) return
  if (!form.partNo || !form.title.trim() || !form.chapterStart) { ElMessage.warning('请填写部分序号、标题和起始章节'); return }
  if (form.chapterEnd && form.chapterEnd < form.chapterStart) { ElMessage.warning('部分结束章节不能早于起始章节'); return }
  savingStoryPart.value = true
  try {
    const path = form.id ? `/api/story-parts/${form.id}` : `/api/story-volumes/${volumeId}/parts`
    const saved = await requestJson<StoryPartDto>(path, { method: form.id ? 'PUT' : 'POST', body: JSON.stringify({
      partNo: form.partNo, title: form.title, chapterStart: form.chapterStart, chapterEnd: form.chapterEnd || null,
      objective: form.objective, plan: form.plan, retrospective: form.retrospective, status, version: form.version,
    }) })
    const index = storyParts.value.findIndex(item => item.id === saved.id)
    if (index >= 0) storyParts.value[index] = saved; else storyParts.value.push(saved)
    storyParts.value.sort((left, right) => left.partNo - right.partNo)
    editStoryPart(saved)
    ElMessage.success(status === 'CONFIRMED' ? '部分事件已确认' : '部分草稿已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '部分保存失败') }
  finally { savingStoryPart.value = false }
}

async function deleteStoryPart() {
  const form = storyPartForm.value
  if (!form.id) return
  try { await ElMessageBox.confirm(`确定删除第${form.partNo}部分“${form.title}”吗？`, '删除部分', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/story-parts/${form.id}`, { method: 'DELETE' })
    storyParts.value = storyParts.value.filter(item => item.id !== form.id)
    resetStoryPartForm()
    ElMessage.success('部分已删除，正文中的章节不会受影响')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '部分删除失败') }
}

function storyVolumeContent(): StoryVolumeContentDto {
  const form = storyVolumeForm.value
  return { title: form.title, objective: form.objective, retrospective: form.retrospective,
    futurePlan: form.futurePlan, keyTurningPoints: form.keyTurningPoints, climax: form.climax,
    endingHook: form.endingHook, foreshadows: form.foreshadows, lockedBeats: form.lockedBeats }
}

async function generateStoryVolume() {
  if (!novelId.value || generatingVolume.value) return
  const form = storyVolumeForm.value
  if (!form.volumeNo || !form.chapterStart) { ElMessage.warning('请先填写卷序和起始章节'); return }
  if (form.chapterEnd && form.chapterEnd < form.chapterStart) { ElMessage.warning('卷末章节不能早于卷首章节'); return }
  generatingVolume.value = true
  try {
    const response = await requestJson<StoryVolumeGenerateDto>(`/api/novels/${novelId.value}/story-volumes/generate`, {
      method: 'POST', body: JSON.stringify({ volumeNo: form.volumeNo, title: form.title,
        chapterStart: form.chapterStart, chapterEnd: form.chapterEnd || null,
        instruction: volumeAiInstruction.value.trim(), currentContent: storyVolumeContent() }),
    })
    Object.assign(storyVolumeForm.value, response.content, {
      analyzedThroughChapterNo: response.analyzedThroughChapterNo ?? undefined,
      status: 'GENERATED', stale: false,
    })
    ElMessage.success(response.changeSummary.filter(Boolean).slice(0, 3).join('；') || '本卷主线已生成，请检查后确认')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '分卷主线生成失败') }
  finally { generatingVolume.value = false }
}

async function saveStoryVolume(status: 'GENERATED' | 'CONFIRMED') {
  if (!novelId.value || savingBible.value) return
  const form = storyVolumeForm.value
  if (!form.volumeNo || !form.title.trim() || !form.chapterStart) { ElMessage.warning('请填写卷序、卷标题和起始章节'); return }
  if (form.chapterEnd && form.chapterEnd < form.chapterStart) { ElMessage.warning('卷末章节不能早于卷首章节'); return }
  if (status === 'CONFIRMED' && !form.objective.trim() && !form.futurePlan.trim()) {
    ElMessage.warning('确认前请至少填写本卷目标或剩余剧情安排'); return
  }
  savingBible.value = true
  try {
    const path = form.id ? `/api/story-volumes/${form.id}` : `/api/novels/${novelId.value}/story-volumes`
    const saved = await requestJson<StoryVolumeDto>(path, { method: form.id ? 'PUT' : 'POST',
      body: JSON.stringify({ volumeNo: form.volumeNo, title: form.title, chapterStart: form.chapterStart,
        chapterEnd: form.chapterEnd || null, objective: form.objective, retrospective: form.retrospective,
        futurePlan: form.futurePlan, keyTurningPoints: form.keyTurningPoints, climax: form.climax,
        endingHook: form.endingHook, foreshadows: form.foreshadows, lockedBeats: form.lockedBeats,
        analyzedThroughChapterNo: form.analyzedThroughChapterNo || null, status, version: form.version }) })
    const index = storyVolumes.value.findIndex(item => item.id === saved.id)
    if (index >= 0) storyVolumes.value[index] = saved; else storyVolumes.value.push(saved)
    storyVolumes.value.sort((left, right) => left.volumeNo - right.volumeNo)
    editStoryVolume(saved)
    ElMessage.success(status === 'CONFIRMED' ? '分卷主线已确认，后续创作将使用它' : '分卷主线草稿已保存')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '分卷主线保存失败') }
  finally { savingBible.value = false }
}

async function deleteStoryVolume() {
  const form = storyVolumeForm.value
  if (!form.id) return
  try { await ElMessageBox.confirm(`确定删除第${form.volumeNo}卷“${form.title}”吗？`, '删除分卷', { type: 'warning' }) } catch { return }
  try {
    await requestJson<void>(`/api/story-volumes/${form.id}`, { method: 'DELETE' })
    storyVolumes.value = storyVolumes.value.filter(item => item.id !== form.id)
    resetStoryVolumeForm(); ElMessage.success('分卷主线已删除')
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '分卷主线删除失败') }
}

async function selectChapter(index: number) {
  showMobileNav.value = false
  if (index === activeChapter.value) return
  if (!await flushSave()) return
  loadChapterIntoEditor(index)
}

async function addChapter() {
  if (creatingChapter.value || !novelId.value || !await flushSave()) return
  creatingChapter.value = true
  try {
    const created = await requestJson<ChapterDto>(`/api/novels/${novelId.value}/chapters`, {
      method: 'POST',
      body: JSON.stringify({ title: `新章节 ${chapters.value.length + 1}`, content: '' }),
    })
    chapters.value.push(toChapterView(created))
    dirty = false
    loadChapterIntoEditor(chapters.value.length - 1)
    saveState.value = '已保存'
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '新建章节失败')
  } finally {
    creatingChapter.value = false
  }
}

async function deleteChapter(chapter: ChapterView, index: number) {
  try {
    await ElMessageBox.confirm(
      `删除后将无法恢复“${chapter.title}”的正文内容，确定继续吗？`,
      '确认删除章节',
      { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning', distinguishCancelAndClose: true },
    )
  } catch {
    return
  }

  if (!await flushSave()) return
  deletingChapterId.value = chapter.id
  try {
    const activeChapterId = chapters.value[activeChapter.value]?.id
    await requestJson<void>(`/api/chapters/${chapter.id}`, { method: 'DELETE' })
    chapters.value.splice(index, 1)
    dirty = false

    if (chapters.value.length === 0 && novelId.value) {
      const replacement = await requestJson<ChapterDto>(`/api/novels/${novelId.value}/chapters`, {
        method: 'POST',
        body: JSON.stringify({ title: '第一章', content: '' }),
      })
      chapters.value.push(toChapterView(replacement))
      loadChapterIntoEditor(0)
    } else {
      const preservedIndex = chapters.value.findIndex(item => item.id === activeChapterId)
      loadChapterIntoEditor(preservedIndex >= 0 ? preservedIndex : Math.min(index, chapters.value.length - 1))
    }
    saveState.value = '已保存'
    ElMessage.success('章节已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除章节失败')
  } finally {
    deletingChapterId.value = undefined
  }
}

function useSelection() {
  prompt.value = `请结合当前章节的结尾续写：${editorContent.value.slice(-110)}`
}

async function generate() {
  generating.value = true
  result.value = ''
  ragDebug.value = undefined
  try {
    const lengthMap: Record<string, number> = { '简短': 80, '标准': 160, '详细': 260 }
    const data = await requestJson<CompletionDto>('/api/ai/completion', {
      method: 'POST',
      body: JSON.stringify({
        novelId: novelId.value,
        chapterId: chapters.value[activeChapter.value]?.id,
        cursorContext: editorContent.value.trim().slice(-6000),
        maxLength: lengthMap[generateLength.value] ?? 160,
        instruction: prompt.value.trim(),
        styleId: selectedWritingStyleId.value,
      }),
    })
    result.value = data.completion?.trim() ?? ''
    ragDebug.value = data.rag
    if (!result.value) throw new Error('AI没有返回续写内容')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'AI续写失败，请稍后重试')
  } finally {
    generating.value = false
  }
}

function clearQuickResult() {
  result.value = ''
  ragDebug.value = undefined
}

function memoryTypeLabel(type: string) {
  return ({ CHAPTER_STORYLINE: '故事线', SCENE: '场景', CHARACTER_CHAPTER: '人物', FORESHADOWING: '伏笔', UNRESOLVED_THREAD: '未解线索', IMPORTANT_ITEM: '重要物品', WORLD_RULE: '世界规则', LOCATION_STATE: '地点状态', RELATIONSHIP: '关系变化', TIME_MARKER: '时间标记', REVEAL: '真相揭露', OTHER: '重要信息' } as Record<string, string>)[type] ?? type
}

async function insertResult() {
  if (!result.value) return
  editorContent.value = `${editorContent.value}\n\n${result.value}`
  handleEditorInput()
  resizeEditor()
  await nextTick()
  editorRef.value?.focus()
  editorRef.value?.setSelectionRange(editorContent.value.length, editorContent.value.length)
  ElMessage.success('已插入当前章节')
}

async function exportBook() {
  if (!await flushSave()) return
  showExport.value = false
  ElMessage.success('作品已整理完成，正在导出')
}

function handleVisibilityChange() {
  if (document.visibilityState === 'hidden') {
    void flushDirectorPlanSave()
    void flushSave()
    if (currentView.value === 'projectPlanning') void flushProjectSave()
  }
}

onMounted(async () => {
  window.addEventListener('resize', updateViewportWidth)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  await restoreAuth()
  if (authUser.value) await initializeWorkspace()
  resizeEditor()
})
onUnmounted(() => {
  window.clearTimeout(saveTimer)
  window.clearTimeout(historyTimer)
  window.clearTimeout(directorPlanSaveTimer)
  window.clearTimeout(projectSaveTimer)
  window.clearTimeout(paneResizeUnlockTimer)
  window.clearTimeout(inlineCompletionTimer)
  inlineCompletionController?.abort()
  window.removeEventListener('resize', updateViewportWidth)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  void flushDirectorPlanSave()
  void flushSave()
  void flushProjectSave()
})
</script>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { margin: 0; }
:global(button), :global(textarea), :global(input) { font: inherit; }

.home-shell { min-height: 100vh; color: #282620; background: radial-gradient(circle at 78% 10%,rgba(132,105,225,.13),transparent 27%),#f4f3ef; font-family: Inter,"PingFang SC","Microsoft YaHei",sans-serif; }
.home-header { height: 72px; display: flex; align-items: center; justify-content: space-between; padding: 0 clamp(24px,5vw,72px); border-bottom: 1px solid #e4e0d8; background: rgba(255,255,252,.88); backdrop-filter: blur(14px); }
.home-header .brand { padding-left: 0; }.home-header .avatar-button { margin: 0; }
.home-main { width: min(1160px,calc(100% - 48px)); margin: 0 auto; padding: 72px 0; }
.home-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 30px; margin-bottom: 38px; }
.home-heading-side { display: flex; align-items: stretch; gap: 12px; }
.home-eyebrow { color: #775bdb; font-size: 11px; font-weight: 800; letter-spacing: 2.5px; }.home-heading h1 { margin: 8px 0 9px; font: 700 38px "STKaiti","KaiTi",serif; }.home-heading p { margin: 0; color: #817b72; font-size: 14px; }
.home-summary { min-width: 260px; display: grid; grid-template-columns: auto auto 1px auto auto; align-items: baseline; gap: 8px; padding: 16px 20px; border: 1px solid #e3ded5; border-radius: 13px; background: rgba(255,255,252,.75); }.home-summary strong { font-size: 22px; }.home-summary span { color: #89837a; font-size: 11px; }.home-summary i { width: 1px; height: 25px; margin: 0 8px; align-self: center; background: #ddd8cf; }
.create-novel-button { min-width: 126px; padding: 0 17px; border: 0; border-radius: 13px; color: white; background: linear-gradient(135deg,#755ada,#9072e9); cursor: pointer; box-shadow: 0 8px 20px rgba(112,84,206,.22); font-size: 13px; font-weight: 700; }.create-novel-button:hover { filter: brightness(1.05); transform: translateY(-1px); }.create-novel-button span { margin-right: 4px; font-size: 18px; vertical-align: -1px; }
.home-loading { min-height: 260px; display: flex; align-items: center; justify-content: center; gap: 10px; color: #776c8c; font-size: 14px; }
.novel-grid { display: grid; grid-template-columns: repeat(auto-fill,minmax(330px,1fr)); gap: 22px; }
.novel-home-card { min-height: 230px; display: grid; grid-template-columns: 112px 1fr; grid-template-rows: 1fr auto; gap: 0 22px; padding: 24px; border: 1px solid #dfdbd2; border-radius: 18px; background: rgba(255,255,252,.92); text-align: left; cursor: pointer; box-shadow: 0 10px 35px rgba(48,42,32,.05); transition: transform .2s,border-color .2s,box-shadow .2s; }.novel-home-card:hover { transform: translateY(-4px); border-color: #b8a9e9; box-shadow: 0 16px 42px rgba(77,60,133,.12); }
.home-cover { grid-row: 1 / 3; position: relative; width: 112px; height: 168px; display: flex; flex-direction: column; align-items: center; justify-content: center; overflow: hidden; border-radius: 6px 12px 12px 6px; color: #eee9dd; background: linear-gradient(150deg,#39434a,#11191e 72%); box-shadow: 8px 10px 22px rgba(28,34,38,.22); }.home-cover::before { content:""; position:absolute; inset:0; background:linear-gradient(105deg,rgba(255,255,255,.14),transparent 28%); }.home-cover b { font: 34px "STKaiti","KaiTi",serif; }.home-cover i { position: absolute; bottom: 16px; font-size: 7px; font-style: normal; letter-spacing: 2px; opacity: .58; }
.home-card-copy { min-width: 0; display: flex; flex-direction: column; }.home-card-copy small { align-self: flex-start; padding: 3px 7px; border-radius: 5px; color: #674fc0; background: #eee9ff; font-size: 10px; }.home-card-copy strong { margin: 12px 0 8px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; font-size: 20px; }.home-card-copy > span { min-height: 42px; color: #817b72; font-size: 12px; line-height: 1.7; }.home-card-copy em { margin-top: auto; color: #a19b91; font-size: 10px; font-style: normal; }
.continue-writing { align-self: end; display: inline-flex; align-items: center; justify-content: flex-end; gap: 5px; color: #674fc0; font-size: 12px; font-weight: 700; }
.novel-form { display: grid; gap: 18px; padding: 4px 2px; }.novel-form label { position: relative; display: grid; gap: 7px; }.novel-form label > span { color: #4c4841; font-size: 13px; font-weight: 700; }.novel-form label > span b { color: #d15c5c; }.novel-form input,.novel-form textarea { width: 100%; border: 1px solid #dcd7ce; border-radius: 9px; outline: none; background: #fffefa; color: #403c35; font-size: 13px; transition: border-color .18s,box-shadow .18s; }.novel-form input { height: 42px; padding: 0 12px; }.novel-form textarea { min-height: 92px; padding: 11px 12px; resize: vertical; line-height: 1.7; }.novel-form textarea.outline-input { min-height: 150px; }.novel-form input:focus,.novel-form textarea:focus { border-color: #927ce1; box-shadow: 0 0 0 3px rgba(119,91,219,.09); }.novel-form label small { position: absolute; right: 9px; bottom: 7px; color: #a9a39a; font-size: 9px; }

.studio-shell {
  --ink: #282620;
  --muted: #8b877d;
  --line: #e7e3da;
  --paper: #fffefa;
  --accent: #775bdb;
  height: 100vh;
  min-height: 680px;
  overflow: hidden;
  color: var(--ink);
  background: #f4f3ef;
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

button { color: inherit; }
.topbar { height: 64px; display: grid; grid-template-columns: 300px 1fr 330px; align-items: center; border-bottom: 1px solid var(--line); background: rgba(255,255,252,.94); box-shadow: 0 2px 16px rgba(56,48,34,.04); position: relative; z-index: 20; }
.brand { display: flex; align-items: center; gap: 10px; padding-left: 22px; }
.back-home-button { width: 31px; height: 31px; display: grid; place-items: center; border: 1px solid #ded9d0; border-radius: 8px; background: white; color: #6c665e; cursor: pointer; }.back-home-button:hover { border-color: #cbbfed; color: #674fc2; background: #f7f4ff; }
.brand-mark { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 11px 11px 11px 3px; background: linear-gradient(145deg,#7659dd,#9a7cf3); color: white; box-shadow: 0 7px 16px rgba(119,91,219,.25); font-family: Georgia,"STKaiti",serif; font-size: 20px; }
.brand-name { font-weight: 800; letter-spacing: 2px; line-height: 1.1; }
.brand-tagline { font-size: 10px; color: #aaa59c; margin-top: 3px; letter-spacing: .5px; }
.book-title { justify-self: center; display: flex; align-items: center; gap: 9px; font-size: 14px; font-weight: 650; }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: #63b986; box-shadow: 0 0 0 4px #e8f6ed; }
.saved { font-weight: 400; color: #aaa69d; font-size: 11px; margin-left: 5px; }
.top-actions { display: flex; justify-content: flex-end; align-items: center; gap: 7px; padding-right: 20px; }
.ghost-action { border: 0; background: transparent; height: 34px; padding: 0 11px; border-radius: 8px; display: flex; align-items: center; gap: 6px; color: #676259; cursor: pointer; font-size: 12px; }
.ghost-action:hover { background: #f3f1ed; color: var(--accent); }
.avatar-button { border: 0; width: 32px; height: 32px; border-radius: 50%; background: #292720; color: white; cursor: pointer; margin-left: 4px; font-size: 12px; }

.workspace { height: calc(100vh - 64px); display: grid; grid-template-columns: 260px minmax(430px, 1fr) 320px; }
.navigator { border-right: 1px solid var(--line); background: #faf9f6; display: flex; flex-direction: column; overflow: hidden; }
.pane-resizer { position: relative; z-index: 8; cursor: col-resize; background: #e9e6df; transition: background .18s; }
.pane-resizer::after { content: ""; position: absolute; top: 50%; left: 2px; width: 2px; height: 42px; border-radius: 2px; background: #bbb4aa; transform: translateY(-50%); opacity: .65; }
.pane-resizer:hover,.pane-resizer:active { background: #ddd7f2; }.pane-resizer:hover::after,.pane-resizer:active::after { background: var(--accent); opacity: 1; }
.novel-card { display: flex; align-items: center; gap: 10px; margin: 17px 14px 11px; padding: 10px; border: 1px solid #e8e3d9; border-radius: 12px; background: #fffefa; }
.cover-mini { width: 36px; height: 45px; flex: 0 0 auto; display: grid; place-items: center; color: #d8d1c0; background: linear-gradient(145deg,#29333b,#10171c); border-radius: 4px; font: 17px Georgia,"STKaiti",serif; box-shadow: 0 4px 10px rgba(30,38,43,.22); }
.novel-meta { min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.novel-meta strong { font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.novel-meta span { font-size: 12px; color: #8f8a81; white-space: nowrap; }
.more-button { margin-left: auto; border: 0; background: transparent; color: #aaa69d; cursor: pointer; letter-spacing: 1px; }
.story-bible-button { margin: 0 14px 10px; min-height: 42px; display: grid; grid-template-columns: 25px 1fr; grid-template-rows: 1fr 1fr; align-items: center; column-gap: 7px; padding: 7px 10px; border: 1px solid #ded7f4; border-radius: 9px; background: #f5f2ff; color: #624ab9; text-align: left; cursor: pointer; }.story-bible-button > span { grid-row: 1 / 3; font-size: 18px; }.story-bible-button strong { font-size: 12px; }.story-bible-button small { color: #9085aa; font-size: 10px; }
.project-planning-button { border-color: #eadcc5; color: #815f2c; background: #fff8ea; }.project-planning-button small { color: #a38b66; }
.chapter-memory-button { margin-top: -3px; border-color: #d7e4dd; color: #426f60; background: #f0f7f3; }.chapter-memory-button small { color: #7e958b; }
.nav-switch { margin: 0 14px 12px; padding: 3px; display: flex; border-radius: 9px; background: #eeece7; }
.nav-switch button { flex: 1; border: 0; padding: 7px; border-radius: 7px; background: transparent; color: #8d887f; cursor: pointer; font-size: 11px; }
.nav-switch button.active { background: white; color: #403d36; font-weight: 650; box-shadow: 0 1px 5px rgba(41,37,29,.08); }
.chapter-panel,.material-panel { flex: 1; overflow-y: auto; padding: 0 10px; scrollbar-width: none; -ms-overflow-style: none; }
.chapter-panel::-webkit-scrollbar,.material-panel::-webkit-scrollbar { display: none; }
.section-label { display: flex; align-items: center; justify-content: space-between; padding: 8px 9px 9px; color: #817c73; font-size: 12px; letter-spacing: .5px; }
.section-label button { border: 0; background: transparent; cursor: pointer; color: #99938a; }.section-label button:disabled { opacity: .55; cursor: wait; }
.section-label .add-chapter-button { padding: 6px 9px; border-radius: 7px; color: #674fc0; background: #eee9ff; font-size: 11px; font-weight: 650; }.add-chapter-button:hover { background: #e4dcff; }.add-chapter-button span { margin-right: 2px; font-size: 14px; }
.chapter-item { width: 100%; min-height: 49px; display: flex; align-items: center; gap: 10px; border: 0; border-radius: 9px; padding: 7px 9px; background: transparent; text-align: left; cursor: pointer; }
.chapter-item:hover { background: #f1efea; }
.chapter-item.active { background: #eeebfa; color: #5e45bd; }
.chapter-item:focus-visible { outline: 2px solid #b9aae9; outline-offset: -2px; }
.chapter-index { color: #9d978d; font: 11px Georgia,serif; }
.chapter-copy { display: flex; flex: 1; flex-direction: column; min-width: 0; gap: 4px; }
.chapter-copy strong { font-size: 13px; font-weight: 600; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.chapter-copy small { font-size: 11px; color: #928d84; }
.chapter-active-dot { width: 4px; height: 4px; border-radius: 50%; background: var(--accent); }
.delete-chapter-button { width: 27px; height: 27px; flex: 0 0 auto; display: grid; place-items: center; border: 0; border-radius: 6px; background: transparent; color: #a29b91; cursor: pointer; opacity: 0; transition: opacity .16s,color .16s,background .16s; }.chapter-item:hover .delete-chapter-button,.chapter-item:focus-within .delete-chapter-button { opacity: 1; }.delete-chapter-button:hover { color: #c34f4f; background: #fdecec; }.delete-chapter-button:disabled { opacity: .45; cursor: wait; }
.material-item { width: 100%; display: flex; align-items: center; gap: 10px; padding: 9px 7px; border: 0; border-bottom: 1px solid #eeece7; background: transparent; cursor: pointer; text-align: left; }
.material-icon { width: 30px; height: 30px; display: grid; place-items: center; border-radius: 8px; color: #5e5860; font-size: 11px; }
.material-item > span:nth-child(2) { flex: 1; display: flex; flex-direction: column; gap: 3px; }
.material-item strong { font-size: 11px; }.material-item small { font-size: 9px; color: #aaa69d; }
.material-item .el-icon { font-size: 11px; color: #b2ada4; }
.add-material { width: 100%; margin-top: 12px; padding: 9px; border: 1px dashed #cec8bd; border-radius: 8px; background: transparent; color: #8e897f; cursor: pointer; font-size: 10px; }
.nav-footer { padding: 14px 18px 16px; border-top: 1px solid var(--line); background: #f7f5f1; }
.word-goal { display: flex; justify-content: space-between; align-items: baseline; font-size: 12px; color: #716c64; }
.word-goal strong { color: #49453e; font-size: 14px; }.word-goal small { font-weight: 400; color: #8f8a80; }
.progress { height: 4px; margin: 9px 0; overflow: hidden; border-radius: 4px; background: #e6e2db; }.progress span { display: block; width: 64%; height: 100%; background: linear-gradient(90deg,#8c72e5,#b299f4); border-radius: inherit; }
.streak { font-size: 11px; color: #8f8a80; }

.editor-section { position: relative; min-width: 0; display: flex; flex-direction: column; background: #efeee9; overflow: hidden; }
.editor-toolbar { height: 54px; flex: 0 0 auto; border-bottom: 1px solid #e0ddd5; background: #f9f8f5; display: flex; align-items: center; padding: 0 18px; }
.panel-toggle { height: 32px; margin-right: 10px; padding: 0 10px; display: inline-flex; align-items: center; gap: 5px; border: 1px solid #ddd9d1; border-radius: 7px; background: white; color: #69645c; cursor: pointer; font-size: 12px; }.panel-toggle:hover,.panel-toggle.active { border-color: #cfc5ee; color: #674fc2; background: #f6f3ff; }.panel-toggle-ai { margin: 0 0 0 12px; }
.inline-completion-toggle { margin-left: 12px; white-space: nowrap; }
.format-actions { display: flex; align-items: center; gap: 3px; }
.format-actions button { width: 32px; height: 32px; border: 0; border-radius: 6px; background: transparent; color: #625d55; cursor: pointer; font-size: 15px; }.format-actions button:hover:not(:disabled) { background: #ece9e3; }.format-actions button:disabled { color: #c5c0b8; cursor: default; }
.format-actions i { width: 1px; height: 15px; background: #ddd9d1; margin: 0 6px; }.format-bold { font-weight: 800; }.format-italic { font-style: italic; font-family: Georgia,serif; }
.font-size-control { height: 32px; margin-left: 12px; padding-left: 12px; display: inline-flex; align-items: center; gap: 7px; border-left: 1px solid #ddd9d1; color: #625d55; font-size: 12px; white-space: nowrap; }.font-size-control select { height: 30px; padding: 0 25px 0 9px; border: 1px solid #d8d3ca; border-radius: 7px; outline: none; background: white; color: #504b44; cursor: pointer; font-size: 12px; }.font-size-control select:focus { border-color: #8e77df; box-shadow: 0 0 0 3px rgba(119,91,219,.08); }
.editor-stats { margin-left: auto; display: flex; gap: 15px; color: #817c73; font-size: 11px; }
.paper { width: min(820px, calc(100% - 60px)); height: calc(100% - 73px); margin: 18px auto 0; padding: 46px clamp(42px,7vw,86px) 30px; border-radius: 3px 3px 0 0; background: var(--paper); box-shadow: 0 4px 22px rgba(54,48,36,.07); overflow-y: scroll; scrollbar-width: thin; scrollbar-color: #aaa399 #ece9e2; }
.paper::-webkit-scrollbar { width: 9px; }
.paper::-webkit-scrollbar-track { background: #ece9e2; }
.paper::-webkit-scrollbar-thumb { border: 2px solid #ece9e2; border-radius: 8px; background: #aaa399; }
.paper::-webkit-scrollbar-thumb:hover { background: #888177; }
.chapter-kicker { text-align: center; color: #8f897e; font: 11px Georgia,serif; letter-spacing: 3px; }
.chapter-title-input { display: block; width: 100%; margin: 14px 0 18px; border: 0; outline: 0; background: transparent; text-align: center; color: #25231f; font: 700 29px "STKaiti","KaiTi",serif; }
.chapter-divider { display: flex; align-items: center; justify-content: center; gap: 11px; margin-bottom: 28px; }.chapter-divider span { width: 34px; height: 1px; background: #ddd7cb; }.chapter-divider i { color: #bdb5a6; font-size: 6px; font-style: normal; }
.manuscript-shell { position: relative; width: 100%; min-height: 520px; }.manuscript-shell.has-inline-completion { padding-bottom: 180px; }
.manuscript { display: block; width: 100%; min-height: 520px; resize: none; border: 0; outline: 0; overflow: hidden; background: transparent; color: #37332d; font: 16px/2.25 "Songti SC","SimSun",serif; letter-spacing: .6px; text-align: justify; }
.inline-completion-ghost { position: absolute; inset: 0; z-index: 2; overflow: visible; pointer-events: none; color: #37332d; background: var(--paper); font: 16px/2.25 "Songti SC","SimSun",serif; letter-spacing: .6px; text-align: justify; white-space: pre-wrap; overflow-wrap: break-word; word-break: break-word; }.inline-completion-prefix,.inline-completion-suffix { color: #37332d; }.inline-completion-copy { color: #8060d7; background: rgba(235,228,255,.72); border-radius: 3px; box-shadow: inset 2px 0 #8c6ce0; text-decoration: none; }.inline-completion-loading { color: #9a82df; font-family: Inter,"PingFang SC",sans-serif; font-size: .72em; letter-spacing: 0; animation: inline-completion-pulse 1.15s ease-in-out infinite; }
@keyframes inline-completion-pulse { 50% { opacity: .42; } }
.ai-panel { border-left: 1px solid var(--line); background: #fffefa; display: flex; flex-direction: column; overflow: hidden; }
.ai-header { height: 47px; flex: 0 0 auto; padding: 0 16px; display: flex; align-items: center; border-bottom: 1px solid var(--line); }
.ai-header > div { display: flex; align-items: center; gap: 7px; }.ai-header strong { font-size: 15px; }.sparkle { color: var(--accent); font-size: 17px; }.beta { padding: 2px 5px; border-radius: 4px; background: #eee9ff; color: #765bd5; font-size: 9px; font-weight: 700; letter-spacing: .5px; }
.agent-tabs { display: grid; grid-template-columns: repeat(5,1fr); gap: 3px; padding: 8px; border-bottom: 1px solid #eeeae3; background: #fcfbf8; }.agent-tabs button { min-width: 0; height: 32px; padding: 0 2px; border: 0; border-radius: 8px; color: #837d75; background: transparent; cursor: pointer; font-size: 9px; }.agent-tabs button.active { color: #6248be; background: #eee9ff; font-weight: 700; }
.organize-status-list { display: grid; gap: 8px; margin-bottom: 13px; }.organize-status-list article { display: grid; grid-template-columns: 30px 1fr auto; align-items: center; gap: 9px; padding: 11px; border: 1px solid #e2ded6; border-radius: 9px; background: white; }.organize-status-list article > span { width: 28px; height: 28px; display: grid; place-items: center; border-radius: 8px; color: #557d6e; background: #e9f4ef; }.organize-status-list article > div { min-width: 0; display: grid; gap: 3px; }.organize-status-list strong { color: #514c44; font-size: 11px; }.organize-status-list p { margin: 0; color: #8a837a; font-size: 9px; line-height: 1.45; }.organize-status-list button { padding: 6px 7px; border: 1px solid #d6d0c7; border-radius: 6px; color: #6750bd; background: #faf8ff; cursor: pointer; font-size: 8px; }.organize-button { margin-top: 4px; background: linear-gradient(135deg,#477566,#629382); }.organize-note { margin-top: 13px; padding: 11px; border-radius: 8px; color: #777067; background: #f5f3ef; }.organize-note strong { color: #5b554d; font-size: 10px; }.organize-note p { margin: 5px 0 0; font-size: 9px; line-height: 1.6; }
.ai-tabs { display: grid; grid-template-columns: repeat(4,1fr); padding: 10px 12px 7px; gap: 4px; border-bottom: 1px solid #eeeae3; }
.ai-tabs button { min-width: 0; border: 0; border-radius: 8px; padding: 7px 2px; background: transparent; color: #918c82; cursor: pointer; font-size: 10px; }.ai-tabs button span { margin-right: 3px; }.ai-tabs button.active { background: #eeeafb; color: #654bc1; font-weight: 650; }
.ai-scroll { flex: 1; overflow-y: auto; padding: 15px 16px 24px; }
.mode-intro { display: flex; gap: 11px; padding: 13px; border-radius: 11px; background: linear-gradient(135deg,#f4f0ff,#faf8ff); margin-bottom: 17px; }.mode-icon { width: 34px; height: 34px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 9px; background: white; color: #7357d7; box-shadow: 0 3px 9px rgba(110,86,190,.12); }.mode-intro strong { font-size: 13px; }.mode-intro p { margin: 5px 0 0; color: #7e7789; font-size: 11px; line-height: 1.55; }
.agent-flow { display: flex; align-items: center; justify-content: center; gap: 5px; margin: -4px 0 16px; color: #aaa49a; font-size: 9px; }.agent-flow span { padding: 4px 6px; border-radius: 5px; background: #f1efeb; }.agent-flow span.done { color: #5d48b3; background: #eee9ff; }.agent-flow i { font-style: normal; }
.field-label { display: flex; justify-content: space-between; margin: 0 1px 8px; color: #4f4a43; font-size: 12px; font-weight: 650; }.field-label span { color: #928d84; font-weight: 400; }
.prompt-box { border: 1px solid #ded9d0; border-radius: 10px; background: white; overflow: hidden; transition: border-color .2s,box-shadow .2s; }.prompt-box:focus-within { border-color: #9c87e5; box-shadow: 0 0 0 3px rgba(119,91,219,.08); }
.prompt-box textarea { width: 100%; height: 110px; padding: 12px; resize: none; border: 0; outline: 0; color: #4b463e; background: transparent; font-size: 13px; line-height: 1.7; }.prompt-box textarea::placeholder { color: #a9a49b; }
.compact-prompt { margin-bottom: 14px; }.compact-prompt textarea { height: 90px; }
.prompt-tools { display: flex; align-items: center; justify-content: space-between; padding: 8px 10px; border-top: 1px solid #efede8; }.prompt-tools button { border: 0; background: transparent; color: #6c54c3; cursor: pointer; font-size: 10px; }.prompt-tools span { color: #9d978e; font-size: 10px; }
.settings-row { display: flex; align-items: center; justify-content: space-between; margin: 16px 0; }.settings-row label { font-size: 12px; color: #4f4a43; font-weight: 650; }.segmented { padding: 2px; border-radius: 7px; background: #f0eee9; }.segmented button { border: 0; padding: 6px 9px; border-radius: 5px; background: transparent; color: #817b72; cursor: pointer; font-size: 10px; }.segmented button.active { background: white; color: #5f49ba; box-shadow: 0 1px 4px rgba(0,0,0,.07); }
.writing-style-picker { display: flex; align-items: end; gap: 8px; margin: 15px 0; }.writing-style-picker label { min-width: 0; flex: 1; display: grid; gap: 6px; }.writing-style-picker label > span { color: #4f4a43; font-size: 12px; font-weight: 650; }.writing-style-picker select { width: 100%; height: 36px; padding: 0 9px; border: 1px solid #ddd8cf; border-radius: 8px; color: #504a43; background: white; }.writing-style-picker > button { height: 36px; padding: 0 11px; border: 1px solid #d8d0eb; border-radius: 8px; color: #674fbd; background: #f5f1ff; cursor: pointer; }
.generate-button { width: 100%; height: 42px; border: 0; border-radius: 10px; background: linear-gradient(135deg,#795fdb,#8f70ea); color: white; cursor: pointer; box-shadow: 0 7px 16px rgba(113,86,208,.2); font-size: 13px; font-weight: 650; }.generate-button:hover { filter: brightness(1.04); transform: translateY(-1px); }.generate-button:disabled { opacity: .75; cursor: wait; }.generate-button span { margin-right: 5px; }
.plan-card { margin-top: 14px; padding-bottom: 12px; border: 1px solid #ded7f1; border-radius: 11px; background: #fff; overflow: hidden; }.plan-card .result-head { margin-bottom: 4px; background: #faf8ff; }.plan-card .result-head small { color: #8d82a4; font-size: 9px; font-weight: 400; }.plan-card > label { display: grid; gap: 5px; padding: 7px 11px 0; }.plan-card > label > span { color: #625c55; font-size: 10px; font-weight: 650; }.plan-card input,.plan-card textarea { width: 100%; padding: 8px 9px; border: 1px solid #e0dcd4; border-radius: 7px; outline: none; color: #4c473f; background: #fffefa; font-size: 11px; line-height: 1.55; resize: vertical; }.plan-card textarea { min-height: 58px; }.plan-card input:focus,.plan-card textarea:focus { border-color: #927ce1; }.plan-card .settings-row { padding: 0 11px; }.approve-button { width: calc(100% - 22px); margin: 0 11px; background: linear-gradient(135deg,#315f54,#4b8577); box-shadow: 0 7px 16px rgba(49,95,84,.18); }
.director-loading { display: flex; align-items: center; justify-content: center; gap: 6px; margin-bottom: 12px; padding: 13px; border-radius: 9px; color: #817795; background: #f7f4fc; font-size: 10px; }.director-chat { display: grid; gap: 8px; max-height: 310px; margin-bottom: 12px; padding: 2px 3px 2px 0; overflow-y: auto; }.director-chat article { width: 92%; padding: 9px 10px; border-radius: 10px; }.director-chat article.user { justify-self: end; color: white; background: linear-gradient(135deg,#7560cb,#8c72df); }.director-chat article.assistant { justify-self: start; border: 1px solid #e5dfef; color: #5d5765; background: white; }.director-chat article > div { display: flex; align-items: center; justify-content: space-between; gap: 7px; }.director-chat article strong { font-size: 9px; }.director-chat article button { padding: 2px 5px; border: 0; border-radius: 5px; color: #705abf; background: #f0ebff; cursor: pointer; font-size: 8px; }.director-chat article p { margin: 6px 0 0; font-size: 10px; line-height: 1.65; white-space: pre-wrap; }.director-chat article ul { display: grid; gap: 3px; margin: 7px 0 0; padding: 7px 7px 7px 20px; border-radius: 6px; color: #766d82; background: #f8f5fc; font-size: 8px; line-height: 1.45; }
.director-waiting { margin-top: 7px; color: #8a8292; font-size: 9px; line-height: 1.5; text-align: center; }.director-error { position: relative; margin-top: 9px; padding: 9px 30px 9px 10px; border: 1px solid #efc6c6; border-radius: 8px; color: #8f4a4a; background: #fff6f6; }.director-error strong { font-size: 10px; }.director-error p { margin: 4px 0 0; font-size: 9px; line-height: 1.5; }.director-error button { position: absolute; top: 7px; right: 7px; border: 0; color: #a06a6a; background: transparent; cursor: pointer; font-size: 8px; }
.agent-steps { margin-top: 14px; padding: 11px; border: 1px solid #e4e0d8; border-radius: 10px; background: #faf9f6; }.agent-steps > strong { display: block; margin-bottom: 8px; color: #5b564e; font-size: 11px; }.agent-steps > div { display: flex; gap: 7px; padding: 5px 0; }.agent-steps > div > span { width: 17px; height: 17px; flex: 0 0 auto; display: grid; place-items: center; border-radius: 50%; color: white; background: #69a68d; font-size: 9px; }.agent-steps p { display: grid; gap: 2px; margin: 0; }.agent-steps b { color: #5d5850; font-size: 10px; }.agent-steps small { color: #928c83; font-size: 9px; line-height: 1.45; }.agent-draft-card p { white-space: pre-wrap; max-height: 360px; overflow-y: auto; }
.rewrite-intro { margin-bottom: 13px; }.selection-card { margin-bottom: 14px; border: 1px solid #d8d0ef; border-radius: 10px; overflow: hidden; background: #fbfaff; }.selection-card > div { height: 34px; display: flex; align-items: center; justify-content: space-between; padding: 0 10px; border-bottom: 1px solid #e8e2f6; }.selection-card strong { color: #604bb3; font-size: 11px; }.selection-card button { border: 0; background: transparent; color: #9186a9; cursor: pointer; font-size: 10px; }.selection-card p { max-height: 130px; margin: 0; padding: 10px; overflow-y: auto; color: #585249; font: 11px/1.75 "Songti SC","SimSun",serif; white-space: pre-wrap; }.selection-empty { margin-bottom: 14px; display: grid; grid-template-columns: 27px 1fr auto; align-items: center; gap: 8px; padding: 12px; border: 1px dashed #cfc6e8; border-radius: 10px; background: #faf8ff; }.selection-empty > span { width: 25px; height: 25px; display: grid; place-items: center; border-radius: 50%; color: white; background: #7b62d8; font-size: 11px; }.selection-empty p { display: grid; gap: 3px; margin: 0; }.selection-empty strong { color: #5a544d; font-size: 11px; }.selection-empty small { color: #969087; font-size: 9px; }.selection-empty button { padding: 6px 8px; border: 0; border-radius: 6px; color: #654dbd; background: #eee9ff; cursor: pointer; font-size: 9px; }.rewrite-label { margin-top: 3px; }.rewrite-modes { display: grid; grid-template-columns: repeat(3,1fr); gap: 6px; margin-bottom: 15px; }.rewrite-modes button { min-width: 0; display: grid; gap: 3px; padding: 8px 5px; border: 1px solid #e0dcd4; border-radius: 8px; background: white; text-align: center; cursor: pointer; }.rewrite-modes button:hover,.rewrite-modes button.active { border-color: #9a86df; background: #f3efff; }.rewrite-modes strong { color: #5e5850; font-size: 10px; }.rewrite-modes button.active strong { color: #654cbd; }.rewrite-modes small { color: #9a948b; font-size: 8px; line-height: 1.35; }
.rewrite-thinking-options { display: grid; grid-template-columns: 1fr 1fr; gap: 7px; margin-bottom: 15px; }.rewrite-thinking-options button { min-width: 0; display: grid; gap: 4px; padding: 10px; border: 1px solid #dfdad2; border-radius: 9px; color: #625c55; background: white; text-align: left; cursor: pointer; }.rewrite-thinking-options button.active { border-color: #8f77dd; color: #6249bb; background: #f2edff; box-shadow: 0 0 0 2px rgba(112,82,201,.06); }.rewrite-thinking-options strong { font-size: 11px; }.rewrite-thinking-options small { color: #918a82; font-size: 9px; line-height: 1.45; }
.rewrite-result { margin-top: 14px; border: 1px solid #ddd7ec; border-radius: 11px; overflow: hidden; background: #fff; }.rewrite-result .result-head { background: #faf8ff; }.rewrite-result .result-head small { color: #6650ba; font-size: 9px; }.comparison-block { margin: 10px; padding: 10px; border-radius: 8px; }.comparison-block > strong { display: block; margin-bottom: 6px; font-size: 10px; }.comparison-block p { max-height: 220px; margin: 0; overflow-y: auto; color: #4e4941; font: 11px/1.75 "Songti SC","SimSun",serif; white-space: pre-wrap; }.comparison-block.original { border: 1px solid #eadada; background: #fffafa; }.comparison-block.original > strong { color: #a66363; }.comparison-block.revised { border: 1px solid #cee4d8; background: #f7fcf9; }.comparison-block.revised > strong { color: #3f7a61; }.comparison-arrow { height: 14px; color: #958e84; text-align: center; font-size: 12px; }.change-summary,.rewrite-warnings { display: grid; gap: 4px; margin: 10px; padding: 9px; border-radius: 7px; background: #f5f3ef; }.change-summary strong,.rewrite-warnings strong { color: #5a554d; font-size: 10px; }.change-summary span,.rewrite-warnings span { color: #817b72; font-size: 9px; line-height: 1.5; }.change-summary span { color: #527763; }.rewrite-warnings { background: #fff8e8; }.rewrite-warnings span { color: #8b7036; }.rewrite-actions { display: grid; grid-template-columns: 1fr 1.4fr; gap: 7px; padding: 0 10px 11px; }.rewrite-actions button { height: 35px; border: 1px solid #ded9d0; border-radius: 8px; background: white; color: #6c665e; cursor: pointer; font-size: 10px; }.rewrite-actions .insert-button { border-color: #3e7564; color: white; background: #3e7564; }.rewrite-actions button:disabled { opacity: .6; cursor: wait; }
.revision-history { margin-top: 16px; padding-top: 13px; border-top: 1px solid #e7e3dc; }.history-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 7px; }.history-title strong { color: #5a554e; font-size: 11px; }.history-title button { border: 0; background: transparent; color: #857e75; cursor: pointer; }.history-list { display: grid; gap: 5px; }.history-list > div { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 7px; padding: 8px; border-radius: 8px; background: #f6f4f0; }.history-mode { padding: 3px 5px; border-radius: 4px; color: #654ebb; background: #eae4fb; font-size: 8px; }.history-list p { min-width: 0; display: grid; gap: 3px; margin: 0; }.history-list p strong { overflow: hidden; color: #5c574f; font-size: 9px; white-space: nowrap; text-overflow: ellipsis; }.history-list p small { color: #999289; font-size: 8px; }.history-list > div > button { padding: 4px 6px; border: 1px solid #d4cec4; border-radius: 5px; background: white; color: #7863bd; cursor: pointer; font-size: 8px; }.empty-history { padding: 12px; color: #a19a91; background: #f7f5f1; border-radius: 7px; text-align: center; font-size: 9px; }
.consistency-intro { margin-bottom: 13px; }.consistency-focus { margin-bottom: 14px; }.consistency-focus textarea { height: 82px; }.check-button { background: linear-gradient(135deg,#3d6d62,#569081); box-shadow: 0 7px 16px rgba(48,105,90,.2); }.consistency-report { margin-top: 14px; }.score-card { display: flex; align-items: center; gap: 12px; padding: 12px; border: 1px solid #e1ddd5; border-radius: 11px; background: white; }.score-card > div { width: 66px; height: 66px; flex: 0 0 auto; display: grid; place-content: center; border: 5px solid; border-radius: 50%; text-align: center; }.score-card > div strong { font-size: 20px; line-height: 1; }.score-card > div small { margin-top: 3px; font-size: 7px; }.score-good { color: #3f8167; border-color: #a7d5c2!important; background: #f2fbf7; }.score-warn { color: #9a742e; border-color: #ead399!important; background: #fffaf0; }.score-bad { color: #b25252; border-color: #e6b1b1!important; background: #fff5f5; }.score-card > p { min-width: 0; display: grid; gap: 6px; margin: 0; }.score-card > p strong { color: #514c45; font-size: 12px; }.score-card > p span { color: #837d74; font-size: 10px; line-height: 1.55; }.issue-list { display: grid; gap: 9px; margin-top: 10px; }.issue-list article { padding: 11px; border: 1px solid #e1ddd5; border-left: 3px solid; border-radius: 9px; background: white; }.issue-list article.severity-high { border-left-color: #d45f5f; }.issue-list article.severity-medium { border-left-color: #d29b3e; }.issue-list article.severity-low { border-left-color: #6a91bf; }.issue-list article.resolved { opacity: .63; background: #f6f5f2; }.issue-head { display: flex; align-items: center; gap: 6px; }.issue-head > span { padding: 2px 5px; border-radius: 4px; color: #9c4d4d; background: #fdeaea; font-size: 8px; }.severity-medium .issue-head > span { color: #8b6727; background: #fff3d9; }.severity-low .issue-head > span { color: #4f729b; background: #eaf2fb; }.issue-head strong { color: #555047; font-size: 10px; }.issue-head small { margin-left: auto; color: #918a81; font-size: 8px; }.issue-list blockquote { margin: 8px 0; padding: 7px 8px; border-left: 2px solid #d8d2c8; color: #777067; background: #f8f6f2; font: 9px/1.55 "Songti SC","SimSun",serif; }.issue-list article > p { margin: 0 0 7px; color: #5e5850; font-size: 10px; line-height: 1.55; }.issue-suggestion { color: #777067; font-size: 9px; line-height: 1.5; }.issue-suggestion b { margin-right: 5px; color: #4f796a; }.issue-actions { display: flex; justify-content: flex-end; gap: 5px; margin-top: 9px; }.issue-actions button { padding: 5px 7px; border: 1px solid #dcd7cf; border-radius: 6px; color: #716a62; background: white; cursor: pointer; font-size: 8px; }.issue-actions button:disabled { opacity: .6; cursor: wait; }.issue-actions .fix-button { border-color: #4c7c6d; color: white; background: #4c7c6d; }.report-history { margin-top: 16px; padding-top: 13px; border-top: 1px solid #e7e3dc; }.report-history-list { display: grid; grid-template-columns: repeat(2,1fr); gap: 5px; }.report-history-list button { min-width: 0; display: grid; gap: 2px; padding: 8px; border: 1px solid #e1ddd5; border-radius: 7px; background: white; text-align: left; cursor: pointer; }.report-history-list button.active { border-color: #8f7bd7; background: #f4f0ff; }.report-history-list strong { color: #4d695f; font-size: 10px; }.report-history-list span,.report-history-list small { color: #948d84; font-size: 8px; }
.upload-zone { width: 100%; min-height: 72px; margin-bottom: 13px; border: 1px dashed #cfc7e9; border-radius: 9px; background: #fbfaff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; color: #725bc6; cursor: pointer; font-size: 9px; }.upload-zone small { color: #aaa3b3; font-size: 7px; }
.result-card { margin-top: 14px; border: 1px solid #e3ded4; border-radius: 11px; overflow: hidden; background: white; box-shadow: 0 5px 15px rgba(45,40,31,.05); }.result-head { height: 38px; display: flex; align-items: center; justify-content: space-between; padding: 0 11px; border-bottom: 1px solid #eeeae3; font-size: 11px; font-weight: 650; }.result-head i { display: inline-block; width: 5px; height: 5px; margin-right: 5px; border-radius: 50%; background: #7a60d9; }.result-head button { border: 0; background: transparent; cursor: pointer; color: #8f8980; }.result-card p { margin: 0; padding: 13px; color: #514c45; font: 13px/1.9 "Songti SC","SimSun",serif; }.result-actions { display: flex; justify-content: flex-end; gap: 7px; padding: 0 11px 11px; }.result-actions button { border: 1px solid #ded9d0; border-radius: 7px; padding: 7px 10px; background: white; color: #69645c; cursor: pointer; font-size: 10px; }.result-actions .insert-button { border-color: #765bd6; background: #765bd6; color: white; }
.rag-debug { margin: 0 11px 11px; border: 1px solid #e6e0f7; border-radius: 9px; background: #faf8ff; overflow: hidden; }.rag-debug summary { display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 9px 10px; color: #67579c; cursor: pointer; font-size: 11px; font-weight: 650; }.rag-debug summary small { color: #9189a8; font-weight: 400; }.rag-memory-list { display: grid; gap: 7px; padding: 0 8px 8px; }.rag-memory-list article { padding: 8px; border: 1px solid #e6e0f4; border-radius: 7px; background: white; }.rag-memory-list article.unused { opacity: .55; }.rag-memory-list article > div { display: flex; align-items: center; gap: 6px; }.rag-memory-list article strong { flex: 1; color: #5e527e; font-size: 10px; }.rag-memory-list article span,.rag-memory-list article i { color: #8e84a8; font-size: 9px; font-style: normal; }.rag-memory-list article i { padding: 2px 5px; border-radius: 8px; background: #eee9fb; }.rag-memory-list article p { padding: 5px 0 0; color: #68616f; font: 10px/1.55 system-ui,sans-serif; }.rag-empty { padding: 3px 10px 10px; color: #8c8497; font-size: 10px; }
.inspiration-card { margin-top: 15px; padding: 12px; border-radius: 10px; background: #f7f5f0; }.inspiration-title { color: #655e55; font-size: 11px; font-weight: 650; }.inspiration-title span { color: #c89132; margin-right: 4px; }.inspiration-card p { margin: 7px 0 0; color: #827c73; font-size: 11px; line-height: 1.7; }
.export-options { display: grid; gap: 10px; }.export-options button { display: flex; align-items: center; gap: 14px; padding: 13px; border: 1px solid #e5e1d8; border-radius: 10px; background: white; text-align: left; cursor: pointer; }.export-options button:hover { border-color: #9079df; background: #faf8ff; }.export-options strong { width: 44px; color: #6e55c9; }.export-options span { color: #8e887f; font-size: 12px; }
.bible-intro { margin: -4px 0 14px; color: #7d776f; font-size: 12px; line-height: 1.65; }.bible-layout { min-height: 410px; display: grid; grid-template-columns: 220px 1fr; gap: 18px; }.bible-list { max-height: 440px; padding-right: 7px; overflow-y: auto; border-right: 1px solid #ece8e1; }.bible-list button { width: 100%; display: grid; gap: 5px; padding: 11px; border: 0; border-radius: 8px; background: transparent; text-align: left; cursor: pointer; }.bible-list button:hover,.bible-list button.active { background: #f0ecfc; }.bible-list strong { color: #4e4942; font-size: 13px; }.bible-list span { color: #928c83; font-size: 11px; }.empty-bible { padding: 28px 14px; color: #9a948b; font-size: 12px; line-height: 1.7; text-align: center; }.bible-form { display: grid; grid-template-columns: 1fr 1fr; align-content: start; gap: 13px; }.bible-form label { display: grid; gap: 6px; }.bible-form label:nth-child(n+3) { grid-column: 1 / 3; }.bible-form label > span { color: #5c574f; font-size: 12px; font-weight: 650; }.bible-form input,.bible-form textarea { width: 100%; padding: 9px 10px; border: 1px solid #ded9d0; border-radius: 8px; outline: none; color: #48433c; font-size: 12px; line-height: 1.6; }.bible-form input { height: 38px; }.bible-form textarea { min-height: 66px; resize: vertical; }.bible-form textarea.large { min-height: 230px; }.bible-form input:focus,.bible-form textarea:focus { border-color: #927ce1; box-shadow: 0 0 0 3px rgba(119,91,219,.07); }.bible-actions { grid-column: 1 / 3; display: grid; grid-template-columns: auto 1fr auto auto; align-items: center; margin-top: 4px; }
.bible-ai-scope-bar { margin: -4px 0 12px; padding: 9px 11px; display: flex; align-items: center; justify-content: space-between; gap: 15px; border: 1px solid #e2dcf2; border-radius: 9px; background: #faf8ff; }.bible-ai-scope-bar > div { display: grid; gap: 2px; }.bible-ai-scope-bar strong { color: #6253a0; font-size: 11px; }.bible-ai-scope-bar small { color: #948ca1; font-size: 9px; }.bible-ai-scope-bar select { min-width: 210px; height: 34px; padding: 0 30px 0 9px; border: 1px solid #d8d0e9; border-radius: 7px; outline: none; color: #5f5669; background: white; font-size: 10px; }
.bible-ai-box { grid-column: 1 / 3; display: grid; gap: 9px; padding: 12px; border: 1px solid #dcd3f3; border-radius: 10px; background: linear-gradient(135deg,#f7f3ff,#fcfbff); }.bible-ai-box > div:first-child { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.bible-ai-box strong { color: #624bb5; font-size: 12px; }.bible-ai-box small { color: #958ba8; font-size: 10px; }.bible-ai-box > textarea { min-height: 72px; background: rgba(255,255,255,.86); }.bible-ai-actions { display: flex; align-items: center; gap: 7px; }.bible-ai-actions > button:not(.el-button) { height: 31px; padding: 0 10px; border: 1px solid #d8d0e9; border-radius: 7px; color: #746b7d; background: white; cursor: pointer; font-size: 10px; }.bible-ai-actions > button.active:not(.el-button) { border-color: #8065d2; color: #654db7; background: #eee8ff; }.bible-ai-actions .el-button { margin-left: auto; }
.volume-layout { min-height: 540px; }.volume-form { max-height: 590px; padding-right: 5px; overflow-y: auto; }.volume-form label { grid-column: auto; }.volume-form > label,.volume-form > .volume-two-columns,.volume-form > .volume-stale-warning { grid-column: 1 / 3; }.volume-range-grid { grid-column: 1 / 3; display: grid; grid-template-columns: 90px minmax(180px,1fr) 110px 110px; gap: 10px; }.volume-two-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }.volume-form label span em { margin-left: 8px; color: #8872ce; font-size: 10px; font-style: normal; font-weight: 500; }.volume-auto-facts textarea[readonly] { color: #625a70; border-color: #ded8e8; background: #f7f5fa; cursor: default; }.volume-auto-facts textarea[readonly]::placeholder { color: #aaa2b4; }.volume-stale-warning { padding: 9px 11px; border: 1px solid #e9c985; border-radius: 8px; color: #8a681e; background: #fff9e9; font-size: 11px; }.volume-ai-box .bible-ai-actions > span { color: #90879e; font-size: 10px; }.volume-new-button { margin-top: 8px; border: 1px dashed #cfc5e8 !important; color: #6f59ba; text-align: center !important; }.volume-actions { position: sticky; bottom: 0; padding: 10px 0 2px; background: #fff; }
.memory-dialog-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin: -5px 0 14px; }.memory-dialog-head > div { min-width: 0; }.memory-dialog-head > div > strong { color: #46413a; font-size: 15px; }.memory-dialog-head p { margin: 5px 0 0; color: #827b72; font-size: 11px; line-height: 1.6; }.memory-dialog-head > span { flex: 0 0 auto; padding: 5px 8px; border-radius: 6px; font-size: 10px; }.memory-none { color: #827b72; background: #efede9; }.memory-generated { color: #76591f; background: #fff1cc; }.memory-confirmed { color: #39745d; background: #e5f5ed; }.memory-stale { color: #a24f4f; background: #fde8e8; }.memory-loading { min-height: 300px; display: flex; align-items: center; justify-content: center; gap: 8px; color: #756d82; font-size: 13px; }.memory-extract-bar { display: grid; grid-template-columns: 1fr auto; gap: 8px; padding: 10px; border-radius: 9px; background: #f5f3ef; }.memory-extract-bar input { min-width: 0; height: 36px; padding: 0 10px; border: 1px solid #ddd8cf; border-radius: 7px; outline: none; font-size: 11px; }.memory-form { max-height: 57vh; margin-top: 12px; padding-right: 5px; overflow-y: auto; }.memory-form section { margin-bottom: 12px; padding: 13px; border: 1px solid #e4e0d8; border-radius: 10px; background: #fffefa; }.memory-form h4 { margin: 0 0 11px; color: #514c44; font-size: 12px; }.memory-form label > span { display: block; margin-bottom: 5px; color: #69635b; font-size: 10px; font-weight: 650; }.memory-form input:not([type=checkbox]),.memory-form textarea,.memory-form select { width: 100%; padding: 8px 9px; border: 1px solid #ddd8cf; border-radius: 7px; outline: none; color: #4d4840; background: white; font-size: 10px; line-height: 1.55; }.memory-form input:not([type=checkbox]) { height: 35px; }.memory-form textarea { resize: vertical; }.memory-form input:focus,.memory-form textarea:focus,.memory-form select:focus { border-color: #8d78d7; }.memory-wide { display: block; margin-bottom: 10px; }.memory-grid { display: grid; grid-template-columns: 1fr 160px; gap: 9px; margin-bottom: 10px; }.memory-section-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 9px; }.memory-section-title h4 { margin: 0; }.memory-section-title button { padding: 5px 7px; border: 0; border-radius: 5px; color: #654cba; background: #eee9ff; cursor: pointer; font-size: 9px; }.memory-row { display: grid; align-items: center; gap: 6px; margin-bottom: 6px; }.character-row { grid-template-columns: 110px 100px minmax(180px,1fr) auto 25px; }.first-appearance { display: flex!important; align-items: center; gap: 4px; color: #716a62; white-space: nowrap; font-size: 9px; }.first-appearance input { width: 14px; height: 14px; }.memory-row > button,.memory-event-card > button { width: 24px; height: 24px; border: 0; border-radius: 50%; color: #a76262; background: #fdeeee; cursor: pointer; }.memory-event-card { position: relative; display: grid; grid-template-columns: 1fr 150px; gap: 6px; margin-bottom: 7px; padding: 9px; border-radius: 8px; background: #f6f4f0; }.memory-event-card > button { position: absolute; top: -4px; right: -4px; }.foreshadow-row { grid-template-columns: minmax(220px,1fr) 100px 80px 25px; }.memory-empty-row { padding: 10px; border-radius: 7px; color: #9c958c; background: #f7f5f1; text-align: center; font-size: 9px; }.memory-question-textarea { width: 100%; }.memory-empty-state { min-height: 300px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #827b72; }.memory-empty-state > span { color: #5b8475; font-size: 35px; }.memory-empty-state strong { margin: 8px 0 5px; color: #514c44; font-size: 14px; }.memory-empty-state p { max-width: 420px; margin: 0; text-align: center; font-size: 11px; line-height: 1.6; }
.memory-section-note { margin: -3px 0 10px; color: #81796f; font-size: 10px; line-height: 1.6; }.profile-change-list { display: grid; gap: 7px; margin-top: 9px; padding-top: 9px; border-top: 1px dashed #ddd5ca; }.profile-change-list > strong { color: #574f70; font-size: 10px; }.profile-change-list article { display: grid; grid-template-columns: 105px 1fr; gap: 9px; padding: 9px; border-radius: 8px; background: #f5f2ff; }.profile-change-list article b { color: #5d4d96; font-size: 10px; }.profile-change-list article p { margin: 4px 0; color: #4f4942; font-size: 10px; line-height: 1.5; }.profile-change-list article small { color: #888078; font-size: 9px; }.profile-change-toggle { display: flex!important; align-items: flex-start; gap: 5px; }.profile-change-toggle input { margin-top: 2px; }.character-history-panel { grid-column: 1 / 3; max-height: 240px; overflow-y: auto; padding: 11px; border: 1px solid #e2ddd4; border-radius: 9px; background: #faf9f6; }.character-history-title { display: flex; justify-content: space-between; margin-bottom: 8px; }.character-history-title strong { color: #504943; font-size: 12px; }.character-history-title small { color: #918a82; font-size: 10px; }.character-history-panel article { padding: 8px 0; border-top: 1px solid #ece7df; }.character-history-panel article strong { color: #765fc0; font-size: 11px; }.character-history-panel article p { margin: 3px 0 0; color: #625c55; font-size: 10px; line-height: 1.55; }
.identity-reveal-review { margin-top: 10px; padding: 11px; border: 1px solid #dfd5fb; border-radius: 9px; background: #f8f5ff; text-align: left; }.identity-reveal-review > strong { color: #59458d; font-size: 12px; }.identity-reveal-review article { margin-top: 8px; padding: 8px; border-radius: 7px; background: white; }.identity-reveal-review article label { display: flex; align-items: center; gap: 7px; color: #4e4740; font-size: 11px; font-weight: 650; }.identity-reveal-review article p { margin: 5px 0 0 22px; color: #81796f; font-size: 9px; }.identity-reveal-review > small { display: block; margin-top: 7px; color: #8d857c; font-size: 9px; }
.run-center-summary { display: grid; grid-template-columns: repeat(3,1fr) auto; gap: 10px; margin-bottom: 14px; }.run-center-summary > div { display: grid; gap: 4px; padding: 12px 14px; border-radius: 9px; background: #f5f3ef; }.run-center-summary span { color: #837c73; font-size: 10px; }.run-center-summary strong { color: #4e4942; font-size: 20px; }.run-center-summary button { padding: 0 13px; border: 1px solid #dad4ca; border-radius: 9px; color: #6650b8; background: white; cursor: pointer; }.run-center-layout { min-height: 460px; max-height: 62vh; display: grid; grid-template-columns: 330px 1fr; overflow: hidden; border: 1px solid #e3ded6; border-radius: 11px; }.run-list { padding: 7px; overflow-y: auto; border-right: 1px solid #e8e4dd; background: #faf9f6; }.run-list button { position: relative; width: 100%; display: grid; grid-template-columns: 1fr auto; gap: 5px 8px; margin-bottom: 5px; padding: 11px; border: 1px solid transparent; border-radius: 8px; background: transparent; text-align: left; cursor: pointer; }.run-list button:hover,.run-list button.active { border-color: #d8cff0; background: #f0ebfd; }.run-operation { color: #6650b9; font-size: 10px; font-weight: 700; }.run-list i { padding: 2px 5px; border-radius: 4px; color: #4d7967; background: #e3f2eb; font-size: 8px; font-style: normal; }.run-list i.status-failed { color: #a54e4e; background: #fde9e9; }.run-list i.status-running { color: #8a6929; background: #fff0ce; }.run-list strong,.run-list small,.run-list time { grid-column: 1 / 3; }.run-list strong { color: #514c44; font-size: 12px; }.run-list small,.run-list time { color: #918a81; font-size: 9px; }.run-detail { padding: 18px; overflow-y: auto; }.run-detail-head { display: flex; justify-content: space-between; align-items: flex-start; padding-bottom: 14px; border-bottom: 1px solid #e8e4dd; }.run-detail-head h3 { margin: 3px 0; color: #4b463f; font-size: 18px; }.run-detail-head p,.run-detail-head small { margin: 0; color: #8b847b; font-size: 10px; }.run-detail-head > div:last-child { display: grid; text-align: right; }.run-detail-head > div:last-child strong { color: #684fc2; font-size: 24px; }.metric-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 8px; margin: 14px 0; }.metric-grid > div { display: grid; gap: 4px; padding: 10px; border-radius: 8px; background: #f7f5f1; }.metric-grid span { color: #918a81; font-size: 9px; }.metric-grid strong { color: #514c44; font-size: 13px; }.run-error { margin-bottom: 13px; padding: 10px; border-radius: 8px; color: #914848; background: #fff0f0; }.run-error strong { font-size: 10px; }.run-error p { margin: 5px 0 0; font-size: 10px; line-height: 1.5; }.call-timeline h4 { margin: 0 0 10px; color: #514c44; font-size: 12px; }.call-timeline article { display: grid; grid-template-columns: 28px 1fr; gap: 9px; padding: 10px 0; border-bottom: 1px solid #eeeae4; }.call-timeline article > span { width: 24px; height: 24px; display: grid; place-items: center; border-radius: 50%; color: white; background: #4b8a71; font-size: 10px; }.call-timeline article > span.call-failed { background: #c45d5d; }.call-timeline article strong { color: #554f47; font-size: 11px; }.call-timeline article p { margin: 4px 0 0; color: #898279; font-size: 9px; }.call-timeline article small { display: block; margin-top: 5px; color: #a34d4d; font-size: 9px; line-height: 1.5; }.run-center-loading,.run-center-empty { min-height: 260px; display: flex; align-items: center; justify-content: center; gap: 7px; color: #847d74; font-size: 12px; }.no-calls { padding: 20px; color: #9a938a; text-align: center; font-size: 10px; }
.eval-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 13px; }.eval-toolbar strong { color: #4e4942; font-size: 14px; }.eval-toolbar p { margin: 4px 0 0; color: #898279; font-size: 10px; }.eval-toolbar > button { height: 36px; padding: 0 13px; border: 0; border-radius: 8px; color: white; background: #7359cf; cursor: pointer; }.eval-layout { min-height: 540px; max-height: 68vh; display: grid; grid-template-columns: 260px 1fr; overflow: hidden; border: 1px solid #e3ded6; border-radius: 11px; }.eval-case-list { padding: 7px; overflow-y: auto; border-right: 1px solid #e6e1d9; background: #faf9f6; }.eval-case-list > button { width: 100%; display: grid; grid-template-columns: 1fr auto; gap: 5px; margin-bottom: 5px; padding: 11px; border: 1px solid transparent; border-radius: 8px; background: transparent; text-align: left; cursor: pointer; }.eval-case-list > button:hover,.eval-case-list > button.active { border-color: #d6ccef; background: #f0ebfd; }.eval-case-list span { color: #857d74; font-size: 8px; }.eval-case-list strong { grid-column: 1 / 3; color: #514b44; font-size: 11px; }.eval-case-list small { grid-column: 1 / 3; color: #918a80; font-size: 9px; }.eval-case-list small.eval-pass { color: #3f7b62; }.eval-case-list small.eval-fail { color: #a45151; }.eval-main { padding: 17px; overflow-y: auto; }.eval-section-head,.eval-case-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }.eval-section-head h3,.eval-case-head h3 { margin: 3px 0; color: #4c473f; font-size: 17px; }.eval-section-head button,.eval-case-head button { padding: 7px 10px; border: 1px solid #d9d3ca; border-radius: 7px; color: #6f675f; background: white; cursor: pointer; }.eval-case-head > div:first-child span,.eval-case-head p { margin: 0; color: #8e877e; font-size: 9px; }.eval-case-head > div:last-child { display: flex; gap: 7px; }.eval-case-head .eval-run-button { border-color: #7359cf; color: white; background: #7359cf; }.eval-form { display: grid; gap: 11px; }.eval-form > label,.eval-form-grid label { display: grid; gap: 5px; }.eval-form label > span { color: #625c54; font-size: 10px; font-weight: 650; }.eval-form input,.eval-form textarea { width: 100%; padding: 8px 9px; border: 1px solid #ddd8cf; border-radius: 7px; outline: none; color: #4d4840; font-size: 10px; line-height: 1.55; resize: vertical; }.eval-form input:focus,.eval-form textarea:focus { border-color: #8e78da; }.eval-form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.length-inputs { display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 6px; }.length-inputs i { color: #999188; font-style: normal; }.eval-form-actions { display: grid; grid-template-columns: auto 1fr auto; }.eval-report { display: grid; grid-template-columns: 120px 1fr; gap: 12px; }.eval-score { grid-row: span 2; min-height: 120px; display: grid; place-content: center; border-radius: 12px; color: #3d755e; background: #eaf6f0; text-align: center; }.eval-score.failed { color: #a34f4f; background: #fcecec; }.eval-score strong { font-size: 35px; line-height: 1; }.eval-score span { margin: 5px 0; font-size: 9px; }.eval-score i { font-size: 10px; font-style: normal; font-weight: 700; }.eval-metrics { display: grid; grid-template-columns: repeat(4,1fr); gap: 7px; }.eval-metrics > div { display: grid; gap: 4px; padding: 10px; border-radius: 8px; background: #f6f4f0; }.eval-metrics span { color: #928b82; font-size: 8px; }.eval-metrics strong { color: #514c44; font-size: 13px; }.eval-report > section,.eval-report > .run-error { grid-column: 1 / 3; }.eval-report section { padding: 11px; border: 1px solid #e5e0d8; border-radius: 9px; }.eval-report h4,.eval-history h4 { margin: 0 0 8px; color: #554f47; font-size: 11px; }.eval-generated { max-height: 190px; margin: 0; overflow-y: auto; color: #514c44; font: 11px/1.8 "Songti SC","SimSun",serif; white-space: pre-wrap; }.eval-violations { display: grid; gap: 5px; }.eval-violations span { padding: 6px 8px; border-radius: 6px; color: #9b4c4c; background: #fff0f0; font-size: 9px; }.eval-clean { margin: 0; color: #3f7b62; font-size: 10px; }.eval-feedback { margin: 0 0 7px; color: #625c54; font-size: 10px; line-height: 1.65; }.eval-report section > small { color: #999188; font-size: 8px; }.eval-empty-report { min-height: 320px; display: grid; place-items: center; color: #958e85; font-size: 11px; text-align: center; }.eval-history { grid-column: 1 / 3; margin-top: 4px; padding-top: 12px; border-top: 1px solid #e7e2db; }.eval-history button { width: 100%; display: grid; grid-template-columns: 80px 1fr auto; gap: 8px; padding: 8px; border: 0; border-radius: 6px; background: transparent; text-align: left; cursor: pointer; }.eval-history button:hover,.eval-history button.active { background: #f0ebfd; }.eval-history strong { color: #5e4bad; font-size: 10px; }.eval-history span,.eval-history small { color: #8e877e; font-size: 9px; }
.eval-toolbar > div:last-child { display: flex; gap: 7px; }.eval-toolbar > div:last-child button { height: 36px; padding: 0 13px; border: 0; border-radius: 8px; color: white; background: #7359cf; cursor: pointer; }.eval-toolbar > div:last-child button.eval-batch-button { border: 1px solid #d5cbed; color: #6650b7; background: #f4f0ff; }.eval-toolbar button:disabled { opacity: .6; cursor: wait; }
.memory-scene-card,.memory-character-card { margin-bottom: 9px; padding: 11px; border: 1px solid #e6e0d8; border-radius: 9px; background: #f8f6f2; }.memory-card-head { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; }.memory-card-head strong { flex: 1; color: #5b554d; font-size: 11px; }.memory-card-head select { width: 125px!important; height: 30px; padding: 3px 7px!important; }.memory-card-head > button,.memory-fact-row > button { width: 24px; height: 24px; border: 0; border-radius: 50%; color: #a76262; background: #fdeeee; cursor: pointer; }.memory-fact-row { display: grid; grid-template-columns: 125px minmax(260px,1fr) 75px 26px; align-items: center; gap: 7px; margin-bottom: 7px; }.memory-fact-row textarea { resize: vertical; }
.director-launcher { min-height: 360px; padding: 36px 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1px solid #ded6f3; border-radius: 14px; background: radial-gradient(circle at 50% 5%,#eee7ff 0,transparent 45%),#fcfbff; text-align: center; }.director-launcher-icon { width: 58px; height: 58px; display: grid; place-items: center; margin-bottom: 17px; border-radius: 18px; color: white; background: linear-gradient(135deg,#7358d2,#9377e8); box-shadow: 0 12px 26px rgba(103,76,196,.22); font-size: 26px; }.director-launcher small { color: #7864bd; font-size: 11px; font-weight: 700; letter-spacing: 1px; }.director-launcher h2 { margin: 8px 0; color: #393442; font-size: 22px; }.director-launcher > p { max-width: 280px; margin: 0 0 22px; color: #7d7585; font-size: 13px; line-height: 1.75; }.director-launcher > div { width: 100%; margin-bottom: 15px; padding: 12px; display: flex; justify-content: space-between; gap: 10px; border-radius: 9px; color: #6c6571; background: white; font-size: 11px; }.director-launcher > div span { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.director-launcher > div strong { flex: 0 0 auto; color: #6750bd; }.director-launcher > button { width: 100%; height: 44px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 10px; color: white; background: linear-gradient(135deg,#7359d1,#8e70e8); box-shadow: 0 8px 18px rgba(103,78,194,.22); cursor: pointer; font-size: 14px; font-weight: 700; }

.director-page-shell { height: 100vh; display: flex; flex-direction: column; overflow: hidden; color: #403b45; background: #f1efe9; }.director-page-header { height: 72px; flex: 0 0 auto; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 22px; padding: 0 30px; border-bottom: 1px solid #ded9d0; background: rgba(255,254,250,.96); }.director-page-brand,.director-page-actions,.director-page-context { display: flex; align-items: center; }.director-page-brand { gap: 11px; }.director-page-brand .back-home-button { margin-right: 3px; }.director-page-context { gap: 11px; }.director-page-context > span { max-width: 190px; overflow: hidden; color: #827b72; white-space: nowrap; text-overflow: ellipsis; font-size: 13px; }.director-page-context select { min-width: 210px; height: 38px; padding: 0 36px 0 13px; border: 1px solid #d9d3ca; border-radius: 9px; outline: none; color: #4f4942; background: white; font-size: 14px; }.director-page-actions { justify-content: flex-end; gap: 10px; }.director-page-actions .ghost-action { border: 1px solid #ddd7ce; background: white; }.director-return-button { height: 38px; padding: 0 17px; border: 0; border-radius: 9px; color: white; background: #7056cb; cursor: pointer; font-size: 13px; font-weight: 650; }
.director-page-main { min-height: 0; flex: 1; display: grid; grid-template-columns: minmax(390px,.85fr) minmax(540px,1.15fr); gap: 20px; padding: 22px clamp(22px,3vw,48px) 28px; }.director-conversation-panel,.director-plan-panel { min-height: 0; display: flex; flex-direction: column; overflow: hidden; border: 1px solid #ded9d1; border-radius: 16px; background: #fffefa; box-shadow: 0 8px 28px rgba(60,52,42,.06); }.director-panel-heading { padding: 23px 25px 19px; display: flex; justify-content: space-between; gap: 15px; border-bottom: 1px solid #ebe7e0; }.director-panel-heading small { color: #7860c5; font-size: 10px; font-weight: 750; letter-spacing: 1.5px; }.director-panel-heading h1 { margin: 6px 0 5px; color: #343038; font-size: 23px; }.director-panel-heading p { margin: 0; color: #817a83; font-size: 13px; line-height: 1.6; }.director-panel-heading > span { height: 28px; flex: 0 0 auto; padding: 0 10px; display: grid; place-items: center; border-radius: 14px; color: #897f90; background: #efede9; font-size: 11px; }.director-panel-heading > span.ready { color: #3d785f; background: #e4f3eb; }.director-panel-heading > span.failed { color: #a34f4f; background: #fbe8e8; }
.director-page-messages { min-height: 0; flex: 1; display: flex; flex-direction: column; gap: 13px; padding: 22px 25px; overflow-y: auto; background: #faf9f6; }.director-page-empty { min-height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #8a8290; text-align: center; }.director-page-empty > span { width: 58px; height: 58px; display: grid; place-items: center; border-radius: 18px; color: #795fce; background: #eee8ff; font-size: 25px; }.director-page-empty h2 { margin: 15px 0 7px; color: #504a54; font-size: 18px; }.director-page-empty p { max-width: 360px; margin: 0; font-size: 14px; line-height: 1.75; }.director-message { width: min(88%,560px); padding: 14px 16px; border-radius: 13px; }.director-message.user { align-self: flex-end; color: white; background: linear-gradient(135deg,#7059c8,#8970dc); border-bottom-right-radius: 4px; }.director-message.user.pending { opacity: .78; }.director-message.user.failed { background: linear-gradient(135deg,#9f6570,#b9757f); }.director-message.assistant { align-self: flex-start; border: 1px solid #e2dceb; color: #514b56; background: white; border-bottom-left-radius: 4px; }.director-message-meta { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.director-message-meta > div { display: flex; align-items: center; gap: 7px; }.director-message-meta > div > span { font-size: 11px; opacity: .82; }.director-message-meta strong { font-size: 13px; }.director-message-meta button { padding: 5px 9px; border: 0; border-radius: 6px; color: #674fbd; background: #eee9ff; cursor: pointer; font-size: 11px; }.director-message > p { margin: 8px 0 0; font-size: 14px; line-height: 1.75; white-space: pre-wrap; }.director-message ul { display: grid; gap: 5px; margin: 10px 0 0; padding: 10px 10px 10px 28px; border-radius: 8px; color: #716779; background: #f7f3fc; font-size: 12px; line-height: 1.55; }.director-thinking { display: flex; align-items: center; gap: 9px; padding: 12px 15px; border-radius: 10px; color: #766b82; background: #eee9f8; font-size: 13px; }
.director-message-meta > div { display: flex; align-items: center; gap: 6px; }.director-message.user .delete-turn-button { color: #fff; background: rgba(255,255,255,.17); }
.director-rag-loading { min-height: 220px; display: flex; align-items: center; justify-content: center; gap: 9px; color: #746987; }.director-rag-summary { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 14px; padding: 13px 15px; border-radius: 10px; background: #f4f0ff; }.director-rag-summary strong { color: #574678; font-size: 14px; }.director-rag-summary p { margin: 5px 0 0; color: #7e748c; font-size: 12px; line-height: 1.6; }.director-rag-summary > span { flex: 0 0 auto; color: #7058bc; font-size: 11px; }.director-rag-list { max-height: 55vh; padding: 0 4px 4px; overflow-y: auto; }.director-rag-list article { padding: 12px 13px; }.director-rag-list article strong { font-size: 12px; }.director-rag-list article span,.director-rag-list article i { font-size: 11px; }.director-rag-list article p { margin: 7px 0 0; font-size: 13px; line-height: 1.7; }.director-rag-empty { padding: 45px 20px; border-radius: 10px; color: #857b91; background: #f7f5fa; text-align: center; font-size: 13px; }
.director-composer { flex: 0 0 auto; padding: 16px 20px 18px; border-top: 1px solid #e7e2db; background: #fffefa; }.director-composer > textarea { width: 100%; height: 104px; padding: 13px 15px; resize: none; border: 1px solid #d9d3ca; border-radius: 11px; outline: none; color: #443f47; background: white; font-size: 15px; line-height: 1.7; }.director-composer > textarea:focus { border-color: #927ce0; box-shadow: 0 0 0 3px rgba(113,84,204,.09); }.director-composer > div:last-child { margin-top: 10px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }.director-composer > div:last-child > span { color: #9a929d; font-size: 11px; }.director-composer-actions { display: flex; align-items: center; gap: 8px; }.director-composer-actions > button { min-width: 112px; height: 40px; padding: 0 15px; border: 0; border-radius: 9px; cursor: pointer; font-size: 13px; font-weight: 650; }.director-composer-actions > .director-chat-button { border: 1px solid #d8d0e7; color: #6d5a9e; background: white; }.director-composer-actions > .director-plan-button { min-width: 142px; color: white; background: linear-gradient(135deg,#7156cd,#8b6fe2); }.director-composer button:disabled { opacity: .62; cursor: wait; }.director-page-error { position: relative; margin-bottom: 10px; padding: 10px 58px 10px 12px; display: grid; gap: 4px; border: 1px solid #edc5c5; border-radius: 9px; color: #934b4b; background: #fff5f5; font-size: 12px; }.director-page-error button { position: absolute; top: 9px; right: 9px; border: 0; color: #9b6262; background: transparent; cursor: pointer; }
.director-thinking-choice { margin-bottom: 10px; display: flex; align-items: center; gap: 7px; color: #6f6872; font-size: 12px; }.director-thinking-choice > span { margin-right: 2px; font-weight: 650; }.director-thinking-choice > button { height: 29px; padding: 0 11px; border: 1px solid #d9d1e7; border-radius: 8px; color: #6f657b; background: #fff; cursor: pointer; }.director-thinking-choice > button.active { border-color: #8569dc; color: #694bc5; background: #f2edff; }.director-thinking-choice > small { margin-left: 4px; color: #9a929d; }
.director-plan-panel { overflow-y: auto; }.director-plan-panel .plan-heading { flex: 0 0 auto; }.director-plan-empty { min-height: 0; flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; color: #8d8790; text-align: center; }.director-plan-empty > span { color: #8065d5; font-size: 35px; }.director-plan-empty h2 { margin: 13px 0 7px; color: #514b54; font-size: 19px; }.director-plan-empty p { max-width: 400px; margin: 0; font-size: 14px; line-height: 1.7; }.director-plan-board { display: grid; gap: 16px; padding: 22px 25px 28px; }.director-plan-board label { display: grid; gap: 7px; }.director-plan-board label > span { color: #59535b; font-size: 13px; font-weight: 700; }.director-plan-board input,.director-plan-board textarea { width: 100%; padding: 11px 12px; border: 1px solid #ddd7ce; border-radius: 9px; outline: none; color: #423d43; background: white; font-size: 14px; line-height: 1.65; resize: vertical; }.director-plan-board input { height: 42px; }.director-plan-board textarea { min-height: 82px; }.director-plan-board input:focus,.director-plan-board textarea:focus { border-color: #8e76df; box-shadow: 0 0 0 3px rgba(112,82,201,.08); }.director-plan-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.director-draft-settings { display: flex; align-items: center; gap: 13px; padding: 15px; border-radius: 11px; background: #f4f1ec; }.director-draft-settings > strong { color: #5a545b; font-size: 13px; }.director-draft-settings .segmented { margin-right: auto; }.director-draft-button { height: 40px; padding: 0 16px; border: 0; border-radius: 9px; color: white; background: linear-gradient(135deg,#376b5d,#4e8a78); cursor: pointer; font-size: 13px; font-weight: 650; }.director-draft-button:disabled { opacity: .65; cursor: wait; }.director-draft-result { border: 1px solid #d9d0ee; border-radius: 12px; overflow: hidden; background: white; }.director-draft-result > div { padding: 13px 15px; display: flex; align-items: center; justify-content: space-between; color: #5d49ae; background: #f5f1ff; }.director-draft-result > div button { border: 0; color: #85799d; background: transparent; cursor: pointer; }.director-draft-result > p { max-height: 420px; margin: 0; padding: 18px; overflow-y: auto; color: #474148; font: 15px/2 "Songti SC","SimSun",serif; white-space: pre-wrap; }.director-draft-result footer { padding: 12px 15px; display: flex; justify-content: flex-end; gap: 9px; border-top: 1px solid #ece7f3; }.director-draft-result footer button { height: 37px; padding: 0 13px; display: inline-flex; align-items: center; gap: 6px; border: 1px solid #d8d1df; border-radius: 8px; color: #665d6c; background: white; cursor: pointer; }.director-draft-result footer .insert-button { border-color: #7057ca; color: white; background: #7057ca; }
.director-style-row { display: flex; align-items: end; gap: 10px; padding: 13px 15px; border: 1px solid #e4ded4; border-radius: 11px; background: #fbfaf7; }.director-style-row label { min-width: 0; flex: 1; }.director-style-row select { width: 100%; height: 39px; padding: 0 10px; border: 1px solid #dad4cb; border-radius: 8px; background: white; }.director-style-row > button { height: 39px; padding: 0 13px; border: 1px solid #d7ceec; border-radius: 8px; color: #674fbd; background: #f4efff; cursor: pointer; }
.style-manager-layout { min-height: 590px; display: grid; grid-template-columns: 225px 1fr; gap: 18px; }.style-profile-list { padding-right: 14px; border-right: 1px solid #e4dfd7; }.style-profile-list > div { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }.style-profile-list > div button { border: 0; color: #674fbd; background: transparent; cursor: pointer; }.style-profile-list > button { width: 100%; display: grid; gap: 4px; margin-bottom: 7px; padding: 11px; border: 1px solid transparent; border-radius: 9px; color: #5a545d; background: #f7f5f1; text-align: left; cursor: pointer; }.style-profile-list > button.active { border-color: #cfc2f1; color: #5f47b6; background: #f0eaff; }.style-profile-list > button span,.style-profile-list > p { color: #918995; font-size: 12px; }.style-profile-editor { display: grid; align-content: start; gap: 12px; }.style-profile-editor > label { display: grid; gap: 6px; }.style-profile-editor label > span { color: #5b555d; font-size: 13px; font-weight: 650; }.style-profile-editor input,.style-profile-editor textarea { width: 100%; padding: 10px 11px; border: 1px solid #ddd7ce; border-radius: 8px; outline: none; resize: vertical; }.style-source-tabs { display: flex; gap: 7px; }.style-source-tabs button { padding: 8px 13px; border: 1px solid #ddd7ce; border-radius: 8px; color: #6c6570; background: white; cursor: pointer; }.style-source-tabs button.active { border-color: #876fd9; color: #6047b8; background: #f1ecff; }.style-ai-button { justify-self: start; padding: 9px 14px; border: 0; border-radius: 8px; color: white; background: #765bd0; cursor: pointer; }.style-form-actions { display: flex; align-items: center; gap: 8px; }.style-form-actions > span { flex: 1; }.style-form-actions button { padding: 9px 14px; border: 1px solid #dcd6cc; border-radius: 8px; background: white; cursor: pointer; }.style-form-actions button.primary { border-color: #7055ca; color: white; background: #7055ca; }.style-form-actions button.danger { border-color: #edc2c2; color: #b54c4c; }
.custom-draft-length { height: 40px; display: inline-flex !important; grid-template-columns: none !important; align-items: center; gap: 5px !important; color: #6c655e; font-size: 12px; }.custom-draft-length input { width: 88px !important; height: 40px !important; padding: 0 8px !important; text-align: center; }.icon-button { border: 0; background: transparent; cursor: pointer; }.mobile-only { display: none; }
.focus-mode .navigator,.focus-mode .ai-panel { display: none; }.focus-mode .workspace { grid-template-columns: 1fr; }.focus-mode .topbar { grid-template-columns: 1fr 1fr; }.focus-mode .brand { display: none; }.focus-mode .book-title { justify-self: start; padding-left: 24px; }

.project-page-shell { height: 100vh; min-height: 680px; display: flex; flex-direction: column; overflow: hidden; color: #342f29; background: radial-gradient(circle at 12% 0,rgba(220,169,77,.16),transparent 28%),radial-gradient(circle at 90% 12%,rgba(120,86,202,.12),transparent 30%),#f2efe8; font-family: Inter,"PingFang SC","Microsoft YaHei",sans-serif; }.project-page-shell > .project-page-header { flex: 0 0 auto; }
.project-page-main { width: min(1540px,calc(100% - 36px)); min-height: 0; flex: 1; display: flex; flex-direction: column; margin: 0 auto; padding: 20px 0 18px; }.project-hero { flex: 0 0 auto; display: flex; align-items: flex-end; justify-content: space-between; gap: 30px; margin-bottom: 15px; }.project-hero > div:first-child { max-width: 760px; }.project-hero > div:first-child > span { color: #91651e; font-size: 9px; font-weight: 800; letter-spacing: 2.2px; }.project-hero h1 { margin: 4px 0; color: #2d2925; font: 700 27px "STKaiti","KaiTi",serif; }.project-hero p { margin: 0; color: #7b746b; font-size: 11px; line-height: 1.55; }.project-status { min-width: 230px; display: grid; gap: 4px; padding: 10px 14px; border: 1px solid #e1d6c5; border-radius: 10px; background: rgba(255,252,245,.8); }.project-status strong { color: #735325; font-size: 12px; }.project-status span { color: #948571; font-size: 9px; }
.project-save-state { display: inline-block; margin-top: 10px; padding: 5px 8px; border-radius: 7px; color: #766f65; background: rgba(255,255,255,.65); font-size: 9px; }.project-save-state.saving,.project-save-state.pending { color: #765b23; background: #fff3d8; }.project-save-state.saved { color: #477160; background: #e9f5ef; }.project-save-state.failed { color: #a04e4e; background: #fdeaea; }.project-save-state.draft { color: #6448b5; background: #eee8ff; }
.project-workspace { display: grid; grid-template-columns: minmax(0,1fr) 410px; align-items: start; gap: 20px; }.project-editor-column { min-width: 0; }
.project-chat { position: sticky; top: 92px; height: calc(100vh - 116px); min-height: 650px; display: grid; grid-template-rows: auto auto minmax(0,1fr) auto; overflow: hidden; border: 1px solid #d9cfeb; border-radius: 18px; background: rgba(255,254,252,.98); box-shadow: 0 16px 48px rgba(63,51,83,.1); }.project-chat > header { padding: 15px 16px; border-bottom: 1px solid #eae4f1; background: linear-gradient(135deg,#f2edff,#fbf8ff); }.project-chat > header > div { display: flex; align-items: center; gap: 10px; }.project-chat > header > div > span { width: 32px; height: 32px; display: grid; place-items: center; border-radius: 9px; color: white; background: #7457cc; }.project-chat > header strong,.project-chat > header small { display: block; }.project-chat > header strong { color: #50435e; font-size: 13px; }.project-chat > header small { margin-top: 2px; color: #968b9f; font-size: 9px; }
.project-chat-source { padding: 11px 14px; border-bottom: 1px solid #ebe6ef; background: #faf8fc; }.project-chat-source > div { display: inline-grid; gap: 2px; }.project-chat-source strong { color: #63556f; font-size: 11px; }.project-chat-source small { color: #9a909f; font-size: 8px; }.project-chat-source label { float: right; display: flex; align-items: center; gap: 5px; color: #654eb0; font-size: 9px; font-weight: 700; cursor: pointer; }.project-chat-source p { clear: both; margin: 7px 0 0; color: #8b8291; font-size: 9px; line-height: 1.55; }
.project-chat-messages { padding: 15px; overflow-y: auto; scroll-behavior: smooth; }.project-chat-empty { min-height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; padding: 25px; color: #948a9a; text-align: center; }.project-chat-empty > span:first-child { color: #775ccb; font-size: 30px; }.project-chat-empty strong { color: #5a515f; }.project-chat-empty p { margin: 0; font-size: 11px; line-height: 1.7; }
.project-chat-message { margin-bottom: 15px; }.project-chat-message > strong { display: block; margin-bottom: 5px; color: #786989; font-size: 9px; }.project-chat-message > p { margin: 0; padding: 10px 12px; border-radius: 4px 12px 12px 12px; color: #504854; background: #f1edf7; font-size: 12px; line-height: 1.72; white-space: pre-wrap; }.project-chat-message > small { display: block; margin-top: 4px; color: #9a8da5; font-size: 8px; }.project-chat-message.user { padding-left: 38px; }.project-chat-message.user > strong { text-align: right; }.project-chat-message.user > p { border-radius: 12px 4px 12px 12px; color: white; background: #7357c8; }.project-chat-thinking { display: flex; align-items: center; gap: 7px; }
.project-suggestion-card { margin-top: 9px; padding: 12px; border: 1px solid #ddd3eb; border-radius: 11px; background: #fff; }.project-suggestion-card > div { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.project-suggestion-card > div strong { color: #59466f; font-size: 11px; }.project-suggestion-card > div span { padding: 3px 6px; border-radius: 5px; color: #654bb3; background: #eee8ff; font-size: 8px; }.project-suggestion-card dl { max-height: 260px; margin: 10px 0 0; overflow-y: auto; }.project-suggestion-card dt { margin-top: 7px; color: #9a8fa1; font-size: 8px; font-weight: 700; }.project-suggestion-card dd { max-height: 68px; margin: 3px 0 0; overflow: hidden; color: #5b535f; font-size: 10px; line-height: 1.55; white-space: pre-wrap; }.project-suggestion-card > button { width: 100%; margin-top: 11px; padding: 8px; border: 0; border-radius: 7px; color: white; background: #6d52c1; cursor: pointer; font-size: 10px; }.project-suggestion-card > button:disabled { color: #8f8593; background: #ece8ef; cursor: default; }
.project-chat > footer { padding: 12px; border-top: 1px solid #eae5ee; background: #fbfafc; }.project-chat > footer textarea { width: 100%; min-height: 90px; padding: 10px; border: 1px solid #dcd4e5; border-radius: 9px; outline: none; resize: none; font: 12px/1.6 inherit; }.project-chat > footer textarea:focus { border-color: #856bd3; box-shadow: 0 0 0 3px rgba(117,87,202,.08); }.project-chat-quick { display: grid; grid-template-columns: repeat(4,1fr); gap: 5px; margin-top: 7px; }.project-chat-quick button { padding: 6px 3px; border: 1px solid #ddd5e8; border-radius: 6px; color: #6b58a4; background: white; cursor: pointer; font-size: 8px; }.project-chat-send { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }.project-chat-send span { color: #9c949f; font-size: 8px; }.project-chat-send button { padding: 7px 16px; border: 0; border-radius: 7px; color: white; background: #7054c7; cursor: pointer; font-size: 10px; }.project-chat-send button:disabled { opacity: .5; cursor: wait; }
.project-ai-panel { display: grid; grid-template-columns: 260px minmax(300px,1fr) auto; align-items: center; gap: 16px; margin-bottom: 18px; padding: 18px; border: 1px solid #d9cbed; border-radius: 16px; background: linear-gradient(135deg,#faf7ff,#fffdf8); box-shadow: 0 12px 36px rgba(69,52,105,.07); }.project-ai-panel strong { color: #5d45ad; font-size: 15px; }.project-ai-panel p { margin: 5px 0 0; color: #84798d; font-size: 10px; line-height: 1.55; }.project-ai-panel textarea { min-height: 74px; padding: 10px 12px; border: 1px solid #ddd5e7; border-radius: 9px; outline: none; resize: vertical; font: 12px/1.6 inherit; }.project-ai-panel textarea:focus { border-color: #8a70d8; box-shadow: 0 0 0 3px rgba(117,87,202,.08); }.project-ai-actions { display: grid; grid-template-columns: repeat(2,auto); gap: 7px; }.project-ai-actions button { min-height: 34px; padding: 0 12px; border: 1px solid #d8cdeb; border-radius: 8px; color: #654db4; background: white; cursor: pointer; font-size: 10px; }.project-ai-actions button.primary { border-color: #7054c8; color: white; background: #7054c8; }.project-ai-actions button:disabled { opacity: .58; cursor: wait; }
.project-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); align-items: start; gap: 18px; }.project-card { display: grid; gap: 14px; padding: 22px; border: 1px solid #ded8cd; border-radius: 16px; background: rgba(255,254,250,.97); box-shadow: 0 14px 40px rgba(62,51,40,.06); }.project-card > header { display: flex; align-items: center; gap: 11px; padding-bottom: 13px; border-bottom: 1px solid #eee9df; }.project-card > header > span { width: 35px; height: 35px; display: grid; place-items: center; border-radius: 10px; color: #7c5a25; background: #f8ecd7; font: 700 11px Georgia,serif; }.project-card h2 { margin: 0; color: #413a32; font-size: 16px; }.project-card header p { margin: 4px 0 0; color: #928a81; font-size: 10px; }.project-card label { display: grid; gap: 6px; }.project-card label > span { color: #5c554d; font-size: 11px; font-weight: 700; }.project-card label > span em { float: right; color: #aaa096; font-size: 9px; font-style: normal; font-weight: 400; }.project-card input,.project-card textarea { width: 100%; padding: 10px 11px; border: 1px solid #dfd9cf; border-radius: 9px; outline: none; color: #47413a; background: #fffefa; font: 12px/1.65 inherit; resize: vertical; }.project-card input { height: 40px; }.project-card input:focus,.project-card textarea:focus { border-color: #9c7f48; box-shadow: 0 0 0 3px rgba(166,126,52,.08); }.project-field-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 12px; }.project-number-grid { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 10px; }.project-output-card { grid-row: span 2; }.project-save-bar { position: sticky; bottom: 18px; z-index: 8; display: flex; align-items: center; gap: 20px; margin-top: 20px; padding: 14px 16px 14px 19px; border: 1px solid #d8d0c3; border-radius: 14px; background: rgba(255,254,250,.94); box-shadow: 0 12px 38px rgba(53,43,32,.13); backdrop-filter: blur(14px); }.project-save-bar > div { flex: 1; display: grid; gap: 3px; }.project-save-bar strong { color: #574f46; font-size: 12px; }.project-save-bar span { color: #938b82; font-size: 10px; }

.story-library-shell { min-height: 100vh; color: #37323b; background: radial-gradient(circle at 86% 2%,rgba(128,94,218,.14),transparent 28%),linear-gradient(180deg,#f5f2eb,#efede8); font-family: Inter,"PingFang SC","Microsoft YaHei",sans-serif; }
.story-library-header { position: sticky; top: 0; z-index: 20; height: 72px; display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; gap: 20px; padding: 0 clamp(20px,3vw,48px); border-bottom: 1px solid rgba(214,207,197,.9); background: rgba(255,254,250,.92); backdrop-filter: blur(16px); box-shadow: 0 4px 20px rgba(65,53,42,.04); }.story-library-brand { display: flex; align-items: center; gap: 11px; }.story-library-brand .back-home-button { margin-right: 3px; }.story-library-book { display: grid; gap: 3px; text-align: center; }.story-library-book small { color: #9c948a; font-size: 9px; letter-spacing: 1px; }.story-library-book strong { max-width: 280px; overflow: hidden; color: #4e4852; white-space: nowrap; text-overflow: ellipsis; font-size: 14px; }.story-library-return { justify-self: end; height: 38px; padding: 0 17px; border: 0; border-radius: 9px; color: white; background: linear-gradient(135deg,#6e54c8,#896ce0); box-shadow: 0 7px 16px rgba(103,78,190,.18); cursor: pointer; font-size: 12px; font-weight: 700; }
.story-library-main { width: min(1500px,calc(100% - 48px)); margin: 0 auto; padding: 38px 0 54px; }.story-library-overview { display: flex; align-items: flex-end; justify-content: space-between; gap: 32px; margin-bottom: 25px; }.story-library-overview > div:first-child { max-width: 680px; }.story-library-overview > div:first-child > span { color: #7559ca; font-size: 10px; font-weight: 800; letter-spacing: 2.2px; }.story-library-overview h1 { margin: 8px 0 8px; color: #302c33; font: 700 32px "STKaiti","KaiTi",serif; }.story-library-overview p { margin: 0; color: #7e7780; font-size: 13px; line-height: 1.7; }.story-library-stats { display: grid; grid-template-columns: repeat(4,minmax(82px,1fr)); gap: 8px; }.story-library-stats article { min-width: 88px; padding: 12px 14px; display: grid; gap: 3px; border: 1px solid #e0d9ce; border-radius: 11px; background: rgba(255,254,250,.74); }.story-library-stats strong { color: #5f49ad; font-size: 20px; }.story-library-stats span { color: #918980; font-size: 9px; }
.story-library-card { min-height: 680px; padding: 18px 20px 22px 0; overflow: hidden; border: 1px solid #ddd6cc; border-radius: 18px; background: rgba(255,254,250,.97); box-shadow: 0 16px 48px rgba(63,51,39,.07); }.story-library-card > .bible-intro { margin: 0 0 14px 205px; padding: 10px 13px; border-radius: 9px; color: #766d80; background: #f6f2ff; }.story-library-loading { min-height: 540px; display: flex; align-items: center; justify-content: center; gap: 10px; border: 1px solid #ddd6cc; border-radius: 18px; color: #725f9d; background: #fffefa; }
.story-library-workspace { display: grid; grid-template-columns: minmax(0,1fr) 390px; align-items: start; gap: 18px; }.story-library-workspace > .story-library-card { min-width: 0; }
.story-bible-chat { position: sticky; top: 92px; height: calc(100vh - 116px); min-height: 620px; display: grid; grid-template-rows: auto minmax(0,1fr) auto; overflow: hidden; border: 1px solid #dcd3e9; border-radius: 18px; background: rgba(255,254,252,.98); box-shadow: 0 16px 48px rgba(63,51,83,.09); }.story-bible-chat > header { padding: 15px 16px; display: flex; align-items: center; justify-content: space-between; gap: 10px; border-bottom: 1px solid #ebe5f2; background: linear-gradient(135deg,#f4efff,#fbf9ff); }.story-bible-chat > header > div { min-width: 0; display: flex; align-items: center; gap: 9px; }.story-bible-chat > header > div > span { width: 31px; height: 31px; display: grid; place-items: center; border-radius: 9px; color: white; background: #7658d0; }.story-bible-chat > header strong,.story-bible-chat > header small { display: block; }.story-bible-chat > header strong { color: #51435f; font-size: 13px; }.story-bible-chat > header small { margin-top: 2px; color: #9b90a5; font-size: 9px; }.story-bible-chat > header em { max-width: 140px; padding: 5px 8px; overflow: hidden; border-radius: 10px; color: #6c55b2; background: white; white-space: nowrap; text-overflow: ellipsis; font-size: 9px; font-style: normal; }
.story-bible-chat-messages { padding: 15px; overflow-y: auto; scroll-behavior: smooth; }.story-bible-chat-empty { min-height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px; padding: 25px; color: #93899a; text-align: center; }.story-bible-chat-empty > span:first-child { color: #795ecb; font-size: 30px; }.story-bible-chat-empty strong { color: #5a505e; }.story-bible-chat-empty p { margin: 0; font-size: 11px; line-height: 1.7; }.story-bible-chat-empty button { padding: 8px 11px; border: 1px solid #d8cbed; border-radius: 8px; color: #6b51bb; background: #f7f3ff; cursor: pointer; font-size: 10px; }
.story-bible-chat-message { margin-bottom: 14px; }.story-bible-chat-message > strong { display: block; margin-bottom: 5px; color: #79678a; font-size: 9px; }.story-bible-chat-message > small { display: block; margin: -2px 0 5px; color: #9b8da7; font-size: 8px; }.story-bible-chat-message > p { margin: 0; padding: 10px 12px; border-radius: 4px 12px 12px 12px; color: #4f4853; background: #f2eef8; font-size: 12px; line-height: 1.7; white-space: pre-wrap; }.story-bible-chat-message.user { padding-left: 34px; }.story-bible-chat-message.user > strong { text-align: right; }.story-bible-chat-message.user > p { border-radius: 12px 4px 12px 12px; color: white; background: #7459ca; }
.character-suggestion-card { margin-top: 9px; padding: 12px; border: 1px solid #ded4ec; border-radius: 11px; background: #fff; }.character-suggestion-card > div { display: flex; align-items: center; justify-content: space-between; }.character-suggestion-card > div span { padding: 3px 6px; border-radius: 5px; color: #6549b4; background: #efe9ff; font-size: 8px; font-weight: 700; }.character-suggestion-card > div em { color: #998fa0; font-size: 9px; font-style: normal; }.character-suggestion-card h4 { margin: 9px 0 5px; color: #493e4d; font-size: 15px; }.character-suggestion-card > p { margin: 0 0 7px; color: #817684; font-size: 10px; line-height: 1.6; }.character-suggestion-card dl { margin: 0; display: grid; grid-template-columns: 34px 1fr; gap: 4px 6px; font-size: 10px; line-height: 1.5; }.character-suggestion-card dt { color: #9a8ca1; }.character-suggestion-card dd { margin: 0; color: #5f5663; }.character-suggestion-card > button { width: 100%; margin-top: 10px; padding: 8px; border: 0; border-radius: 7px; color: white; background: #6e53c2; cursor: pointer; font-size: 10px; }.character-suggestion-card > button:disabled { color: #8d8391; background: #ece8ef; cursor: default; }
.story-bible-chat > footer { padding: 12px; border-top: 1px solid #ebe5f0; background: #fbfafc; }.story-bible-chat > footer textarea { width: 100%; min-height: 82px; padding: 10px; border: 1px solid #dcd4e5; border-radius: 9px; outline: none; resize: none; font: 12px/1.6 inherit; }.story-bible-chat > footer textarea:focus { border-color: #856bd3; box-shadow: 0 0 0 3px rgba(117,87,202,.08); }.story-bible-chat > footer > div { margin-top: 7px; display: flex; align-items: center; justify-content: space-between; gap: 8px; }.story-bible-chat > footer span { color: #9a929e; font-size: 8px; line-height: 1.4; }.story-bible-chat > footer button { flex: 0 0 auto; padding: 7px 13px; border: 0; border-radius: 7px; color: white; background: #7054c7; cursor: pointer; font-size: 10px; }.story-bible-chat > footer button:disabled { opacity: .55; cursor: wait; }
:deep(.story-library-tabs.el-tabs--left) { align-items: stretch; }.story-library-tabs { min-height: 600px; }:deep(.story-library-tabs > .el-tabs__header.is-left) { width: 180px; margin-right: 24px; padding: 4px 12px; border-right: 1px solid #ebe5dc; }:deep(.story-library-tabs .el-tabs__nav-wrap.is-left::after) { display: none; }:deep(.story-library-tabs .el-tabs__item.is-left) { height: 48px; margin: 3px 0; padding: 0 14px; justify-content: flex-start; border-radius: 9px; color: #6d6670; font-size: 12px; font-weight: 650; }:deep(.story-library-tabs .el-tabs__item.is-left:hover) { color: #684dbd; background: #f7f3ff; }:deep(.story-library-tabs .el-tabs__item.is-left.is-active) { color: #5f43b4; background: linear-gradient(135deg,#eee8ff,#f7f4ff); }:deep(.story-library-tabs .el-tabs__active-bar.is-left) { display: none; }:deep(.story-library-tabs > .el-tabs__content) { min-width: 0; overflow: visible; }
.story-library-card .bible-ai-scope-bar { margin: 0 0 15px 204px; }.story-library-tabs .bible-layout { min-height: 580px; grid-template-columns: 260px minmax(0,1fr); gap: 24px; }.story-library-tabs .bible-list { max-height: calc(100vh - 310px); min-height: 520px; padding: 6px 14px 6px 5px; }.story-library-tabs .bible-list button { padding: 13px; }.story-library-tabs .bible-list strong { font-size: 13px; }.story-library-tabs .bible-list span { font-size: 11px; line-height: 1.5; }.story-library-tabs .bible-form { padding: 6px 8px 30px 0; gap: 16px; }.story-library-tabs .bible-form label > span { font-size: 13px; }.story-library-tabs .bible-form input,.story-library-tabs .bible-form textarea { padding: 10px 12px; font-size: 13px; }.story-library-tabs .bible-form input { height: 42px; }.story-library-tabs .bible-form textarea { min-height: 82px; }.story-library-tabs .bible-form textarea.large { min-height: 360px; }.story-library-tabs .volume-form { max-height: none; overflow: visible; }.story-library-tabs .volume-layout { min-height: 720px; }.story-library-tabs .volume-ai-box { padding: 16px; }.story-library-tabs .volume-actions { padding: 14px 0 4px; }

@media (max-width: 1250px) {
  .project-workspace { grid-template-columns: 1fr; }.project-chat { position: static; height: 760px; }
}
@media (max-width: 1100px) {
  .story-library-main { width: calc(100% - 28px); }.story-library-overview { align-items: flex-start; flex-direction: column; }.story-library-stats { width: 100%; }.story-library-workspace { grid-template-columns: 1fr; }.story-bible-chat { position: static; height: 720px; }.story-library-tabs .bible-layout { grid-template-columns: 220px minmax(0,1fr); }.volume-range-grid { grid-template-columns: 80px 1fr 105px 105px; }
  .director-page-header { grid-template-columns: 1fr auto; }.director-page-context { display: none; }.director-page-main { grid-template-columns: minmax(340px,.9fr) minmax(460px,1.1fr); padding-inline: 16px; }
  .workspace { grid-template-columns: 225px minmax(400px,1fr) 285px; }
  .topbar { grid-template-columns: 250px 1fr 300px; }
  .paper { width: calc(100% - 28px); padding-inline: 38px; }
  .font-size-control { margin-left: 5px; padding-left: 5px; }.font-size-control > span { display: none; }
  .editor-stats span:nth-child(2) { display: none; }
}
@media (max-width: 900px) {
  .project-page-main { width: calc(100% - 28px); padding-top: 24px; }.project-hero { align-items: stretch; flex-direction: column; }.project-status { min-width: 0; }.project-grid { grid-template-columns: 1fr; }.project-output-card { grid-row: auto; }
  .story-library-header { grid-template-columns: 1fr auto; padding: 0 15px; }.story-library-book { display: none; }.story-library-main { padding-top: 24px; }.story-library-card { padding-right: 12px; }.story-library-card > .bible-intro,.story-library-card .bible-ai-scope-bar { margin-left: 142px; }:deep(.story-library-tabs > .el-tabs__header.is-left) { width: 126px; margin-right: 16px; padding-inline: 7px; }:deep(.story-library-tabs .el-tabs__item.is-left) { padding-inline: 9px; font-size: 11px; }.story-library-tabs .bible-layout { grid-template-columns: 1fr; }.story-library-tabs .bible-list { min-height: 0; max-height: 210px; padding-bottom: 12px; border-right: 0; border-bottom: 1px solid #ece8e1; }.volume-range-grid { grid-template-columns: 1fr 1fr; }.volume-two-columns { grid-template-columns: 1fr; }
  .director-page-shell { height: auto; min-height: 100vh; overflow: visible; }.director-page-header { position: sticky; top: 0; z-index: 10; height: 64px; padding: 0 15px; }.director-page-actions .ghost-action { display: none; }.director-page-main { display: flex; flex-direction: column; padding: 14px; overflow: visible; }.director-conversation-panel,.director-plan-panel { min-height: 660px; overflow: visible; }.director-page-messages { min-height: 330px; max-height: 55vh; }.director-plan-panel { min-height: 500px; }.director-plan-grid { grid-template-columns: 1fr; }
  .home-main { width: min(100% - 28px,680px); padding: 45px 0; }.home-heading { align-items: flex-start; flex-direction: column; }.home-heading-side { width: 100%; }.home-summary { width: 100%; }.novel-grid { grid-template-columns: 1fr; }
  .mobile-only { display: inline-flex; }
  .panel-toggle { display: none; }
  .studio-shell { min-height: 600px; }
  .topbar { grid-template-columns: 1fr auto; }.book-title { display: none; }.brand { padding-left: 12px; }.top-actions .ghost-action span { display: none; }.top-actions { padding-right: 12px; }
  .workspace { grid-template-columns: 1fr; }.navigator,.ai-panel { position: fixed; top: 0; bottom: 0; z-index: 50; transition: transform .25s ease; box-shadow: 0 0 30px rgba(31,27,21,.16); }
  .navigator { left: 0; width: min(360px,92vw); transform: translateX(-105%); }.navigator.mobile-open { transform: translateX(0); }
  .ai-panel { right: 0; width: min(380px,92vw); transform: translateX(105%); }.ai-panel.mobile-open { transform: translateX(0); }
  .mobile-nav-head { height: 54px; align-items: center; justify-content: space-between; padding: 0 15px; border-bottom: 1px solid var(--line); }
  .mobile-ai { margin-left: auto; align-items: center; gap: 5px; border: 0; border-radius: 8px; padding: 7px 10px; background: #765bd6; color: white; font-size: 10px; }
  .editor-stats { margin-left: 12px; }
  .paper { width: calc(100% - 24px); padding: 38px clamp(25px,8vw,64px); }.manuscript { font-size: 15px; }
  .backdrop { position: fixed; inset: 0; z-index: 45; background: rgba(25,22,18,.3); backdrop-filter: blur(2px); }
}
@media (max-width: 560px) {
  .project-page-main { width: calc(100% - 18px); }.project-hero h1 { font-size: 28px; }.project-card { padding: 16px; }.project-field-grid,.project-number-grid { grid-template-columns: 1fr; }.project-save-bar { bottom: 8px; align-items: stretch; flex-direction: column; }.project-chat { height: 700px; min-height: 0; }.project-chat-quick { grid-template-columns: repeat(2,1fr); }
  .story-library-header { height: 62px; }.story-library-brand .brand-tagline,.story-library-return { display: none; }.story-library-main { width: calc(100% - 18px); padding: 20px 0; }.story-library-overview h1 { font-size: 27px; }.story-library-stats { grid-template-columns: repeat(2,1fr); }.story-library-card { padding: 12px; overflow: visible; }.story-library-card > .bible-intro,.story-library-card .bible-ai-scope-bar { margin-left: 0; }.story-library-card .bible-ai-scope-bar { align-items: stretch; flex-direction: column; }.story-library-card .bible-ai-scope-bar select { width: 100%; min-width: 0; }:deep(.story-library-tabs.el-tabs--left) { display: block; }:deep(.story-library-tabs > .el-tabs__header.is-left) { width: 100%; margin: 0 0 12px; padding: 0 0 10px; border-right: 0; border-bottom: 1px solid #ebe5dc; }:deep(.story-library-tabs .el-tabs__nav.is-left) { display: flex; width: max-content; }:deep(.story-library-tabs .el-tabs__nav-scroll) { overflow-x: auto; }:deep(.story-library-tabs .el-tabs__item.is-left) { height: 38px; margin: 0 4px 0 0; white-space: nowrap; }.story-library-tabs .bible-form { grid-template-columns: 1fr; padding-right: 0; }.story-library-tabs .bible-form > label,.story-library-tabs .bible-actions,.story-library-tabs .bible-ai-box,.story-library-tabs .volume-range-grid,.story-library-tabs .volume-two-columns,.story-library-tabs .volume-stale-warning { grid-column: 1; }.volume-range-grid { grid-template-columns: 1fr; }
  .director-page-brand .brand-mark,.director-page-brand .brand-tagline { display: none; }.director-return-button { padding: 0 11px; }.director-page-main { padding: 9px; }.director-panel-heading,.director-page-messages { padding-inline: 16px; }.director-panel-heading h1 { font-size: 20px; }.director-panel-heading > span { display: none; }.director-composer { padding: 12px; }.director-composer > div:last-child { align-items: stretch; flex-direction: column; }.director-composer-actions { width: 100%; }.director-composer-actions > button { flex: 1; }.director-plan-board { padding: 17px 15px; }.director-draft-settings { align-items: stretch; flex-direction: column; }.director-draft-settings .segmented { margin-right: 0; display: grid; grid-template-columns: repeat(3,1fr); }
  .style-manager-layout { min-height: 0; grid-template-columns: 1fr; }.style-profile-list { max-height: 180px; overflow-y: auto; padding: 0 0 12px; border-right: 0; border-bottom: 1px solid #e4dfd7; }
  .home-header { height: 62px; padding: 0 15px; }.home-main { padding-top: 34px; }.home-heading h1 { font-size: 31px; }.home-heading-side { flex-direction: column; }.home-summary { min-width: 0; padding: 13px; }.create-novel-button { min-height: 44px; }.novel-home-card { grid-template-columns: 82px 1fr; gap: 0 16px; padding: 17px; min-height: 190px; }.home-cover { width: 82px; height: 136px; }.home-card-copy strong { font-size: 17px; }.home-card-copy > span { font-size: 11px; }.home-card-copy em { display: none; }
  .brand-tagline { display: none; }.brand-name { font-size: 14px; }.brand-mark { width: 31px; height: 31px; font-size: 17px; }
  .top-actions .ghost-action:first-child { display: none; }.topbar { height: 56px; }.workspace { height: calc(100vh - 56px); }.editor-toolbar { padding: 0 9px; }
  .format-actions button:nth-of-type(n+4),.format-actions i { display: none; }.paper { margin-top: 9px; height: calc(100% - 63px); padding: 32px 24px; }.chapter-title-input { font-size: 25px; }.manuscript { line-height: 2.15; }
}

@media (max-width: 700px) {
  .memory-grid,.memory-fact-row { grid-template-columns: 1fr; }
  .character-row,.foreshadow-row,.memory-event-card { grid-template-columns: 1fr; }
  .character-row > button,.foreshadow-row > button { justify-self: end; }
  .memory-extract-bar { grid-template-columns: 1fr; }
  .run-center-summary { grid-template-columns: repeat(2,1fr); }.run-center-layout { grid-template-columns: 1fr; max-height: 68vh; }.run-list { max-height: 220px; border-right: 0; border-bottom: 1px solid #e8e4dd; }.metric-grid { grid-template-columns: repeat(2,1fr); }
  .eval-layout { grid-template-columns: 1fr; }.eval-case-list { max-height: 180px; border-right: 0; border-bottom: 1px solid #e6e1d9; }.eval-form-grid,.eval-metrics { grid-template-columns: repeat(2,1fr); }.eval-report { grid-template-columns: 1fr; }.eval-score { grid-row: auto; }.eval-report > section,.eval-report > .run-error,.eval-history { grid-column: 1; }
}
.project-workspace { min-height: 0; flex: 1; grid-template-columns: minmax(0,1fr) minmax(0,1fr); grid-template-areas: "chat editor"; align-items: stretch; gap: 18px; }.project-editor-column { grid-area: editor; min-height: 0; padding: 0 6px 10px 0; overflow-y: auto; }.project-chat { grid-area: chat; position: static; top: auto; height: 100%; min-height: 0; }.project-chat > header { grid-row: 1; }.project-chat-source { grid-row: 2; }.project-chat-messages { grid-row: 3; min-height: 0; }.project-chat > footer { grid-row: 4; }
.project-setting-tabs { position: sticky; top: 0; z-index: 10; display: grid; grid-template-columns: repeat(5,1fr); gap: 5px; margin-bottom: 10px; padding: 7px; border: 1px solid #ded7cb; border-radius: 12px; background: rgba(250,248,243,.96); backdrop-filter: blur(12px); }.project-setting-tabs button { min-width: 0; height: 34px; border: 0; border-radius: 7px; color: #756d64; background: transparent; cursor: pointer; font-size: 10px; }.project-setting-tabs button.active { color: white; background: #7458c9; box-shadow: 0 5px 12px rgba(105,78,190,.2); }.project-grid { grid-template-columns: 1fr; gap: 10px; }.project-output-card { grid-row: auto; }.project-card { box-shadow: 0 8px 24px rgba(62,51,40,.05); }.project-save-bar { bottom: 0; margin-top: 10px; }.project-style-card select { width: 100%; height: 42px; padding: 0 10px; border: 1px solid #dfd9cf; border-radius: 9px; color: #47413a; background: #fffefa; }.project-style-preview { padding: 14px; border-radius: 10px; background: #f6f2ff; }.project-style-preview strong { color: #5d49a5; }.project-style-preview p { color: #756c7d; font-size: 11px; line-height: 1.6; }.project-style-preview pre { max-height: 300px; overflow: auto; color: #574f5d; font: 11px/1.7 inherit; white-space: pre-wrap; }.project-style-manage { justify-self: start; padding: 9px 14px; border: 0; border-radius: 8px; color: white; background: #7358c9; cursor: pointer; }
@media (max-width: 900px) { .project-page-shell { height: auto; min-height: 100vh; overflow: visible; }.project-page-main { display: block; }.project-workspace { display: flex; flex-direction: column; }.project-chat { order: 1; height: 72vh; min-height: 600px; }.project-editor-column { order: 2; overflow: visible; }.project-setting-tabs { grid-template-columns: repeat(3,1fr); }.project-hero { padding-top: 8px; } }
.project-setting-tabs { grid-template-columns: repeat(3,1fr); }.project-memory-card { align-content: start; }.project-memory-group { padding: 12px; border: 1px solid #e4ddee; border-radius: 9px; background: #faf8ff; }.project-memory-group > strong { color: #6651aa; font-size: 11px; }.project-memory-group ul { margin: 8px 0 0; padding-left: 20px; color: #5d5662; font-size: 11px; line-height: 1.7; }.project-memory-empty { padding: 35px 20px; border-radius: 10px; color: #918798; background: #f7f4fa; text-align: center; font-size: 11px; line-height: 1.7; }.project-memory-card > small { color: #9a909e; font-size: 9px; }
</style>
<style scoped src="./styles/editor.css"></style>
<style scoped src="./styles/planning.css"></style>
