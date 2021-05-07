package leaks;

import com.google.common.base.Stopwatch;
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
import org.jetbrains.annotations.NotNull;
import leaks.platform.AndroidPlatformLocator;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;
import vasco.DataFlowSolution;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(ResourceLeakAnalysisTask.class);
    private Stopwatch _stopWatch;
    private AnActionEvent event;
    private Project project;

    //TODO use intellij platform sdk to retrieve info / ask user
    private static final String androidJar = "/home/ricardo/Android/Sdk/platforms";
    private final static String apkPath3 = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3e9ddc7eca/Apk/AnkiDroid 3e9ddc7eca.apk";
    //buggy
    //private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot 76c4f80e47/Apk/ConnectBot 76c4f80e47.apk";
    //fixed
    private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot f5d392e3a3/Apk/ConnectBot f5d392e3a3.apk";

    public ResourceLeakAnalysisTask(Project project, AnActionEvent event){
        super(project, "Resource Leak Analysis");
        this.project = project;
        this.event = event;
        _stopWatch = Stopwatch.createUnstarted();
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        _stopWatch.start();
        indicator.setIndeterminate(false);
        indicator.setText("Setting up Soot");
        indicator.setFraction(0.1);

        Path androidSdkPath = AndroidPlatformLocator.getAndroidPlatformsLocation(project).toAbsolutePath();

        logger.info("Setting up Soot");
        System.out.println("ANDROID SDK: " + androidSdkPath.toString());
        System.out.println("SDK: " + ProjectRootManager.getInstance(project).getProjectSdkName());
        System.out.println("SDK path: " + ProjectRootManager.getInstance(project).getProjectSdk().getHomePath());
        System.out.println("Root path: " + project.getBasePath());

        SootSetup.configSootInstance(androidSdkPath.toString(), connectBot);

        File file = new File("/home/ricardo/ecoandroid.out");
        //Instantiating the PrintStream class
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("exception");
        }

        SootMethod main = Scene.v().getMainMethod();
        CallGraph cg = Scene.v().getCallGraph();
        for (SootClass c : Scene.v().getApplicationClasses()) {
            if (c.getName().equals("org.connectbot.ConsoleActivity")) {
                for (SootMethod m : c.getMethods()) {
                    if (m.getName().equals("onPause") || m.getName().equals("onStop")) {
                        Iterator<Edge> edges = cg.edgesOutOf(m);
                        while (edges.hasNext()) {
                            Edge e = edges.next();
                            System.out.println("");
                        }
                    }
                }
            }
        }

        VascoRLAnalysis vascoRLAnalysis = new VascoRLAnalysis();
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.vascorl",
                new SceneTransformer() {
                    @Override
                    protected void internalTransform(String s, Map<String, String> map) {
                        vascoRLAnalysis.doAnalysis();
                    }
                }));

        /*
        Set<SootMethod> analysisMethods = vascoRLAnalysis.getMethods();
        DataFlowSolution<Unit,Map<FieldInfo, Pair<Local, Boolean>>> solution = vascoRLAnalysis.getMeetOverValidPathsSolution();
        for (SootMethod method : analysisMethods) {
            System.out.println(method);
            for (Unit unit : method.getActiveBody().getUnits()) {
                if (method.getDeclaringClass().getName().equals("org.connectbot.ConsoleActivity") &&
                        (method.getName().equals("onStop") || method.getName().equals("onStart") || method.getName().equals("onPause"))) {
                    System.out.println("----------------------------------------------------------------");
                    System.out.println(unit);
                    System.out.println("IN:  " + solution.getValueBefore(unit));
                    System.out.println("OUT: " + solution.getValueAfter(unit));
                }
            }
            System.out.println("----------------------------------------------------------------");
        }

         */
        ResultsProcessor processor = ServiceManager.getService(project, ResultsProcessor.class);



        indicator.setText("Running Packs");
        indicator.setFraction(0.2);
        PackManager.v().getPack("wjtp").apply();

        Set<SootMethod> analysisMethods = vascoRLAnalysis.getMethods();
        DataFlowSolution<Unit,Map<FieldInfo, Pair<Local, Boolean>>> solution = vascoRLAnalysis.getMeetOverValidPathsSolution();
        for (SootMethod method : analysisMethods) {
            // Check if a resource is leaked in problematic callbacks
            // These callbacks are not taken into account by Soot - the activity can be put on hold
            // after onPause/onStop
            System.out.println(method);
            if (method.getName().matches("onStop|onPause")) {
                Unit returnUnit = method.getActiveBody().getUnits().getLast();
                HashMap<FieldInfo, Pair<Local, Boolean>> facts =
                        (HashMap<FieldInfo, Pair<Local, Boolean>>) solution.getValueAfter(returnUnit);
                for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : facts.entrySet()) {
                    if (entry.getValue().getO2() == true) {
                        processor.processMethodResults(method);
                    }
                }
            }
        }

        indicator.setText("Running intra-procedural analysis");
        indicator.setFraction(0.5);

        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    UnitGraph graph = new ExceptionalUnitGraph(m.getActiveBody());
                    RLAnalysis analysis = new RLAnalysis(graph);
                    if (!analysis.getResults().isEmpty()) {
                        processor.processMethodResults(m);
                    }
                }
            }
        }

        _stopWatch.stop();

        indicator.setText("Finished analysis");

        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Analysis ended", NotificationType.INFORMATION);
        notification.setImportant(false);
        Notifications.Bus.notify(notification);
    }
}