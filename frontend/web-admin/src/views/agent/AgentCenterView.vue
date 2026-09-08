<template>
  <section class="ai-shell">
    <!-- ═══════════ 左侧栏 ═══════════ -->
    <aside class="sidebar">
      <!-- 品牌栏 -->
      <div class="sidebar-brand">
        <div class="brand-left">
          <span class="brand-hex" aria-hidden="true">
            <svg viewBox="0 0 28 28" fill="none">
              <path
                d="M14 2l10.5 6v12L14 26 3.5 20V8z"
                stroke="#45caff"
                stroke-width="1.5"
                fill="none"
              />
              <circle
                cx="14"
                cy="14"
                r="4"
                stroke="#45caff"
                stroke-width="1.2"
                fill="rgba(69,202,255,0.1)"
              />
              <circle cx="14" cy="14" r="1.2" fill="#45caff" />
            </svg>
          </span>
          <div class="brand-text">
            <span class="brand-title">云枢 Agent</span>
          </div>
        </div>
        <button class="new-chat-btn" @click="goBack" title="返回控制台">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M15 18l-6-6 6-6"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
        </button>
        <button class="new-chat-btn" @click="resetChat" title="新建对话">
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M12 5v14M5 12h14"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </div>

      <!-- 内嵌搜索框 -->
      <div class="sidebar-search">
        <span class="search-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none">
            <circle
              cx="11"
              cy="11"
              r="6"
              stroke="currentColor"
              stroke-width="1.6"
            />
            <path
              d="M16 16l4 4"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
        </span>
        <input
          v-model="sessionSearchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索历史对话..."
        />
        <button
          v-if="sessionSearchKeyword"
          class="search-clear"
          @click="sessionSearchKeyword = ''"
          title="清除搜索"
        >
          <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <path
              d="M7 7l10 10M17 7L7 17"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
            />
          </svg>
        </button>
      </div>

      <!-- 快捷操作 chip 矩阵 -->
      <div class="quick-chips">
        <span class="chips-label">快捷操作</span>
        <div class="chips-grid">
          <button
            v-for="chip in quickChips"
            :key="chip.key"
            class="chip"
            :class="['chip--' + chip.accent]"
            @click="handleChipClick(chip)"
            :disabled="sending"
          >
            <span class="chip-indicator"></span>
            <span class="chip-icon-wrap">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
                v-html="chip.icon"
              ></svg>
            </span>
            <span class="chip-text">{{ chip.label }}</span>
          </button>
        </div>
      </div>

      <!-- 会话历史列表 -->
      <div class="session-list-wrap">
        <div
          v-for="group in groupedSessions"
          :key="group.label"
          class="session-group"
        >
          <div class="session-group-label">{{ group.label }}</div>
          <div
            v-for="item in group.items"
            :key="item.session_id"
            class="session-row"
            :class="{ active: item.session_id === currentSessionId }"
            @click="openSession(item.session_id)"
          >
            <span class="session-icon" aria-hidden="true">
              <svg viewBox="0 0 1024 1024" fill="none">
                <path
                  d="M397.312 331.776c-167.936 0-303.104 114.688-303.104 253.952 0 73.728 36.864 135.168 94.208 184.32l-24.576 86.016c-4.096 16.384 12.288 32.768 28.672 28.672l126.976-49.152c24.576 4.096 49.152 8.192 77.824 8.192 167.936 0 303.104-114.688 303.104-253.952S565.248 331.776 397.312 331.776zM397.312 802.816c-20.48 0-45.056-4.096-69.632-8.192-4.096 0-4.096 0-8.192 0-4.096 0-8.192 0-12.288 4.096l-98.304 36.864L225.28 778.24c4.096-16.384 0-28.672-12.288-40.96-49.152-40.96-77.824-94.208-77.824-155.648C135.168 466.944 253.952 368.64 397.312 368.64c147.456 0 262.144 98.304 262.144 217.088S544.768 802.816 397.312 802.816z"
                  fill="white"
                />
                <path
                  d="M925.696 397.312c0-139.264-135.168-253.952-303.104-253.952-122.88 0-229.376 65.536-278.528 155.648 16.384 0 28.672-4.096 45.056-4.096 45.056-65.536 131.072-114.688 233.472-114.688 147.456 0 262.144 98.304 262.144 217.088 0 57.344-28.672 114.688-77.824 155.648-12.288 8.192-16.384 24.576-12.288 36.864l16.384 57.344-77.824-28.672c0 12.288-4.096 24.576-8.192 36.864l102.4 40.96c16.384 8.192 32.768-8.192 28.672-28.672l-24.576-86.016C888.832 536.576 925.696 471.04 925.696 397.312z"
                  fill="white"
                />
              </svg>
            </span>
            <span
              class="session-title"
              v-if="renamingSessionId !== item.session_id"
              >{{ sessionSummary(item) }}</span
            >
            <input
              v-else
              class="session-rename-input"
              v-model="renameInput"
              @keydown.enter.stop="confirmRename(item.session_id)"
              @keydown.escape.stop="cancelRename()"
              @blur="confirmRename(item.session_id)"
              @click.stop
              ref="renameInputRef"
            />
            <button
              class="session-more-btn"
              @click.stop="toggleSessionMenu(item.session_id, $event)"
              title="更多操作"
            >
              <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <circle cx="12" cy="6" r="1.5" fill="currentColor" />
                <circle cx="12" cy="12" r="1.5" fill="currentColor" />
                <circle cx="12" cy="18" r="1.5" fill="currentColor" />
              </svg>
            </button>
            <!-- 右键菜单 -->
            <Teleport to="body">
              <div
                v-if="activeMenuSessionId === item.session_id"
                class="session-context-menu"
                :style="{ top: menuPos.y + 'px', left: menuPos.x + 'px' }"
              >
                <button
                  @click.stop="handleSessionAction('rename', item.session_id)"
                >
                  重命名
                </button>
                <button
                  @click.stop="handleSessionAction('share', item.session_id)"
                >
                  分享
                </button>
                <button
                  class="danger"
                  @click.stop="handleSessionAction('delete', item.session_id)"
                >
                  删除
                </button>
              </div>
            </Teleport>
          </div>
        </div>
        <p v-if="!sessions.length" class="session-empty">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            class="empty-icon"
            aria-hidden="true"
          >
            <circle
              cx="12"
              cy="12"
              r="9"
              stroke="currentColor"
              stroke-width="1.4"
            />
            <path
              d="M8 15s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01"
              stroke="currentColor"
              stroke-width="1.4"
              stroke-linecap="round"
            />
          </svg>
          暂无历史对话
        </p>
      </div>

      <!-- 底部状态栏 -->
      <div class="sidebar-footer">
        <span class="footer-stat">{{ sessions.length }} 段对话</span>
        <span class="footer-sep">·</span>
        <span class="footer-status">
          <span
            class="status-dot"
            :class="backendStatus === '运行中' ? 'online' : 'offline'"
          ></span>
          后端: {{ backendStatus }}
        </span>
      </div>
    </aside>

    <!-- ═══════════ 聊天主区域 ═══════════ -->
    <article class="chat-main" :class="{ 'initial-view': showEntryGreeting }">
      <div v-show="messages.length" ref="messageListRef" class="chat-scroll">
        <div class="chat-thread">
          <div
            v-for="(message, mIdx) in messages"
            :key="message.id"
            :class="['message-item', message.role]"
            :style="{ '--delay': mIdx * 0.04 + 's' }"
          >
            <div class="message-avatar" v-if="message.role === 'assistant'">
              <svg viewBox="0 0 28 28" fill="none" aria-hidden="true">
                <path
                  d="M14 2l10.5 6v12L14 26 3.5 20V8z"
                  stroke="#45caff"
                  stroke-width="1.2"
                  fill="rgba(69,202,255,0.08)"
                />
                <circle cx="14" cy="14" r="3" fill="#45caff" opacity="0.6" />
              </svg>
            </div>
            <div class="message-content-wrap">
              <div class="message-meta">
                <strong>{{
                  message.role === "assistant" ? "智检 AI" : "你"
                }}</strong>
                <span>{{ message.time }}</span>
              </div>
              <div
                v-if="message.role === 'assistant'"
                class="message-body ai-body markdown-body"
              >
                <!-- 报告卡片模式 -->
                <template v-if="message.reportCard">
                  <div class="report-card" v-if="message.reportData">
                    <div class="rc-header">
                      <svg
                        class="rc-header-icon"
                        viewBox="0 0 24 24"
                        fill="none"
                        aria-hidden="true"
                      >
                        <path
                          d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"
                          stroke="#45caff"
                          stroke-width="1.4"
                          stroke-linecap="round"
                        />
                        <path
                          d="M14 2v6h6M16 13H8M16 17H8M10 9H8"
                          stroke="#45caff"
                          stroke-width="1.4"
                          stroke-linecap="round"
                        />
                      </svg>
                      <div>
                        <div class="rc-title">智能检测周报预览</div>
                        <div class="rc-subtitle">
                          {{ message.reportData.week_start }} —
                          {{ message.reportData.week_end }}
                        </div>
                      </div>
                      <div class="rc-badge">自动生成</div>
                    </div>

                    <!-- KPI 指标网格 -->
                    <div class="rc-kpi-grid">
                      <div class="rc-kpi-item">
                        <div class="rc-kpi-val">
                          {{ message.reportData.kpi?.task_count ?? "—" }}
                        </div>
                        <div class="rc-kpi-label">完成任务<span>批</span></div>
                      </div>
                      <div class="rc-kpi-item">
                        <div class="rc-kpi-val">
                          {{ message.reportData.kpi?.total_images ?? "—" }}
                        </div>
                        <div class="rc-kpi-label">检测图片<span>张</span></div>
                      </div>
                      <div
                        class="rc-kpi-item"
                        :class="{
                          'rc-kpi-warn':
                            (message.reportData.kpi?.anomaly_rate ?? 0) > 0.1,
                        }"
                      >
                        <div class="rc-kpi-val">
                          {{
                            (
                              (message.reportData.kpi?.anomaly_rate ?? 0) * 100
                            ).toFixed(2)
                          }}%
                        </div>
                        <div class="rc-kpi-label">综合异常率</div>
                      </div>
                      <div class="rc-kpi-item">
                        <div class="rc-kpi-val">
                          {{ message.reportData.kpi?.total_boxes ?? "—" }}
                        </div>
                        <div class="rc-kpi-label">
                          检出缺陷框<span>个</span>
                        </div>
                      </div>
                    </div>

                    <!-- 近6次任务缺陷类别表 -->
                    <div class="rc-section-title">
                      近期任务缺陷明细（近6次）
                    </div>
                    <div
                      class="rc-table-wrap"
                      v-if="message.reportData.last6_stats?.tasks?.length"
                    >
                      <table class="rc-table">
                        <thead>
                          <tr>
                            <th>#</th>
                            <th>任务编号</th>
                            <th>检测时间</th>
                            <th>图片</th>
                            <th>异常</th>
                            <th
                              v-for="cat in [
                                '凹陷与白色涂抹物',
                                '发丝等丝状物',
                                '微小黑色杂质',
                                '瓶盖异常或异构',
                              ]"
                              :key="cat"
                            >
                              {{ cat }}
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          <tr
                            v-for="(task, ti) in message.reportData.last6_stats
                              .tasks"
                            :key="ti"
                          >
                            <td>{{ ti + 1 }}</td>
                            <td class="rc-mono">
                              {{ task.task_no?.slice(-12) }}
                            </td>
                            <td>{{ task.created_at?.slice(0, 16) }}</td>
                            <td>{{ task.total_images }}</td>
                            <td
                              :class="{ 'rc-td-warn': task.anomaly_images > 0 }"
                            >
                              {{ task.anomaly_images }}
                            </td>
                            <td
                              v-for="cat in [
                                '凹陷与白色涂抹物',
                                '发丝等丝状物',
                                '微小黑色杂质',
                                '瓶盖异常或异构',
                              ]"
                              :key="cat"
                            >
                              {{ task.categories?.[cat] ?? 0 }}
                            </td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                    <div v-else class="rc-empty">暂无近期任务数据</div>

                    <!-- 类别汇总 -->
                    <div class="rc-section-title">4大标准缺陷类别汇总</div>
                    <div
                      class="rc-cat-bars"
                      v-if="
                        message.reportData.last6_stats?.category_totals?.length
                      "
                    >
                      <div
                        v-for="cat in message.reportData.last6_stats
                          .category_totals"
                        :key="cat.class_name"
                        class="rc-bar-row"
                      >
                        <span class="rc-bar-label">{{ cat.class_name }}</span>
                        <div class="rc-bar-track">
                          <div
                            class="rc-bar-fill"
                            :style="{
                              width:
                                rcBarWidth(
                                  cat.count,
                                  message.reportData.last6_stats
                                    .category_totals,
                                ) + '%',
                            }"
                          ></div>
                        </div>
                        <span class="rc-bar-count">{{ cat.count }}</span>
                      </div>
                    </div>

                    <!-- 图表预览 -->
                    <template v-if="message.reportData.charts">
                      <div class="rc-section-title">检测图表</div>
                      <div class="rc-charts-grid">
                        <!-- 日趋势 -->
                        <div
                          class="rc-chart-item rc-chart-wide"
                          v-if="message.reportData.charts.daily_trend"
                        >
                          <div class="rc-chart-caption">
                            日趋势分析（检测量 & 异常率）
                          </div>
                          <img
                            class="rc-chart-img"
                            :src="
                              'data:image/png;base64,' +
                              message.reportData.charts.daily_trend
                            "
                            alt="日趋势"
                          />
                        </div>
                        <!-- 近6次堆叠图 -->
                        <div
                          class="rc-chart-item rc-chart-wide"
                          v-if="message.reportData.charts.last6_stacked"
                        >
                          <div class="rc-chart-caption">
                            近6次任务缺陷类别分布
                          </div>
                          <img
                            class="rc-chart-img"
                            :src="
                              'data:image/png;base64,' +
                              message.reportData.charts.last6_stacked
                            "
                            alt="近6次堆叠"
                          />
                        </div>
                        <!-- 占比饼图 -->
                        <div
                          class="rc-chart-item"
                          v-if="message.reportData.charts.proportion_pie"
                        >
                          <div class="rc-chart-caption">缺陷类别占比</div>
                          <img
                            class="rc-chart-img"
                            :src="
                              'data:image/png;base64,' +
                              message.reportData.charts.proportion_pie
                            "
                            alt="缺陷占比"
                          />
                        </div>
                        <!-- 风险等级分布 -->
                        <div
                          class="rc-chart-item"
                          v-if="message.reportData.charts.risk_dist"
                        >
                          <div class="rc-chart-caption">风险等级分布</div>
                          <img
                            class="rc-chart-img"
                            :src="
                              'data:image/png;base64,' +
                              message.reportData.charts.risk_dist
                            "
                            alt="风险分布"
                          />
                        </div>
                      </div>
                    </template>

                    <!-- 下载按钮 -->
                    <div class="rc-actions">
                      <button
                        class="rc-download-btn"
                        :class="{ loading: exportingReport }"
                        :disabled="exportingReport"
                        @click="exportWeeklyReport(message)"
                      >
                        <svg
                          v-if="!exportingReport"
                          viewBox="0 0 24 24"
                          fill="none"
                          aria-hidden="true"
                        >
                          <path
                            d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M7 10l5 5 5-5M12 15V3"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                          />
                        </svg>
                        <span class="rc-spin" v-else></span>
                        {{ exportingReport ? "正在生成…" : "导出 Word 报表" }}
                      </button>
                    </div>
                  </div>
                  <div v-else class="rc-loading">
                    <span class="rc-spin"></span> 正在加载报告数据…
                  </div>
                </template>

                <!-- 普通 AI 回复 -->
                <template v-else>
                  <!-- 流式输出中：增量 markdown 渲染，边流式边格式化 -->
                  <div
                    v-if="sending && mIdx === messages.length - 1"
                    class="streaming-rendered"
                    v-html="streamingHtml"
                  ></div>
                  <!-- 完成后：最终完整渲染，与流式结束时内容相同无跳变 -->
                  <div
                    v-else
                    class="markdown-rendered"
                    v-html="renderAssistantMarkdown(message.content)"
                  ></div>
                  <span
                    v-if="
                      sending &&
                      mIdx === messages.length - 1 &&
                      message.role === 'assistant'
                    "
                    class="streaming-cursor"
                    >▋</span
                  >
                </template>
              </div>
              <div v-else class="message-body user-body">
                <p
                  v-for="(line, idx) in formattedMessageLines(message)"
                  :key="idx"
                >
                  {{ line }}
                </p>
              </div>
              <button
                v-if="!sending && message.content.trim()"
                class="msg-copy-btn"
                @click="copyMessage(message.content)"
                title="复制内容"
              >
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <rect
                    x="9"
                    y="9"
                    width="10"
                    height="10"
                    rx="2"
                    stroke="currentColor"
                    stroke-width="1.5"
                  />
                  <path
                    d="M5 15V7a2 2 0 012-2h8"
                    stroke="currentColor"
                    stroke-width="1.5"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="composer-wrap">
        <div v-if="showEntryGreeting" class="welcome-block">
          <p class="entry-greeting">{{ entryGreeting }}</p>
        </div>
        <div class="composer" :class="{ focused: composerFocused }">
          <textarea
            v-model="inputText"
            placeholder="输入消息，开始智能分析..."
            @keydown.enter.exact.prevent="handleSend"
            @focus="composerFocused = true"
            @blur="composerFocused = false"
          ></textarea>

          <div class="composer-footer">
            <div class="composer-left-actions">
              <button class="attach-btn" title="添加附件">
                <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                  <path
                    d="M12 5v14M5 12h14"
                    stroke="currentColor"
                    stroke-width="1.8"
                    stroke-linecap="round"
                  />
                </svg>
              </button>
              <div class="think-mode-dropdown" ref="modelDropdownRef">
                <button class="think-mode-trigger" type="button" @click="modelDropdownOpen = !modelDropdownOpen">
                  <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" class="think-icon">
                    <path d="M12 2l9 5-9 5-9-5 9-5z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M3 12l9 5 9-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    <path d="M3 17l9 5 9-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span>{{ currentModelLabel }}</span>
                  <svg viewBox="0 0 24 24" fill="none" class="chevron-icon" :class="{ open: modelDropdownOpen }" aria-hidden="true">
                    <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </button>
                <div v-if="modelDropdownOpen" class="think-dropdown-menu">
                  <button v-for="m in models" :key="m.key" type="button" :class="{ active: selectedModel === m.key }" @click="selectModel(m.key)">
                    <span class="dropdown-label">{{ m.label }}</span>
                    <span class="dropdown-desc">{{ m.desc }}</span>
                  </button>
                </div>
              </div>
              <div class="think-mode-dropdown" ref="thinkDropdownRef">
                <button
                  class="think-mode-trigger"
                  @click="thinkDropdownOpen = !thinkDropdownOpen"
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    aria-hidden="true"
                    class="think-icon"
                  >
                    <path
                      d="M12 2a7 7 0 017 7c0 2.8-1.6 5.2-4 6.3V17a1 1 0 01-1 1h-4a1 1 0 01-1-1v-1.7A7 7 0 0112 2z"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                    />
                    <path
                      d="M9 21h6M10 19h4"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                    />
                  </svg>
                  <span>{{
                    thinkingMode === "standard" ? "标准思考" : "进阶思考"
                  }}</span>
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    class="chevron-icon"
                    :class="{ open: thinkDropdownOpen }"
                    aria-hidden="true"
                  >
                    <path
                      d="M6 9l6 6 6-6"
                      stroke="currentColor"
                      stroke-width="1.6"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
                <div v-if="thinkDropdownOpen" class="think-dropdown-menu">
                  <button
                    :class="{ active: thinkingMode === 'standard' }"
                    @click="
                      thinkingMode = 'standard';
                      thinkDropdownOpen = false;
                    "
                  >
                    <span class="dropdown-label">标准思考</span>
                    <span class="dropdown-desc">快速响应，适合日常分析</span>
                  </button>
                  <button
                    :class="{ active: thinkingMode === 'advanced' }"
                    @click="
                      thinkingMode = 'advanced';
                      thinkDropdownOpen = false;
                    "
                  >
                    <span class="dropdown-label">进阶思考</span>
                    <span class="dropdown-desc">深度推理，适合复杂判断</span>
                  </button>
                </div>
              </div>
            </div>
            <button
              class="send-btn"
              :class="{ sending }"
              :disabled="!sending && !inputText.trim()"
              :aria-label="sending ? '停止生成' : '发送消息'"
              @click="sending ? stopStreaming() : handleSend()"
            >
              <svg
                v-if="!sending"
                class="action-icon send-icon"
                viewBox="0 0 24 24"
                fill="none"
                aria-hidden="true"
              >
                <path d="M12 18.2V5.8" />
                <path d="M6.6 11.2L12 5.8l5.4 5.4" />
              </svg>
              <svg
                v-else
                class="action-icon stop-icon"
                viewBox="0 0 24 24"
                aria-hidden="true"
              >
                <rect
                  x="6"
                  y="6"
                  width="12"
                  height="12"
                  rx="3"
                  fill="currentColor"
                  stroke="none"
                />
              </svg>
            </button>
          </div>
        </div>
      </div>
    </article>
  </section>
