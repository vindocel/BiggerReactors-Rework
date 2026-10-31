package biggerreactorsrework.client.guide;

import biggerreactorsrework.calculator.CalculationResult;
import biggerreactorsrework.client.calculator.CalculatorClient;
import net.minecraft.client.gui.components.EditBox;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Transient calculator state while the guide is open: form, tabs, the result on display and scroll positions.
 * It survives window resizes (the book is rebuilt, this is not) and is discarded when the guide closes.
 */
final class CalculatorSession {
    enum Tab { NEW, RECENT, SAVED }
    enum View { SUMMARY, REACTOR, EXCHANGER, TURBINE, CONNECTIONS }

    static Tab tab = Tab.NEW;
    @Nullable static EditBox target;
    static boolean liquid;
    @Nullable static String fuel, moderator, coolant, coil;
    /** Result on the right page and whether it came from the form (it is cleared when the form changes). */
    @Nullable static CalculationResult shown;
    static boolean shownFromForm;
    @Nullable static CalculationResult lastReceived;
    static View view = View.SUMMARY;
    static int scroll, listScroll, legendScroll;
    /** Selector kind whose list is open, -1 when none, and the list's first visible row. */
    static int dropdown = -1, dropScroll;
    static boolean layout;
    /** Machine index in the layout view; -1 for the whole plant. */
    static int machine = -1;
    static final Map<String, GuideMultiblock.Spread> PREVIEWS = new HashMap<>();

    private CalculatorSession() { }

    static void show(CalculationResult result, boolean fromForm) {
        shown = result;
        shownFromForm = fromForm;
        view = View.SUMMARY;
        scroll = 0;
        layout = false;
        machine = -1;
        legendScroll = 0;
    }

    /** The form changed: a result computed for the previous values no longer describes it. */
    static void formChanged() {
        if (shownFromForm) {
            shown = null;
            layout = false;
        }
    }

    static void closed() {
        CalculatorClient.abandon();
        tab = Tab.NEW;
        target = null;
        liquid = false;
        fuel = moderator = coolant = coil = null;
        shown = null;
        shownFromForm = false;
        lastReceived = null;
        view = View.SUMMARY;
        scroll = listScroll = legendScroll = dropScroll = 0;
        dropdown = -1;
        layout = false;
        machine = -1;
        PREVIEWS.clear();
    }
}
