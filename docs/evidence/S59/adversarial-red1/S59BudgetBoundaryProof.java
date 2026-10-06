package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.reflect.*;import java.util.*;
public final class S59BudgetBoundaryProof {
 public void setupAnim(Object value) {}
 static Object value(Object o,String n)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{var f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 public static void main(String[] args)throws Exception{
  var uf=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);var unsafe=(sun.misc.Unsafe)uf.get(null);
  var p=(OriginalLinearPosePreparation.FinitePass)unsafe.allocateInstance(OriginalLinearPosePreparation.FinitePass.class);
  set(p,"resources",new OriginalModelPrograms(S59BudgetBoundaryProof.class.getClassLoader()));set(p,"codes",new LinkedHashMap<>());set(p,"coverage",new LinkedHashMap<>());set(p,"work",OriginalLinearPosePreparation.FinitePass.MAX_WORK);
  try{p.code(S59BudgetBoundaryProof.class,"(Ljava/lang/Object;)V");throw new AssertionError("S59 ADV exhausted work admitted method");}catch(IllegalArgumentException expected){if(!expected.getMessage().contains("finite_work_limit"))throw expected;}
  int admitted=((Map<?,?>)value(p,"codes")).size(), coverage=((Map<?,?>)value(p,"coverage")).size();System.out.println("S59_BUDGET_BOUNDARY work="+p.work+" templatesAfterReject="+admitted+" coverageAfterReject="+coverage);
  if(admitted!=0||coverage!=0)throw new AssertionError("S59 ADV work cap checked after admitting next method and verifying its instructions");
 }
}
