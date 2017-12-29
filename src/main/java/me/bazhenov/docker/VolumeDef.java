package me.bazhenov.docker;

import java.io.File;
import java.util.Objects;

public final class VolumeDef {

	private final String mountPoint;
	private final File location;
	private final boolean createDirectoryIfMissing;

	public VolumeDef(String mountPoint, File location) {
		this(mountPoint, location, false);
	}

	public VolumeDef(String mountPoint, File location, boolean createDirectoryIfMissing) {
		if (mountPoint == null || mountPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.mountPoint = mountPoint;
		this.location = location;
		this.createDirectoryIfMissing = createDirectoryIfMissing;
	}

	/**
	 * @return the path to the volume mounted inside a container
	 */
	public String getMountPoint() {
		return mountPoint;
	}

	/**
	 * @return location of a volume at host machine (can be {@code null})
	 */
	public File getLocation() {
		return location;
	}

	/**
	 * @return should empty directory be created if location ({@link #getLocation()}) is {@code null}
	 */
	public boolean isCreateDirectoryIfMissing() {
		return createDirectoryIfMissing;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		VolumeDef volumeDef = (VolumeDef) o;
		return createDirectoryIfMissing == volumeDef.createDirectoryIfMissing &&
			Objects.equals(mountPoint, volumeDef.mountPoint) &&
			Objects.equals(location, volumeDef.location);
	}

	@Override
	public int hashCode() {
		return Objects.hash(mountPoint, location, createDirectoryIfMissing);
	}
}
