package org.gwfx.zuoyanmod.ai.core.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.gwfx.zuoyanmod.ai.core.http.HttpResponseData;
import org.gwfx.zuoyanmod.ai.core.http.HttpTransport;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Ollama 原生端点（<code>POST {baseUrl}/api/chat</code>）。
 *
 * <p>走原生接口而不是 Ollama 的 <code>/v1</code> 兼容层，原因是原生接口能拿到
 * {@code prompt_eval_count / eval_count}（即 token 用量），便于向玩家显示本次消耗。
 *
 * <p>本地服务默认不需要密钥，因此<b>不发 Authorization 头</b>。
 */
public final class OllamaProvider implements LlmProvider {

    public static final String ID = "ollama";

    private static final String PATH_CHAT = "/api/chat";
    private static final int MAX_ERROR_SNIPPET = 200;
    private static final int UNKNOWN_TOKENS = -1;

    private final HttpTransport transport;
    private final String baseUrl;
    private final Duration timeout;

    /** @param baseUrl 形如 {@code http://localhost:11434}（末尾斜杠会被去掉） */
    public OllamaProvider(HttpTransport transport, String baseUrl, Duration timeout) {
        this.transport = transport;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.timeout = timeout;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public CompletableFuture<ChatResponse> chat(ChatRequest request) {
        URI uri = URI.create(this.baseUrl + PATH_CHAT);
        return transport
                .postJson(uri, Map.of(), buildBody(request), this.timeout)
                .thenApply(OllamaProvider::parseResponse);
    }

    /** 组装请求体。测试直接断言本方法。 */
    static String buildBody(ChatRequest request) {
        JsonObject root = new JsonObject();
        root.addProperty("model", request.model());

        JsonArray messages = new JsonArray();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(message(ChatMessage.system(request.systemPrompt())));
        }
        for (ChatMessage message : request.messages()) {
            messages.add(message(message));
        }
        root.add("messages", messages);

        root.addProperty("stream", false);

        JsonObject options = new JsonObject();
        options.addProperty("temperature", request.temperature());
        options.addProperty("num_predict", request.maxTokens());
        root.add("options", options);

        // 只在真的给了工具时才出现 tools 字段。
        // ⚠️ 与 OpenAI 侧不同，这里**不发 tool_choice**：Ollama 的 /api/chat 报文里
        // 只有 model/messages/tools/format/options/stream/think/keep_alive/logprobs 这些字段
        // （见 https://docs.ollama.com/api/chat ），并没有 tool_choice。
        // Ollama 用 Go 解析、未定义字段本会被忽略，但发一个对端不存在的字段没有意义，
        // 也违背「不出现多余字段」的原则。
        if (request.hasTools()) {
            root.add("tools", ToolSpec.toJsonArray(request.tools()));
        }

        return root.toString();
    }

    /**
     * 单条消息 → Ollama 报文。
     *
     * <p>与 OpenAI 的三处关键差异：
     * <ul>
     *   <li>assistant 的 {@code function.arguments} 必须是 JSON <b>对象</b>（OpenAI 是字符串），
     *       所以这里把上层规范化的字符串再解析回对象；</li>
     *   <li>tool_calls 里要带 {@code type:"function"} 与 {@code function.index}
     *       —— 与官方报文示例保持一致（见 docs.ollama.com/capabilities/tool-calling）；</li>
     *   <li>工具结果用 {@code tool_name}（Ollama 不返回 tool call id）。</li>
     * </ul>
     */
    private static JsonObject message(ChatMessage message) {
        JsonObject object = new JsonObject();
        object.addProperty("role", message.role());
        object.addProperty("content", message.content());

        if (message.hasToolCalls()) {
            JsonArray calls = new JsonArray();
            for (int i = 0; i < message.toolCalls().size(); i++) {
                ToolCall call = message.toolCalls().get(i);
                JsonObject function = new JsonObject();
                function.addProperty("index", i);
                function.addProperty("name", call.name());
                function.add("arguments", parseArguments(call.argumentsJson()));

                JsonObject callObject = new JsonObject();
                callObject.addProperty("type", "function");
                callObject.add("function", function);
                calls.add(callObject);
            }
            object.add("tool_calls", calls);
        }

        if (ChatMessage.ROLE_TOOL.equals(message.role()) && message.toolName() != null) {
            object.addProperty("tool_name", message.toolName());
        }
        return object;
    }

