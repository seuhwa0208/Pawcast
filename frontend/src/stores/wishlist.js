// 찜 상품은 Pinia가 관리하며, 현재 데모에서는 localStorage에 영속화합니다.
import { ref } from "vue";
import { defineStore } from "pinia";

const STORAGE_KEY = "pawcast-wishlist";
const useWishlistStore = defineStore("wishlist", () => {
  const itemIds = ref(JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]"));
  const isFavorite = (id) => itemIds.value.includes(id);
  function toggle(id) {
    const index = itemIds.value.indexOf(id);
    if (index === -1) itemIds.value.push(id);
    else itemIds.value.splice(index, 1);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(itemIds.value));
  }
  return { itemIds, isFavorite, toggle };
});

export const useWishlist = () => useWishlistStore();
