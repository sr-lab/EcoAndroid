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
import leaks.ui.MessageBox;
import org.jetbrains.annotations.NotNull;
import leaks.platform.AndroidPlatformLocator;
import soot.*;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;

import java.io.File;
import java.io.FileWriter;
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
        _stopWatch.start();
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
        //Instantiating the PrintStream class
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("exception");
        }

        //AnySoft Cursor release in method call
        /*
        SootClass sc = Scene.v().getSootClass("com.menny.android.anysoftkeyboard.dictionary.ContactsDictionary");
        SootMethod sm1 = sc.getMethodByName("loadDictionaryAsync");
        Body b1 = sm1.retrieveActiveBody();
        SootMethod sm2 = sc.getMethodByName("addWords");
        Body b2 = sm2.retrieveActiveBody();
        String localname = b1.getLocals().getFirst().getName();

         */


        SootClass sc = Scene.v().getSootClass("com.ichi2.anki.Deck");
        SootMethod sm = sc.getMethodByName("getBool");
        SootMethod sm2 = sc.getMethodByName("rebuildNewCount");


        /*
        SootClass sc = Scene.v().getSootClass("org.connectbot.ConsoleActivity");
        SootMethod m = sc.getMethodByName("onPause");
        SootMethod m2 = sc.getMethodByName("onResume");
        Body b = m.retrieveActiveBody();
        Chain<Local> locals = b.getLocals();
        UnitPatchingChain units = b.getUnits();
        List<ValueBox> defuse = b.getUseAndDefBoxes();
        List<ValueBox> def = b.getDefBoxes();
        Chain<SootClass> scs = Scene.v().getApplicationClasses();

         */


        ResultsProvider results = ServiceManager.getService(project, ResultsProvider.class);
        results.clearResults();

        registerTransformers();

        indicator.setText("Running intra-procedural analysis");
        indicator.setFraction(0.4);
        //runIntraProceduralAnalysis();

        indicator.setText("Running inter-procedural analysis");
        indicator.setFraction(0.6);
        runInterProceduralAnalysis();

        _stopWatch.stop();

        DaemonCodeAnalyzer.getInstance(project).restart();

        File resultsFileRefactor = new File("/home/ricardo/resultsRefactor/" + project.getName() + ".out");
        try {
            FileWriter writer = new FileWriter(resultsFileRefactor);
            writer.write(results.toCSV());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            System.out.println("exception");
        }

        MessageBox.Show("Resource leak detection complete. Go to Analyze | Code Inspections and" +
                "perform an inspection to see the results");

        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Analysis ended", NotificationType.INFORMATION);
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
        /*
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.rl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                VascoRLAnalysis analysis = new VascoRLAnalysis();
                analysis.doAnalysis();
                analysis.accept(ServiceManager.getService(project, ResultsProcessor.class));
            }
        }));

         */

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                JimpleBasedInterproceduralCFG icfg = new JimpleBasedInterproceduralCFG();

                IFDSTabulationProblem<Unit, Pair<ResourceInfo, Local>,
                        SootMethod,
                        InterproceduralCFG<Unit, SootMethod>> problem = new IFDSResourceLeak(icfg);

                IFDSSolver<Unit, Pair<ResourceInfo, Local>,SootMethod, InterproceduralCFG<Unit, SootMethod>> solver;
                solver = new IFDSSolver<Unit, Pair<ResourceInfo, Local>, SootMethod,InterproceduralCFG<Unit, SootMethod>>(problem);

                for (SootClass c : Scene.v().getApplicationClasses()) {
                    for (SootMethod m : c.getMethods()) {
                        if (m.hasActiveBody()) {
                            System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
                            System.out.println(m.getName() + " | " + c.getName());
                            for (Unit stmt : m.getActiveBody().getUnits()) {
                                System.out.print(stmt+"\n   |-- ");
                                Set<Pair<ResourceInfo, Local>> res = solver.ifdsResultsAt(stmt);
                                System.out.println(res);
                            }
                        }
                    }
                }
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
                analysis.accept(ServiceManager.getService(project, ResultsProcessor.class));
            }
            }
        }));
    }
}