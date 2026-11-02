package com.atg.app.data;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    @POST("api/auth/login")
    Call<Models.AuthRes> login(@Body Models.LoginReq body);

    @POST("api/auth/register")
    Call<Models.AuthRes> register(@Body Models.RegisterReq body);

    @GET("api/me")
    Call<Models.User> me();

    @PUT("api/me")
    Call<Models.User> updateMe(@Body Map<String, String> body);

    @GET("api/home")
    Call<Models.HomeRes> home();

    @GET("api/settings/public")
    Call<Models.SettingsRes> settings();

    @GET("api/signals")
    Call<List<Models.Signal>> signals(@Query("status") String status, @Query("symbol") String symbol);

    @GET("api/markets")
    Call<List<Models.MarketPair>> markets(@Query("category") String category);

    @GET("api/markets/{symbol}")
    Call<Models.PairRes> pair(@Path("symbol") String symbol);

    @POST("api/markets/{symbol}/favourite")
    Call<Map<String, Object>> toggleFavourite(@Path("symbol") String symbol);

    @GET("api/rooms")
    Call<List<Models.Room>> rooms();

    @GET("api/rooms/feed")
    Call<List<Models.FeedItem>> feed(@Query("filter") String filter);

    @GET("api/rooms/{id}/messages")
    Call<List<Models.RoomMessage>> messages(@Path("id") long roomId);

    @POST("api/rooms/{id}/messages")
    Call<Models.RoomMessage> sendMessage(@Path("id") long roomId, @Body Models.TextReq body);

    @GET("api/wallet")
    Call<Models.WalletRes> wallet();

    @POST("api/deposits")
    Call<Models.DepositRes> deposit(@Body Models.DepositReq body);

    @GET("api/deposits")
    Call<List<Models.DepositRes>> deposits();

    @POST("api/withdrawals")
    Call<Models.WithdrawRes> withdraw(@Body Models.WithdrawReq body);

    @GET("api/withdrawals")
    Call<List<Models.WithdrawRes>> withdrawals();

    @Multipart
    @POST("api/uploads")
    Call<Models.UploadRes> upload(@Part MultipartBody.Part file);

    @GET("api/courses")
    Call<List<Models.CourseRow>> courses();

    @GET("api/courses/{id}")
    Call<Models.CourseDetail> course(@Path("id") long id);

    @POST("api/courses/{id}/complete")
    Call<Map<String, Object>> completeLesson(@Path("id") long courseId, @Body Models.CompleteReq body);

    @GET("api/purchases")
    Call<List<Models.PurchaseRes>> purchases();

    @POST("api/purchases")
    Call<Models.PurchaseRes> purchase(@Body Models.PurchaseReq body);

    // Admin
    @GET("api/admin/users")
    Call<List<Models.User>> adminUsers();

    @PATCH("api/admin/users/{id}/membership")
    Call<Models.User> setMembership(@Path("id") long userId, @Body Models.MembershipReq body);

    @GET("api/admin/requests")
    Call<Models.AdminRequests> adminRequests();

    @POST("api/admin/deposits/{id}/decide")
    Call<Models.DepositRes> decideDeposit(@Path("id") long id, @Body Models.Decision body);

    @POST("api/admin/withdrawals/{id}/decide")
    Call<Models.WithdrawRes> decideWithdrawal(@Path("id") long id, @Body Models.Decision body);

    @POST("api/admin/purchases/{id}/decide")
    Call<Models.PurchaseRes> decidePurchase(@Path("id") long id, @Body Models.Decision body);

    @GET("api/admin/signals")
    Call<List<Models.Signal>> adminSignals();

    @POST("api/admin/signals")
    Call<Models.Signal> createSignal(@Body Models.SignalReq body);

    @PUT("api/admin/signals/{id}")
    Call<Models.Signal> updateSignal(@Path("id") long id, @Body Models.SignalReq body);

    @DELETE("api/admin/signals/{id}")
    Call<Map<String, Object>> deleteSignal(@Path("id") long id);

    @POST("api/admin/courses")
    Call<Models.Course> createCourse(@Body Models.CourseReq body);

    @DELETE("api/admin/courses/{id}")
    Call<Map<String, Object>> deleteCourse(@Path("id") long id);

    @GET("api/admin/settings")
    Call<Models.SettingsRes> adminSettings();

    @PUT("api/admin/settings")
    Call<Models.SettingsRes> updateSettings(@Body Models.SettingsReq body);
}
