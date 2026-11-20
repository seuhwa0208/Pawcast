import { jsonBody, springRequest } from "@/services/http/springClient";

export const qnaService = {
  list: () => springRequest("/qna"),
  myList: () => springRequest("/qna/me"),
  create: (payload) => springRequest("/qna", { method: "POST", body: jsonBody(payload) }),
};
