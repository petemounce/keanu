package io.improbable.keanu.vertices;

public class Observation<T> implements Observable<T> {
    // package private
    Observation() {}

    private T value = null;

    @Override
    public void observe(T value) {
        this.value = value;
    }

    @Override
    public void unobserve() {
        this.value = null;
    }

    @Override
    public boolean isObserved() {
        return this.value != null;
    }
}
