package org.gwfx.zuoyanmod.ai.core.llm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gwfx.zuoyanmod.ai.core.http.HttpResponseData;
import org.gwfx.zuoyanmod.ai.core.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaProviderTest {

    @Test
    void buildsNativeChatRequestBody() {
        ChatRequest request = new ChatRequest(
                "llama3", "系统提示", List.of(ChatMessage.user("你好")), 0.5D, 128);

        JsonObject body = JsonParser.parseString(OllamaProvider.buildBody(request)).getAsJsonObject();

        assertEquals("llama3", body.get("model").getAsString());
        assertFalse(body.get("stream").getAsBoolean());
        assertEquals(2, body.getAsJsonArray("messages").size());

        JsonObject options = body.getAsJsonObject("options");
        assertEquals(0.5D, options.get("temperature").getAsDouble(), 1e-9);
        // Ollama 用 num_predict 而不是 max_tokens
        assertEquals(128, options.get("num_predict").getAsInt());
        assertFalse(body.has("max_tokens"));
    }

    @Test
    void hitsNativeApiPathAndSendsNoAuthorizationHeader() {
        OpenAiCompatibleProviderTest.RecordingTransport transport =
                new OpenAiCompatibleProviderTest.RecordingTransport();
        // 假传输默认返回 OpenAI 形状的报文，这里换成 Ollama 的
        transport.response = new HttpResponseData(200, "{\"message\":{\"content\":\"ok\"}}");
        OllamaProvider provider = new OllamaProvider(transport, "http://localhost:11434", Duration.ofSeconds(5));

        provider.chat(new ChatRequest("llama3", "", List.of(ChatMessage.user("hi")), 0.7D, 10)).join();

        assertEquals(URI.create("http://localhost:11434/api/chat"), transport.lastUri);
        // 本地服务不需要鉴权；多余的 Bearer 头反而会被拒绝
        assertTrue(transport.lastHeaders.isEmpty(), "Ollama 请求不应带任何请求头");
    }

    @Test
    void parsesMessageAndTokenCounts() {
        String body = """
                {"model":"llama3","message":{"role":"assistant","content":"你好"},
                 "done":true,"prompt_eval_count":8,"eval_count":21}
                """;

        ChatResponse response = OllamaProvider.parseResponse(new HttpResponseData(200, body));

        assertEquals("你好", response.text());
        assertEquals(8, response.promptTokens());
        assertEquals(21, response.completionTokens());
        assertTrue(response.hasUsage());
    }

    @Test
    void rejectsResponseWithoutMessage() {
        assertThrows(LlmException.class, () ->
                OllamaProvider.parseResponse(new HttpResponseData(200, "{\"done\":true}")));
    }

    @Test
    void surfacesHttpStatusWhenModelMissing() {
        LlmException error = assertThrows(LlmException.class, () ->
                OllamaProvider.parseResponse(new HttpResponseData(404, "{\"error\":\"model not found\"}")));
        assertEquals(404, error.httpStatus());
    }

    @Test
    void stripsTrailingSlashFromBaseUrl() {
        RecordingOllamaTransport transport = new RecordingOllamaTransport();
        OllamaProvider provider = new OllamaProvider(transport, "http://127.0.0.1:11434///", Duration.ofSeconds(5));

        provider.chat(new ChatRequest("m", "", List.of(ChatMessage.user("hi")), 0.7D, 10)).join();

        assertEquals(URI.create("http://127.0.0.1:11434/api/chat"), transport.lastUri);
    }

    // ===== 工具调用报文（与 OpenAI 的两处差异必须在这里被归一化）=====

    private static ToolSpec toolSpec() {
        return ToolSpec.builder("search_entities", "搜索附近实体")
                .stringParam("entity_id", "实体 id", true)
                .build();
    }

    @Test
    void omitsToolsFieldWhenRequestHasNoTools() {
        // 回归门：不带 tools 时请求体里不能多出 tools / tool_choice
        ChatRequest request = new ChatRequest("m", "", List.of(ChatMessage.user("hi")), 0.5D, 10);
        JsonObject body = JsonParser.parseString(OllamaProvider.buildBody(request)).getAsJsonObject();

        assertFalse(body.has("tools"));
        assertFalse(body.has("tool_choice"));
    }

    @Test
    void writesToolCallArgumentsAsObjectAndToolResultsWithToolName() {
        ToolCall call = new ToolCall("ollama_search_entities#0", "search_entities",
                "{\"entity_id\":\"minecraft:sheep\"}");
        ChatRequest request = new ChatRequest("m", "", List.of(
                ChatMessage.assistantToolCalls("", List.of(call)),
                ChatMessage.toolResult(call, "共 3 个")),
                0.5D, 10, List.of(toolSpec()));

        JsonObject body = JsonParser.parseString(OllamaProvider.buildBody(request)).getAsJsonObject();

        assertEquals(1, body.getAsJsonArray("tools").size());
        // Ollama 的 /api/chat 报文里没有 tool_choice 这个字段（见 docs.ollama.com/api/chat），
        // 因此这里刻意不发 —— 与 OpenAI 侧不同
        assertFalse(body.has("tool_choice"));

        JsonArray messages = body.getAsJsonArray("messages");
        JsonObject callObject = messages.get(0).getAsJsonObject()
                .getAsJsonArray("tool_calls").get(0).getAsJsonObject();
        assertEquals("function", callObject.get("type").getAsString());
        JsonObject function = callObject.getAsJsonObject("function");
        assertEquals(0, function.get("index").getAsInt());
        // Ollama 要求 arguments 是 JSON 对象（OpenAI 是字符串）
        JsonObject arguments = function.getAsJsonObject("arguments");
        assertTrue(arguments.isJsonObject());
        assertEquals("minecraft:sheep", arguments.get("entity_id").getAsString());

        JsonObject tool = messages.get(1).getAsJsonObject();
        assertEquals("tool", tool.get("role").getAsString());
        // Ollama 用 tool_name 关联，而不是 tool_call_id
        assertEquals("search_entities", tool.get("tool_name").getAsString());
        assertFalse(tool.has("tool_call_id"));
    }

    @Test
    void normalizesObjectArgumentsIntoJsonStringAndGeneratesId() {
        String body = """
                {"message":{"role":"assistant","content":"","tool_calls":[
                  {"function":{"name":"search_entities","arguments":{"entity_id":"minecraft:sheep"}}}]}}
                """;

        ChatResponse response = OllamaProvider.parseResponse(new HttpResponseData(200, body));

        assertTrue(response.hasToolCalls());
        ToolCall call = response.toolCalls().get(0);
        assertEquals("search_entities", call.name());
        // 对象被序列化成字符串，上层只面对一种形态
        assertEquals("minecraft:sheep",
                JsonParser.parseString(call.argumentsJson()).getAsJsonObject().get("entity_id").getAsString());
        // Ollama 不返回 id → 必须生成非空兜底 id
        assertFalse(call.id().isBlank());
    }

    private static final class RecordingOllamaTransport implements HttpTransport {
        URI lastUri;

        @Override
        public CompletableFuture<HttpResponseData> postJson(
                URI uri, Map<String, String> headers, String jsonBody, Duration timeout) {
            this.lastUri = uri;
            return CompletableFuture.completedFuture(
                    new HttpResponseData(200, "{\"message\":{\"content\":\"ok\"}}"));
        }
    }
}
