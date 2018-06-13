package no.nav.validation;

import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.internal.engine.messageinterpolation.ParameterTermResolver;
import org.hibernate.validator.messageinterpolation.AbstractMessageInterpolator;
import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;

import javax.validation.*;
import java.util.Locale;
import java.util.Set;

import static java.util.stream.Collectors.joining;

public class ValidationUtils {

    private static final Validator VALIDATOR = buildValidator();

    public static void validate(Object object) {
        Set<ConstraintViolation<Object>> constraintViolations = VALIDATOR.validate(object);
        if (!constraintViolations.isEmpty()) {
            throw new IllegalArgumentException(String.format("Validation of '%s' failed:\n%s",
                    object,
                    constraintViolations.stream()
                            .map(ValidationUtils::formatViolation)
                            .sorted()
                            .collect(joining("\n")))
            );
        }
    }

    private static String formatViolation(ConstraintViolation<Object> objectConstraintViolation) {
        return String.format("%s = %s :: %s",
                objectConstraintViolation.getPropertyPath(),
                objectConstraintViolation.getInvalidValue(),
                objectConstraintViolation.getMessage()
        );
    }

    private static Validator buildValidator() {
        return Validation.byProvider(HibernateValidator.class)
                .configure()
                .messageInterpolator(getMessageInterpolator())
                .buildValidatorFactory()
                .getValidator();
    }

    private static MessageInterpolator getMessageInterpolator() {
        try {
            // Depends on an implementation of the Unified Expression Language (JSR 341)
            return new ResourceBundleMessageInterpolator();
        } catch (Exception e) {
            // If not avaliable, use a simplified version
            return new SimpleMessageInterpolator();
        }
    }

    private static class SimpleMessageInterpolator extends AbstractMessageInterpolator {

        private final ParameterTermResolver parameterTermResolver = new ParameterTermResolver();

        @Override
        public String interpolate(Context context, Locale locale, String term) {
            return parameterTermResolver.interpolate(context, term);
        }

    }

}
