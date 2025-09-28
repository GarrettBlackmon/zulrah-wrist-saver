package dev.blackmon;

import javax.inject.Inject;
import java.awt.*;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;

@PluginDescriptor(
        name = "Zulrah Wrist Saver (MVP)",
        description = "Highlights Zulrah 4 ticks after it first appears on-screen, hides once you attack.",
        enabledByDefault = false
)
public class ExamplePlugin extends Plugin
{
    // ======= TWEAKS =======
    private static final int DELAY_TICKS = 4;            // 4 ticks = 2.4s
    private static final Color EDGE_COLOR = new Color(0, 255, 0, 255);
    private static final int FILL_ALPHA = 50;

    @Inject private Client client;
    @Inject private OverlayManager overlayManager;

    private final Overlay overlay = new SimpleHullOverlay();

    private NPC zulrah;                  // current Zulrah
    private Integer startTick;           // when we started counting (first on-screen frame)
    private int tickCounter;             // absolute game ticks since plugin start
    private boolean wasOnscreen;         // last frame's on-screen state
    private boolean suppressUntilNextPhase; // becomes true once you target Zulrah; cleared on phase change/dive

    @Override
    protected void startUp()
    {
        resetAll();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(overlay);
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
            // Start timing when it first becomes visible (handled in overlay)
            startTick = null;
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

    // Covers the “phase change” case even if the NPC doesn’t fully despawn between forms.
    @Subscribe
    public void onNpcChanged(NpcChanged e)
    {
        if (zulrah != null && e.getNpc() == zulrah)
        {
            // New form/phase → treat like a fresh cycle
            startTick = null;
            wasOnscreen = false;
            suppressUntilNextPhase = false;
        }
    }

    // Hide highlight once YOU actually target Zulrah (any combat style).
    @Subscribe
    public void onInteractingChanged(InteractingChanged e)
    {
        if (zulrah == null) return;

        final Actor src = e.getSource();
        final Actor tgt = e.getTarget();

        if (src == client.getLocalPlayer() && tgt == zulrah)
        {
            suppressUntilNextPhase = true; // you clicked; no more highlight this phase
        }
    }

    private boolean shouldHighlight(boolean onscreen)
    {
        if (zulrah == null || !onscreen || startTick == null) return false;
        if (suppressUntilNextPhase) return false;

        return (tickCounter - startTick) >= DELAY_TICKS;
    }

    /** Minimal overlay that tracks on-screen transitions and draws a hull after the delay. */
    private final class SimpleHullOverlay extends Overlay
    {
        SimpleHullOverlay()
        {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.ABOVE_SCENE);
        }

        @Override
        public Dimension render(Graphics2D g)
        {
            if (zulrah == null)
            {
                return null;
            }

            // On-screen detection via convex hull
            final Shape hull = zulrah.getConvexHull();
            final boolean onscreen = hull != null;

            // First visible frame → start tick timer
            if (onscreen && !wasOnscreen)
            {
                startTick = tickCounter;
            }
            // Went off-screen (dive) → clear timer & suppression so next emerge is fresh
            if (!onscreen && wasOnscreen)
            {
                startTick = null;
                suppressUntilNextPhase = false;
            }
            wasOnscreen = onscreen;

            if (!shouldHighlight(onscreen))
            {
                return null;
            }

            // Draw translucent hull
            final Color fill = new Color(EDGE_COLOR.getRed(), EDGE_COLOR.getGreen(), EDGE_COLOR.getBlue(), FILL_ALPHA);
            g.setColor(fill);
            g.fill(hull);

            g.setColor(EDGE_COLOR);
            g.setStroke(new BasicStroke(2));
            g.draw(hull);

            return null;
        }
    }
}
