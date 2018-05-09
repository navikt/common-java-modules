package no.nav.brukerdialog.security.context;

import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import org.junit.jupiter.api.extension.*;

import java.lang.reflect.Parameter;

public class SubjectExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback, ParameterResolver {

    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        SubjectHandlerHijack.release();
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        SubjectHandlerHijack.hijack(getStore(extensionContext));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Parameter parameter = parameterContext.getParameter();
        return parameter.getType() == SubjectStore.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return new SubjectStore(getStore(extensionContext));
    }

    private static ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getStore(ExtensionContext.Namespace.create(SubjectExtension.class, extensionContext.getRequiredTestMethod()));
    }

    private static class SubjectHandlerHijack extends SubjectHandler {

        private static void hijack(ExtensionContext.Store store) {
            setSupplier(() -> (Subject) store.get(SubjectExtension.class));
        }

        private static void release() {
            setSupplier(null);
        }
    }

    public static class SubjectStore {
        private final ExtensionContext.Store store;

        private SubjectStore(ExtensionContext.Store store) {
            this.store = store;
        }

        public void setSubject(Subject subject) {
            store.put(SubjectExtension.class, subject);
        }
    }
}
