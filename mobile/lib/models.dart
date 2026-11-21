double _d(dynamic value) {
  if (value is num) return value.toDouble();
  return double.tryParse('${value ?? ''}') ?? 0;
}

int _i(dynamic value) {
  if (value is num) return value.toInt();
  return int.tryParse('${value ?? ''}') ?? 0;
}

String? _s(dynamic value) => value == null ? null : '$value';

List<dynamic> _l(dynamic value) => value is List ? value : const [];

class AppUser {
  const AppUser({
    required this.id,
    required this.name,
    required this.email,
    required this.phone,
    required this.role,
    required this.membership,
    required this.points,
    this.createdAt,
    this.avatarUrl,
  });

  final int id;
  final String name;
  final String email;
  final String phone;
  final String role;
  final String membership;
  final int points;
  final String? createdAt;
  final String? avatarUrl;

  bool get isAdmin => role == 'ADMIN';

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
    id: _i(json['id']),
    name: _s(json['name']) ?? 'Trader',
    email: _s(json['email']) ?? '',
    phone: _s(json['phone']) ?? '',
    role: _s(json['role']) ?? 'USER',
    membership: _s(json['membership']) ?? 'NONE',
    points: _i(json['points']),
    createdAt: _s(json['createdAt']),
    avatarUrl: _s(json['avatarUrl']),
  );
}

class AuthResult {
  const AuthResult(this.token, this.user);
  final String token;
  final AppUser user;

  factory AuthResult.fromJson(Map<String, dynamic> json) => AuthResult(
    _s(json['token']) ?? '',
    AppUser.fromJson((json['user'] as Map).cast<String, dynamic>()),
  );
}

class Signal {
  const Signal({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.symbol,
    required this.side,
    required this.status,
    required this.roomLabel,
    required this.entry,
    required this.stopLoss,
    required this.targets,
    required this.ruleName,
    required this.participants,
    this.createdAt,
  });

  final int id;
  final String title;
  final String subtitle;
  final String symbol;
  final String side;
  final String status;
  final String roomLabel;
  final String entry;
  final String stopLoss;
  final String targets;
  final String ruleName;
  final int participants;
  final String? createdAt;

  bool get isBuy => side == 'BUY';
  bool get isLive => status == 'LIVE';

  factory Signal.fromJson(Map<String, dynamic> json) => Signal(
    id: _i(json['id']),
    title: _s(json['title']) ?? 'Signal',
    subtitle: _s(json['subtitle']) ?? '',
    symbol: _s(json['symbol']) ?? '',
    side: _s(json['side']) ?? 'BUY',
    status: _s(json['status']) ?? 'LIVE',
    roomLabel: _s(json['roomLabel']) ?? 'OPEN FLOOR',
    entry: _s(json['entry']) ?? '',
    stopLoss: _s(json['stopLoss']) ?? '',
    targets: _s(json['targets']) ?? '',
    ruleName: _s(json['ruleName']) ?? '',
    participants: _i(json['participants']),
    createdAt: _s(json['createdAt']),
  );
}

class HomeData {
  const HomeData({
    required this.name,
    required this.membership,
    required this.fact,
    required this.promotionTitle,
    required this.promotionBody,
    required this.points,
    required this.liveCount,
  });

  final String name;
  final String membership;
  final String fact;
  final String promotionTitle;
  final String promotionBody;
  final int points;
  final int liveCount;

  factory HomeData.fromJson(Map<String, dynamic> json) => HomeData(
    name: _s(json['name']) ?? '',
    membership: _s(json['membership']) ?? 'NONE',
    fact: _s(json['fact']) ?? '',
    promotionTitle: _s(json['promotionTitle']) ?? '',
    promotionBody: _s(json['promotionBody']) ?? '',
    points: _i(json['points']),
    liveCount: _i(json['liveCount']),
  );
}

class Room {
  const Room({
    required this.id,
    required this.name,
    required this.description,
    required this.preview,
    required this.unreadCount,
    this.lastActivityAt,
    required this.locked,
    this.requiredMembership,
  });

  final int id;
  final String name;
  final String description;
  final String preview;
  final int unreadCount;
  final String? lastActivityAt;
  final bool locked;
  final String? requiredMembership;

