package com.yunshu.mes.common.compat;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/common")
public class CommonCompatController {

    private static final String FILES_PREFIX = "/api/common/files/";

    private final LocalFileStorageService storageService;

    public CommonCompatController(LocalFileStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/uploadMinio")
    public Map<String, Object> uploadMinio(@RequestParam("file") MultipartFile file) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            LocalFileStorageService.StoredFile stored = storageService.store(file);
            body.put("code", 200);
            body.put("msg", "上传成功");
            body.put("url", stored.url());
            body.put("fileName", stored.fileName());
            return body;
        } catch (IllegalArgumentException ex) {
            body.put("code", 500);
            body.put("msg", ex.getMessage());
            return body;
        } catch (Exception ex) {
            body.put("code", 500);
            body.put("msg", "上传失败: " + ex.getMessage());
            return body;
        }
    }

    @GetMapping("/files/**")
    public ResponseEntity<FileSystemResource> serveFile(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith(FILES_PREFIX)) {
            return ResponseEntity.notFound().build();
        }
        String relative = uri.substring(FILES_PREFIX.length());
        try {
            Path filePath = storageService.resolvePublicPath(relative);
            if (!Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            FileSystemResource resource = new FileSystemResource(filePath);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (IOException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
