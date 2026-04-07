<template>
  <router-view />
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background-color: #fff;
}

/* 苹果风格设计系统 */
:root {
  --apple-blue: #007aff;
  --apple-green: #34c759;
  --apple-red: #ff3b30;
  --apple-orange: #ff9500;
  --apple-yellow: #ffcc00;
  --apple-purple: #af52de;
  --apple-pink: #ff2d55;
  --apple-gray: #8e8e93;
  --apple-light-gray: #f2f2f7;
  --apple-dark-gray: #1c1c1e;
  --apple-bg: #f5f5f7;
  --apple-card-bg: #ffffff;
  --apple-border: rgba(0, 0, 0, 0.1);
  --apple-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  --apple-radius: 12px;
  --apple-radius-lg: 20px;
}

/* 通用按钮样式 */
.btn-primary {
  background: linear-gradient(135deg, var(--apple-blue), #0051d5);
  color: white;
  border: none;
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 122, 255, 0.3);
}

/* 卡片样式 */
.card {
  background: var(--apple-card-bg);
  border-radius: var(--apple-radius-lg);
  box-shadow: var(--apple-shadow);
  padding: 24px;
}

/* 输入框样式 */
.apple-input {
  border: 1px solid var(--apple-border);
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: var(--apple-light-gray);
}

.apple-input:focus {
  outline: none;
  border-color: var(--apple-blue);
  background: white;
  box-shadow: 0 0 0 4px rgba(0, 122, 255, 0.1);
}
</style>
