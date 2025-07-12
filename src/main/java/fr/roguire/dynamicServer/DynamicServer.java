package fr.roguire.dynamicServer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.leangen.geantyref.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import fr.roguire.dynamicServer.listeners.OnPlayerKickListener;

@Plugin(id = "dynamicserver", name = "DynamicServer", version = "1.0-SNAPSHOT", authors = {"Roguire14"})
public class DynamicServer {

    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private final OkHttpClient client = new OkHttpClient();
    private final Request request = new Request.Builder().url("http://localhost:25550/get-servers").build();
    private final Set<String> knownServers = new HashSet<>();
    private final Gson gson = new Gson();

    @Inject
    public DynamicServer(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getEventManager().register(this, new OnPlayerKickListener(server));
        server.getScheduler().buildTask(this, this::checkServers).repeat(1, TimeUnit.SECONDS).schedule();
    }



    private void checkServers() {
        List<ServerData> activeServers = fetchServers();
        if(activeServers == null) {
            logger.info("No active servers found");
            return;
        }
        Set<String> activeServerNames = new HashSet<>();

        for (ServerData server : activeServers) {
            activeServerNames.add(server.name);
            if (!knownServers.contains(server.name)) {
                addServer(server.name, server.port);
            }
        }

        knownServers.removeIf(serverName -> {
            if (!activeServerNames.contains(serverName)) {
                removeServer(serverName);
                return true;
            }
            return false;
        });
    }

    public void addServer(String name, int port){
        ServerInfo serverInfo = new ServerInfo(name, new InetSocketAddress("localhost", port));
        server.registerServer(serverInfo);
        knownServers.add(name);
        logger.info("Serveur ajouté: {}", serverInfo);
    }

    private void removeServer(String name) {
        Optional<RegisteredServer> server = this.server.getServer(name);
        server.ifPresent(s -> this.server.unregisterServer(s.getServerInfo()));
        logger.info("Serveur supprimé : {}", name);
    }

    private List<ServerData> fetchServers(){
        try(Response response = client.newCall(request).execute()) {
            if(!response.isSuccessful()) return null;
            Type listType = new TypeToken<List<ServerData>>() {}.getType();
            if(response.body() == null) return null;
            JsonObject jsonResponse = JsonParser.parseString(response.body().string()).getAsJsonObject();
            if(jsonResponse.get("code").getAsInt() != 200) return null;
            String message = jsonResponse.get("message").getAsString();
            return gson.fromJson(message, listType);
        } catch (IOException e) {
            return null;
        }
    }

    private static class ServerData {
        String name;
        int port;

        @Override
        public String toString() {
            return "ServerData{"+name+","+port+"}";
        }
    }
}
