package leaks;

import java.util.Set;

public interface IAnalysis {
    public void accept(IResultsProcessor resultsProcessor);
}
