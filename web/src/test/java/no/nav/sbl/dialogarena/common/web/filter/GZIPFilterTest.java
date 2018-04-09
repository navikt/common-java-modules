package no.nav.sbl.dialogarena.common.web.filter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GZIPFilterTest {


    private GZIPFilter gzipFilter = new GZIPFilter();

    @Test
    public void doFilter__nested_filters__with_gzipping_exactly_once() throws IOException, ServletException {
        MockHttpServletResponse mockHttpServletResponse = mockResponse();
        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.addHeader("accept-encoding", "gzip");

        gzipFilter.doFilter(httpServletRequest, mockHttpServletResponse,
                (request, response) -> gzipFilter.doFilter(request, response,
                        (request1, response1) -> gzipFilter.doFilter(request, response,
                                (request2, response2) -> response2.getWriter().write("test")
                        )
                )
        );

        assertThat(readGzip(mockHttpServletResponse.getContentAsByteArray())).isEqualTo("test");
    }

    @Test
    public void doFilter__not_accepted__no_gzipping() throws IOException, ServletException {
        MockHttpServletResponse mockResponse = mockResponse();

        gzipFilter.doFilter(new MockHttpServletRequest(), mockResponse, (request, response) -> {
            PrintWriter writer = response.getWriter();
            writer.write("test");
            writer.flush();
        });

        assertThat(mockResponse.getContentAsString()).isEqualTo("test");
    }

    private MockHttpServletResponse mockResponse() throws IOException {
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        httpServletResponse.setCharacterEncoding("UTF-8");
        return httpServletResponse;
    }

    private String readGzip(byte[] byteArrayOutputStream) throws IOException {
        return IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(byteArrayOutputStream)), "UTF-8");
    }

}