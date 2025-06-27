package com.example.impfile.db;

/**
 * DB에 저장할 레코드 한 건을 나타내는 VO/DTO
 */
public class FileRecord {
    private String groupName;
    private String filePath;
    private int    score;
    private long   lastModified;

    public FileRecord(String groupName, String filePath, int score, long lastModified) {
        this.groupName    = groupName;
        this.filePath     = filePath;
        this.score        = score;
        this.lastModified = lastModified;
    }

    public String getGroupName()    { return groupName; }
    public String getFilePath()     { return filePath; }
    public int    getScore()        { return score; }
    public long   getLastModified() { return lastModified; }

    public void setGroupName(String groupName)     { this.groupName = groupName; }
    public void setFilePath(String filePath)       { this.filePath = filePath; }
    public void setScore(int score)                { this.score = score; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }
}
