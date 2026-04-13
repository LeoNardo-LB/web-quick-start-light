package org.smm.archetype.generated;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.smm.archetype.repository.BaseDO;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * MyBatis Plus 代码生成器。
 * 使用环境变量配置数据库连接：DB_URL, DB_USERNAME, DB_PASSWORD
 * 生成的代码禁止手动修改。
 */
public class MybatisPlusGenerator {

    private static final String DATABASE_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:sqlite:./data/app.db");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");

    private static final String PACKAGE = "org.smm.archetype.generated";
    private static final String[] TABLES = {"system_config"};
    private static final String SOURCE_DIR = Paths.get(System.getProperty("user.dir")) + "/app/src/main/java";

    public static void main(String[] args) {
        FastAutoGenerator.create(DATABASE_URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("CodeGenerator")
                        .outputDir(SOURCE_DIR)
                        .dateType(DateType.TIME_PACK)
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent(PACKAGE)
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, "")))
                .strategyConfig(builder -> builder
                        .addInclude(TABLES)
                        .addTablePrefix("")
                        .entityBuilder()
                        .superClass(BaseDO.class)
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        // SQLite 没有 delete_time 逻辑删除字段，移除 logicDelete 配置
                        .addSuperEntityColumns("id", "create_time", "update_time", "create_user", "update_user")
                        .formatFileName("%sDO")
                        .idType(IdType.ASSIGN_ID)
                        .addTableFills(
                                new Column("create_time", FieldFill.INSERT),
                                new Column("update_time", FieldFill.INSERT_UPDATE),
                                new Column("create_user", FieldFill.INSERT),
                                new Column("update_user", FieldFill.INSERT_UPDATE)
                        )
                        .enableFileOverride()
                        .mapperBuilder()
                        .superClass(com.baomidou.mybatisplus.core.mapper.BaseMapper.class)
                        .mapperAnnotation(org.apache.ibatis.annotations.Mapper.class)
                        .formatMapperFileName("%sMapper")
                        .enableFileOverride()
                        .controllerBuilder()
                        .disable())
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        System.out.println("代码生成完成！输出目录：" + SOURCE_DIR);
    }
}
