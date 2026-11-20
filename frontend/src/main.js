// Vue 앱에 라우터와 전역 스타일을 결합한 뒤 index.html의 #app에 마운트합니다.
import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router.js";
import { loadProducts } from "./data/products";
import { loadNotices } from "./data/notices";
import "./styles/global.css";
import "./styles/corrections.css";
createApp(App).use(createPinia()).use(router).mount("#app");
// 로컬 시드 대신 실제 상품 API가 응답한 뒤 목록을 갱신합니다.
loadProducts().catch(() => {});
loadNotices().catch(() => {});
