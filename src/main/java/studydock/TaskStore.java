package studydock;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Versioned UTF-8 storage. A lifetime lock prevents concurrent lost updates. */
public final class TaskStore implements AutoCloseable {
    private final Path file;
    private final FileChannel channel;
    private final FileLock lock;
    public TaskStore(Path file) throws IOException {
        this.file = file.toAbsolutePath();
        Files.createDirectories(this.file.getParent());
        channel = FileChannel.open(this.file.resolveSibling(this.file.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try {
            acquired = channel.tryLock();
            if (acquired == null) throw new IOException("This task file is already open in another StudyDock window.");
        } catch (IOException | OverlappingFileLockException e) {
            channel.close();
            throw new IOException("Cannot lock the task file. Close the other StudyDock window.", e);
        }
        lock = acquired;
    }
    public List<Task> load() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();
        if (Files.size(file) > 2_000_000) throw new IOException("Task file exceeds the 2 MB safety limit.");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.getFirst().equals("STUDYDOCK\t1")) throw new IOException("Unknown task file format. The file has not been changed.");
        List<Task> tasks = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) {
            try {
                String[] fields = lines.get(i).split("\t", -1);
                if (fields.length != 6) throw new IllegalArgumentException("Expected six fields");
                Task task = new Task(UUID.fromString(fields[0]), decode(fields[1]), decode(fields[2]), LocalDate.parse(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]));
                if (!ids.add(task.id())) throw new IllegalArgumentException("Duplicate ID");
                tasks.add(task);
            } catch (RuntimeException e) { throw new IOException("Invalid task at line " + (i+1) + ". The file has not been changed.", e); }
        }
        return tasks;
    }
    public void save(List<Task> tasks) throws IOException {
        if (tasks.size() > 2000) throw new IOException("Limit: 2000 tasks.");
        if (new HashSet<>(tasks.stream().map(Task::id).toList()).size() != tasks.size()) throw new IOException("Duplicate task IDs.");
        StringBuilder data = new StringBuilder("STUDYDOCK\t1\n");
        for (Task t : tasks) data.append(t.id()).append('\t').append(encode(t.title())).append('\t').append(encode(t.subject())).append('\t')
                .append(t.due()).append('\t').append(t.minutes()).append('\t').append(t.priority()).append('\n');
        byte[] bytes = data.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 2_000_000) throw new IOException("Task data exceeds 2 MB.");
        Path temp = Files.createTempFile(file.getParent(), "studydock-", ".tmp");
        try {
            Files.write(temp, bytes);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    private static String encode(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
    @Override public void close() throws IOException { try { lock.release(); } finally { channel.close(); } }
}
