package com.atg.api.web;

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

@RestController
@RequestMapping("/api")
public class UploadController extends BaseController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UploadController(com.atg.api.repo.UserRepository users) {
        super(users);
    }

    @PostMapping("/uploads")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("No file received");
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
}
