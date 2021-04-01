package resourceleaks;

import com.google.common.base.Stopwatch;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import org.jetbrains.annotations.NotNull;
import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.options.Options;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.TrapUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

public class ResourceLeakAnalysisTask extends Task.Backgroundable {
    private static final Logger logger = Logger.getInstance(ResourceLeakAnalysisTask.class);
    private Stopwatch _stopWatch;
    private AnActionEvent e;
    private Project p;

    /*
    hardcoded for testing purposes
    TODO use intellij platform sdk to retrieve info / ask user
     */
    private static final String sourceDirectory = "/home/ricardo/Documents/meic/tese/";
    private static final String androidJar = "/home/ricardo/Android/Sdk/platforms";
    private final static String apkPath = "/home/ricardo/Downloads/AnkiDroid-2.14.3-universal.apk";
    private final static String apkPath2 = "/home/ricardo/Documents/meic/tese/MealSaver/app/build/outputs/apk/debug/app-debug.apk";
    private final static String apkPath3 = "/home/ricardo/Documents/meic/tese/AndroidResourceLeaks-master/AnkiDroid/AnkiDroid 3e9ddc7eca/Apk/AnkiDroid 3e9ddc7eca.apk";

    public ResourceLeakAnalysisTask(Project p, AnActionEvent e){
        super(p, "Resource Leak Analysis");
        this.p = p;
        this.e = e;
        _stopWatch = Stopwatch.createUnstarted();
    }
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        _stopWatch.start();
        indicator.setText("Setting up Soot");
        indicator.setFraction(0.1);

        System.out.println("SDK: " + ProjectRootManager.getInstance(p).getProjectSdkName());
        System.out.println("SDK path: " + ProjectRootManager.getInstance(p).getProjectSdk().getHomePath());
        System.out.println("Root path: " + p.getBasePath());


        SetupApplication application = new SetupApplication(androidJar, apkPath3);
        Set<SootClass> entrypoints = application.getEntrypointClasses();

        G.reset();
        //generic options
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_whole_program(false);
        Options.v().set_prepend_classpath(true);
        Options.v().set_app(true);

        //read apk options
        Options.v().set_android_jars(androidJar); // The path to Android Platforms
        Options.v().set_src_prec(Options.src_prec_apk); // Determine the input is an APK
        Options.v().set_process_dir(Collections.singletonList(apkPath3)); // Provide paths to the APK
        Options.v().set_process_multiple_dex(true);  // Inform Dexpler that the APK may have more than one .dex files
        Options.v().set_include_all(true);

        //output options
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_validate(true);

        //cg gen options
        Options.v().setPhaseOption("cg", "safe-newinstance:true");
        Options.v().setPhaseOption("cg.cha","enabled:false");

        // Enable SPARK call-graph construction
        Options.v().setPhaseOption("cg.spark","enabled:true");
        Options.v().setPhaseOption("cg.spark","verbose:true");
        Options.v().setPhaseOption("cg.spark","on-fly-cg:true");
        Options.v().setPhaseOption("cg.spark", "string-constants:true");

        //we cannot build the cg with the cg pack
        //we must build it here so it also creates the entry points (i.e. the dummy main)
        //this also builds a "better" cg than the pack, bc it is the flowdroid one
        application.constructCallgraph();

        SootMethod flowdroidDummyMainMethod = application.getDummyMainMethod();
        //prevents bug
        flowdroidDummyMainMethod.setName("main");
        SootClass flowdroidDummyMainClass = flowdroidDummyMainMethod.getDeclaringClass();

        //set flowdroid dummymain method/class as soot instance's main method/class
        Scene.v().setEntryPoints(Collections.singletonList(flowdroidDummyMainMethod));
        Scene.v().setMainClass(flowdroidDummyMainClass);

        //debug: check if prev ops went ok
        SootClass dummyMainClass = Scene.v().getMainClass();
        SootMethod dummyMainMethod = Scene.v().getMainMethod();

        /*
        SootMethod entryPoint = application.getDummyMainMethod();
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
        Options.v().set_main_class(entryPoint.getDeclaringClass().getName());
        //Options.v().set_main_class(entryPoint.getSignature());
        //Scene.v().setEntryPoints(Collections.singletonList(entryPoint));
        */
        /*
        PackManager.v().getPack("jtp")
                .add(new Transform("jtp.myTransform", new BodyTransformer() {
                    protected void internalTransform(Body body, String phase, Map options) {
                        new LocalMustNotAliasAnalysis(new ExceptionalUnitGraph(body));
                        //System.err.println("intraproc");
                    }}));

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.herosifds", new IFDSDataFlowTransformer()));
         */
        //SootClass exampleClass = Scene.v().getSootClass("com.ichi2.anki.MyAccount");
        //PsiElement el = e.getData(LangDataKeys.PSI_ELEMENT);
        //MarkupModel mm = FileEditorManager.getInstance(p)
        /*
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                PsiClass psiClass = JavaPsiFacade.getInstance(p).findClass(exampleClass.getName(), GlobalSearchScope.allScope(p));
                //AnalysisService service = p.getService(AnalysisService.class);
                //service.setPsiElement(psiClass);
            }
        });
         */

        Scene.v().loadNecessaryClasses();

        indicator.setText("Running Packs");
        indicator.setFraction(0.2);
        PackManager.v().runPacks();

        indicator.setText("Running intra-procedural analysis");
        indicator.setFraction(0.5);
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                int counter = 0;
                for (SootClass c : Scene.v().getApplicationClasses()) {
                    PsiClass psiClass = JavaPsiFacade.getInstance(p).findClass(c.getName(), GlobalSearchScope.allScope(p));
                    for (SootMethod m : c.getMethods()) {
                        if (m.hasActiveBody()) {
                            //if (m.getName().equals("modelIdfromDB")) {
                                UnitGraph graph = new ExceptionalUnitGraph(m.getActiveBody());
                                RLAnalysis analysis = new RLAnalysis(graph);

                                if (!analysis.getResults().isEmpty()) {
                                    System.out.println("leak on " + m.getName());
                                    counter++;

                                    //todo refactor this part into results processor
                                    ResultsProcessor processor = ServiceManager.getService(p, ResultsProcessor.class);
                                    PsiMethod[] psiMethods = psiClass.findMethodsByName(m.getName(), true);

                                    boolean processed = processor.processMethodResults(m, psiMethods, analysis.getResults());
                                    System.out.println("was processed? " + processed);
                                }
                            //}
                        }

                    }
                }
                System.out.println("COUNTER " + counter);
            }
        });

        _stopWatch.stop();

        indicator.setText("Finished analysis");
        System.out.println("End of setup");
        logger.info("End of setup");

        Notification notification = new Notification(
                "Tasks", "EcoAndroid", "Analysis ended", NotificationType.INFORMATION);
        notification.setImportant(false);
        Notifications.Bus.notify(notification);
    }
}