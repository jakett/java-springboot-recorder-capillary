package hello;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class UserListController {

    private static List<QueryDocumentSnapshot> mUserData;

    @GetMapping("/userlist")
    public String userlist(@RequestParam(name = "email", required = false, defaultValue = "World") String email, Model model) throws ExecutionException, InterruptedException {
        return getDataFromFireStore(model);
    }

    private static String getDataFromFireStore(Model model) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> query = db.collection("users").get();
        QuerySnapshot querySnapshot = query.get();
        mUserData = querySnapshot.getDocuments();

        for (QueryDocumentSnapshot document : mUserData) {
            System.out.println("TVT email = " + document.getData().get("email"));
            model.addAttribute("email", document.getData().get("email"));
        }

        return "userlist";
    }
}