  factory Room.fromJson(Map<String, dynamic> json) => Room(
    id: _i(json['id']),
    name: _s(json['name']) ?? 'Room',
    description: _s(json['description']) ?? '',
    preview: _s(json['preview']) ?? '',
    unreadCount: _i(json['unreadCount']),
    lastActivityAt: _s(json['lastActivityAt']),
    locked: json['locked'] == true,
    requiredMembership: _s(json['requiredMembership']),
  );
}

class FeedItem {
  const FeedItem({
    required this.id,
    required this.roomId,
    required this.badge,
    required this.title,
    required this.side,
    required this.preview,
    required this.roomName,
    required this.participants,
    required this.open,
    this.createdById,
    this.createdByName = '',
    this.createdAt,
    this.lastActivityAt,
  });

  final int id;
  final int roomId;
  final String badge;
  final String title;
  final String side;
  final String preview;
  final String roomName;
  final int participants;
  final bool open;
  final int? createdById;
  final String createdByName;
  final String? createdAt;
  final String? lastActivityAt;

  bool get isBuy => side == 'BUY';

  factory FeedItem.fromJson(Map<String, dynamic> json) => FeedItem(
    id: _i(json['id']),
    roomId: _i(json['roomId']),
    badge: _s(json['badge']) ?? '',
    title: _s(json['title']) ?? '',
    side: _s(json['side']) ?? 'BUY',
    preview: _s(json['preview']) ?? '',
    roomName: _s(json['roomName']) ?? '',
    participants: _i(json['participants']),
    open: json['open'] == true,
    createdById: json['createdById'] == null ? null : _i(json['createdById']),
    createdByName: _s(json['createdByName']) ?? '',
    createdAt: _s(json['createdAt']),
    lastActivityAt: _s(json['lastActivityAt']),
  );
}

class RoomMessage {
  const RoomMessage({
    required this.id,
    required this.roomId,
    this.authorId,
    required this.author,
    this.authorAvatarUrl,
    required this.text,
    this.attachmentUrl,
    this.createdAt,
  });

  final int id;
  final int roomId;
  final int? authorId;
  final String author;
  final String? authorAvatarUrl;
  final String text;
  final String? attachmentUrl;
  final String? createdAt;

  factory RoomMessage.fromJson(Map<String, dynamic> json) => RoomMessage(
    id: _i(json['id']),
    roomId: _i(json['roomId']),
    authorId: json['authorId'] == null ? null : _i(json['authorId']),
    author: _s(json['author']) ?? '',
    authorAvatarUrl: _s(json['authorAvatarUrl']),
    text: _s(json['text']) ?? '',
    attachmentUrl: _s(json['attachmentUrl']),
    createdAt: _s(json['createdAt']),
  );
}

class WalletTx {
  const WalletTx({
    required this.id,
    required this.type,
    required this.description,
    required this.status,
    required this.amount,
    this.createdAt,
  });

  final int id;
  final String type;
  final String description;
  final String status;
  final double amount;
  final String? createdAt;

  factory WalletTx.fromJson(Map<String, dynamic> json) => WalletTx(
    id: _i(json['id']),
    type: _s(json['type']) ?? 'DEPOSIT',
    description: _s(json['description']) ?? '',
    status: _s(json['status']) ?? 'PENDING',
    amount: _d(json['amount']),
    createdAt: _s(json['createdAt']),
  );
}

class WalletData {
  const WalletData({
    required this.transactions,
    required this.pendingDeposits,
    required this.pendingWithdrawals,
  });

  final List<WalletTx> transactions;
  final int pendingDeposits;
  final int pendingWithdrawals;

  int get pending => pendingDeposits + pendingWithdrawals;

  factory WalletData.fromJson(Map<String, dynamic> json) => WalletData(
    transactions: _l(json['transactions'])
        .map((e) => WalletTx.fromJson((e as Map).cast<String, dynamic>()))
        .toList(),
    pendingDeposits: _i(json['pendingDeposits']),
    pendingWithdrawals: _i(json['pendingWithdrawals']),
  );
}

class DepositResult {
  const DepositResult({
    required this.id,
    required this.broker,
    required this.clientId,
    required this.status,
    required this.wallet,
    required this.mobile,
    required this.amount,
    required this.feePct,
    required this.fee,
    required this.totalPaid,
    this.adminNote,
    this.createdAt,
  });

  final int id;
  final String broker;
  final String clientId;
  final String status;
  final String wallet;
  final String mobile;
  final double amount;
  final double feePct;
  final double fee;
  final double totalPaid;
  final String? adminNote;
  final String? createdAt;

