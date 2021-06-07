package leaks;

public interface IResultsProcessor {
    void visit(RLAnalysis analysis);
    void visit(VascoRLAnalysis analysis);
    void visit(AllInOneRLAnalysis allInOneRLAnalysis);
    void visit(IFDSRLAnalysis ifdsrlAnalysis);
}