    /** 把规范化的 arguments 字符串还原成 JSON 对象；非法内容降级为空对象而不是让整轮失败。 */
    private static JsonElement parseArguments(String argumentsJson) {
        try {
            JsonElement parsed = JsonParser.parseString(argumentsJson);
            return parsed.isJsonObject() ? parsed : new JsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            return new JsonObject();
        }
    }

    /** 解析响应。非 2xx 抛带状态码的 {@link LlmException}。 */
    static ChatResponse parseResponse(HttpResponseData response) {
        if (!response.isSuccess()) {
            throw LlmException.http(response.statusCode(),
                    "HTTP " + response.statusCode() + snippetOf(response.body()));
        }

        JsonObject root;
        try {
            JsonElement parsed = JsonParser.parseString(response.body());
            if (!parsed.isJsonObject()) {
                throw new JsonSyntaxException("响应根节点不是 JSON 对象");
            }
            root = parsed.getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new LlmException("响应不是合法 JSON：" + snippetOf(response.body()), e);
        }

        JsonObject message = root.getAsJsonObject("message");
        if (message == null) {
            throw new LlmException("响应里没有 message：" + snippetOf(response.body()));
        }

        String text = asString(message.get("content"));
        int promptTokens = asInt(root.get("prompt_eval_count"), UNKNOWN_TOKENS);
        int completionTokens = asInt(root.get("eval_count"), UNKNOWN_TOKENS);

        return new ChatResponse(text, promptTokens, completionTokens, null, parseToolCalls(message));
    }

    /**
     * 解析 {@code message.tool_calls}。
     *
     * <p>与 OpenAI 的两处差异在这里归一化掉：
     * <ul>
     *   <li>{@code arguments} 是 JSON <b>对象</b> → 序列化成字符串，上层只面对字符串；</li>
     *   <li>没有 id → 用「函数名 + 本步序号」生成稳定兜底 id。同一轮内唯一即可满足
     *       消息链组装的需要（真正回传时用的是 tool_name，不依赖这个 id）。</li>
     * </ul>
     */
    private static List<ToolCall> parseToolCalls(JsonObject message) {
        JsonArray raw = message.getAsJsonArray("tool_calls");
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<ToolCall> calls = new ArrayList<>(raw.size());
        for (int i = 0; i < raw.size(); i++) {
            JsonElement element = raw.get(i);
            if (element == null || !element.isJsonObject()) {
                continue;
            }
            JsonObject function = element.getAsJsonObject().getAsJsonObject("function");
            if (function == null) {
                continue;
            }
            String name = asString(function.get("name"));
            if (name.isEmpty()) {
                continue;
            }
            JsonElement arguments = function.get("arguments");
            String argumentsJson = arguments == null || arguments.isJsonNull() ? "{}" : arguments.toString();
            calls.add(new ToolCall("ollama_" + name + "#" + i, name, argumentsJson));
        }
        return calls;
    }

    private static String asString(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.isJsonPrimitive() ? element.getAsString() : element.toString();
    }

    private static int asInt(JsonElement element, int fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return fallback;
        }
    }

    private static String snippetOf(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String trimmed = body.strip();
        return trimmed.length() > MAX_ERROR_SNIPPET
                ? "：" + trimmed.substring(0, MAX_ERROR_SNIPPET) + "…"
                : "：" + trimmed;
    }

    private static String stripTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        String trimmed = url.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
