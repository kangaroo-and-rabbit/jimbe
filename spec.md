# Spécifications de jimbe

## Contrôles du build

- `mvn verify` exécute SpotBugs, et tout constat fait échouer le build. La CI lance la même commande avec `-Dgpg.skip=true`.
- Le plugin n'utilise pas de filtre include : un include vide n'autorise aucun constat, l'analyse passe alors sans rien vérifier.
- Une exclusion se justifie seulement quand une copie casserait le comportement. Elle va dans `spotbugs-exclude.xml`, ciblée sur la méthode et le pattern, avec sa raison.

## Propriété des données

- Un `ByteBuffer` qui entre dans un objet ou en sort est dupliqué avec `duplicate()` : le contenu reste partagé sans copie, et chacun garde sa position et sa limite.
- `asReadOnlyBuffer()` est exclu, car le code lit le tableau sous-jacent avec `array()`.
- `duplicate()` remet l'ordre d'octets en big-endian, celui d'EBML. Seul le codec private de `Track` garde l'ordre de l'appelant, car son format dépend du codec.
- Les `Date` sont copiées et les collections exposées sont des copies ou des vues en lecture seule.
- Restent partagés par conception : le `DataWriter` passé aux writers, le parent d'un `Element`, et les `VideoTrack`, `AudioTrack` et `TrackOperation` d'une `Track`.

## Concurrence

- `Reader` est prévu pour un usage séquentiel : `readFile()` se termine avant tout autre appel. `fillLock` sérialise `seek()` et le remplissage de la file de frames.

## Écriture Matroska

- Les tracks sont écrites au premier cluster, ou à la fermeture s'il n'y en a aucun : c'est là qu'elles sont connues. Elles prennent 4096 octets, ou leur taille plus 1024 si elles n'y tiennent pas. Cette place ne change plus ensuite, car les clusters la suivent.
- Une track ajoutée après le premier cluster n'est acceptée que si elle tient dans cette place. Sinon `addTrack()` lève `IllegalStateException` tout de suite, et le fichier reste correct.
- Un bloc porte une seule frame. `Writer` n'utilise pas le lacing, qui reste disponible par `SimpleBlock.setLaceMode()`. Le lacing fixe refuse des frames de tailles différentes.
- Un cluster s'arrête à 5 s ou à 4 Mo. Une frame clé peut le fermer à partir de 1 s ou de 512 Ko. Une frame clé peut être audio : sur un fichier avec audio, un cluster ne commence donc pas toujours sur une frame clé vidéo.
- Un bloc stocke son timecode sur 16 bits signés, relatif à la plus petite frame du cluster, donc de 0 à 32767 unités. Une frame hors de cette plage ferme le cluster avant d'être ajoutée. `SimpleBlock.setTimecode()` refuse une valeur hors plage au lieu de la tronquer.
- `Writer` garde l'ordre d'arrivée. Un fichier reste valide si les frames ne sont pas en ordre, mais un lecteur ne les trouvera qu'à leur place dans le fichier : c'est à l'appelant de les fournir triées par timecode, sous-titres compris.
- `DateElement` stocke des nanosecondes signées depuis 2001-01-01 UTC. `UnixEpochDelay` est en secondes.

## Lecture Matroska

- `Block` lit les trois lacings. La dernière frame prend ce que laissent les autres, et le compteur est non signé.
- `seek()` part du cue qui précède le dernier cue au plus égal au timecode, puis scanne jusqu'à la première frame au-delà. Le SeekHead et les Cues sont lus au premier `seek()`, pour qu'un index abîmé n'empêche pas d'ouvrir le fichier.
- Un index absent, illisible, désordonné, ou qui ne pointe pas sur un cluster, est ignoré : le scan part alors du premier cluster. Un index n'est suivi que si le temps et la position avancent ensemble.
- Les blocs ne sont pas triés entre pistes et les clusters se recouvrent dans le temps. Partir du cue exact perdrait des frames encore dues dans le cluster d'avant : partir un cue plus tôt donne le résultat d'un scan depuis le début. Si le scan trouve d'emblée une frame au-delà du timecode, il recule encore d'un cue.

## Forks

- `docs/upstream-forks.md` liste les forks de jebml audités, leur dernier commit et ce qui en a été repris. `tools/check-upstream-forks.py` dit ce qui a changé depuis.
