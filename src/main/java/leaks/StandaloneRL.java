package leaks;

public class StandaloneRL {

    public static void main(String[] args) {
        if (args.length >= 3) {
            System.out.println("Running analysis");
            AnalysisWrapper.getInstance().RunStandaloneAnalysis(args[1], args[0], args[2]);
        } else {
            System.out.println("Missing arguments: sdkPath apkPath outputFolder");
        }
    }
}
