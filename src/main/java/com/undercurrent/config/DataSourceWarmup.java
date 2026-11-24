package com.undercurrent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * 启动时预热数据库连接池。
 * <p>Hikari 是懒加载的：第一个请求才去建连，如果网络 / MySQL 反查 DNS 慢（实测首次约 16 秒），
 * 会把这个耗时算到第一位用户头上，前端 15 秒超时就先放弃了。
 * 这里在启动阶段跑一次 SELECT 1，把建连成本挪到启动时。
 */
@Component
public class DataSourceWarmup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSourceWarmup.class);

    private final DataSource dataSource;

    public DataSourceWarmup(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        long started = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            log.info("数据库连接预热完成，耗时 {} ms", System.currentTimeMillis() - started);
        } catch (Exception e) {
            // 预热失败不阻断启动，请求时会再报错
            log.warn("数据库连接预热失败（{} ms）：{}", System.currentTimeMillis() - started, e.getMessage());
        }
    }
}
