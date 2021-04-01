package resourceleaks;

public enum Resource {
    CURSOR ("rawQuery", "android.database.sqlite.SQLiteDatabase",
            "close", "android.database.Cursor");

    private final String acquireOp;
    private final String acquireClass;
    private final String releaseOp;
    private final String releaseClass;

    Resource(String acquireOp, String acquireClass, String releaseOp, String releaseClass) {
        this.acquireOp = acquireOp;
        this.acquireClass = acquireClass;
        this.releaseOp = releaseOp;
        this.releaseClass = releaseClass;
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

    public boolean isBeingAcquired(String acquireOp, String acquireClass) {
        return this.acquireOp.equals(acquireOp) && this.acquireClass.equals(acquireClass);
    }

    public boolean isBeingReleased(String releaseOp, String releaseClass) {
        return this.releaseOp.equals(releaseOp) && this.releaseClass.equals(releaseClass);
    }
}
