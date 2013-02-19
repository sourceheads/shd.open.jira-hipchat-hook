package org.sourceheads.open.jhch;

import java.io.StringWriter;
import java.net.URLEncoder;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.junit.Test;

import com.jayway.jsonpath.JsonModel;

/**
 * (...)
 *
 * @author Stefan Fiedler
 */
public class SampleRequestTest {

    @Test
    public void testVelocity() throws Exception {

        final JsonModel jsonModel = JsonModel.model(getClass().getResourceAsStream("sample01.json"));

        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        final VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("json", new VelocityJsonHelper(jsonModel));

        final String issueSelf = jsonModel.get("issue.self");
        if (issueSelf != null && !issueSelf.isEmpty()) {
            velocityContext.put("baseUrl", issueSelf.replaceAll("/rest/api/.*", ""));
        }

        final StringWriter writer = new StringWriter();

        final Template template = velocityEngine.getTemplate("/velocity/message.vm");
        template.merge(velocityContext, writer);

        final String message = writer.toString().trim();
        System.out.println(message);

        final String encoded = URLEncoder.encode(message, "UTF-8");
        System.out.println(encoded);
    }
}
