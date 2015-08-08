package org.cyclops.integrateddynamics.core.evaluate.variable;

import lombok.ToString;
import org.cyclops.cyclopscore.helper.Helpers;

/**
 * Wildcard value type
 * @author rubensworks
 */
public class ValueTypeAny extends ValueTypeBase<ValueTypeAny.ValueAny> {

    public ValueTypeAny() {
        super("any", Helpers.RGBToInt(240, 240, 240), "");
    }

    @Override
    public ValueAny getDefault() {
        return ValueAny.of();
    }

    @Override
    public String toCompactString(ValueAny value) {
        return "any";
    }

    @Override
    public boolean correspondsTo(IValueType valueType) {
        return true;
    }

    @ToString
    public static class ValueAny extends BaseValue {

        private ValueAny() {
            super(ValueTypes.ANY);
        }

        public static ValueAny of() {
            return new ValueAny();
        }

    }

}
