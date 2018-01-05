package no.nav.apiapp.selftest;

import no.nav.apiapp.ApiApplication;
import no.nav.apiapp.ServletUtil;
import no.nav.sbl.dialogarena.common.web.selftest.SelfTestBaseServlet;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletException;
import java.util.Collection;

import static no.nav.apiapp.ServletUtil.getContext;

public class SelfTestServlet extends SelfTestBaseServlet {

    private WebApplicationContext ctx = null;

    @Override
    public void init() throws ServletException {
        ctx = getContext(getServletContext());
        super.init();
    }

    @Override
    protected Collection<Pingable> getPingables() {
        return ctx.getBeansOfType(Pingable.class).values();
    }

    @Override
    protected String getApplicationName() {
        return ctx.getBean(ApiApplication.class).getApplicationName();
    }

}