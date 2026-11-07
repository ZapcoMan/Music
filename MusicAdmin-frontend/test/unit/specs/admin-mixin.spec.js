/**
 * 管理端 mixin 的纯逻辑。
 *
 * 断言按"应当成立的行为"写，而不是照抄现状：
 * changeSex 用松散比较，空串会被判成"女"；attachBirth 遇到 null 会渲染出字符串"null"。
 * 这两条与本轮在数据侧清掉的 "null" 字面量是同一类问题的代码侧版本。
 */
import { mixin } from '@/mixins/index';

const changeSex = mixin.methods.changeSex;
const attachBirth = mixin.methods.attachBirth;

describe('mixins changeSex', () => {
  it('四个已知取值映射为中文', () => {
    expect(changeSex(0)).toBe('女');
    expect(changeSex(1)).toBe('男');
    expect(changeSex(2)).toBe('组合');
    expect(changeSex(3)).toBe('不明');
  });

  it('空串与 null 不应被当作"女"', () => {
    // 原写法 value == 0 会把 '' 判成 0，界面显示"女"
    expect(changeSex('')).not.toBe('女');
    expect(changeSex(null)).toBe('');
    expect(changeSex(undefined)).toBe('');
  });

  it('未知编码返回空而不是把原始值直接渲染出去', () => {
    expect(changeSex(99)).toBe('');
  });
});

describe('mixins attachBirth', () => {
  it('截取日期部分', () => {
    expect(attachBirth('2024-05-07 22:25:21')).toBe('2024-05-07');
    expect(attachBirth('1979-01-18')).toBe('1979-01-18');
  });

  it('null 与 undefined 返回空串，不能渲染出字符串 null', () => {
    expect(attachBirth(null)).toBe('');
    expect(attachBirth(undefined)).toBe('');
  });
});

describe('mixins beforeAvatorUpload', () => {
  const call = (file) => {
    const errs = [];
    const ctx = { $message: { error: (m) => errs.push(m) } };
    return { ok: mixin.methods.beforeAvatorUpload.call(ctx, file), errs };
  };

  it('非 jpg/png 拒绝并给出原因', () => {
    const r = call({ type: 'image/gif', size: 1024 });
    expect(r.ok).toBe(false);
    expect(r.errs[0]).toContain('格式');
  });

  it('超过 2MB 拒绝', () => {
    const r = call({ type: 'image/png', size: 3 * 1024 * 1024 });
    expect(r.ok).toBe(false);
    expect(r.errs[0]).toContain('2MB');
  });

  it('合法图片通过且不产生提示', () => {
    const r = call({ type: 'image/jpeg', size: 500 * 1024 });
    expect(r.ok).toBe(true);
    expect(r.errs).toHaveLength(0);
  });
});

describe('mixins handleAvatorSuccess', () => {
  const call = (res) => {
    const ctx = { getData: jest.fn(), $notify: jest.fn() };
    mixin.methods.handleAvatorSuccess.call(ctx, res);
    return ctx;
  };

  it('成功码 20000 会刷新列表', () => {
    expect(call({ code: 20000 }).getData).toHaveBeenCalled();
  });

  it('success 为 true 同样刷新（后端两种写法都要认）', () => {
    expect(call({ success: true }).getData).toHaveBeenCalled();
  });

  it('失败时不刷新', () => {
    expect(call({ code: 20001, success: false }).getData).not.toHaveBeenCalled();
  });
});
