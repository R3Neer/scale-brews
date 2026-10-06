package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.classfile.*;
import java.lang.constant.*;
import java.util.*;
public final class S59CodeFoundationProof {
 static int checks;
 static void check(boolean b,String reason){checks++;if(!b)throw new AssertionError("S59 S00 "+reason);}
 static void reject(Runnable f,String reason){try{f.run();throw new AssertionError("S59 S00 admitted "+reason);}catch(IllegalArgumentException expected){checks++;}}
 static MethodModel method(java.util.function.Consumer<CodeBuilder> body){byte[] bytes=ClassFile.of(ClassFile.StackMapsOption.DROP_STACK_MAPS).build(ClassDesc.of("audit.FiniteLabels"),b->b.withMethodBody("x",MethodTypeDesc.of(ConstantDescs.CD_void,ConstantDescs.CD_boolean),ClassFile.ACC_STATIC,body));return ClassFile.of().parse(bytes).methods().getFirst();}
 public static void main(String[] args)throws Exception{
  var forward=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();var z=c.newLabel();c.iload(0);c.ifeq(a);c.nop();c.goto_(z);c.labelBinding(a);c.nop();c.labelBinding(z);c.return_();}));
  check(forward.instructions.size()==6,"labels excluded from instruction ordinal");forward.op(Opcode.ILOAD_0);check(forward.branch(Opcode.IFEQ)==4,"forward conditional maps label to instruction4");forward.op(Opcode.NOP);check(forward.branch(Opcode.GOTO)==5,"forward goto maps return instruction5");forward.op(Opcode.NOP);forward.op(Opcode.RETURN);forward.end();checks++;
  var adjacent=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();var b=c.newLabel();c.iload(0);c.ifne(a);c.return_();c.labelBinding(a);c.labelBinding(b);c.return_();}));adjacent.op(Opcode.ILOAD_0);check(adjacent.branch(Opcode.IFNE)==3,"adjacent labels same instruction ordinal");
  var backward=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();c.labelBinding(a);c.nop();c.goto_(a);}));backward.op(Opcode.NOP);check(backward.branch(Opcode.GOTO)==0,"parser exposes backward target; new verifier must reject");
  var terminal=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();c.goto_(a);c.return_();c.labelBinding(a);}));check(terminal.branch(Opcode.GOTO)==2,"end label maps instructions.size, not valid executable target");
  var mismatch=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();c.goto_(a);c.labelBinding(a);c.return_();}));reject(()->mismatch.branch(Opcode.IFNE),"wrong branch opcode");
  var missing=new OriginalModelPrograms.Code(method(c->{var a=c.newLabel();c.goto_(a);c.labelBinding(a);c.return_();}));var labels=OriginalModelPrograms.Code.class.getDeclaredField("labels");labels.setAccessible(true);((Map<?,?>)labels.get(missing)).clear();reject(()->missing.branch(Opcode.GOTO),"unresolved target");
  var suffix=new OriginalModelPrograms.Code(method(c->{c.return_();c.nop();}));suffix.op(Opcode.RETURN);reject(suffix::end,"suffix after return remains visible");suffix.op(Opcode.NOP);suffix.end();reject(suffix::take,"truncation");
  check(new OriginalModelPrograms.Code(method(c->{for(int i=0;i<511;i++)c.nop();c.return_();})).instructions.size()==512,"512 instruction cap inclusive");reject(()->new OriginalModelPrograms.Code(method(c->{for(int i=0;i<512;i++)c.nop();c.return_();})),"513 rejected before retention");
  check(OriginalModelPrograms.MAX_CLASSES==64&&OriginalModelPrograms.MAX_TOTAL_BYTES==8388608,"resource bounds retained");
  System.out.println("S59_S00_CODE_PASS checks="+checks+" actualLabels forward/backward/end/opcode/missing suffix512/513");
 }
}
