package com.streamvault.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    // Cloudinary's identifier for the uploaded asset. This is what we use
    // to regenerate a fresh secure URL on demand -- we never store or
    // expose the raw playback URL directly to clients.
    @Column(nullable = false, unique = true)
    private String cloudinaryPublicId;

    private Integer durationSeconds;

    private LocalDateTime uploadedAt = LocalDateTime.now();
}
