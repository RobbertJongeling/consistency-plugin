//package org.tap4j.plugin;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//import javax.servlet.ServletException;
//
//import org.kohsuke.stapler.DataBoundConstructor;
//import org.kohsuke.stapler.HttpResponse;
//import org.kohsuke.stapler.StaplerRequest;
//
//import hudson.Extension;
//import hudson.XmlFile;
//import hudson.model.AbstractDescribableImpl;
//import hudson.model.Descriptor;
//import hudson.util.FormApply;
//import hudson.util.ListBoxModel;
//import jenkins.model.Jenkins;
//
//@Extension
//public final class ConsistencyRuleList {
//	public String getDescription() {
//		return "Show a heterogeneous list of subitems with different data bindings for radio buttons and checkboxes";
//	}
//
//	public XmlFile getConfigFile() {
//		return new XmlFile(new File(Jenkins.getInstance().getRootDir(), "stuff.xml"));// TODO stuff.xml?????
//	}
//
//	private Config config;
//
//	public ConsistencyRuleList() throws IOException {
//        XmlFile xml = getConfigFile();
//        if (xml.exists()) {
//            xml.unmarshal(this);
//        }
//    }
//
//	public Config getConfig() {
//		return config;
//	}
//
//	public void setConfig(Config config) {
//		this.config = config;
//	}
//
//	public HttpResponse doConfigSubmit(StaplerRequest req) throws ServletException, IOException {
//		config = null; // otherwise bindJSON will never clear it once set
//		req.bindJSON(this, req.getSubmittedForm());
//		getConfigFile().write(this);
//		return FormApply.success(".");
//	}
//
//	public static final class Config extends AbstractDescribableImpl<Config> {
//
//		private final List<Entry> entries;
//
//		@DataBoundConstructor
//		public Config(List<Entry> entries) {
//			this.entries = entries != null ? new ArrayList<Entry>(entries) : Collections.<Entry>emptyList();
//		}
//
//		public List<Entry> getEntries() {
//			return Collections.unmodifiableList(entries);
//		}
//
//		@Extension
//		public static class DescriptorImpl extends Descriptor<Config> {
//			@Override
//			public String getDisplayName() {
//				return "";
//			}
//		}
//
//	}
//
//	public static abstract class Entry extends AbstractDescribableImpl<Entry> {
//	}
//
//	public static final class ConsistencyRuleEntry extends Entry {
//
//		private final String A;
//		private final String B;
//		private final String strictness;
//
//		@DataBoundConstructor
//		public ConsistencyRuleEntry(String A, String B, String strictness) {
//			this.A = A;
//			this.B = B;
//			this.strictness = strictness;
//		}
//
//		public String getA() {
//			return A;
//		}
//
//		public String getB() {
//			return B;
//		}
//
//		public String getStrictness() {
//			return strictness;
//		}
//
//		@Extension
//		public static class DescriptorImpl extends Descriptor<Entry> {
//			@Override
//			public String getDisplayName() {
//				return "Consistency Check";
//			}
//
//			public ListBoxModel doFillChoiceItems() {
//				return new ListBoxModel().add("strict").add("medium").add("loose");
//			}
//		}
//	}
//}
