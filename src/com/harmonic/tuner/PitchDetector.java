package com.harmonic.tuner;

public final class PitchDetector {
    private PitchDetector() {}

    /**
     * Estimate fundamental frequency using a basic autocorrelation method.
     * @param audio mono float samples in range [-1,1]
     * @param sampleRate sample rate in Hz (e.g., 44100)
     * @return estimated frequency in Hz, or -1 if not found
     */
    public static double detect(float[] audio, int sampleRate) {
        int n = audio.length;
        if (n == 0) return -1;

        // Autocorrelation
        int maxLag = Math.min(n - 1, sampleRate / 50); // lowest freq 50 Hz
        int minLag = Math.max(1, sampleRate / 2000); // highest freq ~2000 Hz

        double bestCorr = Double.NEGATIVE_INFINITY;
        int bestLag = -1;

        // Pre-calc energy for normalization
        double energy = 0;
        for (int i = 0; i < n; i++) energy += audio[i] * audio[i];
        if (energy <= 1e-8) return -1;

        for (int lag = minLag; lag <= maxLag; lag++) {
            double corr = 0;
            for (int i = 0; i + lag < n; i++) {
                corr += audio[i] * audio[i + lag];
            }
            // normalize by energy
            corr /= Math.sqrt(energy * energy);
            if (corr > bestCorr) {
                bestCorr = corr;
                bestLag = lag;
            }
        }

        if (bestLag <= 0) return -1;

        // Parabolic interpolation around bestLag for sub-sample precision
        double refinedLag = bestLag;
        if (bestLag > minLag && bestLag < maxLag) {
            double c0 = acorr(audio, n, bestLag - 1);
            double c1 = acorr(audio, n, bestLag);
            double c2 = acorr(audio, n, bestLag + 1);
            double denom = (c0 - 2 * c1 + c2);
            if (Math.abs(denom) > 1e-12) {
                double delta = 0.5 * (c0 - c2) / denom;
                refinedLag = bestLag + delta;
            }
        }

        double freq = sampleRate / refinedLag;
        if (freq <= 0 || freq > sampleRate / 2.0) return -1;
        return freq;
    }

    // helper to compute autocorrelation value at specific lag (normalized similar to above)
    private static double acorr(float[] audio, int n, int lag) {
        if (lag < 1 || lag >= n) return 0;
        double sum = 0;
        for (int i = 0; i + lag < n; i++) sum += audio[i] * audio[i + lag];
        return sum;
    }
}
