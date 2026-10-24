package top.starwindv.cwa.paster;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import top.starwindv.cwa.CarpetWindAddition;

public final class LitematicaBridge {
	private static final String DATA_MANAGER = "fi.dy.masa.litematica.data.DataManager";
	private static final String PLACEMENT = "fi.dy.masa.litematica.schematic.placement.SchematicPlacement";
	private static final String SCHEMATIC = "fi.dy.masa.litematica.schematic.LitematicaSchematic";
	private static final String BOX = "fi.dy.masa.litematica.selection.Box";
	private static final String REQUIRED_ENABLED = "fi.dy.masa.litematica.schematic.placement.SubRegionPlacement$RequiredEnabled";
	private static final String MATERIAL_UTILS = "fi.dy.masa.litematica.materials.MaterialListUtils";
	private static final String MATERIAL_ENTRY = "fi.dy.masa.litematica.materials.MaterialListEntry";
	private static final String BLOCK_CONTAINER = "fi.dy.masa.litematica.schematic.container.LitematicaBlockStateContainer";
	private static final String TASK_SCHEDULER = "fi.dy.masa.litematica.scheduler.TaskScheduler";
	private static final String TASK_INTERFACE = "fi.dy.masa.litematica.scheduler.ITask";
	private static final String COMPLETION_LISTENER = "fi.dy.masa.malilib.interfaces.ICompletionListener";
	private static final String PASTE_TASK = "fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkDirect";
	private static final String COMMAND_PASTE_TASK = "fi.dy.masa.litematica.scheduler.tasks.TaskPasteSchematicPerChunkCommand";
	private static final String LAYER_RANGE = "fi.dy.masa.malilib.util.position.LayerRange";

	private static boolean initialized;
	private static boolean available;
	private static ClientAccess client;

	private static Class<?> dataManagerClass;
	private static Class<?> placementClass;
	private static Class<?> schematicClass;
	private static Class<?> boxClass;
	private static Class<?> requiredEnabledClass;
	private static Class<?> materialUtilsClass;
	private static Class<?> materialEntryClass;
	private static Class<?> blockContainerClass;
	private static Class<?> taskSchedulerClass;
	private static Class<?> taskInterfaceClass;
	private static Class<?> completionListenerClass;
	private static Class<?> pasteTaskClass;
	private static Class<?> commandPasteTaskClass;
	private static Class<?> layerRangeClass;

	private LitematicaBridge() {
	}

	public static boolean isAvailable() {
		return initialize();
	}

	private static synchronized boolean initialize() {
		if (initialized) {
			return available;
		}
		initialized = true;

		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT
				|| !FabricLoader.getInstance().isModLoaded("litematica")) {
			return false;
		}

