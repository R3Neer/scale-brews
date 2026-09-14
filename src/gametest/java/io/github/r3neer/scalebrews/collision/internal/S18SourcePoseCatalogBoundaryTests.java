package io.github.r3neer.scalebrews.collision.internal;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.r3neer.scalebrews.collision.api.AnatomyApi;
import io.github.r3neer.scalebrews.collision.geometry.ModelGeometry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import org.joml.Matrix4f;

/** SourcePose is revision material: codecs, wire identity and atomic rejection must preserve it exactly. */
public final class S18SourcePoseCatalogBoundaryTests {
    private static final String MODEL_ID = "scalebrews_test:s18_source_pose_catalog";
    private static final float X=.3f,Y=.8f,Z=-.6f;
    /** Protocol-v5 capability mask frozen before exact ModelPart source-pose semantics existed. */
    private static final long FROZEN_V5_CAPABILITIES=(1L<<9)-1;

    @GameTest
    public void equivalentMatricesWithDifferentSourcePoseHaveDifferentCatalogIdentity(GameTestHelper h) {
        var canonical = new ModelGeometry.SourcePose(0,0,0,X,Y,Z,1,1,1);
        var alternate = new ModelGeometry.SourcePose(0,0,0,
            X+(float)Math.PI,(float)Math.PI-Y,Z+(float)Math.PI,1,1,1);
        var fixedTransform = ModelGeometry.values(canonical.matrix());
        var first = geometry(canonical,fixedTransform);
        var second = geometry(alternate,fixedTransform);

        h.assertTrue(first.parts().getFirst().transform().equals(second.parts().getFirst().transform()),
            "Holdout precondition: both models must have byte-identical neutral rest matrices");
        h.assertTrue(!first.parts().getFirst().sourcePose().equals(second.parts().getFirst().sourcePose()),
            "Holdout precondition: source-pose identities must remain distinct despite the equal matrix");

        var epoch=UUID.randomUUID();
        var firstPackets=AnatomyCatalogTransfer.encode(epoch,1,Map.of(MODEL_ID,first));
        var secondPackets=AnatomyCatalogTransfer.encode(epoch,1,Map.of(MODEL_ID,second));
        h.assertTrue(!firstPackets.getFirst().digest().equals(secondPackets.getFirst().digest()),
            "Catalog digest must include SourcePose because equal rest matrices can have different Mojang futures");

        var codecJson=AnatomyCodecs.GEOMETRY.encodeStart(JsonOps.INSTANCE,first).getOrThrow();
        var codecRoundTrip=AnatomyCodecs.GEOMETRY.parse(JsonOps.INSTANCE,codecJson).getOrThrow();
        h.assertTrue(first.equals(codecRoundTrip),
            "Datapack geometry codec must round-trip SourcePose exactly");

        var receiver=new AnatomyCatalogTransfer();
        for(var packet:firstPackets)receiver.accept(packet);
        var received=receiver.snapshot().models().get(MODEL_ID);
        h.assertTrue(first.equals(received),
            "Authoritative catalog transfer must preserve SourcePose exactly");
        h.assertTrue(canonical.equals(received.parts().getFirst().sourcePose()),
            "Transferred model must retain the exact prepared source pose, not a matrix decomposition");
        h.succeed();
    }

    @GameTest
    public void sourcePoseSemanticChangeMustBeNegotiated(GameTestHelper h) {
        var pose=new ModelGeometry.SourcePose(0,0,0,X,Y,Z,1,1,1);
        var model=geometry(pose,ModelGeometry.values(pose.matrix()));
        boolean protocolBumped=AnatomyApi.PROTOCOL_VERSION>5;
        boolean capabilityNegotiated=AnatomyApi.capabilities()!=FROZEN_V5_CAPABILITIES;
        boolean geometryFormatBumped=model.format()>2;
        h.assertTrue(protocolBumped || capabilityNegotiated || geometryFormatBumped,
            "Exact SourcePose changes physical pose semantics: a protocol-v5 peer with the frozen capability mask must not accept it as unchanged ModelGeometry format 2. Negotiate via protocol, capability or geometry-format versioning");
        h.succeed();
    }

