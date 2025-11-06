package invoice.utiils;

import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import invoice.exception.UploadMediaFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
public class ServiceUtils {

    public static String getMediaUrl(MultipartFile mediaFile, Uploader uploader) {
        log.info("Trying to upload image to Cloudinary");
        try {
            Map<?, ?> map = ObjectUtils.asMap(
                    "resource_type", "image",
                    "use_filename", true
            );
            Map<?, ?> uploadResponse = uploader.upload(mediaFile.getBytes(), map);
            log.info("Image uploaded successfully");
            return uploadResponse.get("url").toString();
        } catch (Exception exception) {
            log.error("Error while uploading image to Cloudinary", exception);
            throw new UploadMediaFailedException("a: "+exception.getMessage());
        }
    }
}
