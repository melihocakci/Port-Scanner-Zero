package tr.edu.yildiz.ce.pszero.other;

import androidx.annotation.NonNull;

public enum State {
    OPEN("open"),
    CLOSED("closed"),
    FILTERED("filtered");

    String str;

    State(String str) {
        this.str = str;
    }

    @NonNull
    @Override
    public String toString() {
        return str;
    }
}
