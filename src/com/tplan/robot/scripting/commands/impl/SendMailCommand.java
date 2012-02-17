/*
 * T-Plan Robot, automated testing tool based on remote desktop technologies.
 * Copyright (C) 2009  T-Plan Limited (http://www.t-plan.co.uk),
 * Tolvaddon Energy Park, Cornwall, TR14 0HX, United Kingdom
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.tplan.robot.scripting.commands.impl;

import com.tplan.robot.ApplicationSupport;
import com.tplan.robot.preferences.Preference;
import com.tplan.robot.scripting.commands.AbstractCommandHandler;
import com.tplan.robot.preferences.UserConfiguration;
import com.tplan.robot.scripting.SyntaxErrorException;
import com.tplan.robot.scripting.TokenParser;

import com.tplan.robot.scripting.ScriptingContext;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.security.Security;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Handler implementing functionality of the {@doc.cmd SendMail} command.
 * @product.signature
 */
public class SendMailCommand extends AbstractCommandHandler {

    public final static String PARAM_SERVER = "server";
    public final static String PARAM_PORT = "port";
    public final static String PARAM_TO = "to";
    public final static String PARAM_FROM = "from";
    public final static String PARAM_TEXT = "text";
    public final static String PARAM_SUBJECT = "subject";
    public final static String PARAM_ATTACH = "attach";
    public final static String PARAM_DEBUG = "debug";
    public final static String PARAM_USER = "user";
    public final static String PARAM_PASSWD = "passwd";
    private final static KeyStroke contextShortcut = KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK);
    private static Map contextAttributes;

    /**
     * Get a map with context attributes.
     *
     * @return A hash table containing complete list of supported parameters and their descriptions or list of values.
     */
    public Map getContextAttributes() {
        if (contextAttributes == null) {
            contextAttributes = new HashMap();
            ResourceBundle res = ApplicationSupport.getResourceBundle();
            contextAttributes.put(PARAM_SERVER, res.getString("sendmail.param.server"));
            contextAttributes.put(PARAM_TO, res.getString("sendmail.param.to"));
            contextAttributes.put(PARAM_FROM, res.getString("sendmail.param.from"));
            contextAttributes.put(PARAM_SUBJECT, res.getString("sendmail.param.subject"));
            contextAttributes.put(PARAM_TEXT, res.getString("sendmail.param.body"));
            contextAttributes.put(PARAM_ATTACH, MessageFormat.format(res.getString("sendmail.param.attach"), TokenParser.FILE_PATH_SEPARATOR));
            contextAttributes.put(PARAM_DEBUG, "true|false");
            contextAttributes.put(PARAM_USER, res.getString("sendmail.param.userName"));
            contextAttributes.put(PARAM_PASSWD, res.getString("sendmail.param.password"));
        }
        return contextAttributes;
    }

    public void validate(List args, Map values, Map variableContainer, ScriptingContext repository) throws SyntaxErrorException {
        Map vt = variableContainer == null ? new HashMap() : variableContainer;
        String parName;
        String value;
        ResourceBundle res = ApplicationSupport.getResourceBundle();

        // Now proceed to other arguments
        for (int j = 0; j < args.size(); j++) {
            parName = args.get(j).toString().toLowerCase();
            value = (String) values.get(parName);
            value = value == null ? "" : value;

            if (parName.equals(PARAM_SERVER)) {
                try {
                    int i = value.indexOf(":");
                    if (i >= 0) {
                        int port = Integer.parseInt(value.substring(i + 1));
                        vt.put(PARAM_PORT, "" + port);
                        value = value.substring(0, i);
                    }
                    vt.put(PARAM_SERVER, value);
                } catch (Exception ex) {
                    String s = res.getString("command.syntaxErr.invalid");
                    throw new SyntaxErrorException(MessageFormat.format(s, parName));
                }
            } else if (parName.equals(PARAM_TO)) {
                if (value.trim().length() <= 0) {
                    String s = res.getString("command.syntaxErr.empty");
                    throw new SyntaxErrorException(MessageFormat.format(s, parName));
                }
                vt.put(PARAM_TO, value);
            } else if (parName.equals(PARAM_FROM)) {
                if (value.trim().length() <= 0) {
                    String s = res.getString("command.syntaxErr.empty");
                    throw new SyntaxErrorException(MessageFormat.format(s, parName));
                }
                vt.put(PARAM_FROM, value);
            } else if (parName.equals(PARAM_SUBJECT)) {
                vt.put(PARAM_SUBJECT, value);
            } else if (parName.equals(PARAM_TEXT)) {
                vt.put(PARAM_TEXT, value);
            } else if (parName.equals(PARAM_USER)) {
                vt.put(PARAM_USER, value);
            } else if (parName.equals(PARAM_PASSWD)) {
                vt.put(PARAM_PASSWD, value);
            } else if (parName.equals(PARAM_ATTACH)) {
                if (value.trim().length() > 0) {
                    vt.put(PARAM_ATTACH, value);
                }
            } else if (parName.equals(PARAM_DEBUG)) {
                TokenParser parser = repository.getParser();
                vt.put(PARAM_DEBUG, parser.parseBoolean(value, PARAM_DEBUG));
            } else {
                String s = res.getString("command.syntaxErr.unknownParam");
                throw new SyntaxErrorException(MessageFormat.format(s, parName));
            }
        }

        // Post validation - some values are mandatory
        UserConfiguration cfg = repository.getConfiguration();

        // Server must be present or there must be a valid value in the user preferences
        validateValue(cfg, vt, PARAM_SERVER, "SendMailCommand.defaultMailServer");

        // 'From' address must be present or there must be a valid value in the user preferences
        validateValue(cfg, vt, PARAM_FROM, "SendMailCommand.defaultFromAddress");

        // 'To' address must be present or there must be a valid value in the user preferences
        validateValue(cfg, vt, PARAM_TO, "SendMailCommand.defaultToAddress");

        // Port must be present or there must be a valid value in the user preferences
        validateValue(cfg, vt, PARAM_PORT, "SendMailCommand.defaultMailServerPort");

        // If user is not specified, load default value from config
        if (!vt.containsKey(PARAM_USER)) {
            String user = cfg.getString("SendMailCommand.defaultUser");
            if (user != null) {
                vt.put(PARAM_USER, user);
            }
        }
    }

    private void validateValue(UserConfiguration cfg, Map params, String key, String preferenceKey) throws SyntaxErrorException {
        if (!params.containsKey(key)) {
            String param = cfg.getString(preferenceKey);
            if (param == null || param.trim().length() <= 0) {
                String s = ApplicationSupport.getString("sendmail.syntaxErr.mandatoryAndConfigurable");
                throw new SyntaxErrorException(MessageFormat.format(s, key));
            }
            params.put(key, param);
        }
    }

    public String[] getCommandNames() {
        return new String[]{"sendmail"};
    }

    public int execute(List args, Map values, ScriptingContext repository) throws SyntaxErrorException {

        Map t = new HashMap();

        // Validate
        validate(args, values, t, repository);

        try {
            boolean debugMode = false;
            if (t.containsKey(PARAM_DEBUG)) {
                debugMode = ((Boolean) t.get(PARAM_DEBUG)).booleanValue();
            }

            boolean isStop = false;

            SendMailThread smt = new SendMailThread(t, debugMode);
            smt.start();

            // Wait either for the result or break from user
            while (!isStop && smt.isAlive()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    // Do not react
                }
                if (repository.containsKey(ScriptingContext.CONTEXT_STOP_REASON)) {
                    smt.kill();
                    return 1;
                }
            }
            return smt.exitCode;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }

    public List<Preference> getPreferences() {
        List v = new ArrayList();
        ResourceBundle res = ApplicationSupport.getResourceBundle();
        String containerName = res.getString("options.sendmail.group.defaults");
        Preference o = new Preference("SendMailCommand.defaultMailServer",
                Preference.TYPE_STRING,
                res.getString("options.sendmail.defaultMailServer.name"),
                res.getString("options.sendmail.defaultMailServer.desc"));
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("SendMailCommand.defaultMailServerPort",
                Preference.TYPE_INT,
                res.getString("options.sendmail.defaultServerPort.name"),
                null);
        o.setPreferredContainerName(containerName);
        o.setMinValue(0);
        v.add(o);

        o = new Preference("SendMailCommand.defaultFromAddress",
                Preference.TYPE_STRING,
                res.getString("options.sendmail.defaultFromAddress.name"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        o = new Preference("SendMailCommand.defaultToAddress",
                Preference.TYPE_STRING,
                res.getString("options.sendmail.defaultToAddress.name"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);
        o = new Preference("SendMailCommand.defaultUser",
                Preference.TYPE_STRING,
                res.getString("options.sendmail.defaultUserId.name"),
                null);
        o.setPreferredContainerName(containerName);
        v.add(o);

        // SSL connection deferred to future releases
/*        o = new Preference("SendMailCommand.useSSL",
        Preference.TYPE_BOOLEAN,
        "Use SSL (change port to 465 if selected)",
        null);
        o.setPreferredContainerName("Send Mail Defaults");
        v.add(o);
         */
        return v;
    }

    public KeyStroke getContextShortcut() {
        return contextShortcut;
    }

    /**
     * As this command can run without a VNC server connection, this method always returns true.
     *
     * @return this implementation always returns true.
     */
    public boolean canRunWithoutConnection() {
        return true;
    }

    class SendMailThread extends Thread {

        boolean debugMode;
        int exitCode = 0;
        Map t;

        SendMailThread(Map t, boolean debugMode) {
            this.t = t;
            this.debugMode = debugMode;
        }

        public void run() {
            try {
                final String server = (String) t.get(PARAM_SERVER);
                Object p = t.get(PARAM_PORT);
                int port = p instanceof Number ? ((Number) p).intValue() : Integer.parseInt(p.toString());                    // Default SMTP port

                Properties props = new Properties();
                props.put("mail.protocol", "smtp");
                props.put("mail.smtp.host", server);
                props.put("mail.smtp.port", "" + port);

                boolean authenticate = t.containsKey(PARAM_USER) && t.containsKey(PARAM_PASSWD);

                boolean useSSL = UserConfiguration.getCopy().getBoolean("SendMailCommand.useSSL");
                if (useSSL) {
                    Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                    props.put("mail.smtp.socketFactory.port", port);
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.fallback", "true");
                }

                String user = t.get(PARAM_USER).toString(); // + "@" + server;

                // Create a session and set the debug flag
                Session session = Session.getInstance(props, null);
                session.setDebug(debugMode);

                // Create a MIME message
                MimeMessage msg = new MimeMessage(session);

                // Add all recipients
                String to[] = t.get(PARAM_TO).toString().split(",");
                InternetAddress[] toAddress = new InternetAddress[to.length];
                for (int i = 0; i < to.length; i++) {
                    toAddress[i] = new InternetAddress(to[i]);
                }
                msg.setRecipients(Message.RecipientType.TO, toAddress);

                // Set the From field to the first address in the "To:" list
                // if no other value is set
                String from = to[0];
                if (t.containsKey(PARAM_FROM)) {
                    from = t.get(PARAM_FROM).toString();
                }
                msg.setFrom(new InternetAddress(from));

                // Create the message body part
                MimeBodyPart messageBodyPart = new MimeBodyPart();
                String bodyText = t.containsKey(PARAM_TEXT) ? t.get(PARAM_TEXT).toString() : "";

                // Bug fix in 1.3.15 - newline characters must be replaced by \r
                String ss[] = bodyText.split("\\\\n");
                bodyText = "";
                for (String s : ss) {
                    bodyText += s;
                    if (s.endsWith("\\")) {
                        bodyText += "n";
                    } else {
                        bodyText += '\r';
                    }
                }
                // Improvement in 2.0 - if the text starts with "<html>", 
                // set the content type to "text/html"
                String contentType = "text/plain";
                if (bodyText.length() > 6 && bodyText.trim().substring(0, 6).toLowerCase().equals("<html>")) {
                    contentType = "text/html";
                }
                messageBodyPart.setContent(bodyText, contentType);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(messageBodyPart);

                // If attachments are specified, add a MIME body part for
                // each file in the list.
                if (t.containsKey(PARAM_ATTACH)) {
                    String files[] = t.get(PARAM_ATTACH).toString().split(TokenParser.FILE_PATH_SEPARATOR);
                    File f;
                    BodyPart attachmentBodyPart;
                    for (String filename : files) {
                        f = new File(filename);
                        attachmentBodyPart = new MimeBodyPart();

                        if (f.exists() && f.canRead() && !f.isDirectory()) {
                            DataSource source = new FileDataSource(filename);
                            attachmentBodyPart.setDataHandler(new DataHandler(source));

                            // Fixed in 1.3.16 as a bug - set the attachment name to file name instead of full path
                            // attachmentBodyPart.setFileName(filename);
                            attachmentBodyPart.setFileName(f.getName());
                        } else { // Can't read a file -> add a note to the message
                            ResourceBundle res = ApplicationSupport.getResourceBundle();
                            String s = res.getString("sendmail.attachError.prefix");
                            String errText; // = "WARNING: Failed to attach " + f.getAbsolutePath() + " : ";
                            if (!f.exists()) {
                                errText = res.getString("sendmail.attachError.notFound");
                            } else if (f.isDirectory()) {
                                errText = res.getString("sendmail.attachError.isDirectory");
                            } else if (!f.canRead()) {
                                errText = res.getString("sendmail.attachError.cannotRead");
                            } else {
                                errText = res.getString("sendmail.attachError.unknown");
                            }
                            String errMsg = MessageFormat.format(s, f.getAbsolutePath(), errText);
                            attachmentBodyPart.setText(errMsg);
                        }
                        multipart.addBodyPart(attachmentBodyPart);
                    }
                }
                msg.setContent(multipart);

                msg.setSubject(t.containsKey(PARAM_SUBJECT) ? t.get(PARAM_SUBJECT).toString() : "");
                msg.setSentDate(new Date());

                // Send the E-mail
                Transport trans = session.getTransport("smtp");
                if (authenticate) {
                    trans.connect(server, user, t.get(PARAM_PASSWD).toString());
                    trans.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));
                } else {
                    Transport.send(msg);
                }


            } catch (Exception ex) {
                ex.printStackTrace();
                exitCode = 1;
            }
        }

        void kill() {
            exitCode = 10;
        }
    }
}
