package com.huanzhen.fileflexmanager.infrastructure.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class RsyncExecutor {
    // 匹配带-h参数的进度信息，例如: "3.59G 100% 1.29GB/s 0:00:02 (xfr#1, to-chk=0/1)"
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("\\s*([0-9.]+[KMGT]?)\\s+(\\d+)%\\s+([0-9.]+[KMGT]B/s)\\s+(\\d+:\\d+:\\d+)\\s+\\(xfr#(\\d+),\\s+to-chk=(\\d+)/(\\d+)\\)");
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("sent\\s+([\\d,.]+[KMGT]?)\\s+bytes\\s+received\\s+([\\d,.]+[KMGT]?)\\s+bytes\\s+([\\d,.]+[KMGT]?)\\s+bytes/sec");
    private static final Pattern TOTAL_PATTERN = Pattern.compile("total size is ([\\d,.]+[KMGT]?)\\s+speedup is ([0-9.]+)");

    @Data
    @Builder
    public static class RsyncOptions {
        private List<String> sourcePaths;
        private String destinationPath;
        private boolean archive;
        private boolean verbose;
        private boolean showProgress;
        private List<String> additionalOptions;
        private boolean removeSource;  // 用于move操作
    }

    @Data
    @Builder
    public static class RsyncProgress {
        private int percentage;
        private String speed;
        private String remainingTime;
        private String currentSize;
        private String totalSize;
        // 总结信息
        private String sentBytes;
        private String receivedBytes;
        private String averageSpeed;
        private String totalSizeInfo;
        private String speedupInfo;
        // rsync统计信息
        private List<String> statsInfo;

        public String generateMsg() {
            StringBuilder message = new StringBuilder();

            // 如果有总结信息，显示总结和统计信息
            if (CollectionUtil.isNotEmpty(statsInfo)) {
                if (this.getTotalSizeInfo() != null) {
                    message.append("总大小: ").append(this.getTotalSizeInfo());
                }
                if (StrUtil.isAllNotBlank(this.getAverageSpeed())) {
                    message.append("\n平均速度: ").append(this.getAverageSpeed()).append("/s");
                }

                // 添加rsync统计信息
                if (this.getStatsInfo() != null && !this.getStatsInfo().isEmpty()) {
                    message.append("\n\nrsync统计信息:\n");
                    for (String stat : this.getStatsInfo()) {
                        message.append(stat).append("\n");
                    }
                }
                return message.toString();
            }

            // 否则显示进度信息
            message.append(String.format("进度: %d%% (%s)",
                    this.getPercentage(),
                    this.getCurrentSize()));

            if (this.getSpeed() != null) {
                message.append(String.format(" 速度: %s", this.getSpeed()));
            }

            if (this.getPercentage() < 100 && this.getRemainingTime() != null) {
                message.append(String.format(" 剩余时间: %s", this.getRemainingTime()));
            }

            return message.toString();
        }
    }

    private Process currentProcess;
    private volatile boolean isCancelled;
    private List<String> statsInfo = new ArrayList<>();
    private boolean isCollectingStats = false;

    public RsyncProgress execute(RsyncOptions options, Consumer<RsyncProgress> progressCallback) throws Exception {
        List<String> command = buildRsyncCommand(options);
        log.debug("执行rsync命令: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        currentProcess = processBuilder.start();
        RsyncProgress.RsyncProgressBuilder progressBuilder = RsyncProgress.builder();
        statsInfo.clear();
        isCollectingStats = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isCancelled) {
                    currentProcess.destroy();
                    throw new InterruptedException("操作被取消");
                }

                // 当看到"Number of files"时开始收集统计信息
                if (line.startsWith("Number of files:")) {
                    isCollectingStats = true;
                }

                // 如果正在收集统计信息
                if (isCollectingStats) {
                    if (!line.trim().isEmpty()) {
                        statsInfo.add(line.trim());
                        // 如果是sent开头的行，说明是总结信息，需要更新进度
                        if (line.startsWith("sent")) {
                            updateSummary(line, progressBuilder);
                        }
                        // 如果是total开头的行，更新总大小信息
                        else if (line.startsWith("total size is")) {
                            updateTotal(line, progressBuilder);
                            // 在这里回调，因为这是最后一行信息
                            progressBuilder.statsInfo(new ArrayList<>(statsInfo));
                            progressCallback.accept(progressBuilder.build());
                        }
                    }
                }
                // 否则处理进度信息
                else if (line.matches(".*\\d+%.*")) {
                    Matcher progressMatcher = PROGRESS_PATTERN.matcher(line);
                    if (progressMatcher.find()) {
                        String sizeStr = progressMatcher.group(1);
                        int percentage = Integer.parseInt(progressMatcher.group(2));
                        String speed = progressMatcher.group(3);
                        String remainingTime = progressMatcher.group(4);

                        progressBuilder
                                .percentage(percentage)
                                .currentSize(sizeStr)
                                .speed(speed)
                                .remainingTime(remainingTime);

                        progressCallback.accept(progressBuilder.build());
                    }
                }
            }
        }

        int exitCode = currentProcess.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("rsync 执行失败，退出码: " + exitCode);
        }
        return progressBuilder.build();
    }

    private List<String> buildRsyncCommand(RsyncOptions options) {
        List<String> command = new ArrayList<>();
        command.add("rsync");
        command.add("-h");  // 始终使用人类可读格式
        if (options.isShowProgress()) {
            command.add("--info=progress2");
            command.add("--stats"); // 添加统计信息输出
        }

        if (options.isArchive()) {
            command.add("-a");
        }

        if (options.isVerbose()) {
            command.add("-v");
        }

        if (options.isRemoveSource()) {
            command.add("--remove-source-files");
        }

        if (options.getAdditionalOptions() != null) {
            command.addAll(options.getAdditionalOptions());
        }

        // 添加所有源路径
        if (CollectionUtil.isEmpty(options.getSourcePaths())) {
            throw new IllegalArgumentException("源路径列表不能为空");
        }
        command.addAll(options.getSourcePaths());
        command.add(options.getDestinationPath());

        return command;
    }

    private void updateSummary(String line, RsyncProgress.RsyncProgressBuilder progressBuilder) {
        Matcher summaryMatcher = SUMMARY_PATTERN.matcher(line);
        if (summaryMatcher.find()) {
            progressBuilder.sentBytes(summaryMatcher.group(1))
                    .receivedBytes(summaryMatcher.group(2))
                    .averageSpeed(summaryMatcher.group(3));
        }
    }

    private void updateTotal(String line, RsyncProgress.RsyncProgressBuilder progressBuilder) {
        Matcher totalMatcher = TOTAL_PATTERN.matcher(line);
        if (totalMatcher.find()) {
            progressBuilder.totalSizeInfo(totalMatcher.group(1))
                    .speedupInfo(totalMatcher.group(2));
        }
    }

    public void cancel() {
        isCancelled = true;
        if (currentProcess != null) {
            currentProcess.destroy();
        }
    }
} 