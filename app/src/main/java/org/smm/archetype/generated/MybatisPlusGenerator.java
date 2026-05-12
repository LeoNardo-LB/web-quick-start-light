package org.smm.archetype.generated;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.fill.Column;
import org.smm.archetype.entity.base.BaseDO;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

/**
 * 代码生成器：生成完整模块骨架（11 个文件）。
 * <p>
 * 使用方式：
 * <pre>
 * java org.smm.archetype.generated.MybatisPlusGenerator --module systemconfig
 * </pre>
 * <p>
 * 生成的 11 个文件：
 * <ol>
 *   <li>SystemConfigDO.java — 数据对象（MyBatis-Plus Generator 生成）</li>
 *   <li>SystemConfigMapper.java — Mapper 接口（MyBatis-Plus Generator 生成）</li>
 *   <li>SystemConfig.java — 领域模型（模板生成）</li>
 *   <li>SystemConfigRepository.java — 仓储接口（模板生成）</li>
 *   <li>SystemConfigRepositoryImpl.java — 仓储实现（模板生成）</li>
 *   <li>SystemConfigConverter.java — DO↔Model 转换器（模板生成）</li>
 *   <li>SystemConfigService.java — Service（模板生成）</li>
 *   <li>SystemConfigFacade.java — Facade 接口（模板生成）</li>
 *   <li>SystemConfigFacadeImpl.java — Facade 实现（模板生成）</li>
 *   <li>SystemConfigController.java — Controller（模板生成）</li>
 *   <li>SystemConfigVO.java — VO（模板生成）</li>
 * </ol>
 * <p>
 * 生成的代码禁止手动修改。
 */
public class MybatisPlusGenerator {

    private static final String DATABASE_URL = System.getenv().getOrDefault("DB_URL",
            "jdbc:sqlite:./data/app.db");
    private static final String USERNAME = System.getenv().getOrDefault("DB_USERNAME", "");
    private static final String PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    private static final String SOURCE_DIR = Paths.get(System.getProperty("user.dir")) + "/app/src/main/java";

    private static final Map<String, ModuleConfig> MODULES = Map.of(
            "systemconfig", new ModuleConfig("org.smm.archetype.systemconfig", "system_config", "系统配置"),
            "auth", new ModuleConfig("org.smm.archetype.auth", "user", "用户"),
            "operationlog", new ModuleConfig("org.smm.archetype.operationlog", "operation_log", "操作日志")
    );

    public static void main(String[] args) {
        String moduleName = parseModuleName(args);
        ModuleConfig config = MODULES.get(moduleName);

        if (config == null) {
            System.err.println("未知模块: " + moduleName + "，可用模块: " + MODULES.keySet());
            return;
        }

        String entityName = toCamelCase(config.tableName, true);
        String modulePackage = config.packageName;

        System.out.println("=== 开始生成模块: " + moduleName + " ===");
        System.out.println("实体名: " + entityName);
        System.out.println("包名: " + modulePackage);
        System.out.println("表名: " + config.tableName);
        System.out.println();

        // Step 1: 使用 MyBatis-Plus Generator 生成 DO + Mapper（2 个文件）
        generateDoAndMapper(config, modulePackage);

        // Step 2: 生成其余 9 个文件（基于 Freemarker 模板）
        generateCustomFiles(moduleName, entityName, modulePackage, config.tableComment);

        System.out.println();
        System.out.println("=== 代码生成完成！模块: " + moduleName + "，输出目录: " + SOURCE_DIR + " ===");
    }

