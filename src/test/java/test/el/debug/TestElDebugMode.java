package test.el.debug;

import com.adrninistrator.javacg2.conf.JavaCG2ConfigureWrapper;
import com.adrninistrator.javacg2.conf.enums.JavaCG2ConfigKeyEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElAllowedVariableEnum;
import com.adrninistrator.javacg2.el.enums.JavaCG2ElConfigEnum;
import org.junit.Test;
import test.base.TestElBase;

/**
 * @author adrninistrator
 * @date 2026/10/29
 * @description: 表达式执行调试模式的测试
 * 其他表达式测试均不开启调试模式（每条表达式执行都会输出调试日志，方法调用级别的表达式会执行数千次，过于耗时）
 */
public class TestElDebugMode extends TestElBase {
    @Override
    protected JavaCG2ElConfigEnum chooseElConfigEnum() {
        return JavaCG2ElConfigEnum.ECE_PARSE_IGNORE_CLASS;
    }

    @Override
    protected String chooseElText() {
        return JavaCG2ElAllowedVariableEnum.EAVE_PARSE_CLASS_NAME.getVariableName() + " != 'test.callgraph.annotation.CallMethodWithAnnotation'";
    }

    @Override
    protected String chooseTitle() {
        return "表达式执行调试模式";
    }

    @Override
    protected String chooseDesc() {
        return "开启调试模式后，表达式每次执行都会在日志中输出详细信息";
    }

    @Override
    protected void setConfig4ElText(JavaCG2ConfigureWrapper javaCG2ConfigureWrapper) {
        super.setConfig4ElText(javaCG2ConfigureWrapper);
        javaCG2ConfigureWrapper.setMainConfig(JavaCG2ConfigKeyEnum.CKE_EL_DEBUG_MODE, Boolean.TRUE.toString());
    }

    @Test
    public void test() {
        run(null);
    }
}
