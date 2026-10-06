package studydock;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
public final class PlanExport {
    private PlanExport() {}
    private static String csv(String s) {
        // Spreadsheet programs may interpret leading operators as formulas, even inside quotes.
        String trimmed = s.stripLeading();
        if (!trimmed.isEmpty() && "=+-@".indexOf(trimmed.charAt(0)) >= 0) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    public static void write(Planner.Plan plan, Path file) throws IOException {
        StringBuilder out = new StringBuilder("date,title,subject,minutes,status\n");
        for (var b : plan.blocks()) out.append(b.date()).append(',').append(csv(b.title())).append(',').append(csv(b.subject())).append(',').append(b.minutes()).append(",scheduled\n");
        for (var s : plan.shortfalls()) out.append(s.task().due()).append(',').append(csv(s.task().title())).append(',').append(csv(s.task().subject())).append(',').append(s.minutes()).append(',').append(csv(s.reason())).append('\n');
        Files.writeString(file, out, StandardCharsets.UTF_8);
    }
}
