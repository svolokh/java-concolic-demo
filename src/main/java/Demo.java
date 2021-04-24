import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

public class Demo extends JPanel {
    private static String dockerContainerId = null;

    private Thread readerThread = null;
    private final LinkedList<String> lineQueue = new LinkedList<>();
    private JTextArea ta;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdownDockerContainer();
            }
        }));
    }

    public static void shutdownDockerContainer() {
        if (dockerContainerId != null) {
            try {
                Runtime.getRuntime().exec("docker rm -f " + dockerContainerId);
            } catch (IOException e) {
                e.printStackTrace();
            }
            dockerContainerId = null;
        }
    }

    private void updateTextArea() {
        synchronized(lineQueue) {
            StringBuilder text = new StringBuilder(ta.getText());
            while (!lineQueue.isEmpty()) {
                String line = lineQueue.removeFirst();
                text.append(line.replace("Crashed? Yes", "Crashed? YES"));
                text.append("\n");
            }
            ta.setText(text.toString());
        }
    }

    public Demo() {
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();

        setLayout(gridbag);

        c.fill = GridBagConstraints.BOTH;

        c.weighty = 2.0;
        c.insets = new Insets(5, 5, 5, 5);

        c.gridwidth = 1;

        RSyntaxTextArea textArea = new RSyntaxTextArea(30, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);

        c.weightx = 2.0;
        gridbag.setConstraints(sp, c);
        add(sp);

        c.gridwidth = GridBagConstraints.REMAINDER;

        c.weightx = 1.0;
        ta = new JTextArea(30, 20);
        ta.setLineWrap(true);
        ta.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(ta);
        gridbag.setConstraints(scrollPane, c);
        add(scrollPane);

        c.gridwidth = 1;

        c.weighty = 0.5;

        c.weightx = 2.0;
        JPanel jp = new JPanel();
        jp.setLayout(new FlowLayout(FlowLayout.LEADING));
        JComboBox<String> sampleSelector = new JComboBox<>();
        ArrayList<String> samples = new ArrayList<String>(Arrays.asList(new File("samples").list()));
        samples.sort(String::compareTo);
        for (String sample : samples) {
            sampleSelector.addItem(sample);
        }
        sampleSelector.setSelectedItem("dart2");
        ActionListener l = actionEvent -> {
            try (BufferedReader br = new BufferedReader(new FileReader(new File("samples", (String)sampleSelector.getSelectedItem()))))
            {
                StringBuilder sb = new StringBuilder();
                while (br.ready()) {
                    String line = br.readLine();
                    sb.append(line);
                    sb.append("\n");
                }
                textArea.setText(sb.toString());
                textArea.setCaretPosition(0);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        };
        sampleSelector.addActionListener(l);
        l.actionPerformed(null);
        jp.add(sampleSelector);

        gridbag.setConstraints(jp, c);
        add(jp);

        c.gridwidth = GridBagConstraints.REMAINDER;

        c.weightx = 1.0;
        JButton b = new JButton("Test my code!");
        gridbag.setConstraints(b, c);
        add(b);

        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (readerThread != null && readerThread.isAlive()) {
                    shutdownDockerContainer();
                    readerThread.interrupt();
                    ta.setText("");
                    readerThread = null;
                    return;
                }

                ta.setText("");

                try {
                    File f = Files.createTempDirectory("java-concolic-demo").toFile();
                    f.deleteOnExit();

                    File code = new File(f, "Main.java");
                    try (FileWriter fw = new FileWriter(code))
                    {
                        fw.write(textArea.getText());
                    }

                    String cidfile = new File(f, "cid.txt").getAbsolutePath();
                    Process p = Runtime.getRuntime().exec("docker run --cidfile " + cidfile + " -v " + f.getAbsolutePath() + ":/root/demo-code --network=none -m 1g java-concolic-demo");
                    System.out.println("docker run --cidfile " + cidfile + " -v " + f.getAbsolutePath() + ":/root/demo-code --network=none -m 1g java-concolic-demo");
                    b.setEnabled(false);
                    readerThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            InputStream is = p.getInputStream();
                            BufferedReader br = new BufferedReader(new InputStreamReader(is));
                            try {
                                for (;;) {
                                    String line = br.readLine();
                                    if (line == null) {
                                        break;
                                    }
                                    if (dockerContainerId == null) {
                                        try (BufferedReader r = new BufferedReader(new FileReader(new File(cidfile))))
                                        {
                                            dockerContainerId = r.readLine();
                                            SwingUtilities.invokeLater(() -> {
                                                b.setEnabled(true);
                                                b.setText("Cancel");
                                            });
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            break;
                                        }
                                    }

                                    synchronized (lineQueue) {
                                        lineQueue.add(line);
                                    }
                                    SwingUtilities.invokeLater(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateTextArea();
                                        }
                                    });
                                    Thread.sleep(1);
                                }
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                                shutdownDockerContainer();
                            }
                            shutdownDockerContainer();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    b.setEnabled(true);
                                    b.setText("Test my code!");
                                }
                            });
                        }
                    });
                    readerThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame();
                frame.setTitle("java-concolic demo");
                frame.setContentPane(new Demo());
                frame.setSize(1280, 720);
                frame.setVisible(true);
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            }
        });
    }
}
