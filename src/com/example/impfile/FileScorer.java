package com.example.impfile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 파일명 키워드, 크기, 수정시간, 확장자, 길이 등
 * 다양한 요소를 가중치 조합해 상대적 중요도를 계산합니다.
 */
public class FileScorer {
    private final List<String>         keywords;
    private final Map<String, Integer> weightMap;

    public FileScorer(List<String> keywords, Map<String, Integer> weightMap) {
        this.keywords  = keywords;
        this.weightMap = weightMap;
    }

    public int calculateGroupRelativeScore(File file,
                                           long groupEarliestModification, long groupBaselineSize) {

        int score = 0;
        score += scoreByRelativeSize(file, groupBaselineSize)
                * weightMap.getOrDefault("size", 1);
        score += scoreByRelativeModificationTime(file, groupEarliestModification)
                * weightMap.getOrDefault("modified", 1);
        score += scoreByFilenameKeywords(file.getName())
                * weightMap.getOrDefault("keyword", 1);
        score += scoreByRevision(file.getName())
                * weightMap.getOrDefault("revision", 1);
        score += scoreByFilenameLength(file.getName())
                * weightMap.getOrDefault("length", 1);
        score += scoreByExtension(file.getName())
                * weightMap.getOrDefault("extension", 1);

        return score;
    }

    private int scoreByRelativeModificationTime(File file, long earliest) {
        long diffMin = (file.lastModified() - earliest) / (60 * 1000);
        if      (diffMin == 0)   return 3;
        else if (diffMin <=  1)   return 4;
        else if (diffMin <= 15)   return 6;
        else if (diffMin <= 60)   return 8;
        else if (diffMin <=180)   return 10;
        else                      return 7;
    }

    private int scoreByRelativeSize(File file, long baseSize) {
        double ratio = (double) file.length() / baseSize;
        if      (ratio >= 0.9 && ratio <= 1.1) return 5;
        else if (ratio <  0.9)                 return 4;
        else                                   return 6;
    }

    private int scoreByFilenameKeywords(String name) {
        int sc = 0;
        String lower = name.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase())) sc += 5;
        }
        return sc;
    }

    private int scoreByRevision(String name) {
        Pattern p = Pattern.compile("^(진짜)+");
        Matcher m = p.matcher(name);
        if (m.find()) {
            int cnt = m.group().length() / "진짜".length();
            return cnt >= 2 ? 3 : 0;
        }
        return 0;
    }

    private int scoreByFilenameLength(String name) {
        return name.length() >= 15 ? 5 : 2;
    }

    private int scoreByExtension(String name) {
        String lower = name.toLowerCase();
        if      (lower.endsWith(".hwp"))      return 5;
        else if (lower.endsWith(".pptx")
                || lower.endsWith(".pdf"))      return 9;
        else if (lower.endsWith(".docx")
                || lower.endsWith(".xlsx"))    return 7;
        else                                  return 5;
    }
}
