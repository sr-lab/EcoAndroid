package leaks;

import leaks.*;
import leaks.results.AnalysisVisitor;
import leaks.results.ResultsStandalone;
import soot.*;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class StandaloneRL {

    public static void main(String[] args) {
        if (args.length >= 3) {
            System.out.println("Running analysis");
            runAnalysis(args[0], args[1], args[2]);
        } else {
            System.out.println("Missing arguments: sdkPath apkPath outputFolder");
        }
    }

    private static void runAnalysis(String sdkPath, String apkPath, String outputFolder) {
        String apkFileName = apkPath.substring(apkPath.lastIndexOf("/")+1);
        String apkName = apkFileName.substring(0, apkFileName.indexOf("."));

        File file = new File(outputFolder + apkName + ".out");
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("Unable to ");
        }

        System.out.println("~ Resource Leak Analysis - Standalone ver. ~");

        System.out.println("Setting up Soot...");
        System.out.println("Apk path: " + apkPath);

        long startSetup = System.nanoTime();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Integer> future = executor.submit(new Callable() {
            public Integer call() throws Exception {
                SootSetup.configSootInstance(sdkPath, apkPath);
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

        System.out.println("Registering transformers...");
        registerTransformers();

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

    private static void registerTransformers() {
        // whole-jimple packs
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                IFDSRLAnalysis analysis = new IFDSRLAnalysis();
                analysis.doAnalysis(true);
                analysis.accept(AnalysisVisitor.getInstance(), ResultsStandalone.getInstance());
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
                    analysis.accept(AnalysisVisitor.getInstance(), ResultsStandalone.getInstance());
                }
            }
        }));
    }

    // TODO Implement in IntelliJ version!
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
