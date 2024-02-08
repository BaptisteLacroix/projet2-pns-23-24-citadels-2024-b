package com.seinksansdoozebank.fr.statistics;

import java.util.HashMap;
import java.util.Map;

public class PlayerStatistics {
    private int totalGames;
    private int gamesWon;
    private int gamesLost;
    private int totalScore;
    private double averageScore;
    private final Map<Integer, Integer> placementCounts;

    public PlayerStatistics() {
        this.totalGames = 0;
        this.gamesWon = 0;
        this.gamesLost = 0;
        this.totalScore = 0;
        this.placementCounts = new HashMap<>();
        // Initialize all positions to 0 by default
        for (int i = 1; i <= 6; i++) {
            placementCounts.put(i, 0);
        }
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public double getWinningPercentage() {
        return totalGames > 0 ? (double) gamesWon / totalGames * 100 : 0;
    }

    public void setTotalGames(int totalGames) {
        if (totalGames < 0) return;
        this.totalGames += totalGames;
    }

    public void setGamesWon(int gamesWon) {
        if (gamesWon < 0) return;
        this.gamesWon += gamesWon;
    }

    public void setGamesLost(int gamesLost) {
        if (gamesLost < 0) return;
        this.gamesLost += gamesLost;
    }

    public double getAverageScore() {
        return this.averageScore;
    }

    public void setAverageScore(double averageScore) {
        if (averageScore < 0) return;
        this.averageScore = averageScore;
    }


    public void setDetailedPlacement(Map<Integer, Integer> placementCounts) {
        // update placement counts
        for (Map.Entry<Integer, Integer> entry : placementCounts.entrySet()) {
            this.placementCounts.put(entry.getKey(), this.placementCounts.get(entry.getKey()) + entry.getValue());
        }
    }

    public void incrementTotalGames() {
        totalGames++;
    }

    public void incrementGamesWon() {
        gamesWon++;
    }

    public void incrementGamesLost() {
        gamesLost++;
    }

    public void addScore(int score) {
        if (score < 0) return;
        totalScore += score;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void recordPlacement(int placement) {
        if (placement < 1 || placement > 6) return;
        placementCounts.put(placement, placementCounts.getOrDefault(placement, 0) + 1);
    }

    public Map<Integer, Integer> getDetailedPlacement() {
        return placementCounts;
    }

    @Override
    public String toString() {
        return "PlayerStatistics{"
                + "totalGames=" + this.getTotalGames()
                + ", gamesWon=" + this.getGamesWon()
                + ", gamesLost=" + this.getGamesLost()
                + ", totalScore=" + this.getTotalScore()
                + ", averageScore=" + this.getAverageScore()
                + ", winningPercentage=" + this.getWinningPercentage()
                + ", placementCounts=" + getDetailedPlacement() + '}';
    }
}