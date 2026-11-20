// 공지는 실제 API 결과만 보관합니다. 초기 더미 목록은 두지 않습니다.
import { reactive } from "vue";
import { noticeService } from "@/services/noticeService";

export const notices = reactive([]);

export async function loadNotices() {
  const result = await noticeService.list();
  notices.splice(0, notices.length, ...result);
  return notices;
}
