import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;

public final class Main {

    static {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true;
        if (assertionsEnabled) {
            System.out.println("Running with assertions enabled!");
        }
    }

    private static void applySystemLookAndFeel() {
        assert EventQueue.isDispatchThread();

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (final Exception ex) {
            // me no care
        }
    }

    private static void constructUI() {
        assert EventQueue.isDispatchThread();

        final Font mainFont = new Font("Consolas", Font.PLAIN, 14); // TODO(nschultz): Check if font is installed
        final Color mainColor = new Color(235, 233, 216);
        UIManager.put("CheckBox.font", mainFont);
        UIManager.put("Label.font", mainFont);
        UIManager.put("Table.font", mainFont);
        UIManager.put("TableHeader.font", mainFont.deriveFont(Font.BOLD, 16));
        UIManager.put("TextField.font", mainFont);

        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        final JMenuBar menuBar = new JMenuBar() {
            @Override
            public void paintComponent(final Graphics g) {
                super.paintComponent(g);
                g.setColor(mainColor);
                g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
        };
        final JMenu fileMenu = new JMenu("File");
        final JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setOpaque(true);
        exitMenuItem.setBackground(mainColor);
        exitMenuItem.addActionListener(e -> {
            System.exit(0);
        });
        fileMenu.add(exitMenuItem);
        menuBar.add(fileMenu);

        final JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBackground(mainColor);
        final JLabel resultLabel = new JLabel("Occurences: -1", JLabel.LEFT);
        final JLabel timeLabel = new JLabel("Time: -1", JLabel.CENTER);
        final JLabel scannedLabel = new JLabel("Files scanned: -1", JLabel.RIGHT);
        final DefaultTableModel dtm = new DefaultTableModel();
        final JTable resultTable = new JTable(dtm) {
            @Override
            public boolean isCellEditable(final int row, final int column) {
               return false;
            }

            /*@Override
            public Component prepareRenderer(final TableCellRenderer renderer, final int row, final int column) {
               final Component component = super.prepareRenderer(renderer, row, column);
               final int rendererWidth = component.getPreferredSize().width + 8;
               final TableColumn tableColumn = getColumnModel().getColumn(column);
               final double max = Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth());
               tableColumn.setPreferredWidth((int) max);
               return component;
            }*/
        };
        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent evt) {
                final Point point = evt.getPoint();
                final int row = resultTable.rowAtPoint(point);
                if (evt.getClickCount() == 2 && resultTable.getSelectedRow() != -1) {
                    // TODO(nschultz): Hacky, gvim is not installed everywhere
                    final String fileName = dtm.getValueAt(row, 0).toString();
                    final String lineNr   = dtm.getValueAt(row, 1).toString();
                    try {
                        // Runtime.getRuntime().exec(String.format("gvim +%s \"%s\"", lineNr, fileName));
                        Runtime.getRuntime().exec(String.format("gvim --remote-silent +%s \"%s\"", lineNr, fileName));
                    } catch (final IOException ex) {
                        ex.printStackTrace(System.err); // TODO(nschultz): Temporary
                    }
                }
            }
        });
        resultTable.setFillsViewportHeight(true);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.getTableHeader().setOpaque(false);
        resultTable.getTableHeader().setBackground(mainColor);
        resultTable.setBackground(mainColor);
        dtm.setColumnIdentifiers(new Object[] {"File", "Line", "Text"});
        final JPanel resultLabelPanel = new JPanel(new GridLayout(1, 3));
        resultLabelPanel.setBackground(mainColor);
        resultLabelPanel.add(resultLabel);
        resultLabelPanel.add(timeLabel);
        resultLabelPanel.add(scannedLabel);
        resultPanel.add(resultLabelPanel, BorderLayout.NORTH);

        final JProgressBar progressBar = new JProgressBar();

        final JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.getVerticalScrollBar().setBackground(new Color(248, 248, 244));
        //scrollPane.getHorizontalScrollBar().setBackground(Color.BLUE);
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                super.thumbColor = new Color(193, 213, 250);
            }
        });
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        final JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(mainColor);

        final JPanel fieldInputPanel = new JPanel(new GridLayout(3, 1, 10, 1));
        fieldInputPanel.setBackground(mainColor);
        final JTextField dirField = new JTextField(new File(".").getAbsolutePath());
        dirField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        dirField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent evt) {
                if (new File(dirField.getText()).exists()) {
                    dirField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                } else {
                    dirField.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                }
            }
        });
        final JTextField fileField = new JTextField(".*");
        fileField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent evt) {
                try {
                    Pattern.compile(fileField.getText());
                    fileField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                } catch (final PatternSyntaxException ex) {
                    fileField.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
                }
            }
        });
        fileField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        final JTextField searchField = new JTextField();
        searchField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        fieldInputPanel.add(dirField);
        fieldInputPanel.add(fileField);

        final JPanel searchFieldPanel = new JPanel(new BorderLayout());
        searchFieldPanel.setBackground(mainColor);
        searchFieldPanel.add(searchField, BorderLayout.CENTER);
        final JCheckBox multiThreadCheckBox = new JCheckBox("Use multiple threads");
        multiThreadCheckBox.setFocusable(false);
        multiThreadCheckBox.setBackground(mainColor);
        searchFieldPanel.add(multiThreadCheckBox, BorderLayout.EAST);
        fieldInputPanel.add(searchFieldPanel);

        final JPanel labelInputPanel = new JPanel(new GridLayout(3, 1, 10, 1));
        labelInputPanel.setBackground(mainColor);
        final JLabel dirFieldLabel    = new JLabel("Directory ");
        final JLabel fileFieldLabel   = new JLabel("File");
        final JLabel searchFieldLabel = new JLabel("Text");
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent evt) {
                switch (evt.getKeyCode()) {
                    case KeyEvent.VK_ENTER: {
                        if (searchField.getText().strip().isEmpty()) return;

                        // clear previous results
                        dtm.getDataVector().removeAllElements();
                        dtm.fireTableDataChanged();

                        new Thread(() -> {
                            try {
                                EventQueue.invokeLater(() -> {
                                    searchField.setEnabled(false);
                                    resultLabel.setText("Occurences: collecting...");
                                    timeLabel.setText("Time: collecting...");
                                    scannedLabel.setText("Files scanned: collecting...");
                                });

                                final long start =  System.nanoTime() / 1000000;
                                abort = false;
                                if (multiThreadCheckBox.isSelected()) {
                                    seekMultiThreaded(progressBar, resultLabel, scannedLabel, resultTable, dirField.getText(), fileField.getText(), searchField.getText());
                                } else {
                                    seekSingleThreaded(progressBar, resultLabel, scannedLabel, resultTable, dirField.getText(), fileField.getText(), searchField.getText());
                                }
                                final long end = System.nanoTime() / 1000000;
                                final long took = end - start;

                                EventQueue.invokeLater(() -> {
                                    //resultLabel.setText("Occurences: " + occurences);
                                    timeLabel.setText("Time: " + took + " ms");
                                    searchField.setEnabled(true);
                                    progressBar.setValue(0);
                                });
                            } catch (final Throwable ex) {
                                ex.printStackTrace(System.err);
                                EventQueue.invokeLater(() -> {
                                    searchField.setEnabled(true);
                                    resultLabel.setText("Occurences: -1");
                                    timeLabel.setText("Time: -1");
                                    scannedLabel.setText("Files scanned: -1");
                                    progressBar.setIndeterminate(false);
                                    progressBar.setValue(0);
                                });
                            } finally {
                                abort = false;
                                // flush display buffer to make sure the user is seeing all the updates
                                Toolkit.getDefaultToolkit().sync();
                            }
                        }).start();
                    } break;
                }
            }
        });
        labelInputPanel.add(dirFieldLabel);
        labelInputPanel.add(fileFieldLabel);
        labelInputPanel.add(searchFieldLabel);

        inputPanel.add(labelInputPanel, BorderLayout.WEST);
        inputPanel.add(fieldInputPanel, BorderLayout.CENTER);

        final JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(mainColor);
        root.add(inputPanel,  BorderLayout.NORTH);
        root.add(resultPanel, BorderLayout.CENTER);
        root.add(progressBar, BorderLayout.SOUTH);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
            @Override
            public boolean dispatchKeyEvent(final KeyEvent evt) {
                if ((evt.getKeyCode() == KeyEvent.VK_C) && ((evt.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)) {
                    abort = true;
                }

                /*if (evt.getKeyCode() == KeyEvent.VK_TAB) {
                    searchField.requestFocus();
                }*/

                return false;
            }
        });

        final JFrame frame = new JFrame("Seeker v0.1.0");
        frame.setIconImage(new ImageIcon("res/icon.png").getImage());
        frame.setContentPane(root);
        frame.setJMenuBar(menuBar);
        frame.setSize(screenSize.width / 2, screenSize.height / 2); // TODO(nschultz): Clamp, so we do not go below a certain value
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static String[] readLines(final String name) {
        assert name != null;

        final File file = new File(name);
        final StringBuilder sbuffer = new StringBuilder(4096);
        try (final InputStream in = new FileInputStream(file)) {
            for (;;) {
                final byte[] chunk = new byte[4096]; // likely a page
                final int readBytes = in.read(chunk);
                if (readBytes == -1) return sbuffer.toString().split("\n");

                sbuffer.append(new String(chunk, 0, readBytes));
            }
        }  catch (final IOException eX) {
            return null;
        }
    }

    private static void listFiles(final String start, final String filter, final ArrayList<String> files) {
        assert start  != null;
        assert filter != null;
        assert files  != null;

        final File directory = new File(start);
        final File[] list = directory.listFiles();
        if (list != null) {
            for (final File file : list) {
                if (file.isFile()) {
                    if (file.length() >= 100000000) { // 100 MB
                        // TODO(nschultz): HACK!
                        // File is to large for us to read into memory.
                        // This is of course a hack and is just here to avoid OutOfMemory error to crash the application.
                        System.err.printf("Skipped '%s' because it is to large!\n", file.getName());
                        continue;
                    }
                    if (file.getName().matches(filter)) {
                        files.add(file.getAbsolutePath());
                    }
                } else if (file.isDirectory()) {
                    listFiles(file.getAbsolutePath(), filter, files);
                }
            }
        }
    }

    private static ArrayList<String> listRootDirectories(final String location) {
        assert location != null;

        final ArrayList<String> dirNames = new ArrayList<>();
        final File[] files = new File(location).listFiles();
        for (final File file : files) {
            if (file.isDirectory()) {
                dirNames.add(file.getAbsolutePath());
            }
        }
        return dirNames;
    }

    private static volatile boolean abort = false;

    private static void seekSingleThreaded(final JProgressBar progressBar, final JLabel resultLabel,
                                           final JLabel scannedLabel, final JTable table,
                                           final String directory, final String file,
                                           final String searchString) {

        assert !EventQueue.isDispatchThread();

        assert resultLabel  != null;
        assert scannedLabel != null;
        assert progressBar  != null;
        assert table        != null;
        assert directory    != null;
        assert file         != null;
        assert searchString != null;

        final ArrayList<String> fileNames = new ArrayList<>(100); {
            // TODO(nschultz): Event thread?????
            progressBar.setIndeterminate(true);
            listFiles(directory, file, fileNames);
            progressBar.setMaximum(fileNames.size());
            progressBar.setIndeterminate(false);
            progressBar.setValue(0);
        }

        int[] occurences = {0};
        int[] scanned    = {1};
        for (final String fileName : fileNames) {
            if (abort) return;

            final String[] lines = readLines(fileName);
            if (lines != null) {
                final int[] lineNr = {0};
                for (final String line : lines) {
                    if (abort) return;

                    lineNr[0] += 1;
                    if (line.contains(searchString)) {
                        occurences[0] += 1;
                        // :lineNrConst:
                        // We have to use a separate variable, otherwise we would get inconsistent results,
                        // because invokeLater() might take a while and referenceig 'lineNr[0]' directly might then
                        // return a different index (often the last line number in the file).
                        final int lineNrConst = lineNr[0];
                        EventQueue.invokeLater(() -> {
                            final DefaultTableModel dtm = (DefaultTableModel) table.getModel();
                            dtm.addRow(new Object[] {fileName, lineNrConst, line.strip()});
                            dtm.fireTableDataChanged();
                            table.repaint();
                            resultLabel.setText("Occurences: " + occurences[0]);
                        });
                    }
                }
            } else {
                // TODO(nschultz): Handle this case!
                // System.err.println("Failed to read: " + fileName);
            }

            EventQueue.invokeLater(() -> { // invokeAndWait for multi threads (?)
                progressBar.setValue(progressBar.getValue() + 1);
            });

            scanned[0] += 1;
            EventQueue.invokeLater(() -> {
                scannedLabel.setText("Files scanned: " + scanned[0]);
            });
        }
    }

    private static ThreadPoolExecutor createThreadPool(final int amount) {
        assert amount > 0;

        final ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(amount);
        threadPool.setThreadFactory(new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable runnable) {
                final Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                return thread;
            }
        });
        return threadPool;
    }

    public static List<List<Object>> slice(final Object[] source, final int chunks) {
        assert source != null;
        assert source.length >= chunks;

        final List<List<Object>> slices = new ArrayList<>(chunks * 2); {
            int chunkCounter = 0;
            List<Object> slice = new ArrayList<>(chunks);
            for (int i = 0; i < source.length; ++i) {
                slice.add(source[i]);
                chunkCounter += 1;
                if (chunkCounter == chunks) {
                    chunkCounter = 0;
                    slices.add(slice);
                    slice = new ArrayList<>();
                }
            }

            final int remainder = source.length - (slices.size() * chunks);
            if (remainder > 0) {
                final Object[] remainderArray = new Object[remainder];
                int idx = 0;
                for (int i = source.length - remainder; i < source.length; ++i) {
                    remainderArray[idx++] = source[i];
                }
                slices.add(Arrays.asList(remainderArray));
            }
        }
        return slices;
    }


    private static void seekMultiThreaded(final JProgressBar progressBar, final JLabel resultLabel,
                                         final JLabel scannedLabel, final JTable table,
                                         final String directory, final String file,
                                         final String searchString) {

        assert !EventQueue.isDispatchThread();

        assert resultLabel  != null;
        assert scannedLabel != null;
        assert progressBar  != null;
        assert table        != null;
        assert directory    != null;
        assert file         != null;
        assert searchString != null;

        final int cores = Runtime.getRuntime().availableProcessors();

        final ArrayList<String> fileNames = new ArrayList<>(100);

        progressBar.setIndeterminate(true);
        final ArrayList<String> dirs = listRootDirectories(directory);
        if (dirs.size() >= 2) {
            // TODO(nschultz): use thread pooling instead
            final List<String> dirs1 = dirs.subList(0, dirs.size() / 2);
            final List<String> dirs2 = dirs.subList(dirs.size() / 2, dirs.size());

            final ArrayList<String> collection1 = new ArrayList<>();
            final Thread t1 = new Thread(() -> {
                for (final String dir : dirs1) {
                    listFiles(dir, file, collection1);
                }
            });
            t1.setDaemon(true);
            t1.start();

            final ArrayList<String> collection2 = new ArrayList<>();
            final Thread t2 = new Thread(() -> {
                for (final String dir : dirs2) {
                    listFiles(dir, file, collection2);
                }
            });
            t2.setDaemon(true);
            t2.start();

            try {
                t1.join();
                t2.join();
            } catch (final InterruptedException ex) {
                assert false;
            }
            fileNames.addAll(collection1);
            fileNames.addAll(collection2);
        } else {
            listFiles(directory, file, fileNames);
        }

        progressBar.setMaximum(fileNames.size());
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        if (fileNames.size() > cores * 2) {
            final List<List<Object>> slices = slice(fileNames.toArray(), cores);
            final ThreadPoolExecutor threadPool = createThreadPool(cores);

            final AtomicInteger occurences = new AtomicInteger(0);
            final AtomicInteger scanned    = new AtomicInteger(1);
            final ArrayList<Future> futures = new ArrayList<>();
            for (final List<Object> slice : slices) {
                final Future<?> future = threadPool.submit(() -> {
                    for (final Object o : slice) {
                        final String fileName = (String) o;

                        final String[] lines = readLines(fileName);
                        if (lines != null) {
                            final AtomicInteger lineNr = new AtomicInteger(0);
                            for (final String line : lines) {

                                lineNr.incrementAndGet();
                                if (line.contains(searchString)) { // use matches()?
                                    occurences.incrementAndGet();
                                    final int lineNrConst = lineNr.get(); // :lineNrConst:
                                    EventQueue.invokeLater(() -> {
                                        final DefaultTableModel dtm = (DefaultTableModel) table.getModel();
                                        dtm.addRow(new Object[] {fileName, lineNrConst, line.strip()});
                                        dtm.fireTableDataChanged();
                                        table.repaint();
                                        resultLabel.setText("Occurences: " + occurences.get());
                                    });
                                }
                            }
                        } else {
                            // TODO(nschultz): Handle this case!
                            // System.err.println("Failed to read: " + fileName);
                        }
                        EventQueue.invokeLater(() -> { // TODO(nschultz): NOT THREAD SAFE!
                            progressBar.setValue(progressBar.getValue() + 1);
                        });
                        scanned.incrementAndGet();
                        EventQueue.invokeLater(() -> {
                            scannedLabel.setText("Files scanned: " + scanned.get());
                        });
                    }
                });
                futures.add(future);
            }

            // block until all tasks are completed
            for (final Future<?> future : futures) {
                try {
                    future.get();
                }  catch (final ExecutionException ex) {
                    ex.printStackTrace(System.err); // TODO(nschultz): Temporary
                } catch (final InterruptedException ex) {
                    assert false : "Not supposed to interrupt this!";
                }
            }

            threadPool.shutdown(); // TODO(nschultz): We don't want to shutdown, but instead reuse
        } else {
            // TODO(nschultz): Count files twice in this case!!!
            seekSingleThreaded(progressBar, resultLabel, scannedLabel, table, directory, file, searchString);
        }
    }

    public static void main(final String[] args) {
        EventQueue.invokeLater(() -> {
            applySystemLookAndFeel();
            constructUI();
        });
    }
}
