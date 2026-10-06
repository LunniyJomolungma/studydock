package studydock;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record Task(UUID id, String title, String subject, LocalDate due, int minutes, int priority) {
    public Task {
        Objects.requireNonNull(id, "Task ID is required");
        Objects.requireNonNull(due, "Deadline is required");
        title = Objects.requireNonNull(title, "Title is required").strip();
        subject = Objects.requireNonNull(subject, "Subject is required").strip();
        if (title.isEmpty() || title.length() > 120 || subject.isEmpty() || subject.length() > 50)
            throw new IllegalArgumentException("Use a title of 1–120 characters and a subject of 1–50 characters.");
        if (title.codePoints().anyMatch(Character::isISOControl) || subject.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Title and subject cannot contain control characters.");
        if (minutes < 0 || minutes > 6000) throw new IllegalArgumentException("Remaining work must be 0–6000 minutes.");
        if (priority < 1 || priority > 3) throw new IllegalArgumentException("Priority must be 1–3.");
        if (due.getYear() < 2000 || due.getYear() > 2100) throw new IllegalArgumentException("Deadline must be between 2000 and 2100.");
    }
    public boolean done() { return minutes == 0; }
    public Task completed() { return new Task(id, title, subject, due, 0, priority); }
    public String priorityName() { return switch (priority) { case 3 -> "High"; case 2 -> "Normal"; default -> "Low"; }; }
}
