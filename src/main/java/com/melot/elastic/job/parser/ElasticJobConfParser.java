package com.melot.elastic.job.parser;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.melot.elastic.job.autoconfigure.ElasticJobProperties;
import com.melot.elastic.job.autoconfigure.JobZookeeperProperties;
import com.melot.elastic.job.enums.ElasticJobTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
public class ElasticJobConfParser implements ApplicationListener<ApplicationReadyEvent> {
    private JobZookeeperProperties jobZookeeperProperties;
    private ZookeeperRegistryCenter zookeeperRegistryCenter;
    private ElasticJobProperties elasticJobProperties;

    public ElasticJobConfParser(JobZookeeperProperties jobZookeeperProperties, ZookeeperRegistryCenter zookeeperRegistryCenter, ElasticJobProperties elasticJobProperties) {
        this.jobZookeeperProperties = jobZookeeperProperties;
        this.zookeeperRegistryCenter = zookeeperRegistryCenter;
        this.elasticJobProperties = elasticJobProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            ApplicationContext applicationContext = event.getApplicationContext();
            //从已加载的bean中拿simpleJob类型的bean
            Map<String, ElasticJob> beanMap = applicationContext.getBeansOfType(ElasticJob.class);
            for (Map.Entry<String, ElasticJob> entry : beanMap.entrySet()) {
                Class<?> clazz = entry.getValue().getClass();
                if (clazz.getName().indexOf("$") > 0) {
                    String className = clazz.getName();
                    clazz = Class.forName(className.substring(0, className.indexOf("$")));
                }
                // 	获取接口类型 用于判断是什么类型的任务
                String jobTypeName = clazz.getInterfaces()[0].getSimpleName();

                String jobClass = entry.getValue().getClass().getName();
                String beanId = entry.getKey();
                ElasticJobProperties.Config conf = getConfigDetailByJobName(beanId);
                String jobName = this.jobZookeeperProperties.getNamespace() + "." + conf.getName();
                String cron = conf.getCron();
                String shardingItemParameters = conf.getShardingItemParameters();
                String description = conf.getDescription();
                String jobParameter = conf.getJobExceptionHandler();
                String jobExceptionHandler = conf.getJobExceptionHandler();
                String executorServiceHandler = conf.getExecutorServiceHandler();

                String jobShardingStrategyClass = conf.getJobShardingStrategyClass();
                String eventTraceRdbDataSource = conf.getEventTraceRdbDataSource();
                String scriptCommandLine = conf.getScriptCommandLine();

                boolean failover = conf.isFailover();
                boolean misfire = conf.isMisfire();
                boolean overwrite = conf.isOverwrite();
                boolean disabled = conf.isDisabled();
                boolean monitorExecution = conf.isMonitorExecution();
                boolean streamingProcess = conf.isStreamingProcess();

                int shardingTotalCount = conf.getShardingTotalCount();
                int monitorPort = conf.getMonitorPort();
                int maxTimeDiffSeconds = conf.getMaxTimeDiffSeconds();
                int reconcileIntervalMinutes = conf.getReconcileIntervalMinutes();

                //	先把当当网的esjob的相关configuration
                JobCoreConfiguration coreConfig = JobCoreConfiguration
                        .newBuilder(jobName, cron, shardingTotalCount)
                        .shardingItemParameters(shardingItemParameters)
                        .description(description)
                        .failover(failover)
                        .jobParameter(jobParameter)
                        .misfire(misfire)
                        .jobProperties(JobProperties.JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey(), jobExceptionHandler)
                        .jobProperties(JobProperties.JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey(), executorServiceHandler)
                        .build();

                //	我到底要创建什么样的任务.
                JobTypeConfiguration typeConfig = null;
                if (ElasticJobTypeEnum.SIMPLE.getType().equals(jobTypeName)) {
                    typeConfig = new SimpleJobConfiguration(coreConfig, jobClass);
                }

                if (ElasticJobTypeEnum.DATAFLOW.getType().equals(jobTypeName)) {
                    typeConfig = new DataflowJobConfiguration(coreConfig, jobClass, streamingProcess);
                }

                if (ElasticJobTypeEnum.SCRIPT.getType().equals(jobTypeName)) {
                    typeConfig = new ScriptJobConfiguration(coreConfig, scriptCommandLine);
                }

                // LiteJobConfiguration
                LiteJobConfiguration jobConfig = LiteJobConfiguration
                        .newBuilder(typeConfig)
                        .overwrite(overwrite)
                        .disabled(disabled)
                        .monitorPort(monitorPort)
                        .monitorExecution(monitorExecution)
                        .maxTimeDiffSeconds(maxTimeDiffSeconds)
                        .jobShardingStrategyClass(jobShardingStrategyClass)
                        .reconcileIntervalMinutes(reconcileIntervalMinutes)
                        .build();

                // 创建一个Spring的beanDefinition
                BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(SpringJobScheduler.class);
                factory.setInitMethodName("init");
                factory.setScope("prototype");

                //	1.添加bean构造参数，相当于添加自己的真实的任务实现类
                if (!ElasticJobTypeEnum.SCRIPT.getType().equals(jobTypeName)) {
                    factory.addConstructorArgValue(entry.getValue());
                }
                //	2.添加注册中心
                factory.addConstructorArgValue(this.zookeeperRegistryCenter);
                //	3.添加LiteJobConfiguration
                factory.addConstructorArgValue(jobConfig);

                //	4.如果有eventTraceRdbDataSource 则也进行添加
                if (StringUtils.hasText(eventTraceRdbDataSource)) {
                    BeanDefinitionBuilder rdbFactory = BeanDefinitionBuilder.rootBeanDefinition(JobEventRdbConfiguration.class);
                    rdbFactory.addConstructorArgReference(eventTraceRdbDataSource);
                    factory.addConstructorArgValue(rdbFactory.getBeanDefinition());
                }

                //  5.添加监听
                List<?> elasticJobListeners = getTargetElasticJobListeners(conf);
                factory.addConstructorArgValue(elasticJobListeners);

                // 接下来就是把factory 也就是 SpringJobScheduler注入到Spring容器中
                DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

                String registerBeanName = conf.getName() + "SpringJobScheduler";
                defaultListableBeanFactory.registerBeanDefinition(registerBeanName, factory.getBeanDefinition());
                SpringJobScheduler scheduler = (SpringJobScheduler) applicationContext.getBean(registerBeanName);
                scheduler.init();
                log.info("启动elastic-job作业: " + jobName);
            }
            log.info("共计启动elastic-job作业数量为: {} 个", beanMap.values().size());

        } catch (Exception e) {
            log.error("elasticjob 启动异常, 系统强制退出", e);
            System.exit(1);
        }
    }

    private List<BeanDefinition> getTargetElasticJobListeners(ElasticJobProperties.Config conf) {
        List<BeanDefinition> result = new ManagedList<BeanDefinition>(2);
        String listeners = conf.getListener();
        if (StringUtils.hasText(listeners)) {
            BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(listeners);
            factory.setScope("prototype");
            result.add(factory.getBeanDefinition());
        }

        String distributedListeners = conf.getDistributedListener();
        long startedTimeoutMilliseconds = conf.getStartedTimeoutMilliseconds();
        long completedTimeoutMilliseconds = conf.getCompletedTimeoutMilliseconds();

        if (StringUtils.hasText(distributedListeners)) {
            BeanDefinitionBuilder factory = BeanDefinitionBuilder.rootBeanDefinition(distributedListeners);
            factory.setScope("prototype");
            factory.addConstructorArgValue(startedTimeoutMilliseconds);
            factory.addConstructorArgValue(completedTimeoutMilliseconds);
            result.add(factory.getBeanDefinition());
        }
        return result;
    }

    private ElasticJobProperties.Config getConfigDetailByJobName(String beanId) {
        ElasticJobProperties.Config result = null;

        List<ElasticJobProperties.Config> jobList = elasticJobProperties.getJobList();
        for (ElasticJobProperties.Config config : jobList) {
            if (beanId.equals(config.getName())) {
                result = config;
            }
        }

        return result;
    }
}
