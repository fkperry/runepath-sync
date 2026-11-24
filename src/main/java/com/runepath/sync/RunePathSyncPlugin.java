package com.runepath.sync;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Quest;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import okhttp3.OkHttpClient;

/**
 * Opt-in character progress sync. RuneLite state is read on the client thread;
 * third-party HTTP requests happen only on the worker thread.
 */
@PluginDescriptor(
    name = "RunePath Sync",
    description = "Opt-in OSRS progression sync to a third-party RunePath account"
)
public class RunePathSyncPlugin extends Plugin
{
    private static final String GROUP = "runepathsync";
    private static final String TOKEN_KEY = "deviceToken";
    private static final String CHARACTER_KEY = "linkedCharacter";
    private static final long PERIODIC_SECONDS = 300L;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ConfigManager configManager;
    @Inject private RunePathSyncConfig config;
    @Inject private ClientToolbar toolbar;
    @Inject private OkHttpClient http;

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> pairingPoll;
    private RunePathSyncPanel panel;
    private RunePathAchievements achievements;
    private NavigationButton navigation;
    private volatile String deviceCode;
    private volatile String pendingCharacter;
    private final AtomicBoolean queued = new AtomicBoolean(false);
    private final AtomicBoolean inFlight = new AtomicBoolean(false);
    private final AtomicLong lastQueue = new AtomicLong(0);

    @Provides
    RunePathSyncConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(RunePathSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        executor = Executors.newSingleThreadScheduledExecutor(task ->
        {
            Thread thread = new Thread(task, "runepath-worker");
            thread.setDaemon(true);
            return thread;
        });
        achievements = new RunePathAchievements(client);
        panel = new RunePathSyncPanel(this::startPairing, this::queueSync, this::forgetLocalToken);
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gfx = image.createGraphics();
        gfx.setColor(new Color(25, 49, 76));
        gfx.fillRoundRect(0, 0, 32, 32, 9, 9);
        gfx.setColor(new Color(96, 179, 239));
        gfx.drawString("R", 10, 22);
        gfx.dispose();
        navigation = NavigationButton.builder()
            .tooltip("RunePath Sync").icon(image).priority(6).panel(panel).build();
        toolbar.addNavigation(navigation);
        panel.status(isLinked() ? "Paired; enable sync in settings" : "Not connected");
        executor.scheduleAtFixedRate(this::queueSync, PERIODIC_SECONDS, PERIODIC_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected void shutDown()
    {
        deviceCode = null;
        if (pairingPoll != null) pairingPoll.cancel(false);
        if (navigation != null) toolbar.removeNavigation(navigation);
        if (executor != null) executor.shutdownNow();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGIN_SCREEN && achievements != null) achievements.reset();
        if (event.getGameState() == GameState.LOGGED_IN) queueSync();
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        queueSync();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event)
    {
        // Debounced; quest varbits change independently of skills.
        queueSync();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == 741 && achievements != null)
        {
            // The diary journal only exposes named task rows when opened in game.
            clientThread.invokeLater(() -> achievements.observeDiaryJournal());
        }
    }

    private RunePathHttp api()
    {
        try
        {
            URI uri = URI.create(config.serverUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
                uri.getUserInfo() != null || uri.getQuery() != null ||
                uri.getFragment() != null || !(uri.getPath().isEmpty() || "/".equals(uri.getPath())))
            {
                throw new IllegalArgumentException();
            }
            return new RunePathHttp(http, "https://" + uri.getAuthority());
        }
        catch (RuntimeException e)
        {
            throw new IllegalArgumentException("Configure the published RunePath HTTPS origin first.");
        }
    }

    private String localToken()
    {
        return configManager.getConfiguration(GROUP, TOKEN_KEY);
    }

    private boolean isLinked()
    {
        String token = localToken();
        return token != null && !token.isEmpty();
    }

    private void startPairing()
    {
        if (!config.allowProgressSync())
        {
            panel.status("Enable third-party sync in plugin settings");
            return;
        }
        try { api(); } catch (IllegalArgumentException e) { panel.status(e.getMessage()); return; }

        clientThread.invokeLater(() ->
        {
            if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
            {
                panel.status("Log into your OSRS character first");
                return;
            }
            String name = client.getLocalPlayer().getName();
            if (name == null || name.trim().isEmpty()) return;
            if (executor != null && !executor.isShutdown()) executor.execute(() -> requestCode(name));
        });
    }

