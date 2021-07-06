package leaks;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import leaks.platform.AndroidPlatformLocator;
import leaks.results.AnalysisVisitor;
import leaks.results.IResults;
import leaks.results.ResultsIntellij;
import leaks.results.ResultsStandalone;
import leaks.ui.MessageBox;
import soot.*;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class AnalysisWrapper {

    private static AnalysisWrapper INSTANCE;

    //TODO use intellij platform sdk to retrieve info / ask user
    //buggy
    private final static String anySoft = "/home/ricardo/Documents/meic/tese/DroidLeaks/AnySoftKeyboard-rev-b832671708.apk";
    private final static String ankidroid = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3e9ddc7eca/Apk/AnkiDroid 3e9ddc7eca.apk";
    //private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot 76c4f80e47/Apk/ConnectBot 76c4f80e47.apk";
    //fixed
    //private final static String ankidroid = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3a579e4091/Apk/AnkiDroid 3a579e4091.apk";
    private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot f5d392e3a3/Apk/ConnectBot f5d392e3a3.apk";

    private AnalysisWrapper() { }

    public static AnalysisWrapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AnalysisWrapper();
        }
        return INSTANCE;
    }

    public void RunIntellijAnalysis(Project project, ProgressIndicator indicator) {
        ResultsIntellij results = ServiceManager.getService(project, ResultsIntellij.class);
        results.clearAll();

        long startTime = System.nanoTime();
        indicator.setIndeterminate(false);
        indicator.setText("Setting up Soot");
        indicator.setFraction(0.1);

        Path androidSdkPath = AndroidPlatformLocator.getAndroidPlatformsLocation(project).toAbsolutePath();

        System.out.println("Android SDK: " + androidSdkPath);
        System.out.println("SDK: " + ProjectRootManager.getInstance(project).getProjectSdkName());
        System.out.println("SDK path: " + ProjectRootManager.getInstance(project).getProjectSdk().getHomePath());
        System.out.println("Root path: " + project.getBasePath());

        SootSetup.configSootInstance(androidSdkPath.toString(), ankidroid);

        File file = new File("/home/ricardo/ecoandroid.out");
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("exception");
        }

        processJimpleForAliasing();

        registerTransformers(results);

        indicator.setText("Running intra-procedural analysis");
        indicator.setFraction(0.4);
        //runIntraProceduralAnalysis();

        indicator.setText("Running inter-procedural analysis");
        indicator.setFraction(0.6);
        runInterProceduralAnalysis();

        long endTime = System.nanoTime();
        long totalDuration = TimeUnit.SECONDS.convert(endTime - startTime, TimeUnit.NANOSECONDS);

        DaemonCodeAnalyzer.getInstance(project).restart();

        MessageBox.Show("Resource leak detection complete. Go to Analyze | Code Inspections and" +
                "perform an inspection to see the results");

        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Analysis ended, took" + totalDuration + "s", NotificationType.INFORMATION);
        notification.setImportant(false);
        Notifications.Bus.notify(notification);
    }

    public void RunStandaloneAnalysis(String apkPath, String androidSdkPath, String outputFolder) {
        String apkName = getApkName(apkPath);
        File file = new File(outputFolder + apkName + ".out");
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("Unable to write to output!");
        }

        System.out.println("~ Resource Leak Analysis - Standalone ver. ~");
        System.out.println("Apk path: " + apkPath);
        System.out.println("SDK path: " + androidSdkPath);


        System.out.println("Setting up Soot...");
        long startSetup = System.nanoTime();
        setupSoot(apkPath, androidSdkPath);

        System.out.println("Registering transformers...");
        registerTransformers(ResultsStandalone.getInstance());

        System.out.println("Processing Jimple...");
        processJimpleForAliasing();

        long startIntraProc = System.nanoTime();
        System.out.println("Running intra-procedural analysis...");
        runIntraProceduralAnalysis();

        long startInterProc = System.nanoTime();
        System.out.println("Running inter-procedural analysis...");
        runInterProceduralAnalysis();
        long endInterProc = System.nanoTime();

        long setupDuration = TimeUnit.MILLISECONDS.convert(startInterProc - startSetup, TimeUnit.NANOSECONDS);
        long intraProcDuration = TimeUnit.MILLISECONDS.convert(startInterProc - startIntraProc, TimeUnit.NANOSECONDS);
        long interProcDuration = TimeUnit.MILLISECONDS.convert(endInterProc - startInterProc, TimeUnit.NANOSECONDS);
        long analysisDuration = intraProcDuration + interProcDuration;
        long totalDuration = TimeUnit.MILLISECONDS.convert(endInterProc - startSetup, TimeUnit.NANOSECONDS);

        System.out.println(setupDuration + "," + intraProcDuration + "," + interProcDuration + "," + totalDuration);

        writeResultsToFile(apkName, outputFolder);
    }

    private String getApkName(String apkPath) {
        String apkFileName = apkPath.substring(apkPath.lastIndexOf("/")+1);
        return apkFileName.substring(0, apkFileName.indexOf("."));
    }

    private void setupSoot(String apkPath, String androidSdkPath) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable() {
            public Integer call() {
                SootSetup.configSootInstance(androidSdkPath, apkPath);
                return 0;
            }
        });
        try {
            future.get(5, TimeUnit.MINUTES); // Timeout setup in 5 minutes
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            System.out.println("Failed! Soot setup timed out.");
            return;
        }
        executor.shutdownNow();
    }

    private static void writeResultsToFile(String apkName, String outputFolder) {
        try {
            ResultsStandalone.getInstance().toCSV(apkName, outputFolder);
        } catch (IOException e) {
            System.out.println("Failed writing results to file!");
        }
    }

    private static void runInterProceduralAnalysis() {
        PackManager.v().getPack("wjtp").apply();
    }

    private static void runIntraProceduralAnalysis() {
        Pack jtp = PackManager.v().getPack("jtp");
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (sootMethod.hasActiveBody()) {
                    jtp.apply(sootMethod.retrieveActiveBody());
                }
            }
        }
    }

    private static void registerTransformers(IResults results) {
        // whole-jimple packs
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                IFDSRLAnalysis analysis = new IFDSRLAnalysis();
                analysis.doAnalysis(true);
                analysis.accept(AnalysisVisitor.getInstance(), results);
            }
        }));

        // jimple-body packs
        PackManager.v().getPack("jtp").add(new Transform("jtp.rl", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                SootMethod method = body.getMethod();
                UnitGraph graph = new ExceptionalUnitGraph(method.getActiveBody());
                RLAnalysis analysis = new RLAnalysis(graph);

                if (!analysis.getResults().isEmpty()) {
                    analysis.accept(AnalysisVisitor.getInstance(), results);
                }
            }
        }));
    }

    private static void processJimpleForAliasing() {
        int idCounter = 0;
        for (SootClass c : Scene.v().getApplicationClasses()) {
            List<SootMethod> methods = c.getMethods();
            for (SootMethod m : methods) {
                if (m.hasActiveBody()) {
                    Body body = m.retrieveActiveBody();
                    Chain<Local> locals = body.getLocals();
                    UnitPatchingChain units = body.getUnits();

                    if (methodHasResources(m)) {
                        // Steps to instrument Jimple to mitigate aliasing:
                        // - search for assign stmt (rx = ry) where both rx and ry are resources of the same type
                        // - change their name to RESOURCE_ID so that our analysis knows they represent the same resource
                        // - change the locals' name in every occurrence
                        for (Unit unit : units) {
                            if (unit instanceof AssignStmt) {
                                AssignStmt stmt = (AssignStmt) unit;

                                if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof Local) {
                                    Local leftLocal = (Local) stmt.getLeftOp();
                                    Local rightLocal = (Local) stmt.getRightOp();
                                    String leftLocalType = leftLocal.getType().toString();
                                    String rightLocalType = rightLocal.getType().toString();

                                    for (Resource r : Resource.values()) {
                                        if (r.getType().equals(leftLocalType) && leftLocalType.equals(rightLocalType)) {
                                            for (Local l : locals) {
                                                if (l.equivTo(leftLocal) || l.equivTo(rightLocal)) {
                                                    l.setName("RESOURCE_" + idCounter);
                                                }
                                            }
                                            idCounter++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    m.getActiveBody().validate();
                }
            }
            c.validate();
        }
    }

    private static boolean methodHasResources(SootMethod m) {
        Body body = m.getActiveBody();
        Chain<Local> locals = body.getLocals();
        for (Local l : locals) {
            for (Resource r : Resource.values()) {
                if (r.getType().equals(l.getType().toString())) {
                    return true;
                }
            }
        }
        return false;
    }
}