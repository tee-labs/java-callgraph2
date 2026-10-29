package test.config;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.entry.JavaCG2Entry;
import com.adrninistrator.javacg2.exceptions.JavaCG2RuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import test.base.TestBase;

/**
 * @author adrninistrator
 * @date 2023/7/16
 * @description:
 */
@Slf4j
public class TestAllConfig extends TestBase {

    public static final String[] BOOLEAN_VALUES = new String[]{Boolean.TRUE.toString(), Boolean.FALSE.toString()};

    @Test
    public void test() {
        long startTime = System.currentTimeMillis();
        int times = 0;
        for (String parseMethodCallTypeValue : BOOLEAN_VALUES) {
            for (String firstParseInitMethodType : BOOLEAN_VALUES) {
                for (String analyseFieldRelationship : BOOLEAN_VALUES) {
                    for (String continueWhenError : BOOLEAN_VALUES) {
                        times++;
                        log.info("执行次数 {}", times);

                        if (!Boolean.parseBoolean(parseMethodCallTypeValue) &&
                                (Boolean.parseBoolean(firstParseInitMethodType) || Boolean.parseBoolean(analyseFieldRelationship))) {
                            // 非法配置，跳过
                            continue;
                        }

                        JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = genJavaCG2ConfigureWrapper4JACGTest();
                        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_PARSE_METHOD_CALL_TYPE_VALUE, parseMethodCallTypeValue);
                        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_FIRST_PARSE_INIT_METHOD_TYPE, firstParseInitMethodType);
                        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_ANALYSE_FIELD_RELATIONSHIP, analyseFieldRelationship);
                        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_CONTINUE_WHEN_ERROR, continueWhenError);

                        JavaCG2Entry javaCG2Entry = new JavaCG2Entry(javaCG2ConfigureWrapper);
                        if (!javaCG2Entry.run()) {
                            throw new JavaCG2RuntimeException("失败");
                        }
                    }
                }
            }
        }
        log.info("执行耗时 {} ms", (System.currentTimeMillis() - startTime));
    }
}
