# 1. 下游迁移说明（java-all-call-graph 维护者视角）

本文面向从旧上游 `com.github.adrninistrator:java-callgraph2:4.1.0` 迁移到当前 fork `io.github.mccxj:java-callgraph2` 的下游项目（主要是 java-all-call-graph，下称 jacg）的维护者，说明结果文件列结构差异、正确的读回方式、表头参数行为、列结构导出 API、配置方式变化与嵌入式内存模式。

本文内容均以当前代码实际行为为准核对（列结构以 `common/enums/JavaCG2OutPutFileTypeEnum` 枚举常量上定义的 `common/JavaCG2OutputFileSchema` 为单一来源），不以上游过时文档为准。

## 1.1. Maven 坐标与版本切换

|内容|迁移前|迁移后|
|---|---|---|
|groupId|com.github.adrninistrator|io.github.mccxj|
|artifactId|java-callgraph2|java-callgraph2（不变）|
|版本|4.1.0|1.1.x（已发布至 Maven Central）；本文描述的列结构 schema、表头参数与按列名读回 API 属于 1.2.0（未发布），升级到 1.2.0 后可用|

依赖写法示例：

```xml
<dependency>
    <groupId>io.github.mccxj</groupId>
    <artifactId>java-callgraph2</artifactId>
    <version>1.2.0</version>
</dependency>
```

包名未变（`com.adrninistrator.javacg2.*`），jacg 现有代码引用的既有类与枚举常量（如 `JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION`）全部保留；入口类 `new JavaCG2Entry(JavaCG2ConfigureWrapper)` 用法不变。本 fork 在 4.1.0 基础上新增了 builder、schema、writer 读回框架、Jar 兼容性检查、JarDiff 等类，移除了 `conf/writer/*`（`JavaCG2ConfigWriter` 等）。

## 1.2. 结果文件列结构差异对照

### 1.2.1. 注解类文件（必改）

4 个注解类文件在本 fork 中的列结构为「前缀列 + 注解类名 +（注解属性名称 + 注解属性值）」：注解**属性**只有 **2 列（属性名称、属性值）**，没有「注解属性类型」列；注解没有属性值时，省略最后两列（属性名称、属性值），因此行宽可变，最小列数为全列数减 2：

|文件|前缀列|全列数|最小列数|
|---|---|---|---|
|class_annotation|完整类名、注解类名|4|2|
|field_annotation|完整类名、字段名称、注解类名|5|3|
|method_annotation|完整方法、方法返回类型、方法所在的jar文件序号、注解类名|6|4|
|method_arg_annotation|完整方法、方法返回类型、参数序号、注解类名|6|4|

写出代码为 `util/JavaCG2AnnotationUtil#writeAnnotationInfo`：注解没有属性值时写「前缀列 + 注解类名」，有属性值时对每个属性写「前缀列 + 注解类名 + 属性名称 + 格式化后的属性值」，每行一个属性。

jacg 现行读取逻辑按「名称/类型/值」3 列属性处理（`handler/writedb/WriteDbHandler4ClassAnnotation` 等 4 个类的 `genData` 方法，列数常量为 `JACGConstants.ANNOTATION_COLUMN_NUM_WITHOUT_ATTRIBUTE_2/3/4` 与 `ANNOTATION_COLUMN_NUM_WITH_ATTRIBUTE_5/6/7`）。对 4 列的 class_annotation 行，其判断条件 `array.length > 2` 成立后会继续读「属性类型」与「属性值」两列，导致：

- 「注解属性类型」读到的实际是属性值；
- 「注解属性值」越界读到下一数据行的首列（最后一行时读到 null），后续所有行整体错位。

因此迁移时**必须**将 4 个注解文件的读取改为按 2 列属性处理。

需要说明的是，经与代码核对，上游 4.1.0 的二进制实际写出的属性列同样只有「名称/值」2 列（其官方文档 docs/file_format.md 中「注解属性类型」列的描述已过时，jacg 的读取逻辑保留了 3 列处理）。即该列结构差异的实质是 **jacg 读取逻辑与实际写出结构不匹配**，本 fork 通过 schema 将真实结构固化为单一来源；按 2 列属性读取后，解析上游 4.1.0 与本 fork 的输出均正确。此外 method_annotation 文件中「方法所在的jar文件序号」列位于第 3 列（前缀列第 3 项，注解类名之前），上游文档中该列位于最后属同样过时，jacg 现行读取顺序（完整方法 → 方法返回类型 → jar文件序号）与本 fork 一致，无需调整。

### 1.2.2. 行宽可变的 lambda_method_info

