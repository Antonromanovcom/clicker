package dev.awake.mode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class TargetLockedMode extends AbstractLifecycleMode {
    private final Path target;
    private final WakefulnessMode delegate;
    private FileChannel lockChannel;
    private FileLock lock;

    TargetLockedMode(Path target, WakefulnessMode delegate) {
        this.target = target;
        this.delegate = delegate;
    }

    @Override
    public String description() {
        return delegate.description() + "; exclusive target lock enabled";
    }

    @Override
    protected void onStart() throws ModeException {
        acquireLock();
        try {
            delegate.start();
        } catch (ModeException exception) {
            try {
                releaseLock();
            } catch (ModeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Override
    public void pulse() throws ModeException {
        delegate.pulse();
    }

    @Override
    protected void onStop() throws ModeException {
        ModeException failure = null;
        try {
            delegate.stop();
        } catch (ModeException exception) {
            failure = exception;
        }
        try {
            releaseLock();
        } catch (ModeException exception) {
            if (failure == null) failure = exception; else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    private void acquireLock() throws ModeException {
        try {
            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "awake-target-locks");
            Files.createDirectories(directory);
            Path lockFile = directory.resolve(hash(normalizedTarget()) + ".lock");
            lockChannel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
            try {
                lock = lockChannel.tryLock();
            } catch (OverlappingFileLockException exception) {
                lock = null;
            }
            if (lock == null) {
                closeChannel();
                throw new ModeException("Another Awake instance is already using this target.");
            }
            lockChannel.truncate(0);
            lockChannel.write(ByteBuffer.wrap("Awake target lock\n".getBytes(StandardCharsets.UTF_8)));
            lockChannel.force(true);
        } catch (IOException exception) {
            closeChannelQuietly();
            throw new ModeException("Could not create the exclusive target lock.", exception);
        }
    }

    private String normalizedTarget() {
        try {
            return target.toRealPath().toString();
        } catch (IOException ignored) {
            return target.toAbsolutePath().normalize().toString();
        }
    }

    private String hash(String value) throws ModeException {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new ModeException("SHA-256 is unavailable for target locking.", exception);
        }
    }

    private void releaseLock() throws ModeException {
        try {
            if (lock != null) lock.release();
            closeChannel();
        } catch (IOException exception) {
            throw new ModeException("Could not release the exclusive target lock.", exception);
        } finally {
            lock = null;
            lockChannel = null;
        }
    }

    private void closeChannel() throws IOException {
        if (lockChannel != null) lockChannel.close();
    }

    private void closeChannelQuietly() {
        try { closeChannel(); } catch (IOException ignored) { }
        lockChannel = null;
    }
}
