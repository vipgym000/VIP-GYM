package SD_Tech.VipGym.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

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
    private static final java.io.File CREDENTIALS_FILE_PATH = new java.io.File("src/main/resources/credentials.json");
    private static final java.io.File TOKENS_DIRECTORY_PATH = new java.io.File("tokens");

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_FILE);

    private Drive driveService;

    public GoogleDriveUploadService() throws Exception {
        this.driveService = getDriveService();
    }

    private static Credential getCredentials() throws Exception {
        var HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(TOKENS_DIRECTORY_PATH))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private static Drive getDriveService() throws Exception {
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

        // 1️⃣ Upload the file
        File uploadedFile = driveService.files()
                .create(fileMetadata, new InputStreamContent(
                        multipartFile.getContentType(), multipartFile.getInputStream()))
                .setFields("id, name")
                .execute();

        // 2️⃣ Make the file public (anyone with link can view)
        com.google.api.services.drive.model.Permission publicPermission =
                new com.google.api.services.drive.model.Permission()
                        .setType("anyone")
                        .setRole("reader");

        driveService.permissions()
                .create(uploadedFile.getId(), publicPermission)
                .execute();

        // 3️⃣ Return the public link
        return "https://drive.google.com/file/d/" + uploadedFile.getId() + "/view?usp=sharing";
    }

    public void deleteFile(String fileId) throws IOException {
        driveService.files().delete(fileId).execute();
    }
}
