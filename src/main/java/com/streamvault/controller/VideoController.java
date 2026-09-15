package com.streamvault.controller;

import com.streamvault.dto.VideoResponse;
import com.streamvault.model.Video;
import com.streamvault.repository.VideoRepository;
import com.streamvault.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private static final int PLAYBACK_URL_VALIDITY_SECONDS = 300; // 5 minutes

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    /**
     * Uploads a video file, stores it in Cloudinary, and saves only the
     * metadata + Cloudinary public_id in MySQL. The raw file never
     * touches our own disk/server storage.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", defaultValue = "") String description) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }

        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadVideo(file);

            Video video = new Video();
            video.setTitle(title);
            video.setDescription(description);
            video.setCloudinaryPublicId((String) uploadResult.get("public_id"));

            Object duration = uploadResult.get("duration");
            if (duration != null) {
                video.setDurationSeconds(((Number) duration).intValue());
            }

            videoRepository.save(video);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Video uploaded successfully",
                    "videoId", video.getId(),
                    "title", video.getTitle()
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload video: " + e.getMessage()));
        }
    }

    /** Lists video metadata only -- never the Cloudinary public_id or URL. */
    @GetMapping
    public ResponseEntity<List<VideoResponse>> listVideos() {
        List<VideoResponse> videos = videoRepository.findAll().stream()
                .map(v -> new VideoResponse(v.getId(), v.getTitle(), v.getDescription(), v.getUploadedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(videos);
    }

    /**
     * Generates a fresh, short-lived secure URL for playback on demand.
     * The frontend calls this right before rendering the <video> tag
     * rather than storing/caching the URL, since it expires quickly.
     */
    @GetMapping("/{id}/play")
    public ResponseEntity<?> getPlaybackUrl(@PathVariable Long id) {
        Video video = videoRepository.findById(id).orElse(null);

        if (video == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Video not found"));
        }

        String secureUrl = cloudinaryService.generateSecurePlaybackUrl(
                video.getCloudinaryPublicId(), PLAYBACK_URL_VALIDITY_SECONDS);

        return ResponseEntity.ok(Map.of(
                "videoUrl", secureUrl,
                "expiresInSeconds", PLAYBACK_URL_VALIDITY_SECONDS
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVideo(@PathVariable Long id) {
        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Video not found"));
        }

        try {
            cloudinaryService.deleteVideo(video.getCloudinaryPublicId());
            videoRepository.delete(video);
            return ResponseEntity.ok(Map.of("message", "Video deleted"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete video: " + e.getMessage()));
        }
    }
}
