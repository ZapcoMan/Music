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
    // 原为 plugin:vue/essential；此处的 consider switching 注释留了很久但一直没落地，现升档
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

    // ===== 分级说明 =====
    // .eslintignore 此前把全部源码排除在外，lint 从未真正跑过，所以一放开就暴露 4768 项。
    // 全设成 error 会让 lint 直接失败、结果又没人跑，等于回到原状；
    // 因此排版类先降为 warn 建立基线，真缺陷类单独判断。
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
    'vue/html-indent': 'warn',
    'vue/max-attributes-per-line': 'warn',
    'vue/html-self-closing': 'warn',
    'vue/mustache-interpolation-spacing': 'warn',
    // 以下四条 Admin 端存量为 0，为与 User 端保持同一份规则集（避免两份配置漂移），
    // 这里同步设成 warn；User 端存量非零的原因已在它自己的注释里记明
    'vue/attribute-hyphenation': 'warn',
    'vue/v-bind-style': 'warn',
    'padded-blocks': 'warn',
    'handle-callback-err': 'warn',
    'standard/object-curly-even-spacing': 'warn',
    'vue/name-property-casing': 'warn',
    'vue/no-multi-spaces': 'warn',

    // 这几条是真缺陷，但存量非零且不能盲改，暂列 warn，数量记在这里作为还债凭据：
    // eqeqeq 24 处——其中不少是 item.songId == this.id 这类字符串与数字的宽松比较，
    //   直接改成 === 会让本来成立的比较失效，必须逐个确认数据来源；
    // no-sequences 4 处、no-unused-expressions 6 处、no-use-before-define 2 处、
    // vue/require-prop-types 2 处——props 用数组式声明，拿不到类型校验
    'eqeqeq': 'warn',
    'no-sequences': 'warn',
    'no-unused-expressions': 'warn',
    'no-use-before-define': 'warn',
    'vue/require-prop-types': 'warn',

    // 存量为 0 的真缺陷保持 error：放开检查后这些一次都没犯，说明可以当硬门禁。
    // 注意不要写 vue/no-unused-components——它是 eslint-plugin-vue 5.x 才引入的规则，
    // 本工程锁的是 4.7.1，写上会让每条文件都报 "Definition for rule ... not found"
    'no-undef': 'error',
    'vue/no-dupe-keys': 'error',
    'vue/require-v-for-key': 'error'
  }
}
