import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/ConsumerLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/consumer/HomeView.vue')
      },
      {
        path: 'products',
        name: 'ProductList',
        component: () => import('@/views/consumer/ProductListView.vue')
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/consumer/ProductDetailView.vue')
      },
      {
        path: 'product/:id/report',
        name: 'ProductReport',
        component: () => import('@/views/consumer/ReportView.vue')
      },
      {
        path: 'cart',
        name: 'ShoppingCart',
        component: () => import('@/views/consumer/CartView.vue')
      },
      {
        path: 'orders',
        name: 'OrderList',
        component: () => import('@/views/consumer/OrderListView.vue')
      },
      {
        path: 'order/:id',
        name: 'OrderDetail',
        component: () => import('@/views/consumer/OrderDetailView.vue')
      },
      {
        path: 'profile',
        name: 'ConsumerProfile',
        component: () => import('@/views/consumer/ProfileView.vue')
      },
      {
        path: 'address',
        name: 'AddressManage',
        component: () => import('@/views/consumer/AddressView.vue')
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/consumer/FavoritesView.vue')
      },
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('@/views/consumer/MessagesView.vue')
      },
      {
        path: 'followed-shops',
        name: 'FollowedShops',
        component: () => import('@/views/consumer/FollowedShopsView.vue')
      },
      {
        path: 'orders/create',
        name: 'OrderCreate',
        component: () => import('@/views/consumer/OrderCreateView.vue')
      },
      {
        path: 'shop/:id',
        name: 'ShopDetail',
        component: () => import('@/views/consumer/ShopDetailView.vue')
      },
      {
        path: 'shops',
        name: 'ShopList',
        component: () => import('@/views/consumer/ShopListView.vue')
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/LoginView.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/common/RegisterView.vue')
  },
  // 消费者路由（保留兼容，重定向到新路径）
  {
    path: '/consumer/:pathMatch(.*)*',
    redirect: (to) => {
      const path = to.path.replace('/consumer', '')
      return path || '/'
    }
  },
  // 农户路由
  {
    path: '/farmer',
    component: () => import('@/layouts/FarmerLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'FarmerDashboard',
        component: () => import('@/views/farmer/DashboardView.vue')
      },
      {
        path: 'products',
        name: 'FarmerProducts',
        component: () => import('@/views/farmer/ProductManageView.vue')
      },
      {
        path: 'product/add',
        name: 'AddProduct',
        component: () => import('@/views/farmer/AddProductView.vue')
      },
      {
        path: 'product/edit/:id',
        name: 'EditProduct',
        component: () => import('@/views/farmer/AddProductView.vue')
      },
      {
        path: 'orders',
        name: 'FarmerOrders',
        component: () => import('@/views/farmer/OrderManageView.vue')
      },
      {
        path: 'shop',
        name: 'ShopManage',
        component: () => import('@/views/farmer/ShopManageView.vue')
      },
      {
        path: 'income',
        name: 'IncomeStats',
        component: () => import('@/views/farmer/IncomeView.vue')
      },
      {
        path: 'messages',
        name: 'FarmerMessages',
        component: () => import('@/views/farmer/MessagesView.vue')
      },
      {
        path: 'address',
        name: 'FarmerAddress',
        component: () => import('@/views/farmer/AddressView.vue')
      },
      {
        path: 'password',
        name: 'FarmerPassword',
        component: () => import('@/views/farmer/PasswordView.vue')
      }
    ]
  },
  // 物流人员路由
  {
    path: '/logistics',
    component: () => import('@/layouts/LogisticsLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'LogisticsDashboard',
        component: () => import('@/views/logistics/DashboardView.vue')
      },
      {
        path: 'waybills',
        name: 'WaybillManage',
        component: () => import('@/views/logistics/WaybillView.vue')
      },
      {
        path: 'print',
        name: 'PrintWaybill',
        component: () => import('@/views/logistics/PrintView.vue')
      },
      {
        path: 'route',
        name: 'DeliveryRoute',
        component: () => import('@/views/logistics/RouteView.vue')
      },
      {
        path: 'records',
        name: 'DeliveryRecords',
        component: () => import('@/views/logistics/RecordsView.vue')
      },
      {
        path: 'profile',
        name: 'LogisticsProfile',
        component: () => import('@/views/logistics/ProfileView.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory((import.meta as any).env?.BASE_URL || '/'),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
