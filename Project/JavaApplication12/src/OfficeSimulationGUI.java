import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class OfficeSimulationGUI extends JFrame {

    private final JTextField numTAsField;
    private final JTextField numStudentsField;
    private final JTextField numWaitingChairsField;
    private final JButton startButton;
    private final JButton stopButton;
    private final JTextArea outputArea;
    private final JLabel workingThreadsLabel;
    private final JLabel sleepingThreadsLabel;
    private final JLabel waitingStudentsLabel;
    private OfficeSimulation officeSimulation;
    private SimulationThread simulationThread;

    public OfficeSimulationGUI() {
        super("Office Simulation");

        // Initialize components
        numTAsField = new JTextField(5);
        numStudentsField = new JTextField(5);
        numWaitingChairsField = new JTextField(5);
        startButton = new JButton("Start Simulation");
        stopButton = new JButton("Stop Simulation");
        outputArea = new JTextArea(20, 40);
        workingThreadsLabel = new JLabel("0");
        sleepingThreadsLabel = new JLabel("0");
        waitingStudentsLabel = new JLabel("0");

        // Set layout
        setLayout(new BorderLayout());
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Num TAs:"));
        inputPanel.add(numTAsField);
        inputPanel.add(new JLabel("Num Students:"));
        inputPanel.add(numStudentsField);
        inputPanel.add(new JLabel("Num Waiting Chairs:"));
        inputPanel.add(numWaitingChairsField);
        inputPanel.add(startButton);
        inputPanel.add(stopButton);

        JPanel labelPanel = new JPanel(new GridLayout(3, 2));
        labelPanel.add(new JLabel("Working Threads:"));
        labelPanel.add(workingThreadsLabel);
        labelPanel.add(new JLabel("Sleeping Threads:"));
        labelPanel.add(sleepingThreadsLabel);
        labelPanel.add(new JLabel("Waiting Students:"));
        labelPanel.add(waitingStudentsLabel);

        // Add components to the frame
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        add(labelPanel, BorderLayout.SOUTH);

        // Redirect System.out to JTextArea
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);

        // Set up action listeners
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSimulation();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSimulation();
            }
        });

        // Set up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);  // Center the frame on the screen
        setVisible(true);
    }

    private void startSimulation() {
        try {
            int numTAs = Integer.parseInt(numTAsField.getText());
            int numStudents = Integer.parseInt(numStudentsField.getText());
            int numWaitingChairs = Integer.parseInt(numWaitingChairsField.getText());

            if (simulationThread != null && simulationThread.isRunning()) {
                // If a simulation is already running, stop it first
                simulationThread.stopSimulation();
                try {
                    simulationThread.join();  // Wait for the thread to finish
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    // Handle the InterruptedException as needed
                }
            }

            outputArea.setText("");
            officeSimulation = new OfficeSimulation(numTAs, numStudents, numWaitingChairs,
                    outputArea, workingThreadsLabel, sleepingThreadsLabel, waitingStudentsLabel);
            simulationThread = new SimulationThread(officeSimulation);
            simulationThread.start();
        } catch (NumberFormatException ex) {
            // Handle the exception (e.g., show an error message)
            ex.printStackTrace(); // You might want to log the exception or display an error message.
        }
    }

    private void stopSimulation() {
        if (simulationThread != null && simulationThread.isRunning()) {
            simulationThread.stopSimulation();
            try {
                simulationThread.join();  // Wait for the thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
                // Handle the InterruptedException as needed
            }
        }
    }

    // Custom OutputStream to redirect output to JTextArea
    private static class CustomOutputStream extends OutputStream {
        private final JTextArea textArea;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            textArea.append(String.valueOf((char) b));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    private static class SimulationThread extends Thread {
        private final OfficeSimulation officeSimulation;
        private volatile boolean running;

        public SimulationThread(OfficeSimulation officeSimulation) {
            this.officeSimulation = officeSimulation;
            this.running = false;
        }

        @Override
        public void run() {
            running = true;
            officeSimulation.start();
            while (running) {
                try {
                    officeSimulation.updateLabels();
                    Thread.sleep(500);  // Adjust the update frequency as needed
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void stopSimulation() {
            if (officeSimulation != null) {
                officeSimulation.stopSimulation();
            }
            running = false;
        }

        public boolean isRunning() {
            return running;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OfficeSimulationGUI());
    }
}
