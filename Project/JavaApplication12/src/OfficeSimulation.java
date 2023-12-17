import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OfficeSimulation {
    private static Semaphore[] taSemaphores;
    private static Semaphore studentSemaphore = new Semaphore(0);
    private static Semaphore mutex = new Semaphore(1);
    private static List<Integer> waitingStudents = new ArrayList<>();
    private static Lock lock = new ReentrantLock();
    private static List<Thread> taThreads = new ArrayList<>();
    private static List<Thread> studentThreads = new ArrayList<>();

    private JTextArea outputArea;
    private JLabel workingThreadsLabel;
    private JLabel sleepingThreadsLabel;
    private JLabel waitingStudentsLabel;

    public OfficeSimulation(int numTAs, int numStudents, int numWaitingChairs,
                            JTextArea outputArea, JLabel workingThreadsLabel,
                            JLabel sleepingThreadsLabel, JLabel waitingStudentsLabel) {
        this.outputArea = outputArea;
        this.workingThreadsLabel = workingThreadsLabel;
        this.sleepingThreadsLabel = sleepingThreadsLabel;
        this.waitingStudentsLabel = waitingStudentsLabel;

        // Initialize TA semaphores
        taSemaphores = new Semaphore[numTAs];
        for (int i = 0; i < numTAs; i++) {
            taSemaphores[i] = new Semaphore(0);
        }

        // Create threads for each TA
        for (int i = 0; i < numTAs; i++) {
            final int taId = i;
            taThreads.add(new Thread(() -> ta(taId, this)));
        }

        // Create threads for each student
        for (int i = 0; i < numStudents; i++) {
            final int studentIndex = i;
            studentThreads.add(new Thread(() -> student(studentIndex, numWaitingChairs, this)));
        }
    }

    public void start() {
        taThreads.forEach(Thread::start);
        studentThreads.forEach(Thread::start);
    }

    public void stopSimulation() {
        taThreads.forEach(Thread::interrupt);
        studentThreads.forEach(Thread::interrupt);
    }

    public void updateLabels() {
        SwingUtilities.invokeLater(() -> {
            workingThreadsLabel.setText(String.valueOf(countWorkingThreads()));
            sleepingThreadsLabel.setText(String.valueOf(countSleepingThreads()));
            waitingStudentsLabel.setText(String.valueOf(countWaitingStudents()));
        });
    }

    private static int countWorkingThreads() {
        int count = 0;
        for (Thread thread : taThreads) {
            if (thread.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private static int countSleepingThreads() {
        int count = 0;
        for (Thread thread : taThreads) {
            if (!thread.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private static int countWaitingStudents() {
        return waitingStudents.size();
    }

    private static void ta(int taId, OfficeSimulation simulation) {
        while (!Thread.interrupted()) {
            try {
                simulation.outputArea.append("TA " + taId + " is sleeping...\n");
                taSemaphores[taId].acquire();
                lock.lock();
                if (!waitingStudents.isEmpty()) {
                    int student = waitingStudents.remove(0);
                    simulation.outputArea.append("TA " + taId + " is helping student " + student + "\n");
                    lock.unlock();
                    Thread.sleep(new Random().nextInt(200) + 100); // Simulate helping time
                    simulation.outputArea.append("TA " + taId + " has finished helping student " + student + "\n");
                    studentSemaphore.release();
                } else {
                    lock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void student(int index, int numWaitingChairs, OfficeSimulation simulation) {
        try {
            while (!Thread.interrupted()) {
                Thread.sleep(new Random().nextInt(200) + 100); // Simulate time before entering the shop
                lock.lock();
                if (waitingStudents.size() < numWaitingChairs) {
                    waitingStudents.add(index);
                    simulation.outputArea.append("Student " + index + " is waiting for help\n");
                    lock.unlock();
                    taSemaphores[index % taSemaphores.length].release();
                    studentSemaphore.acquire();
                    simulation.outputArea.append("Student " + index + " has finished getting help\n");
                    simulation.updateLabels();  // Update labels when a student finishes getting help
                    break; // Break out of the loop if helped successfully
                } else {
                    lock.unlock();
                    simulation.outputArea.append("Student " + index + " is leaving because the waiting room is full\n");
                    Thread.sleep(new Random().nextInt(2000) + 1000); // Simulate time before returning
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
