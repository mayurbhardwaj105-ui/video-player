package com.streamvault.repository;

import com.streamvault.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
