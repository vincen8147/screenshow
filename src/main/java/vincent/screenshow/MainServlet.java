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

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.JsonNodeValueResolver;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;

import static javax.servlet.http.HttpServletResponse.*;

public class MainServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final Template template;
    private final ScreenshowConfig config;
    private final GoogleDriveSync googleDriveSync;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    MainServlet(ScreenshowConfig config, GoogleDriveSync googleDriveSync) {
        this.config = config;
        this.googleDriveSync = googleDriveSync;
        TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
        Handlebars handlebars = new Handlebars(loader);
        try {
            template = handlebars.compile("home");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {

            String uri = request.getRequestURI();
            if (uri.endsWith(".heic") || uri.endsWith(".HEIC")) {
                response.setStatus(SC_NOT_IMPLEMENTED);
            } else if (uri.startsWith("/" + config.getDownloadFolder())) {
                Path path = Path.of(config.getDownloadFolder(), uri.substring(uri.lastIndexOf("/")));
                new FileInputStream(path.toFile()).transferTo(response.getOutputStream());
                response.setStatus(SC_OK);
            } else if (uri.equals("/")) {
                response.getWriter().println(getHtml());
                response.setContentType("text/html");
                response.setStatus(SC_OK);
            } else {
                response.setStatus(SC_NOT_FOUND);
            }
            logger.info("Serving request uri: " + uri);
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
            throw new IllegalStateException("unable to process", e);
        }
    }

    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        String req = new String(request.getInputStream().readAllBytes());
        if (req.equals("sync")) {
            logger.info("manual sync requested from " + request.getRemoteAddr());
            googleDriveSync.downloadFromFolder();
            writer.print("{\"message\":\"Sync Requested.\"}");
        }
        response.setContentType("text/plain");
    }

    private String getHtml() throws Exception {
        return template.apply(getContext());
    }

    private synchronized Context getContext() {
        config.setFiles(googleDriveSync.getFiles());
        return Context.newBuilder(config)
                .resolver(JsonNodeValueResolver.INSTANCE,
                        JavaBeanValueResolver.INSTANCE,
                        FieldValueResolver.INSTANCE,
                        MapValueResolver.INSTANCE,
                        MethodValueResolver.INSTANCE)
                .build();
    }
}
