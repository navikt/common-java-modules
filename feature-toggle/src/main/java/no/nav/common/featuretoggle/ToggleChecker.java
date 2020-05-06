package no.nav.common.featuretoggle;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Optional.ofNullable;

public class ToggleChecker {

	public static boolean isToggleEnabled(String parameterName, Map<String, String> parameters,
	                                      Predicate<String> checkIsEnabled) {
		return ofNullable(parameters)
				.map(m -> m.get(parameterName))
				.map(s -> s.split(","))
				.map(Arrays::stream)
				.map(s -> s.anyMatch(checkIsEnabled))
				.orElse(false);
	}

}
