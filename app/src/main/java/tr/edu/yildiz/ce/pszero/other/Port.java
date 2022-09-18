package tr.edu.yildiz.ce.pszero.other;

public class Port {
    private final int number;
    private final State state;
    private final String service;

    public Port(int number, State state, String service) {
        this.number = number;
        this.state = state;
        this.service = service;
    }

    public int getNumber() {
        return number;
    }

    public State getState() {
        return state;
    }

    public String getService() {
        return service;
    }
}
