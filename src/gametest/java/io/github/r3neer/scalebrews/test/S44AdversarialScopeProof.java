package io.github.r3neer.scalebrews.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.*;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

/** Scope lifecycle oracle; physical replacement is exercised by separate live cases. */
public final class S44AdversarialScopeProof implements FabricClientGameTest {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Object call(Method m,Object x){
        try{return m.invoke(null,x);}catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    @Override public void runTest(ClientGameTestContext context){
        var world=context.worldBuilder().create();var server=world.getServer();
        try {
            server.runOnServer(s->{
                try {
                    var owner=Class.forName("io.github.r3neer.scalebrews.collision.integration.MovingBodyContext");
                    var enter=owner.getMethod("enter",Entity.class);
                    var exit=owner.getMethod("exit",Entity.class);
                    var a=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                    var b=EntityTypes.COW.create(s.overworld(),EntitySpawnReason.COMMAND);
                    require(a!=null && b!=null,"Scope fixture absent");
                    Object root=call(enter,a);
                    require(root==null,"Prior operation leaked a scope into this server task");
                    try {
                        Object old=call(enter,b);
                        require(old==a,"Nested enter lost exact outer entity identity");
                        try {throw new IllegalStateException("deliberate-test-only-unwind");}
                        catch(IllegalStateException expected){require(expected.getMessage().equals("deliberate-test-only-unwind"),"Wrong exception");}
                        finally {call(exit,old);}
                        Object probe=call(enter,b);
                        require(probe==a,"Exceptional nested operation failed to restore outer scope");
                        call(exit,probe);
                        var failure=new AtomicReference<Throwable>();
                        var worker=new Thread(()->{
                            try {
                                Object other=call(enter,b);
                                require(other==null,"Moving identity escaped across threads");
                                try {require(call(enter,a)==b,"Worker nested identity incorrect");}
                                finally {call(exit,b);call(exit,other);}
                            }catch(Throwable t){failure.set(t);}
                        },"s44-scope-fixture");
                        worker.start();worker.join(5000);
                        require(!worker.isAlive(),"Scope worker timed out");
                        if(failure.get()!=null)throw new AssertionError("Scope worker failed",failure.get());
                        Object afterWorker=call(enter,b);
                        require(afterWorker==a,"Worker modified server scope identity");
                        call(exit,afterWorker);
                    } finally {call(exit,root);}
                    Object finalProbe=call(enter,b);
                    try {require(finalProbe==null,"Root scope failed to retire");}
                    finally {call(exit,finalProbe);}
                    System.out.println("S44_SCOPE_PASS owner="+owner.getName());
                } catch(ReflectiveOperationException|InterruptedException e){throw new AssertionError(e);}
            });
        } finally {world.close();}
    }
}
