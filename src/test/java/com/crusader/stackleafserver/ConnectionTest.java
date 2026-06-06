package com.crusader.stackleafserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void mysqlConnection() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("MySQL 连接成功!");
            System.out.println("  URL:      " + meta.getURL());
            System.out.println("  产品:     " + meta.getDatabaseProductName());
            System.out.println("  版本:     " + meta.getDatabaseProductVersion());
            System.out.println("  用户:     " + meta.getUserName());
            assertFalse(conn.isClosed());
        }
    }

    @Test
    void redisConnection() {
        String key = "stackleaf:test:ping";
        stringRedisTemplate.opsForValue().set(key, "pong");
        String value = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);

        System.out.println("Redis 连接成功!");
        System.out.println("  写入: " + key + " = pong");
        System.out.println("  读取: " + key + " = " + value);
        assertEquals("pong", value);
    }
}
