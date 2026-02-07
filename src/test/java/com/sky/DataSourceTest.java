package com.sky;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.Connection;

// 加载SpringBoot上下文，必须加这个注解
@SpringBootTest
public class DataSourceTest {

    // 自动注入配置好的数据源
    @Autowired
    private DataSource dataSource;

    // 测试数据库连接
    @Test
    public void testConnection() throws Exception {
        Connection conn = dataSource.getConnection();
        System.out.println("数据库连接成功！");
        System.out.println("连接对象：" + conn);
        conn.close();
    }
}