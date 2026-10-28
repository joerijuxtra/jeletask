package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;

/**
 * FNC_REGIME (14): number = regime (0 Auto, 1 Workday, 2 Weekend, 3 Simulation, 4 None, 5 Custom clock list).
 * Lenient on/off: 0 = OFF, any other value = ON, so an unexpected value from the central never breaks the event.
 */
public class RegimeStateCalculator extends OnOffToggleStateCalculator {
    @Override
    public OnOffState fromEvent(ComponentSpec component, byte[] dataBytes) {
        if (dataBytes.length == 0) {
            return null;
        }
        return new OnOffState(dataBytes[0] == 0 ? ValidOnOffToggle.OFF : ValidOnOffToggle.ON);
    }
}
