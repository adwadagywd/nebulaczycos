package pl.nebula;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import pl.nebula.config.Config;
import pl.nebula.module.ModuleManager;
import pl.nebula.util.Notifications;

public class NebulaClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> ModuleManager.tick());
        WorldRenderEvents.START.register(ctx -> ModuleManager.pollKeys());
        WorldRenderEvents.LAST.register(ModuleManager::render3d);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Notifications.render(ctx));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ModuleManager.onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save());
    }
}