</template>

<script setup>
import axios from "axios";
import DOMPurify from "dompurify";
import MarkdownIt from "markdown-it";
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { agentCommandBus } from "@/composables/agentCommandBus";

const router = useRouter();
function goBack() {
  router.push("/app");
}

defineProps({
  backendStatus: {
    type: String,
    default: "未知",
  },
  latestTaskLabel: {
    type: String,
    default: "当前暂无检测任务",
  },
  overviewMetrics: {
    type: Object,
    default: () => ({}),
  },
  currentAiAnalysis: {
    type: Object,
    default: () => ({}),
  },
  defectLibrary: {
    type: Object,
    default: () => ({
      total_samples: 0,
      category_count: 0,
      categories: [],
    }),
  },
  statusText: {
    type: String,
    default: "",
  },
});

const messageListRef = ref(null);
const inputText = ref("");
const sending = ref(false);
const sessions = ref([]);
const currentSessionId = ref("");
const sessionSummaryCache = ref({});
const sessionSearchKeyword = ref("");
const composerFocused = ref(false);
const thinkingMode = ref("standard");
const thinkDropdownOpen = ref(false);
const thinkDropdownRef = ref(null);
const models = ref([]);
const selectedModel = ref("");
const modelDropdownOpen = ref(false);
const modelDropdownRef = ref(null);
const currentModelLabel = computed(() => {
  const m = models.value.find((x) => x.key === selectedModel.value);
  return m ? m.label : "模型";
});
const activeMenuSessionId = ref(null);
const menuPos = ref({ x: 0, y: 0 });
const renamingSessionId = ref(null);
const renameInput = ref("");
const renameInputRef = ref(null);

