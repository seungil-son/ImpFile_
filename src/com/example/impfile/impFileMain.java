package com.example.impfile;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;

/**
 * 애플리케이션 진입점
 */
public class impFileMain {
    public static void main(String[] args) {
        // 1) 키워드 및 가중치 설정
        List<String> keywords = List.of("보고서","완성","계획");
        Map<String,Integer> weightMap = Map.of(
                "size",     1,
                "modified", 2,
                "keyword",  3,
                "revision", 1,
                "length",   1,
                "extension",1
        );

        // 2) Scorer 생성
        FileScorer scorer = new FileScorer(keywords, weightMap);

        // 3) GUI 실행
        SwingUtilities.invokeLater(() -> {
            FileGroupListGUI gui = new FileGroupListGUI(scorer);
            gui.setVisible(true);
        });
    }
}
