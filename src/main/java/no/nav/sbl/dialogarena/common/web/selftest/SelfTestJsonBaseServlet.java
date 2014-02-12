package no.nav.sbl.dialogarena.common.web.selftest;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static no.nav.modig.lang.collections.IterUtils.on;

public abstract class SelfTestJsonBaseServlet extends AbstractSelfTestBaseServlet {

    private Map<String, String> valueMap = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPing();
        valueMap = new HashMap<>();
        valueMap.put("host", getHost());
        valueMap.put("version", getApplicationVersion());
        valueMap.put("status", getStatus());
        valueMap.put("message", getMessage());
        resp.setContentType("application/json");
        resp.getWriter().write(asJson());
    }

    protected String asJson() {
        return "{" + StringUtils.join(on(valueMap).map(new Transformer<Map.Entry<String, String>, String>() {
            @Override
            public String transform(Map.Entry<String, String> entry) {
                return "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"";
            }
        }).collect(), ",") + "}";
    }
}
