package leaks.results;

public interface IResults {
    enum AnalysisType {
        INTRA,
        INTER
    }

    void add(Leak leak, AnalysisType type);
    void clearAll();
}
