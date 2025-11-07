public class PitchDetector {
    private final int sampleRate;
    public PitchDetector(int sampleRate) {
        this.sampleRate = sampleRate;
    }
    public double detectPitch(float[] samples) {
        int n = samples.length;
        double maxCorr = 0;
        int bestLag = -1;
        for (int lag = 20; lag < n / 2; lag++) {
            double corr = 0;
            for (int i = 0; i < n - lag; i++) {
                corr += samples[i] * samples[i + lag];
            }
            if (corr > maxCorr) {
                maxCorr = corr;
                bestLag = lag;
            }
        }
        if (bestLag > 0) {
            return (double) sampleRate / bestLag;
        }
        return -1;
    }
}
