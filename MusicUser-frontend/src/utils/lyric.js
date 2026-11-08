/**
 * LRC 歌词解析，全站唯一实现。
 *
 * 此前同一个函数在 mixins/index.js、components/PlayBar.vue、
 * components/TheAside.vue、pages/Lyric.vue 各抄了一份
 * （Lyric.vue 里还留着第五份注释掉的旧体）。
 * 后来修"歌词只含 [by:xxx] 这类无时间戳行时循环不退出、页面卡死"这个问题，
 * 只改了 mixins 与 PlayBar 两处，TheAside 和 Lyric 里那两份仍然是会卡死页面的版本。
 * 一份逻辑五处拷贝，修一处漏两处，这就是拷贝要付的代价。
 */

/**
 * 时间戳行格式，如 [00:12.34] 或 [00:12.345]
 * 注意这个正则带 g 标志：lastIndex 是有状态的，
 * 所以只能对每个字符串新建使用，不能在模块级复用同一个对象实例做 .test()。
 */
const TIME_PATTERN = /\[\d{2}:\d{2}.(\d{3}|\d{2})\]/g;

/**
 * 把 LRC 文本解析成 [[秒, 歌词], ...]，按时间升序。
 *
 * @param {string} text 原始 LRC 文本
 * @returns {Array<Array>} 每轮 [时间秒数, 文本]
 */
export function parseLyric(text) {
    // 空值守卫：库里大量行的 lyric 是 null 或空串，
    // 原先第一句 text.split() 直接抛 TypeError，表现是切到无歌词的歌时整块区域空白
    if (!text) {
        return [[0, '']];
    }

    let lines = text.split('\n');
    let pattern = new RegExp(TIME_PATTERN.source, 'g');
    let result = [];

    // 完全不形如 LRC 的文本（比如只有一句说明），原样作为第 0 秒的文本返回
    if (!(/\[.+\]/.test(text))) {
        return [[0, text]];
    }

    // 去掉前面格式不正确的行。
    // lines.length 这个出口不能省：只有 [by:xxx] / [offset:0] 这类无时间戳行时，
    // lines 会被 slice 到空，lines[0] 变成 undefined，test 恒为 false，循环不退出。
    while (lines.length && !pattern.test(lines[0])) {
        lines = lines.slice(1);
    }
    if (!lines.length) {
        return [[0, text]];
    }

    for (let item of lines) {
        let time = item.match(pattern); // 前面的时间标签，可能多个
        let value = item.replace(pattern, ''); // 后面的歌词
        // match 不命中时返回 null，不是 undefined；
        // 原写法判的是 == undefined，null 会被漏过去，随后 for...of null 抛错
        if (time == null || value == null) {
            continue;
        }
        for (let item1 of time) {
            let t = item1.slice(1, -1).split(':'); // 取出时间，换算成 [分, 秒]
            if (value != '') {
                result.push([parseInt(t[0], 10) * 60 + parseFloat(t[1]), value]);
            }
        }
    }

    // 按第一个元素——时间——排序
    result.sort(function (a, b) {
        return a[0] - b[0];
    });
    return result;
}

export default parseLyric;
