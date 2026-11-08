package com.shanzhu.music.service;

import com.shanzhu.music.entity.dto.TagSaveRequest;
import com.shanzhu.music.entity.po.Tag;
import com.shanzhu.music.entity.vo.TaggedSongVo;

import java.util.List;

/**
 * 标签服务层。
 *
 * 存在的理由：song.tags（逗号串）与 song_tag（中间表）长期双轨并存且无任何同步代码，
 * 谁都可能只写一侧让另一侧静默滞后。标签的唯一写入口在这里。
 *
 * 分工上刻意不把写 song 表也包进来：song.tags 由歌曲自身的 insert/update 落库
 * （那条路径带缓存失效），本服务只负责校验标签名与重建中间表，
 * 两者由调用方放在同一个事务里。
 */
public interface TagService {

    /**
     * 校验并规范化逗号串标签：去空白、去重、逐个确认存在于标签字典。
     *
     * @param tagsCsv 逗号分隔的标签名
     * @return 规范化后的逗号串；入参为 null 时返回 null 表示不改动
     * @throws BusinessException 含字典中不存在的标签名
     */
    String normalizeTags(String tagsCsv);

    /**
     * 校验并规范化标签名列表：去空白、去重、逐个确认存在于标签字典。
     *
     * <p>与 {@link #normalizeTags(String)} 是同一套校验，区别只在入参形态：
     * 资源式接口用 JSON 数组传标签，逗号不再充当分隔符，
     * 否则标签名本身含逗号时会被拆成两个而误判为非法。
     * 返回值仍是逗号串，因为 song.tags / singer.tags 的存储形态就是它。
     *
     * @param tagNames 标签名列表；入参为 null 时返回 null 表示不改动
     * @return 规范化后的逗号串，空列表得到空串（即清空）
     * @throws BusinessException 含字典中不存在的标签名
     */
    String normalizeTagNames(List<String> tagNames);

    /**
     * 用规范化后的标签串重建某首歌的 song_tag 关联（先删后插）。
     *
     * @param songId  歌曲id
     * @param tagsCsv 已经 normalizeTags 处理过的逗号串
     * @return 生效的标签数量
     */
    int replaceSongTags(Integer songId, String tagsCsv);

    /**
     * 查询歌曲当前标签（取自中间表这一真源）
     *
     * @param songId 歌曲id
     * @return 标签名列表
     */
    List<String> songTagNames(Integer songId);

    /**
     * 热门标签，按关联歌曲数倒序
     *
     * @param limit 数量上限
     * @return 标签列表
     */
    List<Tag> hotTags(int limit);

    /**
     * 按标签集合取歌曲，结果携带 matchCount 供可解释的命中说明。
     *
     * 这条原先由 Controller 直接调 TagMapper，属于 web 层穿透到持久层；
     * limit 的区间夹取（1..100）是业务规则，一起搬进来，
     * 否则换一种调用方式就能绕过上限把整表连歌词一起拉走。
     *
     * @param tagIds 标签id集合，调用方须保证非空
     * @param limit  期望条数，实现内会夹到 1-100
     * @return 带命中数的歌曲视图列表
     */
    List<TaggedSongVo> selectSongsByTagIds(List<Integer> tagIds, int limit);

    /**
     * 批量写入标签字典，按名字去重。
     *
     * @param requests 标签请求，name 为空的条目会被跳过而不是报错
     * @return 实际提交条数（不等于新增条数：重复项由字典表去重忽略，
     *         INSERT IGNORE 的返回值在重复时仍等于提交条数，不能当新增数用）
     */
    int batchAdd(List<TagSaveRequest> requests);

    /**
     * 用规范化标签串重建某位歌手的标签关联（先删后插）。
     * 与歌曲侧同构：singer_tag 是真源，singer.tags 逗号串由歌手更新路径写入。
     *
     * @param singerId 歌手id
     * @param tagsCsv  已经 normalizeTags 处理过的逗号串
     * @return 生效的标签数量
     */
    int replaceSingerTags(Integer singerId, String tagsCsv);

    /**
     * 查询歌手当前标签（取自 singer_tag 这一真源）
     *
     * @param singerId 歌手id
     * @return 标签名列表
     */
    List<String> singerTagNames(Integer singerId);
}
