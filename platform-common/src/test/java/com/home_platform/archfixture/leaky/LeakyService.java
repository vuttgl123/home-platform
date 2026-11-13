package com.home_platform.archfixture.leaky;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Mẫu vi phạm: một service thường tự chạy SQL. */
@Service
public class LeakyService {

	JdbcClient jdbc;

}
