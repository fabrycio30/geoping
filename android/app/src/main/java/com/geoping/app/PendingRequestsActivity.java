package com.geoping.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.geoping.datacollection.R;
import com.geoping.app.adapters.PendingRequestsAdapter;
import com.geoping.app.models.PendingSubscription;
import com.geoping.app.utils.ApiClient;
import com.geoping.app.utils.AuthManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PendingRequestsActivity extends AppCompatActivity implements PendingRequestsAdapter.OnRequestActionListener {

    private static final String TAG = "PendingRequestsActivity";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private TextView textViewEmpty;
    private RecyclerView recyclerViewRequests;
    private PendingRequestsAdapter adapter;
    private AuthManager authManager;
    private ApiClient apiClient;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        roomId = getIntent().getStringExtra("room_id");
        if (roomId == null) {
            Toast.makeText(this, "Erro: ID da sala não fornecido.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        authManager = new AuthManager(this);
        apiClient = new ApiClient(this);

        textViewEmpty = findViewById(R.id.textViewEmpty);
        recyclerViewRequests = findViewById(R.id.recyclerViewRequests);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new PendingRequestsAdapter(this);
        recyclerViewRequests.setAdapter(adapter);

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        String url = apiClient.buildUrl("/api/rooms/" + roomId + "/pending-subscriptions");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", authManager.getAuthorizationHeader())
                .get()
                .build();

        apiClient.getHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(PendingRequestsActivity.this, "Erro ao carregar solicitações: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erro ao carregar solicitações", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseBody = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (jsonResponse.getBoolean("success")) {
                                JSONArray array = jsonResponse.getJSONArray("pending_subscriptions");
                                List<PendingSubscription> requests = new ArrayList<>();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    PendingSubscription sub = new PendingSubscription();
                                    sub.setId(obj.getInt("id"));
                                    sub.setUserId(obj.getInt("user_id"));
                                    sub.setUsername(obj.getString("username"));
                                    sub.setEmail(obj.getString("email"));
                                    sub.setSubscribedAt(obj.getString("subscribed_at"));
                                    requests.add(sub);
                                }
                                adapter.setRequests(requests);
                                textViewEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                                recyclerViewRequests.setVisibility(requests.isEmpty() ? View.GONE : View.VISIBLE);
                            } else {
                                Toast.makeText(PendingRequestsActivity.this, "Erro: " + jsonResponse.getString("error"), Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Erro JSON", e);
                        }
                    } else {
                        Toast.makeText(PendingRequestsActivity.this, "Erro ao carregar solicitações: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onApprove(PendingSubscription request) {
        processRequest(request, true);
    }

    @Override
    public void onReject(PendingSubscription request) {
        processRequest(request, false);
    }

    private void processRequest(PendingSubscription request, boolean approve) {
        try {
            JSONObject json = new JSONObject();
            json.put("subscription_id", request.getId());
            json.put("approve", approve);

            String url = apiClient.buildUrl("/api/rooms/approve-subscription");
            RequestBody body = RequestBody.create(json.toString(), JSON);
            Request req = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authManager.getAuthorizationHeader())
                    .post(body)
                    .build();

            apiClient.getHttpClient().newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> Toast.makeText(PendingRequestsActivity.this, "Erro de rede", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    final String responseBody = response.body() != null ? response.body().string() : "";
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            Toast.makeText(PendingRequestsActivity.this, approve ? "Aprovado!" : "Rejeitado!", Toast.LENGTH_SHORT).show();
                            loadPendingRequests(); // Recarregar lista
                        } else {
                            try {
                                JSONObject err = new JSONObject(responseBody);
                                Toast.makeText(PendingRequestsActivity.this, "Erro: " + err.optString("error"), Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                Toast.makeText(PendingRequestsActivity.this, "Erro ao processar", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


