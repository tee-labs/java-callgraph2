package test.config;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;

/**
 * @author adrninistrator
 * @date 2026/10/28
 * @description: 配置文件缺失时回落到代码中默认值的测试
 */
public class TestConfigMissingFile {

    private File tempConfigDir;

    @Before
    public void init() throws IOException {
        // 创建空的临时目录，作为配置文件根目录
        tempConfigDir = Files.createTempDirectory("javacg2_missing_config_").toFile();
    }

    @After
    public void destroy() {
        // 删除空的临时目录
        Assert.assertTrue(tempConfigDir.delete());
    }

    @Test
    public void testConfigMissingFile() {
        // 使用配置文件模式，但配置文件根目录中不存在任何文件
        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper(false, tempConfigDir.getAbsolutePath());

        // 主要配置使用代码中默认值
        Assert.assertEquals(Boolean.TRUE, javaCG2ConfigureWrapper.getMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE));
        Assert.assertEquals(Integer.valueOf(8), javaCG2ConfigureWrapper.getMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION));

        // Set格式的其他配置使用枚举中定义的默认值
        Set<String> frEqConversionMethodSet = javaCG2ConfigureWrapper.getOtherConfigSet(JavaCG2OtherConfigFileUseSetEnum.OCFUSE_FR_EQ_CONVERSION_METHOD);
        Assert.assertEquals(44, frEqConversionMethodSet.size());
        Assert.assertTrue(frEqConversionMethodSet.contains("java.lang.Boolean:valueOf=1"));

        // List格式的其他配置使用空的List
        List<String> jarDirList = javaCG2ConfigureWrapper.getOtherConfigList(JavaCG2OtherConfigFileUseListEnum.OCFULE_JAR_DIR);
        Assert.assertTrue(jarDirList.isEmpty());

        // 表达式配置使用枚举中定义的默认值
        Assert.assertEquals("string.endsWithAny(full_method, 'hashCode()', 'equals(java.lang.Object)')",
                javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_METHOD));
        Assert.assertTrue(StringUtils.isBlank(javaCG2ConfigureWrapper.getElConfigText(JavaCG2ElConfigEnum.ECE_MERGE_FILE_IGNORE_JAR_IN_DIR)));
    }
}
