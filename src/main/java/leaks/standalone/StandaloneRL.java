package leaks.standalone;

import leaks.IFDSRLAnalysis;
import leaks.RLAnalysis;
import leaks.SootSetup;
import soot.*;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StandaloneRL {

    public static void main(String[] args) {
        System.out.println("~ Resource Leak Analysis - Standalone ver. ~");

        if (args.length >= 2) {
            System.out.println("Running analysis");
            System.out.println("APK: " + args[1]);
            runAnalysis(args[0], args[1]);
        } else {
            System.out.println("Missing arguments: sdkPath apkPath");
            return;
        }
    }

    private static void runAnalysis(String sdkPath, String apkPath) {
        System.out.println("Setting up Soot...");
        long startSetup = System.nanoTime();
        SootSetup.configSootInstance(sdkPath, apkPath);
        System.out.println("Registering transformers...");
        registerTransformers();

        long startInterProc = System.nanoTime();
        System.out.println("Running inter-procedural analysis...");
        runInterProceduralAnalysis();
        long end = System.nanoTime();

        long setupDuration = TimeUnit.MILLISECONDS.convert(startInterProc - startSetup, TimeUnit.NANOSECONDS);
        long analysisDuration = TimeUnit.MILLISECONDS.convert(end - startInterProc, TimeUnit.NANOSECONDS);

        System.out.println("Setup done in " + setupDuration +  "ms, analysis done in " + analysisDuration + "ms");
        writeResultsToFile(apkPath);

        System.out.println("Done!");
        return;
    }

    private static void writeResultsToFile(String apkPath) {
        try {
            String apkFileName = apkPath.substring(apkPath.lastIndexOf("/")+1);
            String apkName = apkFileName.substring(0, apkFileName.indexOf("."));
            Results.getInstance().toCSV(apkName);
        } catch (IOException e) {
            System.out.println("Failed writing results to file!");
            return;
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

    private static void registerTransformers() {
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                IFDSRLAnalysis analysis = new IFDSRLAnalysis();
                analysis.doAnalysis();
                analysis.accept(Results.getInstance());
            }
        }));

        PackManager.v().getPack("jtp").add(new Transform("jtp.rl", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                SootMethod method = body.getMethod();
                UnitGraph graph = new ExceptionalUnitGraph(method.getActiveBody());
                RLAnalysis analysis = new RLAnalysis(graph);

                if (!analysis.getResults().isEmpty()) {
                    analysis.accept(Results.getInstance());
                }
            }
        }));
    }

}
