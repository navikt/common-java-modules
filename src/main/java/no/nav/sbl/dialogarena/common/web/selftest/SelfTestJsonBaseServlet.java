package no.nav.sbl.dialogarena.common.web.selftest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.sbl.dialogarena.common.web.selftest.json.Selftest;
import no.nav.sbl.dialogarena.common.web.selftest.json.SelftestEndpoint;
import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;


public abstract class SelfTestJsonBaseServlet extends AbstractSelfTestBaseServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPing();

        Selftest selftest = new Selftest()
                .setApplication(getApplicationName())
                .setVersion(getApplicationVersion())
                .setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .setAggregateResult(getAggregertStatus())
                .setChecks(result.stream()
                        .map(SelfTestJsonBaseServlet::lagSelftestEndpoint)
                        .collect(toList())
                );

        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        resp.getWriter().write(om.writeValueAsString(selftest));
    }

    private static SelftestEndpoint lagSelftestEndpoint(Pingable.Ping ping) {
        return new SelftestEndpoint()
                .setEndpoint(ping.getEndepunkt())
                .setDescription(ping.getBeskrivelse())
                .setErrorMessage(ping.getFeilmelding())
                .setResult(ping.harFeil() ? STATUS_ERROR : STATUS_OK)
                .setResponseTime(String.format("%dms", ping.getResponstid()))
                .setStacktrace(ofNullable(ping.getFeil())
                        .map(ExceptionUtils::getStackTrace)
                        .orElse(null)
                );
    }
}
