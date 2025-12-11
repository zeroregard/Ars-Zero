package com.arszero.tests;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.slf4j.Logger;

public final class TestRegistrationFilter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Set<Class<?>> ALL_TEST_CLASSES = Set.of(
        WaterVoxelTests.class,
        FireVoxelTests.class,
        ArcaneVoxelTests.class,
        FireWaterVoxelInteractionBehaviour.class,
        ZeroGravityEffectTests.class,
        MultiphaseSpellTurretTests.class,
        WindVoxelWorldInteractionBehaviour.class,
        WindVoxelInteractionBehaviour.class
    );

    private static final Map<String, Class<?>> NAME_LOOKUP = buildNameLookup();
    private static final Set<Class<?>> ALLOWED_CLASSES = resolveAllowedClasses();

    private TestRegistrationFilter() {
    }

    public static void applyFilterToRegistry() {
        if (ALLOWED_CLASSES.isEmpty()) {
            LOGGER.debug("No test filter provided; registry pruning skipped.");
            return;
        }
        GameTestHooks.registerGametests();
        Set<String> allowedBatchNames = ALLOWED_CLASSES.stream()
            .map(Class::getSimpleName)
            .collect(Collectors.toSet());
        Set<TestFunction> allowedFunctions = GameTestRegistry.getAllTestFunctions()
            .stream()
            .filter(function -> allowedBatchNames.contains(function.batchName()))
            .collect(Collectors.toSet());
        Collection<TestFunction> allFunctions = GameTestRegistry.getAllTestFunctions();
        int before = allFunctions.size();
        allFunctions.removeIf(function -> !allowedFunctions.contains(function));
        int removed = before - allFunctions.size();
        LOGGER.debug("Filtered game tests: removed {}, remaining {}.", removed, allFunctions.size());
        GameTestRegistry.getAllTestClassNames().retainAll(allowedBatchNames);
    }

    public static boolean shouldRegister(Class<?> testClass) {
        if (ALLOWED_CLASSES.isEmpty()) {
            return true;
        }
        boolean enabled = ALLOWED_CLASSES.contains(testClass);
        if (!enabled) {
            LOGGER.debug("Skipping registration for {} due to test filter property.", testClass.getName());
        }
        return enabled;
    }

    private static Map<String, Class<?>> buildNameLookup() {
        Map<String, Class<?>> lookup = new HashMap<>();
        for (Class<?> clazz : ALL_TEST_CLASSES) {
            lookup.put(clazz.getSimpleName().toLowerCase(Locale.ROOT), clazz);
            lookup.put(clazz.getName().toLowerCase(Locale.ROOT), clazz);
        }
        return Collections.unmodifiableMap(lookup);
    }

    private static Set<Class<?>> resolveAllowedClasses() {
        String raw = resolveRawFilter();
        if (raw.isEmpty()) {
            LOGGER.debug("No test filter provided; running full suite.");
            return Collections.emptySet();
        }
        String source = determineFilterSource();
        Set<Class<?>> parsed = new HashSet<>();
        for (String token : raw.split(",")) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Class<?> clazz = NAME_LOOKUP.get(trimmed.toLowerCase(Locale.ROOT));
            if (clazz == null) {
                LOGGER.warn("Unknown test filter entry '{}'; ignoring.", trimmed);
                continue;
            }
            parsed.add(clazz);
        }
        if (parsed.isEmpty()) {
            LOGGER.warn("No valid test filters resolved; running full suite.");
            return Collections.emptySet();
        }
        LOGGER.debug("Applying test filter ({}) for classes: {}", source, parsed.stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
        return Collections.unmodifiableSet(parsed);
    }

    private static String resolveRawFilter() {
        String filterValue = System.getProperty("filter", "").trim();
        if (!filterValue.isEmpty()) {
            return filterValue;
        }
        return System.getProperty("ars_zero.testFilter", "").trim();
    }

    private static String determineFilterSource() {
        if (!System.getProperty("filter", "").trim().isEmpty()) {
            return "filter";
        }
        return "ars_zero.testFilter";
    }
}

