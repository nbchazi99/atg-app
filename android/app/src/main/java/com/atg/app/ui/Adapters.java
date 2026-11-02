package com.atg.app.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.atg.app.R;
import com.atg.app.data.Models;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Adapters {

    public interface OnSignal {
        void open(Models.Signal signal);
    }

    public interface OnMarket {
        void open(Models.MarketPair pair);
        void favourite(Models.MarketPair pair, int position);
    }

    public interface OnFeed {
        void open(Models.FeedItem item);
    }

    public interface OnCourse {
        void open(Models.CourseRow row);
    }

    public interface OnLesson {
        void open(Models.Lesson lesson, boolean completed);
    }

    public interface OnRequest {
        void approve(RequestRow row);
        void reject(RequestRow row);
    }

    public interface OnOption {
        void selected(int index);
    }

    public interface OnAdminUser {
        void manage(Models.User user);
    }

    public interface OnDeleteSignal {
        void delete(Models.Signal signal);
    }

    public interface OnDeleteCourse {
        void delete(Models.Course course);
    }

    public static final class Option {
        public String title;
        public String subtitle;
        public String badge;
        public String badgeColor = "#111111";
        public String tag;

        public Option(String title, String subtitle, String badge) {
            this.title = title;
            this.subtitle = subtitle;
            this.badge = badge;
        }

        public Option tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Option color(String color) {
            this.badgeColor = color;
            return this;
        }
    }

    public static class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.Holder> {
        private final List<Option> items = new ArrayList<>();
        private int selected = 0;
        private final OnOption callback;

        public OptionsAdapter(OnOption callback) {
            this.callback = callback;
        }

        public void setItems(List<Option> options) {
            items.clear();
            items.addAll(options);
            if (selected >= items.size()) selected = 0;
            notifyDataSetChanged();
        }

        public int selectedIndex() {
            return selected;
        }

        public Option selectedOption() {
            return items.isEmpty() ? null : items.get(selected);
        }

        public void select(int index) {
            if (index < 0 || index >= items.size()) return;
            selected = index;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_option, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Option o = items.get(position);
            boolean isSelected = position == selected;
            h.itemView.setBackgroundResource(isSelected ? R.drawable.bg_card_selected : R.drawable.bg_card);
            h.title.setText(o.title);
            h.badge.setText(o.badge);
            try {
                h.badge.setTextColor(Color.parseColor(o.badgeColor));
            } catch (IllegalArgumentException e) {
                h.badge.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.ink));
            }
            if (o.subtitle == null || o.subtitle.isBlank()) {
                h.subtitle.setVisibility(View.GONE);
            } else {
                h.subtitle.setVisibility(View.VISIBLE);
                h.subtitle.setText(o.subtitle);
            }
            if (o.tag == null || o.tag.isBlank()) {
                h.tag.setVisibility(View.GONE);
            } else {
                h.tag.setVisibility(View.VISIBLE);
                h.tag.setText(o.tag);
            }
            h.radio.setChecked(isSelected);
            h.itemView.setOnClickListener(v -> {
                selected = position;
                notifyDataSetChanged();
                if (callback != null) callback.selected(position);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, subtitle, badge, tag;
            final RadioButton radio;

            Holder(View view) {
                super(view);
                title = view.findViewById(R.id.tvOptionTitle);
                subtitle = view.findViewById(R.id.tvOptionSubtitle);
                badge = view.findViewById(R.id.tvOptionBadge);
                tag = view.findViewById(R.id.tvOptionTag);
                radio = view.findViewById(R.id.rbOption);
            }
        }
    }

    public static class SignalsAdapter extends RecyclerView.Adapter<SignalsAdapter.Holder> {
        private final List<Models.Signal> items = new ArrayList<>();
        private final OnSignal callback;

        public SignalsAdapter(OnSignal callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.Signal> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_signal, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Signal s = items.get(position);
            Context c = h.itemView.getContext();
            h.title.setText(s.title);
            h.subtitle.setText(s.subtitle);
            h.side.setText(s.side);
            boolean buy = "BUY".equals(s.side);
            h.side.setBackgroundResource(buy ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);
            h.side.setTextColor(ContextCompat.getColor(c, buy ? R.color.green_text : R.color.red_text));
            boolean live = "LIVE".equals(s.status);
            h.live.setVisibility(live ? View.VISIBLE : View.GONE);
            h.room.setText(s.roomLabel);
            h.entry.setText(s.entry);
            h.stop.setText(s.stopLoss);
            h.targets.setText(s.targets);
            h.rule.setText(s.ruleName);
            h.participants.setText(String.valueOf(s.participants));
            h.time.setText(Ui.timeAgo(s.createdAt));
            h.itemView.setOnClickListener(v -> {
                if (callback != null) callback.open(s);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, subtitle, side, live, room, entry, stop, targets, rule, participants, time;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvSignalTitle);
                subtitle = v.findViewById(R.id.tvSignalSubtitle);
                side = v.findViewById(R.id.tvSignalSide);
                live = v.findViewById(R.id.tvSignalLive);
                room = v.findViewById(R.id.tvSignalRoom);
                entry = v.findViewById(R.id.tvSignalEntry);
                stop = v.findViewById(R.id.tvSignalStop);
                targets = v.findViewById(R.id.tvSignalTargets);
                rule = v.findViewById(R.id.tvSignalRule);
                participants = v.findViewById(R.id.tvSignalParticipants);
                time = v.findViewById(R.id.tvSignalTime);
            }
        }
    }

    public static class MarketsAdapter extends RecyclerView.Adapter<MarketsAdapter.Holder> {
        private final List<Models.MarketPair> items = new ArrayList<>();
        private final Set<String> favourites = new HashSet<>();
        private final OnMarket callback;

        public MarketsAdapter(OnMarket callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.MarketPair> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        public void setFavourites(List<Models.MarketPair> favs) {
            favourites.clear();
            for (Models.MarketPair p : favs) favourites.add(p.symbol);
            notifyDataSetChanged();
        }

        public List<Models.MarketPair> current() {
            return items;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_market_pair, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.MarketPair p = items.get(position);
            Context c = h.itemView.getContext();
            h.symbol.setText(p.symbol);
            h.name.setText(p.name);
            h.price.setText(Ui.price(p.price));
            boolean up = p.changePct >= 0;
            h.change.setText(Ui.pct(p.changePct));
            h.change.setBackgroundResource(up ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);
            h.change.setTextColor(ContextCompat.getColor(c, up ? R.color.green_text : R.color.red_text));
            h.updated.setText(Ui.timeAgo(p.updatedAt));
            h.fav.setImageResource(favourites.contains(p.symbol) ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            h.itemView.setOnClickListener(v -> {
                if (callback != null) callback.open(p);
            });
            h.fav.setOnClickListener(v -> {
                if (callback != null) callback.favourite(p, position);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView symbol, name, price, change, updated;
            final ImageButton fav;

            Holder(View v) {
                super(v);
                symbol = v.findViewById(R.id.tvPairSymbol);
                name = v.findViewById(R.id.tvPairName);
                price = v.findViewById(R.id.tvPairPrice);
                change = v.findViewById(R.id.tvPairChange);
                updated = v.findViewById(R.id.tvPairUpdated);
                fav = v.findViewById(R.id.btnFavourite);
            }
        }
    }

    public static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.Holder> {
        private final List<Models.FeedItem> items = new ArrayList<>();
        private final OnFeed callback;

        public FeedAdapter(OnFeed callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.FeedItem> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed_row, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.FeedItem f = items.get(position);
            Context c = h.itemView.getContext();
            h.badge.setText(f.badge);
            boolean buy = "BUY".equals(f.side);
            h.badge.setBackgroundResource(buy ? R.drawable.bg_circle_green : R.drawable.bg_circle_red);
            h.badge.setTextColor(ContextCompat.getColor(c, buy ? R.color.green_text : R.color.red_text));
            h.title.setText(f.title);
            h.side.setText(f.side);
            h.side.setBackgroundResource(buy ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);
            h.side.setTextColor(ContextCompat.getColor(c, buy ? R.color.green_text : R.color.red_text));
            h.preview.setText(f.preview);
            h.time.setText(Ui.timeAgo(f.createdAt));
            if (f.participants > 0) {
                h.count.setVisibility(View.VISIBLE);
                h.count.setText(String.valueOf(f.participants));
            } else {
                h.count.setVisibility(View.GONE);
            }
            h.itemView.setOnClickListener(v -> {
                if (callback != null) callback.open(f);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView badge, title, side, preview, time, count;

            Holder(View v) {
                super(v);
                badge = v.findViewById(R.id.tvFeedBadge);
                title = v.findViewById(R.id.tvFeedTitle);
                side = v.findViewById(R.id.tvFeedSide);
                preview = v.findViewById(R.id.tvFeedPreview);
                time = v.findViewById(R.id.tvFeedTime);
                count = v.findViewById(R.id.tvFeedCount);
            }
        }
    }

    public static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.Holder> {
        private final List<Models.RoomMessage> items = new ArrayList<>();
        private final String selfName;

        public MessagesAdapter(String selfName) {
            this.selfName = selfName;
        }

        public void setItems(List<Models.RoomMessage> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        public void append(Models.RoomMessage message) {
            items.add(message);
            notifyItemInserted(items.size() - 1);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.RoomMessage m = items.get(position);
            boolean own = m.author != null && m.author.equalsIgnoreCase(selfName);
            h.author.setText(m.author);
            h.text.setText(m.text);
            h.time.setText(Ui.timeAgo(m.createdAt));
            h.bubble.setBackgroundResource(own ? R.drawable.bg_bubble_out : R.drawable.bg_bubble_in);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) h.bubble.getLayoutParams();
            params.gravity = own ? android.view.Gravity.END : android.view.Gravity.START;
            h.bubble.setLayoutParams(params);
            h.author.setVisibility(own ? View.GONE : View.VISIBLE);
            h.author.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.gold));
            h.root.setGravity(own ? android.view.Gravity.END : android.view.Gravity.START);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView author, text, time;
            final LinearLayout bubble, root;

            Holder(View v) {
                super(v);
                author = v.findViewById(R.id.tvMsgAuthor);
                text = v.findViewById(R.id.tvMsgText);
                time = v.findViewById(R.id.tvMsgTime);
                bubble = v.findViewById(R.id.bubble);
                root = (LinearLayout) v;
            }
        }
    }

    public static class TxAdapter extends RecyclerView.Adapter<TxAdapter.Holder> {
        private final List<Models.WalletTx> items = new ArrayList<>();

        public TxAdapter() {}

        public void setItems(List<Models.WalletTx> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tx, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.WalletTx t = items.get(position);
            h.title.setText(t.description);
            h.date.setText(Ui.timeAgo(t.createdAt));
            h.amount.setText(Ui.money(t.amount));
            h.status.setText(t.status);
            h.status.setBackgroundResource(statusBg(t.status));
            h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), statusColor(t.status)));
            if ("WITHDRAWAL".equals(t.type)) h.icon.setImageResource(R.drawable.ic_withdraw);
            else if ("PURCHASE".equals(t.type)) h.icon.setImageResource(R.drawable.ic_academy);
            else h.icon.setImageResource(R.drawable.ic_deposit);
        }

        static int statusBg(String status) {
            if (status == null) return R.drawable.bg_chip_grey;
            switch (status) {
                case "COMPLETED":
                case "APPROVED":
                case "LIVE":
                    return R.drawable.bg_chip_green;
                case "REJECTED":
                    return R.drawable.bg_chip_red;
                case "PENDING":
                    return R.drawable.bg_chip_amber;
                default:
                    return R.drawable.bg_chip_grey;
            }
        }

        static int statusColor(String status) {
            if (status == null) return R.color.muted;
            switch (status) {
                case "COMPLETED":
                case "APPROVED":
                case "LIVE":
                    return R.color.green_text;
                case "REJECTED":
                    return R.color.red_text;
                case "PENDING":
                    return R.color.gold;
                default:
                    return R.color.muted;
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, date, amount, status;
            final ImageView icon;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvTxTitle);
                date = v.findViewById(R.id.tvTxDate);
                amount = v.findViewById(R.id.tvTxAmount);
                status = v.findViewById(R.id.tvTxStatus);
                icon = v.findViewById(R.id.ivTxIcon);
            }
        }
    }

    public static class CoursesAdapter extends RecyclerView.Adapter<CoursesAdapter.Holder> {
        private final List<Models.CourseRow> items = new ArrayList<>();
        private final OnCourse callback;

        public CoursesAdapter(OnCourse callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.CourseRow> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.CourseRow row = items.get(position);
            Models.Course c = row.course;
            h.title.setText(c.title);
            h.subtitle.setText(c.subtitle);
            h.level.setText(c.level);
            h.lessons.setText(row.totalLessons + " lessons");
            h.progress.setProgress(row.enrolled ? row.progressPct : 0);
            h.progress.setVisibility(row.enrolled ? View.VISIBLE : View.GONE);
            h.price.setText(row.enrolled ? "Enrolled" : Ui.money(c.price));
            h.price.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                    row.enrolled ? R.color.green_text : R.color.ink));
            if (row.enrolled) {
                h.progressLabel.setText(row.completedLessons + " of " + row.totalLessons + " complete");
            } else {
                h.progressLabel.setText("Buy to unlock lessons");
            }
            h.itemView.setOnClickListener(v -> {
                if (callback != null) callback.open(row);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, subtitle, level, lessons, price, progressLabel;
            final ProgressBar progress;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvCourseTitle);
                subtitle = v.findViewById(R.id.tvCourseSubtitle);
                level = v.findViewById(R.id.tvCourseLevel);
                lessons = v.findViewById(R.id.tvCourseLessons);
                price = v.findViewById(R.id.tvCoursePrice);
                progress = v.findViewById(R.id.pbCourse);
                progressLabel = v.findViewById(R.id.tvCourseProgress);
            }
        }
    }

    public static class LessonsAdapter extends RecyclerView.Adapter<LessonsAdapter.Holder> {
        private final List<Models.Lesson> items = new ArrayList<>();
        private final Set<Long> completed = new HashSet<>();
        private final OnLesson callback;

        public LessonsAdapter(OnLesson callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.Lesson> lessons, List<Long> done) {
            items.clear();
            items.addAll(lessons);
            completed.clear();
            if (done != null) completed.addAll(done);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lesson, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Lesson l = items.get(position);
            boolean done = completed.contains(l.id);
            h.index.setText(String.valueOf(position + 1));
            h.title.setText(l.title);
            h.meta.setText(l.durationMin + " min" + (done ? " · Completed" : ""));
            h.done.setVisibility(done ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> {
                if (callback != null) callback.open(l, done);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView index, title, meta;
            final ImageView done;

            Holder(View v) {
                super(v);
                index = v.findViewById(R.id.tvLessonIndex);
                title = v.findViewById(R.id.tvLessonTitle);
                meta = v.findViewById(R.id.tvLessonMeta);
                done = v.findViewById(R.id.ivLessonDone);
            }
        }
    }

    public static class RequestRow {
        public String kind;
        public long id;
        public String title;
        public String meta;
        public double amount;
        public String status;
    }

    public static class AdminRequestsAdapter extends RecyclerView.Adapter<AdminRequestsAdapter.Holder> {
        private final List<RequestRow> items = new ArrayList<>();
        private final OnRequest callback;

        public AdminRequestsAdapter(OnRequest callback) {
            this.callback = callback;
        }

        public void setItems(List<RequestRow> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_request, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            RequestRow r = items.get(position);
            h.title.setText(r.title);
            h.meta.setText(r.meta);
            h.amount.setText(Ui.money(r.amount));
            h.status.setText(r.status);
            h.status.setBackgroundResource(TxAdapter.statusBg(r.status));
            h.status.setTextColor(ContextCompat.getColor(h.itemView.getContext(), TxAdapter.statusColor(r.status)));
            boolean pending = "PENDING".equals(r.status);
            h.approve.setVisibility(pending ? View.VISIBLE : View.GONE);
            h.reject.setVisibility(pending ? View.VISIBLE : View.GONE);
            h.approve.setOnClickListener(v -> {
                if (callback != null) callback.approve(r);
            });
            h.reject.setOnClickListener(v -> {
                if (callback != null) callback.reject(r);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, meta, amount, status;
            final MaterialButton approve, reject;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvReqTitle);
                meta = v.findViewById(R.id.tvReqMeta);
                amount = v.findViewById(R.id.tvReqAmount);
                status = v.findViewById(R.id.tvReqStatus);
                approve = v.findViewById(R.id.btnApprove);
                reject = v.findViewById(R.id.btnReject);
            }
        }
    }

    public static class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.Holder> {
        private final List<Models.User> items = new ArrayList<>();
        private final OnAdminUser callback;

        public AdminUsersAdapter(OnAdminUser callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.User> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.User u = items.get(position);
            h.name.setText(u.name);
            h.email.setText(u.email + " · " + u.role);
            h.membership.setText(Ui.membershipLabel(u.membership).toUpperCase(Locale.ROOT));
            h.membership.setBackgroundResource("NONE".equals(u.membership) ? R.drawable.bg_chip_grey : R.drawable.bg_chip_green);
            h.membership.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                    "NONE".equals(u.membership) ? R.color.muted : R.color.green_text));
            h.button.setOnClickListener(v -> {
                if (callback != null) callback.manage(u);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView name, email, membership;
            final MaterialButton button;

            Holder(View v) {
                super(v);
                name = v.findViewById(R.id.tvAdminUserName);
                email = v.findViewById(R.id.tvAdminUserEmail);
                membership = v.findViewById(R.id.tvAdminUserMembership);
                button = v.findViewById(R.id.btnMembership);
            }
        }
    }

    public static class AdminSignalsAdapter extends RecyclerView.Adapter<AdminSignalsAdapter.Holder> {
        private final List<Models.Signal> items = new ArrayList<>();
        private final OnDeleteSignal callback;

        public AdminSignalsAdapter(OnDeleteSignal callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.Signal> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_signal, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Signal s = items.get(position);
            h.title.setText(s.title);
            h.side.setText(s.side);
            boolean buy = "BUY".equals(s.side);
            h.side.setBackgroundResource(buy ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);
            h.side.setTextColor(ContextCompat.getColor(h.itemView.getContext(), buy ? R.color.green_text : R.color.red_text));
            h.status.setText(s.status);
            h.meta.setText(s.subtitle + " · " + s.roomLabel);
            h.levels.setText("Entry " + s.entry + " · Stop " + s.stopLoss + " · Targets " + s.targets);
            h.delete.setOnClickListener(v -> {
                if (callback != null) callback.delete(s);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, side, status, meta, levels;
            final MaterialButton delete;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvAdminSignalTitle);
                side = v.findViewById(R.id.tvAdminSignalSide);
                status = v.findViewById(R.id.tvAdminSignalStatus);
                meta = v.findViewById(R.id.tvAdminSignalMeta);
                levels = v.findViewById(R.id.tvAdminSignalLevels);
                delete = v.findViewById(R.id.btnDeleteSignal);
            }
        }
    }

    public static class AdminCoursesAdapter extends RecyclerView.Adapter<AdminCoursesAdapter.Holder> {
        private final List<Models.Course> items = new ArrayList<>();
        private final OnDeleteCourse callback;

        public AdminCoursesAdapter(OnDeleteCourse callback) {
            this.callback = callback;
        }

        public void setItems(List<Models.Course> data) {
            items.clear();
            items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_course, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.Course c = items.get(position);
            h.title.setText(c.title);
            h.meta.setText(c.level + " · " + c.lessonsCount + " lessons · " + Ui.money(c.price));
            h.delete.setOnClickListener(v -> {
                if (callback != null) callback.delete(c);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView title, meta;
            final MaterialButton delete;

            Holder(View v) {
                super(v);
                title = v.findViewById(R.id.tvAdminCourseTitle);
                meta = v.findViewById(R.id.tvAdminCourseMeta);
                delete = v.findViewById(R.id.btnDeleteCourse);
            }
        }
    }

    public static class GlossaryTerm {
        public String term;
        public String definition;

        public GlossaryTerm(String term, String definition) {
            this.term = term;
            this.definition = definition;
        }
    }

    public static class GlossaryAdapter extends RecyclerView.Adapter<GlossaryAdapter.Holder> {
        private final List<GlossaryTerm> items = new ArrayList<>();
        private final List<GlossaryTerm> all = new ArrayList<>();

        public void setItems(List<GlossaryTerm> data) {
            all.clear();
            all.addAll(data);
            filter("");
        }

        public void filter(String query) {
            items.clear();
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            for (GlossaryTerm t : all) {
                if (q.isEmpty() || t.term.toLowerCase(Locale.ROOT).contains(q)
                        || t.definition.toLowerCase(Locale.ROOT).contains(q)) {
                    items.add(t);
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_glossary, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            h.term.setText(items.get(position).term);
            h.definition.setText(items.get(position).definition);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView term, definition;

            Holder(View v) {
                super(v);
                term = v.findViewById(R.id.tvGlossaryTerm);
                definition = v.findViewById(R.id.tvGlossaryDefinition);
            }
        }
    }

    public static class TickerAdapter extends RecyclerView.Adapter<TickerAdapter.Holder> {
        private final List<Models.MarketPair> items = new ArrayList<>();

        public void setItems(List<Models.MarketPair> data) {
            items.clear();
            if (data != null) items.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ticker, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Models.MarketPair p = items.get(position);
            boolean up = p.changePct >= 0;
            h.symbol.setText(p.symbol);
            h.price.setText(Ui.price(p.price));
            h.change.setText(Ui.pct(p.changePct));
            h.change.setTextColor(ContextCompat.getColor(h.itemView.getContext(),
                    up ? R.color.green_text : R.color.red_text));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView symbol, price, change;

            Holder(View v) {
                super(v);
                symbol = v.findViewById(R.id.tvTickerSymbol);
                price = v.findViewById(R.id.tvTickerPrice);
                change = v.findViewById(R.id.tvTickerChange);
            }
        }
    }
}
