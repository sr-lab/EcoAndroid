package leaks.results;

import leaks.analysis.IFDSRLAnalysis;
import leaks.analysis.RLAnalysis;

public interface IAnalysisVisitor {
    void visit(RLAnalysis analysis, IResults storage);
    void visit(IFDSRLAnalysis ifdsrlAnalysis, IResults storage);
}
