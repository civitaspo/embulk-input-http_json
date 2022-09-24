package pro.civitaspo.embulk.input.http_json.config.validation.annotations;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ValueOfEnum.Validator.class)
public @interface ValueOfEnum {
    Class<? extends Enum<?>> enumClass();

    String message() default "must be any of enum {enumClass}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    boolean caseSensitive() default true;

    public class Validator implements ConstraintValidator<ValueOfEnum, CharSequence> {
        private boolean isCaseSensitive;
        private List<String> acceptedValues;

        @Override
        public void initialize(ValueOfEnum annotation) {
            isCaseSensitive = annotation.caseSensitive();
            acceptedValues =
                    Stream.of(annotation.enumClass().getEnumConstants())
                            .map(Enum::name)
                            .collect(Collectors.toList());
        }

        @Override
        public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
            if (value == null) {
                return true;
            }
            if (acceptedValues.stream()
                    .anyMatch(
                            v ->
                                    isCaseSensitive
                                            ? v.equals(value.toString())
                                            : v.equalsIgnoreCase(value.toString()))) {
                return true;
            }

            customizeErrorMessage(context);
            return false;
        }

        private void customizeErrorMessage(ConstraintValidatorContext context) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "must be any of values ["
                                    + acceptedValues.stream()
                                            .map(v -> String.format("'%s'", v))
                                            .collect(Collectors.joining(", "))
                                    + "]")
                    .addConstraintViolation();
        }
    }
}
