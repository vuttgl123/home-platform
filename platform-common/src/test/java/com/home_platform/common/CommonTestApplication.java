package com.home_platform.common;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Ứng dụng tối thiểu cho các test {@code *IT} của module này: không có lớp nghiệp vụ nào,
 * chỉ bật auto-configuration như một service thật sẽ nhận được khi phụ thuộc vào
 * platform-common. Không nằm trong test-jar.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class CommonTestApplication {

}
