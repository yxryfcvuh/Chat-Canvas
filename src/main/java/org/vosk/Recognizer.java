package org.vosk;
public class Recognizer implements AutoCloseable {
    public Recognizer(Model model, float sampleRate) {}
    public boolean acceptWaveForm(byte[] data, int len) { return false; }
    public String getResult() { return ""; }
    public String getPartialResult() { return ""; }
    public String getFinalResult() { return ""; }
    public void close() {}
}
