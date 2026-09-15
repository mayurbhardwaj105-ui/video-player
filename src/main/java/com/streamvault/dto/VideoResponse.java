package com.streamvault.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime uploadedAt;
    // Note: publicId is intentionally NOT exposed here -- only the
    // controller's /play endpoint hands out a signed, expiring URL.
}
