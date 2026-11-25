
import { computed, ref, watch } from "vue";
import { defineStore, storeToRefs } from "pinia";
import { cartService } from "@/services/cartService";
import { useAuthSession } from "@/stores/auth";
import { isManagerRole, readUserRole } from "@/utils/roles";

const GUEST_KEY = "powcast-guest-cart";
const MERGE_KEY = "powcast-guest-cart-merge";

function isManagerSession() {
    // 매니저는 쇼핑 기능 대상이 아니므로 cart API를 호출하지 않는다.
    return isManagerRole(readUserRole());
}

function readGuestCart() {
    try {
        const saved = JSON.parse(
            localStorage.getItem(GUEST_KEY) || "[]"
        );
        return Array.isArray(saved) ? saved : [];
    } catch {
        return [];
    }
}

function readPendingMerge() {
    try {
        const saved = JSON.parse(
            localStorage.getItem(MERGE_KEY) || "null"
        );
        return saved?.mergeId && Array.isArray(saved.items)
            ? saved
            : null;
    } catch {
        return null;
    }
}

const useCartStore = defineStore("cart", () => {
    const { accessToken } = useAuthSession();
    const items = ref(
        accessToken.value ? [] : readGuestCart()
    );
    const count = computed(() =>
        items.value.reduce(
            (sum, item) => sum + item.quantity,
            0
        )
    );
    const total = computed(() =>
        items.value.reduce(
            (sum, item) => sum + item.price * item.quantity,
            0
        )
    );

    let sessionVersion = 0;
    let mergePromise = null;

    function saveGuestCart(nextItems) {
        if (nextItems.length) {
            localStorage.setItem(
                GUEST_KEY,
                JSON.stringify(nextItems)
            );
        } else {
            localStorage.removeItem(GUEST_KEY);
        }

        localStorage.removeItem(MERGE_KEY);

        if (!accessToken.value) {
            items.value = nextItems;
        }

        return nextItems;
    }

    function clearLocalCart() {
        localStorage.removeItem(GUEST_KEY);
        localStorage.removeItem(MERGE_KEY);
        items.value = [];
    }

    async function refresh() {
        const token = accessToken.value;
        const version = sessionVersion;

        if (!token) {
            items.value = readGuestCart();
            return items.value;
        }

        if (isManagerSession()) {
            items.value = [];
            return items.value;
        }

        const cart = await cartService.getCart();

        if (
            token !== accessToken.value ||
            version !== sessionVersion
        ) {
            return items.value;
        }

        items.value = Array.isArray(cart?.items)
            ? cart.items.map((item) => ({
                key: `server-${item.itemId}`,
                serverItemId: item.itemId,
                id: item.productId,
                variantId: item.variantId,
                pet: item.pet,
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
        const variantId = Number(product.variantId);

        if (
            !Number.isSafeInteger(variantId) ||
            variantId < 1
        ) {
            throw new Error("상품 옵션 정보가 필요합니다.");
        }

        if (accessToken.value) {
            await cartService.addItem({
                variantId,
                quantity: 1,
            });
            return refresh();
        }

        const guestItems = readGuestCart();
        const existing = guestItems.find(
            (item) => item.variantId === variantId
        );

        if (existing) {
            return saveGuestCart(
                guestItems.map((item) =>
                    item.variantId === variantId
                        ? {
                            ...item,
                            quantity: item.quantity + 1,
                        }
                        : item
                )
            );
        }

        return saveGuestCart([
            ...guestItems,
            {
                key: `guest-${variantId}`,
                id: product.id,
                variantId,
                pet: pet || product.pet,
                name: product.name,
                image: product.image,
                price: Number(product.price || 0),
                option: product.option,
                quantity: 1,
                stock: product.stock,
            },
        ]);
    }

    async function setQuantity(key, quantity) {
        if (!Number.isSafeInteger(quantity)) {
            return;
        }

        const nextQuantity = Math.max(1, quantity);

        if (!accessToken.value) {
            return saveGuestCart(
                readGuestCart().map((item) =>
                    item.key === key
                        ? {
                            ...item,
                            quantity: nextQuantity,
                        }
                        : item
                )
            );
        }

        const item = items.value.find(
            (entry) => entry.key === key
        );

        if (!item?.serverItemId) {
            return;
        }

        await cartService.updateItem(
            item.serverItemId,
            nextQuantity
        );

        return refresh();
    }

    async function remove(key) {
        if (!accessToken.value) {
            return saveGuestCart(
                readGuestCart().filter(
                    (item) => item.key !== key
                )
            );
        }

        const item = items.value.find(
            (entry) => entry.key === key
        );

        if (!item?.serverItemId) {
            return;
        }

        await cartService.removeItem(item.serverItemId);

        return refresh();
    }

    async function clear() {
        if (!accessToken.value) {
            return saveGuestCart([]);
        }

        const token = accessToken.value;
        const memberItems = items.value.filter(
            (item) => item.serverItemId
        );

        await Promise.all(
            memberItems.map((item) =>
                cartService.removeItem(item.serverItemId)
            )
        );

        if (token === accessToken.value) {
            items.value = [];
        }
    }

    async function mergeGuestCart() {
        if (!accessToken.value) {
            return {
                merged: false,
                reason: "NO_SESSION",
            };
        }

        if (isManagerSession()) {
            items.value = [];
            return {
                merged: false,
                reason: "MANAGER_SESSION",
            };
        }

        if (mergePromise) {
            return mergePromise;
        }

        const guestItems = readGuestCart();

        if (!guestItems.length) {
            await refresh();
            return {
                merged: false,
                reason: "EMPTY",
            };
        }

        const token = accessToken.value;
        const payload = guestItems.map((item) => ({
            variantId: item.variantId,
            quantity: item.quantity,
        }));

        let pending = readPendingMerge();

        if (
            !pending ||
            JSON.stringify(pending.items) !==
                JSON.stringify(payload)
        ) {
            pending = {
                mergeId: crypto.randomUUID(),
                items: payload,
            };

            localStorage.setItem(
                MERGE_KEY,
                JSON.stringify(pending)
            );
        }

        mergePromise = (async () => {
            await cartService.mergeGuestCart(
                pending.mergeId,
                pending.items
            );

            if (token !== accessToken.value) {
                return {
                    merged: false,
                    reason: "SESSION_CHANGED",
                };
            }

            localStorage.removeItem(GUEST_KEY);
            localStorage.removeItem(MERGE_KEY);

            await refresh();

            return {
                merged: true,
            };
        })();

        try {
            return await mergePromise;
        } finally {
            mergePromise = null;
        }
    }

    watch(
        accessToken,
        (token, previousToken) => {
            sessionVersion += 1;

            if (!token && previousToken) {
                clearLocalCart();
                return;
            }

            if (token && token !== previousToken) {
                items.value = [];
            }
        },
        {
            flush: "sync",
        }
    );

    if (accessToken.value) {
        refresh().catch((error) => {
            console.warn("장바구니 조회 실패:", error);
        });
    }

    return {
        items,
        count,
        total,
        refresh,
        add,
        setQuantity,
        remove,
        clear,
        clearLocalCart,
        mergeGuestCart,
    };
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
        clearLocalCart: store.clearLocalCart,
        mergeGuestCart: store.mergeGuestCart,
    };
}