		try {
			Class<?> accessClass = Class.forName("top.starwindv.cwa.paster.LitematicaClientAccess");
			client = (ClientAccess) accessClass.getDeclaredConstructor().newInstance();
			dataManagerClass = Class.forName(DATA_MANAGER);
			placementClass = Class.forName(PLACEMENT);
			schematicClass = Class.forName(SCHEMATIC);
			boxClass = Class.forName(BOX);
			requiredEnabledClass = Class.forName(REQUIRED_ENABLED);
			materialUtilsClass = Class.forName(MATERIAL_UTILS);
			materialEntryClass = Class.forName(MATERIAL_ENTRY);
			blockContainerClass = Class.forName(BLOCK_CONTAINER);
			taskSchedulerClass = Class.forName(TASK_SCHEDULER);
			taskInterfaceClass = Class.forName(TASK_INTERFACE);
			completionListenerClass = Class.forName(COMPLETION_LISTENER);
			pasteTaskClass = Class.forName(PASTE_TASK);
			commandPasteTaskClass = Class.forName(COMMAND_PASTE_TASK);
			layerRangeClass = Class.forName(LAYER_RANGE);
			available = true;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Litematica paster integration is unavailable", throwable);
			available = false;
		}
		return available;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static @Nullable PlacementSnapshot currentPlacement() {
		if (!isAvailable()) {
			return null;
		}

		try {
			Object minecraft = client.minecraft();
			if (minecraft == null) {
				return null;
			}

			Object level = client.level(minecraft);
			if (!(level instanceof Level actualLevel)) {
				return null;
			}

			Object manager = dataManagerClass.getMethod("getSchematicPlacementManager").invoke(null);
			Object placement = manager.getClass().getMethod("getSelectedSchematicPlacement").invoke(manager);
			if (placement == null) {
				return null;
			}

			Object schematic = placementClass.getMethod("getSchematic").invoke(placement);
			if (schematic == null) {
				return null;
			}

			Object enabledValue = Enum.valueOf(
					requiredEnabledClass.asSubclass(Enum.class),
					"PLACEMENT_ENABLED"
			);
			Map<?, ?> boxes = (Map<?, ?>) placementClass
					.getMethod("getSubRegionBoxes", requiredEnabledClass)
					.invoke(placement, enabledValue);
			if (boxes == null || boxes.isEmpty()) {
				return null;
			}

			List<String> regions = new ArrayList<>(boxes.size());
			List<PasterBounds> bounds = new ArrayList<>(boxes.size());
			for (Map.Entry<?, ?> entry : boxes.entrySet()) {
				Object box = entry.getValue();
				BlockPos first = (BlockPos) boxClass.getMethod("getPos1").invoke(box);
				BlockPos second = (BlockPos) boxClass.getMethod("getPos2").invoke(box);
				if (first != null && second != null) {
					String region = String.valueOf(entry.getKey());
					regions.add(region);
					bounds.add(expandToContainerSize(schematic, region, first, second));
				}
			}
			if (bounds.isEmpty()) {
				return null;
			}

			List<?> entries = materialEntries(schematic, regions);
			List<PasterMaterial> required = new ArrayList<>();
			for (Object entry : entries) {
				ItemStack stack = (ItemStack) materialEntryClass.getMethod("getStack").invoke(entry);
				int count = (Integer) materialEntryClass.getMethod("getCountTotal").invoke(entry);
				if (stack != null && !stack.isEmpty() && count > 0) {
					mergeRequired(required, stack, count);
				}
			}

			UUID id = (UUID) placementClass.getMethod("getHashId").invoke(placement);
			String name = (String) placementClass.getMethod("getName").invoke(placement);
			if (name == null) {
				name = "";
			}
			String dimension = actualLevel.dimension().identifier().toString();
			String fingerprint = fingerprint(placement);
			return new PlacementSnapshot(
					id,
					name,
					dimension,
					placement,
					List.copyOf(required),
					List.copyOf(bounds),
					fingerprint == null ? "" : fingerprint
			);
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to read the current Litematica placement", throwable);
			return null;
		}
	}

	private static PasterBounds expandToContainerSize(
			Object schematic,
			String region,
			BlockPos first,
			BlockPos second
	) {
		int minX = Math.min(first.getX(), second.getX());
		int minY = Math.min(first.getY(), second.getY());
		int minZ = Math.min(first.getZ(), second.getZ());
		int maxX = Math.max(first.getX(), second.getX());
		int maxY = Math.max(first.getY(), second.getY());
		int maxZ = Math.max(first.getZ(), second.getZ());
		int originalMaxX = maxX;
		int originalMaxY = maxY;
		int originalMaxZ = maxZ;

		try {
			Object container = schematicClass
					.getMethod("getSubRegionContainer", String.class)
					.invoke(schematic, region);
			if (container != null) {
				Vec3i size = (Vec3i) blockContainerClass.getMethod("getSize").invoke(container);
				// Schematic regions can have a negative Size on an axis. Some
				// Litematica versions then report a collapsed sub-region box
				// while the paste task still uses the full block container.
				maxX = Math.max(maxX, minX + Math.abs(size.getX()) - 1);
				maxY = Math.max(maxY, minY + Math.abs(size.getY()) - 1);
				maxZ = Math.max(maxZ, minZ + Math.abs(size.getZ()) - 1);
			}
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.debug(
					"Failed to expand the paster bounds from the schematic container",
					throwable
			);
		}

		if (maxX != originalMaxX || maxY != originalMaxY || maxZ != originalMaxZ) {
			CarpetWindAddition.LOGGER.info(
					"Expanded paster bound for region {}: {} {} {} -> {} {} {}",
					region,
					originalMaxX,
					originalMaxY,
					originalMaxZ,
					maxX,
					maxY,
					maxZ
			);
		}

		return new PasterBounds(
				new BlockPos(minX, minY, minZ),
				new BlockPos(maxX, maxY, maxZ)
		);
	}

