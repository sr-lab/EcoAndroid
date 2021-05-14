package leaks;

public interface IResultsProcessor {
    public void visit(RLAnalysis analysis);
    public void visit(VascoRLAnalysis analysis);
}
