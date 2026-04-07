import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'

export interface CartItem {
  id: number
  productId: number
  productName: string
  productImage: string
  price: number
  unit?: string
  quantity: number
  stock: number
  selected: boolean
  farmerId: number
  farmerName: string
}

const CART_STORAGE_KEY = 'rural_ecommerce_cart'

export const useCartStore = defineStore('cart', () => {
  // 从 localStorage 加载购物车数据
  const loadCartFromStorage = (): CartItem[] => {
    try {
      const stored = localStorage.getItem(CART_STORAGE_KEY)
      if (stored) {
        return JSON.parse(stored)
      }
    } catch (error) {
      console.error('加载购物车数据失败:', error)
    }
    return []
  }

  const items = ref<CartItem[]>(loadCartFromStorage())

  // 监听购物车变化，保存到 localStorage
  watch(items, (newItems) => {
    try {
      localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(newItems))
    } catch (error) {
      console.error('保存购物车数据失败:', error)
    }
  }, { deep: true })

  const totalCount = computed(() => items.value.reduce((sum, item) => sum + item.quantity, 0))
  const selectedCount = computed(() => items.value.filter(item => item.selected).reduce((sum, item) => sum + item.quantity, 0))
  const totalPrice = computed(() => items.value.filter(item => item.selected).reduce((sum, item) => sum + item.price * item.quantity, 0))

  const addItem = (item: Omit<CartItem, 'selected'>) => {
    const existingItem = items.value.find(i => i.productId === item.productId)
    if (existingItem) {
      existingItem.quantity += item.quantity
    } else {
      items.value.push({ ...item, selected: true })
    }
  }

  const removeItem = (productId: number) => {
    const index = items.value.findIndex(item => item.productId === productId)
    if (index > -1) {
      items.value.splice(index, 1)
    }
  }

  const updateQuantity = (productId: number, quantity: number) => {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.quantity = quantity
    }
  }

  const toggleSelect = (productId: number) => {
    const item = items.value.find(i => i.productId === productId)
    if (item) {
      item.selected = !item.selected
    }
  }

  const selectAll = (selected: boolean) => {
    items.value.forEach(item => {
      item.selected = selected
    })
  }

  const clearCart = () => {
    items.value = []
  }

  return {
    items,
    totalCount,
    selectedCount,
    totalPrice,
    addItem,
    removeItem,
    updateQuantity,
    toggleSelect,
    selectAll,
    clearCart
  }
})
