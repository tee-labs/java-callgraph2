package test.config.invalid;

import com.adrninistrator.javacg2.common.enums.JavaCG2CalleeRawActualEnum;
import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.exceptions.JavaCG2ConfigException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author adrninistrator
 * @date 2026/5/24
 * @description: 验证各配置参数对于合法及非法值赋值的处理是否正确
 */
@Slf4j
public class TestConfigInvalid {

    private JavaCG2ConfigureWrapper javaCG2ConfigureWrapper;

    @Before
    public void init() {
        javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();
    }

    // 测试 CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL 合法值
    @Test
    public void testHandleCalleeNewRawActual_Valid() {
        for (JavaCG2CalleeRawActualEnum calleeRawActualEnum : JavaCG2CalleeRawActualEnum.values()) {
            javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL, calleeRawActualEnum.getType());
        }
    }

    // 测试 CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL 非法值
    @Test
    public void testHandleCalleeNewRawActual_Invalid() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_NEW_RAW_ACTUAL, "invalid_value"));
        log.error("error ", e);
    }

    // 测试 CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL 合法值
    @Test
    public void testHandleCalleeSpringBeanRawActual_Valid() {
        for (JavaCG2CalleeRawActualEnum calleeRawActualEnum : JavaCG2CalleeRawActualEnum.values()) {
            javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL, calleeRawActualEnum.getType());
        }
    }

    // 测试 CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL 非法值
    @Test
    public void testHandleCalleeSpringBeanRawActual_Invalid() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_HANDLE_CALLEE_SPRING_BEAN_RAW_ACTUAL, "invalid_value"));
        log.error("error ", e);
    }

    // 测试 Boolean 类型参数合法值
    @Test
    public void testBooleanValid() {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, "true");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, "false");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, "TRUE");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, "FALSE");
    }

    // 测试 Boolean 类型参数非法值
    @Test
    public void testBooleanInvalid() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, "yes"));
        log.error("error ", e);
    }

    // 测试 Integer 类型参数合法值
    @Test
    public void testIntegerValid() {
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, "8");
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, "17");
    }

    // 测试 Integer 类型参数非法值
    @Test
    public void testIntegerInvalid() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, "abc"));
        log.error("error ", e);
    }

    // 测试参数不允许为null
    @Test
    public void testNullValue() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, null));
        log.error("error ", e);
    }

    // 测试不允许为空的参数传入空字符串
    @Test
    public void testNotBlankEmptyValue() {
        JavaCG2ConfigException e = Assert.assertThrows(JavaCG2ConfigException.class,
                () -> javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_JDK_RUNTIME_MAJOR_VERSION, ""));
        log.error("error ", e);
    }
}
