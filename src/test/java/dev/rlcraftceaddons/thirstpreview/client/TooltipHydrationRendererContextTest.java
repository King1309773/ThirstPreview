package dev.rlcraftceaddons.thirstpreview.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Forge event objects; no Minecraft window or OpenGL context is required. */
public class TooltipHydrationRendererContextTest {
    @BeforeClass public static void bootstrap() { Bootstrap.register(); }

    private List<String> tooltip(int amount) {
        return Arrays.asList("Food", TooltipHydrationSnapshot.create(amount, 48, 4));
    }

    @Test public void listOnlyPostTextKeepsTheGeneratedHydrationAmount() {
        RenderTooltipEvent.PostText event = new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, tooltip(3), 10, 20, null, 60, 20);
        assertTrue(event.getStack().isEmpty());
        assertEquals(Integer.valueOf(3), TooltipHydrationRenderer.renderedAmount(event));
    }

    @Test public void listOnlyEventDataKeepsTheSameSnapshotWithoutAStack() {
        RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(
                ItemStack.EMPTY, new ArrayList<>(tooltip(6)), 10, 20, 320, 240, -1, null);
        assertEquals(Integer.valueOf(6), TooltipHydrationRenderer.renderedAmount(event));
    }

    @Test public void laterTooltipsCannotOverwriteAnEarlierCopiedTooltip() {
        List<String> earlier = Collections.unmodifiableList(new ArrayList<>(tooltip(1)));
        RenderTooltipEvent.PostText later = new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, tooltip(8), 10, 20, null, 60, 20);
        assertEquals(Integer.valueOf(8), TooltipHydrationRenderer.renderedAmount(later));
        RenderTooltipEvent.PostText earlierEvent = new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, earlier, 10, 20, null, 60, 20);
        assertEquals(Integer.valueOf(1), TooltipHydrationRenderer.renderedAmount(earlierEvent));
    }

    @Test public void anUnrelatedRenderStackCannotReplaceTheRowsOwnValues() {
        RenderTooltipEvent.PostText event = new RenderTooltipEvent.PostText(
                new ItemStack(Items.STICK), tooltip(14), 10, 20, null, 60, 20);
        assertEquals(Integer.valueOf(14), TooltipHydrationRenderer.renderedAmount(event));
    }

    @Test public void refinedStorageFooterPreservesTheSnapshotAndMainRowPosition() {
        List<String> lines = Arrays.asList("Food", "Description", "Mod", TooltipHydrationSnapshot.create(8, 48, 4));
        RenderTooltipEvent.PostText event = new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, lines, 10, 20, null, 60, 60);
        assertEquals(Integer.valueOf(8), TooltipHydrationRenderer.renderedAmount(event));
        assertEquals(51, TooltipHydrationLayout.iconTop(event.getY(), event.getHeight(), 3, event.getLines().size()));
    }

    @Test public void unrelatedOrAmbiguousTooltipsDoNotBorrowAValue() {
        RenderTooltipEvent.PostText unrelated = new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, Arrays.asList("Task name"), 10, 20, null, 60, 8);
        assertNull(TooltipHydrationRenderer.renderedAmount(unrelated));
        List<String> ambiguous = Arrays.asList("Food", TooltipHydrationSnapshot.create(1, 48, 4),
                TooltipHydrationSnapshot.create(8, 48, 4));
        assertNull(TooltipHydrationRenderer.renderedAmount(new RenderTooltipEvent.PostText(
                ItemStack.EMPTY, ambiguous, 10, 20, null, 60, 30)));
    }
}
