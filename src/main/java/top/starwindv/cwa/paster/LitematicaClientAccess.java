package top.starwindv.cwa.paster;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class LitematicaClientAccess implements ClientAccess {
	@Override
	public Object minecraft() {
		return Minecraft.getInstance();
	}

	@Override
	public boolean hasSingleplayerServer(Object minecraft) {
		return ((Minecraft) minecraft).hasSingleplayerServer();
	}

	@Override
	public Object level(Object minecraft) {
		return ((Minecraft) minecraft).level;
	}

	@Override
	public void execute(Object minecraft, Runnable task) {
		((Minecraft) minecraft).execute(task);
	}

	@Override
	public void displayMessage(Object minecraft, Component message) {
		Minecraft client = (Minecraft) minecraft;
		if (client.player != null) {
			client.player.sendSystemMessage(message);
		}
	}
}
