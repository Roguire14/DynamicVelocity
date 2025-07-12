package fr.roguire.dynamicServer.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.Optional;

public class OnPlayerKickListener {

    private final ProxyServer proxy;

    public OnPlayerKickListener(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onServerKick(KickedFromServerEvent event) {
        Optional<Component> reason = event.getServerKickReason();
        if (reason.isPresent() && reason.get().toString().contains("closed")) {
            Optional<RegisteredServer> hub = proxy.getServer("hub");
            if (hub.isPresent()) {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(hub.get()));
            } else {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(reason.get()));
            }
        }
    }
}
