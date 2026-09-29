package org.gwfx.zuoyanmod.ai.core.llm;

import java.util.concurrent.CompletableFuture;

/**
 * 离线回显 Provider（{@code ai.provider = "mock"}）。
 *
 * <p>两个用途：
 * <ol>
 *   <li><b>单测</b>：不发网络请求，验证 Agent/命令/分页等上层链路。</li>
 *   <li><b>零成本联调</b>：玩家还没配密钥时也能把「命令 → 上下文 → 分页回复」整条链路跑通，
 *       不会静默触发真实计费接口。这也是本模组把 {@code ai.provider} 默认值设为 {@code mock} 的原因。</li>
 * </ol>
 */
public final class MockLlmProvider implements LlmProvider {

    public static final String ID = "mock";

    private static final int MAX_ECHO_CHARS = 200;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CompletableFuture<ChatResponse> chat(ChatRequest request) {
        String lastUser = lastUserContent(request);
        int contextChars = request.systemPrompt() == null ? 0 : request.systemPrompt().length();

        String reply = "【mock】未接入真实模型，这是一条本地回显。\n"
                + "收到：" + abbreviate(lastUser) + "\n"
                + "本轮上下文 " + contextChars + " 字符，历史 " + request.messages().size() + " 条。\n"
                + "把配置里的 ai.provider 改成 openai-compatible 或 ollama 并配好密钥，即可真实对话。";

        return CompletableFuture.completedFuture(ChatResponse.of(reply));
    }

    private static String lastUserContent(ChatRequest request) {
        String found = "";
        for (ChatMessage message : request.messages()) {
            if (ChatMessage.ROLE_USER.equals(message.role())) {
                found = message.content();
            }
        }
        return found;
    }

    private static String abbreviate(String text) {
        if (text == null || text.isEmpty()) {
            return "(空)";
        }
        String flattened = text.replace('\n', ' ').strip();
        return flattened.length() > MAX_ECHO_CHARS ? flattened.substring(0, MAX_ECHO_CHARS) + "…" : flattened;
    }
}
