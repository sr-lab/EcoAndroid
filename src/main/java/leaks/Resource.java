package leaks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum Resource {

    /**
     * Representation of the Cursor resource. A Cursor represents the result of a query
     * to a database.
     * NOTE:
     */
    CURSOR ( "android.database.Cursor",
            "rawQuery", "android.database.sqlite.SQLiteDatabase",
            "close", "android.database.Cursor",
            true, false),

    WAKELOCK ("android.os.PowerManager$WakeLock",
            "acquire", "android.os.PowerManager$WakeLock",
            "release", "android.os.PowerManager$WakeLock",
            false, true);

    private final String type;
    private final String acquireOp;
    private final String acquireClass;
    private final String releaseOp;
    private final String releaseClass;
    private final boolean intraProcedural;
    private final boolean interProcedural;

    Resource(String type, String acquireOp, String acquireClass, String releaseOp, String releaseClass,
             boolean intraProcedural, boolean interProcedural) {
        this.type = type;
        this.acquireOp = acquireOp;
        this.acquireClass = acquireClass;
        this.releaseOp = releaseOp;
        this.releaseClass = releaseClass;
        this.intraProcedural = intraProcedural;
        this.interProcedural = interProcedural;
    }

    public String getType() {
        return type;
    }

    public String acquireOp() {
        return acquireOp;
    }

    public String acquireClass() {
        return acquireClass;
    }

    public String releaseOp() {
        return releaseOp;
    }

    public String releaseClass() {
        return releaseClass;
    }

    public boolean isIntraProcedural() {
        return intraProcedural;
    }

    public boolean isInterProcedural() {
        return interProcedural;
    }

    public boolean isBeingAcquired(String acquireOp, String acquireClass) {
        return this.acquireOp.equals(acquireOp) && this.acquireClass.equals(acquireClass);
    }

    public boolean isBeingReleased(String releaseOp, String releaseClass) {
        return this.releaseOp.equals(releaseOp) && this.releaseClass.equals(releaseClass);
    }

    public boolean isBeingDeclared(String acquireClass) {
        return this.acquireClass.equals(acquireClass);
    }

    public Set<Resource> getIntraProceduralResources() {
        Set<Resource> intraProceduralResources = new HashSet<>();
        for (Resource r : Resource.values()) {
            if (r.isIntraProcedural()) {
                intraProceduralResources.add(r);
            }
        }

        return Collections.unmodifiableSet(intraProceduralResources);
    }

    public Set<Resource> getInterProceduralResources() {
        Set<Resource> interProceduralResources = new HashSet<>();
        for (Resource r : Resource.values()) {
            if (r.isInterProcedural()) {
                interProceduralResources.add(r);
            }
        }

        return Collections.unmodifiableSet(interProceduralResources);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
