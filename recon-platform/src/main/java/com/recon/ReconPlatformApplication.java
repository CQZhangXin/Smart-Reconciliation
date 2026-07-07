package com.recon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能AI对账平台 - 主应用入口
 *
 * @author Recon Team
 * @since 1.0.0
 */
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
}
