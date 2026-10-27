package io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator;

import io.github.ridiekel.jeletask.client.spec.ComponentSpec;
import io.github.ridiekel.jeletask.client.spec.state.impl.TemperatureControlState;
import io.github.ridiekel.jeletask.utilities.Bytes;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.github.ridiekel.jeletask.client.builder.composer.config.statecalculator.TemperatureStateCalculator.bigDecimalToBytes;

class TemperatureControlStateCalculatorTest {
    private static byte[] event(int preset, int mode, int speed, boolean withNightCooling) {
        byte[] head = Bytes.concat(
                bigDecimalToBytes(new BigDecimal("20.5")),  // current
                bigDecimalToBytes(new BigDecimal("21.0")),  // target
                bigDecimalToBytes(new BigDecimal("21.0")),  // day preset
                bigDecimalToBytes(new BigDecimal("17.0")),  // night preset (heating)
                new byte[]{30, (byte) preset, (byte) mode, (byte) speed, (byte) 255, (byte) 255, 0, 0}
        );
        return withNightCooling ? Bytes.concat(head, bigDecimalToBytes(new BigDecimal("25.0"))) : head;
    }

    private static ComponentSpec component() {
        ComponentSpec component = new ComponentSpec();
        component.setDecimals(1);
        return component;
    }

    @Test
    void knownValuesAreDecoded() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event(26, 95, 89, true));

        Assertions.assertThat(state.getCurrentTemperature()).isEqualByComparingTo("20.5");
        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.0");
        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.DAY);
        Assertions.assertThat(state.getMode()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT);
        Assertions.assertThat(state.getFanspeed()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlSpeed.AUTO);
        Assertions.assertThat(state.getNightAtCoolingPresetTemperature()).isEqualByComparingTo("25.0");
    }

    @Test
    void unknownPresetAndSpeedDoNotThrow() {
        // Radiator thermostat without fan / preset: central reports 0 for these bytes ("No value for key '0'")
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), event(0, 95, 0, true));

        Assertions.assertThat(state.getPreset()).isNull();
        Assertions.assertThat(state.getFanspeed()).isNull();
        Assertions.assertThat(state.getMode()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlMode.HEAT);
        Assertions.assertThat(state.getTargetTemperature()).isEqualByComparingTo("21.0");
    }

    @Test
    void seventeenByteEventWithoutNightCoolingPreset() {
        TemperatureControlState state = new TemperatureControlStateCalculator().fromEvent(component(), Bytes.concat(event(25, 95, 0, false), new byte[]{0}));

        Assertions.assertThat(state.getPreset()).isEqualTo(TemperatureControlStateCalculator.ValidTemperatureControlPreset.NIGHT);
        Assertions.assertThat(state.getNightAtCoolingPresetTemperature()).isNull();
    }

    @Test
    void targetTemperatureCommand() {
        TemperatureControlState state = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        state.setAction(TemperatureControlStateCalculator.ValidTemperatureControlAction.TARGET);
        state.setTargetTemperature(new BigDecimal("21.5"));

        byte[] command = new TemperatureControlStateCalculator().toCommand(component(), state);

        Assertions.assertThat(command).startsWith((byte) 87);
        Assertions.assertThat(command).hasSize(3);
    }

    @Test
    void turningOnFromOffSendsOn() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();
        component.setState(new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.OFF));

        // HA mode_command_template for 'heat' while the thermostat is OFF
        Assertions.assertThat(c.toCommand(component, c.stateFromMessage("{\"state\": \"ON\", \"mode\": \"HEAT\"}")))
                .containsExactly((byte) 255);
        // HA power on payload
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{ \"state\": \"ON\" }")))
                .containsExactly((byte) 255);
        // Already ON: mode command is passed through
        component.setState(new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON));
        Assertions.assertThat(c.toCommand(component, c.stateFromMessage("{\"state\": \"ON\", \"mode\": \"HEAT\"}")))
                .containsExactly((byte) 95);
    }

    @Test
    void manualPresetResendsCurrentTarget() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();
        ComponentSpec component = component();
        TemperatureControlState current = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        current.setTargetTemperature(new BigDecimal("19.0"));
        component.setState(current);

        byte[] manual = c.toCommand(component, c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"MANUAL\"}"));

        TemperatureControlState target = new TemperatureControlState(OnOffToggleStateCalculator.ValidOnOffToggle.ON);
        target.setAction(TemperatureControlStateCalculator.ValidTemperatureControlAction.TARGET);
        target.setTargetTemperature(new BigDecimal("19.0"));
        Assertions.assertThat(manual).containsExactly(c.toCommand(component, target));
    }

    @Test
    void homeAssistantPayloadsToCommand() {
        TemperatureControlStateCalculator c = new TemperatureControlStateCalculator();

        // preset_mode_command_template
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{\"state\": \"ON\", \"preset\": \"NIGHT\"}")))
                .containsExactly((byte) 25);
        // temperature_command_template
        Assertions.assertThat(c.toCommand(component(), c.stateFromMessage("{ \"state\": \"ON\", \"action\": \"TARGET\", \"target_temperature\": 21.5 }")))
                .startsWith((byte) 87).hasSize(3);
    }
}