	private static List<?> materialEntries(Object schematic, List<String> regions) throws ReflectiveOperationException {
		try {
			return (List<?>) materialUtilsClass
					.getMethod("createMaterialListFor", schematicClass, Collection.class)
					.invoke(null, schematic, regions);
		} catch (ReflectiveOperationException | RuntimeException firstFailure) {
			CarpetWindAddition.LOGGER.debug(
					"Falling back to a thread-safe Litematica material list calculation",
					firstFailure
			);
		}

		Object2IntOpenHashMap<BlockState> counts = new Object2IntOpenHashMap<>();
		Method getContainer = schematicClass.getMethod("getSubRegionContainer", String.class);
		Method getSize = blockContainerClass.getMethod("getSize");
		Method getBlockState = blockContainerClass.getMethod("get", int.class, int.class, int.class);

		for (String region : regions) {
			Object container = getContainer.invoke(schematic, region);
			if (container == null) {
				continue;
			}

			Vec3i size = (Vec3i) getSize.invoke(container);
			for (int y = 0; y < size.getY(); y++) {
				for (int z = 0; z < size.getZ(); z++) {
					for (int x = 0; x < size.getX(); x++) {
						BlockState state = (BlockState) getBlockState.invoke(container, x, y, z);
						if (state != null) {
							counts.addTo(state, 1);
						}
					}
				}
			}
		}

		return (List<?>) materialUtilsClass
				.getMethod(
						"getMaterialList",
						Object2IntOpenHashMap.class,
						Object2IntOpenHashMap.class,
						Object2IntOpenHashMap.class,
						Player.class
				)
				.invoke(
						null,
						counts,
						new Object2IntOpenHashMap<>(),
						new Object2IntOpenHashMap<>(),
						null
				);
	}

	private static void mergeRequired(List<PasterMaterial> required, ItemStack stack, int count) {
		for (int i = 0; i < required.size(); i++) {
			PasterMaterial material = required.get(i);
			if (PasterMaterials.matches(material.sample(), stack)) {
				required.set(i, new PasterMaterial(material.sample(), material.count() + count));
				return;
			}
		}
		required.add(new PasterMaterial(stack, count));
	}

	public static @Nullable String fingerprint(Object placement) {
		try {
			Object schematic = placementClass.getMethod("getSchematic").invoke(placement);
			return fingerprintSchematic(schematic);
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to fingerprint the selected Litematica schematic", throwable);
			return null;
		}
	}

	private static @Nullable String fingerprintSchematic(Object schematic) {
		Path path = schematicPath(schematic);
		if (path == null) {
			return null;
		}

		try {
			byte[] bytes = Files.readAllBytes(path);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to fingerprint a Litematica schematic file", throwable);
			return null;
		}
	}

	private static @Nullable Path schematicPath(Object schematic) {
		if (schematic == null) {
			return null;
		}

		try {
			Object file = schematicClass.getMethod("getFile").invoke(schematic);
			return file instanceof Path path ? path : null;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.debug("Failed to read a Litematica schematic path", throwable);
			return null;
		}
	}

	private static boolean samePath(Path first, Path second) {
		return first.toAbsolutePath().normalize().equals(second.toAbsolutePath().normalize());
	}

