package com.home_platform.identity.user;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.home_platform.common.context.UserContext;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.NotFoundException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Người dùng: đủ để biết ai sở hữu không gian nào khi chưa có đăng nhập. Email không trùng. */
@Service
public class UserService {

	private final UserRepository repository;

	private final IdGenerator idGenerator;

	private final Clock clock;

	UserService(UserRepository repository, IdGenerator idGenerator, Clock clock) {
		this.repository = repository;
		this.idGenerator = idGenerator;
		this.clock = clock;
	}

	/** @throws NotFoundException nếu không có */
	public User get(long id) {
		return this.repository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
	}

	/** @throws ConflictException nếu email đã có người dùng */
	@Transactional
	public User create(UserCreateRequest request) {
		Instant now = now();
		User user = new User(this.idGenerator.nextId(), request.email(), request.displayName(), UserStatus.ACTIVE, now,
				now);
		try {
			this.repository.insert(user);
		}
		catch (DuplicateKeyException ex) {
			throw new ConflictException("User with email " + request.email() + " already exists");
		}
		return user;
	}

	@Transactional
	public User rename(UserContext ctx, UserRenameRequest request) {
		User renamed = get(ctx.userId()).withDisplayName(request.displayName()).withUpdatedAt(now());
		this.repository.update(renamed);
		return renamed;
	}

	private Instant now() {
		return this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
	}

}
