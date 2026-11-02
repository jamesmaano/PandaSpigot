#!/usr/bin/env python3
"""
Apply the PHRust kit onto a PandaSpigot checkout AFTER `./panda patch`.

  python3 apply_phrust.py /path/to/PandaSpigot            # apply
  python3 apply_phrust.py /path/to/PandaSpigot --check    # only verify anchors, change nothing

Safe by design: every anchor must match EXACTLY once, all edits are computed in memory first,
and nothing is written unless everything matched. Re-running is harmless (edits are tagged
with PHRUST-KIT and skipped if already present).
"""
import os, re, shutil, sys

KIT = os.path.dirname(os.path.abspath(__file__))
TAG = "PHRUST-KIT"

def die(msg):
    print("ERROR: " + msg)
    sys.exit(1)

def insert_after(text, pattern, builder, label, flags=re.M):
    if TAG + ":" + label in text:
        print("  = %s already applied" % label)
        return text
    matches = list(re.finditer(pattern, text, flags))
    if len(matches) != 1:
        die("anchor for '%s' matched %d times (need exactly 1). Pattern: %s" % (label, len(matches), pattern))
    m = matches[0]
    return text[:m.end()] + builder(m) + text[m.end():]

def main():
    if len(sys.argv) < 2:
        die("usage: apply_phrust.py <PandaSpigot dir> [--check]")
    root = sys.argv[1]
    check_only = "--check" in sys.argv
    srv = os.path.join(root, "PandaSpigot-Server", "src", "main", "java")
    api = os.path.join(root, "PandaSpigot-API", "src", "main", "java")
    nms = os.path.join(srv, "net", "minecraft", "server")
    for d in (srv, api, nms):
        if not os.path.isdir(d):
            die("missing %s - did you run './panda patch' first?" % d)

    edits = {}

    # 1) engine tick right after the world tick
    p = os.path.join(nms, "MinecraftServer.java")
    t = open(p, encoding="utf-8").read()
    t = insert_after(
        t, r"^([ \t]*)worldserver\.doTick\(\);[^\n]*\n",
        lambda m: "%scom.phrust.engine.PHRustEngine.tick(worldserver); // %s:engine engine tick, never throws\n" % (m.group(1), TAG),
        "engine")
    edits[p] = t

    # 2/3/4) PlayerConnection hooks
    p = os.path.join(nms, "PlayerConnection.java")
    t = open(p, encoding="utf-8").read()
    t = insert_after(
        t, r"public void a\(PacketPlayInSteerVehicle (\w+)\) \{\s*\n([ \t]*)PlayerConnectionUtils\.ensureMainThread\([^\n]*\n",
        lambda m: "%sif (PHRustSteer.handle(%s, this.player)) { return; } // %s:steer jetpack + VehicleSteerEvent\n" % (m.group(2), m.group(1), TAG),
        "steer")
    t = insert_after(
        t, r"^([ \t]*)this\.processedDisconnect = true;[^\n]*\n",
        lambda m: "%sJetpackEngine.clearPlayer(this.player.getUniqueID()); // %s:disconnect no jetpack UUID leak\n" % (m.group(1), TAG),
        "disconnect")
    t = insert_after(
        t, r"public void a\(PacketPlayInCustomPayload (\w+)\) \{\s*\n([ \t]*)PlayerConnectionUtils\.ensureMainThread\([^\n]*\n",
        lambda m: ("%sString phrustKick = com.phrust.engine.PHRustBrand.check(this.player.getName(), %s); // %s:brand\n"
                   "%sif (phrustKick != null) { this.disconnect(phrustKick); return; } // %s:brand\n")
                  % (m.group(2), m.group(1), TAG, m.group(2), TAG),
        "brand")
    edits[p] = t

    print("All anchors OK.")
    if check_only:
        return

    for path, text in edits.items():
        open(path, "w", encoding="utf-8").write(text)
        print("  edited " + os.path.relpath(path, root))
    for src_root, dst_root in ((os.path.join(KIT, "files", "server"), srv), (os.path.join(KIT, "files", "api"), api)):
        for d, _, fs in os.walk(src_root):
            for f in fs:
                s = os.path.join(d, f)
                dst = os.path.join(dst_root, os.path.relpath(s, src_root))
                os.makedirs(os.path.dirname(dst), exist_ok=True)
                shutil.copyfile(s, dst)
                print("  added  " + os.path.relpath(dst, root))
    print("Done. Next: cd PandaSpigot-Server && git add -A && git commit  (then ./panda rb to save as patches)")

main()
