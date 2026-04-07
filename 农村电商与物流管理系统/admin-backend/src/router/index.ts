import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '数据概览', icon: 'Odometer' }
      },
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/product/ProductListView.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'product-review',
        name: 'ProductReview',
        component: () => import('@/views/product/ProductReviewView.vue'),
        meta: { title: '商品审核', icon: 'CircleCheck' }
      },
      {
        path: 'product-reports',
        name: 'ProductReports',
        component: () => import('@/views/product/ProductReportView.vue'),
        meta: { title: '举报审核', icon: 'Warning' }
      },
      {
        path: 'review-manage',
        name: 'ReviewManage',
        component: () => import('@/views/product/ReviewManageView.vue'),
        meta: { title: '评价管理', icon: 'ChatDotRound' }
      },
      {
        path: 'review-reports',
        name: 'ReviewReports',
        component: () => import('@/views/product/ReviewReportView.vue'),
        meta: { title: '评价举报', icon: 'WarningFilled' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/product/CategoryView.vue'),
        meta: { title: '分类管理', icon: 'FolderOpened' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/order/OrderListView.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'logistics',
        name: 'Logistics',
        component: () => import('@/views/logistics/LogisticsView.vue'),
        meta: { title: '物流调度', icon: 'Van' }
      },
      {
        path: 'delivery-staff',
        name: 'DeliveryStaff',
        component: () => import('@/views/logistics/DeliveryStaffView.vue'),
        meta: { title: '配送员管理', icon: 'User' }
      },
      {
        path: 'shops',
        name: 'Shops',
        component: () => import('@/views/shop/ShopManageView.vue'),
        meta: { title: '店铺管理', icon: 'Shop' }
      },
      {
        path: 'farmers',
        name: 'Farmers',
        component: () => import('@/views/user/FarmerView.vue'),
        meta: { title: '农户管理', icon: 'Farm' }
      },
      {
        path: 'consumers',
        name: 'Consumers',
        component: () => import('@/views/user/ConsumerView.vue'),
        meta: { title: '用户管理', icon: 'UserFilled' }
      },
      {
        path: 'admins',
        name: 'Admins',
        component: () => import('@/views/user/AdminView.vue'),
        meta: { title: '管理员管理', icon: 'User' }
      },
      {
        path: 'banners',
        name: 'Banners',
        component: () => import('@/views/config/BannerView.vue'),
        meta: { title: '轮播图管理', icon: 'Picture' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/config/SettingsView.vue'),
        meta: { title: '系统设置', icon: 'Setting' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/config/LogView.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      },
      {
        path: 'chat-records',
        name: 'ChatRecords',
        component: () => import('@/views/chat/ChatRecordView.vue'),
        meta: { title: '聊天记录', icon: 'ChatDotRound' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
