package leaks;

import com.google.common.base.Stopwatch;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import leaks.results.AnalysisVisitor;
import leaks.results.ResultsIntellij;
import leaks.ui.MessageBox;
import org.jetbrains.annotations.NotNull;
import leaks.platform.AndroidPlatformLocator;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(ResourceLeakAnalysisTask.class);
    private Stopwatch _stopWatch;
    private AnActionEvent event;
    private Project project;

    //TODO use intellij platform sdk to retrieve info / ask user
    private static final String androidJar = "/home/ricardo/Android/Sdk/platforms";
    //buggy
    private final static String anySoft = "/home/ricardo/Documents/meic/tese/DroidLeaks/AnySoftKeyboard-rev-b832671708.apk";
    private final static String ankidroid = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3e9ddc7eca/Apk/AnkiDroid 3e9ddc7eca.apk";
    //private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot 76c4f80e47/Apk/ConnectBot 76c4f80e47.apk";
    //fixed
    //private final static String ankidroid = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3a579e4091/Apk/AnkiDroid 3a579e4091.apk";
    private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot f5d392e3a3/Apk/ConnectBot f5d392e3a3.apk";

    public ResourceLeakAnalysisTask(Project project, AnActionEvent event){
        super(project, "Resource Leak Analysis");
        this.project = project;
        this.event = event;
        _stopWatch = Stopwatch.createUnstarted();
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {

        ResultsIntellij results = ServiceManager.getService(project, ResultsIntellij.class);
        results.clearAll();

        long startTime = System.nanoTime();
        indicator.setIndeterminate(false);
        indicator.setText("Setting up Soot");
        indicator.setFraction(0.1);

        Path androidSdkPath = AndroidPlatformLocator.getAndroidPlatformsLocation(project).toAbsolutePath();

        logger.info("Setting up Soot");
        System.out.println("Android SDK: " + androidSdkPath.toString());
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

        registerTransformers();

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

    private void runInterProceduralAnalysis() {
        PackManager.v().getPack("wjtp").apply();
    }

    private void runIntraProceduralAnalysis() {
        Pack jtp = PackManager.v().getPack("jtp");
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (sootMethod.hasActiveBody()) {
                    jtp.apply(sootMethod.retrieveActiveBody());
                }
            }
        }
    }

    private void registerTransformers() {
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                IFDSRLAnalysis analysis = new IFDSRLAnalysis();
                analysis.doAnalysis(true);
                analysis.accept(AnalysisVisitor.getInstance(), ServiceManager.getService(project, ResultsIntellij.class));
            }
        }));

        PackManager.v().getPack("jtp").add(new Transform("jtp.rl", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
            SootMethod method = body.getMethod();
            UnitGraph graph = new ExceptionalUnitGraph(method.getActiveBody());
            RLAnalysis analysis = new RLAnalysis(graph);

            // Process analysis' results
            if (!analysis.getResults().isEmpty()) {
                analysis.accept(AnalysisVisitor.getInstance(), ServiceManager.getService(project, ResultsIntellij.class));
            }
            }
        }));
    }

    private void processJimpleForAliasing() {
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

    private boolean methodHasResources(SootMethod m) {
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