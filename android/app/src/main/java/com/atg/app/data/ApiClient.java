package com.atg.app.data;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static Session session;
    private static Retrofit retrofit;
    private static ApiService service;
    private static OkHttpClient http;
    private static final Gson gson = new Gson();

    private ApiClient() {}

    public static void init(Context context) {
        session = new Session(context);
        http = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    var builder = chain.request().newBuilder();
                    String token = session.token();
                    if (token != null) builder.header("Authorization", "Bearer " + token);
                    return chain.proceed(builder.build());
                })
                .build();
        buildRetrofit();
    }

    private static void buildRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl(session.server())
                .client(http)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        service = retrofit.create(ApiService.class);
    }

    public static ApiService api() {
        return service;
    }

    public static Session session() {
        return session;
    }

    public static void reloadServer() {
        buildRetrofit();
    }

    public interface UploadCallback {
        void ok(String url);
        void fail(String message);
    }

    public static void upload(Context context, Uri uri, UploadCallback callback) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            if (input == null) {
                callback.fail("Could not read that file");
                return;
            }
            String name = "proof.jpg";
            String mime = context.getContentResolver().getType(uri);
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0 && cursor.moveToFirst()) {
                    String display = cursor.getString(index);
                    if (display != null) name = display;
                }
                cursor.close();
            }
            if (mime == null) mime = "image/jpeg";
            final String contentType = mime;
            RequestBody body = new RequestBody() {
                @Override
                public MediaType contentType() {
                    return MediaType.parse(contentType);
                }

                @Override
                public void writeTo(@NonNull okio.BufferedSink sink) throws IOException {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) {
                        sink.write(buffer, 0, read);
                    }
                    input.close();
                }
            };
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", name, body);
            service.upload(part).enqueue(new Callback<Models.UploadRes>() {
                @Override
                public void onResponse(@NonNull Call<Models.UploadRes> call, @NonNull Response<Models.UploadRes> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        callback.ok(response.body().url);
                    } else {
                        callback.fail(errorMessage(response));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Models.UploadRes> call, @NonNull Throwable t) {
                    callback.fail(connectionMessage(t));
                }
            });
        } catch (Exception e) {
            callback.fail("Could not read that file");
        }
    }

    public static String errorMessage(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                Models.ErrorRes err = gson.fromJson(raw, Models.ErrorRes.class);
                if (err != null && err.error != null && !err.error.isBlank()) return err.error;
            }
        } catch (Exception ignored) {
        }
        return "Request failed (" + response.code() + ")";
    }

    public static String connectionMessage(Throwable t) {
        return "Cannot reach the ATG server. Check the server address and that the API is running.";
    }
}
