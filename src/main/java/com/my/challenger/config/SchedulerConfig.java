package com.my.challenger.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableAsync
@EnableSchedulerLock(defaultLockAtMostFor = "PT30M")
public class SchedulerConfig {

    private HikariDataSource shedLockDataSource;

    @Bean
    public LockProvider lockProvider(DataSourceProperties dataSourceProperties) {
        // Create dedicated connection pool for ShedLock with auto-commit enabled
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dataSourceProperties.getUrl());
        hikariConfig.setUsername(dataSourceProperties.getUsername());
        hikariConfig.setPassword(dataSourceProperties.getPassword());
        hikariConfig.setDriverClassName(dataSourceProperties.getDriverClassName());
        
        // Small pool - ShedLock only needs minimal connections
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setPoolName("ShedLockPool");
        
        // Critical: Enable auto-commit for ShedLock operations
        hikariConfig.setAutoCommit(true);
        
        // Connection settings
        hikariConfig.setConnectionTimeout(10000);  // 10 seconds
        hikariConfig.setIdleTimeout(300000);       // 5 minutes
        hikariConfig.setMaxLifetime(600000);       // 10 minutes
        hikariConfig.setConnectionTestQuery("SELECT 1");
        
        this.shedLockDataSource = new HikariDataSource(hikariConfig);

        return new JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(shedLockDataSource))
                .withTableName("shedlock")
                .usingDbTime()
                .build()
        );
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(t -> {
            System.err.println("Scheduler error: " + t.getMessage());
            t.printStackTrace();
        });
        return scheduler;
    }

    @PreDestroy
    public void cleanup() {
        if (shedLockDataSource != null && !shedLockDataSource.isClosed()) {
            shedLockDataSource.close();
        }
    }
}