    private void requestCode(String name)
    {
        try
        {
            JsonObject body = new JsonObject();
            body.addProperty("schemaVersion", 1);
            body.addProperty("displayName", name);
            body.addProperty("deviceLabel", "RuneLite desktop");
            RunePathHttp.Result result = api().post("pair_start", body, null);
            if (result.status != 201)
            {
                panel.status("Pairing rejected (HTTP " + result.status + ")");
                return;
            }
            deviceCode = result.field("deviceCode");
            pendingCharacter = name;
            panel.code(result.field("userCode"));
            panel.status("Approve code in RunePath Account");
            if (pairingPoll != null) pairingPoll.cancel(false);
            pairingPoll = executor.scheduleAtFixedRate(this::pollPairing, 6, 6, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            panel.status("Could not reach RunePath server");
        }
    }

    private void pollPairing()
    {
        String code = deviceCode;
        if (code == null) return;
        try
        {
            JsonObject body = new JsonObject();
            body.addProperty("deviceCode", code);
            RunePathHttp.Result result = api().post("pair_token", body, null);
            if (result.status == 202 || result.status == 429) return;
            if (result.status == 200 && "connected".equals(result.field("status")))
            {
                String token = result.field("accessToken");
                if (token.isEmpty()) throw new IllegalStateException("Missing token");
                configManager.setConfiguration(GROUP, TOKEN_KEY, token);
                configManager.setConfiguration(GROUP, CHARACTER_KEY, pendingCharacter);
                panel.status("Paired: " + pendingCharacter);
                panel.code("Connected");
                deviceCode = null;
                pendingCharacter = null;
                if (pairingPoll != null) pairingPoll.cancel(false);
                queueSync();
            }
            else if (result.status == 404 || result.status == 410)
            {
                panel.status("Pairing expired; start again");
                panel.code("No active code");
                deviceCode = null;
                if (pairingPoll != null) pairingPoll.cancel(false);
            }
            else
            {
                panel.status("Pairing HTTP " + result.status);
            }
        }
        catch (Exception e)
        {
            panel.status("Pairing connection unavailable; retrying");
        }
    }

    private void forgetLocalToken()
    {
        configManager.unsetConfiguration(GROUP, TOKEN_KEY);
        configManager.unsetConfiguration(GROUP, CHARACTER_KEY);
        panel.status("Token forgotten. Revoke device in RunePath too.");
        panel.code("Disconnected");
    }

    private void queueSync()
    {
        if (executor == null || executor.isShutdown() || !config.allowProgressSync() || !isLinked()) return;
        long now = System.currentTimeMillis();
        if (now - lastQueue.get() < 60_000) return;
        if (!queued.compareAndSet(false, true)) return;
        lastQueue.set(now);
        executor.schedule(() ->
        {
            queued.set(false);
            clientThread.invokeLater(this::captureAndSend);
        }, 10, TimeUnit.SECONDS);
    }

    private void captureAndSend()
    {
        if (client.getGameState() != GameState.LOGGED_IN ||
            client.getLocalPlayer() == null || !config.allowProgressSync() || !isLinked()) return;
        String current = client.getLocalPlayer().getName();
        String linked = configManager.getConfiguration(GROUP, CHARACTER_KEY);
        if (current == null || linked == null || !current.equalsIgnoreCase(linked))
        {
            panel.status("Character changed; pair this character separately");
            return;
        }
        if (!inFlight.compareAndSet(false, true)) return;
        JsonObject payload;
        try
        {
            payload = capture(current);
        }
        catch (RuntimeException e)
        {
            inFlight.set(false);
            panel.status("Unable to capture game state yet");
            return;
        }
        String token = localToken();
        executor.execute(() ->
        {
            try
            {
                RunePathHttp.Result response = api().post("sync", payload, token);
                if (response.status == 200)
                {
                    panel.status("Synced " + Instant.now());
                }
                else if (response.status == 401 || response.status == 403)
                {
                    panel.status("Connection revoked or character mismatch");
                }
                else
                {
                    panel.status("Sync HTTP " + response.status);
                }
            }
            catch (Exception e)
            {
                panel.status("Network error; retry on next sync");
            }
            finally
            {
                inFlight.set(false);
            }
        });
    }

    private JsonObject capture(String character)
    {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        root.addProperty("observedAt", Instant.now().toString());
        JsonObject who = new JsonObject();
        who.addProperty("displayName", character);
        root.add("character", who);

        JsonObject datasets = new JsonObject();
        JsonObject skillData = new JsonObject();
        skillData.addProperty("mode", "full");
        JsonArray skillRows = new JsonArray();
        for (Skill skill : Skill.values())
        {
            if ("OVERALL".equals(skill.name())) continue;
            int level = client.getRealSkillLevel(skill);
            int xp = client.getSkillExperience(skill);
            if (level < 1 || xp < 0) continue;
            JsonObject row = new JsonObject();
            row.addProperty("name", skill.getName());
            row.addProperty("level", level);
            row.addProperty("xp", xp);
            skillRows.add(row);
        }
        skillData.add("skills", skillRows);
        if (skillRows.size() > 0) datasets.add("skills", skillData);

        JsonObject questData = new JsonObject();
        questData.addProperty("mode", "full");
        JsonArray questRows = new JsonArray();
        for (Quest quest : Quest.values())
        {
            try
            {
                JsonObject q = new JsonObject();
                q.addProperty("id", quest.getId());
                q.addProperty("name", quest.getName());
                q.addProperty("state", quest.getState(client).name());
                questRows.add(q);
            }
            catch (RuntimeException ignored)
            {
                // Never infer missing state as NOT_STARTED.
            }
        }
        questData.add("quests", questRows);
        if (questRows.size() >= 10) datasets.add("quests", questData);

        // Keep datasets independent: incomplete cache data never wipes older snapshots.
        // A game-cache mismatch must not prevent skills and quest progress from syncing.
        try
        {
            JsonObject diaryData = achievements.captureDiaries();
            if (diaryData != null) datasets.add("diaries", diaryData);
        }
        catch (RuntimeException ignored)
        {
            panel.status("Diary data not ready; preserved previous cloud snapshot");
        }
        try
        {
            JsonObject combatData = achievements.captureCombatAchievements();
            if (combatData != null) datasets.add("combatAchievements", combatData);
        }
        catch (RuntimeException ignored)
        {
            panel.status("Combat achievements not ready; preserved previous snapshot");
        }
        root.add("datasets", datasets);
        return root;
    }
}
