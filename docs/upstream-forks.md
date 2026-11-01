# Forks de jebml audités

jimbe est un fork de [Matroska-Org/jebml](https://github.com/Matroska-Org/jebml). Ce relevé dit ce qui a été lu dans l'amont et ses forks, et ce qui en a été fait, pour ne pas refaire ce travail.

Relevé du 2026-11-01 : 36 forks en plus de jimbe, y compris les forks de forks. L'amont est gelé depuis le 2020-02-17 et n'a aucun commit que jimbe n'a pas.

## Reprendre ce relevé

```sh
./tools/check-upstream-forks.py
```

Le script compare les branches de la section « Dernier commit de chaque branche » à GitHub. Il liste ce qui a bougé et les forks apparus depuis. Seuls ces commits restent à lire : `git log <hash noté>..<nouveau hash>`. Il faut ensuite mettre à jour les tableaux de ce fichier.

Un commit se lit par son idée : les paquets et les classes de jimbe sont renommés, rien ne s'applique tel quel. `MatroskaFile` est devenu `matroska/Reader`, `MatroskaFileWriter` `matroska/Writer`, `MatroskaFileCues` `model/Cues`, `MatroskaDocTypes` `model/DocTypes` et `MatroskaFileTrack` `model/Track`.

## Repris dans jimbe

Reproduit sur jimbe avant correction, puis vérifié après correction par les tests et par `ffprobe`.

| Sujet | Origine | Ce qui a été fait |
| --- | --- | --- |
| Date du segment fausse | PR #9, `b83683f` | Corrigé. `UnixEpochDelay` reste en secondes, contrairement à la PR. |
| Lacing par défaut, fichier illisible dès 3 frames | jitsi `63fbcc5` | Repris, et un BlockGroup sans lacing ne prend plus une deuxième frame. |
| Timecode de bloc sur 16 bits | jitsi `5cec011`, PR #12 | Idée reprise. Traite aussi la frame très en avance, et une valeur hors plage lève une erreur. |
| `Tracks` au-delà de 4096 octets | idée de jitsi `f346bca` | Écriture retardée au premier cluster, réservation adaptée. `b310849` n'est pas repris, voir plus bas. |
| Seek qui relit tout le fichier | idée de jitsi `cf1e79a` | Seek par les Cues, avec repli sur le scan si l'index est inutilisable. |

Ces deux bugs de jimbe n'étaient corrigés par aucun fork et l'ont été ici :

- Taille de la dernière frame lacée (`Block`), et compteur de lacing signé.
- `FloatElement.setValue()` qui écrivait un double dans 4 octets.

## Déjà présent dans jimbe

| Sujet | Commit |
| --- | --- |
| Position des Cues dans le SeekHead, PR #10 | `42ef48c`, jitsi `3e89be1` |
| Lecture paresseuse des frames | `08d256c` |
| Flags de SimpleBlock | `badce29`, aussi dans bdhandap `b53f8ee` |
| Tags en lecture | `c0155b5`, `e7c2920`, `9a5cb86` |
| Sûreté des buffers de `ProtoType` | `fd81f7c`, `63502ed`, `2053f68`. jimbe a choisi `duplicate()` : `asReadOnlyBuffer()` casse `array()`. |
| Patch de la taille du Segment | jitsi `09e9281` |
| Clusters regroupés au lieu d'un par frame | jitsi `1299842` |
| Conformité des Cues | `dccbc8f` |
| Synchronisation de la lecture | `0580979`. jimbe a `fillLock`. |
| ColourSpace | `58683e9` |

## Écarté

| Commit | Raison |
| --- | --- |
| jitsi `b310849` (débordement du Void) | Décale le fichier sans recalculer CueClusterPosition, SeekHead ni la taille du Segment (lecture du code, non exécuté). |
| jitsi `657ea84` | Marque toute frame non-clé comme jetable, ce qui est faux pour une frame référencée. |
| jitsi `09e9281` (éléments de la spec v4) | Plusieurs types sont erronés. |
| PR #11, `a75e65f` | Son bug de lecture ne touche pas jimbe, qui ignore les éléments inconnus. Déclarer Colour ne suffirait pas à le conserver au remux. |
| jitsi `f346bca`, `0977f1a` (écrivain non seekable) | karideo écrit toujours vers des fichiers. |
| jitsi `cf1e79a`, `7aa93d4`, `762109e` (lecture inverse) | Sans usage. |
| awaters1 `e11c4e8` | Contourne le bug de lacing en ignorant les frames. Le bug est corrigé dans jimbe. |
| rt1shnik `abcc957` | Lit tout le fichier à l'ouverture et change `scanFirstCluster` par défaut. Fait pour une application Android. |
| bdhandap `b53f8ee` | Limite de cluster à 1 Go et plusieurs champs de Track commentés. Seule la correction des flags était utile, et jimbe l'a. |
| Maven, Gradle, CI, publication, checkstyle | Propres à chaque fork : SiWoC, deusdixit, m2n-gmbh, Adnuntius, silverr21, awaters1, jitsi `c23b58b` à `0a5139f`, PDApps `b468f14` et `e95e398`. |
| PabloAbreu `692fa25` | Fusion de trois dépôts. L'agent de lecture n'y a rien trouvé de plus utile que jitsi. Lecture seule. |

## Restant

Rien de bloquant. À reprendre seulement si un besoin apparaît :

- CodecDelay n'est pas conservé au remux : l'Opus se décale de 7 ms, sans perte de donnée. L'écriture de SeekPreRoll manque aussi.
- Les frames d'un bloc lacé gardent toutes le timecode du bloc à la lecture, sans étalement par DefaultDuration.
- Colour perdu au remux : ajouter l'élément 0x55B0 et le recopier. Sans effet si le flux porte aussi ces informations.
- dstukalov `58bba67` : canal alpha.
- rt1shnik `d5b7a0e` : FlagHearingImpaired, FlagOriginal, FlagVisualImpaired, absents de `DocTypes`.
- `getTagList()` (`08fd130`), et ne plus écrire DefaultDuration, BitDepth et OutputSamplingFrequency à 0.

## Limites du relevé

- Le code des forks n'a pas été exécuté. Seuls les bugs de jimbe ont été reproduits.
- Les jugements sur la spec (bit discardable, types des éléments) viennent de la lecture, sans contrôle avec `mkvalidator`, absent de la machine.

## Dernier commit de chaque branche

`+a / -b` : commits que la branche a en plus de l'amont, et commits de l'amont qu'elle n'a pas.

| Fork | Branche | Dernier commit | Date | Écart |
| --- | --- | --- | --- | --- |
| Matroska-Org/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | amont |
| Adnuntius/jebml | master | `87094d9ef9f180381e742353531781fc97f7dbc8` | 2020-09-07 | +2 / -0 |
| Adnuntius/jebml | fix_checkstyle | `077a4ef27d95b52d11eea2509895ec6738e5b326` | 2020-09-07 | +1 / -0 |
| aszmyd/jebml | master | `478fd49ac59dc9e6cbca365be67dec435c254108` | 2017-08-22 | +30 / -7 |
| aszmyd/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| aszmyd/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| awaters1/jebml | master | `5b99ba45cc34ba55dcfdd6a76f445d1c5839dda9` | 2021-08-22 | +5 / -0 |
| bdhandap/jebml | transmux | `b53f8eef34340a90cc2a79ebfae952e90104becf` | 2020-06-18 | +1 / -0 |
| bdhandap/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | +0 / -0 |
| ddsouz/jebml | master | `40b5ce24d79f9701ebcd4fff77991ccd95fc52d6` | 2016-03-17 | +3 / -9 |
| ddsouz/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| ddsouz/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| deusdixit/jebml | master | `b904175e69f656a575481a75a230a2c76492eceb` | 2022-04-24 | +4 / -0 |
| dstukalov/jebml | master | `38ad455857abb6d290aa7df2a225170d183a5dc6` | 2022-02-10 | +4 / -0 |
| jitsi-kestrel/jebml | jitsi | `b3108495123419d1c297099660627b42af049ca7` | 2024-12-18 | +48 / -2 |
| jitsi-kestrel/jebml | jitsi-pr | `5cec011c4df36ea32981dfc488e99dc91d0038eb` | 2024-12-18 | +47 / -2 |
| jitsi-kestrel/jebml | drop-unused | `71fe104e72035077124c80ad3ce9e456055a11f1` | 2024-10-15 | +46 / -2 |
| jitsi-kestrel/jebml | master | `762109e439d8aa2a068c2f7013b9a7cdabfb7bc9` | 2019-11-04 | +39 / -2 |
| jitsi/jebml | jitsi | `b3108495123419d1c297099660627b42af049ca7` | 2024-12-18 | +48 / -2 |
| jitsi/jebml | jitsi-pr | `5cec011c4df36ea32981dfc488e99dc91d0038eb` | 2024-12-18 | +47 / -2 |
| jitsi/jebml | drop-unused | `71fe104e72035077124c80ad3ce9e456055a11f1` | 2024-10-15 | +46 / -2 |
| jitsi/jebml | master | `762109e439d8aa2a068c2f7013b9a7cdabfb7bc9` | 2019-11-04 | +39 / -2 |
| kokorin/jebml | master | `478fd49ac59dc9e6cbca365be67dec435c254108` | 2017-08-22 | +30 / -7 |
| kokorin/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| kokorin/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| m2n-gmbh/jebml | master | `bc5eb54af2229f5a5163fedb553f91979d69a549` | 2025-10-23 | +7 / -0 |
| mjameson-se/jebml | master | `762109e439d8aa2a068c2f7013b9a7cdabfb7bc9` | 2019-11-04 | +39 / -2 |
| mjameson-se/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| mjameson-se/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| PabloAbreu/jebml | master | `b0c73f6197677327b1a6a099e0ab7a606f5b0b15` | 2026-09-19 | +2 / -0 |
| PDApps/jebml | dicti | `e95e398dd84675338157675e5049d84cec992b8b` | 2022-12-07 | +43 / -2 |
| PDApps/jebml | master | `762109e439d8aa2a068c2f7013b9a7cdabfb7bc9` | 2019-11-04 | +39 / -2 |
| PDApps/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| PDApps/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| rt1shnik/jebml | dicti | `abcc9577f4a9f2437c8987e26fdc54e3db0c5c7e` | 2022-12-06 | +42 / -2 |
| rt1shnik/jebml | master | `762109e439d8aa2a068c2f7013b9a7cdabfb7bc9` | 2019-11-04 | +39 / -2 |
| rt1shnik/jebml | 2.0 | `c1b061442b6b1121e6f5594217988aba329f632e` | 2015-03-03 | +0 / -18 |
| rt1shnik/jebml | 2.1 | `05dfbb80000b065de10a850c1e403f33e5d5c82a` | 2015-04-01 | +0 / -10 |
| Scorpionchiques/jebml | colourMasterElement | `a75e65fc74814d97c12996df0ce352bf724faffa` | 2022-06-27 | +1 / -1 |
| Scorpionchiques/jebml | cues-fix | `42ef48ccaa985a10d382940d71ab03813eabe779` | 2022-06-27 | +1 / -0 |
| Scorpionchiques/jebml | master | `b83683fbba6ae5cc73414b2331313a8b9ceee434` | 2022-05-20 | +1 / -0 |
| silverr21/jebml | master | `8de8607bbbf2769d4783694071634e8118b30cb0` | 2021-12-29 | +1 / -0 |
| SiWoC/jebml | master | `7317f51faa2652dd18ec9f4889b49ae788c1fbf8` | 2024-12-08 | +4 / -2 |

jitsi et jitsi-kestrel portent aussi le tag `v2.4.4`, sur le commit `9c9a025`.

PR ouvertes sur l'amont : #9 `b83683f`, #10 `42ef48c`, #11 `a75e65f` (toutes trois de Scorpionchiques), #12 `5cec011` (jitsi).

## Forks sans commit propre

Leur branche est un ancêtre de l'amont : il n'y a rien à y lire.

| Fork | Branche | Dernier commit | Date | Retard |
| --- | --- | --- | --- | --- |
| alessandrotecnoprax/jebml | master | `b661890b4b27443755a9acef84409ec2c1df1197` | 2016-03-10 | -7 |
| arucard21/jebml | master | `a4e8ce67878731d9ff52331f7582b4cb86eff1b3` | 2011-03-09 | -53 |
| avvero/jebml | master | `b661890b4b27443755a9acef84409ec2c1df1197` | 2016-03-10 | -7 |
| bdiamond42/jebml | master | `61bfcef24e1c85635afb1327dacf50204a7c9e5f` | 2018-08-11 | -2 |
| dbarciela/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | -0 |
| dllentry/jebml | master | `61bfcef24e1c85635afb1327dacf50204a7c9e5f` | 2018-08-11 | -2 |
| enkigodalien/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | -0 |
| felixdoerre/jebml | master | `b661890b4b27443755a9acef84409ec2c1df1197` | 2016-03-10 | -7 |
| flipboar/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | -0 |
| indieboyalex/jebml | master | `a4e8ce67878731d9ff52331f7582b4cb86eff1b3` | 2011-03-09 | -53 |
| llftt/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | -0 |
| maneeshs/jebml | master | `7bfda1a67fe4b88db0af5eb536d78d8077f77e26` | 2020-02-17 | -0 |
| NineWorlds/jebml | master | `a4e8ce67878731d9ff52331f7582b4cb86eff1b3` | 2011-03-09 | -53 |
| NullEmpty/jebml | master | `b661890b4b27443755a9acef84409ec2c1df1197` | 2016-03-10 | -7 |
| RJPercival/jebml | master | `a4e8ce67878731d9ff52331f7582b4cb86eff1b3` | 2011-03-09 | -53 |
| shizm46/jebml | master | `a4e8ce67878731d9ff52331f7582b4cb86eff1b3` | 2011-03-09 | -53 |
| slebs/jebml | master | `630a549f45042aaf7fc9860738ea95bbb68f0661` | 2018-09-27 | -1 |
| taxusorg/jebml | master | `f20726bb23fb7ba1ed9f4311b49c2231531184ff` | 2018-08-10 | -3 |
