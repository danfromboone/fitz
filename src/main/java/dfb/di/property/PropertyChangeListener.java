package dfb.di.property;

@FunctionalInterface
public interface PropertyChangeListener {
    void onPropertyChange(PropertyChangeEvent event);
}
