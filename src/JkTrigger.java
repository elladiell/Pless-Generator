public class JkTrigger {
    boolean j, k, state;

    public void setJ(boolean j) {
        this.j = j;
        updateState();
    }

    public void setK(boolean k) {
        this.k = k;
        updateState();
    }

    /**
     * @see https://ru.wikipedia.org/wiki/%D0%A2%D1%80%D0%B8%D0%B3%D0%B3%D0%B5%D1%80#JK-.D1.82.D1.80.D0.B8.D0.B3.D0.B3.D0.B5.D1.80
     */
    private void updateState() {
        state = (!state && j) || (state && !k);
    }

    public boolean getState() {
        return state;
    }
    public int getStateAsInt() {
        return state ? 1 : 0;
    }
}
