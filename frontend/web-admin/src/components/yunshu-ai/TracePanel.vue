<template>
  <aside v-if="open" id="agent-workflow-panel" class="trace-panel" aria-label="Agent 工作流程">
    <header class="trace-head">
      <div>
        <h3>Agent 工作流程</h3>
        <p>规划 · 操作 · 工具调用</p>
      </div>
      <button type="button" class="trace-close" aria-label="关闭工作流程" @click="$emit('close')">&times;</button>
    </header>
    <div class="trace-body">
      <div v-if="!status && !plan?.length && !agents?.length && !steps?.length && !toolCalls?.length && !events?.length" class="trace-empty">
        <span aria-hidden="true">✦</span>
        <strong>等待新的任务</strong>
        <p>小智开始执行后，这里会展示任务规划、操作步骤与工具调用记录。</p>
      </div>
      <p v-if="status" class="trace-status">状态：<strong>{{ status }}</strong></p>
      <section v-if="events?.length" class="agent-event-section">
        <h4>子 Agent 执行记录</h4>
        <article v-for="event in events" :key="event.event_id" class="agent-event" :class="`is-${event.status}`">
          <div class="event-avatar" :class="avatarClass(event.agent)"><i></i><b></b></div>
          <div class="event-content">
            <div class="event-meta"><strong>{{ displayName(event.agent) }}</strong><span>{{ statusText(event.status) }}</span></div>
            <div class="event-title">{{ event.title }}</div>
            <p>{{ event.summary }}</p>
            <small v-if="event.sources?.length">数据依据 · {{ event.sources.join(' · ') }}</small>
          </div>
        </article>
      </section>
      <div v-if="plan?.length || agents?.length || steps?.length || toolCalls?.length" class="trace-details">
        <section v-if="plan?.length" class="trace-detail-card trace-detail-card--plan">
          <h4>执行计划</h4>
          <ol class="trace-plan"><li v-for="(p, i) in plan" :key="i">{{ p }}</li></ol>
        </section>
        <section v-if="agents?.length" class="trace-detail-card trace-detail-card--agents">
          <h4>参与 Agent</h4>
          <div class="badges"><span v-for="a in agents" :key="a" class="badge">{{ displayName(a) }}</span></div>
        </section>
        <section v-if="steps?.length" class="trace-detail-card trace-detail-card--steps">
          <h4>执行步骤</h4>
          <ul class="steps">
            <li v-for="(s, i) in steps" :key="i"><strong>{{ s.step }}</strong><span>{{ s.detail }}</span></li>
          </ul>
        </section>
        <section v-if="toolCalls?.length" class="trace-detail-card trace-detail-card--tools">
          <h4>工具调用</h4>
          <div class="tool-table">
            <div v-for="(t, i) in toolCalls" :key="i" class="tool-row">
              <code :title="t.tool_name">{{ t.tool_name }}</code>
              <span>{{ displayName(t.agent_name) }}</span>
            </div>
          </div>
        </section>
      </div>
      <div v-if="status || events?.length || plan?.length || agents?.length || steps?.length || toolCalls?.length" class="trace-tail" aria-hidden="true">
        <span></span><i>已记录至 Agent 审计轨迹</i><span></span>
      </div>
    </div>
  </aside>
</template>

<script setup>
defineProps({
  open: Boolean,
  status: String,
  plan: Array,
  agents: Array,
  steps: Array,
  toolCalls: Array,
  events: Array,
});
defineEmits(["close"]);

const displayName = (name) => ({
  IntentRoutingAgent: "意图识别 Agent",
  OrderIntakeAgent: "订单接收 Agent", BomRouteAgent: "BOM与工艺路线 Agent",
  KittingRiskAgent: "齐套与物料风险 Agent", KittingExecutionAgent: "锁料执行 Agent",
  CapacitySchedulingAgent: "智能排产 Agent", DispatchExecutionAgent: "派工执行 Agent",
  SchedulingValidationAgent: "排产校验 Agent", ProductionIssueAgent: "生产领料 Agent",
})[name] || name;
const statusText = (status) => ({ waiting: "等待输入", completed: "已完成", blocked: "已阻塞", running: "执行中" })[status] || "已记录";
const avatarClass = (name) => `avatar-${String(name || '').replace('Agent', '').toLowerCase()}`;
</script>

