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
    this.membershipExpiresAt,
    this.username,
    this.country,
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
  final String? membershipExpiresAt;
  final String? username;
  final String? country;

  bool get isAdmin => role == 'ADMIN';

  bool get membershipExpired {
    final raw = membershipExpiresAt;
    if (raw == null || raw.isEmpty) return false;
    final at = DateTime.tryParse(raw)?.toLocal();
    if (at == null) return false;
    return at.isBefore(DateTime.now());
  }

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
    membershipExpiresAt: _s(json['membershipExpiresAt']),
    username: _s(json['username']),
    country: _s(json['country']),
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

  factory AuthResult.fromProfileUpdateJson(Map<String, dynamic> json) =>
      AuthResult(_s(json['token']) ?? '', AppUser.fromJson(json));
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
    this.result,
    this.closingPrice,
    this.closingUpdate,
    this.closedAt,
    this.updateHistory = const [],
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
  final String? result;
  final String? closingPrice;
  final String? closingUpdate;
  final String? closedAt;
  final List<String> updateHistory;

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
    result: _s(json['result']),
    closingPrice: _s(json['closingPrice']),
    closingUpdate: _s(json['closingUpdate']),
    closedAt: _s(json['closedAt']),
    updateHistory: _l(json['updateHistory']).map((item) => '$item').toList(),
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
    this.createdByAvatarUrl,
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
  final String? createdByAvatarUrl;
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
    createdByAvatarUrl: _s(json['createdByAvatarUrl']),
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
    this.attachmentType,
    this.createdAt,
  });

  final int id;
  final int roomId;
  final int? authorId;
  final String author;
  final String? authorAvatarUrl;
  final String text;
  final String? attachmentUrl;
  final String? attachmentType;
  final String? createdAt;

  factory RoomMessage.fromJson(Map<String, dynamic> json) => RoomMessage(
    id: _i(json['id']),
    roomId: _i(json['roomId']),
    authorId: json['authorId'] == null ? null : _i(json['authorId']),
    author: _s(json['author']) ?? '',
    authorAvatarUrl: _s(json['authorAvatarUrl']),
    text: _s(json['text']) ?? '',
    attachmentUrl: _s(json['attachmentUrl']),
    attachmentType: _s(json['attachmentType']),
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
    this.username,
    this.weltradeId,
    this.phone,
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
  final String? username;
  final String? weltradeId;
  final String? phone;

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
    username: _s(json['username']),
    weltradeId: _s(json['weltradeId']) ?? _s(json['clientId']),
    phone: _s(json['phone']),
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
    this.reviewStatus = 'PUBLISHED',
    this.reviewNote,
    this.reviewedAt,
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
  final String reviewStatus;
  final String? reviewNote;
  final String? reviewedAt;

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
    reviewStatus: _s(json['reviewStatus']) ?? 'PUBLISHED',
    reviewNote: _s(json['reviewNote']),
    reviewedAt: _s(json['reviewedAt']),
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
    required this.maxWithdraw,
    required this.fact,
    required this.promotionTitle,
    required this.promotionBody,
    required this.pointsPerReferral,
    required this.depositMethods,
    required this.withdrawalMethods,
    required this.derivVipPrice,
    required this.weltradeVipPrice,
    required this.vipExpiryDays,
  });

  final double adminFeePct;
  final double withdrawFeePct;
  final double minDeposit;
  final double maxDeposit;
  final double minWithdraw;
  final double maxWithdraw;
  final String fact;
  final String promotionTitle;
  final String promotionBody;
  final int pointsPerReferral;
  final List<String> depositMethods;
  final List<String> withdrawalMethods;
  final double derivVipPrice;
  final double weltradeVipPrice;
  final int vipExpiryDays;

  factory Settings.fromJson(Map<String, dynamic> json) => Settings(
    adminFeePct: _d(json['adminFeePct']),
    withdrawFeePct: json['withdrawFeePct'] == null
        ? 10
        : _d(json['withdrawFeePct']),
    minDeposit: _d(json['minDeposit']),
    maxDeposit: _d(json['maxDeposit']),
    minWithdraw: _d(json['minWithdraw']),
    maxWithdraw: json['maxWithdraw'] == null ? 454 : _d(json['maxWithdraw']),
    fact: _s(json['fact']) ?? '',
    promotionTitle: _s(json['promotionTitle']) ?? '',
    promotionBody: _s(json['promotionBody']) ?? '',
    pointsPerReferral: _i(json['pointsPerReferral']),
    depositMethods: const ['EcoCash'],
    withdrawalMethods: const ['EcoCash'],
    derivVipPrice: json['derivVipPrice'] == null
        ? 30
        : _d(json['derivVipPrice']),
    weltradeVipPrice: json['weltradeVipPrice'] == null
        ? 30
        : _d(json['weltradeVipPrice']),
    vipExpiryDays: json['vipExpiryDays'] == null
        ? 30
        : _i(json['vipExpiryDays']),
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

class SupportConversation {
  const SupportConversation({
    required this.id,
    required this.topic,
    required this.status,
    required this.messageCount,
    this.guestToken,
    this.createdAt,
    this.updatedAt,
  });

  final int id;
  final String topic;
  final String status;
  final int messageCount;
  final String? guestToken;
  final String? createdAt;
  final String? updatedAt;

  factory SupportConversation.fromJson(Map<String, dynamic> json) =>
      SupportConversation(
        id: _i(json['id']),
        topic: _s(json['topic']) ?? '',
        status: _s(json['status']) ?? 'OPEN',
        messageCount: _i(json['messageCount']),
        guestToken: _s(json['guestToken']),
        createdAt: _s(json['createdAt']),
        updatedAt: _s(json['updatedAt']),
      );
}

class SupportMessage {
  const SupportMessage({
    required this.id,
    required this.conversationId,
    required this.authorName,
    required this.authorType,
    required this.text,
    required this.internalNote,
    this.ticketId,
    this.authorId,
    this.attachmentUrls = const [],
    this.createdAt,
  });

  final int id;
  final int conversationId;
  final int? ticketId;
  final int? authorId;
  final String authorName;
  final String authorType;
  final String text;
  final List<String> attachmentUrls;
  final bool internalNote;
  final String? createdAt;

  factory SupportMessage.fromJson(Map<String, dynamic> json) => SupportMessage(
    id: _i(json['id']),
    conversationId: _i(json['conversationId']),
    ticketId: json['ticketId'] == null ? null : _i(json['ticketId']),
    authorId: json['authorId'] == null ? null : _i(json['authorId']),
    authorName: _s(json['authorName']) ?? '',
    authorType: _s(json['authorType']) ?? 'USER',
    text: _s(json['text']) ?? '',
    attachmentUrls: _l(json['attachmentUrls']).map((item) => '$item').toList(),
    internalNote: json['internalNote'] == true,
    createdAt: _s(json['createdAt']),
  );
}

class SupportConversationDetail {
  const SupportConversationDetail({
    required this.conversation,
    required this.messages,
  });

  final SupportConversation conversation;
  final List<SupportMessage> messages;

  factory SupportConversationDetail.fromJson(Map<String, dynamic> json) =>
      SupportConversationDetail(
        conversation: SupportConversation.fromJson(
          (json['conversation'] as Map).cast<String, dynamic>(),
        ),
        messages: _l(json['messages'])
            .map(
              (item) => SupportMessage.fromJson(
                (item as Map).cast<String, dynamic>(),
              ),
            )
            .toList(),
      );
}

class SupportChatResult {
  const SupportChatResult({
    required this.conversation,
    required this.userMessage,
    required this.assistantMessage,
    required this.needsHuman,
    this.confidence = 0,
    this.ticket,
  });

  final SupportConversation conversation;
  final SupportMessage userMessage;
  final SupportMessage assistantMessage;
  final bool needsHuman;
  final double confidence;
  final SupportTicket? ticket;

  factory SupportChatResult.fromJson(Map<String, dynamic> json) =>
      SupportChatResult(
        conversation: SupportConversation.fromJson(
          (json['conversation'] as Map).cast<String, dynamic>(),
        ),
        userMessage: SupportMessage.fromJson(
          (json['userMessage'] as Map).cast<String, dynamic>(),
        ),
        assistantMessage: SupportMessage.fromJson(
          (json['assistantMessage'] as Map).cast<String, dynamic>(),
        ),
        needsHuman: json['needsHuman'] == true,
        confidence: (json['confidence'] is num)
            ? (json['confidence'] as num).toDouble()
            : 0,
        ticket: json['ticket'] is Map
            ? SupportTicket.fromJson(
                (json['ticket'] as Map).cast<String, dynamic>(),
              )
            : null,
      );
}

class SupportTicket {
  const SupportTicket({
    required this.id,
    required this.ticketNumber,
    required this.conversationId,
    required this.contactName,
    required this.phone,
    required this.category,
    required this.subject,
    required this.description,
    required this.priority,
    required this.status,
    this.userId,
    this.email,
    this.preferredContact,
    this.assignedAdminId,
    this.assignedAdminName,
    this.createdAt,
    this.updatedAt,
    this.resolvedAt,
  });

  final int id;
  final String ticketNumber;
  final int conversationId;
  final int? userId;
  final String contactName;
  final String phone;
  final String? email;
  final String? preferredContact;
  final String category;
  final String subject;
  final String description;
  final String priority;
  final String status;
  final int? assignedAdminId;
  final String? assignedAdminName;
  final String? createdAt;
  final String? updatedAt;
  final String? resolvedAt;

  factory SupportTicket.fromJson(Map<String, dynamic> json) => SupportTicket(
    id: _i(json['id']),
    ticketNumber: _s(json['ticketNumber']) ?? '',
    conversationId: _i(json['conversationId']),
    userId: json['userId'] == null ? null : _i(json['userId']),
    contactName: _s(json['contactName']) ?? '',
    phone: _s(json['phone']) ?? '',
    email: _s(json['email']),
    preferredContact: _s(json['preferredContact']),
    category: _s(json['category']) ?? 'GENERAL',
    subject: _s(json['subject']) ?? '',
    description: _s(json['description']) ?? '',
    priority: _s(json['priority']) ?? 'NORMAL',
    status: _s(json['status']) ?? 'OPEN',
    assignedAdminId: json['assignedAdminId'] == null
        ? null
        : _i(json['assignedAdminId']),
    assignedAdminName: _s(json['assignedAdminName']),
    createdAt: _s(json['createdAt']),
    updatedAt: _s(json['updatedAt']),
    resolvedAt: _s(json['resolvedAt']),
  );
}

class SupportTicketDetail {
  const SupportTicketDetail({required this.ticket, required this.messages});

  final SupportTicket ticket;
  final List<SupportMessage> messages;

  factory SupportTicketDetail.fromJson(Map<String, dynamic> json) =>
      SupportTicketDetail(
        ticket: SupportTicket.fromJson(
          (json['ticket'] as Map).cast<String, dynamic>(),
        ),
        messages: _l(json['messages'])
            .map(
              (item) => SupportMessage.fromJson(
                (item as Map).cast<String, dynamic>(),
              ),
            )
            .toList(),
      );
}

class SupportKnowledgeEntry {
  const SupportKnowledgeEntry({
    required this.id,
    required this.title,
    required this.category,
    required this.content,
    required this.keywords,
    required this.active,
    this.updatedBy,
    this.createdAt,
    this.updatedAt,
  });

  final int id;
  final String title;
  final String category;
  final String content;
  final String keywords;
  final bool active;
  final int? updatedBy;
  final String? createdAt;
  final String? updatedAt;

  factory SupportKnowledgeEntry.fromJson(Map<String, dynamic> json) =>
      SupportKnowledgeEntry(
        id: _i(json['id']),
        title: _s(json['title']) ?? '',
        category: _s(json['category']) ?? 'GENERAL',
        content: _s(json['content']) ?? '',
        keywords: _s(json['keywords']) ?? '',
        active: json['active'] == true,
        updatedBy: json['updatedBy'] == null ? null : _i(json['updatedBy']),
        createdAt: _s(json['createdAt']),
        updatedAt: _s(json['updatedAt']),
      );
}

/// One payment attempt through the Dischub gateway.
class PaymentTx {
  const PaymentTx({
    required this.id,
    required this.orderId,
    required this.userId,
    required this.type,
    required this.serviceName,
    required this.amount,
    required this.currency,
    required this.status,
    required this.provider,
    this.courseId,
    this.sender,
    this.statusReason,
    this.receiptNo,
    this.purchaseRequestId,
    this.paidAmount,
    this.paymentMethod,
    this.gatewayTimestamp,
    this.gatewayResponse,
    this.ecocashStatus,
    this.refundNote,
    this.createdAt,
    this.paidAt,
    this.refundedAt,
  });

  final int id;
  final String orderId;
  final int userId;
  final String type;
  final String serviceName;
  final double amount;
  final String currency;
  final String status;
  final String provider;
  final int? courseId;
  final String? sender;
  final String? statusReason;
  final String? receiptNo;
  final int? purchaseRequestId;

  /// Gross the customer actually paid (price + gateway charges), when settled.
  final double? paidAmount;
  final String? paymentMethod;
  final String? gatewayTimestamp;

  /// Raw JSON Dischub returned on the last status check.
  final String? gatewayResponse;

  /// Dischub's EcoCash prompt state at creation (e.g. PENDING SUBSCRIBER VALIDATION).
  final String? ecocashStatus;

  final String? refundNote;
  final String? createdAt;
  final String? paidAt;
  final String? refundedAt;

  bool get isOpen => status == 'CREATED' || status == 'PROCESSING';
  bool get isPaid => status == 'SUCCESS';

  /// Charges the customer covered on top of the price, if any.
  double get charges => paidAmount == null ? 0 : (paidAmount! - amount);

  String get statusLabel => switch (status) {
    'SUCCESS' => 'PAID',
    'CREATED' => 'STARTED',
    'PROCESSING' => 'PENDING',
    'FAILED' => 'FAILED',
    'EXPIRED' => 'EXPIRED',
    'REFUNDED' => 'REFUNDED',
    _ => status,
  };

  factory PaymentTx.fromJson(Map<String, dynamic> json) => PaymentTx(
    id: _i(json['id']),
    orderId: _s(json['orderId']) ?? '',
    userId: _i(json['userId']),
    type: _s(json['type']) ?? '',
    serviceName: _s(json['serviceName']) ?? '',
    amount: _d(json['amount']),
    currency: _s(json['currency']) ?? 'USD',
    status: _s(json['status']) ?? 'CREATED',
    provider: _s(json['provider']) ?? 'dischub',
    courseId: json['courseId'] == null ? null : _i(json['courseId']),
    sender: _s(json['sender']),
    statusReason: _s(json['statusReason']),
    receiptNo: _s(json['receiptNo']),
    purchaseRequestId: json['purchaseRequestId'] == null
        ? null
        : _i(json['purchaseRequestId']),
    paidAmount: json['paidAmount'] == null ? null : _d(json['paidAmount']),
    paymentMethod: _s(json['paymentMethod']),
    gatewayTimestamp: _s(json['gatewayTimestamp']),
    gatewayResponse: _s(json['gatewayResponse']),
    ecocashStatus: _s(json['ecocashStatus']),
    refundNote: _s(json['refundNote']),
    createdAt: _s(json['createdAt']),
    paidAt: _s(json['paidAt']),
    refundedAt: _s(json['refundedAt']),
  );
}
