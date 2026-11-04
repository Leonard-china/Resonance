import org.junit.runner.*;
import org.junit.runner.notification.*;
import org.json.*;
public class AuditJUnitRunner extends RunListener {
    final JSONArray assumptions = new JSONArray(), failures = new JSONArray();
    public void testAssumptionFailure(Failure f) { assumptions.put(f.getDescription().toString()); }
    public void testFailure(Failure f) { failures.put(new JSONObject().put("test", f.getDescription().toString()).put("trace", f.getTrace())); }
    public static void main(String[] args) throws Exception {
        AuditJUnitRunner listener = new AuditJUnitRunner(); JUnitCore core = new JUnitCore(); core.addListener(listener);
        Class<?>[] classes = new Class<?>[args.length]; for(int i=0;i<args.length;i++) classes[i] = Class.forName(args[i]);
        Result r = core.run(classes);
        System.out.println(new JSONObject().put("run", r.getRunCount()).put("failures", listener.failures).put("assumptionSkips", listener.assumptions).put("ignored", r.getIgnoreCount()).put("timeMs", r.getRunTime()).toString(2));
        if (!r.wasSuccessful()) System.exit(1);
    }
}
