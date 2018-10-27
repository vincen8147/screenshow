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

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GoogleDriveSyncJob implements Job {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public GoogleDriveSyncJob() {
    }

    @Override
    public void execute(JobExecutionContext context) {
        try {
            GoogleDriveSync sync = (GoogleDriveSync) context.getJobDetail().getJobDataMap().get("sync");
            sync.downloadFromFolder();
        } catch (IOException e) {
           logger.error("Trouble downloading from google.", e);
        }
    }
}
