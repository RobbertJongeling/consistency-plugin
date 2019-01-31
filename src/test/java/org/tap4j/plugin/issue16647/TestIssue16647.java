//package org.tap4j.plugin.issue16647;
//
//import hudson.Launcher;
//import hudson.model.BuildListener;
//import hudson.model.FreeStyleBuild;
//import hudson.model.AbstractBuild;
//import hudson.model.FreeStyleProject;
//
//import static org.junit.Assert.assertEquals;
//
//import java.io.IOException;
//import java.util.concurrent.ExecutionException;
//
//import org.junit.Rule;
//import org.junit.Test;
//import org.jvnet.hudson.test.JenkinsRule;
//import org.jvnet.hudson.test.TestBuilder;
//import org.tap4j.plugin.ConsistencyChecker;
//import org.tap4j.plugin.TapTestResultAction;
//import org.tap4j.plugin.model.TapStreamResult;
//import org.tap4j.plugin.model.TapTestResultResult;
//
//public class TestIssue16647 {
//
//    @Rule
//    public JenkinsRule jenkins = new JenkinsRule();
//
//    @Test
//    public void testDurationMs() throws IOException, InterruptedException, ExecutionException {
//        FreeStyleProject project = jenkins.createProject(FreeStyleProject.class, "tap-bug-16647");
//
//        final String tap = "1..2\n" +
//                "ok 1 - Input file opened\n" +
//                "not ok 2 - First line of the input valid\n" +
//                "  ---\n" +
//                "    duration_ms: 100660.00\n" +
//                "  ...\n";
//
//        project.getBuildersList().add(new TestBuilder() {
//            @Override
//            public boolean perform(AbstractBuild<?, ?> build, Launcher arg1,
//                    BuildListener arg2) throws InterruptedException, IOException {
//                build.getWorkspace().child("result.tap").write(tap,"UTF-8");
//                return true;
//            }
//        });
//
//        ConsistencyChecker publisher = new ConsistencyChecker(false, false);
//        project.getPublishersList().add(publisher);
//        project.save();
//        FreeStyleBuild build = (FreeStyleBuild) project.scheduleBuild2(0).get();
//
//        TapTestResultAction action = build.getAction(TapTestResultAction.class);
//        TapStreamResult result = (TapStreamResult) action.getResult();
//
//        TapTestResultResult[] results = result.getChildren().toArray(new TapTestResultResult[0]);
//        assertEquals(100.66f, results[1].getDuration(), /* delta */ 0.0001f);
//    }
//
//}
