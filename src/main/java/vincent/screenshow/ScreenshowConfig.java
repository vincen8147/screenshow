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

import java.util.ArrayList;
import java.util.List;

public class ScreenshowConfig {

    private int port;
    private long startDelay;
    private String downloadFolder;
    private String downloadFrequency;
    private List<ScreenshowFile> files = new ArrayList<>();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    public void setDownloadFolder(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public String getDownloadFrequency() {
        return downloadFrequency;
    }

    public void setDownloadFrequency(String downloadFrequency) {
        this.downloadFrequency = downloadFrequency;
    }

    public List<ScreenshowFile> getFiles() {
        return files;
    }

    public void setFiles(List<ScreenshowFile> files) {
        this.files.clear();
        this.files.addAll(files);
    }
}
