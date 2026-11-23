// Vue 앱에 라우터와 전역 스타일을 결합한 뒤 index.html의 #app에 마운트합니다.
import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router.js";
import "./styles/global.css";
import "./styles/corrections.css";
import "./styles/admin.css";
import "./styles/admin-overview.css";
import "./styles/admin-operations.css";
createApp(App).use(createPinia()).use(router).mount("#app");
