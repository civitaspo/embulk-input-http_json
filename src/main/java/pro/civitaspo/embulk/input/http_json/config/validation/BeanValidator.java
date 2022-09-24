package pro.civitaspo.embulk.input.http_json.config.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.function.Supplier;
import org.embulk.config.ConfigException;
import org.hibernate.validator.HibernateValidator;

public class BeanValidator {

    public static <T> void validate(T bean) {
        Validator validator =
                switchingClassLoader(
                        bean.getClass(),
                        () -> {
                            return Validation.byProvider(HibernateValidator.class)
                                    .configure()
                                    .buildValidatorFactory()
                                    .getValidator();
                        });
        final Set<ConstraintViolation<T>> violations = validator.validate(bean);
        if (!violations.isEmpty()) {
            throw new ConfigException(
                    formatMessage(violations), new ConstraintViolationException(violations));
        }
    }

    private static <T> String formatMessage(Set<ConstraintViolation<T>> violations) {
        StringBuilder sb = new StringBuilder();
        sb.append("Configuration violates constraints validated in task definition.");
        for (ConstraintViolation<?> violation : violations) {
            sb.append(" '");
            sb.append(violation.getPropertyPath());
            sb.append("' ");
            sb.append(violation.getMessage());
            sb.append(" but got ");
            sb.append(violation.getInvalidValue());
            sb.append('.');
        }
        return sb.toString();
    }

    private static <T> T switchingClassLoader(Class<?> klass, Supplier<T> supplier) {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader target = klass.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(target);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
}
