package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.state.impl.OnOffState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RegimeStateCalculatorTest {
    @Test
    void lenientOnOff() {
        RegimeStateCalculator c = new RegimeStateCalculator();
        Assertions.assertThat(c.fromEvent(null, new byte[]{0}).getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.OFF);
        Assertions.assertThat(c.fromEvent(null, new byte[]{(byte) 255}).getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        Assertions.assertThat(c.fromEvent(null, new byte[]{1}).getState()).isEqualTo(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        Assertions.assertThat(c.fromEvent(null, new byte[]{}) ).isNull();
    }

    @Test
    void commandStillUsesOnOff() {
        Assertions.assertThat(new RegimeStateCalculator().toCommand(null, new OnOffState(OnOffToggleStateCalculator.ValidOnOffToggle.ON))).containsExactly((byte) 255);
    }
}
