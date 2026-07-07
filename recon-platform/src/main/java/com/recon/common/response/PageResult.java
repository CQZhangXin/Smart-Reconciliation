package com.recon.common.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private long total;
    private int page;
    private int size;
    private List<T> records;

    public static <T> PageResult<T> of(long total, int page, int size, List<T> records) {
        return new PageResult<>(total, page, size, records);
    }

    public static <T> PageResult<T> of(long total, List<T> records) {
        return new PageResult<>(total, 1, records != null ? records.size() : 0, records);
    }

    /**
     * 从MyBatis-Plus IPage对象构建分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize(),
                page.getRecords() != null ? page.getRecords() : Collections.emptyList()
        );
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0, 1, 10, Collections.emptyList());
    }
}
