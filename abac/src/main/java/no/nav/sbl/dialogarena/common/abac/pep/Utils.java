package no.nav.sbl.dialogarena.common.abac.pep;

import lombok.SneakyThrows;
import no.nav.metrics.MetricsFactory;
import no.nav.metrics.Timer;
import no.nav.sbl.dialogarena.common.abac.pep.domain.Attribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.BaseAttribute;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.Request;
import no.nav.sbl.dialogarena.common.abac.pep.domain.request.XacmlRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.System.getProperty;


public class Utils {

    @SneakyThrows
    public static <T> T timed(String name, Callable<T> task, Consumer<Timer> additions) {
        Timer timer = MetricsFactory.createTimer(name);
        try {
            timer.start();
            return task.call();
        } catch (Throwable e) {
            timer.setFailed();
            throw e;
        } finally {
            timer.stop();
            if (additions != null) {
                additions.accept(timer);
            }
            timer.report();
        }
    }

    public static <T> T timed(String name, Callable<T> task) throws Exception {
        return timed(name, task, null);
    }

    static boolean invalidClientValues(RequestData requestData) {
        return requestData.getDomain() == null
                || requestData.getCredentialResource() == null
                || (requestData.getOidcToken() == null && requestData.getSamlToken() == null && requestData.getSubjectId() == null)
                ;
    }

    public static String getResourceAttribute(XacmlRequest request, String requestedAttribute) {
        return Optional.ofNullable(request)
                .map(XacmlRequest::getRequest)
                .flatMap(Request::getFirstResource)
                .map(BaseAttribute::getAttribute)
                .map(findAttribute(requestedAttribute))
                .orElse("EMPTY");
    }

    private static Function<List<Attribute>, String> findAttribute(String requestedAttribute) {
        return attributes -> findAttribute(attributes, requestedAttribute);
    }

    private static String findAttribute(List<Attribute> attributes, String requestedAttribute) {
        return attributes.stream()
                .filter(a -> requestedAttribute.equals(a.getAttributeId()))
                .findFirst()
                .orElse(new Attribute("EMPTY", "EMPTY"))
                .getValue();
    }
}
