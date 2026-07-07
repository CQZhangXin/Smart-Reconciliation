package com.recon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / SpringDoc OpenAPI 配置
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能AI对账平台 API")
                        .version("1.0.0")
                        .description("""
                                Intelligent AI Reconciliation Platform REST API

                                ## 功能模块
                                - 数据源管理: 多源连接、智能字段映射、数据同步
                                - 规则引擎: 预置模板、可视化编排、自然语言规则生成
                                - 智能匹配引擎: 精确匹配 + 规则匹配 + AI语义匹配 + 拆单匹配
                                - 差异管理中心: AI自动分类、根因分析、处理建议
                                - 对账工作台: 任务调度、进度监控、批量操作
                                - 智能分析: 健康度仪表盘、自然语言查询、报告生成
                                - 审批工作流: 可配置流程、任务分配、SLA监控
                                - 系统管理: 多组织、RBAC、审计日志

                                ## 认证方式
                                在请求头中添加: `Authorization: Bearer {token}`
                                """)
                        .contact(new Contact()
                                .name("Recon Team")
                                .email("team@recon-platform.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://recon-platform.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .name("Bearer")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("输入JWT Token: Bearer {token}")));
    }
}
