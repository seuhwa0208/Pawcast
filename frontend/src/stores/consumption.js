// 소진일 도메인의 화면 상태를 캡슐화합니다. 백엔드 연동 시 각 액션 내부를 Service 호출로 교체합니다.
import { ref } from "vue";
import { defineStore, storeToRefs } from "pinia";

const ITEM_KEY = "powcast-consumption-items";
const SETTING_KEY = "powcast-consumption-settings";

const useConsumptionStore = defineStore("consumption", () => {
  const items = ref(JSON.parse(localStorage.getItem(ITEM_KEY) || "[]"));
  const settings = ref(
    JSON.parse(localStorage.getItem(SETTING_KEY) || "null") || {
      petWeight: 5.2,
      dailyAmount: 36,
      dailyCount: 2,
    },
  );

  const persistItems = () =>
    localStorage.setItem(ITEM_KEY, JSON.stringify(items.value));

  function addItem(payload) {
    items.value.unshift({ id: Date.now(), ...payload });
    persistItems();
  }
  function updateItem(id, payload) {
    const index = items.value.findIndex((item) => item.id === id);
    if (index === -1) return;
    items.value[index] = { ...items.value[index], ...payload };
    persistItems();
  }
  function saveSettings(payload) {
    settings.value = { ...settings.value, ...payload };
    localStorage.setItem(SETTING_KEY, JSON.stringify(settings.value));
  }

  return { items, settings, addItem, updateItem, saveSettings };
});

export function useConsumption() {
  const store = useConsumptionStore();
  return {
    ...storeToRefs(store),
    addItem: store.addItem,
    updateItem: store.updateItem,
    saveSettings: store.saveSettings,
  };
}
