/*
 * Copyright 2018 Jason Vincent https://github.com/vincen8147/screenshow
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package vincent.screenshow;

import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.core.jmx.JobDataMapSupport.newJobDataMap;

class GoogleDriveSync {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final java.io.File downloadFolder;
    private String driveFolderId;
    private final java.io.File downloadTempFolder;
    private final Scheduler scheduler;

    private Drive googleDrive;

    GoogleDriveSync(ScreenshowConfig config, Drive googleDrive, String driveFolderId) throws SchedulerException {
        this.googleDrive = googleDrive;
        this.downloadFolder = new java.io.File(config.getDownloadFolder());
        this.driveFolderId = driveFolderId;
        this.downloadTempFolder = new java.io.File(this.downloadFolder, "inprogress");
        this.downloadTempFolder.mkdirs();
        logger.info("Configured download folder = " + this.downloadFolder.getAbsolutePath());
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("sync", this);
        JobDetail jobDetail =
                newJob(GoogleDriveSyncJob.class)
                        .withIdentity("syncJob", "syncGroup")
                        .usingJobData(newJobDataMap(dataMap))
                        .build();

        Trigger trigger = newTrigger().withIdentity("syncTrigger", "syncGroup")
                .withSchedule(cronSchedule(config.getDownloadFrequency()))
                .build();

        SchedulerFactory sf = new StdSchedulerFactory();
        scheduler = sf.getScheduler();
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
    }

    void downloadFromFolder() throws IOException {
        String pageToken = null;
        Set<String> googleSyncedFiles = new HashSet<>();
        do {
            FileList result = googleDrive.files().list()
                    .setQ("mimeType != 'application/vnd.google-apps.folder' "
                            + "and trashed = false and '"
                            + driveFolderId + "' in parents")

                    .setSpaces("drive")
                    .setFields("files,nextPageToken")
                    .setPageToken(pageToken)
                    .execute();
            for (File file : result.getFiles()) {
                googleSyncedFiles.add(downloadFile(file).getAbsolutePath());
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);

        deleteRemovedFiles(googleSyncedFiles);
    }

    private void deleteRemovedFiles(Set<String> googleSyncedFiles) {
        java.io.File[] files = downloadFolder.listFiles();
        if (null == files) {
            return;
        }
        stream(files)
                .filter(file -> !file.isDirectory()
                        && !googleSyncedFiles.contains(file.getAbsolutePath())
                        && file.delete())
                .forEach(file -> logger.info("Deleting removed file: {}", file.getAbsolutePath()));
    }

    private java.io.File downloadFile(File file) throws IOException {
        String safeFileName = safeFileName(file.getName());
        java.io.File saveAs = new java.io.File(downloadFolder, safeFileName);
        if (saveAs.exists()) {
            logger.info("Not downloading existing file: " + saveAs.getAbsolutePath());
            return saveAs;
        }
        java.io.File downloadTo = java.io.File.createTempFile("acc-sync_", "_" + safeFileName, downloadTempFolder);
        logger.info("downloading file:" + safeFileName + " to " + downloadTo.getAbsolutePath());
        try (FileOutputStream fos = new FileOutputStream(downloadTo)) {
            Drive.Files.Get request = googleDrive.files().get(file.getId());
            request.getMediaHttpDownloader()
                    .setProgressListener(new ProgressListener()).setChunkSize(1024 * 1024);
            request.executeMediaAndDownloadTo(fos);
            fos.flush();
        }
        downloadTo.renameTo(saveAs);
        logger.info("Download Complete: " + saveAs.getAbsolutePath());
        return saveAs;
    }

    private String safeFileName(String name) {
        return name.toLowerCase().replaceAll(" ", "_").replaceAll("[^a-z0-9_\\.]", "");
    }

    void shutdown() {
        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            logger.error("Trouble shutting down GoogleDriveSync scheduler.", e);
        }
    }

    private class ProgressListener implements MediaHttpDownloaderProgressListener {

        @Override
        public void progressChanged(MediaHttpDownloader mediaHttpDownloader) throws IOException {
            if (null == mediaHttpDownloader) {
                return;
            }
            switch (mediaHttpDownloader.getDownloadState()) {
                case MEDIA_IN_PROGRESS: {
                    double percent = mediaHttpDownloader.getProgress() * 100.0;
                    logger.info(String.format("download progress: %3.2f", percent));
                }
                case MEDIA_COMPLETE:
                    logger.info("download complete.");
            }
        }
    }

}
