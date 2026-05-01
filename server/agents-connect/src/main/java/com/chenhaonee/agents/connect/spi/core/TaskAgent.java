package com.chenhaonee.agents.connect.spi.core;

import com.chenhaonee.agents.connect.spi.model.task.TaskPrepareResult;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnRequest;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnResult;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;

/**
 * 任务型 Agent SPI。
 * <p>
 * 面向系统/调度触发的独立任务执行场景。
 * 与实时会话不同，任务执行按回合推进，回合结果仅表达停机结论。
 * 步骤推进、协同发起等过程动作应通过 capability 或 MCP tool 主动完成。
 * impl 层通过注入的 AgentConfigApi 按需查询 agent profile 和配置。
 */
public interface TaskAgent {

    AgentProvider supportedType();

    /**
     * 任务下发 / 环境准备。
     * <p>
     * 在一次 run 正式开始前调用。impl 可在此做执行环境初始化，例如：
     * - 在 Agent 工作目录下创建任务子文件夹
     * - 写入任务描述文件
     * - 准备任务运行所需的上下文文件
     * <p>
     * 是否真正执行准备动作由具体 impl 自行把握。
     * impl 可通过注入的 AgentConfigApi 查询 agent profile 和 config。
     *
     * @param agentCode Agent 编码
     * @param taskCode  任务编码
     * @return 准备结果
     */
    TaskPrepareResult prepare(String agentCode, String taskCode);

    /**
     * 执行一个任务回合。
     * <p>
     * 每次调用代表 app 对 Agent 的一次任务推进。Agent 持续执行直到停机点。
     *
     * @param agentCode Agent 编码
     * @param request   当前回合的稳定标识
     * @return 当前回合输出
     */
    TaskTurnResult runTurn(String agentCode, TaskTurnRequest request);
}
