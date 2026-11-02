package com.atg.api.config;

import com.atg.api.model.*;
import com.atg.api.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final MarketPairRepository markets;
    private final SignalRepository signals;
    private final RoomRepository rooms;
    private final RoomMessageRepository messages;
    private final RoomPostRepository posts;
    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final AppSettingsRepository settings;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, MarketPairRepository markets, SignalRepository signals,
                      RoomRepository rooms, RoomMessageRepository messages, RoomPostRepository posts,
                      CourseRepository courses, LessonRepository lessons, AppSettingsRepository settings,
                      PasswordEncoder encoder) {
        this.users = users;
        this.markets = markets;
        this.signals = signals;
        this.rooms = rooms;
        this.messages = messages;
        this.posts = posts;
        this.courses = courses;
        this.lessons = lessons;
        this.settings = settings;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (settings.findById(1L).isEmpty()) {
            settings.save(new AppSettings());
        }
        if (users.count() > 0) {
            return;
        }

        User admin = new User();
        admin.setName("ATG Admin");
        admin.setEmail("admin@atg.app");
        admin.setPhone("+263787322850");
        admin.setPasswordHash(encoder.encode("Admin@123"));
        admin.setRole(User.Role.ADMIN);
        admin.setPoints(0);
        users.save(admin);

        User user = new User();
        user.setName("Blessed Noah");
        user.setEmail("blessed@atg.app");
        user.setPhone("+263787322850");
        user.setPasswordHash(encoder.encode("User@123"));
        user.setRole(User.Role.USER);
        user.setPoints(2);
        users.save(user);

        seedMarkets();
        seedSignals();
        seedRooms();
        seedCourses();
    }

    private void seedMarkets() {
        market("XAUUSD", "Gold", MarketPair.AssetClass.METALS, "4168.83", "-0.64", "4210.50", "4120.20", "4195.66");
        market("XAGUSD", "Silver", MarketPair.AssetClass.METALS, "52.14", "0.33", "52.60", "51.40", "51.97");
        market("EURUSD", "Euro / US Dollar", MarketPair.AssetClass.FOREX, "1.11718", "-0.21", "1.12171", "1.11697", "1.11951");
        market("GBPUSD", "Pound / US Dollar", MarketPair.AssetClass.FOREX, "1.32322", "0.02", "1.32650", "1.31752", "1.32295");
        market("USDJPY", "US Dollar / Yen", MarketPair.AssetClass.FOREX, "158.664", "0.21", "158.900", "158.110", "158.330");
        market("GBPJPY", "Pound / Yen", MarketPair.AssetClass.FOREX, "209.820", "0.16", "210.400", "209.020", "209.480");
        market("AUDUSD", "Aussie / US Dollar", MarketPair.AssetClass.FOREX, "0.67284", "-0.12", "0.67520", "0.67110", "0.67370");
        market("USDCAD", "US Dollar / Loonie", MarketPair.AssetClass.FOREX, "1.39042", "0.08", "1.39310", "1.38820", "1.38930");
        market("NZDUSD", "Kiwi / US Dollar", MarketPair.AssetClass.FOREX, "0.60812", "-0.17", "0.61100", "0.60640", "0.60916");
        market("USDCHF", "US Dollar / Franc", MarketPair.AssetClass.FOREX, "0.88426", "0.05", "0.88600", "0.88290", "0.88382");
        market("EURGBP", "Euro / Pound", MarketPair.AssetClass.FOREX, "0.84431", "-0.09", "0.84620", "0.84310", "0.84507");
        market("EURJPY", "Euro / Yen", MarketPair.AssetClass.FOREX, "177.312", "0.04", "177.900", "176.800", "177.240");
        market("BTCUSD", "Bitcoin", MarketPair.AssetClass.CRYPTO, "82962.01", "-0.97", "84120.00", "82110.00", "83774.55");
        market("ETHUSD", "Ethereum", MarketPair.AssetClass.CRYPTO, "3120.44", "1.12", "3152.00", "3071.20", "3085.87");
        market("SOLUSD", "Solana", MarketPair.AssetClass.CRYPTO, "146.82", "-1.44", "150.10", "145.20", "148.96");
    }

    private void market(String symbol, String name, MarketPair.AssetClass assetClass, String price,
                        String change, String high, String low, String prevClose) {
        MarketPair p = new MarketPair();
        p.setSymbol(symbol);
        p.setName(name);
        p.setAssetClass(assetClass);
        p.setPrice(new BigDecimal(price));
        p.setChangePct(new BigDecimal(change));
        p.setHigh(new BigDecimal(high));
        p.setLow(new BigDecimal(low));
        p.setPrevClose(new BigDecimal(prevClose));
        p.setUpdatedAt(LocalDateTime.now().minusMinutes(12));
        markets.save(p);
    }

    private void seedSignals() {
        signal("GAINX400", "GAINX400 · M1", "GAINX400", Signal.Side.BUY, Signal.Status.LIVE,
                "OPEN FLOOR", "At market, now", "Not given", "Not given", "M10 rule", 1, 1);
        signal("MAXPAINX1000", "MAXPAINX1000 · M1", "MAXPAINX1000", Signal.Side.SELL, Signal.Status.LIVE,
                "OPEN FLOOR", "At market, now", "Not given", "Not given", "M10 rule", 2, 2);
        signal("PAINX400", "PAINX400 · M1", "PAINX400", Signal.Side.SELL, Signal.Status.LIVE,
                "OPEN FLOOR", "At market, now", "Not given", "Not given", "M10 rule", 2, 10);
        signal("GAINX600", "GAINX600 · M1", "GAINX600", Signal.Side.BUY, Signal.Status.LIVE,
                "OPEN FLOOR", "At market, now", "Not given", "Not given", "M10 rule", 0, 14);
        signal("PAINX999", "PAINX999 · M1", "PAINX999", Signal.Side.BUY, Signal.Status.LIVE,
                "OPEN FLOOR", "At market, now", "Not given", "Not given", "M10 rule", 46, 3);
        signal("MAXGAINX2000", "MAXGAINX2000 · M1", "MAXGAINX2000", Signal.Side.BUY, Signal.Status.INNER,
                "INNER CIRCLE", "At market, now", "Not given", "Not given", "M10 rule", 4, 20);
        signal("EURUSD", "EUR/USD · M15", "EURUSD", Signal.Side.SELL, Signal.Status.CLOSED,
                "OPEN FLOOR", "1.11900", "1.12150", "1.11500", "M15 confluence", 0, 120);
    }

    private void signal(String title, String subtitle, String symbol, Signal.Side side, Signal.Status status,
                        String room, String entry, String stop, String targets, String rule, int participants, int minutesAgo) {
        Signal s = new Signal();
        s.setTitle(title);
        s.setSubtitle(subtitle);
        s.setSymbol(symbol);
        s.setSide(side);
        s.setStatus(status);
        s.setRoomLabel(room);
        s.setEntry(entry);
        s.setStopLoss(stop);
        s.setTargets(targets);
        s.setRuleName(rule);
        s.setParticipants(participants);
        s.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        signals.save(s);
    }

    private void seedRooms() {
        Room floor = room("ATG Trading Floor", "The main room. Every call, live.",
                "Valentine: Tired of trading blind? We go...", 99, 59);
        Room deriv = room("Deriv VIP", "Calls and updates for Deriv VIP members.",
                "Kupiwa: In", 12, 8);
        Room weltrade = room("Weltrade VIP", "Calls and updates for Weltrade VIP members.",
                "Tadziwa: Tell us when to close!!", 4, 18);

        message(floor, "Valentine", "Tired of trading blind? We go live at the top of the hour.", 59);
        message(floor, "Kupiwa", "In", 4);
        message(floor, "Tadiwanashe", "Deep blueesssss 💙", 3);
        message(floor, "Paula", "Sent an image", 9);
        message(floor, "Tadziwa", "Tell us when to close!!", 18);
        message(floor, "Tanatsa", "Observe the 10", 21);

        message(deriv, "Kupiwa", "In", 8);
        message(deriv, "Tanatsa", "Observe the 10", 20);

        message(weltrade, "Tadziwa", "Tell us when to close!!", 17);
        message(weltrade, "Valentine", "Locked in.", 25);

        post(floor, "GAIN", "GAINX400", Signal.Side.BUY, "Kupiwa: In", 2, true, 0);
        post(floor, "PAIN", "PAINX999", Signal.Side.BUY, "Tadiwanashe: Deep blueesssss 💙", 46, true, 2);
        post(floor, "MAXP", "MAXPAINX1000", Signal.Side.SELL, "Nothing yet. Say the first thing.", 0, true, 5);
        post(floor, "PAIN", "PAINX400", Signal.Side.SELL, "Paula: Sent an image", 2, true, 9);
        post(floor, "GAIN", "GAINX600", Signal.Side.BUY, "Nothing yet. Say the first thing.", 0, true, 14);
        post(floor, "MAXP", "MAXPAINX1000", Signal.Side.SELL, "Tadziwa: Tell us when to close!!", 15, true, 18);
        post(floor, "MAXG", "MAXGAINX2000", Signal.Side.BUY, "Tanatsa: Observe the 10", 4, true, 21);
        post(deriv, "EURU", "EURUSD", Signal.Side.SELL, "Setup shared. Manage your risk.", 9, false, 120);
    }

    private Room room(String name, String description, String preview, int unread, int minutesAgo) {
        Room r = new Room();
        r.setName(name);
        r.setDescription(description);
        r.setPreview(preview);
        r.setUnreadCount(unread);
        r.setLastActivityAt(LocalDateTime.now().minusMinutes(minutesAgo));
        return rooms.save(r);
    }

    private void message(Room room, String author, String text, int minutesAgo) {
        RoomMessage m = new RoomMessage();
        m.setRoomId(room.getId());
        m.setAuthor(author);
        m.setText(text);
        m.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        messages.save(m);
    }

    private void post(Room room, String badge, String title, Signal.Side side, String preview,
                      int participants, boolean open, int minutesAgo) {
        RoomPost p = new RoomPost();
        p.setRoomId(room.getId());
        p.setBadge(badge);
        p.setTitle(title);
        p.setSide(side);
        p.setPreview(preview);
        p.setParticipants(participants);
        p.setOpen(open);
        p.setCreatedAt(LocalDateTime.now().minusMinutes(minutesAgo));
        posts.save(p);
    }

    private void seedCourses() {
        course("Zero to First Trade", "Start here", 20, "Beginner",
                "From pip to position size. Learn how a trade is built, placed and closed with confidence.",
                "What a pip is and how it moves your money.",
                "Reading a quote and placing your first order.",
                "Stop loss, take profit and the 1% rule.",
                "Your first trade, step by step.");
        course("Risk & Position Sizing", "Protect the account", 25, "Intermediate",
                "The maths that keeps you in the game when the market goes against you.",
                "Risk per trade and how to cap it.",
                "Lot size from stop distance.",
                "Scaling out without giving back the edge.");
        course("Price Action Fundamentals", "Read the chart", 30, "Advanced",
                "Structure, liquidity and the sessions that move the majors.",
                "Market structure: higher highs, lower lows.",
                "Supply, demand and the open floor rule.",
                "Trading the London and New York sessions.");
    }

    private Course course(String title, String subtitle, int price, String level, String description, String... lessonTitles) {
        Course c = new Course();
        c.setTitle(title);
        c.setSubtitle(subtitle);
        c.setDescription(description);
        c.setPrice(BigDecimal.valueOf(price));
        c.setLevel(level);
        c = courses.save(c);
        int i = 0;
        for (String t : lessonTitles) {
            Lesson l = new Lesson();
            l.setCourseId(c.getId());
            l.setTitle(t);
            l.setDurationMin(12 + i * 4);
            l.setOrderIndex(i);
            l.setContent("Lesson " + (i + 1) + " of " + title + "\n\n" +
                    t + "\n\n" +
                    "This lesson walks through the rule with live examples. Read it, then open the chart in Markets " +
                    "and mark the levels. Nothing here replaces your own practice: demo first, then size small.\n\n" +
                    "Key points\n" +
                    "• One rule at a time. Practise it until it is boring.\n" +
                    "• Write the trade down before you take it: entry, stop, target, risk.\n" +
                    "• If the setup is not on your checklist, you do not take it.");
            lessons.save(l);
            i++;
        }
        c.setLessonsCount(i);
        return courses.save(c);
    }
}
