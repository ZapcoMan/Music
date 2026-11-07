import Vue from 'vue'

Vue.config.productionTip = false

// jest 22 捆绑的 jsdom 在本机 Node 24 下不提供 window.sessionStorage，
// 而播放器 store 的回读逻辑正是围绕它工作的。
// 这里补一个内存实现作为测试替身 —— 被测代码保持不动，
// 测的是它的真实分支（null 才回读、0/false/[] 原样保留、脏 JSON 不抛错）。
if (typeof window !== 'undefined' && !window.sessionStorage) {
  const memory = {};
  Object.defineProperty(window, 'sessionStorage', {
    configurable: true,
    value: {
      getItem(key) {
        return Object.prototype.hasOwnProperty.call(memory, key) ? memory[key] : null;
      },
      setItem(key, value) {
        memory[key] = String(value);
      },
      removeItem(key) {
        delete memory[key];
      },
      clear() {
        Object.keys(memory).forEach((k) => delete memory[k]);
      },
      key(index) {
        return Object.keys(memory)[index] || null;
      },
      get length() {
        return Object.keys(memory).length;
      }
    }
  });
}
