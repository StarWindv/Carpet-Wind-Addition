package top.starwindv.cwa.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CwaMessages {
	private static final Map<String, String> EN_US = load();

	private CwaMessages() {
	}

	public static MutableComponent translatable(String key, Object... args) {
		String fallback = EN_US.getOrDefault(key, key);
		return Component.translatableWithFallback(key, fallback, args);
	}

	private static Map<String, String> load() {
		try (InputStream stream = CwaMessages.class.getClassLoader()
				.getResourceAsStream("assets/cwa/lang/en_us.json")) {
			if (stream == null) {
				return Map.of();
			}
			Map<String, String> translations = new Gson().fromJson(
					new InputStreamReader(stream, StandardCharsets.UTF_8),
					new TypeToken<Map<String, String>>() {
					}.getType()
			);
			return translations == null ? Map.of() : Map.copyOf(translations);
		} catch (Exception exception) {
			return Map.of();
		}
	}
}
