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

public class ScreenshowFile {

    private String imageUrl;
    private String fileDate;
    private String fileName;

    public ScreenshowFile(String imageUrl, String fileDate, String fileName) {
        this.imageUrl = imageUrl;
        this.fileDate = fileDate;
        this.fileName = fileName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFileDate() {
        return fileDate;
    }

    public String getFileName() {
        return fileName;
    }
}
