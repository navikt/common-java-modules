package no.nav.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;


@Slf4j
public class SubjectRule implements MethodRule {

    private Subject subject;

    public SubjectRule() {
    }

    public SubjectRule(Subject subject) {
        this.subject = subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
        log.info("subject is: {}", subject);
    }

    @Override
    public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object o) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                SubjectHandler.withSubjectProvider(() -> subject, statement::evaluate);
            }
        };
    }
}
