package studydock;
import java.time.LocalDate;
import java.util.*;
public final class Demo {
    private Demo() {}
    public static List<Task> tasks(LocalDate today) {
        return new ArrayList<>(List.of(
            task("Finish graph traversal lab", "Algorithms", today.plusDays(1), 150, 3),
            task("Prepare database diagrams", "Databases", today.plusDays(3), 180, 2),
            task("Practice integration problems", "Calculus", today.plusDays(4), 240, 3),
            task("Draft hackathon pitch", "Team project", today.plusDays(6), 120, 2),
            task("Review lecture notes", "Networks", today.plusDays(2), 60, 1),
            task("Submit last week's worksheet", "Calculus", today.minusDays(1), 45, 2),
            task("Set up development tools", "Team project", today, 0, 1)));
    }
    private static Task task(String title, String subject, LocalDate due, int minutes, int priority) {
        return new Task(UUID.nameUUIDFromBytes(title.getBytes(java.nio.charset.StandardCharsets.UTF_8)), title, subject, due, minutes, priority);
    }
}
