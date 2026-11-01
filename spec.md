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
