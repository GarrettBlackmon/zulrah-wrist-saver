package dev.blackmon;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;

import com.google.inject.Provides;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@PluginDescriptor(
        name = "Zulrah Wrist Saver",
        description = "Outlines Zulrah after a short delay and hides once you attack.",
        enabledByDefault = false
)
public class ZulrahWristSaver extends Plugin
{
    // MVP: fixed delay and width (color + feather are configurable via config UI)
    private static final int DELAY_TICKS = 4;   // 4 ticks = 2.4s
    private static final int OUTLINE_WIDTH = 3; // constant for MVP (can make configurable later)

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;
    @Inject private ModelOutlineRenderer modelOutlineRenderer;
    @Inject private ZulrahWristSaverConfig config;

    private Overlay overlay;

    private NPC zulrah;                       // current Zulrah instance
    private Integer startTick;                // tick when first on-screen frame occurred
    private int tickCounter;                  // absolute ticks since plugin start
    private boolean wasOnscreen;              // visibility last frame
    private boolean suppressUntilNextPhase;   // true after you target Zulrah (hide outline until next phase)

    @Override
    protected void startUp()
    {
        resetAll();
        overlay = new OutlineOverlay();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        if (overlay != null)
        {
            overlayManager.remove(overlay);
            overlay = null;
        }
        resetAll();
    }

    private void resetAll()
    {
        zulrah = null;
        startTick = null;
        tickCounter = 0;
        wasOnscreen = false;
        suppressUntilNextPhase = false;
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        tickCounter++;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged e)
    {
        GameState s = e.getGameState();
        if (s == GameState.LOADING || s == GameState.HOPPING || s == GameState.LOGIN_SCREEN)
        {
            resetAll();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned e)
    {
        final NPC npc = e.getNpc();
        final String name = npc.getName();
        if (name != null && name.equalsIgnoreCase("Zulrah"))
        {
            zulrah = npc;
            startTick = null;          // start timing on first on-screen frame
            wasOnscreen = false;
            suppressUntilNextPhase = false;
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e)
    {
        if (zulrah != null && e.getNpc() == zulrah)
        {
            zulrah = null;
            startTick = null;
            wasOnscreen = false;
            suppressUntilNextPhase = false;
        }
    }

    // Phase morphs (green/red/blue forms) → treat as a fresh emerge
    @Subscribe
    public void onNpcChanged(NpcChanged e)
    {
        if (zulrah != null && e.getNpc() == zulrah)
        {
            startTick = null;
            wasOnscreen = false;
            suppressUntilNextPhase = false;
        }
    }

    // Hide outline after YOU successfully target Zulrah
    @Subscribe
    public void onInteractingChanged(InteractingChanged e)
    {
        if (zulrah == null) return;

        final Actor src = e.getSource();
        final Actor tgt = e.getTarget();
        if (src == client.getLocalPlayer() && tgt == zulrah)
        {
            suppressUntilNextPhase = true;
        }
    }

    private boolean shouldOutline(boolean onscreen)
    {
        if (zulrah == null || !onscreen || startTick == null) return false;
        if (suppressUntilNextPhase) return false;
        return (tickCounter - startTick) >= DELAY_TICKS;
    }

    /** Overlay that draws a crisp model outline; convex hull is used only to detect visibility. */
    private final class OutlineOverlay extends Overlay
    {
        OutlineOverlay()
        {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.ABOVE_SCENE);
        }

        @Override
        public Dimension render(java.awt.Graphics2D g)
        {
            if (zulrah == null)
            {
                return null;
            }

            // Visibility via convex hull
            final Shape hull = zulrah.getConvexHull();
            final boolean onscreen = hull != null;

            // First visible frame → start timer
            if (onscreen && !wasOnscreen)
            {
                startTick = tickCounter;
            }
            // Went off-screen (dive) → clear for next emerge
            if (!onscreen && wasOnscreen)
            {
                startTick = null;
                suppressUntilNextPhase = false;
            }
            wasOnscreen = onscreen;

            if (!shouldOutline(onscreen))
            {
                return null;
            }

            // Draw model outline with configurable color and feather
            final Color color = config.outlineColor();
            final int feather = clamp(config.outlineFeather(), 0, 12);
            modelOutlineRenderer.drawOutline(zulrah, OUTLINE_WIDTH, color, feather);

            return null;
        }
    }

    private static int clamp(int v, int lo, int hi)
    {
        return Math.max(lo, Math.min(hi, v));
    }

    // Provide the config implementation
    @Provides
    ZulrahWristSaverConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(ZulrahWristSaverConfig.class);
    }
}
