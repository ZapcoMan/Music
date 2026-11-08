/**
 * 歌词解析。
 *
 * 锁的是本轮修掉的一个会卡死浏览器主线程的缺陷：
 * 原写法 `while(!pattern.test(lines[0]))` 在 lines 被 slice 空之后 lines[0] 变成
 * undefined，test 恒为 false，循环永不退出 —— 只要歌词全是
 * [by:xxx] / [offset:0.5] 这类无时间戳行就会命中。
 */
import { mixin } from '@/mixins/index';

jest.mock('@/api/index', () => ({
  likeSongOfName: jest.fn(),
  getCollectOfUserId: jest.fn(),
  recordSearchHistory: jest.fn()
}));

const parse = mixin.methods.parseLyric;

describe('mixins parseLyric', () => {
  it('标准 LRC 解析出时间与文本，并按时间排序', () => {
    const out = parse('[00:05.50]第二句\n[00:01.00]第一句');
    expect(out).toEqual([[1, '第一句'], [5.5, '第二句']]);
  });

  it('跳过开头的非时间戳行', () => {
    const out = parse('[by:某制作组]\n[00:00.30]起步');
    expect(out).toEqual([[0.3, '起步']]);
  });

  it('完全没有时间戳的文本原样返回，不丢内容', () => {
    const out = parse('就是一段没有歌词的文本');
    expect(out).toEqual([[0, '就是一段没有歌词的文本']]);
  });

  it('只有元信息行时不能死循环（本轮修复点）', () => {
    // 旧写法在这里 lines 会被 slice 到空，lines[0] 为 undefined，循环不退出
    const out = parse('[by:制作组]\n[offset:0.5]');
    expect(Array.isArray(out)).toBe(true);
    expect(out.length).toBeGreaterThan(0);
  }, 2000);

  it('空字符串不应抛错也不应死循环', () => {
    const out = parse('');
    expect(out).toEqual([[0, '']]);
  }, 2000);

  it('value 为空的行被丢弃，不产生空歌词条目', () => {
    const out = parse('[00:01.00]\n[00:02.00]有字');
    expect(out).toEqual([[2, '有字']]);
  });
});
