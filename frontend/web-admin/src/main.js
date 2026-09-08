import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./styles/theme.css";
import "./styles/main.css";
import "./styles/chrome.css";
import "./styles/admin-glass.css";
import "./styles/mes-table.css";
import "./styles/planning-theme.css";
import "./styles/page-hero.css";
import "./styles/spirit-companion.css";
import "@/yunshu-ui/assets/icons/register.js";

createApp(App).use(createPinia()).use(router).mount("#app");
