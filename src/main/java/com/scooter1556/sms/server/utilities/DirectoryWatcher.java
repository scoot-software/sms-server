package com.scooter1556.sms.server.utilities;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatcher implements Runnable {

    public enum Event {
        ENTRY_CREATE,
        ENTRY_MODIFY,
        ENTRY_DELETE
    }

    private static final Map<WatchEvent.Kind<Path>, Event> EVENT_MAP =
            new HashMap<WatchEvent.Kind<Path>, Event>() {{
                put(ENTRY_CREATE, Event.ENTRY_CREATE);
                put(ENTRY_MODIFY, Event.ENTRY_MODIFY);
                put(ENTRY_DELETE, Event.ENTRY_DELETE);
            }};

    private final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private Future<?> watcherTask;

    private final Set<Path> watched;
    private final boolean preExistingAsCreated;
    private final Listener listener;

    public DirectoryWatcher(Builder builder) {
        watched = builder.watched;
        preExistingAsCreated = builder.preExistingAsCreated;
        listener = builder.listener;
    }

    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    public void start() throws Exception {
        watcherTask = EXECUTOR.submit(this);
    }

    public void stop() {
        if(watcherTask == null) {
            return;
        }
        
        watcherTask.cancel(true);
        watcherTask = null;
    }

    @Override
    public void run() {
        WatchService watchService;
        
        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException ioe) {
            throw new RuntimeException("Exception while creating watch service.", ioe);
        }
        
        Map<WatchKey, Path> watchKeyToDirectory = new HashMap<>();

        for (Path dir : watched) {
            try {
                if (preExistingAsCreated) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        for (Path path : stream) {
                            listener.onEvent(Event.ENTRY_CREATE, dir.resolve(path));
                        }
                    }
                }

                WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                watchKeyToDirectory.put(key, dir);
            } catch (IOException ioe) {
            }
        }

        while (true) {
            if (Thread.interrupted()) {
                break;
            }

            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                continue;
            }

            Path dir = watchKeyToDirectory.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind().equals(OVERFLOW)) {
                    break;
                }

                WatchEvent<Path> pathEvent = cast(event);
                WatchEvent.Kind<Path> kind = pathEvent.kind();

                Path path = dir.resolve(pathEvent.context());
                if (EVENT_MAP.containsKey(kind)) {
                    listener.onEvent(EVENT_MAP.get(kind), path);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyToDirectory.remove(key);
                if (watchKeyToDirectory.isEmpty()) {
                    break;
                }
            }
        }
    }

    public interface Listener {
        void onEvent(Event event, Path path);
    }

    public static class Builder {
        private Set<Path> watched = new HashSet<>();
        private boolean preExistingAsCreated = false;
        private Listener listener;

        public Builder addDirectories(String dirPath) {
            return addDirectories(Paths.get(dirPath));
        }

        public Builder addDirectories(Path dirPath) {
            watched.add(dirPath);
            return this;
        }

        public Builder addDirectories(Path... dirPaths) {
            for (Path dirPath : dirPaths) {
                watched.add(dirPath);
            }
            return this;
        }

        public Builder addDirectories(Iterable<? extends Path> dirPaths) {
            for (Path dirPath : dirPaths) {
                watched.add(dirPath);
            }
            return this;
        }

        public Builder setPreExistingAsCreated(boolean value) {
            preExistingAsCreated = value;
            return this;
        }

        public DirectoryWatcher build(Listener listener) {
            this.listener = listener;
            return new DirectoryWatcher(this);
        }
    }
}