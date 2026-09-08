<template>
  <aside
    v-if="open"
    id="agent-overview-panel"
    class="agent-overview-panel"
    aria-label="云枢小智 Agent 总览"
    @keydown.esc="$emit('close')"
  >
    <header class="overview-head">
      <div>
        <div class="overview-eyebrow"><span aria-hidden="true">✦</span> YUNSHU AGENT NETWORK</div>
        <h3>Agent 总览</h3>
        <p>16 个业务子 Agent · 按需协作</p>
      </div>
      <button ref="closeButton" type="button" class="overview-close" aria-label="关闭 Agent 总览" @click="$emit('close')">&times;</button>
    </header>

    <div class="overview-body">
      <section class="agent-grid" aria-label="16 个子 Agent">
        <article v-for="(agent, index) in agents" :key="agent.id" class="agent-card" :class="`agent-card--${agent.tone}`">
          <div class="agent-card-top">
            <span class="agent-orb" :class="`agent-orb--${agent.tone}`" aria-hidden="true"><i></i><b></b></span>
            <span class="agent-index">{{ String(index + 1).padStart(2, '0') }}</span>
          </div>
          <h4>{{ agent.label }}</h4>
          <p>{{ agent.module }}</p>
          <span class="agent-state" :class="`agent-state--${agent.status}`">{{ agent.status === 'active' ? '已接入' : '待接入' }}</span>
        </article>
      </section>

      <div class="overview-foot" aria-hidden="true"><span></span><i>实时数据 · 受控工具 · 可审计</i><span></span></div>
    </div>
  </aside>
</template>

<script setup>
import { nextTick, onMounted, ref, watch } from "vue";

const props = defineProps({
  open: Boolean,
  agents: { type: Array, default: () => [] },
});
defineEmits(["close"]);

const closeButton = ref(null);
watch(() => props.open, async (open) => {
  if (open) {
    await nextTick();
    closeButton.value?.focus();
  }
});
onMounted(() => {
  if (props.open) closeButton.value?.focus();
});
</script>

