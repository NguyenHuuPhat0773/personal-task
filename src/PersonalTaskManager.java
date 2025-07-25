import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PersonalTaskManager{

    private static final String DB_FILE_PATH = "tasks_database.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Phương thức trợ giúp để tải dữ liệu (sẽ được gọi lặp lại)
    private static JSONArray loadTasksFromDb() {
        JSONParser parser = new JSONParser();
        try (FileReader reader = new FileReader(DB_FILE_PATH)) {
            Object obj = parser.parse(reader);
            if (obj instanceof JSONArray) {
                return (JSONArray) obj;
            }
        } catch (IOException | ParseException e) {
            System.err.println("Lỗi khi đọc file database: " + e.getMessage());
        }
        return new JSONArray();
    }
    

    // Phương thức trợ giúp để lưu dữ liệu
    private static void saveTasksToDb(JSONArray tasksData) {
        try (FileWriter file = new FileWriter(DB_FILE_PATH)) {
            file.write(tasksData.toJSONString());
            file.flush();
        } catch (IOException e) {
            System.err.println("Lỗi khi ghi vào file database: " + e.getMessage());
        }
    }

    /**
     * Chức năng thêm nhiệm vụ mới
     *
     * @param title Tiêu đề nhiệm vụ.
     * @param description Mô tả nhiệm vụ.
     * @param dueDateStr Ngày đến hạn (định dạng YYYY-MM-DD).
     * @param priorityLevel Mức độ ưu tiên ("Thấp", "Trung bình", "Cao").
     * @param isRecurring Boolean có phải là nhiệm vụ lặp lại không.
     * @return JSONObject của nhiệm vụ đã thêm, hoặc null nếu có lỗi.
     */
    public JSONObject addNewTaskWithViolations(String title, String description, String dueDateStr, PriorityLevel priorityLevel, boolean isRecurring) {

        if (!isTitleValid(title)) {
            System.out.println("Lỗi: Tiêu đề không được để trống.");
            return null;
        }
        LocalDate dueDate;
        try {
            dueDate = parseDueDate(dueDateStr);
        } catch (IllegalArgumentException e) {
            System.out.println("Lỗi: " + e.getMessage());
            return null;
        }
        // Tải dữ liệu
        JSONArray tasks = loadTasksFromDb();

        // Kiểm tra trùng lặp
        if (isDuplicateTask(tasks, title, dueDate.format(DATE_FORMATTER))) {
            System.out.println(String.format("Lỗi: Nhiệm vụ '%s' đã tồn tại với cùng ngày đến hạn.", title));
            return null;
        }

        String taskId = UUID.randomUUID().toString(); // YAGNI: Có thể dùng số nguyên tăng dần đơn giản hơn.

        JSONObject newTask = new JSONObject();
        newTask.put("id", taskId);
        newTask.put("title", title);
        newTask.put("description", description);
        newTask.put("due_date", dueDate.format(DATE_FORMATTER));
        newTask.put("priority", priorityLevel.getDisplay());
        newTask.put("status", "Chưa hoàn thành");
        newTask.put("created_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("last_updated_at", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newTask.put("is_recurring", isRecurring); // YAGNI: Thêm thuộc tính này dù chưa có chức năng xử lý nhiệm vụ lặp lại
        if (isRecurring) {
            newTask.put("recurrence_pattern", "Chưa xác định");
        }

        tasks.add(newTask);

        // Lưu dữ liệu
        saveTasksToDb(tasks);

        System.out.println(String.format("Đã thêm nhiệm vụ mới thành công với ID: %s", taskId));
        return newTask;
    }
    

    public static void main(String[] args) {
        PersonalTaskManagerViolations manager = new PersonalTaskManagerViolations();
        System.out.println("\nThêm nhiệm vụ hợp lệ:");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            "Cao",
            false
        );

        System.out.println("\nThêm nhiệm vụ trùng lặp (minh họa DRY - lặp lại code đọc/ghi DB và kiểm tra trùng):");
        manager.addNewTaskWithViolations(
            "Mua sách",
            "Sách Công nghệ phần mềm.",
            "2025-07-20",
            PriorityLevel.fromString("Cao"),
            false
        );

        System.out.println("\nThêm nhiệm vụ lặp lại (minh họa YAGNI - thêm tính năng không cần thiết ngay):");
        manager.addNewTaskWithViolations(
            "Tập thể dục",
            "Tập gym 1 tiếng.",
            "2025-07-21",
            "Trung bình",
            true 
        );

        System.out.println("\nThêm nhiệm vụ với tiêu đề rỗng:");
        manager.addNewTaskWithViolations(
            "",
            "Nhiệm vụ không có tiêu đề.",
            "2025-07-22",
            "Thấp",
            false
        );
    }
}
// Kiểm tra tiêu đề có hợp lệ không (không null, không trống)
private boolean isTitleValid(String title) {
    return title != null && !title.trim().isEmpty();
}

// Parse ngày đến hạn từ chuỗi sang LocalDate, ném lỗi nếu không hợp lệ
private LocalDate parseDueDate(String dueDateStr) {
    if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
        throw new IllegalArgumentException("Ngày đến hạn không được để trống.");
    }
    try {
        return LocalDate.parse(dueDateStr, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Ngày đến hạn không hợp lệ. Vui lòng sử dụng định dạng YYYY-MM-DD.");
    }
}
public enum PriorityLevel {
    THAP("Thấp"),
    TRUNG_BINH("Trung bình"),
    CAO("Cao");

    private final String display;

    PriorityLevel(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public static PriorityLevel fromDisplay(String display) {
        for (PriorityLevel level : values()) {
            if (level.display.equalsIgnoreCase(display)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Mức độ ưu tiên không hợp lệ: " + display);
    }
}
private boolean isDuplicateTask(JSONArray tasks, String title, String dueDateStr) {
    for (Object obj : tasks) {
        JSONObject task = (JSONObject) obj;
        String existingTitle = task.get("title").toString();
        String existingDueDate = task.get("due_date").toString();

        if (existingTitle.equalsIgnoreCase(title) && existingDueDate.equals(dueDateStr)) {
            return true;
        }
    }
    return false;
}

