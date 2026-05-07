package com.shanzhu.music.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果对象封装
 */
@Data
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码
     */
    private int pageNum;
    
    /**
     * 每页显示条数
     */
    private int pageSize;
    
    /**
     * 总页数
     */
    private int pages;
    
    /**
     * 数据列表
     */
    private List<T> list;
    
    public PageResult() {
    }
    
    public PageResult(long total, int pageNum, int pageSize, int pages, List<T> list) {
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
        this.list = list;
    }
}
