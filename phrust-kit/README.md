# PHRust Fork Kit (on top of PandaSpigot)

Atong kaugalingong fork, patch-based sama sa PandaSpigot. Gamay ra ang atong gi-usab sa server core;
ang jetpack ug vehicles naa gihapon sa plugin (wala nato gitandi ang plugin code).


## Pinaka-dali nga paagi sa pagkuha sa jar: GitHub Actions (walay local setup)
Dili kinahanglan JDK/Gradle sa imong PC. Ang GitHub mismo ang mu-build:
1. I-fork ang PandaSpigot. Sa imong fork, i-upload ang kini nga kit folder isip `phrust-kit/`.
2. I-copy ang `github-workflow/build-phrust.yml` ngadto sa `.github/workflows/build-phrust.yml`.
3. GitHub -> Actions -> **Build PHRust jar** -> Run workflow.
4. Pagkahuman (mga 10-20 min), i-download ang **PHRust-server-jar** gikan sa Artifacts. Mao na ang imong server jar (Paperclip).
Kung mu-red ang "Verify PHRust hook anchors", ipadala ang log; ayaw pugsa.
Note: dili pa nako na-run ning workflow (walay access ang sandbox sa GitHub Actions), gikopya ra nako gikan sa build.yml sa Panda.

## Steps (sa imong PC, kinahanglan JDK 17 + git + bash)
1. Fork https://github.com/hpfxd/PandaSpigot sa imong GitHub, dayon:
   `git clone <imong-fork> PHRustServer && cd PHRustServer`
2. `./panda patch`              # mubuhat sa PandaSpigot-Server/ ug PandaSpigot-API/
3. `python3 /path/to/kit/apply_phrust.py . --check`   # verify anchors, wala'y usbon
4. `python3 /path/to/kit/apply_phrust.py .`           # apply
5. `cd PandaSpigot-Server && git add -A && git commit -m "PHRust: engine, steer, brand"`
   `cd ../PandaSpigot-API && git add -A && git commit -m "PHRust: VehicleSteerEvent"`
6. `cd .. && ./panda rb`        # i-convert ang commits ngadto sa patches/ (i-commit sa imong fork)
7. `./panda jar`                # paperclip.jar
8. Kung naay bag-ong update sa Panda: `git pull` gikan sa upstream, `./panda patch`, ayuha ang conflict (kung naa).

Kung mu-fail ang step 3, ang message mag-ingon kung unsang anchor ang wala mag-match (usa ra ang gitugot).
Ayaw pugsa; ipadala ang error sa akoa.

## Unsa ang gi-add
| Gi-add | Unsa |
|---|---|
| `PHRustEngine` + `PHRustSubsystem` | Engine tick nga dili gyud mu-throw (isolated per subsystem, auto-disable pagkahuman sa 5 ka failure). Gi-hook human sa `worldserver.doTick()`. |
| `JetpackEngine` | **Parehas gyud ang public API** (setListener, setJetpacking, isJetpacking, handleJetpackMovement, Listener.onJetpackSteer) busa dili kinahanglan usbon ang PHRustJetpack plugin. Dugang: `clearPlayer()` (no leak) ug try/catch sa listener. |
| `PHRustSteer` | Una ang jetpack (parehas sa una), dayon i-fire ang `VehicleSteerEvent`. |
| `VehicleSteerEvent` (API) | Naa na sa API pero **wala gyud na-fire** sa daan nga code. Karon na-fire na kung naa'y nag-listen ug naka-sakay ang player. Cancel = skip vanilla steer. |
| `PHRustBrand` + `phrust.yml` | Client-brand whitelist (default OFF, gi-log ang brand). |

## phrust.yml (mugawas sa server folder sa first start)
```yaml
client-brand-whitelist:
  enabled: false
  log-brands: true
  allowed: [vanilla]
  kick-message: "&cYour Minecraft client is not allowed on this server."
engine:
  max-subsystem-failures: 5
```

## Para sa kalit nga restart (config sa host, dili code)
- `spigot.yml`: `settings.timeout-time` 60 -> 120-180; `settings.restart-on-crash: false` samtang nag-debug.
- Siguroha nga naa ang `restart-script` (`./start.sh`).
- Basaha ang `logs/latest.log` ug `crash-reports/`: "The server has stopped responding!" = watchdog; "Exception ticking world" = crash sa tick.

## Unsa ang verified / dili
- Verified: ang 4 ka anchors nag-match **gyud kausa** sa tinuod nga 1.8.8 NMS code (gisulayan sa Nacho copy);
  ang script idempotent; ang tanang Java files nag-compile (`-source 8`) batok sa minimal stubs; ang brand decoder gi-test.
- DILI verified: wala ma-build ang tibuok Panda dinhi (blocked ang Maven/Mojang). Ang anchors sa tinuod nga Panda tree
  mahimong magkalahi gamay; mao ang purpose sa `--check`.
- Pag-test sa staging una: jetpack, vehicles, steer, join/quit, ug brand log.
- Package: ang plugin mo-reference sa `net.minecraft.server.JetpackEngine`, sama sa una (nag-relocate sa v1_8_R3 sa jar). Parehas ang class name/package.
- GPL: kung i-share ang jar, kinahanglan i-share pud ang source.
