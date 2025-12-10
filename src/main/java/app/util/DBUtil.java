package app.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBUtil {
    private static final HikariDataSource ds;

    static {
        HikariConfig cfg = new HikariConfig();
        // Use 127.0.0.1 not hostname to avoid DNS lookup delays
        cfg.setJdbcUrl("jdbc:mysql://10.10.116.201:3306/mandar?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=30000");
        cfg.setUsername("root");       // change
        cfg.setPassword("root");       // change
        cfg.setMaximumPoolSize(10);
        cfg.setMinimumIdle(2);
        cfg.setConnectionTimeout(5000); // wait this long for connection from pool
        cfg.setIdleTimeout(600000);
        cfg.setMaxLifetime(1800000);
        ds = new HikariDataSource(cfg);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