    /**
     * Step 1: 使用 MyBatis-Plus Generator 生成 DO + Mapper。
     */
    private static void generateDoAndMapper(ModuleConfig config, String modulePackage) {
        System.out.println("[Step 1] 生成 DO + Mapper（MyBatis-Plus Generator）...");

        FastAutoGenerator.create(DATABASE_URL, USERNAME, PASSWORD)
                .globalConfig(builder -> builder
                        .author("CodeGenerator")
                        .outputDir(SOURCE_DIR)
                        .dateType(DateType.TIME_PACK)
                        .disableOpenDir())
                .packageConfig(builder -> builder
                        .parent(modulePackage + ".internal")
                        .entity("entity")
                        .mapper("mapper")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, "")))
                .strategyConfig(builder -> builder
                        .addInclude(config.tableName)
                        .addTablePrefix("")
                        .entityBuilder()
                        .superClass(BaseDO.class)
                        .naming(NamingStrategy.underline_to_camel)
                        .columnNaming(NamingStrategy.underline_to_camel)
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .addSuperEntityColumns("id", "create_time", "update_time",
                                "create_user", "update_user", "delete_time", "delete_user")
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

        System.out.println("[Step 1] 完成：DO + Mapper 已生成");
    }

    /**
     * Step 2: 基于 Freemarker 模板生成 9 个自定义文件。
     */
    private static void generateCustomFiles(String moduleName, String entityName,
                                            String modulePackage, String tableComment) {
        System.out.println("[Step 2] 生成 9 个自定义文件（Freemarker 模板）...");

        Configuration freemarkerConfig = createFreemarkerConfig();

        String internalPackage = modulePackage + ".internal";
        String packagePath = modulePackage.replace('.', '/');
        String internalPath = internalPackage.replace('.', '/');

        // 构建模板数据模型
        Map<String, Object> dataModel = new LinkedHashMap<>();
        dataModel.put("modulePackage", modulePackage);
        dataModel.put("internalPackage", internalPackage);
        dataModel.put("entityName", entityName);
        dataModel.put("moduleName", moduleName);
        dataModel.put("tableComment", tableComment);
        dataModel.put("columns", Collections.emptyList()); // 列信息需要从数据库读取，模板中使用占位符

        // 定义要生成的文件：模板名 → (输出目录, 文件名)
        // 保持插入顺序
        Map<String, FileSpec> files = new LinkedHashMap<>();
        files.put("model.java.ftl",           new FileSpec(internalPath, entityName + ".java"));
        files.put("repository.java.ftl",      new FileSpec(internalPath, entityName + "Repository.java"));
        files.put("repositoryImpl.java.ftl",  new FileSpec(internalPath, entityName + "RepositoryImpl.java"));
        files.put("converter.java.ftl",       new FileSpec(internalPath, entityName + "Converter.java"));
        files.put("service.java.ftl",         new FileSpec(internalPath, entityName + "Service.java"));
        files.put("facadeImpl.java.ftl",      new FileSpec(internalPath, entityName + "FacadeImpl.java"));
        files.put("controller.java.ftl",      new FileSpec(internalPath, entityName + "Controller.java"));
        files.put("vo.java.ftl",              new FileSpec(internalPath, entityName + "VO.java"));
        files.put("facade.java.ftl",          new FileSpec(packagePath,  entityName + "Facade.java"));

        int generated = 0;
        int skipped = 0;

        for (Map.Entry<String, FileSpec> entry : files.entrySet()) {
            String templateName = entry.getKey();
            FileSpec spec = entry.getValue();
            Path outputPath = Paths.get(SOURCE_DIR, spec.directory, spec.fileName);

            if (Files.exists(outputPath)) {
                System.out.println("  跳过已存在的文件: " + outputPath.getFileName());
                skipped++;
                continue;
            }

            try {
                // 确保输出目录存在
                Files.createDirectories(outputPath.getParent());

                // 使用 Freemarker 渲染模板
                Template template = freemarkerConfig.getTemplate(templateName);
                StringWriter writer = new StringWriter();
                template.process(dataModel, writer);

                // 写入文件
                Files.writeString(outputPath, writer.toString(), StandardCharsets.UTF_8);
                System.out.println("  生成: " + outputPath.getFileName());
                generated++;
            } catch (IOException | TemplateException e) {
                System.err.println("  错误: 生成 " + templateName + " 失败: " + e.getMessage());
            }
        }

        System.out.println("[Step 2] 完成：生成 " + generated + " 个文件，跳过 " + skipped + " 个已存在文件");
    }

    /**
     * 创建 Freemarker 配置（从 classpath templates/ 目录加载模板）。
     */
    private static Configuration createFreemarkerConfig() {
        try {
            Path templatesDir = Paths.get(System.getProperty("user.dir"),
                    "app/src/main/resources/templates");

            if (!Files.isDirectory(templatesDir)) {
                throw new IllegalStateException("模板目录不存在: " + templatesDir);
            }

            Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
            cfg.setDirectoryForTemplateLoading(templatesDir.toFile());
            cfg.setDefaultEncoding("UTF-8");
            cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            cfg.setLogTemplateExceptions(false);
            return cfg;
        } catch (IOException e) {
            throw new IllegalStateException("无法初始化 Freemarker 配置", e);
        }
    }

    private static String parseModuleName(String[] args) {
        if (args.length == 0) return "systemconfig";
        String first = args[0];
        if (first.startsWith("--module=")) return first.substring("--module=".length());
        if (first.startsWith("--")) return first.substring(2);
        return first;
    }

    private static String toCamelCase(String underlineName, boolean capitalize) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = capitalize;
        for (char c : underlineName.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    private record ModuleConfig(String packageName, String tableName, String tableComment) {}
    private record FileSpec(String directory, String fileName) {}
}
