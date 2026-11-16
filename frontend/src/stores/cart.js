// 비회원 장바구니를 Pinia와 localStorage에 유지합니다.
import { computed, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";

const STORAGE_KEY = "powcast-cart-vue";

const useCartStore = defineStore("cart", () => {
  const items = ref(JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"));
  const count = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0),
  );
  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
  );
  const persist = () =>
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items.value));

  function add(product, pet) {
    // 같은 상품도 선택 옵션이 다르면 각각 수량과 가격을 관리해야 하므로 옵션을 장바구니 키에 포함합니다.
    const key = `${pet}-${product.id}-${product.option || "default"}`;
    const found = items.value.find((item) => item.key === key);
    if (found) found.quantity += 1;
    else
      items.value.push({
        key,
        id: product.id,
        pet,
        name: product.name,
        image: product.image,
        price: product.price,
        option: product.option,
        quantity: 1,
      });
    persist();
  }

  function setQuantity(key, quantity) {
    const item = items.value.find((entry) => entry.key === key);
    if (!item) return;
    item.quantity = Math.max(1, quantity);
    persist();
  }

  function remove(key) {
    const index = items.value.findIndex((item) => item.key === key);
    if (index === -1) return;
    items.value.splice(index, 1);
    persist();
  }

  function clear() {
    items.value = [];
    persist();
  }

  return { items, count, total, add, setQuantity, remove, clear };
});

export function useCart() {
  const store = useCartStore();
  return {
    ...storeToRefs(store),
    add: store.add,
    setQuantity: store.setQuantity,
    remove: store.remove,
    clear: store.clear,
  };
}
