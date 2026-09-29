package org.gwfx.zuoyanmod.ai.core.llm;

import org.gwfx.zuoyanmod.ai.core.http.HttpResponseData;
import org.gwfx.zuoyanmod.ai.core.http.HttpTransport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderRegistryTest {

    private static final HttpTransport NOOP_TRANSPORT = (uri, headers, body, timeout) ->
            CompletableFuture.completedFuture(new HttpResponseData(200, "{}"));

    private static ProviderRegistry.ProviderSettings settings() {
        return new ProviderRegistry.ProviderSettings("http://localhost", "", Duration.ofSeconds(5), 1);
    }

    @Test
    void recognizesKnownIdsCaseInsensitively() {
        assertTrue(ProviderRegistry.isKnown("ollama"));
        assertTrue(ProviderRegistry.isKnown("  Ollama  "));
        assertTrue(ProviderRegistry.isKnown("openai-compatible"));
        assertTrue(ProviderRegistry.isKnown("mock"));
        assertFalse(ProviderRegistry.isKnown("gemini"));
        assertFalse(ProviderRegistry.isKnown(""));
        assertFalse(ProviderRegistry.isKnown(null));
    }

    @Test
    void fallsBackToMockForUnknownOrBlankIds() {
        assertEquals(MockLlmProvider.ID, ProviderRegistry.normalizeId("gemini"));
        assertEquals(MockLlmProvider.ID, ProviderRegistry.normalizeId(""));
        assertEquals(MockLlmProvider.ID, ProviderRegistry.normalizeId(null));
        assertEquals(OllamaProvider.ID, ProviderRegistry.normalizeId("OLLAMA"));
    }

    @Test
    void knownIdsAreStableAndComplete() {
        // 这个列表会直接展示给用户（/ai provider 的合法取值、/ai status 的说明），
        // 顺序固定，且必须与 isKnown 保持一致
        assertEquals(java.util.List.of("mock", "openai-compatible", "ollama"), ProviderRegistry.knownIds());
        for (String id : ProviderRegistry.knownIds()) {
            assertTrue(ProviderRegistry.isKnown(id), id + " 应被 isKnown 认可");
        }
    }

    @Test
    void createsMockProviderForUnknownIdInsteadOfThrowing() {
        ProviderRegistry registry = new ProviderRegistry(NOOP_TRANSPORT);

        // 配置里的错别字不该让服务端起不来，也不该抛给玩家
        assertInstanceOf(MockLlmProvider.class, registry.create("gemini", settings()));
        assertInstanceOf(OpenAiCompatibleProvider.class, registry.create("openai-compatible", settings()));
        assertInstanceOf(OllamaProvider.class, registry.create("ollama", settings()));
    }

    @Test
    void mockProviderEchoesWithoutNetwork() {
        MockLlmProvider provider = new MockLlmProvider();

        ChatResponse response = provider.chat(new ChatRequest(
                "m", "上下文", java.util.List.of(ChatMessage.user("附近有没有宝箱")), 0.7D, 10)).join();

        assertEquals(MockLlmProvider.ID, provider.id());
        assertTrue(response.text().contains("附近有没有宝箱"));
        assertTrue(response.text().contains("mock"));
    }
}
