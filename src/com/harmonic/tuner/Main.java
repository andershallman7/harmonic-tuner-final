package com.harmonic.tuner;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Main {
    private JFrame frame;
    private JComboBox<Mixer.Info> mixerBox;
    private JTextField targetField;
    private JButton startButton;
    private JLabel detectedLabel;
    private JLabel noteLabel;
    private JLabel centsLabel;
    private JSlider centsSlider;

    private volatile boolean running = false;
    private Thread captureThread;

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 4096; // samples
    private static final int SMOOTHING_WINDOW_MS = 4000; // 4 second window for averaging
    private final java.util.Queue<Double> freqBuffer = new java.util.LinkedList<>();
    private long lastUpdateTime = 0;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().createAndShowGui());
    }

    private void createAndShowGui() {
        frame = new JFrame("Harmonic Tuner");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 300);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
        top.add(new JLabel("Microphone:"), c);

        mixerBox = new JComboBox<>();
        populateMixers();
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        top.add(mixerBox, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        top.add(new JLabel("Target Hz:"), c);
        targetField = new JTextField("440", 8);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        top.add(targetField, c);

        startButton = new JButton("Start");
        startButton.addActionListener(e -> onStartStop());
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.fill = GridBagConstraints.NONE;
        top.add(startButton, c);

        root.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        JPanel topInfo = new JPanel(new GridLayout(2, 1, 4, 4));
        
        detectedLabel = new JLabel("Detected: --- Hz");
        detectedLabel.setFont(detectedLabel.getFont().deriveFont(18f));
        topInfo.add(detectedLabel);
        
        noteLabel = new JLabel("Note: ---");
        noteLabel.setFont(noteLabel.getFont().deriveFont(24f));
        noteLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topInfo.add(noteLabel);
        
        center.add(topInfo, BorderLayout.NORTH);

        centsSlider = new JSlider(-100, 100, 0);
        centsSlider.setMajorTickSpacing(50);
        centsSlider.setPaintTicks(true);
        centsSlider.setPaintLabels(true);
        centsSlider.setEnabled(false);
        center.add(centsSlider, BorderLayout.CENTER);

        centsLabel = new JLabel("Cents: ---");
        center.add(centsLabel, BorderLayout.SOUTH);

        root.add(center, BorderLayout.CENTER);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private void populateMixers() {
        Mixer.Info[] infos = AudioSystem.getMixerInfo();
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info lineInfo = new Line.Info(TargetDataLine.class);
            if (mixer.isLineSupported(lineInfo)) {
                mixerBox.addItem(info);
            }
        }
        if (mixerBox.getItemCount() == 0) {
            mixerBox.addItem(null);
        }
        mixerBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = (value == null) ? "(no microphone)" : ((Mixer.Info) value).getName();
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
    }

    private void onStartStop() {
        if (!running) {
            double targetHz;
            try {
                targetHz = Double.parseDouble(targetField.getText().trim());
                if (targetHz <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Enter a valid positive target frequency (e.g., 440)", "Invalid input", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Mixer.Info sel = (Mixer.Info) mixerBox.getSelectedItem();
            startCapture(sel, targetHz);
            startButton.setText("Stop");
        } else {
            stopCapture();
            startButton.setText("Start");
        }
    }

    private void startCapture(Mixer.Info mixerInfo, double targetHz) {
        running = true;
        centsSlider.setEnabled(true);

        captureThread = new Thread(() -> {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            TargetDataLine line = null;
            try {
                if (mixerInfo != null) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    line = (TargetDataLine) mixer.getLine(info);
                } else {
                    line = (TargetDataLine) AudioSystem.getLine(info);
                }
                if (line == null) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Unable to open microphone: no line available", "Audio Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }
                try {
                    line.open(format, BUFFER_SIZE * 2);
                } catch (LineUnavailableException ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Unable to open microphone: " + ex.getMessage(), "Audio Error", JOptionPane.ERROR_MESSAGE));
                    return;
                }
                line.start();

                byte[] buffer = new byte[BUFFER_SIZE * 2]; // 16-bit -> 2 bytes/sample
                float[] samples = new float[BUFFER_SIZE];

                int iteration = 0;
                while (running) {
                    int read = line.read(buffer, 0, buffer.length);
                    if (read <= 0) continue;
                    int samplesRead = read / 2;
                    // convert
                    ByteBuffer bb = ByteBuffer.wrap(buffer);
                    bb.order(ByteOrder.LITTLE_ENDIAN);
                    for (int i = 0; i < samplesRead; i++) {
                        short s = bb.getShort(i * 2);
                        samples[i] = s / 32768f;
                    }

                    double freq = PitchDetector.detect(samples, SAMPLE_RATE);
                    
                    // Add frequency to buffer and remove old readings
                    long currentTime = System.currentTimeMillis();
                    if (freq > 0) {
                        freqBuffer.offer(freq);
                    }
                    
                    // Remove readings older than SMOOTHING_WINDOW_MS
                    while (!freqBuffer.isEmpty() && currentTime - lastUpdateTime > SMOOTHING_WINDOW_MS) {
                        freqBuffer.poll();
                    }
                    
                    // Calculate averaged frequency
                    double avgFreq = 0;
                    if (!freqBuffer.isEmpty()) {
                        avgFreq = freqBuffer.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    }
                    
                    // Only update UI once per millisecond
                    if (currentTime - lastUpdateTime >= 1) {
                        lastUpdateTime = currentTime;
                        
                        final String detectedText;
                        final String centsText;
                        final int centsVal;
                        if (avgFreq <= 0) {
                            detectedText = "Detected: --- Hz";
                            centsText = "Cents: ---";
                            centsVal = 0;
                        } else {
                            detectedText = String.format("Detected: %.2f Hz", avgFreq);
                            double cents = 1200.0 * (Math.log(avgFreq / targetHz) / Math.log(2));
                            centsText = String.format("Cents: %.1f", cents);
                            centsVal = (int) Math.max(-100, Math.min(100, Math.round(cents)));
                        }

                        // Calculate musical note
                        String noteName = "---";
                        if (avgFreq > 0) {
                            // A4 is 440Hz, each semitone is 2^(1/12)
                            double a4 = 440.0;
                            double semitones = 12 * Math.log(avgFreq / a4) / Math.log(2);
                            int midiNote = (int) Math.round(69 + semitones); // 69 is A4 in MIDI
                            noteName = getNoteNameFromMidi(midiNote);
                        }
                        final String noteText = "Note: " + noteName;

                        SwingUtilities.invokeLater(() -> {
                            detectedLabel.setText(detectedText);
                            noteLabel.setText(noteText);
                            centsLabel.setText(centsText);
                            centsSlider.setValue(centsVal);
                        });
                    }

                    // Process every few iterations to avoid UI flooding
                    if (++iteration % 3 == 0) {
                        // UI update code would go here
                    }
                }

            } catch (LineUnavailableException ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(frame, "Unable to open microphone: " + ex.getMessage(), "Audio Error", JOptionPane.ERROR_MESSAGE));
            } finally {
                if (line != null) {
                    line.stop();
                    line.close();
                }
                running = false;
                SwingUtilities.invokeLater(() -> {
                    startButton.setText("Start");
                    centsSlider.setEnabled(false);
                });
            }
        }, "Audio-Capture-Thread");
        captureThread.setDaemon(true);
        captureThread.start();
    }

    private void stopCapture() {
        running = false;
        if (captureThread != null) {
            try { 
                captureThread.join(200); 
            } catch (InterruptedException ignored) {}
            captureThread = null;
        }
        // Clear the frequency buffer
        freqBuffer.clear();
        lastUpdateTime = 0;
    }

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    
    private String getNoteNameFromMidi(int midiNote) {
        int note = midiNote % 12;
        int octave = (midiNote / 12) - 1;
        return NOTE_NAMES[note] + octave;
    }
}
