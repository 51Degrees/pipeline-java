package fiftyone.pipeline.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(BuilderClassPathTestRunner.class)
public class PipelineFilterTests {

    private FilterConfig config;
    private ServletContext context;

    @Before
    public void init() {
        config = mock(FilterConfig.class);
        when(config.getInitParameter(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocationOnMock) throws Throwable {
                switch ((String)invocationOnMock.getArgument(0)) {
                    case "config-file":
                        return "filename";
                    default:
                        return null;
                }
            }
        });
        context = mock(ServletContext.class);
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("config.xml").getFile());
        when(context.getRealPath(anyString())).thenReturn(file.getAbsolutePath());

        when(config.getServletContext()).thenReturn(context);
    }

    @Test
    public void PipelineFilter_Initialise() throws ServletException {
        PipelineFilter filter = new PipelineFilter();

        filter.init(config);
    }
}
