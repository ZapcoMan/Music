'use strict'

/**
 * 构建期环境变量装载。
 *
 * 为什么需要这个文件：本工程的 dev 走 vue-cli-service（原生支持 .env），
 * 但 build 走的是 webpack 3 老模板 node build/build.js，它的 DefinePlugin 只认
 * config/prod.env.js 导出的那几个键。后果是 Dockerfile 里的
 * `ARG/ENV VUE_APP_API_BASE_URL` 与 compose 传进来的同名变量在打包时被整体丢弃，
 * 源码里的 process.env.VUE_APP_API_BASE_URL 被编译成 undefined，
 * 生产包实际写死了 http://localhost:9171。
 *
 * 这里不引入 dotenv：一个只读 KEY=VALUE 的解析器十几行就够，
 * 而为一个构建期细节给两个前端各加一个运行时依赖不值得。
 *
 * 优先级：真实环境变量 > .env.<mode> > .env
 * 前者保证容器/CI 能覆盖，后者作为仓库内的默认值。
 */

const fs = require('fs')
const path = require('path')

function parseFile (file) {
  if (!fs.existsSync(file)) return {}
  const out = {}
  fs.readFileSync(file, 'utf8')
    .split(/\r?\n/)
    .forEach(line => {
      const text = line.trim()
      if (!text || text.startsWith('#')) return
      const eq = text.indexOf('=')
      if (eq < 1) return
      const key = text.slice(0, eq).trim()
      let value = text.slice(eq + 1).trim()
      // 去掉成对引号，让 .env 里写 "x" 与写 x 等价
      if (value.length >= 2 && ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'")))) {
        value = value.slice(1, -1)
      }
      out[key] = value
    })
  return out
}

/**
 * 读取 .env 与 .env.<mode>，返回可直接交给 DefinePlugin 的对象。
 *
 * DefinePlugin 做的是源码文本替换，值必须是"代码字面量"，
 * 所以字符串要再包一层引号——这也是老模板里 NODE_ENV 写成 '"production"' 的原因。
 */
function load (projectRoot, mode) {
  const merged = Object.assign(
    {},
    parseFile(path.join(projectRoot, '.env')),
    parseFile(path.join(projectRoot, `.env.${mode}`))
  )

  Object.keys(merged).forEach(key => {
    if (process.env[key] !== undefined) {
      merged[key] = process.env[key]
    }
  })

  const forDefinePlugin = {}
  Object.keys(merged).forEach(key => {
    forDefinePlugin[key] = JSON.stringify(merged[key])
  })
  return forDefinePlugin
}

module.exports = { load }
