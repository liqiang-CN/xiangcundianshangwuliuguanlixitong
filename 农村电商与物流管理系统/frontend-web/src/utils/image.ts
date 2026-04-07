/**
 * 处理图片URL - 统一处理图片路径
 * @param url 图片URL或路径
 * @param defaultImage 默认图片URL
 * @returns 处理后的图片URL
 */
export const getImageUrl = (url?: string, defaultImage: string = '/images/default-product.jpg'): string => {
  if (!url) return defaultImage
  // 如果已经是完整URL，直接返回
  if (url.startsWith('http')) return url
  // 如果是 /images 开头的路径，直接返回
  if (url.startsWith('/images/')) return url
  // 如果是 /uploads/ 开头的路径，使用相对路径让代理处理
  if (url.startsWith('/uploads/')) {
    return url
  }
  return url
}
