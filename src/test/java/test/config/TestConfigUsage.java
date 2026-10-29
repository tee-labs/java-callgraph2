package test.config;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseListEnum;
import com.adrninistrator.javacg2.conf.enums.JavaCG2OtherConfigFileUseSetEnum;
import com.adrninistrator.javacg2.conf.enums.interfaces.ConfigInterface;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author adrninistrator
 * @date 2025/6/2
 * @description:
 */
@Slf4j
public class TestConfigUsage {

    private final JavaCG2ConfigureWrapper javaCG2ConfigureWrapper = new JavaCG2ConfigureWrapper();

    @Test
    public void test() {
        doTest(JavaCG2ElConfigEnum.values());
        doTest(JavaCG2ConfigKeyEnum.values());
        doTest(JavaCG2OtherConfigFileUseListEnum.values());
        doTest(JavaCG2OtherConfigFileUseSetEnum.values());
    }

    private void doTest(ConfigInterface[] configInterfaces) {
        for (ConfigInterface configInterface : configInterfaces) {
            String usage = configInterface.genConfigUsage();
            log.info("### {}", usage);
        }
    }
}
