package com.streamvault.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        config.put("secure", "true");

        this.cloudinary = new Cloudinary(config);
    } 

    /**
     * Uploads a video file to Cloudinary and returns the response map,
     * which includes "public_id", "secure_url", "duration", etc.
     * Videos are uploaded to a dedicated "streamvault_videos" folder.
     */
    public Map<String, Object> uploadVideo(MultipartFile file) throws IOException {
        Map<String, Object> uploadParams = ObjectUtils.asMap(
                "resource_type", "video",
                "folder", "streamvault_videos",
                "type", "authenticated" // requires a signed URL to access, not publicly guessable
        );

        return cloudinary.uploader().upload(file.getBytes(), uploadParams);
    }

    /**
     * Generates a fresh, time-limited, signed URL for an already-uploaded
     * video. The URL expires after `validitySeconds`, so links can't be
     * shared indefinitely or hot-linked from elsewhere.
     */
    public String generateSecurePlaybackUrl(String publicId, int validitySeconds) {
        long expiresAt = (System.currentTimeMillis() / 1000L) + validitySeconds;

        Map<String, Object> options = ObjectUtils.asMap(
                "resource_type", "video",
                "type", "authenticated",
                "sign_url", true,
                "secure", true,
                "auth_token", ObjectUtils.asMap(
                        "duration", validitySeconds,
                        "start_time", System.currentTimeMillis() / 1000L
                )
        );

        return cloudinary.url()
                .resourceType("video")
                .type("authenticated")
                .signed(true)
                .secure(true)
                .generate(publicId);
    }

    public void deleteVideo(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", "video", "type", "authenticated"));
    }
}
