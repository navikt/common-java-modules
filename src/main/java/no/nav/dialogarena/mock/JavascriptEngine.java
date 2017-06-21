package no.nav.dialogarena.mock;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.SneakyThrows;
import no.nav.apiapp.util.JsonUtils;
import org.eclipse.jetty.util.resource.Resource;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static javax.script.ScriptContext.ENGINE_SCOPE;

public class JavascriptEngine {

    @SneakyThrows
    static void evaluateJavascript(Resource javascript, HttpServletRequest request, HttpServletResponse httpServletResponse) {
        ScriptEngine scriptEngine = new NashornScriptEngineFactory().getScriptEngine();
        try (Reader reader = new InputStreamReader(javascript.getInputStream())) {

            ScriptContext scriptContext = scriptEngine.getContext();
            Response response = new Response(scriptEngine);
            scriptContext.setAttribute("response", response, ENGINE_SCOPE);
            scriptContext.setAttribute("request", Request.create(scriptEngine, request), ENGINE_SCOPE);

            scriptEngine.eval(reader, scriptContext);

            httpServletResponse.setStatus(response.status);
            httpServletResponse.getWriter().write(response.responseTekst);
        }
    }

    public static class Request {
        public Map<String, String> params = new HashMap<>();

        public Request(HttpServletRequest request) {
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();
                params.put(parameterName, request.getParameter(parameterName));
            }
        }

        @SneakyThrows
        public static Object create(ScriptEngine scriptEngine, HttpServletRequest httpServletRequest) {
            String requestHandlerJson = JsonUtils.toJson(new Request(httpServletRequest));
            ScriptObjectMirror json = (ScriptObjectMirror) scriptEngine.eval("JSON");
            return json.callMember("parse", requestHandlerJson);
        }
    }

    public static class Response {
        private final ScriptEngine scriptEngine;

        private int status = 200;
        private String responseTekst = "";

        public Response(ScriptEngine scriptEngine) {
            this.scriptEngine = scriptEngine;
        }

        public void setStatus(int newStatus) {
            this.status = newStatus;
        }

        public void setResponseTekst(String responseTekst) {
            this.responseTekst = responseTekst;
        }

        @SneakyThrows
        public void setResponseJson(Object response) {
            ScriptObjectMirror json = (ScriptObjectMirror) scriptEngine.eval("JSON");
            this.responseTekst = (String) json.callMember("stringify", response);
        }

        public void send(String aString) {
            System.out.println(aString);
        }
    }



}
