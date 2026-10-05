package io.github.r3neer.scalebrews.client.collision.preparation;
import java.lang.classfile.*;
import java.lang.constant.*;
import java.io.*;
import java.util.*;
public final class S51AdversarialParserProof {
 static int checks;
 static void yes(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void reject(Runnable r,String reason){checks++;try{r.run();throw new AssertionError("Accepted "+reason);}catch(IllegalArgumentException e){if(!e.getMessage().contains(reason))throw e;}}
 static byte[] bytes(String name,int methods,int nops){return ClassFile.of().build(ClassDesc.of(name),b->{for(int i=0;i<methods;i++) b.withMethodBody("m"+i,MethodTypeDesc.of(ConstantDescs.CD_void),ClassFile.ACC_PUBLIC,c->{for(int j=0;j<nops;j++)c.nop();c.return_();});});}
 static MethodModel method(java.util.function.Consumer<CodeBuilder> action){return ClassFile.of().parse(ClassFile.of().build(ClassDesc.of("probe.Code"),b->b.withMethodBody("test",MethodTypeDesc.of(ConstantDescs.CD_void),ClassFile.ACC_PUBLIC,action))).methods().getFirst();}
 static final class Loader extends ClassLoader {
  Map<String,byte[]> resources=new HashMap<>(); int reads;
  Class<?> define(String name,byte[] b){resources.put(name.replace('.','/')+".class",b);return defineClass(name,b,0,b.length);}
  public InputStream getResourceAsStream(String name){byte[] b=resources.get(name);if(b!=null){reads++;return new ByteArrayInputStream(b);}return super.getResourceAsStream(name);}
 }
 public static void main(String[] args) throws Exception{
  reject(()->new OriginalModelPrograms.Code(method(c->{var start=c.newBoundLabel();c.nop();var end=c.newBoundLabel();c.return_();var handler=c.newBoundLabel();c.astore(1);c.return_();c.exceptionCatchAll(start,end,handler);})),"exception_handlers"); var exact=new OriginalModelPrograms.Code(method(c->{for(int i=0;i<511;i++)c.nop();c.return_();}));yes(exact.instructions.size()==512,"exact instruction cap");
  reject(()->new OriginalModelPrograms.Code(method(c->{for(int i=0;i<512;i++)c.nop();c.return_();})),"instruction_limit");
  var tail=new OriginalModelPrograms.Code(method(c->{c.nop();c.return_();}));tail.op(Opcode.NOP);reject(tail::end,"extra_program");tail.op(Opcode.RETURN);tail.end();reject(tail::take,"truncated_program");
  reject(()->new OriginalModelPrograms.Code(method(c->{c.fload(1);c.pop();c.return_();})).load(1),"program_local");
  reject(()->new OriginalModelPrograms.Code(method(c->{c.aload(1);c.pop();c.return_();})).loadFloat(1),"program_float_local");
  var branch=new OriginalModelPrograms.Code(method(c->{var t=c.newLabel();c.aload(0);c.ifnonnull(t);c.return_();c.labelBinding(t);c.return_();}));branch.load(0);yes(branch.branch(Opcode.IFNONNULL)==3,"exact branch target");
  Loader loader=new Loader();var pass=new OriginalModelPrograms(loader);
  Class<?> first=null;for(int i=0;i<64;i++){String n="probe.P"+i;var cls=loader.define(n,bytes(n,1,0));pass.model(cls);if(i==0)first=cls;}
  yes(pass.digests().size()==64,"class cap");int reads=loader.reads;pass.model(first);yes(loader.reads==reads,"identity cache avoids repeated IO");
  var over=loader.define("probe.P64",bytes("probe.P64",1,0));reject(()->pass.model(over),"class_limit");
  var max=loader.define("probe.Method1024",bytes("probe.Method1024",1024,0));yes(new OriginalModelPrograms(loader).model(max).methods().size()==1024,"method cap");
  var many=loader.define("probe.Method1025",bytes("probe.Method1025",1025,0));reject(()->new OriginalModelPrograms(loader).model(many),"class_identity_or_method_limit");
  var cls=loader.define("probe.Bytes",bytes("probe.Bytes",1,0));loader.resources.put("probe/Bytes.class",new byte[1_048_577]);reject(()->new OriginalModelPrograms(loader).model(cls),"byte_limit");
  var total=new OriginalModelPrograms(loader);var totalField=OriginalModelPrograms.class.getDeclaredField("totalBytes");totalField.setAccessible(true);int length=loader.resources.get("probe/P0.class").length;totalField.setInt(total,8_388_608-length);total.model(first);yes(totalField.getInt(total)==8_388_608,"exact cumulative byte cap");reject(()->total.model(over),"byte_limit"); var wrong=loader.define("probe.Wrong",bytes("probe.Wrong",1,0));loader.resources.put("probe/Wrong.class",bytes("probe.Other",1,0));reject(()->new OriginalModelPrograms(loader).model(wrong),"class_identity_or_method_limit");final Class<?> pinned=first;reject(()->new OriginalModelPrograms(loader).pinned(pinned,"0".repeat(64)),"original_dependency_changed");
  System.out.println("S51_PARSER_PASS checks="+checks+" reads="+loader.reads);
 }
}