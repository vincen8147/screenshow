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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Directory to store user credentials.
     */
    private static final java.io.File DATA_STORE_DIR =
            new java.io.File(System.getProperty("user.home"), ".store/googledrivefiles");

    private static String APPLICATION_NAME = "slideshow-pi";

    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single
     * globally shared instance across your application.
     */
    private static FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static GoogleDriveSync googleDriveSync;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println(
                    "USAGE: java -jar screenshow-1.0.jar [Google Drive email] [Google Drive Folder ID]");
            System.exit(1);
        }

        InputStream resource = Main.class.getResourceAsStream("/config.json");
        if (null == resource) {
            throw new IllegalStateException("Unable to find 'config.json' file in classpath.");
        }
        ScreenshowConfig config;
        try {
            config = new ObjectMapper().readValue(resource, ScreenshowConfig.class);
            if (config.getPort() <= 0) {
                throw new IllegalStateException("Problem with config?");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read 'config.json'.", e);
        }

        // Global Drive API client.
        Drive drive;
        try {
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
            // authorization
            Credential credential = authorize(args[0]);
            // set up the global Drive instance
            drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }


        // Create a basic jetty server object that will listen on port 8080.
        // Note that if you set this to port 0 then a randomly available port
        // will be assigned that you can either look in the logs for the port,
        // or programmatically obtain it for use in test cases.
        Server server = new Server(config.getPort());

        // The ServletHandler is a dead simple way to create a context handler
        // that is backed by an instance of a Servlet.
        // This handler then needs to be registered with the Server object.
        HandlerCollection handlers = new HandlerCollection();
        server.setHandler(handlers);

        ResourceHandler fileHandler = new ResourceHandler();
        fileHandler.setBaseResource(Resource.newClassPathResource("script"));
        fileHandler.setDirectoriesListed(true);
        handlers.addHandler(fileHandler);

        ServletHandler handler = new ServletHandler();
        handlers.addHandler(handler);

        // Start things up!
        try {
            googleDriveSync = new GoogleDriveSync(config, drive, args[1]);

            MainServlet mainServlet = new MainServlet(config);
            ServletHolder servletHolder = new ServletHolder();
            servletHolder.setServlet(mainServlet);
            handler.addServletWithMapping(servletHolder, "/");

            server.start();
            logger.info("Web service started.");
            server.addLifeCycleListener(
                    new AbstractLifeCycleListener() {
                        @Override
                        public void lifeCycleStopping(LifeCycle event) {
                            googleDriveSync.shutdown();
                            logger.info("Shutting down.");
                        }
                    }
            );
            server.join();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to start.", e);
        }
    }


    /**
     * Authorizes the installed application to access user's protected data.
     */
    private static Credential authorize(String googleUserName) throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(Main.class.getResourceAsStream("/client_secrets.json")));

        // set up authorization code flow
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
                        Arrays.asList(DriveScopes.DRIVE, DriveScopes.DRIVE_FILE))
                        .setDataStoreFactory(dataStoreFactory)
                        .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                .authorize(googleUserName);
    }
}
