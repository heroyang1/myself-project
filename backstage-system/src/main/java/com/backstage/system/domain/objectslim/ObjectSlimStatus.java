package com.backstage.system.domain.objectslim;

public final class ObjectSlimStatus {
    public static final String PENDING = "pending";
    public static final String RUNNING = "running";
    public static final String SUCCESS = "success";
    public static final String FAILED = "failed";
    public static final String SKIPPED = "skipped";
    public static final String NO_NEED = "no_need";
    public static final String ROLLED_BACK = "rolled_back";
    public static final String BACKUP_DELETED = "backup_deleted";

    private ObjectSlimStatus() {
    }
}
