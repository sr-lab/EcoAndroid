package leaks.standalone;

import leaks.IFDSRLAnalysis;
import leaks.RLAnalysis;
import leaks.Resource;
import leaks.SootSetup;
import soot.*;
import soot.jimple.AbstractStmtSwitch;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class StandaloneRL {

    public static void main(String[] args) {

        File file = new File("/home/ricardo/ecoandroid.out");
        try {
            PrintStream stream = new PrintStream(file);
            System.setOut(stream);
        } catch (Exception e) {
            System.out.println("exception");
        }

        System.out.println("~ Resource Leak Analysis - Standalone ver. ~");

        if (args.length >= 2) {
            System.out.println("Running analysis");
            runAnalysis(args[0], args[1]);
        } else {
            System.out.println("Missing arguments: sdkPath apkPath");
        }
    }

    private static void runAnalysis(String sdkPath, String apkPath) {
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
            future.get(5, TimeUnit.MINUTES); //timeout is in 2 seconds
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            System.out.println("Failed! Soot setup timed out.");
            return;
        }
        executor.shutdownNow();

        System.out.println("Registering transformers...");
        registerTransformers();

        int counter = 0;
        for (SootClass c : Scene.v().getApplicationClasses()) {
            //HashSet<SootMethod> methods = new HashSet<>(c.getMethods());
            List<SootMethod> methods = c.getMethods();
            int count = c.getMethodCount();
            for (SootMethod m : methods) {
                if (m.getName().equals("splitTagsList") || m.getName().equals("getWhiteboardState") ||
                    c.getName().equals("com.ichi2.anki.Deck")) {
                    System.out.print("");
                }
                if (m.hasActiveBody()) {
                    Body body = m.retrieveActiveBody();

                    Chain<Local> locals = body.getLocals();
                    UnitPatchingChain units = body.getUnits();
                    if (methodHasResources(m)) {
                        // Passos para instrumentação de aliasing:
                        //  - procurar por assign stmt (rx = ry) onde rx e ry são recursos do mesmo tipo
                        //  - escolher um dos locals para representar o recurso ao longo do método
                        //  - alterar todas as ocorrências do outro local pelo seu representante
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
                                            //stmt.setLeftOp(rightLocal);
                                            for (Local l : locals) {
                                                if (l.equivTo(leftLocal) || l.equivTo(rightLocal)) {
                                                    l.setName("RESOURCE_" + counter);
                                                    //l.setNumber(rightLocal.getNumber());
                                                }
                                            }
                                            counter++;
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

        writeResultsToFile(apkPath);

        System.out.println("Done!");
    }

    private static void writeResultsToFile(String apkPath) {
        try {
            String apkFileName = apkPath.substring(apkPath.lastIndexOf("/")+1);
            String apkName = apkFileName.substring(0, apkFileName.indexOf("."));
            Results.getInstance().toCSV(apkName);
        } catch (IOException e) {
            System.out.println("Failed writing results to file!");
        }
    }

    private static void runInterProceduralAnalysis() {
        PackManager.v().getPack("wjtp").apply();
        PackManager.v().writeOutput();
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
        /*

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

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.instr", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                for (SootClass c : Scene.v().getApplicationClasses()) {
                    HashSet<SootMethod> methods = new HashSet<>(c.getMethods());
                    for (SootMethod m : methods) {
                        if (m.getName().equals("splitTagsList")) {
                            System.out.print("");
                        }
                        if (!m.hasActiveBody()) break;

                        Body body = m.retrieveActiveBody();
                        Chain<Local> locals = body.getLocals();
                        UnitPatchingChain units = body.getUnits();
                        if (methodHasResources(m)) {
                            // Passos para instrumentação de aliasing:
                            //  - procurar por assign stmt (rx = ry) onde rx e ry são recursos do mesmo tipo
                            //  - escolher um dos locals para representar o recurso ao longo do método
                            //  - alterar todas as ocorrências do outro local pelo seu representante

                            for(Iterator iter = units.snapshotIterator(); iter.hasNext();) {
                                final Unit unit = (Unit) iter.next();
                                unit.apply(new AbstractStmtSwitch() {

                                    public void caseAssignStmt(AssignStmt stmt) {
                                        if (stmt.getLeftOp() instanceof Local && stmt.getRightOp() instanceof Local) {
                                            Local leftLocal = (Local) stmt.getLeftOp();
                                            Local rightLocal = (Local) stmt.getRightOp();
                                            String leftLocalType = leftLocal.getType().toString();
                                            String rightLocalType = rightLocal.getType().toString();

                                            for (Resource r : Resource.values()) {
                                                if (r.getType().equals(leftLocalType) && leftLocalType.equals(rightLocalType)) {
                                                    //stmt.setLeftOp(rightLocal);
                                                    for (Local l : locals) {
                                                        if (l.equivTo(leftLocal)) {
                                                            l.setName("RESOURCE");
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                        m.getActiveBody().validate();
                                    }

                                });
                            }

                        }

                    }

                    c.validate();
                }
            }
        }));
        PackManager.v().getPack("wjtp").apply();
        PackManager.v().writeOutput();


         */

        PackManager.v().getPack("wjtp").add(new Transform("wjtp.ifdsrl", new SceneTransformer() {
            @Override
            protected void internalTransform(String s, Map<String, String> map) {
                IFDSRLAnalysis analysis = new IFDSRLAnalysis();
                analysis.doAnalysis();
                analysis.accept(Results.getInstance());
            }
        }));

        //PackManager.v().getPack("wjtp").apply();
    }

    // TESTING
    public static boolean methodHasResources(SootMethod m) {
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
