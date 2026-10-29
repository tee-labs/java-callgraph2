# AGENTS.md

## 项目简介

java-callgraph2：对 Java 编译产物（class/jar/war/jmod）做静态分析，生成方法调用关系、类型/常量值、继承实现、Spring、泛型、Lambda 等结构化结果文件。fork 自 gousiosg/java-callgraph，已大幅改造。纯库组件，无命令行入口；下游项目为 java-all-call-graph。

- 构建：Maven（已从 Gradle 迁移），Java 8 语言级别，UTF-8 编码
- 关键依赖：Apache BCEL 6.10.0（字节码解析）、Aviator（EL 表达式）、Lombok、slf4j-api
- 代码位于 `src/main/java/com/adrninistrator/javacg2/`，类名统一 `JavaCG2` 前缀；测试位于 `src/test/java/test/`

## 常用命令

```shell
mvn test                                      # 全量测试（约10秒）
mvn package -DskipTests -Ptest_gen_jar        # 生成 target/test.jar（测试用解析输入；mvn test 时 antrun 也会自动生成）
mvn package -Pgen_run_jar -DskipTests         # 生成 jar_output_dir/jar/run_javacg2.jar 及 lib/
mvn package -DskipTests -Pgen_run_jar,gen_jar_in_jar   # 生成 target/jar_output.jar
mvn clean deploy -Prelease                    # 发布到 Maven Central（需 ossrh 凭据）
```

无独立 lint/typecheck；编译警告已开启 `-Xlint:unchecked/-Xlint:deprecation`，构建即检查。

## 架构要点

- 入口：`entry/JavaCG2Entry`，经 `builder/JavaCG2EntryBuilder` 构建；实例只能 `run()` 一次
- 配置：仅支持代码方式（4.1.0 起已移除配置文件支持）。两种方式：
  - `conf/JavaCG2ConfigureWrapper`（setMainConfig / setOtherConfigList / setOtherConfigSet / setElConfigText）
  - `builder/` 下的链式 Builder（JavaCG2ConfigBuilder、JavaCG2JarDirBuilder 等）
- 配置默认值全部定义在枚举中：`conf/enums/JavaCG2ConfigKeyEnum`、`JavaCG2OtherConfigFileUseListEnum`、`JavaCG2OtherConfigFileUseSetEnum`、`el/enums/JavaCG2ElConfigEnum`。新增配置参数 = 新增枚举常量，不要引入配置文件
- 主要包职责：`parser/`（jar 条目解析）、`handler/`（类/方法/指令处理核心）、`el/`（Aviator 忽略表达式）、`spring/`（Spring Bean/AOP/XML）、`writer/`（记录级 RecordSink 输出，结果文件经 OutputAdapter 创建）、`extensions/`（对外扩展接口）、`dto/`、`util/`
- 输出写到输出根目录（测试用 `out/test`），扩展名可配（测试默认 `.md`）

## 编码约定

- 注释、Javadoc、README、docs 均使用中文；提交信息用中文 Conventional Commits（如 `refactor(config)!: 移除...`），破坏性变更加 `!`
- 使用 Lombok（@Getter/@Setter/@Slf4j 等），不要手写 getter/setter 与 logger 样板
- 主代码只用 slf4j-api；log4j2 仅测试 scope
- Java 8 语法，不要用 Java 9+ 特性

## 测试与已知坑

- 测试基类 `test.base.TestBase`：以 `target/test.jar`（即本项目测试类打成的 jar）作为解析输入；部分测试依赖兄弟目录的 java-all-call-graph 工程或系统属性 `javacg2.test.jacg.dir`，缺失时通过 `AssumptionViolatedException` 跳过而非报错——新增测试请沿用该模式，不要硬依赖环境
- surefire 复用同一 JVM（forkCount=1, reuseForks=true），全量测试才快；流未关闭（尤其 zip4j）会在 Windows 下锁文件导致卡死，资源一律 try-with-resources
- BCEL 不要升到 6.7.0（`LDC.getValue()` 报错），当前用 6.10.0；升级需回归测试
- surefire 排除 `**/runlocal/**`；`src/test/java/test/temp/` 是未跟踪的草稿区，勿提交
- `target/`、`build/`、`out/`、`jar_output_dir/`、`log_javacg2/` 均为生成产物，已在 .gitignore

## 参考文档（改动前建议先读）

- `docs/file_desc.md`：可生成的结果文件清单
- `docs/file_format.md`：结果文件格式
- `docs/call_type.md`：方法调用类型
- `docs/_javacg2_all_config.md`：全部配置参数说明（注意：其中部分「配置文件」描述已过时，配置只能走代码/Builder）
- `src/main/resources/_el_example/el_usage.md`、`el_usage_javacg2.md`、`string_compare.md`：EL 表达式用法（改 `el/` 包前必读）
- `README.md`：完整使用说明（约 66KB，按章节查阅）
