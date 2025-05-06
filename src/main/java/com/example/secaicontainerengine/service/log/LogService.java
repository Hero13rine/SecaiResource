package com.example.secaicontainerengine.service.log;

import com.example.secaicontainerengine.pojo.entity.Log;
import com.example.secaicontainerengine.pojo.vo.LogVO;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 容器运行过程中产生的日志 服务类
 * </p>
 *
 * @author CFZ
 * @since 2025-02-19
 */
public interface LogService{

    int saveLog(Log log);

    LogVO getLatestLogByMysql(String containerName, String messageKey);

    List<LogVO> getAllLogByMysql(String containerName);

    Map<String, LogVO> getLatestLogByModelId(Long modelId, String messageKey);

    Map<String, List<LogVO>> getAllLogByModelId(Long modelId);

    void deleteByModelId(Long modelId);

    void deleteByContainerName(String containerName);

    void deleteByTime(LocalDateTime lastTime);

}
