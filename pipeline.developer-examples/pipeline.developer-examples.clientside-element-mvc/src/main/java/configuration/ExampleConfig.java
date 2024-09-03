/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package configuration;

import fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfig;
import fiftyone.pipeline.web.mvc.configuration.FiftyOneInterceptorConfigDefault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;

import javax.servlet.ServletContext;

import static fiftyone.pipeline.web.mvc.components.FiftyOneInterceptor.enableClientsideProperties;

@EnableWebMvc
@Configuration
@ComponentScan({"controller","fiftyone.pipeline.web.mvc"})
public class ExampleConfig implements WebMvcConfigurer {

    public ExampleConfig() {
        super();
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        enableClientsideProperties(registry);
    }

    @Autowired
    ServletContext servletContext;

    @Bean
    public FiftyOneInterceptorConfig fiftyOneInterceptorConfig() {
        final FiftyOneInterceptorConfigDefault bean = new FiftyOneInterceptorConfigDefault();

        bean.setDataFilePath(servletContext.getRealPath("/WEB-INF/51Degrees.xml"));
        bean.setClientsidePropertiesEnabled(true);

        return bean;
    }

    @Bean
    public ViewResolver viewResolver() {
        final InternalResourceViewResolver bean = new InternalResourceViewResolver();

        bean.setViewClass(JstlView.class);
        bean.setPrefix("/WEB-INF/views/");
        bean.setSuffix(".jsp");

        return bean;
    }

    @Autowired
    FiftyOneInterceptor fiftyOneInterceptor;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(fiftyOneInterceptor);
    }
}
