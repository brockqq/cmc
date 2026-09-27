package com.kata.backend.pettycash;

import com.kata.backend.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores receipt files on local disk under {@code app.upload-dir}. Only images and PDFs are accepted; the type is
 * verified from the file's leading bytes rather than trusting the client-provided content type.
 */
@Component
public class ReceiptStorage {

    /** Allowed content types → stored file extension. */
    private static final Map<String, String> TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "application/pdf", ".pdf");

    private final Path root;

    public ReceiptStorage(@Value("${app.upload-dir}") String uploadDir) throws IOException {
        this.root = Path.of(uploadDir, "receipts").toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public record Stored(String key, String contentType, long size) {
    }

    public Stored store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "請選擇檔案");
        }
        String type;
        try (InputStream in = file.getInputStream()) {
            type = sniff(in.readNBytes(12));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "無法讀取檔案");
        }
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "單據僅接受圖片（JPG、PNG、GIF、WebP）或 PDF");
        }
        String key = UUID.randomUUID() + TYPES.get(type);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(key), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "檔案儲存失敗");
        }
        return new Stored(key, type, file.getSize());
    }

    public Resource load(String key) {
        Path path = resolve(key);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "找不到單據檔案");
        }
        return new PathResource(path);
    }

    public void delete(String key) {
        if (key == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ignored) {
            // A leftover file is harmless; the database no longer references it
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "無效的檔案");
        }
        return path;
    }

    /** Detects the real type from magic bytes; null when not an allowed type. */
    static String sniff(byte[] b) {
        if (startsWith(b, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (startsWith(b, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (startsWith(b, 'G', 'I', 'F', '8')) return "image/gif";
        if (startsWith(b, 'R', 'I', 'F', 'F') && b.length >= 12 && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        if (startsWith(b, '%', 'P', 'D', 'F', '-')) return "application/pdf";
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
