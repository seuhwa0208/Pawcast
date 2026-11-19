// npm run dev에서만 관리자 집계 API를 제공하는 Vite 미들웨어 플러그인입니다.
import { createAdminDashboardResponse } from "./adminDashboardData.js";

function sendJson(response, status, body) {
  response.statusCode = status;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.end(JSON.stringify(body));
}

export function adminDashboardMockPlugin() {
  return {
    name: "pawcast-admin-dashboard-mock-api",
    configureServer(server) {
      server.middlewares.use(
        "/api/admin/dashboard/sales",
        (request, response) => {
          const authorization = request.headers.authorization || "";
          if (authorization !== "Bearer temporary-admin:admin") {
            sendJson(response, 403, {
              success: false,
              status: 403,
              code: "ADMIN_ACCESS_DENIED",
              message: "관리자 권한이 필요합니다.",
            });
            return;
          }

          const url = new URL(request.url || "/", "http://localhost");
          sendJson(
            response,
            200,
            createAdminDashboardResponse(Object.fromEntries(url.searchParams)),
          );
        },
      );
    },
  };
}
