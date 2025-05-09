package com.example.secaicontainerengine.service.scheduledTable;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.secaicontainerengine.mapper.ScheduledTableMapper;
import com.example.secaicontainerengine.pojo.entity.Container;
import com.example.secaicontainerengine.pojo.entity.ScheduledTable;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secaicontainerengine.service.container.ContainerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 调度任务表，用于保存所有被调度的任务 服务实现类
 * </p>
 *
 * @author CFZ
 * @since 2025-05-08
 */
@Service
public class ScheduledTableServiceImpl extends ServiceImpl<ScheduledTableMapper, ScheduledTable> implements ScheduledTableService {

    @Autowired
    private ContainerService containerService;

    @Override
    public List<Long> getModelIdByStatus() {
        //查询出还未调度的任务
        List<ScheduledTable> taskList = this.list(
                new QueryWrapper<ScheduledTable>().eq("status", 0)
        );
        //提取modelId字段
        List<Long> modelIds = taskList.stream()
                .map(ScheduledTable::getModelId)
                .filter(Objects::nonNull)
                .toList();
        //把这些任务的status修改为1,代表已调度
        for (ScheduledTable task : taskList) {
            task.setStatus(1);
        }
        this.updateBatchById(taskList);
        return modelIds;
    }

    @Override
    public List<Long> getFinishedModelId() {
        // 查询出status字段为1的任务
        List<ScheduledTable> taskList = this.list(
                new QueryWrapper<ScheduledTable>().eq("status", 1)
        );
        //提取modelId字段
        List<Long> modelIds = taskList.stream()
                .map(ScheduledTable::getModelId)
                .filter(Objects::nonNull)
                .toList();
        List<Long> finished=new ArrayList<>();
        // 依次检查这些modelId对应的pod是否已经执行完毕
        for (Long modelId : modelIds) {
            List<Container> containers = containerService.list(
                    new QueryWrapper<Container>().eq("modelId", modelId)
            );
            boolean allFinished = containers.stream()
                    .allMatch(container -> {
                        String status = container.getStatus();
                        return "Succeed".equalsIgnoreCase(status) || "Failed".equalsIgnoreCase(status);
                    });
            if (allFinished) {
                finished.add(modelId);
            }
        }
        return finished;
    }


}
