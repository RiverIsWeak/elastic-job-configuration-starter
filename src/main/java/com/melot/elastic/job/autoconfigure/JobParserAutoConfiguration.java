package com.melot.elastic.job.autoconfigure;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.melot.elastic.job.parser.ElasticJobConfParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "elastic.job.zk",name = {"namespace","serverLists"},matchIfMissing = false)
@EnableConfigurationProperties({JobZookeeperProperties.class, ElasticJobProperties.class})
public class JobParserAutoConfiguration {

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter zookeeperRegistryCenter(JobZookeeperProperties jobZookeeperProperties) {
        ZookeeperConfiguration zkConfig = new ZookeeperConfiguration(jobZookeeperProperties.getServerLists(),
                jobZookeeperProperties.getNamespace());
        zkConfig.setBaseSleepTimeMilliseconds(zkConfig.getBaseSleepTimeMilliseconds());
        zkConfig.setMaxSleepTimeMilliseconds(zkConfig.getMaxSleepTimeMilliseconds());
        zkConfig.setConnectionTimeoutMilliseconds(zkConfig.getConnectionTimeoutMilliseconds());
        zkConfig.setSessionTimeoutMilliseconds(zkConfig.getSessionTimeoutMilliseconds());
        zkConfig.setMaxRetries(zkConfig.getMaxRetries());
        zkConfig.setDigest(zkConfig.getDigest());
        log.info("初始化job注册中心配置成功, zk address : {}, namespace : {}", jobZookeeperProperties.getServerLists(), jobZookeeperProperties.getNamespace());
        return new ZookeeperRegistryCenter(zkConfig);
    }

    @Bean
    public ElasticJobConfParser elasticJobConfParser(JobZookeeperProperties jobZookeeperProperties, ZookeeperRegistryCenter zookeeperRegistryCenter, ElasticJobProperties elasticJobProperties) {
        return new ElasticJobConfParser(jobZookeeperProperties, zookeeperRegistryCenter, elasticJobProperties);
    }


}
