package invoice.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto")
        );
        return uploadResult.get("secure_url").toString();
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return; // nothing to delete
        }

        try {
            String publicId = extractPublicIdFromUrl(fileUrl);
            if (publicId == null) return;

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());

            if (!"ok".equals(result.get("result"))) {
                System.err.println("Cloudinary failed to delete: " + result);
            }

        } catch (Exception e) {
            System.err.println("Failed to delete file from Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Extract Cloudinary public_id safely
     */
    private String extractPublicIdFromUrl(String url) {
        try {
            String withoutQuery = url.split("\\?")[0];

            // gets: /upload/.../file.ext
            String[] splitUpload = withoutQuery.split("/upload/");
            if (splitUpload.length < 2) return null;

            String path = splitUpload[1];

            // remove transformations if present
            // e.g. "w_200,h_200/v12345/folder/file.png"
            if (path.startsWith("v") && path.matches("v\\d+/.*")) {
                path = path.substring(path.indexOf("/") + 1);
            } else if (path.matches(".*v\\d+/.*")) {
                path = path.substring(path.indexOf("v", 1));
                path = path.substring(path.indexOf("/") + 1);
            }

            // remove extension
            int lastDot = path.lastIndexOf('.');
            return lastDot > 0 ? path.substring(0, lastDot) : path;

        } catch (Exception e) {
            System.err.println("Failed to extract public_id from URL: " + e.getMessage());
            return null;
        }
    }
}
