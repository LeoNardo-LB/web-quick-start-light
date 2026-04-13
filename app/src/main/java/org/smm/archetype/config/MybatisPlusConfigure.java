package org.smm.archetype.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

/**
 * MyBatis-Plus 手动配置.
 * <p>
 * MyBatis-Plus 3.5.16 的 {@code MybatisPlusAutoConfiguration} 使用
 * {@code @ConditionalOnSingleCandidate(javax.sql.DataSource)}，在 Spring Boot 4.0.2 下
 * 因 DataSource 类型解析变化（javax → java.sql）导致条件不满足，自动配置未生效。
 * 因此手动注册 {@code SqlSessionFactory} 以确保 MyBatis-Plus 正常工作。
 */
@Configuration
public class MybatisPlusConfigure {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);

        // 全局配置
        GlobalConfig globalConfig = new GlobalConfig();
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.ASSIGN_ID);
        globalConfig.setDbConfig(dbConfig);
        bean.setGlobalConfig(globalConfig);

        // MyBatis 配置
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        bean.setConfiguration(configuration);

        // 分页插件（v3.5.9+ 需要引入 mybatis-plus-jsqlparser 依赖）
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.SQLITE));
        bean.setPlugins(interceptor);

        // Mapper XML 位置（如有）
        bean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/**/*.xml")
        );

        return bean.getObject();
    }
}
