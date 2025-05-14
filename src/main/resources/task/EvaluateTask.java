package com.ruoyi.quartz.task;

import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.quartz.domain.SysJob;
import com.ruoyi.quartz.service.ISysJobService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component("evaluateTask")
@Slf4j
public class EvaluateTask {

    @Autowired
    private ISysJobService jobService;

    @Autowired
    private OkHttpClient okHttpClient;


    //评测任务启动
    public void startEvaluation(Long modelId) throws SchedulerException, TaskException {
        log.info("本次调度操作的评测模型id：{}", modelId);
        SysJob job = jobService.getByModelId(modelId);
        switch (job.getEvaluate_status()) {
            case "0": {
                //---------------1.评估状态为0，调用评测启动接口----------
                String json = "{ \"modelId\": " + modelId + " }";
                RequestBody body = RequestBody.create(
                        json,
                        MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder()
                        .url("http://localhost:8081/scheduled-task") // 替换成你的后端地址
                        .post(body)
                        .build();
                try {
                    Response response = okHttpClient.newCall(request).execute();
                    //修改评测状态为1
                    job.setEvaluate_status("1");
                    jobService.updateJob(job);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //结束本次调度
                return;
            }
            case "1": {
                //---------2.评估状态为1，调用查询所有Pod是否执行完毕接口---
                HttpUrl url = HttpUrl.parse("http://localhost:8081/scheduled-task")
                        .newBuilder()
                        .addQueryParameter("modelId", String.valueOf(modelId))
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .get()
                        .build();
                try {
                    Response response = okHttpClient.newCall(request).execute();
                    String responseBody = response.body().string();
                    if (responseBody.equals("0")) {
                        //0代表还有Pod没有执行完毕
                        return;
                    } else if (responseBody.equals("1")) {
                        //1代表所有Pod执行成功
                        job.setEvaluate_status("2");
                    } else if (responseBody.equals("2")) {
                        //2代表有Pod执行失败
                        job.setEvaluate_status("3");
                    }
                    jobService.updateJob(job);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //结束本次调度
                return;
            }
            case "2":
                //---------3.评估状态为2，调用计算评测结果接口接口---------
                String json = "{ \"modelId\": " + modelId + " }";
                RequestBody body = RequestBody.create(
                        json,
                        MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder()
                        .url("http://localhost:8081/scheduled-task/result") // 替换成你的后端地址
                        .post(body)
                        .build();
                try {
                    Response response = okHttpClient.newCall(request).execute();
                    //修改评测状态为1
                    job.setEvaluate_status("1");
                    jobService.updateJob(job);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                job.setEvaluate_status("4");
                jobService.updateJob(job);
                return;
            default:
                //---------3.评估状态为3或4，结束当前调度任务-- ---------
                job.setStatus("1");
                jobService.changeStatus(job);
                log.info("评测模型id：{}的调度任务已被暂停", modelId);
                break;
        }
    }

}
