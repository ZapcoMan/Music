/**
 * 播放器 store 的回读语义。
 *
 * 这里锁的是本轮修过的一条真实缺陷：原 getter 用 `if (!state.x)` 判断是否回读
 * sessionStorage，把"未初始化"与"合法的零值"混为一谈 ——
 * listIndex=0（第一首）被历史索引替换、isPlay=false（刚暂停）被恢复成 true、
 * curTime=0 被旧进度覆盖导致进度条停在半路、listOfSongs 清空成 [] 又被旧缓存填回。
 * 现在初值为 null，只有 null 才回读。
 */
import song from '@/store/song';

const state = () => JSON.parse(JSON.stringify(Object.assign({}, song.state)));
const call = (key, s) => song.getters[key](s);

describe('store/song 缓存回读', () => {
  beforeEach(() => {
    window.sessionStorage.clear();
  });

  it('未初始化（null）时回读 sessionStorage', () => {
    window.sessionStorage.setItem('isPlay', 'true');
    window.sessionStorage.setItem('listIndex', '7');
    const s = state();
    expect(call('isPlay', s)).toBe(true);
    expect(call('listIndex', s)).toBe(7);
  });

  it('合法的零值不被历史缓存覆盖', () => {
    // 这正是旧写法出问题的场景：缓存里有旧值，而内存里是当前有效的零值
    window.sessionStorage.setItem('listIndex', '7');
    window.sessionStorage.setItem('isPlay', 'true');
    window.sessionStorage.setItem('duration', '123');
    const s = state();
    s.listIndex = 0;
    s.isPlay = false;
    s.duration = 0;
    expect(call('listIndex', s)).toBe(0);
    expect(call('isPlay', s)).toBe(false);
    expect(call('duration', s)).toBe(0);
  });

  it('显式清空的空列表不会被旧缓存填回', () => {
    window.sessionStorage.setItem('listOfSongs', JSON.stringify([{ id: 1 }]));
    const s = state();
    s.listOfSongs = [];
    expect(call('listOfSongs', s)).toEqual([]);
  });

  it('播放进度不回读旧值，避免进度条一进来就停在半路', () => {
    window.sessionStorage.setItem('curTime', '66');
    const s = state();
    expect(call('curTime', s)).toBe(0);
    s.curTime = 12;
    expect(call('curTime', s)).toBe(12);
  });

  it('脏缓存不应让整个 store 初始化抛错', () => {
    window.sessionStorage.setItem('listOfSongs', '{不是合法 JSON');
    const s = state();
    expect(call('listOfSongs', s)).toEqual([]);
  });

  it('setCurTime 这类高频瞬时状态不写缓存', () => {
    const s = state();
    song.mutations.setCurTime(s, 33);
    expect(s.curTime).toBe(33);
    expect(window.sessionStorage.getItem('curTime')).toBeNull();
  });

  it('需要持久化的状态会写缓存', () => {
    const s = state();
    song.mutations.setIsPlay(s, true);
    song.mutations.setListOfSongs(s, [{ id: 9 }]);
    expect(window.sessionStorage.getItem('isPlay')).toBe('true');
    expect(JSON.parse(window.sessionStorage.getItem('listOfSongs'))).toEqual([{ id: 9 }]);
  });
});