  factory DepositResult.fromJson(Map<String, dynamic> json) => DepositResult(
    id: _i(json['id']),
    broker: _s(json['broker']) ?? '',
    clientId: _s(json['clientId']) ?? '',
    status: _s(json['status']) ?? 'PENDING',
    wallet: _s(json['wallet']) ?? '',
    mobile: _s(json['mobile']) ?? '',
    amount: _d(json['amount']),
    feePct: _d(json['feePct']),
    fee: _d(json['fee']),
    totalPaid: _d(json['totalPaid']),
    adminNote: _s(json['adminNote']),
    createdAt: _s(json['createdAt']),
  );
}

class WithdrawResult {
  const WithdrawResult({
    required this.id,
    required this.payout,
    required this.mobile,
    required this.sourceType,
    required this.sourceDetail,
    required this.proofUrl,
    required this.status,
    required this.amount,
    required this.feePct,
    required this.fee,
    required this.receive,
    this.adminNote,
    this.createdAt,
  });

  final int id;
  final String payout;
  final String mobile;
  final String sourceType;
  final String sourceDetail;
  final String proofUrl;
  final String status;
  final double amount;
  final double feePct;
  final double fee;
  final double receive;
  final String? adminNote;
  final String? createdAt;

  factory WithdrawResult.fromJson(Map<String, dynamic> json) => WithdrawResult(
    id: _i(json['id']),
    payout: _s(json['payout']) ?? '',
    mobile: _s(json['mobile']) ?? '',
    sourceType: _s(json['sourceType']) ?? '',
    sourceDetail: _s(json['sourceDetail']) ?? '',
    proofUrl: _s(json['proofUrl']) ?? '',
    status: _s(json['status']) ?? 'PENDING',
    amount: _d(json['amount']),
    feePct: _d(json['feePct']),
    fee: _d(json['fee']),
    receive: _d(json['receive']),
    adminNote: _s(json['adminNote']),
    createdAt: _s(json['createdAt']),
  );
}

class Purchase {
  const Purchase({
    required this.id,
    required this.type,
    required this.status,
    required this.amount,
    this.courseId,
    this.proofUrl,
    this.adminNote,
    this.createdAt,
  });

  final int id;
  final String type;
  final String status;
  final double amount;
  final int? courseId;
  final String? proofUrl;
  final String? adminNote;
  final String? createdAt;

  factory Purchase.fromJson(Map<String, dynamic> json) => Purchase(
    id: _i(json['id']),
    type: _s(json['type']) ?? '',
    status: _s(json['status']) ?? 'PENDING',
    amount: _d(json['amount']),
    courseId: json['courseId'] == null ? null : _i(json['courseId']),
    proofUrl: _s(json['proofUrl']),
    adminNote: _s(json['adminNote']),
    createdAt: _s(json['createdAt']),
  );
}

class Course {
  const Course({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.description,
    required this.level,
    required this.price,
    required this.lessonsCount,
    this.imageUrl = '',
    this.createdAt,
  });

  final int id;
  final String title;
  final String subtitle;
  final String description;
  final String level;
  final double price;
  final int lessonsCount;
  final String imageUrl;
  final String? createdAt;

  factory Course.fromJson(Map<String, dynamic> json) => Course(
    id: _i(json['id']),
    title: _s(json['title']) ?? 'Course',
    subtitle: _s(json['subtitle']) ?? '',
    description: _s(json['description']) ?? '',
    level: _s(json['level']) ?? 'Beginner',
    price: _d(json['price']),
    lessonsCount: _i(json['lessonsCount']),
    imageUrl: _s(json['imageUrl']) ?? '',
    createdAt: _s(json['createdAt']),
  );
}

class CourseRow {
  const CourseRow({
    required this.course,
    required this.enrolled,
    required this.completedLessons,
    required this.totalLessons,
    required this.progressPct,
  });

  final Course course;
  final bool enrolled;
  final int completedLessons;
  final int totalLessons;
  final int progressPct;

  factory CourseRow.fromJson(Map<String, dynamic> json) => CourseRow(
    course: Course.fromJson((json['course'] as Map).cast<String, dynamic>()),
    enrolled: json['enrolled'] == true,
    completedLessons: _i(json['completedLessons']),
    totalLessons: _i(json['totalLessons']),
    progressPct: _i(json['progressPct']),
  );
}