const quickChips = [
  {
    key: "stat",
    label: "统计报表",
    accent: "cyan",
    prompt: "请生成本周统计报表摘要，包含任务数、图片数、异常率等核心指标",
    icon: '<path d="M5 19h14M7 16V9M12 16V6M17 16v-4" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  },
  {
    key: "classify",
    label: "缺陷分类",
    accent: "blue",
    prompt: "请按缺陷分类给出分组结论，列出每个类别的检出数量和占比",
    icon: '<path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/>',
  },
  {
    key: "report",
    label: "综合报告",
    accent: "purple",
    prompt:
      "请给出本批次综合质检报告，包含检测概况、缺陷分布、风险等级和处置建议",
    icon: '<path d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  {
    key: "quick",
    label: "快速分析",
    accent: "amber",
    prompt: "请快速分析当前检测数据中的异常模式，给出关键发现",
    icon: '<path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  {
    key: "yield",
    label: "良率趋势",
    accent: "green",
    prompt: "请分析近30天良率变化趋势，标出异常波动日期并推测原因",
    icon: '<path d="M3 17l4-4 4 4 4-8 6 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/><circle cx="7" cy="13" r="1" fill="currentColor"/><circle cx="11" cy="17" r="1" fill="currentColor"/><circle cx="15" cy="9" r="1" fill="currentColor"/><circle cx="21" cy="15" r="1" fill="currentColor"/>',
  },
  {
    key: "alert",
    label: "异常预警",
    accent: "red",
    prompt:
      "请检查最近检测数据是否存在缺陷类型激增情况，给出预警等级和处置建议",
    icon: '<path d="M12 9v4M12 17h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  {
    key: "weekly",
    label: "导出周报",
    accent: "indigo",
    prompt:
      "请生成本周完整检测周报，包含KPI汇总、日趋势分析、风险项清单与改善建议，格式适合直接复制存档",
    icon: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  {
    key: "trace",
    label: "缺陷溯源",
    accent: "orange",
    prompt:
      "请对近期高发缺陷类别进行根因分析，推测可能的产线环节和工艺根因，给出排查优先级",
    icon: '<circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M21 21l-4.35-4.35" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M11 8v6M8 11h6" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>',
  },
  {
    key: "compare",
    label: "产线对比",
    accent: "teal",
    prompt: "请对比各条产线的检测表现，指出异常率最高的产线及差异原因分析",
    icon: '<path d="M18 20V10M12 20V4M6 20v-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><path d="M3 20h18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  {
    key: "suggest",
    label: "改善建议",
    accent: "lime",
    prompt:
      "基于当前检测数据和缺陷分布，请给出3条具体可落地的产线改善建议，包含责任环节和预期效果",
    icon: '<path d="M12 2a7 7 0 017 7c0 2.8-1.6 5.2-4 6.3V17a1 1 0 01-1 1h-4a1 1 0 01-1-1v-1.7A7 7 0 0112 2z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><path d="M9 21h6M10 19h4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
];

const entryGreeting = ref("");
const entryGreetingCandidates = [
  "准备好了，随时开始！",
  "我能帮您什么忙吗？",
  "您好，准备好开始了吗？",
  "你今天在想些什么？",
];

const contextCharts = ref({
  recent_task_anomaly: [],
  defect_category_samples: [],
});

const messages = ref([]);
const streamingHtml = ref(""); // 流式阶段增量渲染的 HTML
const streamState = ref({
  phase: "idle",
  label: "流式状态：未开始",
  tokenCount: 0,
});

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
});

let tokenPumpFrame = 0;
let streamAbortController = null;
let streamStoppedByUser = false;
let _pumpLastTick = 0;
let _pumpLastRenderTick = 0;
const PUMP_INTERVAL_MS = 38; // 字符需拍间隔 38ms ≈ 26字/秒
const RENDER_INTERVAL_MS = 80; // markdown 渲染节拍 80ms ≈ 12fps

/**
 * 流式阶段的 markdown 渲染：自动补齐未闭合的代码块，避免语法不完整时崩块
 */
function renderStreamingMarkdown(text) {
  const fenceCount = (text.match(/```/g) || []).length;
  const normalized = fenceCount % 2 === 1 ? text + "\n```" : text;
  return renderAssistantMarkdown(normalized);
}

const streamProgressPercent = computed(() => {
  const phase = streamState.value.phase;
  const tokenCount = Number(streamState.value.tokenCount || 0);

  if (phase === "done") return 100;
  if (phase === "error" || phase === "stopped") {
    return Math.min(
      100,
      Math.max(8, Math.round(100 * (1 - Math.exp(-tokenCount / 140)))),
    );
  }
  if (phase === "connecting") return 4;
  if (phase === "connected") return 8;
  if (phase === "streaming") {
    return Math.min(
      96,
      Math.max(10, Math.round(100 * (1 - Math.exp(-tokenCount / 110)))),
    );
  }
  return 0;
});

const visibleSessions = computed(() => {
  const keyword = String(sessionSearchKeyword.value || "")
    .trim()
    .toLowerCase();
  if (!keyword) return sessions.value;
  return sessions.value.filter((item) => {
    const source = [item?.session_summary, item?.title, item?.latest_message]
      .map((x) => String(x || "").toLowerCase())
      .join(" ");
    return source.includes(keyword);
  });
});

const groupedSessions = computed(() => {
  const now = new Date();
  const todayStr = now.toISOString().slice(0, 10);
  const weekAgo = new Date(now.getTime() - 7 * 86400000)
    .toISOString()
    .slice(0, 10);
  const today = [],
    week = [],
    older = [];
  visibleSessions.value.forEach((item) => {
    const raw = String(item.last_message_at || item.created_at || "").trim();
    const dateStr = raw.slice(0, 10);
    if (dateStr >= todayStr) today.push(item);
    else if (dateStr >= weekAgo) week.push(item);
    else older.push(item);
  });
  return [
    { label: "今天", items: today },
    { label: "本周", items: week },
    { label: "更早", items: older },
  ].filter((g) => g.items.length);
});

function toggleSessionMenu(sessionId, event) {
  if (activeMenuSessionId.value === sessionId) {
    activeMenuSessionId.value = null;
  } else {
    const btn = event.currentTarget;
    const rect = btn.getBoundingClientRect();
    menuPos.value = {
      x: rect.right + 4,
      y: rect.top,
    };
    activeMenuSessionId.value = sessionId;
  }
}

function handleSessionAction(action, sessionId) {
  activeMenuSessionId.value = null;
  if (action === "delete") {
    axios
      .delete(`/api/agent/chat/sessions/${sessionId}`)
      .then(() => {
        sessions.value = sessions.value.filter(
          (s) => s.session_id !== sessionId,
        );
        delete sessionSummaryCache.value[sessionId];
        if (currentSessionId.value === sessionId) resetChat();
      })
      .catch((err) => {
        console.error("删除会话失败", err);
      });
  } else if (action === "rename") {
    const item = sessions.value.find((s) => s.session_id === sessionId);
    renameInput.value =
      sessionSummaryCache.value[sessionId] ||
      item?.title ||
      item?.session_summary ||
      "";
    renamingSessionId.value = sessionId;
    nextTick(() => {
      const el = renameInputRef.value;
      if (Array.isArray(el) ? el[0] : el) {
        (Array.isArray(el) ? el[0] : el).focus();
        (Array.isArray(el) ? el[0] : el).select();
      }
    });
  } else if (action === "share") {
    console.log("share", sessionId);
  }
}

function confirmRename(sessionId) {
  const newTitle = renameInput.value.trim();
  if (!newTitle) {
    renamingSessionId.value = null;
    return;
  }
  axios
    .patch(`/api/agent/chat/sessions/${sessionId}`, { title: newTitle })
    .then(() => {
      const item = sessions.value.find((s) => s.session_id === sessionId);
      if (item) item.title = newTitle;
      sessionSummaryCache.value[sessionId] = newTitle;
    })
    .catch((err) => {
      console.error("重命名失败", err);
    })
    .finally(() => {
      renamingSessionId.value = null;
    });
}

function cancelRename() {
  renamingSessionId.value = null;
}

function closeMenuOnClickOutside(e) {
  activeMenuSessionId.value = null;
  if (thinkDropdownRef.value && !thinkDropdownRef.value.contains(e.target)) {
    thinkDropdownOpen.value = false;
  }
  if (modelDropdownRef.value && !modelDropdownRef.value.contains(e.target)) {
    modelDropdownOpen.value = false;
  }
}

function copyMessage(text) {
  const cleaned = sanitizeDisplayText(text);
  navigator.clipboard.writeText(cleaned).catch(() => {});
}

function setStreamState(phase, label) {
  streamState.value = {
    ...streamState.value,
    phase,
    label,
  };
}

function stopTokenPump() {
  if (tokenPumpFrame) {
    cancelAnimationFrame(tokenPumpFrame);
    tokenPumpFrame = 0;
  }
}

// 按时间节拍平滑吐字，目标约 40ms/字（25字/秒），积压时适当加速但不超过 3字/帧
function startTokenPump(assistantMessage, queue) {
  stopTokenPump();
  _pumpLastTick = performance.now();
  _pumpLastRenderTick = 0;
  streamingHtml.value = "";

  const pump = (now) => {
    const elapsed = now - _pumpLastTick;
    if (elapsed >= PUMP_INTERVAL_MS && queue.length) {
      const burst = queue.length > 60 ? 3 : queue.length > 20 ? 2 : 1;
      const chunk = queue.splice(0, burst).join("");
      assistantMessage.content += chunk;
      _pumpLastTick = now - (elapsed % PUMP_INTERVAL_MS);
      scrollToBottom();
    }
    // 独立节拍重渲染 markdown，速率与字符吐出分离
    if (
      assistantMessage.content &&
      now - _pumpLastRenderTick >= RENDER_INTERVAL_MS
    ) {
      streamingHtml.value = renderStreamingMarkdown(assistantMessage.content);
      _pumpLastRenderTick = now;
    }
    tokenPumpFrame = requestAnimationFrame(pump);
  };

  tokenPumpFrame = requestAnimationFrame(pump);
}

function stopStreaming() {
  if (!sending.value) return;
  streamStoppedByUser = true;
  if (streamAbortController) {
    streamAbortController.abort();
  }
  setStreamState("stopped", "流式状态：已停止");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const STREAM_API_URL = "/api/agent/chat/stream";

function nowText() {
  const d = new Date();
  const hh = String(d.getHours()).padStart(2, "0");
  const mm = String(d.getMinutes()).padStart(2, "0");
  return `${hh}:${mm}`;
}

function pickEntryGreeting() {
  const idx = Math.floor(Math.random() * entryGreetingCandidates.length);
  entryGreeting.value = entryGreetingCandidates[idx];
}

/**
 * 流式阶段轻量清洗：去除标题符号、水平线、粗斜体标记、行首引用符。
 * 保留代码块原样（遇到 ``` 不处理其内部），保留换行结构。
 */
function stripStreamingNoise(text) {
  const raw = String(text || "");
  const lines = raw.split("\n");
  let inCodeBlock = false;
  const out = [];
  for (const line of lines) {
    if (line.trimStart().startsWith("```")) {
      inCodeBlock = !inCodeBlock;
      out.push(line);
      continue;
    }
    if (inCodeBlock) {
      out.push(line);
      continue;
    }
    let l = line;
    // 去掉行首标题符号 #
    l = l.replace(/^\s*#{1,6}\s+/, "");
    // 去掉纯水平线
    l = l.replace(/^\s*[-*_]{3,}\s*$/, "");
    // 去掉行首引用符 >
    l = l.replace(/^\s*>\s?/, "");
    // 去掉粗体 **text** → text，斜体 *text* → text
    l = l.replace(/\*\*([^*]+)\*\*/g, "$1");
    l = l.replace(/\*([^*]+)\*/g, "$1");
    // 行首列表符 - /  * / 数字. → 保留内容
    l = l.replace(/^\s*[-*]\s+/, "");
    l = l.replace(/^\s*\d+\.\s+/, "");
    out.push(l);
  }
  return out.join("\n");
}

function sanitizeDisplayText(text) {
  return String(text || "")
    .replace(/```[\s\S]*?```/g, "")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/^\s*#{1,6}\s+/gm, "")
    .replace(/^\s*[-*]\s+/gm, "")
    .replace(/^\s*>\s?/gm, "")
    .replace(/\*\*(.*?)\*\*/g, "$1")
    .replace(/\*(.*?)\*/g, "$1")
    .replace(/\r/g, "")
    .trim();
}

function normalizeAssistantMarkdown(text) {
  return String(text || "")
    .replace(/\r/g, "")
    .replace(/^\s*#{1,6}\s*$/gm, "")
    .replace(/^\s*[-*]{3,}\s*$/gm, "")
    .replace(/^\s*`{3,}\s*$/gm, "")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function renderAssistantMarkdown(text) {
  const normalized = normalizeAssistantMarkdown(text);
  const rawHtml = md.render(normalized || "");
  return DOMPurify.sanitize(rawHtml, {
    USE_PROFILES: { html: true },
  });
}

function compactText(text, max = 12) {
  const cleaned = sanitizeDisplayText(text).replace(/\s+/g, " ").trim();
  if (!cleaned) return "未命名对话";
  return cleaned.slice(0, max);
}

function sessionSummary(item) {
  const sid = String(item?.session_id || "");
  const localSummary = sid ? sessionSummaryCache.value[sid] : "";
  return compactText(
    localSummary ||
      item?.session_summary ||
      item?.title ||
      item?.latest_message ||
      "",
    12,
  );
}

function formatSessionTime(value) {
  const raw = String(value || "").trim();
  if (!raw) return "--:--";
  if (raw.length >= 16 && raw.includes(" ")) return raw.slice(11, 16);
  return raw.slice(0, 5);
}

function formatLines(text) {
  return String(text || "").split("\n");
}

function formattedMessageLines(message) {
  const raw = message?.content || "";
  const content =
    message?.role === "assistant" ? sanitizeDisplayText(raw) : raw;
  const rows = formatLines(content).filter((line) => line.trim().length > 0);
  return rows.length ? rows : [""];
}

const showEntryGreeting = computed(
  () => !sending.value && messages.value.length === 0,
);

function scrollToBottom() {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
    }
  });
}

function normalizeMessageTime(text) {
  const raw = String(text || "").trim();
  if (!raw) return nowText();
  if (raw.length >= 16 && raw.includes(" ")) return raw.slice(11, 16);
  return raw;
}

function resetChat() {
  stopStreaming();
  currentSessionId.value = "";
  messages.value = [];
  stopTokenPump();
  streamState.value = {
    phase: "idle",
    label: "流式状态：未开始",
    tokenCount: 0,
  };
  inputText.value = "";
  pickEntryGreeting();
  scrollToBottom();
}

function sendQuickPrompt(text) {
  inputText.value = text;
  handleSend();
}

// ── 导出周报 ──────────────────────────────────────────────────────────────────
const exportingReport = ref(false);
const weeklyReportPending = ref(false); // 流式结束后触发报告卡片

function rcBarWidth(count, totals) {
  const max = Math.max(...totals.map((t) => t.count), 1);
  return Math.round((count / max) * 100);
}

async function appendWeeklyReportCard(aiContent) {
  const base = "";
  let reportData = null;
  try {
    const resp = await fetch(`${base}/api/agent/weekly-report/preview`);
    const json = await resp.json();
    reportData = json.data || null;
  } catch (e) {
    console.error("预览数据获取失败", e);
  }
  messages.value.push({
    id: `report-card-${Date.now()}`,
    role: "assistant",
    time: nowText(),
    content: "",
    reportCard: true,
    reportData,
    aiContent, // 保存 AI 文本用于嵌入报告
  });
  await nextTick();
  scrollToBottom();
}

async function exportWeeklyReport(message) {
  if (exportingReport.value) return;
  exportingReport.value = true;
  const aiText = message?.aiContent ?? "";
  try {
    const base = "";
    const resp = await fetch(`${base}/api/agent/weekly-report`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ai_text: aiText }),
    });
    if (!resp.ok) {
      const errJson = await resp.json().catch(() => ({}));
      throw new Error(errJson.detail || `HTTP ${resp.status}`);
    }
    const blob = await resp.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");
    a.href = url;
    a.download = `weekly_report_${today}.docx`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  } catch (err) {
    console.error("导出周报失败", err);
    alert(`导出失败：${err?.message || "未知错误"}`);
  } finally {
    exportingReport.value = false;
  }
}

function handleChipClick(chip) {
  if (chip.key === "weekly") {
    weeklyReportPending.value = true;
  }
  sendQuickPrompt(chip.prompt);
}

// Search is now inline via sessionSearchKeyword v-model

async function loadAiContext() {
  try {
    const response = await axios.get("/api/agent/context");
    const chartPayload = response.data?.data?.chart_payload || {};
    contextCharts.value = {
      recent_task_anomaly: chartPayload.recent_task_anomaly || [],
      defect_category_samples: chartPayload.defect_category_samples || [],
    };
  } catch (error) {
    console.error(error);
  }
}

async function loadSessions() {
  try {
    const response = await axios.get("/api/agent/chat/sessions", {
      params: { limit: 30 },
    });
    const list = response.data?.data || [];
    sessions.value = list;
    sessionSummaryCache.value = {};
    list.forEach((item) => {
      const sid = String(item?.session_id || "");
      if (!sid) return;
      const fromServer = compactText(
        item?.session_summary || item?.title || "",
        12,
      );
      if (fromServer) {
        sessionSummaryCache.value[sid] = fromServer;
      }
    });
  } catch (error) {
    console.error(error);
  }
}

async function loadModels() {
  try {
    const resp = await axios.get("/api/agent/models");
    const list = resp.data?.data || [];
    models.value = list;
    if (!selectedModel.value && list.length) {
      const def = list.find((x) => x.default) || list[0];
      selectedModel.value = def.key;
    }
  } catch (e) {
    models.value = [];
  }
}

function selectModel(key) {
  selectedModel.value = key;
  modelDropdownOpen.value = false;
}

async function openSession(sessionId) {
  if (!sessionId) return;
  try {
    const response = await axios.get(
      `/api/agent/chat/sessions/${sessionId}/messages`,
      {
        params: { limit: 300 },
      },
    );
    const rows = response.data?.data || [];
    currentSessionId.value = sessionId;
    messages.value = rows.map((item, idx) => ({
      id: `${sessionId}-${idx}-${Date.now()}`,
      role: item.role || "assistant",
      time: normalizeMessageTime(item.time),
      content: item.content || "",
    }));
    if (!messages.value.length) {
      pickEntryGreeting();
    }
    scrollToBottom();
  } catch (error) {
    console.error(error);
  }
}

async function streamReply(text, assistantMessage) {
  const tokenQueue = [];
  let streamDone = false;
  startTokenPump(assistantMessage, tokenQueue);
  streamStoppedByUser = false;
  streamAbortController = new AbortController();

  const response = await fetch(STREAM_API_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    signal: streamAbortController.signal,
    body: JSON.stringify({
      prompt: text,
      session_id: currentSessionId.value || null,
      model: selectedModel.value || null,
    }),
  });

  if (!response.ok || !response.body) {
    throw new Error(`请求失败: ${response.status}`);
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder("utf-8");
  let buffer = "";

  setStreamState("connecting", "流式状态：连接中...");
  streamState.value.tokenCount = 0;

  const consumeEvents = () => {
    while (true) {
      const sepMatch = buffer.match(/\r?\n\r?\n/);
      if (!sepMatch || sepMatch.index === undefined) {
        return;
      }

      const sepIndex = sepMatch.index;
      const sepLength = sepMatch[0].length;
      const rawEvent = buffer.slice(0, sepIndex);
      buffer = buffer.slice(sepIndex + sepLength);

      const lines = rawEvent.split(/\r?\n/);
      const dataPayload = lines
        .filter((line) => line.startsWith("data:"))
        .map((line) => line.slice(5).trimStart())
        .join("\n");

      if (!dataPayload) continue;

      let payload = null;
      try {
        payload = JSON.parse(dataPayload);
      } catch {
        continue;
      }

      if (payload?.type === "session") {
        setStreamState("connected", "流式状态：已连接");
        currentSessionId.value = payload.session_id || currentSessionId.value;
        if (payload.chart_payload) {
          contextCharts.value = {
            recent_task_anomaly:
              payload.chart_payload.recent_task_anomaly || [],
            defect_category_samples:
              payload.chart_payload.defect_category_samples || [],
          };
        }
      } else if (payload?.type === "token") {
        const content = String(payload.content || "");
        if (content) {
          for (const ch of content) {
            tokenQueue.push(ch);
          }
          streamState.value.tokenCount += 1;
          setStreamState("streaming", "流式状态：生成中...");
        }
      } else if (payload?.type === "done") {
        streamDone = true;
        currentSessionId.value = payload.session_id || currentSessionId.value;
        if (payload.chart_payload) {
          contextCharts.value = {
            recent_task_anomaly:
              payload.chart_payload.recent_task_anomaly || [],
            defect_category_samples:
              payload.chart_payload.defect_category_samples || [],
          };
        }
      } else if (payload?.type === "error") {
        throw new Error(payload.message || "流式对话失败");
      }
    }
  };

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      consumeEvents();
      break;
    }

    buffer += decoder.decode(value, { stream: true });
    consumeEvents();
  }

  while (tokenQueue.length > 0) {
    await sleep(24);
  }
  stopTokenPump();

  if (streamDone) {
    setStreamState("done", "流式状态：完成");
  }

  streamAbortController = null;
}

async function handleSend() {
  const text = inputText.value.trim();
  if (!text || sending.value) return;

  messages.value.push({
    id: Date.now(),
    role: "user",
    time: nowText(),
    content: text,
  });

  inputText.value = "";
  sending.value = true;
  scrollToBottom();

  const assistantMessage = {
    id: `${Date.now()}-assistant`,
    role: "assistant",
    time: nowText(),
    content: "",
  };
  messages.value.push(assistantMessage);
  // 必须取 reactive 版本（proxy）传给 pump，otherwise Vue Set trap 不触发
  const reactiveAssistantMsg = messages.value[messages.value.length - 1];

  try {
    await streamReply(text, reactiveAssistantMsg);
    if (!assistantMessage.content.trim()) {
      assistantMessage.content = "模型未返回有效内容，请稍后再试。";
    }
  } catch (error) {
    console.error(error);
    stopTokenPump();
    if (streamStoppedByUser || error?.name === "AbortError") {
      setStreamState("stopped", "流式状态：已停止");
      if (!reactiveAssistantMsg.content.trim()) {
        reactiveAssistantMsg.content = "已停止本次生成。";
      }
    } else {
      setStreamState("error", "流式状态：异常");
      reactiveAssistantMsg.content = `调用大模型失败：${error?.message || "未知错误"}`;
    }
  } finally {
    stopTokenPump();
    // 停止前做最终同步渲染，尺齐流式结束和 v-else 之间的内容，消除跳变
    const lastMsg = messages.value[messages.value.length - 1];
    if (lastMsg?.role === "assistant" && lastMsg.content) {
      streamingHtml.value = renderAssistantMarkdown(lastMsg.content);
    }
    streamAbortController = null;
    sending.value = false;
    await loadAiContext();
    await loadSessions();
    scrollToBottom();

    // 周报模式：流式结束后追加报告卡片
    if (weeklyReportPending.value) {
      weeklyReportPending.value = false;
      const lastAiMsg = [...messages.value]
        .reverse()
        .find((m) => m.role === "assistant" && !m.reportCard);
      await appendWeeklyReportCard(lastAiMsg?.content ?? "");
    }
  }
}

onMounted(async () => {
  resetChat();
  pickEntryGreeting();
  document.addEventListener("click", closeMenuOnClickOutside);
  // 预留：Agent 下发的指令（打开页面、执行工具）经命令总线派发，与其他模块解耦
  agentCommandBus.onCommand((cmd) => {
    if (cmd?.type === "navigate" && cmd.path) router.push(cmd.path);
  });
  await Promise.all([loadAiContext(), loadSessions(), loadModels()]);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", closeMenuOnClickOutside);
});
</script>

<style scoped>
/* ═══════════ 变量 & 根布局 ═══════════ */
.ai-shell {
  --bg: #0a0e14;
  --sidebar-bg: rgba(18, 24, 33, 0.5);
  --accent: #5bc8e0;
  --accent-dim: rgba(91, 200, 224, 0.12);
  --accent-glow: rgba(91, 200, 224, 0.22);
  --line: rgba(231, 238, 246, 0.07);
  --text-main: #e6eaf0;
  --text-sub: #6b7686;
  --text-dim: #4a5260;
  --mono: "Cascadia Code", "Fira Mono", "Consolas", monospace;
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  height: calc(100vh - 100px);
  color: var(--text-main);
  background: transparent;
  overflow: hidden;
  position: relative;
}

.ai-shell::before {
  content: "";
  position: absolute;
  top: 0;
  bottom: 0;
  left: 280px;
  width: 1px;
  background: linear-gradient(
    180deg,
    transparent,
    var(--accent-dim) 20%,
    var(--accent-dim) 80%,
    transparent
  );
  pointer-events: none;
  z-index: 2;
}

/* ═══════════ 侧栏 ═══════════ */
.sidebar {
  background: var(--sidebar-bg);
  display: grid;
  grid-template-rows: auto auto auto minmax(0, 1fr) auto;
  width: 280px;
  min-width: 280px;
  max-width: 280px;
  min-height: 0;
  height: 100%;
}

/* ── 品牌栏 ── */
.sidebar-brand {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--line);
}

.brand-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-hex {
  width: 28px;
  height: 28px;
  display: block;
  flex-shrink: 0;
}

.brand-hex svg {
  width: 28px;
  height: 28px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.brand-title {
  font-size: 14px;
  font-weight: 700;
  color: #e8f4ff;
  letter-spacing: 0.04em;
}

.new-chat-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid rgba(69, 202, 255, 0.3);
  background: rgba(69, 202, 255, 0.06);
  color: var(--accent);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.new-chat-btn svg {
  width: 18px;
  height: 18px;
}

.new-chat-btn:hover {
  background: rgba(69, 202, 255, 0.14);
  border-color: rgba(69, 202, 255, 0.5);
  box-shadow: 0 0 12px rgba(69, 202, 255, 0.15);
}

/* ── 搜索框 ── */
.sidebar-search {
  padding: 10px 16px 8px;
  position: relative;
}

.search-icon {
  position: absolute;
  left: 26px;
  top: 50%;
  transform: translateY(-50%);
  width: 15px;
  height: 15px;
  color: var(--text-sub);
  pointer-events: none;
}

.search-icon svg {
  width: 15px;
  height: 15px;
}

.search-input {
  width: 100%;
  height: 34px;
  padding: 0 32px 0 36px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-main);
  font-size: 12px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
}

.search-input::placeholder {
  color: var(--text-dim);
}

.search-input:focus {
  border-color: rgba(69, 202, 255, 0.4);
  background: rgba(69, 202, 255, 0.04);
  box-shadow: 0 0 0 3px rgba(69, 202, 255, 0.06);
}

.search-clear {
  position: absolute;
  right: 22px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  border: none;
  background: transparent;
  color: var(--text-sub);
  cursor: pointer;
  padding: 0;
  display: grid;
  place-items: center;
}

.search-clear svg {
  width: 14px;
  height: 14px;
}

.search-clear:hover {
  color: var(--accent);
}

/* ── 快捷 Chip ── */
.quick-chips {
  padding: 4px 16px 8px;
  border-bottom: 1px solid var(--line);
}

.chips-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  text-align: center;
  color: #45caff;
  margin-bottom: 6px;
}

.chips-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 5px;
}

/* ── Chip 基础 ── */
.chip {
  --c: var(--accent);
  height: 38px;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--c) 14%, transparent);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--c) 4%, transparent) 0%,
    rgba(255, 255, 255, 0.012) 100%
  );
  color: color-mix(in srgb, var(--c) 55%, #8e9fb4);
  font-size: 11.5px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 11px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

/* 左侧指示条（独立元素，非伪元素） */
.chip-indicator {
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 2px;
  border-radius: 0 2px 2px 0;
  background: var(--c);
  opacity: 0.3;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

/* 扫描光 */
.chip::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(
    110deg,
    transparent 30%,
    color-mix(in srgb, var(--c) 8%, transparent) 50%,
    transparent 70%
  );
  opacity: 0;
  transform: translateX(-100%);
  transition:
    opacity 0.3s ease,
    transform 0.5s ease;
  pointer-events: none;
}

/* ── Hover ── */
.chip:hover {
  border-color: color-mix(in srgb, var(--c) 35%, transparent);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--c) 10%, transparent) 0%,
    color-mix(in srgb, var(--c) 3%, transparent) 100%
  );
  color: color-mix(in srgb, var(--c) 30%, #e8f4ff);
  box-shadow:
    0 0 1px color-mix(in srgb, var(--c) 20%, transparent),
    0 2px 8px color-mix(in srgb, var(--c) 8%, transparent),
    inset 0 1px 0 color-mix(in srgb, var(--c) 8%, transparent);
}

.chip:hover .chip-indicator {
  opacity: 1;
  width: 3px;
  box-shadow: 0 0 8px color-mix(in srgb, var(--c) 60%, transparent);
}

.chip:hover::after {
  opacity: 1;
  transform: translateX(100%);
}

.chip:active {
  transform: scale(0.97);
  transition-duration: 0.08s;
}

/* ── 图标 ── */
.chip-icon-wrap {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 5px;
  background: color-mix(in srgb, var(--c) 10%, transparent);
  transition: all 0.25s ease;
}

.chip-icon-wrap svg {
  width: 12px;
  height: 12px;
  color: var(--c);
  opacity: 0.75;
  transition: all 0.25s ease;
}

.chip:hover .chip-icon-wrap {
  background: color-mix(in srgb, var(--c) 18%, transparent);
  box-shadow: 0 0 6px color-mix(in srgb, var(--c) 25%, transparent);
}

.chip:hover .chip-icon-wrap svg {
  opacity: 1;
  filter: drop-shadow(0 0 3px color-mix(in srgb, var(--c) 50%, transparent));
}

/* ── 文字 ── */
.chip-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1;
  letter-spacing: 0.02em;
}

/* ── 色彩主题 ── */
.chip--cyan {
  --c: #45caff;
}
.chip--blue {
  --c: #3b82f6;
}
.chip--purple {
  --c: #a78bfa;
}
.chip--amber {
  --c: #f59e0b;
}
.chip--green {
  --c: #22c55e;
}
.chip--red {
  --c: #ef4444;
}
.chip--indigo {
  --c: #818cf8;
}
.chip--orange {
  --c: #f97316;
}
.chip--teal {
  --c: #2dd4bf;
}
.chip--lime {
  --c: #a3e635;
}

/* ── 会话列表 ── */
.session-list-wrap {
  overflow-y: auto;
  overflow-x: hidden;
  padding: 4px 2px 4px 8px;
  scrollbar-width: none;
}

.session-list-wrap:hover {
  scrollbar-width: thin;
  scrollbar-color: rgba(69, 202, 255, 0.12) transparent;
}

.session-list-wrap::-webkit-scrollbar {
  width: 5px;
}

.session-list-wrap::-webkit-scrollbar-track {
  background: transparent;
}

.session-list-wrap::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 4px;
  transition: background 0.3s;
}

.session-list-wrap:hover::-webkit-scrollbar-thumb {
  background: rgba(69, 202, 255, 0.15);
}

.session-list-wrap:hover::-webkit-scrollbar-thumb:hover {
  background: rgba(69, 202, 255, 0.3);
}

.session-group-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-sub);
  padding: 12px 8px 4px;
}

.session-row {
  width: 100%;
  height: 42px;
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) 24px;
  align-items: center;
  gap: 6px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #8a9cb4;
  text-align: left;
  padding: 0 8px;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.18s ease;
  position: relative;
}

.session-row:hover {
  background: rgba(69, 202, 255, 0.05);
  color: #c0d4e8;
  transform: translateX(2px);
}

.session-row.active {
  background: rgba(69, 202, 255, 0.08);
  color: #e8f9ff;
}

/* 对话图标（白色带圆框）*/
.session-icon {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1.2px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.04);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.session-icon svg {
  width: 11px;
  height: 11px;
}

.session-row.active .session-icon {
  border-color: rgba(69, 202, 255, 0.4);
  background: rgba(69, 202, 255, 0.08);
}

/* 三点菜单按钮 */
.session-more-btn {
  width: 24px;
  height: 24px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-dim);
  cursor: pointer;
  display: grid;
  place-items: center;
  opacity: 0;
  transition: all 0.15s ease;
  padding: 0;
}

.session-more-btn svg {
  width: 16px;
  height: 16px;
}

.session-row:hover .session-more-btn {
  opacity: 1;
}

.session-more-btn:hover {
  background: rgba(69, 202, 255, 0.1);
  color: var(--accent);
}

/* 右键菜单（Teleport 到 body，需 :global） */
:global(.session-context-menu) {
  position: fixed;
  z-index: 99999;
  min-width: 130px;
  background: rgba(12, 18, 30, 0.97);
  border: 1px solid rgba(69, 202, 255, 0.15);
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(14px);
}

:global(.session-context-menu button) {
  width: 100%;
  height: 34px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #b0c4d8;
  font-size: 12.5px;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  padding: 0 12px;
  transition: all 0.15s ease;
}

:global(.session-context-menu button:hover) {
  background: rgba(69, 202, 255, 0.08);
  color: #e0f0ff;
}

:global(.session-context-menu button.danger:hover) {
  background: rgba(240, 68, 56, 0.12);
  color: #f04438;
}

.session-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  line-height: 1;
  max-width: 12em;
}

.session-rename-input {
  flex: 1;
  min-width: 0;
  max-width: 12em;
  font-size: 13px;
  line-height: 1;
  color: #e8f9ff;
  background: rgba(69, 202, 255, 0.08);
  border: 1px solid var(--accent);
  border-radius: 4px;
  padding: 2px 6px;
  outline: none;
  font-family: inherit;
}

.session-empty {
  margin: 0;
  color: var(--text-dim);
  font-size: 12px;
  padding: 24px 10px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-icon {
  width: 28px;
  height: 28px;
  color: var(--text-dim);
  opacity: 0.5;
}

/* ── 底部状态栏 ── */
.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-top: 1px solid var(--line);
  font-size: 11px;
  color: var(--text-dim);
}

.footer-sep {
  opacity: 0.4;
}

.footer-status {
  display: flex;
  align-items: center;
  gap: 5px;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.online {
  background: #12b76a;
  box-shadow: 0 0 6px rgba(18, 183, 106, 0.5);
}

.status-dot.offline {
  background: #f04438;
  box-shadow: 0 0 6px rgba(240, 68, 56, 0.4);
}

/* ═══════════ 聊天主区域 ═══════════ */
.chat-main {
  display: grid;
  grid-template-rows: minmax(0, 1fr) auto;
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
  padding-top: 10px;
  padding-bottom: 2px;
}

.chat-main.initial-view {
  grid-template-rows: 1fr;
}

.chat-scroll {
  min-height: 0;
  overflow: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(69, 202, 255, 0.12) transparent;
}

.chat-scroll::-webkit-scrollbar {
  width: 6px;
}

.chat-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.chat-scroll::-webkit-scrollbar-thumb {
  background: rgba(69, 202, 255, 0.15);
  border-radius: 4px;
}

.chat-scroll::-webkit-scrollbar-thumb:hover {
  background: rgba(69, 202, 255, 0.3);
}

.chat-thread {
  max-width: 1040px;
  margin: 0 auto;
  padding: 26px 22px 8px;
  display: grid;
  gap: 24px;
}

/* ── 消息项 ── */
.message-item {
  display: flex;
  gap: 12px;
  animation: msg-in 0.3s ease both;
  animation-delay: var(--delay, 0s);
}

@keyframes msg-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}

.message-avatar svg {
  width: 28px;
  height: 28px;
}

.message-content-wrap {
  min-width: 0;
  max-width: 100%;
  position: relative;
}

.message-item.user .message-content-wrap {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 4px;
}

.message-meta strong {
  font-size: 13px;
  font-weight: 600;
  color: #c0d8ec;
}

.message-item.assistant .message-meta strong {
  color: var(--accent);
}

.message-meta span {
  color: var(--text-dim);
  font-size: 11px;
  font-family: var(--mono);
}

/* AI 消息体 */
.message-body.ai-body {
  background: transparent;
  padding: 0 0 0 14px;
  border-left: 3px solid rgba(69, 202, 255, 0.3);
  position: relative;
}

/* 用户消息体 */
.message-body.user-body {
  background: rgba(14, 22, 36, 0.85);
  border: 1px solid rgba(69, 202, 255, 0.15);
  border-radius: 14px 14px 4px 14px;
  padding: 10px 16px;
  max-width: 680px;
}

.message-body p {
  margin: 0 0 8px;
  line-height: 1.75;
  color: #dde8f2;
  word-break: break-word;
}

.message-body p:last-child {
  margin-bottom: 0;
}

/* 流式中纯文本容器：保留换行、字体与 markdown-body 一致 */
.streaming-plain {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-size: inherit;
  color: inherit;
}

/* markdown 渲染完成后淡入 */
.markdown-rendered {
  animation: md-fadein 0.18s ease;
}

@keyframes md-fadein {
  from {
    opacity: 0.6;
  }
  to {
    opacity: 1;
  }
}

/* 流式光标 */
.streaming-cursor {
  display: inline-block;
  color: var(--accent);
  font-weight: 700;
  animation: blink-cursor 0.8s steps(2) infinite;
  margin-left: 1px;
}

@keyframes blink-cursor {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 复制按钮 */
.msg-copy-btn {
  position: absolute;
  top: 0;
  right: -36px;
  width: 26px;
  height: 26px;
  border: none;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-dim);
  cursor: pointer;
  display: grid;
  place-items: center;
  opacity: 0;
  transition: all 0.18s ease;
}

.msg-copy-btn svg {
  width: 15px;
  height: 15px;
}

.message-content-wrap:hover .msg-copy-btn {
  opacity: 1;
}

.msg-copy-btn:hover {
  background: rgba(69, 202, 255, 0.1);
  color: var(--accent);
}

/* Markdown 渲染 — 表格 */
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  background: #05090f;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(69, 202, 255, 0.18);
  font-size: 13px;
  box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.5);
}

.markdown-body :deep(thead) {
  background: #071120;
  border-bottom: 1px solid rgba(69, 202, 255, 0.2);
}

.markdown-body :deep(th) {
  padding: 10px 12px;
  text-align: left;
  color: #45caff;
  font-weight: 700;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-shadow: 0 0 10px rgba(69, 202, 255, 0.35);
  border: none;
  border-right: 1px solid rgba(69, 202, 255, 0.08);
}

.markdown-body :deep(th:last-child) {
  border-right: none;
}

.markdown-body :deep(tbody) {
  background: transparent;
}

.markdown-body :deep(td) {
  padding: 9px 12px;
  color: #d4e2f0;
  font-size: 13px;
  font-weight: 500;
  text-align: left;
  vertical-align: middle;
  border-bottom: 1px solid rgba(69, 202, 255, 0.06);
  border-right: 1px solid rgba(69, 202, 255, 0.06);
  background: transparent;
}

.markdown-body :deep(td:last-child) {
  border-right: none;
}

.markdown-body :deep(tbody tr:nth-child(even) td) {
  background: rgba(69, 202, 255, 0.025);
}

.markdown-body :deep(tbody tr:last-child td) {
  border-bottom: none;
}

.markdown-body :deep(tbody tr:hover td) {
  background: rgba(69, 202, 255, 0.07);
}

.markdown-body :deep(code) {
  background: rgba(69, 202, 255, 0.08);
  padding: 1px 5px;
  border-radius: 3px;
  font-family: var(--mono);
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  background: rgba(6, 10, 18, 0.8);
  border: 1px solid rgba(69, 202, 255, 0.1);
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
}

/* ═══════════ 欢迎页 ═══════════ */
.composer-wrap {
  padding: 10px 16px 6px;
  background: transparent;
}

.chat-main.initial-view .composer-wrap {
  align-self: center;
  width: 100%;
  padding: 0 16px;
}

.welcome-block {
  text-align: center;
  transform: translateY(-70px);
}

.entry-greeting {
  margin: 0 auto 0;
  max-width: 1040px;
  color: #d0e0f0;
  font-size: 36px;
  line-height: 1.25;
  font-weight: 600;
  background: linear-gradient(135deg, #e8f4ff 0%, #45caff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* ═══════════ 输入框 ═══════════ */
.composer {
  max-width: 1040px;
  margin: 0 auto;
  background: rgba(10, 16, 28, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 10px 14px;
  transition: all 0.25s ease;
}

.composer.focused {
  border-color: rgba(69, 202, 255, 0.35);
  box-shadow:
    0 0 0 3px rgba(69, 202, 255, 0.06),
    0 2px 20px rgba(69, 202, 255, 0.08);
}

.composer textarea {
  width: 100%;
  min-height: 52px;
  resize: vertical;
  border: none;
  outline: none;
  font: inherit;
  background: transparent;
  color: #e8f0f8;
  line-height: 1.75;
  font-size: 14px;
  scrollbar-width: thin;
  scrollbar-color: rgba(69, 202, 255, 0.12) transparent;
}

.composer textarea::-webkit-scrollbar {
  width: 4px;
}

.composer textarea::-webkit-scrollbar-thumb {
  background: rgba(69, 202, 255, 0.15);
  border-radius: 4px;
}

.composer textarea::placeholder {
  color: #3a5068;
  font-size: 15px;
}

.composer-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

/* 左侧操作区 */
.composer-left-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.attach-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-sub);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.18s ease;
  padding: 0;
}

.attach-btn svg {
  width: 16px;
  height: 16px;
}

.attach-btn:hover {
  border-color: rgba(69, 202, 255, 0.3);
  color: var(--accent);
  background: rgba(69, 202, 255, 0.06);
}

/* 思考模式下拉 */
.think-mode-dropdown {
  position: relative;
}

.think-mode-trigger {
  height: 32px;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid rgba(69, 202, 255, 0.25);
  background: rgba(69, 202, 255, 0.06);
  color: var(--accent);
  font-size: 11.5px;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.22s ease;
  white-space: nowrap;
}

.think-mode-trigger:hover {
  border-color: rgba(69, 202, 255, 0.45);
  background: rgba(69, 202, 255, 0.12);
  color: #8ae0ff;
  box-shadow: 0 0 14px rgba(69, 202, 255, 0.1);
}

.think-mode-trigger .think-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
  color: var(--accent);
  opacity: 0.85;
  filter: drop-shadow(0 0 3px rgba(69, 202, 255, 0.4));
}

.think-mode-trigger .chevron-icon {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  transition: transform 0.2s ease;
  opacity: 0.6;
  color: var(--accent);
}

.think-mode-trigger .chevron-icon.open {
  transform: rotate(180deg);
}

.think-dropdown-menu {
  position: absolute;
  bottom: calc(100% + 6px);
  left: 0;
  z-index: 9999;
  min-width: 200px;
  background: rgba(12, 18, 30, 0.97);
  border: 1px solid rgba(69, 202, 255, 0.15);
  border-radius: 10px;
  padding: 4px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(14px);
}

.think-dropdown-menu button {
  width: 100%;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #8a9cb4;
  font-family: inherit;
  cursor: pointer;
  text-align: left;
  padding: 8px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  transition: all 0.15s ease;
}

.think-dropdown-menu button:hover {
  background: rgba(69, 202, 255, 0.06);
  color: #d0e8ff;
}

.think-dropdown-menu button.active {
  background: rgba(69, 202, 255, 0.1);
}

.think-dropdown-menu button.active .dropdown-label {
  color: var(--accent);
  font-weight: 600;
}

.dropdown-label {
  font-size: 12.5px;
  color: #b0c4d8;
}

.dropdown-desc {
  font-size: 10.5px;
  color: var(--text-dim);
}

.send-btn {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  border: 1px solid rgba(69, 202, 255, 0.4);
  background: linear-gradient(
    135deg,
    rgba(69, 202, 255, 0.2),
    rgba(69, 202, 255, 0.08)
  );
  color: var(--accent);
  display: inline-grid;
  place-items: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.send-btn svg {
  width: 20px;
  height: 20px;
  display: block;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.send-btn .stop-icon {
  stroke: none;
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(
    135deg,
    rgba(69, 202, 255, 0.35),
    rgba(69, 202, 255, 0.15)
  );
  box-shadow: 0 0 16px rgba(69, 202, 255, 0.2);
}

.send-btn.sending {
  background: rgba(240, 68, 56, 0.15);
  border-color: rgba(240, 68, 56, 0.4);
  color: #f04438;
  animation: stop-pulse 2s ease-in-out infinite;
}

@keyframes stop-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 transparent;
  }
  50% {
    box-shadow: 0 0 12px rgba(240, 68, 56, 0.2);
  }
}

.send-btn.sending:hover {
  background: rgba(240, 68, 56, 0.25);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* ═══════════ 响应式 ═══════════ */
@media (max-width: 980px) {
  .ai-shell::before {
    display: none;
  }

  .ai-shell {
    grid-template-columns: 1fr;
    height: auto;
  }

  .sidebar {
    width: 100%;
    min-width: 0;
    max-width: none;
    border-bottom: 1px solid var(--line);
    max-height: 360px;
  }

  .chat-thread {
    padding: 20px 14px 14px;
  }

  .chat-main {
    padding-top: 0;
    padding-bottom: 0;
  }

  .entry-greeting {
    font-size: 28px;
    padding: 0 2px;
  }

  .msg-copy-btn {
    right: 0;
    top: -28px;
  }
}

@media (max-width: 640px) {
  .entry-greeting {
    font-size: 24px;
  }

  .composer textarea {
    min-height: 60px;
  }

  .chips-grid {
    grid-template-columns: 1fr;
  }
}

/* ═══════════ 报告卡片 ═══════════ */
.report-card {
  background: linear-gradient(
    135deg,
    rgba(10, 20, 40, 0.95) 0%,
    rgba(6, 14, 28, 0.98) 100%
  );
  border: 1px solid rgba(69, 202, 255, 0.2);
  border-radius: 14px;
  overflow: hidden;
  margin: 4px 0;
}

.rc-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(
    90deg,
    rgba(69, 202, 255, 0.1) 0%,
    transparent 100%
  );
  border-bottom: 1px solid rgba(69, 202, 255, 0.12);
}

.rc-header-icon {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.rc-title {
  font-size: 15px;
  font-weight: 700;
  color: #e0ecf8;
  letter-spacing: 0.3px;
}

.rc-subtitle {
  font-size: 11px;
  color: #4a5f78;
  margin-top: 2px;
}

.rc-badge {
  margin-left: auto;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 600;
  color: #45caff;
  background: rgba(69, 202, 255, 0.1);
  border: 1px solid rgba(69, 202, 255, 0.25);
  letter-spacing: 0.5px;
}

/* KPI 格子 */
.rc-kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: rgba(69, 202, 255, 0.08);
  border-bottom: 1px solid rgba(69, 202, 255, 0.12);
}

.rc-kpi-item {
  padding: 14px 12px;
  background: rgba(7, 11, 20, 0.6);
  text-align: center;
}

.rc-kpi-item.rc-kpi-warn .rc-kpi-val {
  color: #f97316;
}

.rc-kpi-val {
  font-size: 22px;
  font-weight: 700;
  color: #45caff;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.rc-kpi-label {
  font-size: 11px;
  color: #4a5f78;
  margin-top: 4px;
}

.rc-kpi-label span {
  color: #3a4c62;
  margin-left: 2px;
}

/* 节标题 */
.rc-section-title {
  padding: 12px 20px 6px;
  font-size: 12px;
  font-weight: 600;
  color: #45caff;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  border-bottom: 1px solid rgba(69, 202, 255, 0.08);
}

/* 表格 */
.rc-table-wrap {
  padding: 0 20px 12px;
  overflow-x: auto;
}

.rc-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 11.5px;
  margin-top: 8px;
}

.rc-table th {
  background: rgba(26, 58, 92, 0.7);
  color: #9bb8d4;
  font-weight: 600;
  padding: 7px 10px;
  text-align: center;
  white-space: nowrap;
  border: 1px solid rgba(69, 202, 255, 0.1);
}

.rc-table td {
  padding: 6px 10px;
  text-align: center;
  color: #c8dcea;
  border: 1px solid rgba(69, 202, 255, 0.07);
}

.rc-table tbody tr:nth-child(even) td {
  background: rgba(20, 40, 65, 0.3);
}

.rc-table tbody tr:hover td {
  background: rgba(69, 202, 255, 0.06);
}

.rc-mono {
  font-family: "Cascadia Code", "Fira Mono", monospace;
  font-size: 10.5px;
}

.rc-td-warn {
  color: #f97316 !important;
  font-weight: 600;
}

.rc-empty {
  padding: 20px;
  text-align: center;
  color: #3a4c62;
  font-size: 13px;
}

/* 类别条形 */
.rc-cat-bars {
  padding: 8px 20px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rc-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rc-bar-label {
  font-size: 11.5px;
  color: #9bb8d4;
  width: 120px;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rc-bar-track {
  flex: 1;
  height: 8px;
  background: rgba(69, 202, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.rc-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #1a78c2, #45caff);
  border-radius: 4px;
  transition: width 0.5s ease;
  min-width: 2px;
}

.rc-bar-count {
  font-size: 12px;
  font-weight: 600;
  color: #45caff;
  width: 32px;
  text-align: right;
  flex-shrink: 0;
}

/* 操作区 */
.rc-actions {
  padding: 14px 20px 18px;
  border-top: 1px solid rgba(69, 202, 255, 0.1);
  display: flex;
  justify-content: flex-end;
}

.rc-download-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 22px;
  background: linear-gradient(135deg, #1a78c2 0%, #45caff 100%);
  color: #070b14;
  font-size: 13px;
  font-weight: 700;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition:
    opacity 0.2s,
    transform 0.15s;
  letter-spacing: 0.3px;
}

.rc-download-btn:hover:not(:disabled) {
  opacity: 0.88;
  transform: translateY(-1px);
}

.rc-download-btn:disabled,
.rc-download-btn.loading {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.rc-download-btn svg {
  width: 16px;
  height: 16px;
}

.rc-spin,
.export-toast-spin {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(7, 11, 20, 0.3);
  border-top-color: #070b14;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
  display: inline-block;
}

.rc-loading {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #4a5f78;
  font-size: 13px;
  justify-content: center;
}

.rc-loading .rc-spin {
  border-top-color: #45caff;
  border-color: rgba(69, 202, 255, 0.2);
  border-top-color: #45caff;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ═══════════ 报告卡片图表网格 ═══════════ */
.rc-charts-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 8px 20px 16px;
}

.rc-chart-wide {
  grid-column: span 2;
}

.rc-chart-item {
  background: rgba(7, 11, 20, 0.7);
  border: 1px solid rgba(69, 202, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
}

.rc-chart-caption {
  font-size: 11px;
  font-weight: 600;
  color: #4a5f78;
  padding: 6px 12px;
  border-bottom: 1px solid rgba(69, 202, 255, 0.07);
  text-align: center;
  letter-spacing: 0.3px;
}

.rc-chart-img {
  width: 100%;
  display: block;
  object-fit: contain;
}
</style>
