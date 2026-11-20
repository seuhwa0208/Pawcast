// 찜 상품은 Pinia가 관리하며, 현재 데모에서는 localStorage에 영속화합니다.
import { ref } from "vue";
import { defineStore } from "pinia";
import { wishlistService } from "@/services/wishlistService";

const STORAGE_KEY = "powcast-wishlist";
const useWishlistStore = defineStore("wishlist", () => {
  const itemIds = ref(JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"));
  const isFavorite = (id) => itemIds.value.includes(id);
  const hasSession = () => Boolean(localStorage.getItem("powcast-access-token"));
  async function refresh() {
    if (!hasSession()) {
      itemIds.value = [];
      return itemIds.value;
    }
    const response = await wishlistService.list();
    itemIds.value = Array.isArray(response)
      ? response.map((item) => item.product?.id ?? item.productId).filter(Boolean)
      : [];
    return itemIds.value;
  }
  async function toggle(id) {
    if (hasSession()) {
      if (isFavorite(id)) await wishlistService.remove(id);
      else await wishlistService.add(id);
      return refresh();
    }
    const index = itemIds.value.indexOf(id);
    if (index === -1) itemIds.value.push(id);
    else itemIds.value.splice(index, 1);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(itemIds.value));
  }
  if (hasSession()) refresh().catch(() => {});
  return { itemIds, isFavorite, refresh, toggle };
});

export const useWishlist = () => useWishlistStore();
