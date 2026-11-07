'use strict'
const merge = require('webpack-merge')
const path = require('path')
const { load } = require('./load-env')

// 此前这里是 merge(prodEnv, {...})，等于把 production 那份 .env 的值带进 dev；
// 改成独立读 development 模式，两种模式各取自己的文件
module.exports = merge(
  { NODE_ENV: '"development"' },
  load(path.resolve(__dirname, '..'), 'development')
)
