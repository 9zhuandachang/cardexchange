package com.example.cardexchange.Pojo;

import java.util.ArrayList;
import java.util.List;

public class RecognitionResultVo {
    private boolean recognized;
    private String message;
    private boolean faceDetected;
    private double faceConfidence;
    private List<Double> faceBox;
    private List<String> ocrTexts = new ArrayList<>();
    private long elapsedMs;
    private List<MatchItem> matches = new ArrayList<>();

    public static class MatchItem {
        private String imgUrl;
        private String team;
        private String playerName;
        private double score;
        private double faceScore;
        private boolean ocrMatched;
        private int confidencePercent;
        private List<String> matchReasons = new ArrayList<>();
        private CardVo card;

        public String getImgUrl() {
            return imgUrl;
        }

        public void setImgUrl(String imgUrl) {
            this.imgUrl = imgUrl;
        }

        public String getTeam() {
            return team;
        }

        public void setTeam(String team) {
            this.team = team;
        }

        public String getPlayerName() {
            return playerName;
        }

        public void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getFaceScore() {
            return faceScore;
        }

        public void setFaceScore(double faceScore) {
            this.faceScore = faceScore;
        }

        public boolean isOcrMatched() {
            return ocrMatched;
        }

        public void setOcrMatched(boolean ocrMatched) {
            this.ocrMatched = ocrMatched;
        }

        public int getConfidencePercent() {
            return confidencePercent;
        }

        public void setConfidencePercent(int confidencePercent) {
            this.confidencePercent = confidencePercent;
        }

        public List<String> getMatchReasons() {
            return matchReasons;
        }

        public void setMatchReasons(List<String> matchReasons) {
            this.matchReasons = matchReasons;
        }

        public CardVo getCard() {
            return card;
        }

        public void setCard(CardVo card) {
            this.card = card;
        }
    }

    public boolean isRecognized() {
        return recognized;
    }

    public void setRecognized(boolean recognized) {
        this.recognized = recognized;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFaceDetected() {
        return faceDetected;
    }

    public void setFaceDetected(boolean faceDetected) {
        this.faceDetected = faceDetected;
    }

    public double getFaceConfidence() {
        return faceConfidence;
    }

    public void setFaceConfidence(double faceConfidence) {
        this.faceConfidence = faceConfidence;
    }

    public List<Double> getFaceBox() {
        return faceBox;
    }

    public void setFaceBox(List<Double> faceBox) {
        this.faceBox = faceBox;
    }

    public List<String> getOcrTexts() {
        return ocrTexts;
    }

    public void setOcrTexts(List<String> ocrTexts) {
        this.ocrTexts = ocrTexts;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public List<MatchItem> getMatches() {
        return matches;
    }

    public void setMatches(List<MatchItem> matches) {
        this.matches = matches;
    }
}