<style scoped>
.agent-overview-panel {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 12;
  display: flex;
  width: min(760px, 100%);
  flex-direction: column;
  overflow: hidden;
  border-left: 1px solid rgba(159, 181, 224, 0.52);
  background:
    radial-gradient(circle at 93% 5%, rgba(255, 215, 193, 0.34), transparent 24%),
    radial-gradient(circle at 8% 25%, rgba(207, 221, 255, 0.48), transparent 30%),
    linear-gradient(148deg, rgba(255, 255, 255, 0.98), rgba(246, 250, 255, 0.98) 55%, rgba(255, 249, 246, 0.98));
  box-shadow: -16px 0 42px rgba(62, 81, 132, 0.14);
  backdrop-filter: blur(18px);
  animation: overview-enter 220ms ease-out both;
}
.overview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 21px 23px 17px;
  border-bottom: 1px solid rgba(181, 198, 229, 0.54);
}
.overview-eyebrow { color: #8998c6; font-size: 11px; font-weight: 800; letter-spacing: .12em; }
.overview-eyebrow span { margin-right: 5px; color: #8292df; font-size: 14px; }
.overview-head h3 { margin: 4px 0 0; color: #3f5279; font-size: 25px; font-weight: 850; letter-spacing: .055em; }
.overview-head p { margin: 6px 0 0; color: #8b98b2; font-size: 14px; font-weight: 650; letter-spacing: .045em; }
.overview-close {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  padding: 0;
  border: 1px solid rgba(184, 199, 229, .64);
  border-radius: 10px;
  background: rgba(255,255,255,.62);
  color: #8392af;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: background .16s ease, color .16s ease, border-color .16s ease, transform .16s ease;
}
.overview-close:hover { border-color: rgba(127, 152, 212, .8); background: rgba(246, 248, 255, .96); color: #5d75b6; transform: rotate(4deg); }
.overview-close:focus-visible { outline: 3px solid rgba(126, 152, 219, .25); outline-offset: 2px; }
.overview-body { flex: 1; overflow: auto; padding: 20px 23px 30px; scrollbar-gutter: stable; }
.agent-orb { position: relative; display: inline-block; overflow: hidden; border-radius: 48% 52% 50% 50% / 52% 49% 51% 48%; background: linear-gradient(145deg, #e3f3ff, #9bc7ec 56%, #a993df); box-shadow: inset -3px -4px 8px rgba(90, 87, 172, .1), inset 2px 2px 5px rgba(255,255,255,.68), 0 4px 10px rgba(86, 115, 181, .16); }
.agent-orb::before { content: ""; position: absolute; top: 17%; left: 18%; width: 30%; height: 15%; border-radius: 50%; background: rgba(255,255,255,.58); filter: blur(1px); transform: rotate(-30deg); }
.agent-orb i, .agent-orb b { position: absolute; top: 39%; z-index: 1; width: 11%; height: 18%; box-sizing: border-box; border: 1px solid rgba(44, 78, 116, .2); border-radius: 48% 52% 50% 50%; background: linear-gradient(160deg, #7898ba 0%, #4f7195 55%, #385776 100%); box-shadow: inset 0 1px 1px rgba(255,255,255,.56), 0 1px 1px rgba(56,86,118,.16); }
.agent-orb i { --eye-tilt: 0deg; left: 32%; animation: agent-eye-left 5.8s ease-in-out infinite; }
.agent-orb b { --eye-tilt: 0deg; left: 57%; animation: agent-eye-right 5.8s ease-in-out infinite; }
.agent-orb i::after, .agent-orb b::after { content: ""; position: absolute; top: 16%; left: 22%; width: 32%; height: 24%; border-radius: 50%; background: rgba(255,255,255,.9); box-shadow: 0 0 2px rgba(255,255,255,.6); }
.agent-card:nth-child(4n + 2) .agent-orb i, .agent-card:nth-child(4n + 2) .agent-orb b { animation-delay: -1.45s; }
.agent-card:nth-child(4n + 3) .agent-orb i, .agent-card:nth-child(4n + 3) .agent-orb b { animation-delay: -2.85s; }
.agent-card:nth-child(4n) .agent-orb i, .agent-card:nth-child(4n) .agent-orb b { animation-delay: -4.1s; }
.agent-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.agent-card { position: relative; min-width: 0; min-height: 126px; padding: 9px 12px 8px; overflow: hidden; border: 1px solid rgba(190, 205, 235, .62); border-radius: 16px; background: rgba(255,255,255,.68); box-shadow: 0 7px 18px rgba(74, 99, 153, .045); transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease; }
.agent-card::after { content:""; position:absolute; right:-19px; bottom:-25px; width:64px; height:64px; border-radius:50%; background:var(--agent-glow, rgba(159,181,229,.16)); filter:blur(1px); opacity:.42; }
.agent-card:hover { z-index: 1; border-color: var(--agent-border, rgba(133,160,219,.75)); box-shadow: 0 11px 24px rgba(68, 94, 152, .11); transform: translateY(-2px); }
.agent-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 7px; }
.agent-orb { width: 37px; height: 37px; }
.agent-index { color: #a2adc1; font-size: 11px; font-weight: 800; letter-spacing: .06em; }
.agent-card h4 { position: relative; z-index: 1; min-height: 32px; margin: 6px 0 2px; color: #506383; font-size: 14px; font-weight: 800; line-height: 1.3; letter-spacing: .01em; }
.agent-card p { position: relative; z-index: 1; min-height: 17px; margin: 0; overflow: hidden; color: #909bb0; font-size: 12px; line-height: 1.4; text-overflow: ellipsis; white-space: nowrap; }
.agent-state { position: relative; z-index: 1; display: inline-flex; margin-top: 4px; padding: 3px 7px; border-radius: 99px; font-size: 10px; font-weight: 800; letter-spacing: .035em; }
.agent-state--active { background: rgba(216, 243, 228, .82); color: #4c9670; }
.agent-state--ready { background: rgba(231, 236, 249, .82); color: #7789ac; }
.agent-card--intent { --agent-glow: rgba(173, 159, 232, .24); --agent-border: rgba(152, 136, 218, .75); }
.agent-card--intake { --agent-glow: rgba(141, 199, 233, .23); --agent-border: rgba(115, 174, 211, .72); }
.agent-card--bom { --agent-glow: rgba(143, 205, 173, .22); --agent-border: rgba(108, 176, 143, .72); }
.agent-card--risk { --agent-glow: rgba(244, 194, 128, .24); --agent-border: rgba(219, 162, 88, .72); }
.agent-card--lock { --agent-glow: rgba(141, 182, 227, .23); --agent-border: rgba(103, 149, 210, .7); }
.agent-card--schedule { --agent-glow: rgba(188, 166, 237, .24); --agent-border: rgba(151, 134, 214, .72); }
.agent-card--dispatch { --agent-glow: rgba(129, 210, 220, .22); --agent-border: rgba(86, 173, 191, .7); }
.agent-card--validate { --agent-glow: rgba(133, 211, 167, .22); --agent-border: rgba(85, 173, 129, .7); }
.agent-card--issue { --agent-glow: rgba(235, 161, 182, .23); --agent-border: rgba(207, 123, 153, .68); }
.agent-card--production { --agent-glow: rgba(239, 174, 143, .24); --agent-border: rgba(214, 132, 96, .7); }
.agent-card--quality { --agent-glow: rgba(238, 207, 125, .24); --agent-border: rgba(205, 165, 63, .7); }
.agent-card--warehouse { --agent-glow: rgba(117, 202, 215, .23); --agent-border: rgba(69, 169, 187, .7); }
.agent-card--trace { --agent-glow: rgba(192, 162, 232, .24); --agent-border: rgba(157, 119, 207, .7); }
.agent-card--equipment { --agent-glow: rgba(132, 183, 199, .23); --agent-border: rgba(86, 145, 167, .7); }
.agent-card--andon { --agent-glow: rgba(247, 173, 114, .25); --agent-border: rgba(224, 131, 67, .72); }
.agent-card--insight { --agent-glow: rgba(138, 158, 229, .24); --agent-border: rgba(99, 123, 204, .72); }
.agent-orb--intent { background: linear-gradient(145deg,#e9ecff,#b8c5ff 58%,#a996e4); }.agent-orb--intake { background:linear-gradient(145deg,#e5f8ff,#9bd8ec 55%,#92b7e2); }.agent-orb--bom { background:linear-gradient(145deg,#eff9ee,#a9d9bd 55%,#84b9ad); }.agent-orb--risk { background:linear-gradient(145deg,#fff6e6,#f2ca93 55%,#d6a77e); }.agent-orb--lock { background:linear-gradient(145deg,#edf7ff,#a8d2f0 55%,#8aa8da); }.agent-orb--schedule { background:linear-gradient(145deg,#f1efff,#c7b6ee 55%,#9da2e4); }.agent-orb--dispatch { background:linear-gradient(145deg,#eafaff,#9cd9e7 55%,#79adc8); }.agent-orb--validate { background:linear-gradient(145deg,#edfff5,#a7e2c1 55%,#78bd9f); }.agent-orb--issue { background:linear-gradient(145deg,#fff0f2,#efb2c1 55%,#c98ead); }.agent-orb--production { background:linear-gradient(145deg,#fff0e9,#f0b38e 55%,#d88769); }.agent-orb--quality { background:linear-gradient(145deg,#fff8df,#f0d37d 55%,#d0aa56); }.agent-orb--warehouse { background:linear-gradient(145deg,#e5fbff,#9adbe4 55%,#6daabd); }.agent-orb--trace { background:linear-gradient(145deg,#f6eeff,#ceb9ed 55%,#aa8bd2); }.agent-orb--equipment { background:linear-gradient(145deg,#e8f7fa,#a5ccd4 55%,#789caf); }.agent-orb--andon { background:linear-gradient(145deg,#fff1e7,#f2b37d 55%,#d98260); }.agent-orb--insight { background:linear-gradient(145deg,#edf0ff,#b8c5f2 55%,#8999dc); }
.overview-foot { display:flex; align-items:center; gap:8px; margin:22px 2px 0; color:#a0acc2; font-size:11px; letter-spacing:.045em; white-space:nowrap; }.overview-foot span { height:1px; flex:1; background:linear-gradient(90deg,transparent,rgba(177,191,221,.66)); }.overview-foot span:last-child { transform:scaleX(-1); }.overview-foot i { font-style:normal; }
@keyframes overview-enter { from { opacity:0; transform:translateX(18px); } to { opacity:1; transform:translateX(0); } }
@keyframes agent-eye-left { 0%, 13%, 65%, 100% { transform: translate(0, 0) rotate(var(--eye-tilt)) scaleY(1); } 22%, 31% { transform: translate(-1px, -0.55px) rotate(var(--eye-tilt)) scaleY(1); } 43%, 49% { transform: translate(1px, -0.35px) rotate(var(--eye-tilt)) scaleY(1); } 54%, 56% { transform: translate(1px, -0.35px) rotate(var(--eye-tilt)) scaleY(.16); } 76%, 82% { transform: translate(-.55px, .25px) rotate(var(--eye-tilt)) scaleY(1); } }
@keyframes agent-eye-right { 0%, 13%, 65%, 100% { transform: translate(0, 0) rotate(var(--eye-tilt)) scaleY(1); } 22%, 31% { transform: translate(-1px, -0.55px) rotate(var(--eye-tilt)) scaleY(1); } 43%, 49% { transform: translate(1px, -0.35px) rotate(var(--eye-tilt)) scaleY(1); } 54%, 56% { transform: translate(1px, -0.35px) rotate(var(--eye-tilt)) scaleY(.16); } 76%, 82% { transform: translate(-.55px, .25px) rotate(var(--eye-tilt)) scaleY(1); } }
@media (max-width: 690px) { .agent-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .agent-card { min-height: 120px; } }
</style>
