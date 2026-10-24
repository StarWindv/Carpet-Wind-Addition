package top.starwindv.cwa.paster;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PasterDiscardRules {
	private static final Map<UUID, List<PasterDiscardRule>> RULES = new ConcurrentHashMap<>();

	private PasterDiscardRules() {
	}

	public static List<PasterDiscardRule> get(UUID placementId) {
		List<PasterDiscardRule> rules = RULES.get(placementId);
		return rules == null ? List.of() : List.copyOf(rules);
	}

	public static void add(UUID placementId, PasterDiscardRule rule) {
		RULES.compute(placementId, (id, existing) -> {
			List<PasterDiscardRule> rules = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
			if (!rules.contains(rule)) {
				rules.add(rule);
			}
			return List.copyOf(rules);
		});
	}

	public static void clear(UUID placementId) {
		RULES.remove(placementId);
	}
}
