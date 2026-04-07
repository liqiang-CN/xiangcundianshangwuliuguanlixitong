import type { DirectiveBinding } from 'vue'

interface ClickOutsideElement extends HTMLElement {
  _clickOutside?: (event: Event) => void
}

export const vClickOutside = {
  mounted(el: ClickOutsideElement, binding: DirectiveBinding<() => void>) {
    el._clickOutside = (event: Event) => {
      if (!(el === event.target || el.contains(event.target as Node))) {
        binding.value()
      }
    }
    document.addEventListener('click', el._clickOutside, true)
  },
  unmounted(el: ClickOutsideElement) {
    if (el._clickOutside) {
      document.removeEventListener('click', el._clickOutside, true)
    }
  }
}

declare module 'vue' {
  interface ComponentCustomProperties {
    vClickOutside: typeof vClickOutside
  }
}
