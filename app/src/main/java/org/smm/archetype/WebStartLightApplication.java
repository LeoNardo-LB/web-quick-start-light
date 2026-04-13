package org.smm.archetype;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.smm.archetype.config.properties.AppInfoProperties;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@EnableAspectJAutoProxy
@SpringBootApplication
@EnableConfigurationProperties(AppInfoProperties.class)
@RequiredArgsConstructor
@org.mybatis.spring.annotation.MapperScan(basePackages = "org.smm.archetype.generated.mapper", annotationClass = Mapper.class)
public class WebStartLightApplication implements CommandLineRunner {

    private final AppInfoProperties appInfoProperties;

    public static void main(String[] args) {
        SpringApplication.run(WebStartLightApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String port = appInfoProperties.getPort();
        String contextPath = appInfoProperties.getContextPath();
        String appName = appInfoProperties.getAppName();
        String openapiUrl = appInfoProperties.getOpenapiUrl();
        String apiDocUrl = appInfoProperties.getApiDocUrl();

        log.info("[{}]应用启动成功!", appName);
        log.info("Request URL: {}", String.format("http://127.0.0.1:%s%s", port, contextPath));
        log.info("Swagger URL: {}", String.format("http://127.0.0.1:%s%s%s", port, contextPath, openapiUrl));
        log.info("API-Doc URL: {}", String.format("http://127.0.0.1:%s%s%s", port, contextPath, apiDocUrl));
    }

}
