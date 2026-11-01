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
- Un bloc porte une seule frame. `Writer` n'utilise pas le lacing.
- Un bloc stocke son timecode sur 16 bits signés, relatif à la plus petite frame du cluster, donc de 0 à 32767 unités. Une frame hors de cette plage ferme le cluster avant d'être ajoutée. `SimpleBlock.setTimecode()` refuse une valeur hors plage au lieu de la tronquer.
- `Writer` garde l'ordre d'arrivée. Un fichier reste valide si les frames ne sont pas en ordre, mais un lecteur ne les trouvera qu'à leur place dans le fichier : c'est à l'appelant de les fournir triées par timecode, sous-titres compris.
- `DateElement` stocke des nanosecondes signées depuis 2001-01-01 UTC. `UnixEpochDelay` est en secondes.

## Lecture Matroska

- `Block` lit les trois lacings. La dernière frame prend ce que laissent les autres, et le compteur est non signé.
- `seek()` part du dernier cluster que les Cues donnent pour le timecode, puis scanne jusqu'à la première frame au-delà. Le SeekHead et les Cues sont lus au premier `seek()`.
- Un index absent, illisible, désordonné, ou qui ne pointe pas sur un cluster, est ignoré : le scan part alors du premier cluster. Un index n'est suivi que si le temps et la position avancent ensemble.
- Les blocs d'un cluster ne sont pas triés entre pistes. Si le scan trouve d'emblée une frame au-delà du timecode, il repart du cue précédent.
- Le scan depuis les Cues peut trouver une frame plus proche que le scan depuis le début, qui s'arrête à la première frame trop tardive, même dans un cluster antérieur.

## Forks

- `docs/upstream-forks.md` liste les forks de jebml audités, leur dernier commit et ce qui en a été repris. `tools/check-upstream-forks.py` dit ce qui a changé depuis.
