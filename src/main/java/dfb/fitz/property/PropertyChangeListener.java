package dfb.fitz.property;

/**
 * Listener notified when a {@link PollingPropertySource} detects one or more property changes.
 */
@FunctionalInterface
public interface PropertyChangeListener {
    /**
     * Invoked when one or more properties have been added, modified, or removed.
     *
     * @param event the event containing the list of detected {@link PropertyChange}s
     */
    void onPropertyChange(PropertyChangeEvent event);
}
