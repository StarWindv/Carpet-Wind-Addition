package top.starwindv.cwa.paster;

import net.minecraft.network.chat.Component;

public interface ClientAccess {
	Object minecraft();

	boolean hasSingleplayerServer(Object minecraft);

	Object level(Object minecraft);

	void execute(Object minecraft, Runnable task);

	void displayMessage(Object minecraft, Component message);
}
