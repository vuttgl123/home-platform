package com.home_platform.media.storage;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTests extends FileStorageContract {

	@TempDir
	Path root;

	private FileStorage storage;

	@BeforeEach
	void createStorage() {
		this.storage = new LocalFileStorage(this.root);
	}

	@Override
	FileStorage storage() {
		return this.storage;
	}

}