lambda_method_info 在未找到下一个被调用方法时省略最后一列「下一个被调用完整方法」，行为与上游 4.1.0 一致；本 fork 通过 schema 的最小列数（minColumnNum=2）将该行为明确化。按列下标读取最后一列前需先判断列数。

### 1.2.3. 与 4.1.0 结构一致的文件（已逐项核对）

以下近期有演进的文件经与 schema 逐列核对，列数、列顺序与 4.1.0 实际写出一致，jacg 按列下标读取无需改动：

- method_call（13 列）：与 4.1.0 完全一致。仅表头文本的列名表述规范化——第 4 列「(方法调用类型)被调用方，完整方法」→「方法调用类型及被调用方完整方法」，第 7 列「被调用方，对象数组的维度」→「被调用对象数组的维度」；数据格式与列顺序不变，且默认不输出表头（见 1.4），按列下标读不受影响
- method_call_info（12 列）：完全一致
- method_call_field_actual_type（5 列）、spring_di_field（4 列）：4.1.0 新增的两个文件，本 fork 保持一致
- spring_bean（7 列）、spring_scan_package_java（4 列）、spring_aop_aspect_java（2 列）、spring_aop_pointcut_java（3 列）、spring_aop_advice_java（6 列）：一致
- class_info（5 列）、method_info（8 列）、field_info（13 列）、jar_info（4 列）、extends_impl（4 列）等基础文件：一致

其余文件（class_reference、dup_*、enum_init_*、field_generics_type、field_relationship、field_usage_other、get/set_method、inner_class、javacg2_config、lambda_method_info、method_arg_generics_type、method_argument、method_call_method_call_return、method_call_static_field、method_call_non_static_field、method_call_static_field_mcr、method_call_raw_callee、method_catch、method_finally、method_line_number、method_return_*、method_throw、package_info、sf_field_method_call、class_ext_impl_generics_type、class_signature_generics_type 等）亦均与 4.1.0 的实际写出结构一致，未做列结构变更。全量列定义见 [生成文件格式](file_format.md)。

### 1.2.4. 本 fork 新增输出的文件

以下结果文件在本 fork 中由组件原生写出，上游 4.1.0 不生成（jacg 此前按需自行生成其中部分数据，迁移后可直接消费组件原生产物，已存在的文件对未注册读取逻辑的下游无影响）：

|文件|说明|列数|
|---|---|---|
|spring_controller|Spring Controller 信息（由方法注解派生）|13|
|spring_task_java|Spring 定时任务信息，通过 Java 代码注解定义|9|
|spring_task_xml|Spring 定时任务信息，通过 XML 定义|3|
|spring_aop_aspect_xml|Spring AOP aspect 信息，在 XML 中定义|4|
|spring_aop_pointcut_xml|Spring AOP pointcut 信息，在 XML 中定义|4|
|spring_aop_advice_xml|Spring AOP advice 信息，在 XML 中定义|8|
|spring_scan_package_xml|Spring 的包扫描路径，在 XML 文件中定义|4|
|compatibility_class_reference_missing|Jar 兼容性检查-被引用的类不存在|5|
|compatibility_method_call_missing|Jar 兼容性检查-方法调用中的被调用类/方法不存在或不可访问|10|
|compatibility_field_usage_missing|Jar 兼容性检查-被使用的字段不存在或不可访问|10|
|compatibility_dup_class_conflict|Jar 兼容性检查-重复同名类冲突|11|

各文件的完整列名与顺序参考 [生成文件格式](file_format.md)，也可通过 1.5 的 schema 导出 API 获得。

## 1.3. 正确读回方式（Java 8）

### 1.3.1. 方式一（推荐）：JavaCG2RecordSource + 按列名取列

`writer/JavaCG2RecordSource` 按结果文件类型定位一个结果文件的数据行，`getSchema()`（1.2.0 新增）返回该文件对应的列结构。数据行的表头行由实现**在内部跳过**，调用方无需关心 `output.file.header.enable` 的取值：

