package com.home_platform.identity.workspace;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.home_platform.common.context.UserContext;
import com.home_platform.common.id.IdGenerator;
import com.home_platform.common.web.ConflictException;
import com.home_platform.common.web.CursorPage;
import com.home_platform.common.web.CursorRequest;
import com.home_platform.common.web.NotFoundException;
import com.home_platform.identity.user.User;
import com.home_platform.identity.user.UserService;
import com.home_platform.identity.user.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Không gian của người dùng. Người tạo là chủ và có dòng thành viên vai trò OWNER. Không gian
 * mà người gọi không phải thành viên được coi là không tồn tại.
 *
 * <p>Sau khi tạo, client gọi {@code POST /setup} của registry để có dữ liệu mặc định; hai
 * service chưa gọi nhau.
 */
@Service
public class WorkspaceService {

	private final WorkspaceRepository repository;

	private final UserService users;

	private final IdGenerator idGenerator;

	private final Clock clock;

	WorkspaceService(WorkspaceRepository repository, UserService users, IdGenerator idGenerator, Clock clock) {
		this.repository = repository;
		this.users = users;
		this.idGenerator = idGenerator;
		this.clock = clock;
	}

	/**
	 * @throws NotFoundException nếu người gọi không tồn tại
	 * @throws ConflictException nếu người gọi đã bị vô hiệu
	 */
	@Transactional
	public MemberWorkspace create(UserContext ctx, WorkspaceRequest request) {
		User owner = this.users.get(ctx.userId());
		if (owner.status() == UserStatus.DISABLED) {
			throw new ConflictException("User " + owner.id() + " is disabled");
		}
		Instant now = now();
		Workspace workspace = new Workspace(this.idGenerator.nextId(), owner.id(), request.name(),
				WorkspaceStatus.ACTIVE, now, now);
		this.repository.insert(workspace);
		this.repository.insertMember(workspace.id(), owner.id(), MemberRole.OWNER, now);
		return new MemberWorkspace(workspace, MemberRole.OWNER);
	}

	/** @throws NotFoundException nếu không có, hoặc người gọi không phải thành viên */
	public MemberWorkspace get(UserContext ctx, long id) {
		return this.repository.findForMember(ctx.userId(), id).orElseThrow(() -> NotFoundException.of("Workspace", id));
	}

	/** Các không gian người gọi là thành viên, theo tên. */
	public CursorPage<MemberWorkspace> mine(UserContext ctx, CursorRequest page) {
		return this.repository.listForMember(ctx.userId(), page);
	}

	@Transactional
	public MemberWorkspace rename(UserContext ctx, long id, WorkspaceRequest request) {
		MemberWorkspace current = get(ctx, id);
		Workspace renamed = current.workspace().withName(request.name()).withUpdatedAt(now());
		this.repository.update(renamed);
		return new MemberWorkspace(renamed, current.role());
	}

	private Instant now() {
		return this.clock.instant().truncatedTo(ChronoUnit.MILLIS);
	}

}
