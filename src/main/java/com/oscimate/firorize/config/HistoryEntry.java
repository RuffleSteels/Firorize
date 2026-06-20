package com.oscimate.firorize.config;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.awt.Color;
import java.util.ArrayList;

/**
 * One reversible step in the fire-colour config undo/redo history.
 *
 * <p>Two flavours:
 * <ul>
 *   <li>{@link Type#COLOR} — a colour-wheel/slider adjustment. Wheel edits only change the live
 *       {@code pickedColor} (they are not committed to config until Apply), so this stores the
 *       before/after {@code pickedColor} pair plus which side ({@code overlay}) was edited.</li>
 *   <li>{@link Type#CONFIG} — Apply, reorder, or reset. These all mutate the persisted config, so we
 *       store a deep before/after snapshot of {@code currentBlockFireColors} and the priority order
 *       and simply restore the relevant snapshot on undo/redo.</li>
 * </ul>
 *
 * <p>Every entry also carries the UI navigation context (search tab, base/overlay side, and the
 * target list-entry id) so undo/redo can "click into" the element the action belonged to.
 */
@Environment(value = EnvType.CLIENT)
public class HistoryEntry {
    public enum Type { COLOR, CONFIG }

    public final Type type;

    // Navigation context.
    public final int tab;          // currentSearchButton at action time (0 blocks / 1 tags / 2 biomes)
    public final String target;    // list-entry id to re-select, or null to leave selection alone
    public final boolean overlay;  // isOverlay side to show

    // COLOR payload.
    public final Color[] colorBefore;
    public final Color[] colorAfter;

    // CONFIG payload — deep snapshots of currentBlockFireColors + priority order + custom colour presets.
    public final KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgBefore;
    public final KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgAfter;
    public final ArrayList<Integer> prioBefore;
    public final ArrayList<Integer> prioAfter;
    public final ListOrderedMap<String, int[]> presetBefore;
    public final ListOrderedMap<String, int[]> presetAfter;
    /** When true this entry only touched the custom colour presets — don't disturb list/tab selection. */
    public final boolean presetOnly;

    private HistoryEntry(Type type, int tab, String target, boolean overlay, boolean presetOnly,
                         Color[] colorBefore, Color[] colorAfter,
                         KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgBefore,
                         KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgAfter,
                         ArrayList<Integer> prioBefore, ArrayList<Integer> prioAfter,
                         ListOrderedMap<String, int[]> presetBefore, ListOrderedMap<String, int[]> presetAfter) {
        this.type = type;
        this.tab = tab;
        this.target = target;
        this.overlay = overlay;
        this.presetOnly = presetOnly;
        this.colorBefore = colorBefore;
        this.colorAfter = colorAfter;
        this.cfgBefore = cfgBefore;
        this.cfgAfter = cfgAfter;
        this.prioBefore = prioBefore;
        this.prioAfter = prioAfter;
        this.presetBefore = presetBefore;
        this.presetAfter = presetAfter;
    }

    public static HistoryEntry color(int tab, String target, boolean overlay, Color[] before, Color[] after) {
        return new HistoryEntry(Type.COLOR, tab, target, overlay, false,
                new Color[]{before[0], before[1]}, new Color[]{after[0], after[1]},
                null, null, null, null, null, null);
    }

    public static HistoryEntry config(int tab, String target, boolean overlay, boolean presetOnly,
                                      KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgBefore,
                                      ArrayList<Integer> prioBefore,
                                      ListOrderedMap<String, int[]> presetBefore,
                                      KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> cfgAfter,
                                      ArrayList<Integer> prioAfter,
                                      ListOrderedMap<String, int[]> presetAfter) {
        return new HistoryEntry(Type.CONFIG, tab, target, overlay, presetOnly,
                null, null, cfgBefore, cfgAfter, prioBefore, prioAfter, presetBefore, presetAfter);
    }
}