class Lesson {
  const Lesson({
    required this.id,
    required this.courseId,
    required this.title,
    required this.content,
    required this.videoUrl,
    this.materialUrl,
    this.imageUrl,
    required this.durationMin,
    required this.orderIndex,
  });

  final int id;
  final int courseId;
  final String title;
  final String content;
  final String videoUrl;
  final String? materialUrl;
  final String? imageUrl;
  final int durationMin;
  final int orderIndex;

  factory Lesson.fromJson(Map<String, dynamic> json) => Lesson(
    id: _i(json['id']),
    courseId: _i(json['courseId']),
    title: _s(json['title']) ?? 'Lesson',
    content: _s(json['content']) ?? '',
    videoUrl: _s(json['videoUrl']) ?? '',
    materialUrl: _s(json['materialUrl']),
    imageUrl: _s(json['imageUrl']),
    durationMin: _i(json['durationMin']),
    orderIndex: _i(json['orderIndex']),
  );
}

class AcademyComment {
  const AcademyComment({
    required this.id,
    required this.courseId,
    this.lessonId,
    this.authorId,
    required this.authorName,
    required this.text,
    this.createdAt,
  });

  final int id;
  final int courseId;
  final int? lessonId;
  final int? authorId;
  final String authorName;
  final String text;
  final String? createdAt;

  factory AcademyComment.fromJson(Map<String, dynamic> json) => AcademyComment(
    id: _i(json['id']),
    courseId: _i(json['courseId']),
    lessonId: json['lessonId'] == null ? null : _i(json['lessonId']),
    authorId: json['authorId'] == null ? null : _i(json['authorId']),
    authorName: _s(json['authorName']) ?? '',
    text: _s(json['text']) ?? '',
    createdAt: _s(json['createdAt']),
  );
}

class CourseDetail {
  const CourseDetail({
    required this.course,
    required this.lessons,
    required this.enrolled,
    required this.completedLessonIds,
  });

  final Course course;
  final List<Lesson> lessons;
  final bool enrolled;
  final List<int> completedLessonIds;

  factory CourseDetail.fromJson(Map<String, dynamic> json) => CourseDetail(
    course: Course.fromJson((json['course'] as Map).cast<String, dynamic>()),
    lessons: _l(json['lessons'])
        .map((e) => Lesson.fromJson((e as Map).cast<String, dynamic>()))
        .toList(),
    enrolled: json['enrolled'] == true,
    completedLessonIds: _l(json['completedLessonIds']).map(_i).toList(),
  );
}

class Settings {
  const Settings({
    required this.adminFeePct,
    required this.withdrawFeePct,
    required this.minDeposit,
    required this.maxDeposit,
    required this.minWithdraw,
    required this.fact,
    required this.promotionTitle,
    required this.promotionBody,
    required this.pointsPerReferral,
  });

  final double adminFeePct;
  final double withdrawFeePct;
  final double minDeposit;
  final double maxDeposit;
  final double minWithdraw;
  final String fact;
  final String promotionTitle;
  final String promotionBody;
  final int pointsPerReferral;

  factory Settings.fromJson(Map<String, dynamic> json) => Settings(
    adminFeePct: _d(json['adminFeePct']),
    withdrawFeePct: _d(json['withdrawFeePct']),
    minDeposit: _d(json['minDeposit']),
    maxDeposit: _d(json['maxDeposit']),
    minWithdraw: _d(json['minWithdraw']),
    fact: _s(json['fact']) ?? '',
    promotionTitle: _s(json['promotionTitle']) ?? '',
    promotionBody: _s(json['promotionBody']) ?? '',
    pointsPerReferral: _i(json['pointsPerReferral']),
  );
}

class AdminRequests {
  const AdminRequests({
    required this.deposits,
    required this.withdrawals,
    required this.purchases,
  });

  final List<DepositResult> deposits;
  final List<WithdrawResult> withdrawals;
  final List<Purchase> purchases;

  factory AdminRequests.fromJson(Map<String, dynamic> json) => AdminRequests(
    deposits: _l(json['deposits'])
        .map((e) => DepositResult.fromJson((e as Map).cast<String, dynamic>()))
        .toList(),
    withdrawals: _l(json['withdrawals'])
        .map((e) => WithdrawResult.fromJson((e as Map).cast<String, dynamic>()))
        .toList(),
    purchases: _l(json['purchases'])
        .map((e) => Purchase.fromJson((e as Map).cast<String, dynamic>()))
        .toList(),
  );
}
