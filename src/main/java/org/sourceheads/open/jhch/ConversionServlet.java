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

    public static final String ENCODING_UTF8 = "UTF-8";

    public static final String PARAM_CONFIG = "config";

    public static final String CONFIG_PREFIX = "/config/";
    public static final String CONFIG_SUFFIX = ".properties";

    public static final String HIPCHAT_URL_V1 = "https://api.hipchat.com/v1";
    public static final String HIPCHAT_URL_ROOMS_MESSAGE = HIPCHAT_URL_V1 + "/rooms/message";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversionServlet.class);

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            doPostInternal(req, resp);
        }
        catch (final Exception e) {
            LOGGER.error("doPost | Exception caught", e);
        }
    }

    protected void doPostInternal(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        final String[] configNames = request.getParameterValues(PARAM_CONFIG);
        for (final String configName : configNames) {
            final Properties config = new Properties();
            final InputStream configStream = getClass().getClassLoader().getResourceAsStream(
                    CONFIG_PREFIX + configName + CONFIG_SUFFIX);
            config.load(configStream);
            runConfig(config, request, response);
        }
    }

    protected void runConfig(final Properties config, final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        // build json model

        final JsonModel jsonModel = JsonModel.model(request.getInputStream());
        LOGGER.debug("runConfig | json={}", jsonModel.toJson());

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

        final Template template = velocityEngine.getTemplate(config.getProperty("velocity.template"));
        template.merge(velocityContext, writer);

        final String message = writer.toString().trim();
        LOGGER.debug("runConfig | message={}", message);

        if (message.isEmpty()) {
            // do not send empty message
            LOGGER.debug("runConfig | message is empty; skipping hipchat API call");
            return;
        }

        // call hipchat api

        final StringBuilder buf = new StringBuilder();
        buf.append("room_id=").append(getConfig(config, "hipchat.roomId"));
        buf.append("&from=").append(getConfig(config, "hipchat.from"));
        buf.append("&message=").append(URLEncoder.encode(message, ENCODING_UTF8));
        buf.append("&message_format=").append(getConfig(config, "hipchat.messageFormat"));
        buf.append("&notify=").append(getConfig(config, "hipchat.notify"));
        buf.append("&color=").append(getConfig(config, "hipchat.color"));

        final String params = buf.toString();
        LOGGER.debug("runConfig | params={}", params);

        final URL hipChatUrl = new URL(HIPCHAT_URL_ROOMS_MESSAGE + "?format=json&auth_token=" +
                getConfig(config, "hipchat.apiToken"));
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
        LOGGER.debug("runConfig | result={}", result);

        connection.disconnect();

        LOGGER.info("runConfig | sent message: message={}; result={}", message, result);
    }

    protected String getConfig(final Properties config, final String key) throws IOException {
        return URLEncoder.encode(config.getProperty(key), ENCODING_UTF8);
    }
}
