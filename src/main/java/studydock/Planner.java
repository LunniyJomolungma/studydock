package studydock;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** Earliest-deadline-first allocation. This is an explainable heuristic, not an optimizer. */
public final class Planner {
    private Planner() {}
    public record Block(LocalDate date, UUID taskId, String title, String subject, int minutes) {}
    public record Shortfall(Task task, int minutes, String reason) {}
    public record Plan(List<Block> blocks, List<Shortfall> shortfalls, Map<LocalDate,Integer> used, int capacity) {
        public int allocated() { return blocks.stream().mapToInt(Block::minutes).sum(); }
        public int unallocated() { return shortfalls.stream().mapToInt(Shortfall::minutes).sum(); }
    }
    public static Plan build(List<Task> tasks, LocalDate today, int horizon, int dailyBudget, boolean weekends) {
        Objects.requireNonNull(today);
        if (horizon < 1 || horizon > 60 || dailyBudget < 15 || dailyBudget > 720)
            throw new IllegalArgumentException("Use 1–60 days and 15–720 minutes per day.");
        if (new HashSet<>(tasks.stream().map(Task::id).toList()).size() != tasks.size())
            throw new IllegalArgumentException("Task IDs must be unique.");
        Map<LocalDate,Integer> used = new LinkedHashMap<>();
        for (int i = 0; i < horizon; i++) {
            LocalDate day = today.plusDays(i);
            if (weekends || (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY)) used.put(day, 0);
        }
        List<Task> ordered = tasks.stream().filter(t -> !t.done())
                .sorted(Comparator.comparing(Task::due).thenComparing(Comparator.comparingInt(Task::priority).reversed())
                        .thenComparing(Task::title).thenComparing(t -> t.id().toString())).toList();
        List<Block> blocks = new ArrayList<>();
        List<Shortfall> shortfalls = new ArrayList<>();
        for (Task task : ordered) {
            int remaining = task.minutes();
            for (var entry : used.entrySet()) {
                if (entry.getKey().isAfter(task.due()) || remaining == 0) break;
                int available = dailyBudget - entry.getValue();
                while (available > 0 && remaining > 0) {
                    int amount = Math.min(60, Math.min(available, remaining));
                    blocks.add(new Block(entry.getKey(), task.id(), task.title(), task.subject(), amount));
                    entry.setValue(entry.getValue() + amount);
                    available -= amount;
                    remaining -= amount;
                }
            }
            if (remaining > 0) shortfalls.add(new Shortfall(task, remaining,
                    task.due().isBefore(today) ? "Deadline already passed" : task.due().isAfter(today.plusDays(horizon - 1))
                            ? "Outside current planning capacity; deadline is beyond this window" : "Not enough available time before the deadline"));
        }
        blocks.sort(Comparator.comparing(Block::date));
        return new Plan(List.copyOf(blocks), List.copyOf(shortfalls), Collections.unmodifiableMap(new LinkedHashMap<>(used)), used.size() * dailyBudget);
    }
}
