// ChatUpdateWorker.java
package com.example.persona.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import androidx.work.WorkerParameters;
import com.example.persona.MessageStorageHelper;

public class ChatUpdateWorker extends Worker {

    private MessageStorageHelper messageStorageHelper;

    public ChatUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        messageStorageHelper = new MessageStorageHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Delete messages older than 12 hours
        String userId = getInputData().getString("userId");
        if (userId == null) {
            return Result.failure();
        }
        long twelveHoursInMillis = 12 * 60 * 60 * 1000;
        messageStorageHelper.deleteOldMessages(userId, twelveHoursInMillis);

        return Result.success();
    }
}
