package com.home_platform.archfixture.leaky;

import org.springframework.web.bind.annotation.RestController;

/** Mẫu vi phạm: controller gọi thẳng repository. */
@RestController
public class LeakyController {

	LeakyRepository repository;

}
