package golden.lambda;

import golden.dto.GoldenDto;
import golden.holder.GoldenStaticHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 黄金基线夹具：Lambda表达式，覆盖Lambda方法信息的识别（含Lambda中调用的下一个方法）与方法引用
 */
public class GoldenLambdaService {

    /**
     * Lambda表达式调用其他对象的方法
     */
    public List<String> mapNames(List<GoldenDto> dtoList) {
        List<String> nameList = new ArrayList<>();
        Function<GoldenDto, String> nameGetter = dto -> dto.getName();
        for (GoldenDto dto : dtoList) {
            nameList.add(nameGetter.apply(dto));
        }
        return nameList;
    }

    /**
     * Lambda表达式作为参数传入并被执行
     */
    public void runByLambda(Runnable task) {
        GoldenStaticHolder.increaseCall();
        task.run();
    }

    /**
     * Lambda表达式调用本类方法，覆盖Lambda方法信息中记录的下一个被调用方法
     */
    public String lambdaCallingOwnMethod() {
        Supplier<String> supplier = () -> transform("lambda");
        return supplier.get();
    }

    /**
     * 方法引用静态方法
     */
    public Runnable methodRef() {
        return GoldenStaticHolder::increaseCall;
    }

    /**
     * 被Lambda表达式调用的本类方法
     */
    public String transform(String input) {
        return "transformed:" + input;
    }
}
