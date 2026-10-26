package top.starwindv.cwa.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.permissions.PermissionSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.starwindv.cwa.allow.AllowManager;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Inject(method = "parseCommand", at = @At("HEAD"), cancellable = true)
	private void cwa$parseAllowedCommand(String command, CallbackInfoReturnable<ParseResults<CommandSourceStack>> cir) {
		ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
		ServerPlayer player = listener.player;
		if (player == null) {
			return;
		}

		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}

		CommandSourceStack source = player.createCommandSourceStack();
		if (!AllowManager.isAllowed(source, AllowManager.commandRoot(command))) {
			return;
		}

		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		cir.setReturnValue(dispatcher.parse(
				Commands.trimOptionalPrefix(command),
				source.withPermission(PermissionSet.ALL_PERMISSIONS)
		));
	}

	@Inject(method = "handleCustomCommandSuggestions", at = @At("HEAD"), cancellable = true)
	private void cwa$handleAllowedSuggestions(ServerboundCommandSuggestionPacket packet, CallbackInfo ci) {
		ServerGamePacketListenerImpl listener = (ServerGamePacketListenerImpl) (Object) this;
		ServerPlayer player = listener.player;
		if (player == null) {
			return;
		}

		MinecraftServer server = player.level().getServer();
		if (server == null) {
			return;
		}

		CommandSourceStack source = player.createCommandSourceStack();
		if (!AllowManager.isAllowed(source, AllowManager.commandRoot(packet.command()))) {
			return;
		}

		StringReader command = new StringReader(packet.command());
		if (command.canRead() && command.peek() == '/') {
			command.skip();
		}

		CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
		ParseResults<CommandSourceStack> parse = dispatcher.parse(
				command,
				source.withPermission(PermissionSet.ALL_PERMISSIONS)
		);
		dispatcher.getCompletionSuggestions(parse).thenAccept(results -> {
			Suggestions suggestions = results.getList().size() <= 1000
					? results
					: new Suggestions(results.getRange(), results.getList().subList(0, 1000));
			listener.send(new ClientboundCommandSuggestionsPacket(packet.id(), suggestions));
		});
		ci.cancel();
	}
}
