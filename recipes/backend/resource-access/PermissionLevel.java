package recipes.resourceaccess; // snapshot: adapt package + imports

import java.util.Map;

public enum PermissionLevel {
    VIEW, ACTION, GRANT;

    // Explicit ranks, independent of declaration order — reordering/inserting
    // enum constants above can't silently flip permission-check results.
    private static final Map<PermissionLevel, Integer> RANK = Map.of(
            VIEW, 0,
            ACTION, 1,
            GRANT, 2
    );

    public boolean atLeast(PermissionLevel minimum) {
        return RANK.get(this) >= RANK.get(minimum);
    }
}
