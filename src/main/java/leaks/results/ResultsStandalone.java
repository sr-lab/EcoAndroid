package leaks.results;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ResultsStandalone implements IResults {
    private final Set<Leak> results = new HashSet<>();
    private final Set<Leak> interProcResults = new HashSet<>();
    private final Set<Leak> intraProcResults = new HashSet<>();

    private static ResultsStandalone INSTANCE;

    private ResultsStandalone() { }

    public static ResultsStandalone getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ResultsStandalone();
        }
        return INSTANCE;
    }

    @Override
    public void add(Leak leak, AnalysisType type) {
        results.add(leak);
        switch (type) {
            case INTER:
                interProcResults.add(leak);
                break;
            case INTRA:
                intraProcResults.add(leak);
                break;
        }
    }

    @Override
    public void clearAll() {
        results.clear();
        interProcResults.clear();
        intraProcResults.clear();
    }

    public void toCSV(String fileName, String outputFolder, long setupDuration, long analysisDuration, long totalDuration) throws IOException {
        writeToFile(results, fileName, outputFolder, "all");
        writeToFile(interProcResults, fileName, outputFolder, "inter");
        writeToFile(intraProcResults, fileName, outputFolder, "intra");
        writeToBatchFile(interProcResults, fileName, outputFolder, setupDuration, analysisDuration, totalDuration);
    }

    private void writeToFile(Set<Leak> results, String fileName, String outputFolder, String type) throws IOException {
        File file = new File(outputFolder + fileName + "_" + type + ".csv");
        FileWriter writer = new FileWriter(file);
        writer.write("leakedInMethod,leakedInClass,declaredInMethod,declaredInClass,resource,classMember,lineno\n");
        for (Leak l : results) {
            String classMember = l.isClassMember() ? "T" : "F";
            String res = l.getLeakedMethodName() + "," +
                    l.getLeakedClassName() + "," +
                    l.getDeclaredMethodName() + "," +
                    l.getDeclaredClassName() + "," +
                    l.getResource() + "," +
                    classMember + "," +
                    l.getLineNumber() + "\n";
            writer.write(res);
        }
        writer.flush();
        writer.close();
    }

    private void writeToBatchFile(Set<Leak> results, String appName, String outputFolder, long setupDuration, long analysisDuration, long totalDuration) throws IOException {
        File file = new File(outputFolder + "batch_all" + ".csv");
        FileWriter writer = new FileWriter(file, true);
        if (file.length() == 0) {
            writer.write("app,leakedInMethod,leakedInClass,declaredInMethod,declaredInClass,resource,classMember," +
                    "lineno,setupTime,analysisTime,totalTime,classification,description\n");
        }
        for (Leak l : results) {
            String classMember = l.isClassMember() ? "T" : "F";
            String res = appName + "," +
                    l.getLeakedMethodName() + "," +
                    l.getLeakedClassName() + "," +
                    l.getDeclaredMethodName() + "," +
                    l.getDeclaredClassName() + "," +
                    l.getResource() + "," +
                    classMember + "," +
                    l.getLineNumber() + "," +
                    setupDuration + "," +
                    analysisDuration + "," +
                    totalDuration + "\n";
            writer.write(res);
        }
        writer.flush();
        writer.close();
    }
}
