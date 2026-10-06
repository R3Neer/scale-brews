package io.github.r3neer.scalebrews.collision.pose;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.resources.Identifier;

/** Neutral complete predicate table. Each row names an existing revision-owned numeric resource. */
public record FinitePoseProgram(int format, String source, String version,
                                List<String> booleans, List<Row> rows) {
    public static final int FORMAT = 1, MAX_BOOLEANS = 5, MAX_ROWS = 32, MAX_ID_LENGTH = 256;

    public record Row(int mask, String program) {
        public Row {
            if (mask < 0 || mask >= MAX_ROWS) throw new IllegalArgumentException("Invalid finite row mask");
            program = resourceId(program);
        }
    }

    public FinitePoseProgram {
        if (format != FORMAT || source == null || source.isBlank() || source.length() > 256
            || version == null || version.isBlank() || version.length() > 128
            || booleans == null || booleans.size() > MAX_BOOLEANS
            || rows == null || rows.isEmpty() || rows.size() > MAX_ROWS)
            throw new IllegalArgumentException("Missing/oversized finite program");
        String previous = null;
        for (String name : booleans) {
            if (name == null || !name.matches("[a-z0-9_.-]{1,64}")
                || (previous != null && previous.compareTo(name) >= 0))
                throw new IllegalArgumentException("Noncanonical finite boolean schema");
            previous = name;
        }
        int count = 1 << booleans.size();
        if (rows.size() != count) throw new IllegalArgumentException("Incomplete finite assignment table");
        var masks = new HashSet<Integer>();
        var canonical = new ArrayList<Row>(rows.size());
        for (var row : rows) {
            if (row == null || row.mask() < 0 || row.mask() >= count || !masks.add(row.mask()))
                throw new IllegalArgumentException("Invalid/duplicate finite assignment");
            // Check raw nested records too; Gson or other serializers may bypass their constructors.
            canonical.add(new Row(row.mask(), row.program()));
        }
        booleans = List.copyOf(booleans);
        rows = canonical.stream().sorted(Comparator.comparingInt(Row::mask)).toList();
    }

    /** Check caps before copying or expanding any caller-controlled nested collection. */
    public static FinitePoseProgram validatedCopy(FinitePoseProgram raw) {
        if (raw == null || raw.booleans() == null || raw.booleans().size() > MAX_BOOLEANS
            || raw.rows() == null || raw.rows().isEmpty() || raw.rows().size() > MAX_ROWS)
            throw new IllegalArgumentException("Missing/oversized finite program");
        return new FinitePoseProgram(raw.format(), raw.source(), raw.version(), raw.booleans(), raw.rows());
    }

    public static String resourceId(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_ID_LENGTH)
            throw new IllegalArgumentException("Missing/oversized finite resource id");
        return Identifier.parse(value).toString();
    }
}
