package io.github.r3neer.scalebrews.client.collision.preparation;
import java.util.*;import java.lang.reflect.*;
public final class S59FiniteBoundaryProof {
 public static final class Forward {public void setupAnim(boolean value){if(value) System.nanoTime();}}
 public static final class Loop {public void setupAnim(boolean value){while(value)value=false;}}
 public static final class Compare {public void setupAnim(float value){if(value>0)System.nanoTime();}}
 static int checks;
 static void check(boolean b,String m){checks++;if(!b)throw new AssertionError("S59 ADV "+m);}
 static Object field(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object value)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,value);}
 static OriginalLinearPosePreparation.FinitePass pass(int work)throws Exception{var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);var p=(OriginalLinearPosePreparation.FinitePass)u.allocateInstance(OriginalLinearPosePreparation.FinitePass.class);set(p,"resources",new OriginalModelPrograms(S59FiniteBoundaryProof.class.getClassLoader()));set(p,"codes",new LinkedHashMap<>());set(p,"coverage",new LinkedHashMap<>());set(p,"work",work);return p;}
 static void reject(Runnable action,String reason){try{action.run();throw new AssertionError("S59 ADV admitted "+reason);}catch(IllegalArgumentException x){check(x.getMessage().contains(reason),"wrong rejection "+reason+": "+x);}catch(RuntimeException x){throw new AssertionError("S59 ADV rejection bypassed "+reason,x);}}
 public static void main(String[] args)throws Exception{
  check(OriginalFinitePosePreparation.MAX_BOOLEANS==5&&OriginalFinitePosePreparation.MAX_ROWS==32,"finite declared caps5/32");
  var exhausted=pass(1048576);reject(()->exhausted.code(Forward.class,"(Z)V"),"finite_work_limit");check(((Map<?,?>)field(exhausted,"codes")).isEmpty()&&((Map<?,?>)field(exhausted,"coverage")).isEmpty(),"work cap before insertion");
  var p=pass(0);var a=p.code(Forward.class,"(Z)V");int n=a.instructions.size();check(p.work==n,"method inventory charged once per attempt");a.take();var b=p.code(Forward.class,"(Z)V");check(a.next==1&&b.next==0&&a.instructions==b.instructions,"independent cursors shared immutable inventory");check(p.work==2*n,"repeated attempt charged globally");
  var exact=pass(1048576-n);exact.code(Forward.class,"(Z)V");check(exact.work==1048576,"exact total cap inclusive");reject(()->exact.code(Forward.class,"(Z)V"),"finite_work_limit");
  var below=pass(1048577-n);reject(()->below.code(Forward.class,"(Z)V"),"finite_work_limit");check(((Map<?,?>)field(below,"codes")).isEmpty(),"method exceeding remaining budget not inserted");
  var loop=pass(0);reject(()->loop.code(Loop.class,"(Z)V"),"finite_branch_target");var compare=pass(0);reject(()->compare.code(Compare.class,"(F)V"),"finite_branch_opcode");
  var uncovered=pass(0);uncovered.code(Forward.class,"(Z)V");reject(uncovered::finish,"finite_uncovered_program");
  var compiler=Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation$Compiler");var booleanClass=Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation$BooleanValue");var ctor=booleanClass.getDeclaredConstructors()[0];ctor.setAccessible(true);var value=ctor.newInstance(true);var method=compiler.getDeclaredMethod("booleanValue",Object.class);method.setAccessible(true);check(method.invoke(null,value)==value,"exact typed boolean identity");
  try{method.invoke(null,Float.valueOf(1));throw new AssertionError("S59 ADV numeric threshold fabricated boolean");}catch(InvocationTargetException x){check(x.getCause() instanceof IllegalArgumentException,"typed boolean rejects float");}
  System.out.println("S59_FINITE_BOUNDARIES_PASS checks="+checks+" budget/targets/type/coverage/cursors caps5/32");
 }
}
