package com.arszero.tests;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.TestReporter;
import org.slf4j.Logger;

public final class FriendlyTestReporter implements TestReporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicReference<List<String>> FINAL_LINES = new AtomicReference<>();
    private static final Thread SUMMARY_HOOK = new Thread(() -> {
        List<String> lines = FINAL_LINES.get();
        if (lines != null) {
            lines.forEach(System.out::println);
        }
    }, "ars-zero-test-summary");
    private final List<String> passedTests = new ArrayList<>();
    private final List<String> failedTests = new ArrayList<>();
    private boolean summaryPrinted;

    static {
        Runtime.getRuntime().addShutdownHook(SUMMARY_HOOK);
    }

    @Override
    public void onTestFailed(GameTestInfo info) {
        failedTests.add(formatEntry(info));
    }

    @Override
    public void onTestSuccess(GameTestInfo info) {
        passedTests.add(formatEntry(info));
    }

    @Override
    public void finish() {
        int passedCount = passedTests.size();
        int failedCount = failedTests.size();
        int totalCount = passedCount + failedCount;
        List<String> lines = buildSummary(totalCount, passedCount, failedCount);
        FINAL_LINES.set(lines);
        lines.forEach(line -> LOGGER.info("{}", line));
        summaryPrinted = true;
    }

    private String formatEntry(GameTestInfo info) {
        return info.getTestName();
    }

    private List<String> buildSummary(int totalCount, int passedCount, int failedCount) {
        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("===== Game Tests Summary =====");
        lines.add("Total: " + totalCount + " (passed " + passedCount + ", failed " + failedCount + ")");
        if (failedCount > 0) {
            lines.add("Failed tests (" + failedCount + "): " + String.join(", ", failedTests.stream().sorted().toList()));
        }
        if (passedCount > 0) {
            lines.add("Passed tests (" + passedCount + "): " + String.join(", ", passedTests.stream().sorted().toList()));
        }
        if (totalCount == 0) {
            lines.add("Game test reporter received no results.");
        }
        if (!summaryPrinted) {
            lines.add("");
        }
        return lines;
    }
}

