package leaks;

import org.jetbrains.annotations.NotNull;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.config.SootConfigForAndroid;
import soot.options.Options;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Utility class used to setup Soot to work alongside FlowDroid
 */
public class SootSetup {

    /**
     * Configures Soot (and FlowDroid)
     * @param sdkPath path for the Android SDK ((...)/Android/Sdk/platforms/)
     * @param apkPath path for the APK to be analyzed
     */
    public static void configSootInstance(String sdkPath, String apkPath) {
        SootConfigForAndroid sootConfig = createSootConfigForAndroid(sdkPath, apkPath);
        InfoflowAndroidConfiguration infoFlowConfig = createInfoflowAndroidConfiguration(sdkPath, apkPath);

        sootConfig.setSootOptions(Options.v(), infoFlowConfig);
        SetupApplication application = new SetupApplication(infoFlowConfig);
        application.setSootConfig(sootConfig);

        Scene.v().loadNecessaryClasses();

        //we cannot build the cg with the cg pack
        //we must build it here so it also creates the entry points (i.e. the dummy main)
        //this also builds a "better" cg than the Soot's cg pack
        application.constructCallgraph();

        //must be after cg construction
        configAppMain(application);
    }


    private static void configAppMain(SetupApplication application) {
        // Prevents bug caused by FlowDroid creating the dummy main
        // without calling it main, which would cause Soot to not function properly
        SootMethod flowdroidDummyMainMethod = application.getDummyMainMethod();
        flowdroidDummyMainMethod.setName("main");
        SootClass flowdroidDummyMainClass = flowdroidDummyMainMethod.getDeclaringClass();

        // Set FlowDroid dummy main method/class as Soot instance's main method/class
        Scene.v().setEntryPoints(Collections.singletonList(flowdroidDummyMainMethod));
        Scene.v().setMainClass(flowdroidDummyMainClass);
    }

    private static SootConfigForAndroid createSootConfigForAndroid(String sdkPath, String apkPath) {
        return new SootConfigForAndroid() {
            @Override
            public void setSootOptions(@NotNull Options options, InfoflowConfiguration config) {
                G.reset();

                // Generic options
                options.v().set_allow_phantom_refs(true);
                options.v().set_whole_program(true);
                options.v().set_prepend_classpath(true);
                options.v().set_app(true);
                options.v().set_no_bodies_for_excluded(false);

                // Lib classes to be included for analysis to work with VASCO
                /*
                List<String> includeList = new LinkedList<String>();
                includeList.add("android.*");
                includeList.add("android.app.*");
                includeList.add("android.app.Activity");
                includeList.add("android.preference.*");
                includeList.add("android.content.*");
                options.v().set_include(includeList);
                options.v().set_include_all(true);
                 */

                // Experimental
                options.v().set_drop_bodies_after_load(false);
                options.v().setPhaseOption("wjtp", "use-original-names:true");
                //options.v().set_soot_classpath("/home/ricardo/Android/Sdk/platforms/android-30/android.jar");

                // Read APK options
                options.v().set_android_jars(sdkPath); // The path to Android Platforms
                //Options.v().set_force_android_jar(androidJar);
                options.v().set_src_prec(Options.src_prec_apk); // Determine the input is an APK

                options.v().set_process_dir(Collections.singletonList(apkPath)); // Provide paths to the APK
                options.v().set_process_multiple_dex(true);  // Inform Dexpler that the APK may have more than one .dex files

                // Output options
                options.v().set_output_format(Options.output_format_none);

                // CG gen options
                options.v().setPhaseOption("cg", "safe-newinstance:true");
                options.v().setPhaseOption("cg.cha","enabled:false");

                // Enable SPARK call-graph construction
                options.v().setPhaseOption("cg.spark","enabled:true");
                options.v().setPhaseOption("cg.spark","verbose:true");
                options.v().setPhaseOption("cg.spark","on-fly-cg:true");
                options.v().setPhaseOption("cg.spark", "string-constants:true");
            }
        };
    }

    private static InfoflowAndroidConfiguration createInfoflowAndroidConfiguration(String sdkPath, String apkPath) {
        InfoflowAndroidConfiguration infoFlowConfig = new InfoflowAndroidConfiguration();
        infoFlowConfig.setTaintAnalysisEnabled(false);
        infoFlowConfig.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        infoFlowConfig.getAnalysisFileConfig().setAndroidPlatformDir(sdkPath);
        //infoFlowConfig.getAnalysisFileConfig().setAdditionalClasspath("/home/ricardo/Android/Sdk/platforms/android-30/android.jar");
        infoFlowConfig.setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.UseExistingInstance);
        return infoFlowConfig;
    }

}
