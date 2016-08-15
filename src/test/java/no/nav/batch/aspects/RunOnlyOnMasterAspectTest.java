package no.nav.batch.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static java.lang.System.setProperty;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class RunOnlyOnMasterAspectTest {

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private RunOnlyOnMaster runOnlyOnMaster;

    @Mock
    private Signature signature;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void proceedHvisMasterNode() throws Throwable {
        setProperty("cluster.ismasternode", "true");
        new RunOnlyOnMasterAspect().runOnlyOnMaster(proceedingJoinPoint, runOnlyOnMaster);

        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    public void stoppHvisIkkeMasterNode() throws Throwable {
        setProperty("cluster.ismasternode", "false");
        new RunOnlyOnMasterAspect().runOnlyOnMaster(proceedingJoinPoint, runOnlyOnMaster);

        verify(proceedingJoinPoint, times(0)).proceed();
    }
}
