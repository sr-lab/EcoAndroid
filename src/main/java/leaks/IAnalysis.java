package leaks;

import leaks.results.IAnalysisVisitor;
import leaks.results.IResults;

public interface IAnalysis {
    void accept(IAnalysisVisitor visitor, IResults storage);
}
