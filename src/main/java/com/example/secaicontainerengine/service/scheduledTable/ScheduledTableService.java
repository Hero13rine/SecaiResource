package com.example.secaicontainerengine.service.scheduledTable;

import com.example.secaicontainerengine.pojo.entity.ScheduledTable;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 调度任务表，用于保存所有被调度的任务 服务类
 * </p>
 *
 * @author CFZ
 * @since 2025-05-08
 */
public interface ScheduledTableService extends IService<ScheduledTable> {
    List<Long> getModelIdByStatus();

    List<Long> getFinishedModelId();
}
