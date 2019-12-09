package hello;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.capillary.Config;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

@SpringBootApplication
public class Application {
    private static final Logger logger = Logger.getLogger(Application.class.getName());
    private static final String PORT_OPTION = "port";
    private static final String DATABASE_PATH_OPTION = "database_path";
    private static final String ECDSA_PRIVATE_KEY_PATH_OPTION = "ecdsa_private_key_path";
    private static final String TLS_CERT_PATH_OPTION = "tls_cert_path";
    private static final String TLS_PRIVATE_KEY_PATH_OPTION = "tls_private_key_path";
    private static final String SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION = "service_account_credentials_path";
    private static final String FIREBASE_PROJECT_ID_OPTION = "firebase_project_id";

    private static List<QueryDocumentSnapshot> mUserData;

//    @RequestMapping("/")
//    public String home() {
//        return "Hello Docker World";
//    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);

        // Initialize the Capillary library.
        Config.initialize();

        initFirebase();
    }

    private static void initFirebase() throws ExecutionException, InterruptedException {
        try {
            FileInputStream serviceAccount = new FileInputStream("library/resources/firebase/service-account.json");
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://alarm-app-a3ee2.firebaseio.com")
                    .setStorageBucket("alarm-app-a3ee2.appspot.com")
                    .build();
            FirebaseApp.initializeApp(options);
            System.out.println("Init Firebase success");
            getDataFromFireStore();
        } catch (IOException e) {
            System.out.println("ERROR: invalid service account credentials. See README.");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void getDataFromFireStore() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> query = db.collection("users").get();
        QuerySnapshot querySnapshot = query.get();
        mUserData = querySnapshot.getDocuments();

        for (QueryDocumentSnapshot document : mUserData) {
            System.out.println("TVT email = " + document.getData().get("email"));
        }
    }


//	private void start(CommandLine cmd) throws IOException, GeneralSecurityException, SQLException {
//		// The port on which the server should run.
//		int port = Integer.valueOf(cmd.getOptionValue(PORT_OPTION));
//		// The FCM message sender.
//		FcmSender fcmSender = new FcmSender(cmd.getOptionValue(FIREBASE_PROJECT_ID_OPTION),
//				cmd.getOptionValue(SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION));
//		// The Capillary encrypter managers.
//		RsaEcdsaEncrypterManager rsaEcdsaEncrypterManager;
//		try (FileInputStream senderSigningKey = new FileInputStream(
//				cmd.getOptionValue(ECDSA_PRIVATE_KEY_PATH_OPTION))) {
//			rsaEcdsaEncrypterManager = new RsaEcdsaEncrypterManager(senderSigningKey);
//		}
//		WebPushEncrypterManager webPushEncrypterManager = new WebPushEncrypterManager();
//		// The {certificate, private key} pair to use for gRPC TLS.
//		File tlsCertFile = new File(cmd.getOptionValue(TLS_CERT_PATH_OPTION));
//		File tlsPrivateKeyFile = new File(cmd.getOptionValue(TLS_PRIVATE_KEY_PATH_OPTION));
//		// The interface to demo SQLite DB.
//		DemoDb db = new DemoDb("jdbc:sqlite:" + cmd.getOptionValue(DATABASE_PATH_OPTION));
//		// The demo service.
//		BindableService demoService = new DemoServiceImpl(db, rsaEcdsaEncrypterManager, webPushEncrypterManager,
//				fcmSender);
//		// Create and start the gRPC server instance.
//		server = ServerBuilder.forPort(port).useTransportSecurity(tlsCertFile, tlsPrivateKeyFile)
//				.addService(demoService).build().start();
//		logger.info("Server started, listening on " + port);
//
//		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//			// Use stderr here since the logger may have been reset by its JVM shutdown
//			// hook.
//			System.err.println("*** shutting down gRPC server since JVM is shutting down");
//			shutdown();
//			System.err.println("*** server shut down");
//		}));
//	}
}
