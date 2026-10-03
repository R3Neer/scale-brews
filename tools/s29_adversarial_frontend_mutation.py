"""Prepare one compiled frontend mutant outside production sources.

Use the printed Gradle properties with tools/s29_adversarial.init.gradle.
A compiler failure is never evidence of an oracle kill.
"""
import argparse
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('mutation', choices=['inference', 'binding', 'tracking', 'epoch', 'dimension', 'nearest', 'material-barrier', 'bound', 'cross-support'])
parser.add_argument('destination', type=Path)
args = parser.parse_args()
repo = Path(__file__).resolve().parents[1]
client = 'src/client/java/io/github/r3neer/scalebrews/client/collision/network/AnatomyClientNetworking.java'
inbox = 'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyCertifiedIntervalInbox.java'
history = 'src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyFrameHistory.java'
source = client
changes = []
if args.mutation == 'cross-support':
    anchor = '        var history=frames.get(support.getUUID());\n        if(certificate==null || history==null'
    replacement = '''        var history=frames.get(support.getUUID());
        if(certificate==null) {
            for(var other:poseLevel.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,support.getBoundingBox().inflate(16),entity->entity!=support))
                if(certifiedIntervals.exact(other.getUUID(),materialSerial,clientTick).isPresent())
                    return presentationInterval(other,materialSerial,fraction);
        }
        if(certificate==null || history==null'''
    changes = [(anchor, replacement)]
elif args.mutation == 'inference':
    helper = '''    private static AnatomyMaterialIntervalPayload adversarialInferredCertificate(net.minecraft.world.entity.LivingEntity support,long serial) {
        var history=frames.get(support.getUUID());
        if(history==null || history.current()==null)return null;
        var b=history.current();var a=history.packet(b.frameSerial()-1).orElse(null);
        if(a==null)return null;
        return new AnatomyMaterialIntervalPayload(b.epoch(),b.revision(),b.dimension(),b.entityId(),b.entity(),b.bindingGeneration(),b.trackingGeneration(),serial,
            a.frameSerial(),b.frameSerial(),a.authorityTick(),b.authorityTick(),a.jointSampleTick(),b.jointSampleTick());
    }

'''
    changes = [('certifiedIntervals.exact(support.getUUID(),materialSerial,clientTick).orElse(null)',
                'certifiedIntervals.exact(support.getUUID(),materialSerial,clientTick).orElseGet(()->adversarialInferredCertificate(support,materialSerial))'),
               ('    private static boolean certificateMatches(', helper + '    private static boolean certificateMatches(')]
elif args.mutation in ('binding', 'tracking'):
    axis = args.mutation + 'Generation'
    changes = [(f'certificate.{axis}()==packet.{axis}()', 'true')]
elif args.mutation == 'epoch':
    changes = [('certificate.epoch().equals(packet.epoch())', 'true'),
               ('!packet.epoch().equals(transfer.epoch())', 'false')]
elif args.mutation == 'dimension':
    changes = [('certificate.dimension().equals(packet.dimension())', 'true'),
               ('!packet.dimension().equals(level.dimension().identifier())', 'false')]
elif args.mutation == 'nearest':
    source = inbox
    changes = [('entry.payload().materialSerial()==materialSerial', 'true')]
elif args.mutation == 'bound':
    source = inbox
    changes = [('state.entries.size()>=MAX_PER_SUPPORT', 'false')]
elif args.mutation == 'material-barrier':
    source = history
    changes = [('!next.available() || current!=null && (!current.available() || current.gravity()!=next.gravity())', 'false')]
text = (repo / source).read_text(encoding='utf-8')
for original, replacement in changes:
    if original not in text:
        raise SystemExit(f'Mutation anchor missing: {original}')
    text = text.replace(original, replacement)
destination = args.destination.resolve()
if destination == repo or repo in destination.parents:
    raise SystemExit('Mutants must be outside the repository')
environment = 'client' if source == client else 'main'
relative = Path(source).relative_to(f'src/{environment}/java')
target = destination / relative
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(text, encoding='utf-8')
print(f'-Ps29MutationOriginal={source}')
print(f'-Ps29MutationEnvironment={environment}')
print(f'-Ps29MutationDir={destination.as_posix()}')
