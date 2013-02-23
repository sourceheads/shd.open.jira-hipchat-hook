package org.sourceheads.open.jhch;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jayway.jsonpath.JsonModel;

/**
 * (...)
 *
 * @author Stefan Fiedler
 */
public class ConversionServlet extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionServlet.class);

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            doPostInternal(req, resp);
        }
        catch (final Exception e) {
            LOGGER.error("doPost | Exception caught", e);
        }
    }

    protected void doPostInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final String config = request.getParameter("config");
        final Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("/config/" + config + ".properties"));

        // build json model

        final JsonModel jsonModel = JsonModel.model(request.getInputStream());
        LOGGER.debug("doPostInternal | json={}", jsonModel.toJson());

        // render velocity template

        final VelocityEngine velocityEngine = new VelocityEngine();
        velocityEngine.setProperty("resource.loader", "class");
        velocityEngine.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        velocityEngine.init();

        final VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("json", new VelocityJsonHelper(jsonModel));

        final String issueSelf = jsonModel.get("issue.self");
        if (issueSelf != null && !issueSelf.isEmpty()) {
            velocityContext.put("baseUrl", issueSelf.replaceAll("\\\\", "").replaceAll("/rest/api/.*", ""));
        }

        final StringWriter writer = new StringWriter();

        final Template template = velocityEngine.getTemplate(properties.getProperty("velocity.template"));
        template.merge(velocityContext, writer);

        final String message = writer.toString().trim();
        LOGGER.debug("doPostInternal | message={}", message);

        // call hipchat api

        final StringBuilder buf = new StringBuilder();
        buf.append("room_id=").append(URLEncoder.encode(properties.getProperty("hipchat.roomId"), "UTF-8"));
        buf.append("&from=").append(URLEncoder.encode(properties.getProperty("hipchat.from"), "UTF-8"));
        buf.append("&message=").append(URLEncoder.encode(message, "UTF-8"));
        buf.append("&message_format=").append(URLEncoder.encode(properties.getProperty("hipchat.messageFormat"), "UTF-8"));
        buf.append("&notify=").append(URLEncoder.encode(properties.getProperty("hipchat.notify"), "UTF-8"));
        buf.append("&color=").append(URLEncoder.encode(properties.getProperty("hipchat.color"), "UTF-8"));

        final String params = buf.toString();
        LOGGER.debug("doPostInternal | params={}", params);

        final URL hipChatUrl = new URL("https://api.hipchat.com/v1/rooms/message?format=json&auth_token=" +
                properties.getProperty("hipchat.apiToken"));
        final HttpURLConnection connection = (HttpURLConnection) hipChatUrl.openConnection();
        connection.setDoOutput(true);

        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(params.getBytes().length));
        connection.setRequestProperty("Content-Language", "en-US");

        try (final BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {
            IOUtils.write(params, outputStream);
        }

        final InputStream inputStream = connection.getInputStream();
        final String result = IOUtils.toString(inputStream);
        LOGGER.debug("doPostInternal | result={}", result);

        connection.disconnect();

        LOGGER.info("doPostInternal | sent message: config={}; message={}; result={}", config, message, result);
    }
}
