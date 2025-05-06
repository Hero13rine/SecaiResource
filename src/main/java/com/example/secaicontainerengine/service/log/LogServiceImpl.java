package com.example.secaicontainerengine.service.log;

import cn.hutool.core.collection.CollUtil;
import com.example.secaicontainerengine.mapper.ContainerMapper;
import com.example.secaicontainerengine.pojo.entity.Log;
import com.example.secaicontainerengine.mapper.LogMapper;
import com.example.secaicontainerengine.pojo.vo.LogVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 容器运行过程中产生的日志 服务实现类
 * </p>
 *
 * @author CFZ
 * @since 2025-02-19
 */
@Service
@Slf4j
public class LogServiceImpl implements LogService {

    @Autowired
    private LogMapper logMapper;

    @Autowired
    private ContainerMapper containerMapper;

    @Override
    public int saveLog(Log newlog) {
        int res = logMapper.insert(newlog);
        if(res == 0){
            log.error("日志插入失败");
        }
        return res;
    }

    @Override
    public LogVO getLatestLogByMysql(String containerName, String messageKey) {
        LogVO result = logMapper.getLatestMessageValue(containerName, messageKey);
        if(result == null) {
            log.info("该容器还未产生日志或者不存在该容器");
            return null;
        }
        return result;
    }

    @Override
    public List<LogVO> getAllLogByMysql(String containerName) {
        return logMapper.getAllMessageValue(containerName);
    }

    @Override
    public Map<String, LogVO> getLatestLogByModelId(Long modelId, String messageKey) {
        List<String> containers = containerMapper.getContainerNameByModelId(modelId);
        if(CollUtil.isEmpty(containers)) {
            return Map.of();
        }
        Map<String, LogVO> resultMap = new HashMap<>();
        containers.forEach(container -> {
            LogVO res = getLatestLogByMysql(container, messageKey);
            resultMap.put(container, res);
        });
        return resultMap;
    }

    @Override
    public Map<String, List<LogVO>> getAllLogByModelId(Long modelId) {
        List<String> containers = containerMapper.getContainerNameByModelId(modelId);
        if(CollUtil.isEmpty(containers)) {
            return Map.of();
        }
        Map<String, List<LogVO>> resultMap = new HashMap<>();
        containers.forEach(container -> {
            List<LogVO> resList = logMapper.getAllMessageValue(container);
            resultMap.put(container, resList);
        });
        return resultMap;
    }

    @Override
    public void deleteByModelId(Long modelId) {
        //1. 获取modelId对应的所有容器名称
        List<String> containers = containerMapper.getContainerNameByModelId(modelId);
        //2. 删除这些容器的日志
        logMapper.deleteByContainers(containers);
    }

    @Override
    public void deleteByContainerName(String containerName) {
        logMapper.deleteByContainer(containerName);
    }

    @Override
    public void deleteByTime(LocalDateTime lastTime) {
        logMapper.deleteByTime(lastTime);
    }

}