```java
JavaCG2RecordSource recordSource = new JavaCG2FileRecordSource(outputDirPath, outputFileExt,
        JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_ANNOTATION);
JavaCG2OutputFileSchema schema = recordSource.getSchema();
if (schema == null) {
    // 文件不存在、扩展文件或未定义列结构的文件类型返回null，只能按列下标读取
    return;
}
int classNameIndex = schema.getColumnIndex("完整类名");
int annotationNameIndex = schema.getColumnIndex("注解类名");
int attributeNameIndex = schema.getColumnIndex("注解属性名称");
int attributeValueIndex = schema.getColumnIndex("注解属性值");

try (BufferedReader reader = recordSource.genBufferedReader()) {
    String line;
    while ((line = reader.readLine()) != null) {
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        // 校验行宽，注解类文件在注解没有属性值时省略最后两列，允许的列数为最小列数~列数
        if (!schema.checkColumnNumValid(columns.length)) {
            continue;
        }
        String className = columns[classNameIndex];
        String annotationName = columns[annotationNameIndex];
        // 省略的可选列视为空字符串
        String attributeName = columns.length > attributeNameIndex ? columns[attributeNameIndex] : "";
        String attributeValue = columns.length > attributeValueIndex ? columns[attributeValueIndex] : "";
        // ...按列名对应的数据处理
    }
}
```

说明：

- `getColumnIndex` 列名不存在时返回 -1，建议对取得的下标做一次缓存或校验
- 内存模式下（`JavaCG2MemoryOutputAdapter`）通过 `genRecordSource(fileTypeEnum)`、`genOtherRecordSource(fileName)` 获得的数据来源用法相同
- 经 `genOtherRecordSource(fileName)` 读回的扩展文件（文件名未在枚举中定义列结构）`getSchema()` 返回 null

### 1.3.2. 方式二：自写裸读器（按 schema 跳表头 + 校验列数）

不使用 RecordSource、直接逐行读结果文件时，按 schema 判断并跳过表头行、校验列数后按列名取列：

```java
JavaCG2OutputFileSchema schema = JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL.getSchema();
int calleeIndex = schema.getColumnIndex("方法调用类型及被调用方完整方法");
int lineNumIndex = schema.getColumnIndex("调用方法源代码行号");

try (BufferedReader reader = JavaCG2FileUtil.genBufferedReader(filePath)) {
    String line;
    boolean firstLine = true;
    while ((line = reader.readLine()) != null) {
        if (firstLine) {
            firstLine = false;
            if (schema.isHeaderLine(line)) {
                // 开启output.file.header.enable时首行为表头行，跳过；关闭时首行即数据行，不回退
                continue;
            }
        }
        String[] columns = StringUtils.splitPreserveAllTokens(line, '\t');
        if (!schema.checkColumnNumValid(columns.length)) {
            continue;
        }
        String calleeFullMethod = columns[calleeIndex];
        String srcLine = columns[lineNumIndex];
        // ...
    }
}
```

### 1.3.3. 两种方式与旧行为的关系

- 默认配置下（表头关闭，见 1.4），结果文件与上游 4.1.0 完全兼容（无表头行），jacg 现有按列下标的裸读逻辑只需按 1.2.1 修正注解文件属性列数
- 开启表头后，方式一无需任何改动；方式二必须包含表头行跳过逻辑，否则首个数据行会被当作表头丢失（或表头行被当作数据行）

## 1.4. 表头参数行为

- 配置参数 `output.file.header.enable`（`JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE`），**默认 false**，默认行为与上游 4.1.0 一致（所有行均为数据行）
- 设置为 true 时，定义了列结构且允许表头的结果文件在首行输出表头，内容为各列名称（与 schema 列名一致），以 tab 分隔
- 以下文件**不输出表头**：
  - jar_info：文件中除数据行外还写入解析结果保存目录、最终解析的 jar 文件路径等特殊行（schema 的 headerSupported=false，即使开启参数也不输出）
  - 扩展机制生成的、未在枚举中定义列结构的其他类型文件
  - _log_method_spend_time 等无固定列结构的文件
- 组件内部读回逻辑（Jar 兼容性检查、JarDiff 比较）经 RecordSource 读回，表头行自动跳过

设置方式：

```java
// 通过 JavaCG2ConfigureWrapper
javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_OUTPUT_FILE_HEADER_ENABLE, Boolean.TRUE.toString());

// 或通过 JavaCG2ConfigBuilder
JavaCG2ConfigBuilder.builder().setOutputFileHeaderEnable(true).build();
```

## 1.5. 列结构导出 API（用于生成/校验列映射）

`common/JavaCG2OutputFileSchemaExporter` 遍历全部文件类型枚举，为每个定义了列结构的文件类型生成结构化信息与 markdown 文档，可用于下游**生成或校验列映射**（例如在 jacg 中按版本断言列数与列名未漂移）：

