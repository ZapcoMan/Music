package com.shanzhu.music.common;

/**
 * 分页每页条数的上限。
 *
 * 为什么写在代码里，而不是用 pagehelper.max-page-size：那个配置只挂在
 * Page.setPageSize() 这条路径上，而本项目四处入口都是
 * PageHelper.startPage(pageNum, pageSize) 显式起页——其构造函数直接给字段赋值，
 * 不经过 setPageSize，所以上限根本不会被应用。实测 pageSize=5000 时
 * PageInfo.pageSize 原样返回 5000（用例见 SongControllerTest
 * #testPageSizeCappedByMaxPageSize）。写了不生效的配置，比没有配置更容易骗过后人。
 */
public final class PageLimits {

    /**
     * 单页最多返回条数。此前任意登录用户传大 pageSize 就能把整张表
     * （歌曲侧还连带 lyric 大字段）一次性拉走。
     */
    public static final int MAX_PAGE_SIZE = 100;

    private PageLimits() {
    }

    /**
     * 把客户端传入的每页条数夹到上限内。
     * 只处理过大的情况：非正值维持 PageHelper 原有语义，不在此处顺手改写，
     * 免得把"传 0 拿到空列表"这类既有行为一并变更。
     *
     * @param pageSize 客户端传入的每页条数
     * @return 不超过 {@link #MAX_PAGE_SIZE} 的每页条数
     */
    public static int clamp(int pageSize) {
        return pageSize > MAX_PAGE_SIZE ? MAX_PAGE_SIZE : pageSize;
    }
}
