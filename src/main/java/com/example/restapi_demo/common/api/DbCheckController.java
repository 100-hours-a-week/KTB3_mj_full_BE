package com.example.restapi_demo.common.api;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Profile({"default", "dev"}) // 선택: 운영(prod)에서는 자동으로 비활성화
@RestController
public class DbCheckController {

    private final DataSource dataSource;

    public DbCheckController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/db/check")
    public String check() throws Exception {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            int ok = rs.getInt(1); // 기대값 1
            return "DB OK: " + ok + " (url=" + conn.getMetaData().getURL() + ")";
        }
    }
}
