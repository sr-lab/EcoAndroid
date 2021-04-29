package leaks;

import com.google.common.base.Stopwatch;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import leaks.platform.AndroidPlatformLocator;
import soot.*;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.Pair;
import soot.util.Chain;
import vasco.DataFlowSolution;

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
    private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot 76c4f80e47/Apk/ConnectBot 76c4f80e47.apk";
    //private final static String connectBot = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/ConnectBot/ConnectBot f5d392e3a3/Apk/ConnectBot f5d392e3a3.apk";

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

        SootSetup.configSootInstance(androidSdkPath.toString(), apkPath3);

        /*
        File file = new File("/home/ricardo/ecoandroid.out");
        //Instantiating the PrintStream class
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("exception");
        }
         */



        /*
        VascoRLAnalysis analysis = new VascoRLAnalysis();
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.vascorl",
                new SceneTransformer() {
                    @Override
                    protected void internalTransform(String s, Map<String, String> map) {
                        analysis.doAnalysis();
                    }
                }));

        Set<SootMethod> analysisMethods = analysis.getMethods();
        DataFlowSolution<Unit,Map<FieldInfo, Pair<Local, Boolean>>> solution = analysis.getMeetOverValidPathsSolution();
        for (SootMethod method :  analysis.getMethods()) {
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

        Unit lastUnitMain = Scene.v().getMainMethod().getActiveBody().getUnits().getLast();
        Map<FieldInfo, Pair<Local, Boolean>> interProcLeaks = solution.getValueAfter(lastUnitMain);
        ResultsProcessor processor = ServiceManager.getService(project, ResultsProcessor.class);
        for (Map.Entry<FieldInfo, Pair<Local, Boolean>> entry : interProcLeaks.entrySet()) {

        }

         */


        //PsiElement el = e.getData(LangDataKeys.PSI_ELEMENT);
        //MarkupModel mm = FileEditorManager.getInstance(p)

        indicator.setText("Running Packs");
        indicator.setFraction(0.2);
        PackManager.v().getPack("wjtp").apply();

        indicator.setText("Running intra-procedural analysis");
        indicator.setFraction(0.5);
/*
        int counter = 0;
        for (SootClass c : Scene.v().getApplicationClasses()) {
            for (SootMethod m : c.getMethods()) {
                if (m.hasActiveBody()) {
                    //if (m.getName().equals("modelIdfromDB")) {
                    UnitGraph graph = new ExceptionalUnitGraph(m.getActiveBody());
                    RLAnalysis analysis = new RLAnalysis(graph);
                    Set<Local> results = analysis.getResults();
                    if (!results.isEmpty()) {
                        System.out.println("leak on " + m.getName());
                        counter++;

                        ResultsProcessor processor = ServiceManager.getService(p, ResultsProcessor.class);
                        boolean processed = processor.processMethodResults(m, results);
                        System.out.println("was processed? " + processed);
                    }
                    //}
                }

            }
        }
        System.out.println("COUNTER " + counter);
 */
        ResultsProcessor processor = ServiceManager.getService(project, ResultsProcessor.class);
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

        /*
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                for (SootClass c : Scene.v().getApplicationClasses()) {
                    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(c.getName(), GlobalSearchScope.allScope(project));
                    for (SootMethod m : c.getMethods()) {
                        if (m.hasActiveBody()) {
                            UnitGraph graph = new ExceptionalUnitGraph(m.getActiveBody());
                            RLAnalysis analysis = new RLAnalysis(graph);

                            if (!analysis.getResults().isEmpty()) {
                                System.out.println("leak on " + m.getName());
                                counter++;

                                //todo refactor this part into results processor
                                ResultsProcessor processor = ServiceManager.getService(project, ResultsProcessor.class);
                                PsiMethod[] psiMethods = psiClass.findMethodsByName(m.getName(), true);

                                boolean processed = processor.processMethodResults(m, psiMethods);
                                System.out.println("was processed? " + processed);
                            }
                        }

                    }
                }
                System.out.println("COUNTER " + counter);
            }
        });

         */
        _stopWatch.stop();

        indicator.setText("Finished analysis");

        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Analysis ended", NotificationType.INFORMATION);
        notification.setImportant(false);
        Notifications.Bus.notify(notification);
    }
}