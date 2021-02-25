package resourceleaks;

import com.google.common.base.Stopwatch;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;
import soot.options.Options;

import java.util.Collections;
import java.util.Iterator;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(ResourceLeakAnalysisTask.class);
    private Stopwatch _stopWatch;
    /*
    TODO use intellij platform sdk to retrieve info / ask user
     */
    private static final String sourceDirectory = "/home/ricardo/Documents/meic/tese/";
    private static final String androidJar = "/home/ricardo/Android/Sdk/platforms";
    private final static String apkPath = "/home/ricardo/Documents/meic/tese/MealSaver/app/build/outputs/apk/debug/app-debug.apk";
    static SetupApplication application;

    public ResourceLeakAnalysisTask(Project p){
        super(p, "Performing Resource Leak Detection");

        _stopWatch = Stopwatch.createUnstarted();
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        _stopWatch.start();

        G.reset();
        application = new SetupApplication(androidJar, apkPath);
        Options.v().set_process_multiple_dex(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        // SootClass sc = Scene.v().loadClassAndSupport(clsName);
        // sc.setApplicationClass();

        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(apkPath));
        Options.v().set_android_jars(androidJar);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().setPhaseOption("cg.spark", "on");

        Scene.v().loadNecessaryClasses();
        logger.info("End of setup");

        application.constructCallgraph();
        CallGraph cg = Scene.v().getCallGraph();
        int numberOfEdges = cg.size();
        int counter = 0;
        for(SootClass sc : Scene.v().getApplicationClasses()){
            int counter2 = 0;
            if(counter++ >= 10) break;
            for(SootMethod m : sc.getMethods()){
                if(counter2++ >= 10) break;
                Iterator<MethodOrMethodContext> targets = new Targets(cg.edgesOutOf(m));
                while (targets.hasNext()) {
                    SootMethod tgt = (SootMethod) targets.next();
                    Notification notification = new Notification(
                            "Tasks", "EcoAndroid", m + " may call " + tgt, NotificationType.INFORMATION);
                    notification.setImportant(true);
                    Notifications.Bus.notify(notification);
                    logger.info(m + " may call " + tgt);
                    System.out.println(m + " may call " + tgt);
                }
            }
        }
        _stopWatch.stop();
        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Number of edges: " + numberOfEdges + "| took ", NotificationType.INFORMATION);
        notification.setImportant(true);
        Notifications.Bus.notify(notification);
    }
}
