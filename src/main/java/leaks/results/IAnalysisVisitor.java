package leaks.results;

import leaks.IFDSRLAnalysis;
import leaks.RLAnalysis;

public interface IAnalysisVisitor {
    void visit(RLAnalysis analysis, IResults storage);
    void visit(IFDSRLAnalysis ifdsrlAnalysis, IResults storage);
}