	private static Object completionListener(Consumer<Boolean> completion) {
		return Proxy.newProxyInstance(
				completionListenerClass.getClassLoader(),
				new Class<?>[]{completionListenerClass},
				(proxy, method, args) -> {
					switch (method.getName()) {
						case "onTaskCompleted" -> completion.accept(true);
						case "onTaskAborted" -> completion.accept(false);
						case "toString" -> {
							return "cwa-paster-completion-listener";
						}
						case "hashCode" -> {
							return System.identityHashCode(proxy);
						}
						case "equals" -> {
							return proxy == args[0];
						}
						default -> {
						}
					}
					return null;
				}
		);
	}

	public static boolean startCommandPaste(UUID placementId, String fingerprint, Consumer<Boolean> completion) {
		if (!isAvailable()) {
			return false;
		}

		try {
			Object minecraft = client.minecraft();
			if (minecraft == null) {
				return false;
			}

			client.execute(minecraft, () -> {
				try {
					Object manager = dataManagerClass.getMethod("getSchematicPlacementManager").invoke(null);
					List<?> placements = (List<?>) manager.getClass()
							.getMethod("getAllSchematicsPlacements")
							.invoke(manager);
					Object selected = null;
					for (Object placement : placements) {
						UUID id = (UUID) placementClass.getMethod("getHashId").invoke(placement);
						if (placementId.equals(id)) {
							selected = placement;
							break;
						}
					}
					if (selected == null || fingerprint == null || !fingerprint.equals(fingerprint(selected))) {
						completion.accept(false);
						return;
					}

					Object layerRange = dataManagerClass.getMethod("getRenderLayerRange").invoke(null);
					Object scheduler = taskSchedulerClass.getMethod("getInstanceClient").invoke(null);
					Object task = commandPasteTaskClass
							.getConstructor(Collection.class, layerRangeClass, boolean.class)
							.newInstance(List.of(selected), layerRange, true);
					PasterClient.setActiveCommandTask(task);
					task.getClass()
							.getMethod("setCompletionListener", completionListenerClass)
							.invoke(task, completionListener(completion));
					taskSchedulerClass
							.getMethod("scheduleTask", taskInterfaceClass, int.class)
							.invoke(scheduler, task, 1);
				} catch (Throwable throwable) {
					CarpetWindAddition.LOGGER.warn("Failed to start the Litematica command paste", throwable);
					completion.accept(false);
				}
			});
			return true;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to hand the command paste to the client thread", throwable);
			return false;
		}
	}

	public static boolean startPaste(Object placement, Consumer<Boolean> completion, Runnable failure) {
		if (!isAvailable()) {
			return false;
		}
		if (placement == null) {
			CarpetWindAddition.LOGGER.warn("Cannot start a local Litematica paste without a placement object");
			failure.run();
			return false;
		}

		try {
			Object minecraft = client.minecraft();
			if (minecraft == null) {
				return false;
			}

			client.execute(minecraft, () -> {
				try {
					Object layerRange = dataManagerClass.getMethod("getRenderLayerRange").invoke(null);
					Object scheduler = taskSchedulerClass.getMethod("getInstanceServer").invoke(null);
					Object task = pasteTaskClass
							.getConstructor(Collection.class, layerRangeClass, boolean.class)
							.newInstance(List.of(placement), layerRange, true);
					Object listener = Proxy.newProxyInstance(
							completionListenerClass.getClassLoader(),
							new Class<?>[]{completionListenerClass},
							(proxy, method, args) -> {
								switch (method.getName()) {
									case "onTaskCompleted" -> completion.accept(true);
									case "onTaskAborted" -> completion.accept(false);
									case "toString" -> {
										return "cwa-paster-completion-listener";
									}
									case "hashCode" -> {
										return System.identityHashCode(proxy);
									}
									case "equals" -> {
										return proxy == args[0];
									}
									default -> {
									}
								}
								return null;
							}
					);
					task.getClass()
							.getMethod("setCompletionListener", completionListenerClass)
							.invoke(task, listener);
					taskSchedulerClass
							.getMethod("scheduleTask", taskInterfaceClass, int.class)
							.invoke(scheduler, task, 1);
				} catch (Throwable throwable) {
					CarpetWindAddition.LOGGER.warn("Failed to schedule the Litematica paste task", throwable);
					failure.run();
				}
			});
			return true;
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to hand the paste task to the client thread", throwable);
			return false;
		}
	}

