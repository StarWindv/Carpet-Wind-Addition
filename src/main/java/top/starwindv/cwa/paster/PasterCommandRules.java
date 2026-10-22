package top.starwindv.cwa.paster;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;

/**
 * The remote paster lets Litematica's normal command based paste task run on
 * the client. The commands are routed through CWA instead of the vanilla chat
 * command packet, so the server can keep normal players from gaining general
 * command permissions.
 */
public final class PasterCommandRules {
	private static final Set<String> COMMAND_ROOTS = Set.of(
			"setblock",
			"fill",
			"summon",
			"data",
			"clone",
			"gamerule"
	);
	private static final Pattern COMMAND_FEEDBACK_GAMERULE = Pattern.compile(
			"(?i)gamerule\\s+send_command_feedback(?:\\s+(?:true|false))?"
	);
	private static final Pattern SELECTOR_COORDINATE = Pattern.compile(
			"(?<![A-Za-z0-9_])([xyz])=(-?\\d+(?:\\.\\d+)?)"
	);

	private PasterCommandRules() {
	}

	public static String normalize(String command) {
		if (command == null) {
			return "";
		}

		String normalized = command.trim();
		while (normalized.startsWith("/")) {
			normalized = normalized.substring(1).trim();
		}
		return normalized;
	}

	public static String root(String command) {
		String normalized = normalize(command);
		int end = 0;
		while (end < normalized.length() && !Character.isWhitespace(normalized.charAt(end))) {
			end++;
		}
		return normalized.substring(0, end);
	}

	public static boolean canRoute(String command) {
		return COMMAND_ROOTS.contains(root(command));
	}

	public static boolean isValid(String command, List<PasterBounds> bounds) {
		String normalized = normalize(command);
		if (normalized.isEmpty()) {
			return false;
		}

		String[] tokens = normalized.split("\\s+");
		if (tokens.length == 0) {
			return false;
		}

		if (tokens[0].equals("gamerule")) {
			return COMMAND_FEEDBACK_GAMERULE.matcher(normalized).matches();
		}
		if (bounds == null || bounds.isEmpty()) {
			return false;
		}

		return switch (tokens[0]) {
			case "setblock" -> tokens.length >= 5
					&& (blockPositions(tokens, bounds, 1) || isAir(tokens[4]));
			case "fill" -> tokens.length >= 8 && blockPositions(tokens, bounds, 1, 4);
			case "summon" -> tokens.length >= 5 && floatingPositions(tokens, bounds, 2);
			case "clone" -> tokens.length >= 10 && blockPositions(tokens, bounds, 7);
			case "data" -> validDataCommand(tokens, bounds);
			default -> false;
		};
	}

	private static boolean validDataCommand(String[] tokens, List<PasterBounds> bounds) {
		if (tokens.length < 4 || !tokens[1].equals("modify")) {
			return false;
		}
		if (tokens[2].equals("block")) {
			return tokens.length >= 6 && blockPositions(tokens, bounds, 3);
		}
		if (tokens[2].equals("entity")) {
			return selectorPositions(tokens[3], bounds);
		}
		return false;
	}

	private static boolean isAir(String blockState) {
		return blockState.equals("air")
				|| blockState.equals("minecraft:air")
				|| blockState.equals("cave_air")
				|| blockState.equals("minecraft:cave_air")
				|| blockState.equals("void_air")
				|| blockState.equals("minecraft:void_air");
	}

	private static boolean blockPositions(String[] tokens, List<PasterBounds> bounds, int... starts) {
		for (int start : starts) {
			if (start + 2 >= tokens.length) {
				return false;
			}

			try {
				int x = Integer.parseInt(tokens[start]);
				int y = Integer.parseInt(tokens[start + 1]);
				int z = Integer.parseInt(tokens[start + 2]);
				if (!inside(bounds, x, y, z)) {
					return false;
				}
			} catch (NumberFormatException ignored) {
				return false;
			}
		}
		return true;
	}

	private static boolean floatingPositions(String[] tokens, List<PasterBounds> bounds, int start) {
		if (start + 2 >= tokens.length) {
			return false;
		}

		try {
			int x = (int) Math.floor(Double.parseDouble(tokens[start]));
			int y = (int) Math.floor(Double.parseDouble(tokens[start + 1]));
			int z = (int) Math.floor(Double.parseDouble(tokens[start + 2]));
			return inside(bounds, x, y, z);
		} catch (NumberFormatException ignored) {
			return false;
		}
	}

	private static boolean selectorPositions(String selector, List<PasterBounds> bounds) {
		Matcher matcher = SELECTOR_COORDINATE.matcher(selector);
		int x = 0;
		int y = 0;
		int z = 0;
		boolean hasX = false;
		boolean hasY = false;
		boolean hasZ = false;

		while (matcher.find()) {
			double value;
			try {
				value = Double.parseDouble(matcher.group(2));
			} catch (NumberFormatException ignored) {
				return false;
			}
			int coordinate = (int) Math.floor(value);
			switch (matcher.group(1)) {
				case "x" -> {
					x = coordinate;
					hasX = true;
				}
				case "y" -> {
					y = coordinate;
					hasY = true;
				}
				case "z" -> {
					z = coordinate;
					hasZ = true;
				}
				default -> {
					return false;
				}
			}
		}

		return hasX && hasY && hasZ && inside(bounds, x, y, z);
	}

	private static boolean inside(List<PasterBounds> bounds, int x, int y, int z) {
		for (PasterBounds bound : bounds) {
			BlockPos start = bound.start();
			BlockPos end = bound.end();
			if (x >= start.getX() && x <= end.getX()
					&& y >= start.getY() && y <= end.getY()
					&& z >= start.getZ() && z <= end.getZ()) {
				return true;
			}
		}
		return false;
	}
}
