package com.example.impfile;

import com.example.impfile.db.FileRecord;
import com.example.impfile.db.FileRecordDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 메인 GUI: 폴더/파일 드래그&드롭 → 그룹별 중요도 산정 → DB 저장/백업 → 테이블·복구 다이얼로그
 */
public class FileGroupListGUI extends JFrame {
    private final FileRecordDAO dao    = new FileRecordDAO();
    private final FileScorer    scorer;
    private final JTable        mainTable;
    private final DefaultTableModel mainModel;

    // 드롭된 세션(폴더 또는 "야생파일")
    private static class Session {
        final String     name;
        final List<File> files;
        Session(String name, List<File> files) {
            this.name  = name;
            this.files = files;
        }
    }
    private final List<Session> sessions    = new ArrayList<>();
    private final JPanel        sessionPanel;

    private Map<String,Integer> groupMaxScores = new HashMap<>();
    private Map<String,Color>   groupColorMap  = new HashMap<>();

    public FileGroupListGUI(FileScorer scorer) {
        this.scorer = scorer;
        setTitle("그룹별 중요도 목록");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        // ─── 상단 안내 + DB 보기 버튼 ─────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel hint = new JLabel("폴더나 파일을 이 창에 드래그하세요", SwingConstants.CENTER);
        hint.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        topBar.add(hint, BorderLayout.CENTER);
        JButton btnDB = new JButton("DB 보기");
        btnDB.addActionListener(e -> showDBDialog());
        topBar.add(btnDB, BorderLayout.EAST);

        // ─── 세션 표시 패널 ─────────────────────────────────
        sessionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        sessionPanel.setOpaque(false);
        JScrollPane sessionScroll = new JScrollPane(
                sessionPanel,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        sessionScroll.setOpaque(false);
        sessionScroll.getViewport().setOpaque(false);
        sessionScroll.setBorder(null);
        sessionScroll.setPreferredSize(new Dimension(900, 60));

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topBar, BorderLayout.NORTH);
        northContainer.add(sessionScroll, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // ─── 메인 테이블 세팅 ─────────────────────────────────
        mainModel = new DefaultTableModel(
                new Object[]{"그룹","파일 이름","파일 경로","중요도","열기"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 4;
            }
        };
        mainTable = new JTable(mainModel);
        mainTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col
            ){
                Component comp = super.getTableCellRendererComponent(t,v,sel,foc,row,col);
                if (col != 4) {
                    String grp = (String) t.getValueAt(row,0);
                    int sc     = (Integer) t.getValueAt(row,3);
                    if (sc == groupMaxScores.getOrDefault(grp, sc)) {
                        comp.setBackground(groupColorMap.getOrDefault(grp, new Color(255,255,200)));
                        comp.setFont(comp.getFont().deriveFont(Font.BOLD));
                    } else {
                        comp.setBackground(Color.WHITE);
                        comp.setFont(comp.getFont().deriveFont(Font.PLAIN));
                    }
                }
                return comp;
            }
        });
        mainTable.getColumn("열기").setCellRenderer(new ButtonRenderer("열기"));
        mainTable.getColumn("열기").setCellEditor(new OpenButtonEditor());
        add(new JScrollPane(mainTable), BorderLayout.CENTER);

        // ─── Drag & Drop 처리 ─────────────────────────────────
        setTransferHandler(new TransferHandler(){
            @Override public boolean canImport(TransferSupport sup) {
                return sup.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }
            @Override @SuppressWarnings("unchecked")
            public boolean importData(TransferSupport sup) {
                try {
                    List<File> dropped = (List<File>) sup.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    List<File> wildFiles = new ArrayList<>();

                    for (File f : dropped) {
                        if (f.isDirectory()) {
                            File[] arr = Optional.ofNullable(f.listFiles()).orElse(new File[0]);
                            List<File> inside = Stream.of(arr)
                                    .filter(File::isFile)
                                    .filter(x -> !x.getName().toLowerCase().matches(".*\\.(tmp|ds_store)$"))
                                    .toList();
                            if (!inside.isEmpty()) addSession(f.getName(), inside);
                        } else if (f.isFile()) {
                            wildFiles.add(f);
                        }
                    }
                    if (!wildFiles.isEmpty()) {
                        addSession("야생파일", wildFiles);
                    }
                    if (!sessions.isEmpty()) {
                        // 드롭 후에는 항상 영속성 모드
                        refreshView(true);
                        return true;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return false;
            }
        });
    }

    /** 새로운 세션 추가 + X 버튼 핸들러 (X 누르면 UI만 갱신) */
    private void addSession(String name, List<File> files) {
        for (Session s : sessions) {
            if (s.name.equals(name)) return;  // 중복 방지
        }
        Session s = new Session(name, files);
        sessions.add(s);

        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pnl.setOpaque(false);
        JLabel lbl = new JLabel(" " + name + " ");
        JButton btn = new JButton("X");
        btn.setMargin(new Insets(0,2,0,2));
        btn.addActionListener((ActionEvent e) -> {
            sessions.remove(s);
            sessionPanel.remove(pnl);
            sessionPanel.revalidate();
            sessionPanel.repaint();
            // UI만 갱신: DB/백업 로직 건너뜀
            refreshView(false);
        });
        pnl.add(lbl);
        pnl.add(btn);
        sessionPanel.add(pnl);
        sessionPanel.revalidate();
        sessionPanel.repaint();
    }

    /** 기본 호출: persist=true */
    private void refreshView() {
        refreshView(true);
    }

    /**
     * @param persist true: DB insertOrReplace/backupRec 수행
     *                false: UI 렌더링만
     */
    private void refreshView(boolean persist) {
        List<File> all = sessions.stream()
                .flatMap(sess -> sess.files.stream())
                .toList();

        mainModel.setRowCount(0);
        groupMaxScores.clear();
        groupColorMap.clear();
        if (all.isEmpty()) return;

        Map<String,List<File>> groups = FileGrouper.groupFiles(all);
        Map<String,Long> earliest = new HashMap<>();
        Map<String,Long> baseSize = new HashMap<>();
        groups.forEach((grp, flist) -> {
            long e = Long.MAX_VALUE, s = 0;
            for (File f : flist) {
                if (f.lastModified() < e) {
                    e = f.lastModified();
                    s = f.length();
                }
            }
            earliest.put(grp, e);
            baseSize.put(grp, s);
        });

        Random rnd = new Random();
        for (var en : groups.entrySet()) {
            String grp = en.getKey();
            Color bg = new Color(
                    200 + rnd.nextInt(55),
                    200 + rnd.nextInt(55),
                    200 + rnd.nextInt(55)
            );
            groupColorMap.put(grp, bg);

            long eTime = earliest.get(grp), bSz = baseSize.get(grp);
            for (File f : en.getValue()) {
                int score = scorer.calculateGroupRelativeScore(f, eTime, bSz);

                if (persist) {
                    FileRecord old = dao.findByGroup(grp);
                    boolean update = old == null
                            || score > old.getScore()
                            || (score == old.getScore() && f.lastModified() > old.getLastModified());

                    File backupDir = new File("backup", grp);
                    boolean missing = !backupDir.exists()
                            || Arrays.stream(Optional.ofNullable(backupDir.list()).orElse(new String[0]))
                            .noneMatch(n -> n.equals(f.getName()));

                    if (update || missing) {
                        FileRecord rec = new FileRecord(
                                grp, f.getAbsolutePath(), score, f.lastModified()
                        );
                        dao.insertOrReplace(rec);
                        backupRec(rec);
                    }
                }

                groupMaxScores.put(grp,
                        Math.max(groupMaxScores.getOrDefault(grp, 0), score));
                mainModel.addRow(new Object[]{ grp, f.getName(), f.getAbsolutePath(), score, "열기" });
            }
        }
    }

    /** 파일을 backup/{group}/{filename} 으로 복사 */
    private void backupRec(FileRecord rec) {
        File orig = new File(rec.getFilePath());
        File dir  = new File("backup", rec.getGroupName());
        if (!dir.exists()) dir.mkdirs();
        File dst = new File(dir, orig.getName());
        try {
            Files.copy(orig.toPath(), dst.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** DB 저장 항목 보기 & 복구 대화상자 */
    private void showDBDialog() {
        // 1) 데이터 로드
        List<FileRecord> rows = dao.findAll();
        // 2) 테이블 모델 생성
        DefaultTableModel dbModel = new DefaultTableModel(
                new Object[]{"그룹","파일 경로","점수","수정시간","복구"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) {
                return c == 4;
            }
        };
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (FileRecord rec : rows) {
            dbModel.addRow(new Object[]{
                    rec.getGroupName(),
                    rec.getFilePath(),
                    rec.getScore(),
                    sdf.format(rec.getLastModified()),
                    "복구"
            });
        }

        // 3) 테이블 셋업
        JTable dbTable = new JTable(dbModel);
        dbTable.getColumn("복구").setCellRenderer(new ButtonRenderer("복구"));
        dbTable.getColumn("복구").setCellEditor(new RecoverButtonEditor(dbTable));

        // 4) 다이얼로그 구성
        JDialog dlg = new JDialog(this, "DB 저장 파일 보기", true);
        dlg.setSize(800, 450);
        dlg.setLocationRelativeTo(this);

        // 백업 정리 버튼
        JButton btnClean = new JButton("백업 정리");
        btnClean.addActionListener(e -> cleanBackups(dlg, dbModel));

        JPanel p = new JPanel(new BorderLayout());
        p.add(btnClean, BorderLayout.EAST);
        dlg.add(p, BorderLayout.NORTH);
        dlg.add(new JScrollPane(dbTable), BorderLayout.CENTER);

        dlg.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                dlg.dispose();
            }
        });
        dlg.setVisible(true);
    }

    /**
     * 백업 파일 삭제 + DB 레코드 전체 삭제 후
     * 보여주던 dbModel 리셋
     */
    private void cleanBackups(Component parent, DefaultTableModel dbModel) {
        int ans = JOptionPane.showConfirmDialog(parent,
                "백업파일 및 DB 기록을 정말 삭제하시겠습니까?",
                "백업 정리 확인",
                JOptionPane.YES_NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        // 1) 백업 폴더 전체 삭제
        File root = new File("backup");
        int deletedCount = 0;
        if (root.exists()) {
            File[] dirs = root.listFiles(File::isDirectory);
            if (dirs != null) {
                for (File dir : dirs) {
                    deletedCount += deleteDirectoryRecursively(dir);
                }
            }
        }
        // 2) DB 전체 삭제
        dao.deleteAll();
        // 3) 테이블 모델 갱신
        dbModel.setRowCount(0);

        JOptionPane.showMessageDialog(parent,
                "총 " + deletedCount + "개의 백업 파일이 삭제되었고,\n" +
                        "DB의 기록이 모두 초기화되어 목록이 비워졌습니다.");
    }

    /** 디렉토리 및 하위 파일 재귀 삭제 */
    private int deleteDirectoryRecursively(File dir) {
        int count = 0;
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        count += deleteDirectoryRecursively(f);
                    } else if (f.delete()) {
                        count++;
                    }
                }
            }
            if (dir.delete()) count++;
        }
        return count;
    }

    /** 테이블 버튼 렌더러 */
    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer(String txt) {
            setText(txt);
            setOpaque(true);
        }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col
        ) {
            setText(v == null ? getText() : v.toString());
            return this;
        }
    }

    /** “열기” 버튼 에디터 */
    private class OpenButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton();
        private boolean pushed;

        public OpenButtonEditor() {
            super(new JCheckBox());
            btn.setOpaque(true);
            btn.addActionListener(e -> fireEditingStopped());
        }

        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col
        ) {
            btn.setText("열기");
            pushed = true;
            return btn;
        }

        @Override public Object getCellEditorValue() {
            if (pushed) {
                int sel = mainTable.getSelectedRow();
                String path = (String) mainTable.getValueAt(sel, 2);
                File f = new File(path);
                try {
                    if (f.exists()) Desktop.getDesktop().open(f);
                    else JOptionPane.showMessageDialog(mainTable, "파일이 없습니다");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(mainTable, "오류: " + ex.getMessage());
                }
            }
            pushed = false;
            return "열기";
        }
    }

    /** “복구” 버튼 에디터 */
    private class RecoverButtonEditor extends DefaultCellEditor {
        private final JButton btn = new JButton();
        private boolean pushed;
        private final JTable table;

        public RecoverButtonEditor(JTable tbl) {
            super(new JCheckBox());
            this.table = tbl;
            btn.setOpaque(true);
            btn.addActionListener(e -> fireEditingStopped());
        }

        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col
        ) {
            btn.setText("복구");
            pushed = true;
            return btn;
        }

        @Override public Object getCellEditorValue() {
            if (pushed) {
                int sel = table.getSelectedRow();
                String grp = (String) table.getValueAt(sel, 0);
                String path = (String) table.getValueAt(sel, 1);

                File orig = new File(path);
                if (orig.exists()) {
                    JOptionPane.showMessageDialog(table,
                            "원본 파일이 이미 존재합니다. 복구할 필요가 없습니다.");
                } else {
                    File bak = new File("backup", grp + "/" + new File(path).getName());
                    if (!bak.exists()) {
                        JOptionPane.showMessageDialog(table,
                                "백업본이 없어 복구할 수 없습니다.");
                    } else {
                        try {
                            String userHome   = System.getProperty("user.home");
                            Path   desktopDir = Paths.get(userHome, "Desktop", grp);
                            if (!Files.exists(desktopDir)) {
                                Files.createDirectories(desktopDir);
                            }
                            Path target = desktopDir.resolve(bak.getName());
                            Files.copy(bak.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                            JOptionPane.showMessageDialog(table,
                                    "바탕화면으로 복구되었습니다:\n" + target);
                        } catch (IOException ex) {
                            JOptionPane.showMessageDialog(table,
                                    "오류: " + ex.getMessage());
                        }
                    }
                }
            }
            pushed = false;
            return "복구";
        }
    }
}