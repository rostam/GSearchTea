package org;

import graphtea.extensions.G6Format;
import graphtea.extensions.reports.ChromaticNumber;
import graphtea.extensions.reports.basicreports.*;
import graphtea.extensions.reports.spectralreports.LaplacianEnergy;
import graphtea.extensions.reports.spectralreports.LaplacianEnergyLike;
import graphtea.extensions.reports.zagreb.*;
import graphtea.graph.graph.GraphModel;
import graphtea.plugins.reports.extension.GraphReportExtension;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GraphReportGUI extends JFrame {

    // --- Report registry ---

    private static final LinkedHashMap<String, List<GraphReportExtension>> REPORT_GROUPS = new LinkedHashMap<>();

    static {
        List<GraphReportExtension> general = new ArrayList<>();
        general.add(new NumOfVertices());
        general.add(new NumOfEdges());
        general.add(new MaxAndMinDegree());
        general.add(new NumOfTriangles());
        general.add(new NumOfConnectedComponents());
        general.add(new GirthSize());
        general.add(new IsBipartite());
        general.add(new IsEulerian());
        REPORT_GROUPS.put("General", general);

        List<GraphReportExtension> coloring = new ArrayList<>();
        coloring.add(new ChromaticNumber());
        REPORT_GROUPS.put("Coloring", coloring);

        List<GraphReportExtension> indices = new ArrayList<>();
        indices.add(new RandicIndex());
        indices.add(new HarmonicIndex());
        indices.add(new HyperZagrebIndex());
        indices.add(new BalabanIndex());
        REPORT_GROUPS.put("Topological Indices", indices);

        List<GraphReportExtension> spectral = new ArrayList<>();
        spectral.add(new LaplacianEnergy());
        spectral.add(new LaplacianEnergyLike());
        REPORT_GROUPS.put("Spectral", spectral);
    }

    // --- UI components ---

    private final DefaultListModel<String> graphListModel = new DefaultListModel<>();
    private final JList<String> graphList = new JList<>(graphListModel);
    private final Map<GraphReportExtension, JCheckBox> reportCheckBoxes = new LinkedHashMap<>();
    private final DefaultTableModel tableModel = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final JTable resultsTable = new JTable(tableModel);
    private final JLabel statusLabel = new JLabel("Load a G6 file to begin.");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JTextArea summaryArea = new JTextArea(4, 30);

    private List<String> loadedG6Lines = new ArrayList<>();

    public GraphReportGUI() {
        super("Graph Report Tool");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildCenterPanel());
        mainSplit.setDividerLocation(300);

        add(mainSplit, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);
    }

    // -------------------------------------------------------
    // Panel builders
    // -------------------------------------------------------

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 4));

        // File chooser row
        JButton loadBtn = new JButton("Open G6 File...");
        loadBtn.addActionListener(e -> loadG6File());
        panel.add(loadBtn, BorderLayout.NORTH);

        // Graph list
        graphList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane listScroll = new JScrollPane(graphList);
        listScroll.setBorder(new TitledBorder("Graphs"));
        panel.add(listScroll, BorderLayout.CENTER);

        // Select all / none buttons
        JPanel selBtns = new JPanel(new GridLayout(1, 2, 4, 0));
        JButton selAll = new JButton("Select All");
        selAll.addActionListener(e -> graphList.setSelectionInterval(0, graphListModel.size() - 1));
        JButton selNone = new JButton("Select None");
        selNone.addActionListener(e -> graphList.clearSelection());
        selBtns.add(selAll);
        selBtns.add(selNone);
        panel.add(selBtns, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 8));

        // Top: report selection
        panel.add(buildReportSelectionPanel(), BorderLayout.NORTH);

        // Center: results table
        resultsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(resultsTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setBorder(new TitledBorder("Results"));
        panel.add(tableScroll, BorderLayout.CENTER);

        // Bottom: summary + run/export
        panel.add(buildActionAndSummaryPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JScrollPane buildReportSelectionPanel() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        inner.setBorder(new TitledBorder("Reports"));

        for (Map.Entry<String, List<GraphReportExtension>> entry : REPORT_GROUPS.entrySet()) {
            JPanel groupPanel = new JPanel();
            groupPanel.setLayout(new BoxLayout(groupPanel, BoxLayout.Y_AXIS));
            groupPanel.setBorder(new TitledBorder(entry.getKey()));

            for (GraphReportExtension report : entry.getValue()) {
                JCheckBox cb = new JCheckBox(report.getName());
                reportCheckBoxes.put(report, cb);
                groupPanel.add(cb);
            }
            inner.add(groupPanel);
            inner.add(Box.createHorizontalStrut(8));
        }

        JScrollPane scroll = new JScrollPane(inner,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(new Dimension(0, 130));
        return scroll;
    }

    private JPanel buildActionAndSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        // Summary text area
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane summaryScroll = new JScrollPane(summaryArea);
        summaryScroll.setBorder(new TitledBorder("Summary Statistics"));
        panel.add(summaryScroll, BorderLayout.CENTER);

        // Run + Export buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton runBtn = new JButton("Run Reports");
        runBtn.setFont(runBtn.getFont().deriveFont(Font.BOLD));
        runBtn.addActionListener(this::runReports);
        JButton exportBtn = new JButton("Export CSV...");
        exportBtn.addActionListener(this::exportCsv);
        btnPanel.add(runBtn);
        btnPanel.add(exportBtn);
        panel.add(btnPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout(4, 0));
        bar.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        progressBar.setStringPainted(true);
        progressBar.setString("");
        bar.add(statusLabel, BorderLayout.CENTER);
        bar.add(progressBar, BorderLayout.EAST);
        progressBar.setPreferredSize(new Dimension(200, 20));
        return bar;
    }

    // -------------------------------------------------------
    // Actions
    // -------------------------------------------------------

    private void loadG6File() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Open G6 File");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try {
            List<String> lines = Files.readAllLines(Paths.get(file.getAbsolutePath()))
                    .stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .collect(Collectors.toList());

            loadedG6Lines = lines;
            graphListModel.clear();
            for (int i = 0; i < lines.size(); i++) {
                graphListModel.addElement((i + 1) + ": " + lines.get(i));
            }
            graphList.setSelectionInterval(0, graphListModel.size() - 1);
            statusLabel.setText("Loaded " + lines.size() + " graphs from " + file.getName());
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            summaryArea.setText("");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void runReports(ActionEvent e) {
        int[] selectedIndices = graphList.getSelectedIndices();
        if (selectedIndices.length == 0) {
            JOptionPane.showMessageDialog(this, "Select at least one graph.", "No graphs selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<GraphReportExtension> selectedReports = reportCheckBoxes.entrySet().stream()
                .filter(en -> en.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selectedReports.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one report.", "No reports selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Build columns: Graph | report1 | report2 | ...
        String[] columns = new String[selectedReports.size() + 1];
        columns[0] = "Graph (G6)";
        for (int i = 0; i < selectedReports.size(); i++) {
            columns[i + 1] = selectedReports.get(i).getName();
        }

        tableModel.setRowCount(0);
        tableModel.setColumnIdentifiers(columns);

        SwingWorker<List<Object[]>, Object[]> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<>();
                int total = selectedIndices.length;
                for (int progress = 0; progress < total; progress++) {
                    int idx = selectedIndices[progress];
                    String g6 = loadedG6Lines.get(idx);
                    GraphModel graph;
                    try {
                        graph = G6Format.stringToGraphModel(g6);
                    } catch (Exception ex) {
                        continue;
                    }

                    Object[] row = new Object[selectedReports.size() + 1];
                    row[0] = g6;
                    for (int r = 0; r < selectedReports.size(); r++) {
                        try {
                            Object result = selectedReports.get(r).calculate(graph);
                            row[r + 1] = formatResult(result);
                        } catch (Exception ex) {
                            row[r + 1] = "error";
                        }
                    }
                    rows.add(row);
                    publish(row);
                    setProgress((int) ((progress + 1) * 100.0 / total));
                }
                return rows;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    tableModel.addRow(row);
                }
            }

            @Override
            protected void done() {
                try {
                    List<Object[]> allRows = get();
                    progressBar.setValue(100);
                    progressBar.setString("Done");
                    statusLabel.setText("Computed " + selectedReports.size() + " report(s) on "
                            + allRows.size() + " graph(s).");
                    autoResizeColumns();
                    updateSummary(allRows, selectedReports);
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        };

        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int p = (Integer) evt.getNewValue();
                progressBar.setValue(p);
                progressBar.setString(p + "%");
            }
        });

        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Running...");
        tableModel.setRowCount(0);
        worker.execute();
    }

    private void exportCsv(ActionEvent e) {
        if (tableModel.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "No results to export.", "Nothing to export",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save CSV");
        fc.setSelectedFile(new File("results.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header
            List<String> header = new ArrayList<>();
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                header.add(csvEscape(tableModel.getColumnName(c)));
            }
            pw.println(String.join(",", header));

            // Rows
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                List<String> row = new ArrayList<>();
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    Object val = tableModel.getValueAt(r, c);
                    row.add(csvEscape(val == null ? "" : val.toString()));
                }
                pw.println(String.join(",", row));
            }

            statusLabel.setText("Exported to " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    @SuppressWarnings("unchecked")
    private String formatResult(Object result) {
        if (result == null) return "N/A";
        if (result instanceof ArrayList) {
            List<?> list = (List<?>) result;
            if (list.isEmpty()) return "[]";
            Object first = list.get(0);
            // RandicIndex returns ["Randic Index : 5.23"] — extract the number part
            if (first instanceof String && ((String) first).contains(":")) {
                return ((String) first).substring(((String) first).lastIndexOf(':') + 1).trim();
            }
            // MaxAndMinDegree returns [max, min]
            if (first instanceof Integer && list.size() == 2) {
                return list.get(0) + " / " + list.get(1);
            }
            return list.stream().map(Object::toString).collect(Collectors.joining(", "));
        }
        return result.toString();
    }

    private void autoResizeColumns() {
        for (int col = 0; col < resultsTable.getColumnCount(); col++) {
            int maxWidth = resultsTable.getColumnModel().getColumn(col)
                    .getHeaderValue().toString().length() * 8 + 16;
            for (int row = 0; row < Math.min(resultsTable.getRowCount(), 200); row++) {
                Object val = resultsTable.getValueAt(row, col);
                if (val != null) {
                    int w = val.toString().length() * 7 + 16;
                    maxWidth = Math.max(maxWidth, w);
                }
            }
            resultsTable.getColumnModel().getColumn(col).setPreferredWidth(Math.min(maxWidth, 250));
        }
    }

    private void updateSummary(List<Object[]> rows, List<GraphReportExtension> reports) {
        if (rows.isEmpty()) {
            summaryArea.setText("No results.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-30s  %8s  %8s  %8s  %8s%n",
                "Report", "Min", "Max", "Mean", "Count"));
        sb.append("-".repeat(70)).append("\n");

        for (int r = 0; r < reports.size(); r++) {
            final int col = r + 1; // column in row array
            List<Double> nums = new ArrayList<>();
            for (Object[] row : rows) {
                if (row[col] == null) continue;
                try {
                    nums.add(Double.parseDouble(row[col].toString()));
                } catch (NumberFormatException ignore) {}
            }
            if (nums.isEmpty()) continue;

            double min = nums.stream().mapToDouble(d -> d).min().getAsDouble();
            double max = nums.stream().mapToDouble(d -> d).max().getAsDouble();
            double mean = nums.stream().mapToDouble(d -> d).average().getAsDouble();
            String name = reports.get(r).getName();
            if (name.length() > 29) name = name.substring(0, 26) + "...";

            // Use integer format if all values are whole numbers
            boolean allInt = nums.stream().allMatch(d -> d == Math.floor(d));
            if (allInt) {
                sb.append(String.format("%-30s  %8.0f  %8.0f  %8.2f  %8d%n",
                        name, min, max, mean, nums.size()));
            } else {
                sb.append(String.format("%-30s  %8.4f  %8.4f  %8.4f  %8d%n",
                        name, min, max, mean, nums.size()));
            }
        }
        summaryArea.setText(sb.toString());
    }

    private static String csvEscape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    // -------------------------------------------------------
    // Main
    // -------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {}
            new GraphReportGUI().setVisible(true);
        });
    }
}
