package com.example.impfile;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 파일명 전체(확장자 제외)를 우선 그룹명으로 하되,
 * 수식어(최종, 진짜, 숫자 조합 등)를 제거합니다.
 */
public class FileGrouper {
    // “최종”, “진짜” 반복 + 숫자 조합 접미사 전부 제거
    private static final Pattern SUFFIXES
            = Pattern.compile("(?i)(최종|진짜|\\d+)+$");

    public static Map<String, List<File>> groupFiles(List<File> files) {
        Map<String, List<File>> groups = new HashMap<>();
        for (File f : files) {
            String name = f.getName();
            // 1) 확장자 제외한 전체를 그룹 후보로
            String base = name.contains(".")
                    ? name.substring(0, name.lastIndexOf('.'))
                    : name;
            // 2) 접미사(진짜/최종/숫자) 제거
            String grp = SUFFIXES.matcher(base).replaceAll("");
            // 3) 공백만 있거나 빈 경우 원본 base 사용
            if (grp.isBlank()) grp = base;

            groups.computeIfAbsent(grp, k -> new ArrayList<>()).add(f);
        }
        return groups;
    }
}
