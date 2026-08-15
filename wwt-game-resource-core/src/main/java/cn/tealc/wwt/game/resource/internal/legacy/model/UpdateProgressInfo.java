package cn.tealc.wwt.game.resource.internal.legacy.model;

/**
 * Progress info reported by UpdateFlow, PredownloadFlow, RepairFlow.
 * Corresponds to KRResources/KRUpdateProgressInfo.cs.
 */
public class UpdateProgressInfo {
    public static final long MAX_REMAINING_TIME = 359999L;

    public int progressPercentage;

    public long completedSize;

    public long totalSize;

    public long completedCount;

    public long totalCount;

    public double speed;

    private long remainingTime;

    public long getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(long value) {
        if (value > MAX_REMAINING_TIME || value < 0) {
            remainingTime = MAX_REMAINING_TIME;
        } else {
            remainingTime = value;
        }
    }
}
