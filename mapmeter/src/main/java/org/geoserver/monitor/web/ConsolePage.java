package org.geoserver.monitor.web;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.Model;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;
import org.opengeo.console.monitor.check.ConnectionChecker;
import org.opengeo.console.monitor.check.ConnectionResult;
import org.opengeo.console.monitor.config.MessageTransportConfig;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

public class ConsolePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logging.getLogger(ConsolePage.class);

    private final transient MessageTransportConfig messageTransportConfig;

    private final transient ConnectionChecker connectionChecker;

    public ConsolePage() {
        GeoServerApplication geoServerApplication = getGeoServerApplication();
        this.messageTransportConfig = geoServerApplication.getBeanOfType(MessageTransportConfig.class);
        if (messageTransportConfig == null) {
            throw new IllegalStateException("Error finding MessageTransportConfig bean");
        }
        this.connectionChecker = geoServerApplication.getBeanOfType(ConnectionChecker.class);
        if (connectionChecker == null) {
            throw new IllegalStateException("Error finding ConnectionChecker bean");
        }
        addElements();
    }

    private void addElements() {
        addApiKeyForm();
        addConnectionCheckForm();
    }

    private void addConnectionCheckForm() {
        final Form<?> connectionCheckForm = new Form<Void>("connection-check-form");

        connectionCheckForm.add(new FeedbackPanel("connection-check-feedback"));

        AjaxLink<?> connectionCheckButton = new IndicatingAjaxLink<Void>("connection-check-button") {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.addComponent(connectionCheckForm);
                Optional<String> maybeApiKey;
                synchronized (messageTransportConfig) {
                    maybeApiKey = messageTransportConfig.getApiKey();
                }
                if (maybeApiKey.isPresent()) {
                    ConnectionResult result = connectionChecker.checkConnection(maybeApiKey.get());
                    if (result.isError()) {
                        Optional<Integer> maybeStatusCode = result.getStatusCode();
                        if (maybeStatusCode.isPresent()) {
                            int statusCode = maybeStatusCode.get();
                            if (statusCode == HttpStatus.SC_OK) {
                                connectionCheckForm.error(result.getError());
                            } else {
                                connectionCheckForm.error(statusCode + ": " + result.getError());
                            }
                        } else {
                            connectionCheckForm.error(result.getError());
                        }
                    } else {
                        connectionCheckForm.info("Connection successfully established.");
                    }
                } else {
                    connectionCheckForm.error("Please set an api key first");
                }
            }
        };
        connectionCheckForm.add(connectionCheckButton);

        add(connectionCheckForm);
    }

    public void addApiKeyForm() {
        Form<?> apiKeyForm = new Form<Void>("apikey-form");

        String apiKey;
        synchronized (messageTransportConfig) {
            apiKey = messageTransportConfig.getApiKey().or("");
        }
        final RequiredTextField<String> apiKeyField = new RequiredTextField<String>("apikey",
                Model.of(apiKey));
        apiKeyForm.add(apiKeyField);

        apiKeyForm.add(new FeedbackPanel("apikey-feedback"));

        AjaxButton apiKeyButton = new IndicatingAjaxButton("apikey-button") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String apiKey = apiKeyField.getModelObject().trim();
                try {
                    save(apiKey);
                    form.info("API key saved");
                } catch (IOException e) {
                    String msg = "Failure saving api key: " + apiKey;
                    LOGGER.severe(msg);
                    LOGGER.severe(e.getLocalizedMessage());
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(Throwables.getStackTraceAsString(e));
                    }
                    form.error(msg);
                }
                target.addComponent(form);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(form);
            }

        };
        apiKeyForm.add(apiKeyButton);

        add(apiKeyForm);
    }

    private void save(String apiKey) throws IOException {
        synchronized (messageTransportConfig) {
            messageTransportConfig.setApiKey(apiKey);
            messageTransportConfig.save();
        }
    }

}
