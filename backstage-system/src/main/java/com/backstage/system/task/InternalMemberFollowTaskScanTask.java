package com.backstage.system.task;

import com.backstage.system.service.internalmember.IInternalMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InternalMemberFollowTaskScanTask {
    private static final Logger log = LoggerFactory.getLogger(InternalMemberFollowTaskScanTask.class);

    private final IInternalMemberService internalMemberService;

    public InternalMemberFollowTaskScanTask(IInternalMemberService internalMemberService) {
        this.internalMemberService = internalMemberService;
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void processDueReminders() {
        log.info("Start scanning internal-member follow task reminders");
        internalMemberService.processDueFollowTaskReminders();
        log.info("Finished scanning internal-member follow task reminders");
    }

    @Scheduled(cron = "0 0/5 * * * ?")
    public void markOverdueTasks() {
        log.info("Start scanning overdue internal-member follow tasks");
        internalMemberService.markOverdueFollowTasks();
        log.info("Finished scanning overdue internal-member follow tasks");
    }
}
