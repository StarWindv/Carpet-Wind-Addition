package top.starwindv.cwa.paster;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
import org.jetbrains.annotations.Nullable;
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
	private static Class<?> layerRangeClass;

	private LitematicaBridge() {
	}

	public static boolean isAvailable() {
		return initialize();
	}

	private static boolean isPrinterLoaded() {
		return FabricLoader.getInstance().isModLoaded("litematica_printer")
				|| FabricLoader.getInstance().isModLoaded("litematica-printer");
	}

	private static synchronized boolean initialize() {
		if (initialized) {
			return available;
		}
		initialized = true;

		if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT
				|| !FabricLoader.getInstance().isModLoaded("litematica")
				|| !isPrinterLoaded()) {
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
			if (minecraft == null || !client.hasSingleplayerServer(minecraft)) {
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
					regions.add(String.valueOf(entry.getKey()));
					bounds.add(new PasterBounds(first, second));
				}
			}
			if (bounds.isEmpty()) {
				return null;
			}

			Object schematic = placementClass.getMethod("getSchematic").invoke(placement);
			if (schematic == null) {
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
			String dimension = actualLevel.dimension().location().toString();
			return new PlacementSnapshot(id, name, dimension, placement, List.copyOf(required), List.copyOf(bounds));
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to read the current Litematica placement", throwable);
			return null;
		}
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

	public static boolean startPaste(Object placement, Consumer<Boolean> completion, Runnable failure) {
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

	public static boolean isSchematicUsedByStoredPlacements(Object schematic, Set<String> storedPlacementIds) {
		if (!isAvailable() || storedPlacementIds.isEmpty()) {
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
				if (id != null && storedPlacementIds.contains(id.toString())
						&& getSchematic.invoke(placement) == schematic) {
					return true;
				}
			}
		} catch (Throwable throwable) {
			CarpetWindAddition.LOGGER.warn("Failed to check whether a Litematica schematic is protected", throwable);
		}
		return false;
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