```java
JavaCG2OutputFileSchemaExporter exporter = new JavaCG2OutputFileSchemaExporter();

// 结构化信息：文件名、中文描述、列数、最小列数、是否允许表头、带下标的列名列表
List<JavaCG2OutputFileSchemaInfo> schemaInfoList = exporter.genSchemaInfoList();
for (JavaCG2OutputFileSchemaInfo schemaInfo : schemaInfoList) {
    if ("class_annotation".equals(schemaInfo.getFileName())) {
        // 与 jacg 读取逻辑声明的列名列表比对，不一致时使校验失败
        Assert.assertEquals(4, schemaInfo.getColumnNum());
        Assert.assertEquals(2, schemaInfo.getMinColumnNum());
    }
}

// 或生成全量 markdown 文档（与docs/file_format.md中的列结构一致）
exporter.writeToFile("javacg2_schema.md");
```

单个文件类型也可直接使用枚举上的 schema：`JavaCG2OutPutFileTypeEnum.OPFTE_METHOD_CALL.getSchema()` 提供 `getColumnNum`、`getMinColumnNum`、`isHeaderSupported`、`getColumnIndex(列名)`、`getColumnName(下标)`、`checkColumnNumValid(列数)`、`isHeaderLine(行内容)`、`genHeaderLine()`。

## 1.6. 配置方式变化提示

- 配置文件已移除（自 fork 基线 4.2.0 起，发布物中不再内置配置文件，`conf/writer/JavaCG2ConfigWriter` 等生成配置文件的类也已移除）：仅支持代码方式配置，即 `conf/JavaCG2ConfigureWrapper`（setMainConfig / setOtherConfigList / setOtherConfigSet / setElConfigText）或 `builder/` 下的链式 Builder（JavaCG2ConfigBuilder、JavaCG2JarDirBuilder、JavaCG2EntryBuilder 等）
- 配置参数默认值全部定义在枚举中（`JavaCG2ConfigKeyEnum`、`JavaCG2OtherConfigFileUseListEnum`、`JavaCG2OtherConfigFileUseSetEnum`、`el/enums/JavaCG2ElConfigEnum`），新增配置参数不会再引入配置文件
- 表达式开关 Builder（JavaCG2ParseSwitchBuilder 等，1.1.0 破坏性变更）仅保留原始 Aviator 表达式文本设置入口，语义化方法已移除；并新增了包含（白名单）表达式，详见 [更新说明](change_log.md)

## 1.7. 嵌入式内存模式简介

本 fork 提供 `writer/JavaCG2MemoryOutputAdapter` 纯内存输出模式：解析结果不落盘，数据在内存中供同进程直接消费，不依赖 `output.root.path`（参数未设置或无效均不影响 `JavaCG2Entry.run()` 成功）：

```java
JavaCG2ConfigureWrapper configureWrapper = new JavaCG2ConfigureWrapper();
// ...按代码方式设置参数
JavaCG2Entry javaCG2Entry = new JavaCG2Entry(configureWrapper);
JavaCG2MemoryOutputAdapter memoryOutputAdapter = new JavaCG2MemoryOutputAdapter();
javaCG2Entry.setOutputAdapter(memoryOutputAdapter);
javaCG2Entry.run();

// 按结果文件类型读回（数据来源为内存中收集的内容，格式与写入结果文件一致）
try (BufferedReader reader = memoryOutputAdapter.genRecordSource(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO).genBufferedReader()) {
    // ...
}
// 或直接获取某结果文件的全部内容（包含表头行，如有）
String classInfoContent = memoryOutputAdapter.getMainRecordContent(JavaCG2OutPutFileTypeEnum.OPFTE_CLASS_INFO);
```

构造 `JavaCG2MemoryOutputAdapter(true)` 可在内存数据的首行包含表头行，写出规则与文件实现一致（含 jar_info 不输出表头的规则）；经 `genRecordSource` 读回时表头行同样自动跳过。

## 1.8. 迁移核对清单

1. Maven 坐标切换为 io.github.mccxj:java-callgraph2:1.2.0（含 schema、表头、RecordSource 读回 API）
2. 4 个注解类文件的读取逻辑改为按「注解属性名称 + 注解属性值」2 列属性处理（见 1.2.1）
3. lambda_method_info 等行宽可变文件按最小列数判断可缺省列（见 1.2.2）
4. 若开启 output.file.header.enable：裸读器增加表头行跳过（见 1.3.2），或改用 RecordSource 按列名读取（见 1.3.1）
5. 如需防列结构漂移，接入 JavaCG2OutputFileSchemaExporter 在测试中校验列映射（见 1.5）
6. 将原通过配置文件/启动参数的配置改为代码方式（见 1.6）
7. 按需接入新增的原生结果文件（spring_controller、spring_task_*、spring_aop_*_xml、spring_scan_package_xml、compatibility_*，见 1.2.4）与嵌入式内存模式（见 1.7）
