package com.melot.elastic.job.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Calendar;
import java.util.List;

/**
 * Title: ElasticJobProperties
 * <p>
 * Description:
 * </p>
 *
 * @author <a href="junjian.lan@melot.cn">蓝钧剑</a>
 * @version V1.0
 * @since 2020/7/3
 */

@Data
@ConfigurationProperties(prefix = "elastic.job.config")
public class ElasticJobProperties {

    List<Config> jobList;

    @Data
    public static class Config {
        String name;    //elasticjob的名称

        String cron = "";

        int shardingTotalCount = 1;

        String shardingItemParameters = "";

        String jobParameter = "";

        boolean failover = false;

        boolean misfire = true;

        String description = "";

        boolean overwrite = false;

        boolean streamingProcess = false;

        String scriptCommandLine = "";

        boolean monitorExecution = false;

        public int monitorPort = -1;    //must

        public int maxTimeDiffSeconds = -1;    //must

        public String jobShardingStrategyClass = "";    //must

        public int reconcileIntervalMinutes = 10;    //must

        public String eventTraceRdbDataSource = "";    //must

        public String listener = "";    //must

        public boolean disabled = false;    //must

        public String distributedListener = "";

        public long startedTimeoutMilliseconds = Long.MAX_VALUE;    //must

        public long completedTimeoutMilliseconds = Long.MAX_VALUE;        //must

        public String jobExceptionHandler = "com.dangdang.ddframe.job.executor.handler.impl.DefaultJobExceptionHandler";

        public String executorServiceHandler = "com.dangdang.ddframe.job.executor.handler.impl.DefaultExecutorServiceHandler";
    }
}
