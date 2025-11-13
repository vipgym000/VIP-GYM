package SD_Tech.VipGym.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

@Service
public class GoogleDriveUploadService {

    private static final String APPLICATION_NAME = "VipGymUploader";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);
    
    private final Drive driveService;
    
    public GoogleDriveUploadService(@Value("${google.drive.folder.id}") String folderId) throws Exception {
        this.driveService = getDriveService();
    }

    private Credential getCredentials() throws Exception {
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        // Load credentials from classpath instead of file system
        InputStream in = GoogleDriveUploadService.class.getResourceAsStream("/credentials.json");
        if (in == null) {
            throw new IOException("Credentials file not found in classpath: /credentials.json");
        }
        
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(System.getProperty("java.io.tmpdir"), "drive-tokens")))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Drive getDriveService() throws Exception {
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public String uploadFile(MultipartFile multipartFile, String folderId) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(multipartFile.getOriginalFilename());
        if (folderId != null) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }

        File file = driveService.files().create(fileMetadata,
                new InputStreamContent(multipartFile.getContentType(), multipartFile.getInputStream()))
                .setFields("id, name")
                .execute();

        return "File uploaded: " + file.getName() + " (ID: " + file.getId() + ")";
    }

    public void listFiles() throws IOException {
        FileList result = driveService.files().list()
                .setPageSize(10)
                .setFields("files(id, name)")
                .execute();
        for (File file : result.getFiles()) {
            System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
        }
    }
    
    public String uploadToDrive(MultipartFile multipartFile, String fileName) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);

        // Upload the file
        File uploadedFile = driveService.files()
                .create(fileMetadata, new InputStreamContent(
                        multipartFile.getContentType(), multipartFile.getInputStream()))
                .setFields("id, name")
                .execute();

        // Make the file public
        com.google.api.services.drive.model.Permission publicPermission =
                new com.google.api.services.drive.model.Permission()
                        .setType("anyone")
                        .setRole("reader");

        driveService.permissions()
                .create(uploadedFile.getId(), publicPermission)
                .execute();

        // Return the public link
        return "https://drive.google.com/file/d/" + uploadedFile.getId() + "/view?usp=sharing";
    }

    public void deleteFile(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
    }
}