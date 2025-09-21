package com.example.temporal.common.workflow;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface NotificationActivities {

    @ActivityMethod
    void notifyTransferInitiated(final Long transferId);

    @ActivityMethod
    void notifyTransferCompleted(final Long transferId);

    @ActivityMethod
    void notifyTransferFailed(final Long transferId, final String reason);

}
