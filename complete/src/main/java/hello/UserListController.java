package hello;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.concurrent.ExecutionException;

import jdk.nashorn.internal.parser.JSONParser;

@Controller
public class UserListController {

    private static List<QueryDocumentSnapshot> mUserData;

    @GetMapping("/userlist")
    public String userlist(@RequestParam(name = "email", required = false, defaultValue = "World") String email, Model model) throws ExecutionException, InterruptedException {
        return getDataFromFireStore(model);
    }

    @PostMapping(path = "/userlist", consumes = "application/json", produces = "application/json")
    public void setActivateForUser(@RequestBody String object) throws ExecutionException, InterruptedException {
        try {
            JSONObject jsonObject = new JSONObject(object);
            System.out.println("TVT go to testActivated function, object = " + jsonObject.toString());
            switch (jsonObject.get("type").toString()) {
                case "activated":
                    handleActivatedForUser(jsonObject.get("value").toString());
                    break;
                case "sendtext":
                    break;
                case "sendimage":
                    break;
                default:
            }
        } catch (JSONException err) {

        }
    }

    private static String getDataFromFireStore(Model model) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> query = db.collection("users").get();
        QuerySnapshot querySnapshot = query.get();
        mUserData = querySnapshot.getDocuments();
        model.addAttribute("users", mUserData);

        return "userlist";
    }

    private static void handleActivatedForUser(String email) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        CollectionReference users = db.collection("users");
        Query query = users.whereEqualTo("email", email);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        for (DocumentSnapshot document : querySnapshot.get().getDocuments()) {
            db.collection("users").document(document.getId()).update("activated", true);
        }
    }
}
