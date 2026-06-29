package com.oscimate.firorize.config;

import com.google.gson.Gson;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.collections4.map.ListOrderedMap;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    public long currentFireHeightSlider = -1;

    // Total time (ms) the player has spent on Firorize config screens, accumulated across sessions and
    // persisted in firorize.json. Drives the periodic Ko-fi donation popup (see DonationTracker).
    public long accumulatedConfigTimeMs = 0;
    // How many donation popups have already been shown; the next one fires once accumulatedConfigTimeMs
    // crosses (donationPopupsShown + 1) * DonationTracker.THRESHOLD_MS.
    public int donationPopupsShown = 0;

    public long getAccumulatedConfigTimeMs() {
        return accumulatedConfigTimeMs;
    }

    public void setAccumulatedConfigTimeMs(long accumulatedConfigTimeMs) {
        this.accumulatedConfigTimeMs = accumulatedConfigTimeMs;
    }

    public int getDonationPopupsShown() {
        return donationPopupsShown;
    }

    public void setDonationPopupsShown(int donationPopupsShown) {
        this.donationPopupsShown = donationPopupsShown;
    }

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> getFireColorPresets() {
        return fireColorPresets;
    }
    public ListOrderedMap<String, int[]> customColorPresets;

    public ListOrderedMap<String, int[]> getCustomColorPresets() {
        return customColorPresets;
    }

    public void setCustomColorPresets(ListOrderedMap<String, int[]> customColorPresets) {
        this.customColorPresets = customColorPresets;
    }

    public String getCurrentPreset() {
        return currentPreset;
    }

    public void setCurrentPreset(String currentPreset) {
        this.currentPreset = currentPreset;
    }

    public String currentPreset;

    public void setFireColorPresets(ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets) {
        this.fireColorPresets = fireColorPresets;
    }

    public ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> fireColorPresets;

    public void setCurrentBlockFireColors(KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors) {
        this.blockFireColors = blockFireColors;
    }

    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> blockFireColors;

    public ArrayList<Integer> getPriorityOrder() {
        return priorityOrder;
    }

    public void setPriorityOrder(ArrayList<Integer> priorityOrder) {
        this.priorityOrder = priorityOrder;
    }

    public ArrayList<Integer> priorityOrder;

    // Names of local profiles that were imported from the online gallery; drives the globe marker in
    // the preset list. Persisted in firorize.json. LinkedHashSet keeps a stable order for the JSON.
    public LinkedHashSet<String> importedProfiles;

    public LinkedHashSet<String> getImportedProfiles() {
        return importedProfiles;
    }

    public void setImportedProfiles(LinkedHashSet<String> importedProfiles) {
        this.importedProfiles = importedProfiles;
    }

    // Author (Minecraft username) of each imported profile, keyed by local profile name; powers the
    // "Created by …" globe tooltip. Kept in lockstep with importedProfiles. Persisted in firorize.json.
    public LinkedHashMap<String, String> importedAuthors;

    public LinkedHashMap<String, String> getImportedAuthors() {
        return importedAuthors;
    }

    public void setImportedAuthors(LinkedHashMap<String, String> importedAuthors) {
        this.importedAuthors = importedAuthors;
    }

    // Subset of importedProfiles that arrived via the Inbox (sent by another player). Drives the
    // person silhouette + "Sent by …" marker instead of the globe + "Created by …". Persisted.
    public LinkedHashSet<String> inboxImports;

    public LinkedHashSet<String> getInboxImports() {
        return inboxImports;
    }

    public void setInboxImports(LinkedHashSet<String> inboxImports) {
        this.inboxImports = inboxImports;
    }

    // Names of the profiles that are currently *active* (applied to in-world fire). Multiple may be
    // active at once; when more than one maps the same block/tag/biome, the one earliest in the
    // fireColorPresets order (top of the list) wins. Independent of currentPreset, which is only the
    // profile being edited in the colour wheel. Persisted in firorize.json.
    public LinkedHashSet<String> activeProfiles;

    public LinkedHashSet<String> getActiveProfiles() {
        return activeProfiles;
    }

    public void setActiveProfiles(LinkedHashSet<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    // Subset of importedProfiles that came from the curated built-in gallery. Drives the built-in
    // marker + "Built-in profile" tooltip instead of the globe/person markers. Persisted.
    public LinkedHashSet<String> builtinImports;

    public LinkedHashSet<String> getBuiltinImports() {
        return builtinImports;
    }

    public void setBuiltinImports(LinkedHashSet<String> builtinImports) {
        this.builtinImports = builtinImports;
    }

    // Each profile is exactly one type: 0 = block, 1 = tag, 2 = biome. Stored alongside the profile
    // (the serialized profile structure is unchanged for back-compat; only one of its three maps is
    // ever populated). Persisted in firorize.json.
    public LinkedHashMap<String, Integer> profileTypes;

    public LinkedHashMap<String, Integer> getProfileTypes() {
        return profileTypes;
    }

    public void setProfileTypes(LinkedHashMap<String, Integer> profileTypes) {
        this.profileTypes = profileTypes;
    }

    /** The type (0 block / 1 tag / 2 biome) of a local profile: the stored value, else derived from
     *  whichever of its maps is non-empty, else block. */
    public int getProfileType(String name) {
        Integer t = profileTypes == null ? null : profileTypes.get(name);
        if (t != null) return t;
        KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile =
                fireColorPresets == null ? null : fireColorPresets.get(name);
        int d = profile == null ? -1 : deriveType(profile);
        return d < 0 ? 0 : d;
    }

    /** Index (0/1/2) of the first non-empty category map in a profile, or -1 if all are empty. */
    public static int deriveType(KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile) {
        ArrayList<ListOrderedMap<String, int[]>> maps = profile.getLeft().getLeft();
        for (int i = 0; i < 3 && i < maps.size(); i++) {
            if (!maps.get(i).isEmpty()) return i;
        }
        return -1;
    }

    public KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]> getCurrentBlockFireColors() {
        return blockFireColors;
    }

    private static final Gson GSON = new Gson();
    public static Path file = FabricLoader.getInstance().getConfigDir().resolve("firorize" + ".json");

    public long getCurrentFireHeightSlider() {
        return this.currentFireHeightSlider;
    }
    public void setCurrentFireHeightSlider(long fireHeightSlider) {

        this.currentFireHeightSlider =  fireHeightSlider;
    }
    public Boolean fileExists() {
        return Files.exists(file);
    }

    public KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> getDefaultProfile() {
        ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();
        ListOrderedMap<String, int[]> soulStuff = new ListOrderedMap<>();
        soulStuff.put("minecraft:soul_sand", new int[]{-15171708,-14766934});
        soulStuff.put("minecraft:soul_soil", new int[]{-15171708,-14766934});
        temp.add(soulStuff);
        temp.add(new ListOrderedMap<String, int[]>());
        temp.add(new ListOrderedMap<String, int[]>());
        ArrayList<Integer> temp2 = new ArrayList<>();
        temp2.add(0);
        temp2.add(1);
        temp2.add(2);

        return KeyValuePair.of(KeyValuePair.of(temp, new int[]{-7456000,-6456034}), temp2);
    }

    public void getStartupConfig() {
        FireLogicConfig jsonOutput = null;
        if(fileExists()) {
            try (Reader reader = Files.newBufferedReader(file)) {
                jsonOutput = GSON.fromJson(reader, FireLogicConfig.class);
            } catch (IOException e) {

            }
        }

        if(jsonOutput.getFireHeightSlider() > 100 || jsonOutput.getFireHeightSlider() < 0) {
            setCurrentFireHeightSlider(100);
            save();
        } else {
            setCurrentFireHeightSlider(jsonOutput.getFireHeightSlider());
        }
        if(jsonOutput.getCustomColorPresets() == null || jsonOutput.getCustomColorPresets().size() == 0) {
            ListOrderedMap<String, int[]> map = new ListOrderedMap<>();
            map.put("RED", new int[]{-9628645,-7655374});
            map.put("ORANGE", new int[]{-7456000,-6456034});
            map.put("GRAY", new int[]{-12569022,-11185318});
            map.put("BLUE", new int[]{-15171708,-14766934});
            map.put("YELLOW", new int[]{-6584292,-5068772});
            map.put("PURPLE", new int[]{-12446675,-10870735});

            setCustomColorPresets(map);
            save();
        } else {
            setCustomColorPresets(jsonOutput.getCustomColorPresets());
        }
        if (jsonOutput.getCurrentBlockFireColours() == null || jsonOutput.getCurrentBlockFireColours().getLeft().isEmpty()) {
            ArrayList<ListOrderedMap<String, int[]>> temp = new ArrayList<ListOrderedMap<String, int[]>>();

            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            temp.add(new ListOrderedMap<String, int[]>());
            setCurrentBlockFireColors(KeyValuePair.of(temp, new int[]{-7456000,-6456034}));
            save();
        } else {
            setCurrentBlockFireColors(jsonOutput.getCurrentBlockFireColours());
        }
        if (jsonOutput.getPriorityOrder() == null || jsonOutput.getPriorityOrder().size() == 0) {
            ArrayList<Integer> temp = new ArrayList<>();
            temp.add(0);
            temp.add(1);
            temp.add(2);
            setPriorityOrder(temp);
            save();
        } else {
            setPriorityOrder(jsonOutput.getPriorityOrder());
        }
        if (jsonOutput.getFireColorPresets() == null || jsonOutput.getFireColorPresets().size() == 0) {


            ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>,  int[]>, ArrayList<Integer>>> map = new ListOrderedMap<>();

            ListOrderedMap<String, int[]> test = new ListOrderedMap<>();
            test.put("minecraft:crimson_forest", new int[]{-10154472,-7650023});
            test.put("minecraft:warped_forest", new int[]{-14190522,-7562173});
            test.put("minecraft:basalt_deltas", new int[]{-12633022,-11185318});
            test.put("minecraft:soul_sand_valley", new int[]{-15171708,-14766934});

            ArrayList<Integer> temp22 = new ArrayList<>();
            temp22.add(2);
            temp22.add(0);
            temp22.add(1);

            // Single-type biome profile (block/tag maps left empty); "Soul Fire" already covers the
            // soul blocks as a block profile.
            ArrayList<ListOrderedMap<String, int[]>> eeka = new ArrayList<>();
            eeka.add(new ListOrderedMap<String, int[]>());
            eeka.add(new ListOrderedMap<String, int[]>());
            eeka.add(test);
            KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]> mapp2 = KeyValuePair.of(eeka,  new int[]{-7456000,-6456034});

            map.put("Soul Fire", getDefaultProfile());
            map.put("Nether Biomes", KeyValuePair.of(mapp2, temp22));

            setFireColorPresets(map);
            save();
        } else {
            setFireColorPresets(jsonOutput.getFireColorPresets());
        }
        if(jsonOutput.getCurrentPreset() == null || jsonOutput.getCurrentPreset().equals("")) {
            setCurrentPreset("Soul Fire");
            save();
        } else {
            setCurrentPreset(jsonOutput.getCurrentPreset());
        }
        if (jsonOutput.getImportedProfiles() == null) {
            setImportedProfiles(new LinkedHashSet<>());
            save();
        } else {
            setImportedProfiles(jsonOutput.getImportedProfiles());
        }
        if (jsonOutput.getImportedAuthors() == null) {
            setImportedAuthors(new LinkedHashMap<>());
            save();
        } else {
            setImportedAuthors(jsonOutput.getImportedAuthors());
        }
        if (jsonOutput.getInboxImports() == null) {
            setInboxImports(new LinkedHashSet<>());
            save();
        } else {
            setInboxImports(jsonOutput.getInboxImports());
        }
        if (jsonOutput.getBuiltinImports() == null) {
            setBuiltinImports(new LinkedHashSet<>());
            save();
        } else {
            setBuiltinImports(jsonOutput.getBuiltinImports());
        }
        setProfileTypes(jsonOutput.getProfileTypes() == null ? new LinkedHashMap<>() : jsonOutput.getProfileTypes());
        // Active profiles: absent in pre-multi-profile configs → migrate by activating the single
        // profile that used to be "the selected one", so existing setups render identically.
        if (jsonOutput.getActiveProfiles() == null || jsonOutput.getActiveProfiles().isEmpty()) {
            LinkedHashSet<String> active = new LinkedHashSet<>();
            String cur = getCurrentPreset();
            if (cur != null && getFireColorPresets().containsKey(cur)) {
                active.add(cur);
            } else if (!getFireColorPresets().isEmpty()) {
                active.add(getFireColorPresets().keyList().get(0));
            }
            setActiveProfiles(active);
            save();
        } else {
            // Drop any stale names that no longer correspond to an existing profile.
            LinkedHashSet<String> active = new LinkedHashSet<>();
            for (String name : jsonOutput.getActiveProfiles()) {
                if (getFireColorPresets().containsKey(name)) active.add(name);
            }
            setActiveProfiles(active);
        }
        // The old undeletable default profile was named "Initial"; it's now the deletable "Soul Fire".
        // Rename it in place on existing configs so its colours/active state carry over seamlessly.
        renameProfile("Initial", "Soul Fire");
        // Split any legacy multi-category profiles into one single-type profile each, and record every
        // profile's type. Runs after all flag sets are loaded so it can carry them onto the splits.
        migrateSingleTypeProfiles();
        // Primitives: absent in older configs → default 0, which is the correct starting state.
        setAccumulatedConfigTimeMs(jsonOutput.getAccumulatedConfigTimeMs());
        setDonationPopupsShown(jsonOutput.getDonationPopupsShown());
    }

    private static final String[] TYPE_LABELS = {"Block", "Tag", "Biome"};

    /**
     * Enforces the single-type model on load: a profile that fills more than one category map (and has
     * no recorded type yet) is split into one profile per non-empty category ("<name> (Block|Tag|Biome)"),
     * preserving its colours and base; every profile ends up with a recorded {@link #profileTypes} entry.
     * The active/imported/inbox/built-in flags and {@code currentPreset} are carried onto the splits.
     */
    private void migrateSingleTypeProfiles() {
        if (profileTypes == null) profileTypes = new LinkedHashMap<>();
        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> presets = getFireColorPresets();
        if (presets == null) return;

        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> rebuilt = new ListOrderedMap<>();
        LinkedHashMap<String, List<String>> renames = new LinkedHashMap<>();
        boolean changed = false;

        for (String name : new ArrayList<>(presets.keyList())) {
            KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>> profile = presets.get(name);
            ArrayList<ListOrderedMap<String, int[]>> maps = profile.getLeft().getLeft();
            List<Integer> nonEmpty = new ArrayList<>();
            for (int i = 0; i < 3 && i < maps.size(); i++) if (!maps.get(i).isEmpty()) nonEmpty.add(i);
            Integer existing = profileTypes.get(name);

            if (existing == null && nonEmpty.size() > 1) {
                List<String> newNames = new ArrayList<>();
                for (int cat : nonEmpty) {
                    String newName = uniqueName(name + " (" + TYPE_LABELS[cat] + ")", rebuilt);
                    ArrayList<ListOrderedMap<String, int[]>> nm = new ArrayList<>();
                    for (int i = 0; i < 3; i++) nm.add(i == cat ? copyMap(maps.get(i)) : new ListOrderedMap<>());
                    rebuilt.put(newName, KeyValuePair.of(KeyValuePair.of(nm, profile.getLeft().getRight().clone()),
                            new ArrayList<>(List.of(0, 1, 2))));
                    profileTypes.put(newName, cat);
                    newNames.add(newName);
                }
                renames.put(name, newNames);
                profileTypes.remove(name);
                changed = true;
            } else {
                int type = existing != null ? existing : (nonEmpty.isEmpty() ? 0 : nonEmpty.get(0));
                rebuilt.put(name, profile);
                if (!Integer.valueOf(type).equals(profileTypes.get(name))) {
                    profileTypes.put(name, type);
                    changed = true;
                }
            }
        }

        if (!changed) return;
        setFireColorPresets(rebuilt);
        for (Map.Entry<String, List<String>> e : renames.entrySet()) {
            String old = e.getKey();
            List<String> news = e.getValue();
            renameInSet(activeProfiles, old, news);
            renameInSet(importedProfiles, old, news);
            renameInSet(inboxImports, old, news);
            renameInSet(builtinImports, old, news);
            if (importedAuthors != null && importedAuthors.containsKey(old)) {
                String author = importedAuthors.remove(old);
                for (String n : news) importedAuthors.put(n, author);
            }
            if (old.equals(currentPreset)) currentPreset = news.get(0);
        }
        save();
    }

    private static void renameInSet(LinkedHashSet<String> set, String old, List<String> news) {
        if (set != null && set.remove(old)) set.addAll(news);
    }

    /**
     * Renames a profile, preserving its position in the order and carrying over every side-table entry
     * (type, active/imported/inbox/built-in flags, author, currentPreset). No-op if {@code old} is
     * absent or {@code now} is already taken (so a user-created "Soul Fire" is never clobbered).
     */
    private void renameProfile(String old, String now) {
        if (fireColorPresets == null || !fireColorPresets.containsKey(old) || fireColorPresets.containsKey(now)) return;
        ListOrderedMap<String, KeyValuePair<KeyValuePair<ArrayList<ListOrderedMap<String, int[]>>, int[]>, ArrayList<Integer>>> rebuilt = new ListOrderedMap<>();
        for (String name : new ArrayList<>(fireColorPresets.keyList())) {
            rebuilt.put(name.equals(old) ? now : name, fireColorPresets.get(name));
        }
        setFireColorPresets(rebuilt);
        renameInSet(activeProfiles, old, List.of(now));
        renameInSet(importedProfiles, old, List.of(now));
        renameInSet(inboxImports, old, List.of(now));
        renameInSet(builtinImports, old, List.of(now));
        if (profileTypes != null && profileTypes.containsKey(old)) profileTypes.put(now, profileTypes.remove(old));
        if (importedAuthors != null && importedAuthors.containsKey(old)) importedAuthors.put(now, importedAuthors.remove(old));
        if (old.equals(currentPreset)) currentPreset = now;
        save();
    }

    private static ListOrderedMap<String, int[]> copyMap(ListOrderedMap<String, int[]> src) {
        ListOrderedMap<String, int[]> dst = new ListOrderedMap<>();
        for (String k : src.keyList()) dst.put(k, src.get(k).clone());
        return dst;
    }

    private static String uniqueName(String base, ListOrderedMap<String, ?> taken) {
        if (!taken.containsKey(base)) return base;
        int n = 2;
        while (taken.containsKey(base + " (" + n + ")")) n++;
        return base + " (" + n + ")";
    }

    public void save() {
        try {
            Files.writeString(file, GSON.toJson(new FireLogicConfig()));
        } catch (IOException e) {

        }
    }
}
