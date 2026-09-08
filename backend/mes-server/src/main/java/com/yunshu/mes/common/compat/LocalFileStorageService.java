package com.yunshu.mes.common.compat;

import com.yunshu.mes.config.UploadProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadProperties uploadProperties;

    public LocalFileStorageService(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    public StoredFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }
        String ext = resolveExtension(file.getOriginalFilename(), file.getContentType());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("仅支持 jpg/jpeg/png/gif/webp 图片");
        }

        String dateDir = LocalDate.now().format(DATE_DIR);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path baseDir = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
        Path targetDir = baseDir.resolve(dateDir.replace('/', java.io.File.separatorChar));
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(storedName);
        file.transferTo(targetFile.toFile());

        String publicPath = "/api/common/files/" + dateDir + "/" + storedName;
        return new StoredFile(publicPath, publicPath);
    }

    public Path resolvePublicPath(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new IllegalArgumentException("文件路径无效");
        }
        String normalized = relativePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("文件路径无效");
        }
        Path baseDir = Paths.get(uploadProperties.getPath()).toAbsolutePath().normalize();
        Path resolved = baseDir.resolve(normalized).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IllegalArgumentException("文件路径无效");
        }
        return resolved;
    }

    private static String resolveExtension(String originalFilename, String contentType) {
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!StringUtils.hasText(ext) && StringUtils.hasText(contentType)) {
            ext = switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg" -> "jpg";
                case "image/png" -> "png";
                case "image/gif" -> "gif";
                case "image/webp" -> "webp";
                default -> "";
            };
        }
        return ext;
    }

    public record StoredFile(String url, String fileName) {}
}
