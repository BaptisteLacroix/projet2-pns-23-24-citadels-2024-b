package com.seinksansdoozebank.fr;

import com.beust.jcommander.JCommander;
import com.seinksansdoozebank.fr.jcommander.CommandLineArgs;
import com.seinksansdoozebank.fr.statistics.GameStatisticsAnalyzer;

import static com.seinksansdoozebank.fr.statistics.GameStatisticsAnalyzer.CsvCategory.BEST_AGAINST_SECOND;
import static com.seinksansdoozebank.fr.statistics.GameStatisticsAnalyzer.CsvCategory.BEST_BOTS_AGAINST;
import static com.seinksansdoozebank.fr.statistics.GameStatisticsAnalyzer.CsvCategory.DEMO_GAME;

/**
 * The main class of the application
 */
public class Launcher {
    /**
     * The main method of the application
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        // Define a class to hold your command-line parameters
        Launcher launcher = new Launcher();
        CommandLineArgs cmdArgs = new CommandLineArgs();

        // Parse command-line arguments
        JCommander.newBuilder()
                .addObject(cmdArgs)
                .build()
                .parse(args);

        if (cmdArgs.getQuickValue() != null) {
            launcher.runQuickDemo(Integer.parseInt(cmdArgs.getQuickValue()), cmdArgs.isCsv());
        } else if (cmdArgs.isDemo()) {
            launcher.runDemo(cmdArgs.isCsv(), cmdArgs.isVariante());
        } else if (cmdArgs.is2Thousands()) {
            launcher.twoThousand(cmdArgs.isCsv());
        } else if (cmdArgs.isCsv()) {
            launcher.csvDemo();
        }
    }

    /**
     * Run the demo
     *
     * @param saveInCsv true if the results should be saved in a csv file
     */
    public void runDemo(boolean saveInCsv, boolean variante) {
        GameStatisticsAnalyzer analyzer = new GameStatisticsAnalyzer(saveInCsv);
        analyzer.runDemo(variante);
    }

    /**
     * Run a quick demo
     *
     * @param nbDistricts the number of districts
     * @param saveInCsv   true if the results should be saved in a csv file
     */
    public void runQuickDemo(int nbDistricts, boolean saveInCsv) {
        GameStatisticsAnalyzer analyzer = new GameStatisticsAnalyzer(saveInCsv);
        analyzer.runQuickDemo(nbDistricts);
    }

    /**
     * Run two thousand games
     *
     * @param saveInCsv true if the results should be saved in a csv file
     */
    public void twoThousand(boolean saveInCsv) {
        GameStatisticsAnalyzer analyzer = new GameStatisticsAnalyzer(1000, saveInCsv, BEST_AGAINST_SECOND);
        analyzer.runAndAnalyze(1, 1, 1, 1, 1, 1);
        analyzer = new GameStatisticsAnalyzer(1000, saveInCsv, BEST_BOTS_AGAINST);
        analyzer.runAndAnalyze(0, 6, 0, 0, 0, 0);
    }

    public void csvDemo() {
        GameStatisticsAnalyzer analyzer = new GameStatisticsAnalyzer(100, true, DEMO_GAME);
        analyzer.runAndAnalyze(1, 1, 1, 1, 1, 1);
    }
}
