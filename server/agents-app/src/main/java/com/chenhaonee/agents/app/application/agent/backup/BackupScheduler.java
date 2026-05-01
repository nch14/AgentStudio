package com.chenhaonee.agents.app.application.agent.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时触发器，按配置周期调用 BackupService.backupAll()。
 */
@Component
public class BackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(BackupScheduler.class);

    private final BackupService backupService;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

    @Value("${agents.scheduler.backup.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${agents.scheduler.backup.cron}")
    public void runBackup() {
        if (!enabled) {
            return;
        }

        log.info("Starting scheduled backup scan...");
        BackupService.BackupStats stats = backupService.backupAll();
        log.info("Backup scan completed: backedUp={}, skipped={}, failed={}", stats.backedUpCount(), stats.skippedCount(), stats.failedCount());
    }
}
