package org.jenkinsci.plugins.nodeofflinenotification;

import com.google.inject.Inject;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerListener;
import hudson.slaves.NodeProperty;
import hudson.slaves.OfflineCause;
import hudson.tasks.Mailer;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.mail.Message.RecipientType.TO;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class EmailComputerListener extends ComputerListener {

    @Inject private Mailer.DescriptorImpl mailer;

    @Override
    public void onOnline(Computer c, TaskListener listener) throws IOException, InterruptedException {
        String name = c.getName();

        if ("".equals(name)) { // Master

            for (Computer computer : Jenkins.getInstance().getComputers()) {
                if (computer.isOffline()) {
                    startUpNotification(c);
                }
            }
        } else {
            notifyResponsibleOnline(c);
        }

    }

    @Override
    public void onOffline(Computer c, OfflineCause cause) {
        if (cause != null) {
            notifyResponsibleOffline(c);
        }
    }

    private void notifyResponsibleOffline(Computer c) {
        notifyResponsible(c, false, false);
    }

    private void notifyResponsibleOnline(Computer c) {
        notifyResponsible(c, true, false);
    }


    private void startUpNotification(Computer c) {
        notifyResponsible(c, true, true);
    }

    private void notifyResponsible(Computer c, boolean onlineCalled, boolean masterStart)  {
        String[] emails = updateEmailAddresses(c);
        for (String email : emails) {
            try {
                sendEmail(email, c.getName(), onlineCalled, masterStart);
            } catch (MessagingException e) {
                LOGGER.log(Level.WARNING, "failed to send email to administrator "+e.getMessage());
            }
        }
    }

    private @Nonnull String[] updateEmailAddresses(Computer c) {
        String prop = getEmailNodeProperty(c);


        if (StringUtils.isEmpty(prop)) return new String[0];

        String expand = new EnvVars(EnvVars.masterEnvVars).expand(prop);
        return expand.split(" ");
    }

    private @Nonnull String getEmailNodeProperty(Computer c) {
        for (NodeProperty p : c.getNode().getNodeProperties()) {
            if (p instanceof EmailNodeProperty) return ((EmailNodeProperty) p).getEmail();
        }
        return "";
    }

    private void sendEmail(String email, String name, boolean onlineCalled, boolean masterStart) throws MessagingException {

        Session newSession = mailer.createSession();
        MimeMessage msg = new MimeMessage(newSession);
        if (masterStart) {
            msg.setSubject("The master has been restarted.");
            msg.setText("The master was restarted and the node (#{name}) is offline. If you don't recieve an online message it is probably offline still.");
        }
        else {
            if (onlineCalled) {
                msg.setSubject("Slave back online");
                msg.setText("The connection to the slave (#{name}) was restored.");
            } else {
                msg.setSubject("Slave Connection lost");
                msg.setText("The connection to the slave (#{name}) you are responsible for, has been lost.");
            }
        }

        msg.setFrom(new InternetAddress(JenkinsLocationConfiguration.get().getAdminAddress()));
        msg.setRecipient(TO, new InternetAddress(email));
        Transport.send(msg);
    }

    private static final Logger LOGGER = Logger.getLogger(EmailComputerListener.class.getName());

}
