package com.atg.api.web;

import com.atg.api.service.FirebaseCourseAssetStorage;
import com.atg.api.model.Room;
import com.atg.api.repo.RoomRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class UploadController extends BaseController {

    private static final long MAX_GENERAL_UPLOAD_BYTES = 52L * 1024 * 1024;
    private static final long MAX_CHAT_ATTACHMENT_BYTES = 80L * 1024 * 1024;
    private static final long MAX_COURSE_ASSET_BYTES = 250L * 1024 * 1024;
    private static final Set<String> CHAT_ATTACHMENT_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif",
            ".mp4", ".webm", ".mov", ".m4v",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".wav", ".m4a", ".aac", ".ogg", ".opus", ".flac");
    private static final Set<String> COURSE_ASSET_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif",
            ".mp4", ".webm", ".mov", ".m4v",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac");

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final FirebaseCourseAssetStorage courseAssetStorage;
    private final RoomRepository rooms;

    public UploadController(
            com.atg.api.repo.UserRepository users,
            RoomRepository rooms,
            FirebaseCourseAssetStorage courseAssetStorage) {
        super(users);
        this.rooms = rooms;
        this.courseAssetStorage = courseAssetStorage;
    }

    @PostMapping("/uploads")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file received");
        if (file.getSize() > MAX_GENERAL_UPLOAD_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Uploads must be 52 MB or smaller");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean image = type.startsWith("image/");
        boolean video = type.equals("video/mp4") || type.equals("video/webm")
                || type.equals("video/quicktime") || type.equals("video/x-m4v");
        if (!image && !video) throw bad("Upload an image or a supported video file");
        if (video && file.getSize() > 50L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Videos must be 50 MB or smaller");
        }
        String name = file.getOriginalFilename() == null ? "proof" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT) : ".jpg";
        if (ext.length() > 6) ext = ".jpg";
        String stored = UUID.randomUUID() + ext;
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(stored).toFile());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the file");
        }
        return Map.of("url", baseUrl + "/uploads/" + stored);
    }

    @PostMapping("/rooms/{roomId}/attachments")
    public Map<String, Object> uploadChatAttachment(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile file) {
        var user = me();
        Room room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        MembershipAccess.requireAccess(
                user, MembershipAccess.requiredForRoom(room), room.getName());
        String extension = chatAttachmentExtension(file);
        String url = courseAssetStorage.storeChatAttachment(
                file, extension, chatAttachmentContentType(extension));
        return Map.of("url", url);
    }

    static String chatAttachmentExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file received");
        if (file.getSize() > MAX_CHAT_ATTACHMENT_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Chat files must be 80 MB or smaller");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!CHAT_ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw bad("Upload an image, video, PDF, Office document, or audio file");
        }
        return extension;
    }

    private String chatAttachmentContentType(String extension) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".mp4", ".m4v" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".m4a" -> "audio/mp4";
            case ".aac" -> "audio/aac";
            case ".ogg", ".opus" -> "audio/ogg";
            case ".flac" -> "audio/flac";
            default -> throw bad("Unsupported chat file type");
        };
    }

    @PostMapping("/admin/course-assets")
    public Map<String, Object> uploadCourseAsset(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file received");
        if (file.getSize() > MAX_COURSE_ASSET_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Course files must be 250 MB or smaller");
        }
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!COURSE_ASSET_EXTENSIONS.contains(extension)) {
            throw bad("Upload an image, video, PDF, Office document, or audio file");
        }
        String contentType = courseAssetContentType(extension);
        return Map.of("url", courseAssetStorage.store(file, extension, contentType));
    }

    private String courseAssetContentType(String extension) {
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".mp4", ".m4v" -> "video/mp4";
            case ".webm" -> "video/webm";
            case ".mov" -> "video/quicktime";
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".m4a" -> "audio/mp4";
            case ".aac" -> "audio/aac";
            case ".ogg" -> "audio/ogg";
            case ".flac" -> "audio/flac";
            default -> throw bad("Unsupported course file type");
        };
    }
}
