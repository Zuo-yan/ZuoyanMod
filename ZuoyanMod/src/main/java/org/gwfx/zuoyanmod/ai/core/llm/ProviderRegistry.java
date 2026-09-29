package org.gwfx.zuoyanmod.ai.core.llm;

import org.gwfx.zuoyanmod.ai.core.http.HttpTransport;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Provider 工厂注册表：把配置里的 {@code ai.provider} 字符串映射成具体实现。
 *
 * <p>未知 id <b>不抛异常</b>，而是回退到 {@link MockLlmProvider}。
 * 理由：配置项是用户手改的，一个错别字不应该让专用服务端加载失败或让玩家拿到一句
 * 看不懂的堆栈；回退后 {@code /ai status} 会显示当前实际生效的 Provider，足够定位。
 */
public final class ProviderRegistry {

    /**
     * 创建一个 Provider 所需的运行期参数。
     *
     * <p>密钥只经由本对象随构造注入，<b>不会进入 {@code AiConfig}</b>，
     * 因此配置快照可以安全地被日志/命令回显。
     */
    public record ProviderSettings(String baseUrl, String apiKey, Duration timeout, int retryCount) {
    }

    /**
     * 已注册的 Provider id，<b>顺序固定</b>，便于直接展示给用户
     * （{@code /ai provider} 的用法提示、{@code /ai status} 的选项列表）。
     */
    public static List<String> knownIds() {
        return List.of(MockLlmProvider.ID, OpenAiCompatibleProvider.ID, OllamaProvider.ID);
    }

    private static final Set<String> KNOWN_IDS = Set.copyOf(knownIds());

    private final Map<String, Function<ProviderSettings, LlmProvider>> factories = new LinkedHashMap<>();
    private final HttpTransport transport;

    public ProviderRegistry(HttpTransport transport) {
        this.transport = transport;
        register(MockLlmProvider.ID, settings -> new MockLlmProvider());
        register(OpenAiCompatibleProvider.ID, settings -> new OpenAiCompatibleProvider(
                this.transport, settings.baseUrl(), settings.apiKey(), settings.timeout()));
        register(OllamaProvider.ID, settings -> new OllamaProvider(
                this.transport, settings.baseUrl(), settings.timeout()));
    }

    public void register(String id, Function<ProviderSettings, LlmProvider> factory) {
        factories.put(id, factory);
    }

    /** 是否为已注册的 Provider id（大小写不敏感）。 */
    public static boolean isKnown(String id) {
        return id != null && KNOWN_IDS.contains(normalize(id));
    }

    /** 归一化 id：去空白、转小写；未知或空值回退到 mock。 */
    public static String normalizeId(String id) {
        String normalized = normalize(id);
        return KNOWN_IDS.contains(normalized) ? normalized : MockLlmProvider.ID;
    }

    private static String normalize(String id) {
        return id == null ? "" : id.strip().toLowerCase(Locale.ROOT);
    }

    /** 按 id 创建实例；未知 id 回退 mock。 */
    public LlmProvider create(String id, ProviderSettings settings) {
        Function<ProviderSettings, LlmProvider> factory = factories.get(normalizeId(id));
        if (factory == null) {
            factory = factories.get(MockLlmProvider.ID);
        }
        return factory.apply(settings);
    }
}
