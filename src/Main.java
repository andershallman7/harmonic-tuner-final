import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
// Import PitchDetector


public class Main {
    private final JFrame frame;
    private final JComboBox<String> micDropdown;
    private final JTextField targetHzField;
    private final JLabel detectedFreqLabel;
    private final JLabel centsLabel;
    private final JSlider tuningSlider;
    private final JButton startButton;
    private final JButton stopButton;
    private volatile boolean running = false;
    private TargetDataLine micLine;
    private Thread audioThread;
    private Mixer.Info[] micInfos;
    private float targetHz = 440.0f;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    public Main() {
        frame = new JFrame("Harmonic Tuner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);
        frame.setLayout(new GridLayout(5, 2));

        micDropdown = new JComboBox<>();
        targetHzField = new JTextField("440");
        detectedFreqLabel = new JLabel("Detected: -- Hz");
        centsLabel = new JLabel("Offset: -- cents");
        tuningSlider = new JSlider(-100, 100, 0);
        tuningSlider.setEnabled(false);
        tuningSlider.setMajorTickSpacing(50);
        tuningSlider.setPaintTicks(true);
        tuningSlider.setPaintLabels(true);
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        frame.add(new JLabel("Microphone:"));
        frame.add(micDropdown);
        frame.add(new JLabel("Target Hz:"));
        frame.add(targetHzField);
        frame.add(startButton);
        frame.add(stopButton);
        frame.add(detectedFreqLabel);
        frame.add(centsLabel);
        frame.add(new JLabel("Tuning:"));
        frame.add(tuningSlider);

        populateMicDropdown();

        startButton.addActionListener(e -> startTuning());
        stopButton.addActionListener(e -> stopTuning());
        targetHzField.addActionListener(e -> {
            try {
                targetHz = Float.parseFloat(targetHzField.getText());
            } catch (NumberFormatException ex) {
                targetHzField.setText("440");
                targetHz = 440.0f;
            }
        });

        frame.setVisible(true);
    }

    private void populateMicDropdown() {
        List<String> names = new ArrayList<>();
        List<Mixer.Info> infos = new ArrayList<>();
        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] lines = mixer.getTargetLineInfo();
            for (Line.Info lineInfo : lines) {
                if (TargetDataLine.class.isAssignableFrom(lineInfo.getLineClass())) {
                    names.add(info.getName());
                    infos.add(info);
                    break;
                }
            }
        }
        micInfos = infos.toArray(Mixer.Info[]::new);
        for (String name : names) micDropdown.addItem(name);
        if (!names.isEmpty()) micDropdown.setSelectedIndex(0);
    }

    private void startTuning() {
        if (running) return;
        running = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        int idx = micDropdown.getSelectedIndex();
        if (idx < 0 || idx >= micInfos.length) return;
        Mixer.Info selectedInfo = micInfos[idx];
        try {
            Mixer mixer = AudioSystem.getMixer(selectedInfo);
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            micLine = (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, format));
            micLine.open(format);
            micLine.start();
            audioThread = new Thread(() -> captureAndDetect());
            audioThread.start();
        } catch (LineUnavailableException | IllegalArgumentException ex) {
            detectedFreqLabel.setText("Error: " + ex.getMessage());
            running = false;
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    private void stopTuning() {
        running = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        if (micLine != null) {
            micLine.stop();
            micLine.close();
        }
        if (audioThread != null) {
            try { audioThread.join(500); } catch (InterruptedException ignored) {}
        }
    }

    private void captureAndDetect() {
        byte[] buffer = new byte[4096];
        float[] samples = new float[buffer.length / 2];
        PitchDetector detector = new PitchDetector(44100);
        while (running) {
            int read = micLine.read(buffer, 0, buffer.length);
            for (int i = 0; i < samples.length && i * 2 + 1 < read; i++) {
                int lo = buffer[i * 2] & 0xFF;
                int hi = buffer[i * 2 + 1];
                int val = (hi << 8) | lo;
                samples[i] = val / 32768f;
            }
            double freq = detector.detectPitch(samples);
            SwingUtilities.invokeLater(() -> updateUI(freq));
        }
    }

    private void updateUI(double freq) {
        if (freq > 0) {
            detectedFreqLabel.setText(String.format("Detected: %.2f Hz", freq));
            double cents = 1200 * Math.log(freq / targetHz) / Math.log(2);
            centsLabel.setText(String.format("Offset: %.1f cents", cents));
            // Map cents to slider (-100 to 100)
            int sliderValue = (int) Math.max(-100, Math.min(100, Math.round(cents)));
            tuningSlider.setValue(sliderValue);
        } else {
            detectedFreqLabel.setText("Detected: -- Hz");
            centsLabel.setText("Offset: -- cents");
        }
    }
}