<style scoped>
.trace-panel {
  position: absolute; top: 0; right: 0; bottom: 0; width: min(700px, 100%);
  background:
    radial-gradient(circle at 96% 3%, rgba(255, 219, 199, 0.3), transparent 25%),
    radial-gradient(circle at 5% 24%, rgba(217, 225, 255, 0.42), transparent 29%),
    linear-gradient(150deg, rgba(255, 255, 255, 0.97), rgba(248, 251, 255, 0.97) 56%, rgba(255, 249, 246, 0.97));
  border-left: 1px solid rgba(163, 184, 223, 0.45); z-index: 10;
  display: flex; flex-direction: column; box-shadow: -14px 0 38px rgba(62, 81, 132, 0.12);
  backdrop-filter: blur(18px);
}
.trace-head { display: flex; align-items: center; justify-content: space-between; padding: 18px 18px 15px; border-bottom: 1px solid rgba(181, 198, 229, 0.5); }
.trace-head h3 { margin: 0; color: #3f5279; font-size: 17px; font-weight: 800; letter-spacing: .045em; }
.trace-head p { margin: 5px 0 0; color: #909bb2; font-size: 11px; font-weight: 650; letter-spacing: .07em; }
.trace-close { display: grid; width: 30px; height: 30px; place-items: center; padding: 0; border: 1px solid rgba(184, 199, 229, 0.58); border-radius: 9px; background: rgba(255,255,255,.55); font-size: 21px; line-height: 1; color: #8a98b4; cursor: pointer; transition: background .16s ease, color .16s ease, border-color .16s ease; }
.trace-close:hover { border-color: rgba(132, 157, 213, .7); background: rgba(245, 247, 255, .9); color: #5d75b6; }
.trace-body { flex: 1; overflow: auto; padding: 17px 18px 30px; font-size: 13px; color: #53617a; scrollbar-gutter: stable; }
.trace-empty { margin: 22px 0; padding: 22px 17px; border: 1px dashed rgba(173, 193, 231, .76); border-radius: 16px; background: rgba(255,255,255,.44); color: #8b98b0; text-align: center; }
.trace-empty > span { display: block; margin-bottom: 8px; color: #8495dc; font-size: 20px; }
.trace-empty strong { display: block; color: #5c7097; font-size: 14px; }
.trace-empty p { margin: 8px 0 0; font-size: 12px; line-height: 1.7; }
.trace-body h4 { margin: 18px 0 8px; color: #7988a8; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: .08em; }
.trace-body h4:first-child { margin-top: 0; }
.trace-status { margin: 0 0 12px; padding: 9px 11px; border: 1px solid rgba(187, 201, 232, .48); border-radius: 10px; background: rgba(239, 244, 255, .68); color: #7282a2; }
.trace-status strong { color: #526fb2; }
.trace-details { display:grid; grid-template-columns:repeat(2, minmax(0, 1fr)); gap:10px; margin-top:15px; padding-top:15px; border-top:1px solid rgba(195,207,232,.5); }
.trace-detail-card { min-width:0; padding:12px; border:1px solid rgba(191,205,233,.58); border-radius:13px; background:rgba(255,255,255,.57); box-shadow:0 6px 16px rgba(82,105,157,.035); }
.trace-detail-card h4 { margin:0 0 9px; color:#7484a6; font-size:10px; letter-spacing:.09em; }
.trace-detail-card--plan, .trace-detail-card--steps, .trace-detail-card--tools { grid-column:1 / -1; }
.trace-plan { display:grid; gap:7px; margin:0; padding-left:19px; color:#687893; font-size:12px; line-height:1.55; }
.trace-plan li { padding-left:2px; }
.badges { display: flex; flex-wrap: wrap; gap:6px; }
.badge { max-width:100%; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:11px; padding:5px 8px; background:rgba(225,233,255,.74); border:1px solid rgba(184,202,237,.55); border-radius:999px; color:#6177aa; }
.steps { list-style:none; padding:0; margin:0; }
.steps li { padding:9px 0; border-bottom:1px solid rgba(207,217,236,.5); display:grid; gap:3px; }
.steps li:last-child { padding-bottom:0; border-bottom:0; }
.steps strong { font-size: 13px; color: #506383; }
.steps span { font-size: 12px; color: #8b98ad; line-height: 1.55; }
.tool-table { overflow:hidden; border:1px solid rgba(207,217,236,.56); border-radius:9px; background:rgba(247,250,255,.72); }
.tool-row { display:grid; grid-template-columns:minmax(0, 1fr) auto; align-items:center; gap:12px; min-height:35px; padding:0 10px; border-bottom:1px solid rgba(207,217,236,.48); }
.tool-row:last-child { border-bottom:0; }
.tool-row code { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; font-size:11px; color:#546b99; }
.tool-row span { max-width:150px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:#8b98ad; font-size:11px; }
.trace-tail { display:flex; align-items:center; gap:8px; margin:18px 2px 2px; color:#a1acc0; font-size:10px; letter-spacing:.035em; white-space:nowrap; }
.trace-tail span { height:1px; flex:1; background:linear-gradient(90deg, transparent, rgba(177,191,221,.66)); }
.trace-tail span:last-child { transform:scaleX(-1); }
.trace-tail i { font-style:normal; }
.agent-event-section { display:grid; gap:9px; }
.agent-event { display:flex; gap:10px; padding:11px; border:1px solid rgba(188,203,233,.58); border-radius:14px; background:rgba(255,255,255,.72); box-shadow:0 7px 18px rgba(81,105,162,.055); }
.agent-event.is-blocked { border-color:rgba(220,171,104,.52); background:rgba(255,248,237,.8); }
.event-avatar { position:relative; width:31px; height:31px; flex:0 0 31px; border-radius:50%; background:linear-gradient(145deg,#bcd8ff,#6c96df); box-shadow:inset 0 2px 4px rgba(255,255,255,.65),0 3px 8px rgba(75,110,180,.22); }
.event-avatar i,.event-avatar b { position:absolute; top:12px; width:3px; height:6px; border-radius:50%; background:#385477; } .event-avatar i{left:10px}.event-avatar b{right:10px}
.avatar-bomroute{background:linear-gradient(145deg,#d8c8ff,#8d78d5)} .avatar-kittingrisk,.avatar-kittingexecution{background:linear-gradient(145deg,#ffe4a8,#d79b4f)} .avatar-capacityscheduling,.avatar-dispatchexecution{background:linear-gradient(145deg,#b9e8ff,#5f9dce)} .avatar-productionissue{background:linear-gradient(145deg,#c5f3d4,#60a87d)}
.event-content{min-width:0;flex:1}.event-meta{display:flex;justify-content:space-between;gap:8px;color:#506383;font-size:12px}.event-meta span{color:#7390bd;font-size:10px;font-weight:800}.is-blocked .event-meta span{color:#b77c3f}.event-title{margin-top:4px;color:#425878;font-size:12px;font-weight:800}.event-content p{margin:4px 0;color:#71809a;font-size:12px;line-height:1.55}.event-content small{display:block;color:#9ba6b8;font-size:10px;line-height:1.45;word-break:break-word}
@media (max-width: 580px) { .trace-details { grid-template-columns:1fr; } .trace-detail-card--plan, .trace-detail-card--steps, .trace-detail-card--tools { grid-column:auto; } .tool-row { grid-template-columns:minmax(0,1fr); gap:1px; padding:7px 10px; } .tool-row span { max-width:none; } }
</style>
