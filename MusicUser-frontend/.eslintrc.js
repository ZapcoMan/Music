// https://eslint.org/docs/user-guide/configuring

module.exports = {
  root: true,
  parserOptions: {
    parser: 'babel-eslint'
  },
  env: {
    browser: true,
  },
  extends: [
    // https://github.com/vuejs/eslint-plugin-vue#priority-a-essential-error-prevention
    // 原为 plugin:vue/essential；此处遗留的 consider switching 注释一直没落地，现升档
    'plugin:vue/strongly-recommended', 
    // https://github.com/standard/standard/blob/master/docs/RULES-en.md
    'standard'
  ],
  // required to lint *.vue files
  plugins: [
    'vue'
  ],
  // add your custom rules here
  rules: {
    // allow async-await
    'generator-star-spacing': 'off',
    // allow debugger during development
    'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',

    // ===== 分级说明（与 Admin 端同一口径）=====
    // .eslintignore 原本以 /* 结尾，把源码全部排除，lint 从未真正检查过任何文件。
    // 放开后存量问题一次性出现，其中绝大多数是排版；全设 error 会让 lint 直接失败、
    // 反而没人再跑，因此排版类降 warn，真缺陷类单独判断。
    'indent': 'warn',
    'semi': 'warn',
    'quotes': 'warn',
    'comma-spacing': 'warn',
    'comma-dangle': 'warn',
    'spaced-comment': 'warn',
    'space-before-function-paren': 'warn',
    'space-before-blocks': 'warn',
    'keyword-spacing': 'warn',
    'space-infix-ops': 'warn',
    'no-multi-spaces': 'warn',
    'block-spacing': 'warn',
    'key-spacing': 'warn',
    'arrow-spacing': 'warn',
    'no-multiple-empty-lines': 'warn',
    'eol-last': 'warn',
    'one-var': 'warn',
    'no-trailing-spaces': 'warn',
    'vue/html-indent': 'warn',
    'vue/max-attributes-per-line': 'warn',
    'vue/html-self-closing': 'warn',
    'vue/mustache-interpolation-spacing': 'warn',
    'vue/name-property-casing': 'warn',
    'vue/no-multi-spaces': 'warn',
    // 这几条在 User 端存量非零，Admin 端为 0，两端口径保持一致以免配置漂移
    'vue/attribute-hyphenation': 'warn',
    'vue/v-bind-style': 'warn',
    'padded-blocks': 'warn',
    'standard/object-curly-even-spacing': 'warn',
    // handle-callback-err 是真问题（catch 里把异常丢掉不看），
    // 但补日志分支会改变现有错误路径的行为，需逐个确认后再接入
    'handle-callback-err': 'warn',

    // 真缺陷但存量非零、且不能盲改的，列 warn 并把数量记在这里作为还债凭据：
    // eqeqeq —— 多为 item.songId == this.id 这类字符串/数字宽松比较，
    //   改成 === 会让本来成立的比较失效，需逐个确认数据来源
    'eqeqeq': 'warn',
    'no-sequences': 'warn',
    'no-unused-expressions': 'warn',
    'no-use-before-define': 'warn',
    'vue/require-prop-types': 'warn',

    // 存量为 0 的真缺陷保持 error。
    // 不写 vue/no-unused-components：本工程锁 eslint-plugin-vue 4.7.1，该规则 5.x 才有，
    // 写上会让每个文件都报 "Definition for rule ... not found"
    'no-undef': 'error',
    'vue/no-dupe-keys': 'error',
    'vue/require-v-for-key': 'error'
  }
}
