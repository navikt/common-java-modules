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
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPing();
        Map<String, String> map = new HashMap<>();
        map.put("host", getHost());
        map.put("version", getApplicationVersion());
        map.put("status", getStatus());
        map.put("message", getMessage());
        resp.setContentType("application/json");
        resp.getWriter().write(asJson(map));
    }

    private static String asJson(Map<String, String> map) {
        return "{" + StringUtils.join(on(map).map(new Transformer<Map.Entry<String, String>, String>() {
            @Override
            public String transform(Map.Entry<String, String> entry) {
                return "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"";
            }
        }).collect(), ",") + "}";
    }
}
