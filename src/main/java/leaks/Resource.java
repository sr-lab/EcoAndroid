package leaks;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Representation of a Android resource.
 */
public enum Resource {

    /**
     * Representation of the Cursor resource.
     * A Cursor represents the result of a query to a database.
     */
    CURSOR ( "android.database.Cursor",
            new String[]{"rawQuery","query"}, "android.database.sqlite.SQLiteDatabase",
            "close", "android.database.Cursor",
            true, false), // TODO might be inter-proc too...

    /**
     * Representation of the (inter-procedural) Wakelock resource.
     * A Wakelock is used to prevent the device from going to sleep, usually to perform critical operations.
     */
    WAKELOCK ("android.os.PowerManager$WakeLock",
            new String[]{"acquire"}, "android.os.PowerManager$WakeLock",
            "release", "android.os.PowerManager$WakeLock",
            false, true);

    private final String type;
    private final String[] acquireOp;
    private final String acquireClass;
    private final String releaseOp;
    private final String releaseClass;
    private final boolean intraProcedural;
    private final boolean interProcedural;

    /**
     *
     * @param type Class of the resource
     * @param acquireOp Array of operations used to release the resource
     * @param acquireClass Class of the object where the acquire operation is invoked
     * @param releaseOp Operation used to release the resource
     * @param releaseClass Class of the object where the release operation is invoked
     * @param intraProcedural
     * @param interProcedural
     */
    Resource(String type, String[] acquireOp, String acquireClass, String releaseOp, String releaseClass,
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

    public boolean isIntraProcedural() {
        return intraProcedural;
    }

    public boolean isInterProcedural() {
        return interProcedural;
    }

    public boolean isBeingAcquired(String acquireOp, String acquireClass) {
        boolean acquireOpMatch = false;
        for (String op : this.acquireOp) {
            if (acquireOp.equals(op)) {
                acquireOpMatch = true;
            }
        }
        return acquireOpMatch && this.acquireClass.equals(acquireClass);
    }

    public boolean isBeingReleased(String releaseOp, String releaseClass) {
        return this.releaseOp.equals(releaseOp) && this.releaseClass.equals(releaseClass);
    }

    public boolean isBeingDeclared(String acquireClass) {
        return this.acquireClass.equals(acquireClass);
    }

    @Override
    public String toString() {
        return this.name();
    }
}