    @GameTest
    public void incoherentSourcePoseCandidateIsRejectedAtomically(GameTestHelper h) {
        var pose=new ModelGeometry.SourcePose(0,0,0,X,Y,Z,1,1,1);
        var model=geometry(pose,ModelGeometry.values(pose.matrix()));
        var epoch=UUID.randomUUID();
        var receiver=new AnatomyCatalogTransfer();
        for(var packet:AnatomyCatalogTransfer.encode(epoch,1,Map.of(MODEL_ID,model)))receiver.accept(packet);
        var accepted=receiver.snapshot();
        h.assertTrue(receiver.revision()==1 && receiver.ready(),
            "Precondition: revision 1 must be fully accepted before injecting the malformed replacement");

        byte[] valid=AnatomyCatalogTransfer.serializedBundle(Map.of(MODEL_ID,model),List.of());
        var bundle=JsonParser.parseString(new String(valid,StandardCharsets.UTF_8)).getAsJsonObject();
        var sourcePose=bundle.getAsJsonObject("models").getAsJsonObject(MODEL_ID)
            .getAsJsonArray("parts").get(0).getAsJsonObject().getAsJsonObject("sourcePose");
        sourcePose.addProperty("xRot",X+.5f);
        byte[] corrupt=bundle.toString().getBytes(StandardCharsets.UTF_8);

        boolean rejected=false;
        try {
            for(var packet:packets(epoch,2,corrupt))receiver.accept(packet);
        } catch(RuntimeException expected) {
            rejected=containsCause(expected,"Source pose does not match local transform");
            if(!rejected)throw expected;
        }
        h.assertTrue(rejected,
            "SourcePose that no longer reconstructs the stored local transform must reject the entire candidate revision");
        h.assertTrue(receiver.snapshot()==accepted && receiver.revision()==1,
            "Malformed SourcePose must preserve the exact previously accepted snapshot and revision");

        receiver.rejectPending();
        h.assertTrue(receiver.snapshot()==accepted && receiver.ready(),
            "Rejecting the malformed pending revision must restore READY on the unchanged accepted snapshot");
        h.succeed();
    }

    private static boolean containsCause(Throwable error,String text) {
        for(Throwable current=error;current!=null;current=current.getCause())
            if(current instanceof IllegalArgumentException && current.getMessage()!=null && current.getMessage().contains(text))return true;
        return false;
    }

    private static ModelGeometry geometry(ModelGeometry.SourcePose pose,List<Float> transform) {
        var part=new ModelGeometry.Part("root",null,transform,pose);
        var piece=new ModelGeometry.Piece("root/cube_0","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null);
        return new ModelGeometry(2,MODEL_ID,"26.2",List.of(part),List.of(piece),ModelGeometry.values(new Matrix4f()));
    }

    private static List<AnatomyCatalogPayload> packets(UUID epoch,long revision,byte[] bytes) {
        int count=(bytes.length+AnatomyCatalogPayload.CHUNK-1)/AnatomyCatalogPayload.CHUNK;
        String digest=sha256(bytes);
        var result=new ArrayList<AnatomyCatalogPayload>(count);
        for(int index=0;index<count;index++) {
            int start=index*AnatomyCatalogPayload.CHUNK;
            int end=Math.min(bytes.length,start+AnatomyCatalogPayload.CHUNK);
            result.add(new AnatomyCatalogPayload(epoch,AnatomyApi.PROTOCOL_VERSION,AnatomyApi.capabilities(),revision,
                index,count,bytes.length,digest,java.util.Arrays.copyOfRange(bytes,start,end)));
        }
        return result;
    }

    private static String sha256(byte[] bytes) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
