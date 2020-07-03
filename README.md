# elastic-job-spring-boot-starter
elastic-job自动装配中间件

## 使用说明
- 新项目中引入依赖
```pom
        <dependency>
          <groupId>com.melot</groupId>
            <artifactId>elasticjob-spring-boot-starter</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>
```

- 写业务simple job（只开启了simple-job验证） 

```java

@Log4j2
@Component
public class HourlyRankRewardJob implements SimpleJob {
    @Override
    public void execute(ShardingContext shardingContext) {
        System.out.println("aaaaa");;
    }
}

```

- 配置对应的job (apollo或者本地配置均可，本地配置时请将overwrite=true)
<br>其余参数见 com.melot.elastic.job.autoconfigure.ElasticJobProperties

```properties
elastic.job.config.job-list[0].cron = 0/1 * * * * ?
elastic.job.config.job-list[0].description = 111
elastic.job.config.job-list[0].name = hourlyRankRewardJob

elastic.job.config.job-list[1].cron = 0/5 * * * * ?
elastic.job.config.job-list[1].description = 222
elastic.job.config.job-list[1].name = testJob

```
- 配置zk地址和namespace <br>其余参数见com.melot.elastic.job.autoconfigure.JobZookeeperProperties

```properties
elastic:
  job:
    zk:
      namespace: elastic-job-lite
      serverLists: zk1.kktv2.com:2181,zk2.kktv2.com:2181,zk3.kktv2.com:2181
```
