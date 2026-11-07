'use strict'

const path = require('path')
const { load } = require('./load-env')

// NODE_ENV 保持老模板的字面量写法（DefinePlugin 做的是源码替换，要的是代码字面量）。
// VUE_APP_* 从 .env / .env.production 读，并可被容器构建期的同名环境变量覆盖，
// 这样 Dockerfile 里那个 ARG 才第一次真正参与到打包结果中。
module.exports = Object.assign(
  { NODE_ENV: '"production"' },
  load(path.resolve(__dirname, '..'), 'production')
)
