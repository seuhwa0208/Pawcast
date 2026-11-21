import { springRequest } from "@/services/http/springClient";

function toNotice(item, index) {
  return {
    // 현 백엔드 응답에 공지 ID가 없으므로 목록용 안정 키만 부여합니다.
    // # Spring NoticeResponse의 식별자는 id가 아니라 noticeId다.
    id: item.noticeId ?? item.id ?? `notice-${index}-${item.createdAt || ""}`,
    pinned: Boolean(item.pinned ?? item.isPinned),
    type: item.noticeType === "EVENT" ? "이벤트" : "공지",
    title: item.title,
    content: item.content,
    date: item.createdAt?.slice(0, 10).replaceAll("-", ".") || "",
  };
}

export const noticeService = {
  list: async (filters = {}) => {
    const query = new URLSearchParams(filters);
    const response = await springRequest(
      `/notices${query.size ? `?${query}` : ""}`,
    );
    return Array.isArray(response) ? response.map(toNotice) : [];
  },
};
