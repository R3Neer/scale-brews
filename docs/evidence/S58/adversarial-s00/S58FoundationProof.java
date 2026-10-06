package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.classfile.*;import java.lang.constant.*;import java.lang.reflect.*;import java.util.*;
public final class S58FoundationProof {
 static int assertions;static void check(boolean b,String m){assertions++;if(!b)throw new AssertionError("S58 S00 "+m);}
 static void reject(Runnable r,String m){try{r.run();throw new AssertionError("S58 S00 admitted "+m);}catch(IllegalArgumentException expected){assertions++;}}
 static Object invoke(Method m,Object...args){try{return m.invoke(null,args);}catch(InvocationTargetException x){if(x.getCause() instanceof RuntimeException e)throw e;throw new AssertionError(x);}catch(Exception e){throw new AssertionError(e);}}
 static Method helper(Class<?> c,String n,Class<?>...p)throws Exception {var m=c.getDeclaredMethod(n,p);m.setAccessible(true);return m;}
 static MethodModel program(int n){var bytes=ClassFile.of().build(ClassDesc.of("audit.Code"),b->b.withMethodBody("x",MethodTypeDesc.of(ConstantDescs.CD_void),ClassFile.ACC_STATIC,c->{for(int i=0;i<n-1;i++)c.nop();c.return_();}));return ClassFile.of().parse(bytes).methods().getFirst();}
 public static void main(String[] args)throws Exception{
  var c=Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation$Compiler");
  var ref=Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation$Ref");var num=Class.forName("io.github.r3neer.scalebrews.client.collision.preparation.OriginalLinearPosePreparation$Number");
  var rc=ref.getDeclaredConstructors()[0];rc.setAccessible(true);var nc=num.getDeclaredConstructors()[0];nc.setAccessible(true);
  Object a=rc.newInstance("root/part",false), state=rc.newInstance(null,true), scalar=nc.newInstance(2,false), promoted=nc.newInstance(3,true);
  for(var type:List.of(ref,num)){check(type.isRecord(),"immutable symbolic record");for(var f:type.getDeclaredFields())check(Modifier.isFinal(f.getModifiers()),"immutable field");}
  var push=helper(c,"push",List.class,Object.class);var pop=helper(c,"pop",List.class);var numeric=helper(c,"numeric",Object.class,boolean.class);var part=helper(c,"part",Object.class);List<Object> stack=new ArrayList<>();
  invoke(push,stack,a);invoke(push,stack,a);check(invoke(pop,stack)==a&&invoke(pop,stack)==a,"exact symbolic receiver identity");reject(()->invoke(pop,stack),"underflow");
  for(int i=0;i<32;i++)invoke(push,stack,scalar);check(stack.size()==32,"exact stack cap");reject(()->invoke(push,stack,scalar),"stack overflow");check(stack.size()==32,"cap before growth");
  check(invoke(numeric,scalar,false)==scalar,"float identity");check(invoke(numeric,promoted,true)==promoted,"promoted identity");reject(()->invoke(numeric,promoted,false),"double as category1");reject(()->invoke(numeric,scalar,true),"float as promoted");reject(()->invoke(numeric,a,false),"reference numeric");
  check(invoke(part,a)==a,"part identity");reject(()->invoke(part,state),"state part");reject(()->invoke(part,scalar),"number part");
  var code=new OriginalModelPrograms.Code(program(512));check(code.instructions.size()==512,"exact instruction cap");reject(()->new OriginalModelPrograms.Code(program(513)),"instruction cap");
  var shortCode=new OriginalModelPrograms.Code(program(2));shortCode.op(Opcode.NOP);reject(shortCode::end,"unconsumed suffix");shortCode.op(Opcode.RETURN);shortCode.end();reject(shortCode::take,"truncated");
  check(OriginalModelPrograms.MAX_CLASSES==64&&OriginalModelPrograms.MAX_TOTAL_BYTES==8388608,"resource bounds unchanged");
  System.out.println("S58_S00_PASS assertions="+assertions+" actualStackHelpers category1/2 identity cap32/33 instruction512/513 fullConsumption resourcesBounds");
 }
}
