package no.nav.apiapp.selftest;

import no.nav.sbl.dialogarena.common.web.selftest.SelfTestJsonBaseServlet;
import no.nav.sbl.dialogarena.types.Pingable;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletException;
import java.util.Collection;

import static no.nav.apiapp.ServletUtil.getContext;

public class SelfTestJsonServlet extends SelfTestJsonBaseServlet {

    private ApplicationContext ctx = null;

    @Override
    public void init() throws ServletException {
        ctx = getContext(getServletContext());
        super.init();
    }

    @Override
    protected Collection<Pingable> getPingables() {
        return ctx.getBeansOfType(Pingable.class).values();
    }
}
