import { jsonBody, springRequest } from "@/services/http/springClient";

export const qnaService = {
  list: () => springRequest("/qna"),
  myList: () => springRequest("/qna/me"),
  detail: (qnaId) => springRequest(`/qna/${qnaId}`),
  myDetail: (qnaId) => springRequest(`/qna/me/${qnaId}`),
  create: ({ type, ...payload }) => springRequest("/qna", {
    method: "POST",
    // # 화면의 한글 유형을 Spring QnaType enum 값으로 변환한다.
    body: jsonBody({ ...payload, type: type === "상품" ? "PRODUCT" : "GENERAL" }),
  }),
  update: (qnaId, { type, ...payload }) => springRequest(`/qna/${qnaId}`, {
    method: "PUT",
    body: jsonBody({ ...payload, type: type === "상품" ? "PRODUCT" : "GENERAL" }),
  }),
  remove: (qnaId) => springRequest(`/qna/${qnaId}`, { method: "DELETE" }),
};