	public static boolean isSchematicUsedByStoredPlacements(
			Object schematic,
			Map<String, String> storedPlacements
	) {
		if (!isAvailable() || storedPlacements.isEmpty()) {
			return false;
		}

		Path unloadingPath = schematicPath(schematic);
		String unloadingFingerprint = fingerprintSchematic(schematic);
		if (unloadingPath == null || unloadingFingerprint == null) {
			// Without a stable file identity we cannot prove the stored materials
			// belong to this schematic, so do not block the unload.
			return false;
		}

		try {
			Object manager = dataManagerClass.getMethod("getSchematicPlacementManager").invoke(null);
			List<?> placements = (List<?>) manager.getClass()
					.getMethod("getAllSchematicsPlacements")
					.invoke(manager);
			Method getHashId = placementClass.getMethod("getHashId");
			Method getSchematic = placementClass.getMethod("getSchematic");

			for (Object placement : placements) {
				UUID id = (UUID) getHashId.invoke(placement);
				if (id == null) {
					continue;
				}

				String storedFingerprint = storedPlacements.get(id.toString());
				if (storedFingerprint == null) {
					continue;
				}

				Object placementSchematic = getSchematic.invoke(placement);
				Path placementPath = schematicPath(placementSchematic);
				if (placementPath == null || !samePath(placementPath, unloadingPath)) {
					continue;
				}
				if (!storedFingerprint.isEmpty() && !storedFingerprint.equals(unloadingFingerprint)) {
					// Same file path, but the schematic content was replaced since
					// the materials were collected. The stored materials belong to
					// the old content and must not block the new one.
					continue;
				}

				CarpetWindAddition.LOGGER.info(
						"Paster protection: placement {} ({}) schematic {} still has stored materials",
						placementClass.getMethod("getName").invoke(placement),
						id,
						unloadingPath
				);
				return true;
			}
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to check whether a Litematica schematic is protected", throwable);
		}
		return false;
	}

	public static @Nullable Set<String> activePlacementIds() {
		if (!isAvailable()) {
			return null;
		}

		try {
			Object manager = dataManagerClass.getMethod("getSchematicPlacementManager").invoke(null);
			List<?> placements = (List<?>) manager.getClass()
					.getMethod("getAllSchematicsPlacements")
					.invoke(manager);
			Method getHashId = placementClass.getMethod("getHashId");
			Set<String> ids = new java.util.HashSet<>();
			for (Object placement : placements) {
				UUID id = (UUID) getHashId.invoke(placement);
				if (id != null) {
					ids.add(id.toString());
				}
			}
			return Set.copyOf(ids);
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to read the active Litematica placements", throwable);
			return null;
		}
	}

	public static void displayMessage(Component message) {
		if (!isAvailable()) {
			return;
		}

		try {
			Object minecraft = client.minecraft();
			if (minecraft != null) {
				client.displayMessage(minecraft, message);
			}
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to display a paster message on the client", throwable);
		}
	}

	public static boolean hasPasteTask() {
		if (!isAvailable()) {
			return false;
		}

		try {
			Object scheduler = taskSchedulerClass.getMethod("getInstanceServer").invoke(null);
			return (Boolean) taskSchedulerClass
					.getMethod("hasTask", Class.class)
					.invoke(scheduler, pasteTaskClass);
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to inspect the Litematica task scheduler", throwable);
			return false;
		}
	}

}
