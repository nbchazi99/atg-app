package com.atg.app.data;

import java.util.List;

public class Models {

    public static class User {
        public long id;
        public String name;
        public String email;
        public String phone;
        public String role;
        public String membership;
        public int points;
        public String createdAt;
    }

    public static class AuthRes {
        public String token;
        public User user;
    }

    public static class LoginReq {
        public String email;
        public String password;

        public LoginReq(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class RegisterReq {
        public String name;
        public String email;
        public String phone;
        public String password;

        public RegisterReq(String name, String email, String phone, String password) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.password = password;
        }
    }

    public static class MarketPair {
        public long id;
        public String symbol;
        public String name;
        public String assetClass;
        public double price;
        public double changePct;
        public double high;
        public double low;
        public double prevClose;
        public String updatedAt;
    }

    public static class Signal {
        public long id;
        public String title;
        public String subtitle;
        public String symbol;
        public String side;
        public String status;
        public String roomLabel;
        public String entry;
        public String stopLoss;
        public String targets;
        public String ruleName;
        public int participants;
        public String createdAt;
    }

    public static class PairRes {
        public MarketPair pair;
        public List<Signal> calls;
        public boolean favourite;
    }

    public static class HomeRes {
        public String name;
        public String membership;
        public String fact;
        public String promotionTitle;
        public String promotionBody;
        public int points;
        public int liveCount;
        public List<MarketPair> tickers;
    }

    public static class Room {
        public long id;
        public String name;
        public String description;
        public String preview;
        public int unreadCount;
        public String lastActivityAt;
        public boolean locked;
        public String requiredMembership;
    }

    public static class FeedItem {
        public long id;
        public long roomId;
        public String badge;
        public String title;
        public String side;
        public String preview;
        public String roomName;
        public int participants;
        public boolean open;
        public String createdAt;
    }

    public static class RoomMessage {
        public long id;
        public long roomId;
        public String author;
        public String text;
        public String createdAt;
    }

    public static class TextReq {
        public String text;

        public TextReq(String text) {
            this.text = text;
        }
    }

    public static class WalletTx {
        public long id;
        public String type;
        public String description;
        public String status;
        public String createdAt;
        public double amount;
    }

    public static class WalletRes {
        public List<WalletTx> transactions;
        public long pendingDeposits;
        public long pendingWithdrawals;
    }

    public static class DepositReq {
        public String broker;
        public String clientId;
        public double amount;
        public String wallet;
        public String mobile;
    }

    public static class DepositRes {
        public long id;
        public String broker;
        public String clientId;
        public String status;
        public String wallet;
        public String mobile;
        public double amount;
        public double feePct;
        public double fee;
        public double totalPaid;
        public String adminNote;
        public String createdAt;
    }

    public static class WithdrawReq {
        public String payout;
        public String mobile;
        public double amount;
        public String sourceType;
        public String sourceDetail;
        public String proofUrl;
    }

    public static class WithdrawRes {
        public long id;
        public String payout;
        public String mobile;
        public String sourceType;
        public String sourceDetail;
        public String proofUrl;
        public String status;
        public String adminNote;
        public double amount;
        public double feePct;
        public double fee;
        public double receive;
        public String createdAt;
    }

    public static class Course {
        public long id;
        public String title;
        public String subtitle;
        public String description;
        public String level;
        public double price;
        public int lessonsCount;
        public String createdAt;
    }

    public static class CourseRow {
        public Course course;
        public boolean enrolled;
        public int completedLessons;
        public int totalLessons;
        public int progressPct;
    }

    public static class Lesson {
        public long id;
        public long courseId;
        public String title;
        public String content;
        public String videoUrl;
        public int durationMin;
        public int orderIndex;
    }

    public static class CourseDetail {
        public Course course;
        public List<Lesson> lessons;
        public boolean enrolled;
        public List<Long> completedLessonIds;
    }

    public static class CompleteReq {
        public long lessonId;

        public CompleteReq(long lessonId) {
            this.lessonId = lessonId;
        }
    }

    public static class PurchaseReq {
        public String type;
        public Long courseId;
        public String proofUrl;

        public PurchaseReq(String type, Long courseId, String proofUrl) {
            this.type = type;
            this.courseId = courseId;
            this.proofUrl = proofUrl;
        }
    }

    public static class PurchaseRes {
        public long id;
        public String type;
        public String status;
        public Long courseId;
        public String proofUrl;
        public String adminNote;
        public double amount;
        public String createdAt;
    }

    public static class SettingsRes {
        public double adminFeePct;
        public double withdrawFeePct;
        public double minDeposit;
        public double maxDeposit;
        public double minWithdraw;
        public int pointsPerReferral;
        public String fact;
        public String promotionTitle;
        public String promotionBody;
    }

    public static class UploadRes {
        public String url;
    }

    public static class AdminRequests {
        public List<DepositRes> deposits;
        public List<WithdrawRes> withdrawals;
        public List<PurchaseRes> purchases;
        public List<WalletTx> transactions;
    }

    public static class Decision {
        public boolean approve;
        public String note;

        public Decision(boolean approve, String note) {
            this.approve = approve;
            this.note = note;
        }
    }

    public static class MembershipReq {
        public String membership;

        public MembershipReq(String membership) {
            this.membership = membership;
        }
    }

    public static class SignalReq {
        public String title;
        public String subtitle;
        public String symbol;
        public String side;
        public String status;
        public String roomLabel;
        public String entry;
        public String stopLoss;
        public String targets;
        public String ruleName;
        public Integer participants;
    }

    public static class LessonReq {
        public String title;
        public int durationMin;
        public String content;
        public String videoUrl;
    }

    public static class CourseReq {
        public String title;
        public String subtitle;
        public String description;
        public double price;
        public String level;
        public List<LessonReq> lessons;
    }

    public static class ErrorRes {
        public String error;
    }

    public static class SettingsReq {
        public Double adminFeePct;
        public Double withdrawFeePct;
        public Double minDeposit;
        public Double maxDeposit;
        public Double minWithdraw;
        public String fact;
        public String promotionTitle;
        public String promotionBody;
    }
}
