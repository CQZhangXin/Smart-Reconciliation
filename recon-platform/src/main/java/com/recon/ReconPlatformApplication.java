package com.recon;

import com.recon.module.system.entity.SysUser;
import com.recon.module.system.repository.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 智能AI对账平台 - 主应用入口
 *
 * @author Recon Team
 * @since 1.0.0
 */
@Slf4j
@EnableAsync
@EnableScheduling
@SpringBootApplication
public class ReconPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconPlatformApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════════════════╗
                ║          智能 AI 对账平台 v1.0.0                          ║
                ║          Intelligent AI Reconciliation Platform           ║
                ║                                                          ║
                ║  Swagger UI: http://localhost:8080/swagger-ui.html       ║
                ║  API Docs:   http://localhost:8080/v3/api-docs           ║
                ║  Health:     http://localhost:8080/actuator/health       ║
                ╚══════════════════════════════════════════════════════════╝
                """);
    }

    /**
     * 启动时确保 admin 密码哈希正确（修复 schema.sql 中手工构造的无效哈希）
     */
    @Bean
    public ApplicationRunner resetAdminPassword(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        return args -> {
            SysUser admin = userMapper.selectById(1L);
            if (admin == null) {
                log.warn("Admin user (id=1) not found in database. Run schema.sql first.");
                return;
            }
            String newHash = passwordEncoder.encode("admin123");
            if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                admin.setPassword(newHash);
                userMapper.updateById(admin);
                log.info("Admin password hash has been reset to 'admin123'");
            }
        };
    }
}
