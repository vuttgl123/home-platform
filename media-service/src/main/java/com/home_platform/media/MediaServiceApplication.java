package com.home_platform.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Ảnh của địa điểm, người, món đồ; có lịch dọn file tải lên dở ({@code PendingCleanup}). */
@SpringBootApplication
@EnableScheduling
public class MediaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaServiceApplication.class, args);
	}

}
