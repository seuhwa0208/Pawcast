// URL과 화면 컴포넌트를 연결하고 회원/본인확인 접근 규칙을 적용하는 라우터입니다.
import { createRouter, createWebHistory } from "vue-router";
import HomeView from "@/views/HomeView.vue";
import CategoryView from "@/views/CategoryView.vue";
import ProductDetailView from "@/views/ProductDetailView.vue";
import CartView from "@/views/CartView.vue";
import LoginView from "@/views/LoginView.vue";
import SignupView from "@/views/SignupView.vue";
import PasswordResetView from "@/views/PasswordResetView.vue";
import OAuthCallbackView from "@/views/OAuthCallbackView.vue";
import QnaView from "@/views/QnaView.vue";
import NoticeView from "@/views/NoticeView.vue";
import MyPageView from "@/views/MyPageView.vue";
import MemberDashboardView from "@/views/MemberDashboardView.vue";
import ClaimManagementView from "@/views/ClaimManagementView.vue";
import PetManagementView from "@/views/PetManagementView.vue";
import CheckoutView from "@/views/CheckoutView.vue";
import PaymentResultView from "@/views/PaymentResultView.vue";
import ErrorView from "@/views/ErrorView.vue";
import { useAuthSession } from "@/stores/auth";

// 용량이 큰 ECharts는 관리자 경로에 들어왔을 때만 내려받도록 화면을 지연 로딩합니다.
const AdminDashboardView = () => import("@/views/AdminDashboardView.vue");
const AdminOverviewView = () => import("@/views/AdminOverviewView.vue");
const AdminOperationsView = () => import("@/views/AdminOperationsView.vue");
const AdminProductCreateView = () => import("@/views/AdminProductCreateView.vue");
const AdminDemoView = () => import("@/views/AdminDemoView.vue");

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
    { path: "/qna", name: "qna-list", component: QnaView },
    { path: "/qna/:id(\\d+)", name: "qna-detail", component: QnaView },
    { path: "/notices", name: "notice-list", component: NoticeView },
    { path: "/notices/:id(\\d+)", name: "notice-detail", component: NoticeView },
    { path: "/login", component: LoginView },
    { path: "/signup", component: SignupView },
    { path: "/password-reset", component: PasswordResetView },
    { path: "/oauth/callback", component: OAuthCallbackView },
    {
      path: "/admin/dashboard",
      name: "admin-dashboard",
      component: AdminOverviewView,
      meta: { requiresAuth: true, requiresManager: true, adminLayout: true },
    },
    {
      path: "/admin/sales",
      name: "admin-sales",
      component: AdminDashboardView,
      meta: { requiresAuth: true, requiresManager: true, adminLayout: true },
    },
    {
      path: "/admin/members",
      name: "admin-members",
      component: AdminOperationsView,
      props: { section: "members" },
      meta: { requiresAuth: true, requiresAdmin: true, adminLayout: true },
    },
    {
      path: "/admin/products/new",
      name: "admin-product-create",
      component: AdminProductCreateView,
      meta: { requiresAuth: true, requiresManager: true, adminLayout: true },
    },
    {
      path: "/admin/:section(orders|products|qna|notices)",
      name: "admin-operations",
      component: AdminOperationsView,
      meta: { requiresAuth: true, requiresManager: true, adminLayout: true },
    },
    {
      path: "/admin/:tool(category|settings)",
      name: "admin-demo",
      component: AdminDemoView,
      meta: { requiresAuth: true, requiresManager: true, adminLayout: true },
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

// 모든 이동 직전에 인증 메타 정보를 검사해 원래 목적지를 보존한 채 로그인으로 보냅니다.
router.beforeEach((to) => {
  const { accessToken } = useAuthSession();
  if (to.meta.requiresAuth && !accessToken.value)
    return { path: "/login", query: { redirect: to.fullPath } };
  if (
    to.meta.requiresAdmin &&
    sessionStorage.getItem("powcast-user-role") !== "ADMIN"
  )
    return "/error/403";
  if (
    to.meta.requiresManager &&
    !["ADMIN", "MANAGER"].includes(sessionStorage.getItem("powcast-user-role"))
  )
    return "/error/403";
  // 민감한 회원정보 화면은 현재 토큰으로 비밀번호 확인을 마친 세션만 허용합니다.
  if (
    to.meta.requiresPasswordVerification &&
    sessionStorage.getItem("powcast-profile-verified") !== accessToken.value
  )
    return { path: "/mypage", query: { verify: "1" } };
  if ((to.path === "/login" || to.path === "/signup") && accessToken.value)
    return ["ADMIN", "MANAGER"].includes(sessionStorage.getItem("powcast-user-role"))
      ? "/admin/dashboard"
      : "/member";
});

export default router;
