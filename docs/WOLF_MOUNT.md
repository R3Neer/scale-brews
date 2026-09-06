# Wolf tiny mount

Wolf extends the existing tiny-mount registry, saddle slot, direct steering and relative-size policy. The bundled definition permits a maximum rider/mount effective SCALE ratio of 0.76. Wolves must be adult, tamed and saddled. Any compatible player may ride; the owner remains unchanged. Wolf Armor uses BODY and the saddle uses SADDLE, and both remain on dismount.

WASD controls the wolf using the rider's look direction. The server owns movement and attacks. A short Space press (up to three server ticks) attempts the wolf's normal forward attack; holding Space charges a pounce over ten ticks. The existing horse jump HUD shows charge. Release launches along yaw with bounded pitch-dependent vertical movement. The first valid target intersected during a pounce receives one normal wolf attack. The action cooldown uses the vanilla melee goal's 20-tick interval. Landing removes residual pounce sliding.

The saddle uses the existing hand-authored leather pixel-art generator and manually adapted equipment geometry. It follows the wolf body animation and coexists with the vanilla armor layer. No image-generation assets are used.

Development validation covers shared mounting, owner and equipment preservation, effective-scale ratios, real damage attribution and transient commanded-attack scope. Additional two-player manual acceptance remains necessary for latency, team/PvP combinations and the complete betrayal interaction. No special gameplay tutorial is added.
