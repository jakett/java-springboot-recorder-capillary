package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.capillary.Config;
import com.google.capillary.RsaEcdsaEncrypterManager;
import com.google.capillary.WebPushEncrypterManager;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {
	private static final Logger logger = Logger.getLogger(Application.class.getName());
	private static final String PORT_OPTION = "port";
	private static final String DATABASE_PATH_OPTION = "database_path";
	private static final String ECDSA_PRIVATE_KEY_PATH_OPTION = "ecdsa_private_key_path";
	private static final String TLS_CERT_PATH_OPTION = "tls_cert_path";
	private static final String TLS_PRIVATE_KEY_PATH_OPTION = "tls_private_key_path";
	private static final String SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION = "service_account_credentials_path";
	private static final String FIREBASE_PROJECT_ID_OPTION = "firebase_project_id";

	private Server server;

	@RequestMapping("/")
	public String home() {
		return "Hello Docker World";
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);

		// Initialize the Capillary library.
		Config.initialize();

		// Obtain command line options.
		CommandLine cmd = generateCommandLine(args);

		// Initialize and start gRPC server.
		Application server = new Application();
		server.start(cmd);
		server.blockUntilShutdown();
	}

	public Application() {
        initialize();
    }

	private void initialize() {
//		frame = new JFrame();
//		frame.setBounds(100, 100, 450, 300);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.getContentPane().setLayout(null);
//
//		JLabel lblEmail = new JLabel("Email");
//		lblEmail.setBounds(83, 48, 87, 23);
//		frame.getContentPane().add(lblEmail);
//
//		JLabel lblPassword = new JLabel("Password");
//		lblPassword.setBounds(83, 102, 87, 23);
//		frame.getContentPane().add(lblPassword);
//
//		email = new JTextField();
//		email.setBounds(180, 48, 121, 23);
//		frame.getContentPane().add(email);
//		email.setColumns(10);
//
//		password = new JPasswordField();
//		password.setBounds(180, 103, 121, 23);
//		frame.getContentPane().add(password);
//
//		JButton btnLogin = new JButton("Login");
//		btnLogin.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent arg0) {
//				String emailStr = email.getText();
//				String passwordStr = password.getText();
//			}
//		});
//		btnLogin.setBounds(110, 173, 89, 23);
//		frame.getContentPane().add(btnLogin);
//
//		JButton btnRegister = new JButton("Register");
//		btnRegister.setBounds(238, 173, 89, 23);
//		frame.getContentPane().add(btnRegister);
	}

	private static CommandLine generateCommandLine(String[] commandLineArguments) throws ParseException {
		Option port = Option.builder().longOpt(PORT_OPTION).desc("The port to use.").hasArg().required()
				.type(Integer.class).build();
		Option firebaseProjectId = Option.builder().longOpt(FIREBASE_PROJECT_ID_OPTION)
				.desc("The ID of the Firebase project.").hasArg().required().build();
		Option serviceAccountCredentialsPath = Option.builder().longOpt(SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION)
				.desc("The path to Firebase service account credentials.").hasArg().required().build();
		Option ecdsaPrivateKeyPath = Option.builder().longOpt(ECDSA_PRIVATE_KEY_PATH_OPTION)
				.desc("The path to ecdsa private key.").hasArg().required().build();
		Option tlsCertPath = Option.builder().longOpt(TLS_CERT_PATH_OPTION).desc("The path to tls cert.").hasArg()
				.required().build();
		Option tlsPrivateKeyPath = Option.builder().longOpt(TLS_PRIVATE_KEY_PATH_OPTION)
				.desc("The path to tls private key.").hasArg().required().build();
		Option databasePath = Option.builder().longOpt(DATABASE_PATH_OPTION).desc("The path to sqlite database.")
				.hasArg().required().build();

		Options options = new Options();
		options.addOption(port);
		options.addOption(firebaseProjectId);
		options.addOption(serviceAccountCredentialsPath);
		options.addOption(ecdsaPrivateKeyPath);
		options.addOption(tlsPrivateKeyPath);
		options.addOption(tlsCertPath);
		options.addOption(databasePath);

		CommandLineParser cmdLineParser = new DefaultParser();
		return cmdLineParser.parse(options, commandLineArguments);
	}

	private void start(CommandLine cmd) throws IOException, GeneralSecurityException, SQLException {
		// The port on which the server should run.
		int port = Integer.valueOf(cmd.getOptionValue(PORT_OPTION));
		// The FCM message sender.
		FcmSender fcmSender = new FcmSender(cmd.getOptionValue(FIREBASE_PROJECT_ID_OPTION),
				cmd.getOptionValue(SERVICE_ACCOUNT_CREDENTIALS_PATH_OPTION));
		// The Capillary encrypter managers.
		RsaEcdsaEncrypterManager rsaEcdsaEncrypterManager;
		try (FileInputStream senderSigningKey = new FileInputStream(
				cmd.getOptionValue(ECDSA_PRIVATE_KEY_PATH_OPTION))) {
			rsaEcdsaEncrypterManager = new RsaEcdsaEncrypterManager(senderSigningKey);
		}
		WebPushEncrypterManager webPushEncrypterManager = new WebPushEncrypterManager();
		// The {certificate, private key} pair to use for gRPC TLS.
		File tlsCertFile = new File(cmd.getOptionValue(TLS_CERT_PATH_OPTION));
		File tlsPrivateKeyFile = new File(cmd.getOptionValue(TLS_PRIVATE_KEY_PATH_OPTION));
		// The interface to demo SQLite DB.
		DemoDb db = new DemoDb("jdbc:sqlite:" + cmd.getOptionValue(DATABASE_PATH_OPTION));
		// The demo service.
		BindableService demoService = new DemoServiceImpl(db, rsaEcdsaEncrypterManager, webPushEncrypterManager,
				fcmSender);
		// Create and start the gRPC server instance.
		server = ServerBuilder.forPort(port).useTransportSecurity(tlsCertFile, tlsPrivateKeyFile)
				.addService(demoService).build().start();
		logger.info("Server started, listening on " + port);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			// Use stderr here since the logger may have been reset by its JVM shutdown
			// hook.
			System.err.println("*** shutting down gRPC server since JVM is shutting down");
			shutdown();
			System.err.println("*** server shut down");
		}));

//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					logger.info("go to open interface of java server");
//					Application window = new Application();
//					window.frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
	}

	// Await termination on the main thread since the gRPC library uses daemon
	// threads.
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	private void shutdown() {
		if (server != null) {
			server.shutdown();
		}
	}

}
