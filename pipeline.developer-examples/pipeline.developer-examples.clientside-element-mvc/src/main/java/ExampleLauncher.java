import org.apache.catalina.LifecycleException;

import static fiftyone.pipeline.developerexamples.web.shared.EmbedTomcat.runWebApp;

public class ExampleLauncher {
    public static void main(String[] args) throws LifecycleException {
        runWebApp(
                "pipeline.developer-examples/pipeline.developer-examples.clientside-element-mvc/src/main/webapp",
                "pipeline.developer-examples/pipeline.developer-examples.clientside-element-mvc/target",
                8088);
    }
}
