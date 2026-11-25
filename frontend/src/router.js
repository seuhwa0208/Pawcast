// URL과 화면 컴포넌트를 연결하고 회원/본인확인 접근 규칙을 적용하는 라우터입니다.
import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import CategoryView from "@/views/CategoryView.vue";
import ProductDetailView from "@/views/ProductDetailView.vue";
import CartView from "@/views/CartView.vue";
import LoginView from "@/views/LoginView.vue";
import SignupView from "@/views/SignupView.vue";
import PasswordResetView from "@/views/PasswordResetView.vue";
import QnaView from "@/views/QnaView.vue";
import NoticeView from "@/views/NoticeView.vue";
import MyPageView from "@/views/MyPageView.vue";
import MemberDashboardView from "@/views/MemberDashboardView.vue";
import ClaimManagementView from "@/views/ClaimManagementView.vue";
import PetManagementView from "@/views/PetManagementView.vue";
import CheckoutView from "@/views/CheckoutView.vue";
import PaymentResultView from "@/views/PaymentResultView.vue";
import GuestOrderLookupView from "@/views/GuestOrderLookupView.vue";
import ErrorView from "@/views/ErrorView.vue";
import { useAuthSession } from "@/stores/auth";
import { canManageAdminArea, isAdminRole, isManagerRole, readUserRole } from "@/utils/roles";
import AdminLayout from "@/layouts/AdminLayout.vue";

// 용량이 큰 ECharts는 관리자 경로에 들어왔을 때만 내려받도록 화면을 지연 로딩합니다.
const AdminSalesAnalyticsView = () =>
  import("@/views/admin/AdminSalesAnalyticsView.vue");
const AdminOrdersView = () => import("@/views/admin/AdminOrdersView.vue");
const AdminProductsView = () => import("@/views/admin/AdminProductsView.vue");
const AdminProductCreateView = () =>
  import("@/views/admin/AdminProductCreateView.vue");
const AdminMembersView = () => import("@/views/admin/AdminMembersView.vue");
const AdminNoticesView = () => import("@/views/admin/AdminNoticesView.vue");
const AdminQnaView = () => import("@/views/admin/AdminQnaView.vue");
const AdminSupportView = () => import("@/views/admin/AdminSupportView.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", component: HomeView, props: { memberMode: false } },
    {
      path: "/member",
      name: "member-home",
      component: HomeView,
      props: { memberMode: true },
      meta: { requiresAuth: true },
    },
    {
      path: "/mypage",
      name: "mypage",
      component: MemberDashboardView,
      meta: { requiresAuth: true },
    },
    {
      path: "/mypage/settings",
      name: "member-settings",
      component: MyPageView,
      meta: { requiresAuth: true, requiresPasswordVerification: true },
    },
    {
      path: "/mypage/claims",
      name: "claim-management",
      component: ClaimManagementView,
      meta: { requiresAuth: true },
    },
    {
      path: "/mypage/pets",
      name: "pet-management",
      component: PetManagementView,
      meta: { requiresAuth: true },
    },
    { path: "/dog", component: CategoryView, props: { pet: "DOG" } },
    { path: "/cat", component: CategoryView, props: { pet: "CAT" } },
    { path: "/tableware", component: CategoryView, props: { device: true } },
    { path: "/products/:pet/:id", component: ProductDetailView, props: true },
    { path: "/cart", component: CartView },
    {
      path: "/checkout",
      name: "checkout",
      component: CheckoutView,
    },
    {
      path: "/payment/result",
      name: "payment-result",
      component: PaymentResultView,
    },
    {
      path: "/guest-orders",
      name: "guest-order-lookup",
      component: GuestOrderLookupView,
    },
    { path: "/qna", name: "qna-list", component: QnaView },
    { path: "/qna/:id(\\d+)", name: "qna-detail", component: QnaView },
    { path: "/notices", name: "notice-list", component: NoticeView },
    { path: "/notices/:id(\\d+)", name: "notice-detail", component: NoticeView },
    { path: "/login", component: LoginView },
    { path: "/signup", component: SignupView },
    { path: "/password-reset", component: PasswordResetView },

    // 관리자 페이지 공통 레이아웃
    {
      path: "/admin",
      component: AdminLayout,
      meta: {
        requiresAuth: true,
        requiresManager: true,
        adminLayout: true,
      },
      children: [
        // 관리자 기본 진입 경로
        {
          path: "",
          redirect: "/admin/sales",
        },

        // 매출 분석
        {
          path: "sales",
          name: "admin-sales",
          component: AdminSalesAnalyticsView,
        },

        // 주문·배송 관리: 주문 조회, 배송 상태 변경, 환불 승인
        {
          path: "orders",
          name: "admin-orders",
          component: AdminOrdersView,
        },

        // 상품 관리: 실제 상품 목록과 베스트·워스트 판매 성과
        {
          path: "products",
          name: "admin-products",
          component: AdminProductsView,
        },

        // 회원 관리 (ADMIN 전용)
        {
          path: "members",
          name: "admin-members",
          component: AdminMembersView,
          meta: {
            requiresAdmin: true,
          },
        },

        // 상품 등록
        {
          path: "products/new",
          name: "admin-product-create",
          component: AdminProductCreateView,
        },

        // 공지사항 관리: 등록·수정·삭제·고정 처리
        {
          path: "notices",
          name: "admin-notices",
          component: AdminNoticesView,
        },

        // 공지사항 상세 보기
        {
          path: "notices/:id(\\d+)",
          name: "admin-notice-detail",
          component: NoticeView,
        },

        // QnA 관리: 답변 대기 우선 정렬과 답변 등록·수정
        {
          path: "qna",
          name: "admin-qna",
          component: AdminQnaView,
        },
      ],
    },
    {
      path: "/error/:status(403|404|500)",
      name: "error",
      component: ErrorView,
      props: true,
    },
    { path: "/:pathMatch(.*)*", redirect: "/error/404" },
  ],
  scrollBehavior: (to) =>
    to.hash ? { el: to.hash, top: 120, behavior: "smooth" } : { top: 0 },
});

// 모든 페이지 이동에서 인증과 역할을 확인합니다. 매니저는 허용된 관리자 하위 경로만 열 수 있습니다.
router.beforeEach((to) => {
  const { accessToken } = useAuthSession();
  const role = readUserRole();
  const isAuthenticated = Boolean(accessToken.value);

  if (to.meta.requiresAuth && !isAuthenticated) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  if (
    isAuthenticated &&
    isManagerRole(role) &&
    !to.meta.requiresManager &&
    to.path !== "/error/403"
  ) {
    return "/admin/sales";
  }

  if (to.meta.requiresAdmin && !isAdminRole(role)) {
    return "/error/403";
  }

  if (to.meta.requiresManager && !canManageAdminArea(role)) {
    return "/error/403";
  }

  // 민감한 회원정보 화면은 현재 토큰으로 비밀번호 확인을 마친 세션만 허용합니다.
  if (
    to.meta.requiresPasswordVerification &&
    sessionStorage.getItem("powcast-profile-verified") !== accessToken.value
  ) {
    return { path: "/mypage", query: { verify: "1" } };
  }

  if ((to.path === "/login" || to.path === "/signup") && isAuthenticated) {
    return canManageAdminArea(role) ? "/admin/sales" : "/member";
  }
});
export default router;