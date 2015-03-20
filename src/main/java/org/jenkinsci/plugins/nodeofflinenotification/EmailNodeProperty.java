package org.jenkinsci.plugins.nodeofflinenotification;

import hudson.Extension;
import hudson.model.Slave;
import hudson.slaves.NodeProperty;
import hudson.slaves.NodePropertyDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class EmailNodeProperty extends NodeProperty<Slave> {

    public final String email;

    @DataBoundConstructor
    public EmailNodeProperty(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    @Extension
    public static class DescriptorImpl extends NodePropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Node Offline Email Notification";
        }


    }
}
