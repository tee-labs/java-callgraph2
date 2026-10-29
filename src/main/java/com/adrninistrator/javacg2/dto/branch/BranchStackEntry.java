package com.adrninistrator.javacg2.dto.branch;

import com.adrninistrator.javacg2.dto.frame.FrameSnapshotEntry;
import com.adrninistrator.javacg2.dto.frame.InstructionStepList;
import lombok.Getter;
import lombok.Setter;
import org.apache.bcel.generic.InstructionHandle;

import java.util.ArrayList;
import java.util.List;

/**
 * @author adrninistrator
 * @date 2022/10/16
 * @description: 对分支指令进行遍历时使用的栈结构
 */
@Getter
public class BranchStackEntry {

    // 分支指令
    private final InstructionHandle branchIh;

    // 目标指令列表
    private final List<InstructionHandle> targetIhList;

    // 当前处理的目标指令序号
    @Setter
    private int targetIhIndex;

    // 栈桢信息快照
    @Setter
    private FrameSnapshotEntry frameSnapshotEntry;

    // 指令执行步骤
    @Setter
    private InstructionStepList instructionStepList;

    public BranchStackEntry(InstructionHandle branchIh, int targetNum) {
        this.branchIh = branchIh;
        targetIhList = new ArrayList<>(targetNum);
        targetIhIndex = 0;
    }

    public void addTargetIh(InstructionHandle targetIh) {
        targetIhList.add(targetIh);
    }

    @Override
    public String toString() {
        return "BranchStackEntry{" +
                "branchIh=" + branchIh +
                ", targetIhList=" + targetIhList +
                ", targetIhIndex=" + targetIhIndex +
                '}';
    }
}
