package quick.job.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import quick.job.common.listener.RedisMessageListener;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class JobConfig {

    @Bean
    public MessageListenerAdapter messageListenerAdapter() {
        return new MessageListenerAdapter(new RedisMessageListener());
    }

    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic("pubsub:redis-job");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(MessageListener messageListener, ChannelTopic topic, JedisConnectionFactory jedisConnectionFactory) {
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(jedisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(messageListener, topic);
        return redisMessageListenerContainer;
    }

    @Bean
    public SchedulerFactoryBean scheduler(DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setOverwriteExistingJobs(true);
        //quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "JobScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        //线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20");
        prop.put("org.quartz.threadPool.threadPriority", "5");
        //JobStore配置
        prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        //集群配置
        prop.put("org.quartz.jobStore.isClustered", "true");
        prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        prop.put("org.quartz.jobStore.misfireThreshold", "12000");
        prop.put("org.quartz.jobStore.tablePrefix", "QUARTZ_");
        schedulerFactoryBean.setQuartzProperties(prop);

        schedulerFactoryBean.setSchedulerName("JobScheduler");
        //延时启动
        schedulerFactoryBean.setStartupDelay(30);
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContextKey");
        //可选，QuartzScheduler 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        return schedulerFactoryBean;
    }
}

