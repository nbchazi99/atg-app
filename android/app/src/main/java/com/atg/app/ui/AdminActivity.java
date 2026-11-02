package com.atg.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.ApiClient;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private static final int TAB_REQUESTS = 0;
    private static final int TAB_USERS = 1;
    private static final int TAB_SIGNALS = 2;
    private static final int TAB_COURSES = 3;
    private static final int TAB_SETTINGS = 4;

    private int tab = TAB_REQUESTS;
    private RecyclerView list;
    private View actions, settingsForm;
    private MaterialButton tabRequests, tabUsers, tabSignals, tabCourses, tabSettings;
    private MaterialButton addSignal, addCourse;

    private Adapters.AdminRequestsAdapter requestsAdapter;
    private Adapters.AdminUsersAdapter usersAdapter;
    private Adapters.AdminSignalsAdapter signalsAdapter;
    private Adapters.AdminCoursesAdapter coursesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tvHeaderTitle)).setText("Admin");
        findViewById(R.id.btnBell).setVisibility(View.GONE);
        findViewById(R.id.tvHeaderAvatar).setVisibility(View.GONE);

        list = findViewById(R.id.rvAdmin);
        list.setLayoutManager(new LinearLayoutManager(this));
        actions = findViewById(R.id.adminActions);
        settingsForm = findViewById(R.id.settingsForm);
        addSignal = findViewById(R.id.btnAddSignal);
        addCourse = findViewById(R.id.btnAddCourse);

        tabRequests = findViewById(R.id.btnTabRequests);
        tabUsers = findViewById(R.id.btnTabUsers);
        tabSignals = findViewById(R.id.btnTabSignals);
        tabCourses = findViewById(R.id.btnTabCourses);
        tabSettings = findViewById(R.id.btnTabSettings);

        tabRequests.setOnClickListener(v -> select(TAB_REQUESTS));
        tabUsers.setOnClickListener(v -> select(TAB_USERS));
        tabSignals.setOnClickListener(v -> select(TAB_SIGNALS));
        tabCourses.setOnClickListener(v -> select(TAB_COURSES));
        tabSettings.setOnClickListener(v -> select(TAB_SETTINGS));

        addSignal.setOnClickListener(v -> addSignal());
        addCourse.setOnClickListener(v -> addCourse());
        findViewById(R.id.btnSaveSettings).setOnClickListener(v -> saveSettings());

        buildAdapters();
        select(TAB_REQUESTS);
    }

    private void buildAdapters() {
        requestsAdapter = new Adapters.AdminRequestsAdapter(new Adapters.OnRequest() {
            @Override
            public void approve(Adapters.RequestRow row) {
                decide(row, true);
            }

            @Override
            public void reject(Adapters.RequestRow row) {
                decide(row, false);
            }
        });

        usersAdapter = new Adapters.AdminUsersAdapter(this::manageMembership);

        signalsAdapter = new Adapters.AdminSignalsAdapter(this::confirmDeleteSignal);

        coursesAdapter = new Adapters.AdminCoursesAdapter(this::confirmDeleteCourse);
    }

    private void select(int next) {
        tab = next;
        Ui.setChip(tabRequests, tab == TAB_REQUESTS);
        Ui.setChip(tabUsers, tab == TAB_USERS);
        Ui.setChip(tabSignals, tab == TAB_SIGNALS);
        Ui.setChip(tabCourses, tab == TAB_COURSES);
        Ui.setChip(tabSettings, tab == TAB_SETTINGS);

        actions.setVisibility(tab == TAB_SIGNALS || tab == TAB_COURSES ? View.VISIBLE : View.GONE);
        addSignal.setVisibility(tab == TAB_SIGNALS ? View.VISIBLE : View.GONE);
        addCourse.setVisibility(tab == TAB_COURSES ? View.VISIBLE : View.GONE);

        boolean settings = tab == TAB_SETTINGS;
        settingsForm.setVisibility(settings ? View.VISIBLE : View.GONE);
        list.setVisibility(settings ? View.GONE : View.VISIBLE);

        switch (tab) {
            case TAB_USERS:
                list.setAdapter(usersAdapter);
                loadUsers();
                break;
            case TAB_SIGNALS:
                list.setAdapter(signalsAdapter);
                loadSignals();
                break;
            case TAB_COURSES:
                list.setAdapter(coursesAdapter);
                loadCourses();
                break;
            case TAB_SETTINGS:
                loadSettings();
                break;
            default:
                list.setAdapter(requestsAdapter);
                loadRequests();
        }
    }

    private void loadRequests() {
        ApiClient.api().adminRequests().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.AdminRequests> call,
                                   @NonNull Response<Models.AdminRequests> response) {
                Models.AdminRequests body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                List<Adapters.RequestRow> rows = new ArrayList<>();
                if (body.deposits != null) {
                    for (Models.DepositRes d : body.deposits) {
                        Adapters.RequestRow row = new Adapters.RequestRow();
                        row.kind = "DEPOSIT";
                        row.id = d.id;
                        row.title = "Deposit · " + safe(d.broker);
                        row.meta = "Client " + safe(d.clientId) + " · " + safe(d.wallet) + " · " + safe(d.mobile);
                        row.amount = d.totalPaid;
                        row.status = d.status;
                        rows.add(row);
                    }
                }
                if (body.withdrawals != null) {
                    for (Models.WithdrawRes w : body.withdrawals) {
                        Adapters.RequestRow row = new Adapters.RequestRow();
                        row.kind = "WITHDRAWAL";
                        row.id = w.id;
                        row.title = "Withdrawal · " + safe(w.payout);
                        row.meta = safe(w.sourceType) + " · " + safe(w.mobile) + " · receive " + Ui.money(w.receive);
                        row.amount = w.amount;
                        row.status = w.status;
                        rows.add(row);
                    }
                }
                if (body.purchases != null) {
                    for (Models.PurchaseRes p : body.purchases) {
                        Adapters.RequestRow row = new Adapters.RequestRow();
                        row.kind = "PURCHASE";
                        row.id = p.id;
                        row.title = "Purchase · " + purchaseLabel(p.type);
                        row.meta = "Reference P-" + p.id + (p.courseId == null ? "" : " · Course " + p.courseId);
                        row.amount = p.amount;
                        row.status = p.status;
                        rows.add(row);
                    }
                }
                requestsAdapter.setItems(rows);
            }

            @Override
            public void onFailure(@NonNull Call<Models.AdminRequests> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void decide(Adapters.RequestRow row, boolean approve) {
        EditText note = new EditText(this);
        note.setHint("Note for the user (optional)");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        note.setPadding(pad, pad, pad, pad);

        new MaterialAlertDialogBuilder(this)
                .setTitle((approve ? "Approve " : "Reject ") + row.title)
                .setMessage(Ui.money(row.amount) + " · " + row.status)
                .setView(note)
                .setNegativeButton("Cancel", null)
                .setPositiveButton(approve ? "Approve" : "Reject", (d, w) -> sendDecision(row,
                        approve, text(note)))
                .show();
    }

    private void sendDecision(Adapters.RequestRow row, boolean approve, String note) {
        Models.Decision body = new Models.Decision(approve, note);
        switch (row.kind) {
            case "DEPOSIT":
                ApiClient.api().decideDeposit(row.id, body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Models.DepositRes> call,
                                           @NonNull Response<Models.DepositRes> response) {
                        afterDecision(response.isSuccessful(), response);
                    }

                    @Override
                    public void onFailure(@NonNull Call<Models.DepositRes> call, @NonNull Throwable t) {
                        Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
                    }
                });
                break;
            case "WITHDRAWAL":
                ApiClient.api().decideWithdrawal(row.id, body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Models.WithdrawRes> call,
                                           @NonNull Response<Models.WithdrawRes> response) {
                        afterDecision(response.isSuccessful(), response);
                    }

                    @Override
                    public void onFailure(@NonNull Call<Models.WithdrawRes> call, @NonNull Throwable t) {
                        Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
                    }
                });
                break;
            default:
                ApiClient.api().decidePurchase(row.id, body).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<Models.PurchaseRes> call,
                                           @NonNull Response<Models.PurchaseRes> response) {
                        afterDecision(response.isSuccessful(), response);
                    }

                    @Override
                    public void onFailure(@NonNull Call<Models.PurchaseRes> call, @NonNull Throwable t) {
                        Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
                    }
                });
        }
    }

    private void afterDecision(boolean ok, Response<?> response) {
        Ui.toast(this, ok ? "Request updated" : ApiClient.errorMessage(response));
        loadRequests();
    }

    private void loadUsers() {
        ApiClient.api().adminUsers().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.User>> call,
                                   @NonNull Response<List<Models.User>> response) {
                List<Models.User> body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                usersAdapter.setItems(body);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.User>> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void manageMembership(Models.User user) {
        String[] options = {"No membership", "Deriv VIP", "Weltrade VIP"};
        String[] values = {"NONE", "DERIV_VIP", "WELTRADE_VIP"};
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(user.membership)) checked = i;
        }
        final int[] chosen = {checked};
        new MaterialAlertDialogBuilder(this)
                .setTitle(user.name)
                .setSingleChoiceItems(options, checked, (d, which) -> chosen[0] = which)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> ApiClient.api()
                        .setMembership(user.id, new Models.MembershipReq(values[chosen[0]]))
                        .enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<Models.User> call,
                                                   @NonNull Response<Models.User> response) {
                                Ui.toast(AdminActivity.this, response.isSuccessful()
                                        ? "Membership updated" : ApiClient.errorMessage(response));
                                loadUsers();
                            }

                            @Override
                            public void onFailure(@NonNull Call<Models.User> call, @NonNull Throwable t) {
                                Ui.alert(AdminActivity.this, "Server not reachable",
                                        ApiClient.connectionMessage(t));
                            }
                        }))
                .show();
    }

    private void loadSignals() {
        ApiClient.api().adminSignals().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.Signal>> call,
                                   @NonNull Response<List<Models.Signal>> response) {
                List<Models.Signal> body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                signalsAdapter.setItems(body);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.Signal>> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void addSignal() {
        View view = getLayoutInflater().inflate(R.layout.dialog_signal, null);
        EditText title = view.findViewById(R.id.etSigTitle);
        EditText subtitle = view.findViewById(R.id.etSigSubtitle);
        EditText symbol = view.findViewById(R.id.etSigSymbol);
        EditText side = view.findViewById(R.id.etSigSide);
        EditText status = view.findViewById(R.id.etSigStatus);
        EditText room = view.findViewById(R.id.etSigRoom);
        EditText entry = view.findViewById(R.id.etSigEntry);
        EditText stop = view.findViewById(R.id.etSigStop);
        EditText targets = view.findViewById(R.id.etSigTargets);
        EditText rule = view.findViewById(R.id.etSigRule);
        EditText participants = view.findViewById(R.id.etSigParticipants);

        side.setText("BUY");
        status.setText("LIVE");
        room.setText("OPEN FLOOR");
        entry.setText("At market, now");
        stop.setText("Not given");
        targets.setText("Not given");
        rule.setText("M10 rule");
        participants.setText("1");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("New signal")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Publish", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (text(title).isEmpty()) {
                        Ui.toast(this, "Give the signal a title");
                        return;
                    }
                    Models.SignalReq req = new Models.SignalReq();
                    req.title = text(title);
                    req.subtitle = text(subtitle);
                    req.symbol = text(symbol);
                    req.side = text(side);
                    req.status = text(status);
                    req.roomLabel = text(room);
                    req.entry = text(entry);
                    req.stopLoss = text(stop);
                    req.targets = text(targets);
                    req.ruleName = text(rule);
                    req.participants = (int) Ui.parseAmount(text(participants), 1);
                    publishSignal(req, dialog);
                }));
        dialog.show();
    }

    private void publishSignal(Models.SignalReq req, AlertDialog dialog) {
        ApiClient.api().createSignal(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.Signal> call, @NonNull Response<Models.Signal> response) {
                if (response.isSuccessful()) {
                    dialog.dismiss();
                    Ui.toast(AdminActivity.this, "Signal published");
                    loadSignals();
                } else {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.Signal> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void confirmDeleteSignal(Models.Signal signal) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + signal.title + "?")
                .setMessage("The call disappears for every trader on the floor.")
                .setNegativeButton("Keep", null)
                .setPositiveButton("Delete", (d, w) -> ApiClient.api().deleteSignal(signal.id)
                        .enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                                                   @NonNull Response<java.util.Map<String, Object>> response) {
                                Ui.toast(AdminActivity.this, response.isSuccessful()
                                        ? "Signal deleted" : ApiClient.errorMessage(response));
                                loadSignals();
                            }

                            @Override
                            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call,
                                                  @NonNull Throwable t) {
                                Ui.alert(AdminActivity.this, "Server not reachable",
                                        ApiClient.connectionMessage(t));
                            }
                        }))
                .show();
    }

    private void loadCourses() {
        ApiClient.api().courses().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<Models.CourseRow>> call,
                                   @NonNull Response<List<Models.CourseRow>> response) {
                List<Models.CourseRow> body = response.body();
                if (!response.isSuccessful() || body == null) {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                List<Models.Course> rows = new ArrayList<>();
                for (Models.CourseRow row : body) {
                    if (row.course != null) rows.add(row.course);
                }
                coursesAdapter.setItems(rows);
            }

            @Override
            public void onFailure(@NonNull Call<List<Models.CourseRow>> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void addCourse() {
        View view = getLayoutInflater().inflate(R.layout.dialog_course, null);
        EditText title = view.findViewById(R.id.etCourseTitle);
        EditText subtitle = view.findViewById(R.id.etCourseSubtitle);
        EditText desc = view.findViewById(R.id.etCourseDesc);
        EditText level = view.findViewById(R.id.etCourseLevel);
        EditText price = view.findViewById(R.id.etCoursePrice);
        EditText lessons = view.findViewById(R.id.etCourseLessons);

        level.setText("Beginner");
        price.setText("30");
        lessons.setText("Welcome and chart basics | 12 | Why price moves, how to read a candle and set up your charts.\n"
                + "The M10 rule | 18 | Entries, stops and targets on the 10-minute chart.\n"
                + "Risk and review | 15 | Position sizing, journaling and weekly review.");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("New course")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (text(title).isEmpty()) {
                        Ui.toast(this, "Give the course a title");
                        return;
                    }
                    Models.CourseReq req = new Models.CourseReq();
                    req.title = text(title);
                    req.subtitle = text(subtitle);
                    req.description = text(desc);
                    req.level = text(level);
                    req.price = Ui.parseAmount(text(price), 0);
                    req.lessons = parseLessons(text(lessons));
                    createCourse(req, dialog);
                }));
        dialog.show();
    }

    private static List<Models.LessonReq> parseLessons(String raw) {
        List<Models.LessonReq> items = new ArrayList<>();
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\|", 4);
            Models.LessonReq lesson = new Models.LessonReq();
            lesson.title = parts[0].trim();
            lesson.durationMin = parts.length > 1 ? (int) Ui.parseAmount(parts[1], 10) : 10;
            lesson.content = parts.length > 2 ? parts[2].trim() : "";
            lesson.videoUrl = parts.length > 3 ? parts[3].trim() : null;
            if (!lesson.title.isEmpty()) items.add(lesson);
        }
        return items;
    }

    private void createCourse(Models.CourseReq req, AlertDialog dialog) {
        ApiClient.api().createCourse(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.Course> call, @NonNull Response<Models.Course> response) {
                if (response.isSuccessful()) {
                    dialog.dismiss();
                    Ui.toast(AdminActivity.this, "Course created");
                    loadCourses();
                } else {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Models.Course> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void confirmDeleteCourse(Models.Course course) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete " + course.title + "?")
                .setMessage("Lessons and course access are removed. Enrolled traders lose the course.")
                .setNegativeButton("Keep", null)
                .setPositiveButton("Delete", (d, w) -> ApiClient.api().deleteCourse(course.id)
                        .enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<java.util.Map<String, Object>> call,
                                                   @NonNull Response<java.util.Map<String, Object>> response) {
                                Ui.toast(AdminActivity.this, response.isSuccessful()
                                        ? "Course deleted" : ApiClient.errorMessage(response));
                                loadCourses();
                            }

                            @Override
                            public void onFailure(@NonNull Call<java.util.Map<String, Object>> call,
                                                  @NonNull Throwable t) {
                                Ui.alert(AdminActivity.this, "Server not reachable",
                                        ApiClient.connectionMessage(t));
                            }
                        }))
                .show();
    }

    private void loadSettings() {
        ApiClient.api().adminSettings().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.SettingsRes> call,
                                   @NonNull Response<Models.SettingsRes> response) {
                Models.SettingsRes s = response.body();
                if (!response.isSuccessful() || s == null) {
                    Ui.toast(AdminActivity.this, ApiClient.errorMessage(response));
                    return;
                }
                set(R.id.etAdminFee, trim(s.adminFeePct));
                set(R.id.etWithdrawFee, trim(s.withdrawFeePct));
                set(R.id.etMinDeposit, trim(s.minDeposit));
                set(R.id.etMaxDeposit, trim(s.maxDeposit));
                set(R.id.etMinWithdraw, trim(s.minWithdraw));
                set(R.id.etFact, s.fact);
                set(R.id.etPromoTitle, s.promotionTitle);
                set(R.id.etPromoBody, s.promotionBody);
            }

            @Override
            public void onFailure(@NonNull Call<Models.SettingsRes> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void saveSettings() {
        Models.SettingsReq req = new Models.SettingsReq();
        req.adminFeePct = Ui.parseAmount(text((EditText) findViewById(R.id.etAdminFee)), 12);
        req.withdrawFeePct = Ui.parseAmount(text((EditText) findViewById(R.id.etWithdrawFee)), 7);
        req.minDeposit = Ui.parseAmount(text((EditText) findViewById(R.id.etMinDeposit)), 12.5);
        req.maxDeposit = Ui.parseAmount(text((EditText) findViewById(R.id.etMaxDeposit)), 454.54);
        req.minWithdraw = Ui.parseAmount(text((EditText) findViewById(R.id.etMinWithdraw)), 12.5);
        req.fact = text((EditText) findViewById(R.id.etFact));
        req.promotionTitle = text((EditText) findViewById(R.id.etPromoTitle));
        req.promotionBody = text((EditText) findViewById(R.id.etPromoBody));

        ApiClient.api().updateSettings(req).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<Models.SettingsRes> call,
                                   @NonNull Response<Models.SettingsRes> response) {
                Ui.toast(AdminActivity.this, response.isSuccessful()
                        ? "Settings saved" : ApiClient.errorMessage(response));
            }

            @Override
            public void onFailure(@NonNull Call<Models.SettingsRes> call, @NonNull Throwable t) {
                Ui.alert(AdminActivity.this, "Server not reachable", ApiClient.connectionMessage(t));
            }
        });
    }

    private void set(int id, String value) {
        EditText field = findViewById(id);
        field.setText(value == null ? "" : value);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String purchaseLabel(String type) {
        if ("COURSE".equals(type)) return "Course";
        if ("VIP_DERIV".equals(type)) return "Deriv VIP";
        if ("VIP_WELTRADE".equals(type)) return "Weltrade VIP";
        return type;
    }

    private static String text(EditText field) {
        return field.getText() == null ? "" : field.getText().toString().trim();
    }
}
