// 장바구니는 로그인 회원의 서버 데이터만 사용합니다.
import { computed, ref } from "vue";
import { defineStore, storeToRefs } from "pinia";
import { cartService } from "@/services/cartService";

const useCartStore = defineStore("cart", () => {
  const items = ref([]);
  const count = computed(() =>
    items.value.reduce((sum, item) => sum + item.quantity, 0),
  );
  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0),
  );
  const hasSession = () => Boolean(localStorage.getItem("powcast-access-token"));
  async function refresh() {
    if (!hasSession()) {
      items.value = [];
      return items.value;
    }
    const cart = await cartService.getCart();
    items.value = Array.isArray(cart?.items)
      ? cart.items.map((item) => ({
          key: `server-${item.itemId}`,
          serverItemId: item.itemId,
          id: item.productId,
          variantId: item.variantId,
          name: item.productName,
          image: item.imagePath,
          price: Number(item.price || 0),
          option: item.optionLabel,
          quantity: item.quantity,
          stock: item.stock,
        }))
      : [];
    return items.value;
  }

  async function add(product, pet) {
    if (!hasSession()) throw new Error("LOGIN_REQUIRED");
    if (!product.variantId) throw new Error("상품 옵션 정보가 필요합니다.");
    await cartService.addItem({ variantId: product.variantId, quantity: 1 });
    return refresh();
  }

  async function setQuantity(key, quantity) {
    const item = items.value.find((entry) => entry.key === key);
    if (!item) return;
    const nextQuantity = Math.max(1, quantity);
    if (hasSession() && item.serverItemId) {
      await cartService.updateItem(item.serverItemId, nextQuantity);
      return refresh();
    }
    if (!hasSession()) throw new Error("LOGIN_REQUIRED");
  }

  async function remove(key) {
    const index = items.value.findIndex((item) => item.key === key);
    if (index === -1) return;
    if (hasSession() && items.value[index].serverItemId) {
      await cartService.removeItem(items.value[index].serverItemId);
      return refresh();
    }
    if (!hasSession()) throw new Error("LOGIN_REQUIRED");
  }

  async function clear() {
    if (hasSession()) {
      await Promise.all(items.value.filter((item) => item.serverItemId).map((item) => cartService.removeItem(item.serverItemId)));
    }
    items.value = [];
  }

  if (hasSession()) refresh().catch(() => {});
  return { items, count, total, refresh, add, setQuantity, remove, clear };
});

export function useCart() {
  const store = useCartStore();
  return {
    ...storeToRefs(store),
    refresh: store.refresh,
    add: store.add,
    setQuantity: store.setQuantity,
    remove: store.remove,
    clear: store.clear,
  };
}
